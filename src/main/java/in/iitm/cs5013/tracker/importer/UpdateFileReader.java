package in.iitm.cs5013.tracker.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Both formats produce the same ordered fields for shared validation. */
public final class UpdateFileReader {
    public ImportData.Parsed read(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) return csv(path);
        if (name.endsWith(".xlsx")) return excel(path);
        throw new IllegalArgumentException("Choose a .csv or .xlsx file.");
    }

    private static List<Integer> mapping(List<String> header) {
        var errors = new ArrayList<String>();
        var normalized = header.stream().map(value -> value.replace("\uFEFF", "").trim()).toList();
        if (new HashSet<>(normalized).size() != normalized.size()) errors.add("Header: duplicate column names.");
        var indexes = new ArrayList<Integer>();
        for (String column : ImportData.COLUMNS) {
            int index = normalized.indexOf(column);
            if (index < 0) errors.add("Header: missing column " + column);
            indexes.add(index);
        }
        if (!errors.isEmpty()) throw ImportData.invalid(errors);
        return indexes;
    }

    private ImportData.Parsed csv(Path path) throws IOException {
        var rows = new ArrayList<ImportData.Row>();
        var errors = new ArrayList<String>();
        // Keep empty physical records so record numbers remain useful. Quoted multiline
        // values are one CSV record; reported numbers are record numbers, not line numbers.
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             var parser = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).build().parse(reader)) {
            var records = parser.getRecords();
            if (records.isEmpty()) throw ImportData.invalid(List.of("Header: file is empty."));
            var indexes = mapping(records.get(0).toList());
            int width = records.get(0).size();
            for (int i = 1; i < records.size(); i++) {
                var record = records.get(i);
                if (record.toList().stream().allMatch(String::isBlank)) continue;
                if (record.size() != width) {
                    errors.add("Row " + record.getRecordNumber() + ": field count differs from the header.");
                    continue;
                }
                rows.add(new ImportData.Row(record.getRecordNumber(), indexes.stream().map(record::get).toList()));
            }
        }
        return new ImportData.Parsed(rows, errors);
    }

    private ImportData.Parsed excel(Path path) throws IOException {
        var rows = new ArrayList<ImportData.Row>();
        var errors = new ArrayList<String>();
        try (var stream = Files.newInputStream(path); var workbook = new XSSFWorkbook(stream)) {
            if (workbook.getNumberOfSheets() == 0) throw ImportData.invalid(List.of("Header: workbook has no sheets."));
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            if (header == null) throw ImportData.invalid(List.of("Header: first sheet must have headers in row 1."));
            var formatter = new DataFormatter(Locale.ROOT);
            var names = new ArrayList<String>();
            for (int column = 0; column < header.getLastCellNum(); column++) names.add(formatter.formatCellValue(header.getCell(column)));
            var indexes = mapping(names);
            for (int number = 1; number <= sheet.getLastRowNum(); number++) {
                var row = sheet.getRow(number);
                if (row == null) continue;
                var values = new ArrayList<String>();
                boolean bad = false;
                for (int i = 0; i < indexes.size(); i++) {
                    var cell = row.getCell(indexes.get(i));
                    if (cell != null && (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR)) {
                        errors.add("Row " + (number + 1) + ": " + ImportData.COLUMNS.get(i) + " must contain a value, not a formula/error.");
                        bad = true;
                    }
                    boolean dateColumn = i == 3 || i == 5;
                    if (dateColumn && cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        values.add(cell.getLocalDateTimeCellValue().toLocalDate().toString());
                    } else {
                        values.add(formatter.formatCellValue(cell));
                    }
                }
                if (!bad && !values.stream().allMatch(String::isBlank)) rows.add(new ImportData.Row(number + 1, values));
            }
        }
        return new ImportData.Parsed(rows, errors);
    }
}
