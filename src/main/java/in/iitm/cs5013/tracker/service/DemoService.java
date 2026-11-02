package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.Database;
import java.io.IOException;
import java.sql.*;
import java.time.*;

/** Creates an atomic, twelve-week example only when the demo database is empty. */
public final class DemoService {
    private final Database database;
    private final Clock clock;
    public DemoService(Database database, Clock clock) { this.database = database; this.clock = clock; }

    public boolean seed() throws SQLException, IOException {
        database.initialize();
        try (var connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT COUNT(*) FROM projects")) {
                    if (result.getInt(1) > 0) { connection.rollback(); return false; }
                }
                LocalDate today = LocalDate.now(clock), start = today.minusWeeks(12);
                String[] names = {"Student A", "Student B", "Student C", "Student D", "Student E"};
                String[] titles = {"Graph Theory", "Probability Models", "Algebra Study", "Numerical Methods", "Completed Study"};
                for (int i = 0; i < names.length; i++) write(connection,
                    "INSERT INTO projects VALUES(?,?,?,?,?)", "D00"+(i+1), names[i], titles[i], i==4 ? "COMPLETED" : "ACTIVE", start);
                for (int week = 1; week <= 12; week++) {
                    write(connection, "INSERT INTO updates VALUES(?,?,?,?,?)", "DA"+week,"D001",start.plusWeeks(week),
                        "Week " + week + ": reviewed results and proof outline", "Prepare the next draft");
                    if (week <= 8) write(connection,"INSERT INTO updates VALUES(?,?,?,?,?)","DB"+week,"D002",start.plusWeeks(week),
                        "Week " + week + ": explored model assumptions", "Send revised calculations");
                }
                write(connection,"INSERT INTO updates VALUES(?,?,?,?,?)","DC1","D003",today.minusDays(14),"Reviewed algebra examples","Present findings");
                write(connection,"INSERT INTO updates VALUES(?,?,?,?,?)","DE1","D005",today.minusDays(7),"Final report submitted","");
                write(connection,"INSERT INTO tasks VALUES(?,?,?,?,?)","DT1","D001","Submit draft",today.plusDays(3),null);
                write(connection,"INSERT INTO tasks VALUES(?,?,?,?,?)","DT2","D002","Send overdue calculations",today.minusDays(2),null);
                write(connection,"INSERT INTO tasks VALUES(?,?,?,?,?)","DT3","D003","Present proof",today,null);
                write(connection,"INSERT INTO tasks VALUES(?,?,?,?,?)","DT4","D003","Submit slides",today,null);
                write(connection,"INSERT INTO tasks VALUES(?,?,?,?,?)","DT5","D001","Literature review",today.minusDays(7),today.minusDays(5));
                write(connection,"INSERT INTO followups VALUES(?,?,?,?,?,?)","DF1","D002","Reply with revised calculations",today.minusDays(10),today.minusDays(3),null);
                write(connection,"INSERT INTO followups VALUES(?,?,?,?,?,?)","DF2","D001","Confirm next meeting",today.minusDays(2),today.plusDays(1),null);
                write(connection,"INSERT INTO followups VALUES(?,?,?,?,?,?)","DF3","D003","Send bibliography",today.minusDays(10),today.minusDays(5),today.minusDays(2));
                connection.commit(); return true;
            } catch (SQLException | RuntimeException exception) { connection.rollback(); throw exception; }
        }
    }
    private static void write(Connection connection, String sql, Object... values) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) statement.setString(i+1,values[i]==null ? null : values[i].toString());
            statement.executeUpdate();
        }
    }
}
