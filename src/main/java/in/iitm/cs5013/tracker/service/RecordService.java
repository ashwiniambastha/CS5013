package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.model.Project;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Editable fields and table names are whitelisted; IDs never change. */
public final class RecordService {
    public enum Kind {
        PROJECT("projects",new String[]{"student_name","title","status","started_on"}),
        UPDATE("updates",new String[]{"update_date","summary","next_steps"}),
        TASK("tasks",new String[]{"description","due_date","completed_on"}),
        FOLLOWUP("followups",new String[]{"description","requested_on","due_date","resolved_on"});
        public final String table; public final String[] columns;
        Kind(String table,String[] columns) { this.table=table; this.columns=columns; }
    }
    private final Database database; private final Clock clock;
    public RecordService(Database database,Clock clock) { this.database=database;this.clock=clock; }
    public List<String> values(Kind kind,String id) throws Exception {
        try(var connection=database.open();var statement=connection.prepareStatement("SELECT "+String.join(",",kind.columns)+" FROM "+kind.table+" WHERE id=?")) {
            statement.setString(1,id);
            try(var row=statement.executeQuery()) {
                if(!row.next()) throw new IllegalArgumentException("Record no longer exists.");
                var values=new ArrayList<String>();for(int i=1;i<=kind.columns.length;i++) values.add(Objects.toString(row.getString(i),""));return values;
            }
        }
    }
    public void edit(Kind kind,String id,List<String> raw) throws Exception {
        if(raw.size()!=kind.columns.length) throw new IllegalArgumentException("Wrong field count.");
        List<String> values=raw.stream().map(String::trim).toList();
        for(int i=0;i<values.size();i++) {
            String column=kind.columns[i],value=values.get(i);
            boolean required=Set.of("student_name","title","status","started_on","summary","update_date","description","requested_on").contains(column);
            if(required) Project.required(value,column);
            if((column.endsWith("_on")||column.endsWith("_date"))&&!value.isEmpty()) {
                LocalDate date=LocalDate.parse(value);
                if(!column.equals("due_date")&&date.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException(column+" cannot be in the future.");
            }
            if(column.equals("status")) Project.Status.valueOf(value);
        }
        try(var connection=database.open()) {
            connection.setAutoCommit(false);
            try {
                String assignments=String.join(",",Arrays.stream(kind.columns).map(column->column+"=?").toList());
                try(var statement=connection.prepareStatement("UPDATE "+kind.table+" SET "+assignments+" WHERE id=?")) {
                    for(int i=0;i<values.size();i++) statement.setString(i+1,values.get(i).isEmpty()&&!kind.columns[i].equals("next_steps")?null:values.get(i));
                    statement.setString(values.size()+1,id);
                    if(statement.executeUpdate()!=1) throw new IllegalArgumentException("Record no longer exists.");
                }
                validateDates(connection); connection.commit();
            }catch(Exception exception){connection.rollback();throw exception;}
        }
    }
    private static void validateDates(Connection connection) throws SQLException {
        String[] checks={
            "SELECT 1 FROM updates u JOIN projects p ON p.id=u.project_id WHERE u.update_date<p.started_on",
            "SELECT 1 FROM tasks t JOIN projects p ON p.id=t.project_id WHERE t.due_date<p.started_on OR t.completed_on<p.started_on",
            "SELECT 1 FROM followups f JOIN projects p ON p.id=f.project_id WHERE f.requested_on<p.started_on OR f.due_date<f.requested_on OR f.resolved_on<f.requested_on"
        };
        for(String sql:checks) try(var statement=connection.createStatement();var row=statement.executeQuery(sql)) {
            if(row.next()) throw new IllegalArgumentException("Dates conflict with the project start or request date. No changes saved.");
        }
    }
    public Path delete(Kind kind,String id,BackupService backups) throws Exception {
        values(kind,id);
        Path recovery=backups.backup(backups.defaultDirectory());
        try(var connection=database.open()) {
            connection.setAutoCommit(false);
            try {
                if(kind==Kind.PROJECT) for(String table:List.of("updates","tasks","followups"))
                    try(var statement=connection.prepareStatement("DELETE FROM "+table+" WHERE project_id=?")) {statement.setString(1,id);statement.executeUpdate();}
                try(var statement=connection.prepareStatement("DELETE FROM "+kind.table+" WHERE id=?")) {
                    statement.setString(1,id);if(statement.executeUpdate()!=1) throw new IllegalArgumentException("Record no longer exists.");
                }
                connection.commit(); return recovery;
            }catch(Exception exception){connection.rollback();throw exception;}
        }
    }
}
