package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FacultyFeaturesTest {
 @TempDir Path directory;
 Database database;ProjectService projects;WorkService work;RecordService records;BackupService backups;
 final Clock clock=Clock.fixed(Instant.parse("2026-11-02T06:00:00Z"),ZoneOffset.UTC);
 @BeforeEach void setup()throws Exception{
  database=new Database(directory.resolve("tracker.db"));database.initialize();
  projects=new ProjectService(new SqliteProjectRepository(database),clock);work=new WorkService(new SqliteWorkRepository(database),projects,clock);
  records=new RecordService(database,clock);backups=new BackupService(database,clock);
  projects.add("P1","Student A","Study",LocalDate.parse("2026-10-01"));
  projects.addUpdate("U1","P1",LocalDate.parse("2026-10-10"),"Old summary","");
  work.addTask("T1","P1","Draft",LocalDate.parse("2026-11-05"));
  work.addFollowup("F1","P1","Respond",LocalDate.parse("2026-10-10"),null);
 }
 @Test void editAllRecordTypesAndRejectInconsistentDates()throws Exception{
  records.edit(RecordService.Kind.UPDATE,"U1",List.of("2026-10-11","Revised summary","Proof"));
  records.edit(RecordService.Kind.TASK,"T1",List.of("New draft","2026-11-06",""));
  records.edit(RecordService.Kind.FOLLOWUP,"F1",List.of("New request","2026-10-11","2026-11-03",""));
  records.edit(RecordService.Kind.PROJECT,"P1",List.of("Student A","Revised title","COMPLETED","2026-10-01"));
  assertEquals("Revised summary",projects.updates("P1").get(0).summary());assertTrue(projects.list(false).isEmpty());
  assertThrows(IllegalArgumentException.class,()->records.edit(RecordService.Kind.PROJECT,"P1",List.of("Student A","Broken","ACTIVE","2026-10-12")));
  assertEquals("Revised title",projects.get("P1").title());
 }
 @Test void deleteProjectBacksUpAndRemovesDependentsAtomically()throws Exception{
  Path recovery=records.delete(RecordService.Kind.PROJECT,"P1",backups);
  assertTrue(projects.list(true).isEmpty());backups.restore(recovery);
  assertEquals(1,projects.updates("P1").size());assertEquals(1,work.tasks("P1",true).size());assertEquals(1,work.followups("P1",true).size());
 }
 @Test void exportsWorkbookAndSafeCsvWithoutChangingData()throws Exception{
  records.edit(RecordService.Kind.UPDATE,"U1",List.of("2026-10-10","=HYPERLINK(\"bad\")",""));
  var exporter=new ExportService(database);Path excel=exporter.export(directory,true);
  try(var workbook=new XSSFWorkbook(Files.newInputStream(excel))){assertEquals(5,workbook.getNumberOfSheets());assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,workbook.getSheet("updates").getRow(1).getCell(3).getCellType());}
  Path csv=exporter.export(directory,false);assertTrue(Files.readString(csv.resolve("updates.csv")).contains("'=HYPERLINK"));assertEquals(1,projects.updates("P1").size());
 }
 @Test void preferencesPersistAndDraftIsEncodedWithoutSending()throws Exception{
  var settings=new SettingsService(directory);Properties p=settings.load();p.setProperty("staleDays","7");p.setProperty("submissionUrl","https://example.com/s/abc");settings.save(p);
  assertEquals(7,new SettingsService(directory).staleDays());
  var draft=new ReminderService().draft("student@example.com","Student A","P1","Please send an update.",p.getProperty("submissionUrl"));
  assertEquals("mailto",draft.getScheme());assertTrue(draft.toString().contains("%20"));
  assertThrows(IllegalArgumentException.class,()->new ReminderService().draft("x@example.com\r\nBcc:other@example.com","A","P1","",""));
 }
}
