package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.Database;
import org.apache.commons.csv.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class ExportService {
    private final Database database;
    public ExportService(Database database){this.database=database;}
    private record Sheet(String name,List<String> headers,List<List<String>> rows){}
    public Path export(Path folder,boolean excel) throws Exception {
        List<Sheet> sheets=new ArrayList<>();
        try(var connection=database.open()) {
            connection.setAutoCommit(false);
            for(String table:List.of("projects","updates","tasks","followups","import_runs")) {
                try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT * FROM "+table+" ORDER BY id")) {
                    var headers=new ArrayList<String>();var rows=new ArrayList<List<String>>();
                    for(int i=1;i<=result.getMetaData().getColumnCount();i++)headers.add(result.getMetaData().getColumnName(i));
                    while(result.next()){var row=new ArrayList<String>();for(int i=1;i<=headers.size();i++)row.add(Objects.toString(result.getString(i),""));rows.add(row);}
                    sheets.add(new Sheet(table,headers,rows));
                }
            }
            connection.commit();
        }
        Files.createDirectories(folder);
        Path destination=folder.resolve("tracker-export-"+UUID.randomUUID()+(excel?".xlsx":""));
        if(excel) {
            try(var workbook=new XSSFWorkbook()) {
                for(var data:sheets) {
                    var sheet=workbook.createSheet(data.name());int number=0;
                    var lines=new ArrayList<List<String>>();lines.add(data.headers());lines.addAll(data.rows());
                    for(var line:lines){var row=sheet.createRow(number++);for(int i=0;i<line.size();i++)row.createCell(i).setCellValue(line.get(i));}
                    sheet.createFreezePane(0,1);for(int i=0;i<data.headers().size();i++)sheet.setColumnWidth(i,6000);
                }
                try(var output=Files.newOutputStream(destination,StandardOpenOption.CREATE_NEW)){workbook.write(output);}
            }
        } else {
            Files.createDirectory(destination);
            for(var data:sheets)try(var writer=Files.newBufferedWriter(destination.resolve(data.name()+".csv"));var csv=new CSVPrinter(writer,CSVFormat.DEFAULT)) {
                csv.printRecord(data.headers());for(var row:data.rows())csv.printRecord(row.stream().map(ExportService::safeCsv).toList());
            }
        }
        return destination;
    }
    private static String safeCsv(String value){String start=value.stripLeading();return !start.isEmpty()&&"=+@-".indexOf(start.charAt(0))>=0?"'"+value:value;}
}
