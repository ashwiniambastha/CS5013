package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.importer.ImportData;
import in.iitm.cs5013.tracker.repository.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ImportServiceTest {
    @TempDir Path temporary;
    private Database database;
    private ImportService importer;
    private static final String HEADER = String.join(",", ImportData.COLUMNS) + "\n";
    private static final String ROW = "P001,Student A,Graph Theory,2026-09-01,U001,2026-10-07,Read paper,Draft proof\n";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);

    @BeforeEach void setup() throws Exception {
        database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        importer = new ImportService(new SqliteImportRepository(database), CLOCK);
    }
    private Path csv(String content) throws Exception {
        Path path = temporary.resolve("updates.csv");
        Files.writeString(path, content);
        return path;
    }
    private int count(String table) throws Exception {
        try (var connection = database.open(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getInt(1);
        }
    }

    @Test void csvPreviewReimportAndQuotedFields() throws Exception {
        Path file = csv("\uFEFF" + HEADER + "\n" + ROW.replace("Read paper", "\"Read paper, checked proof\"")
            + ROW.replace("U001", "U002").replace("2026-10-07", "2026-10-14"));
        var preview = importer.importFile(file, true);
        assertEquals(1, preview.projectsAdded()); assertEquals(2, preview.updatesAdded());
        assertEquals(0, count("projects")); assertEquals(0, count("import_runs"));
        importer.importFile(file, false);
        var repeat = importer.importFile(file, false);
        assertEquals(0, repeat.updatesAdded()); assertEquals(2, repeat.updatesSkipped());
        assertEquals(1, count("projects")); assertEquals(2, count("updates")); assertEquals(1, count("import_runs"));
        var projects = new ProjectService(new SqliteProjectRepository(database), CLOCK);
        assertEquals("Read paper, checked proof", projects.updates("P001").get(1).summary());
    }

    @Test void reportsMultipleRowsAndRejectsEntireMalformedFile() throws Exception {
        Path file = csv(HEADER + ROW + ROW.replace("U001", "U002").replace("2026-10-07", "07/10/2026")
            + ROW.replace("U001", "U003").replace("2026-10-07", "2026-02-30"));
        var error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(file, false));
        assertTrue(error.getMessage().contains("Row 3")); assertTrue(error.getMessage().contains("Row 4"));
        assertEquals(0, count("projects")); assertEquals(0, count("updates"));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW + ROW), false));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER.replace("summary,", "") + ROW), false));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER), false));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW.replace("Draft proof", "Draft proof,extra")), false));
    }

    @Test void conflictingStoredIdsRejectAllNewRowsAndCompletedProjectsStayCompleted() throws Exception {
        importer.importFile(csv(HEADER + ROW), false);
        String other = ROW.replace("P001", "P002").replace("U001", "U002");
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + other + ROW.replace("Read paper", "Changed")), false));
        assertEquals(1, count("projects")); assertEquals(1, count("updates"));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW.replace("Graph Theory", "Different title")), false));
        var projects = new ProjectService(new SqliteProjectRepository(database), CLOCK);
        projects.complete("P001");
        assertEquals(1, importer.importFile(csv(HEADER + ROW), false).updatesSkipped());
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW.replace("U001", "U003")), false));
        assertTrue(projects.list(false).isEmpty());
    }

    @Test void databaseFailureRollsBackProjectsUpdatesAndImportHistory() throws Exception {
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_update BEFORE INSERT ON updates BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(java.sql.SQLException.class, () -> importer.importFile(csv(HEADER + ROW), false));
        assertEquals(0, count("projects")); assertEquals(0, count("updates")); assertEquals(0, count("import_runs"));
    }

    @Test void acceptsReorderedHeadersButRejectsFutureDatesAndConflictingProjectRows() throws Exception {
        Path reordered = csv("summary,update_date,update_id,started_on,project_title,student_name,project_id,next_steps,extra\n"
            + "Read paper,2026-10-07,U001,2026-09-01,Graph Theory,Student A,P001,,ignored\n");
        assertEquals(1, importer.importFile(reordered, true).updatesAdded());
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW.replace("2026-10-07", "2026-10-15")), false));
        assertThrows(IllegalArgumentException.class, () -> importer.importFile(csv(HEADER + ROW + ROW.replace("U001", "U002").replace("Student A", "Other student")), false));
        assertEquals(0, count("projects"));
    }

    @Test void excelUsesSharedValidationAcceptsDateCellsAndRejectsFormula() throws Exception {
        Path file = temporary.resolve("updates.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Updates");
            var header = sheet.createRow(0);
            for (int i = 0; i < ImportData.COLUMNS.size(); i++) header.createCell(i).setCellValue(ImportData.COLUMNS.get(i));
            var row = sheet.createRow(1);
            String[] values = ROW.trim().split(",", -1);
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            row.getCell(5).setCellValue(LocalDate.parse("2026-10-07"));
            row.getCell(5).setCellStyle(dateStyle);
            try (var stream = Files.newOutputStream(file)) { workbook.write(stream); }
            assertEquals(1, importer.importFile(file, false).updatesAdded());
            assertEquals(1, importer.importFile(csv(HEADER + ROW), false).updatesSkipped());
            row.getCell(6).setCellFormula("1+1");
            try (var stream = Files.newOutputStream(file)) { workbook.write(stream); }
        }
        var error = assertThrows(IllegalArgumentException.class, () -> importer.importFile(file, false));
        assertTrue(error.getMessage().contains("Row 2"));
        assertEquals(1, count("updates"));
    }
}
