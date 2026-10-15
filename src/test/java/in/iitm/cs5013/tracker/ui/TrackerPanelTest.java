package in.iitm.cs5013.tracker.ui;

import in.iitm.cs5013.tracker.repository.*;
import in.iitm.cs5013.tracker.service.*;
import in.iitm.cs5013.tracker.report.TextSummaryRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import in.iitm.cs5013.tracker.importer.ImportData;
import static org.junit.jupiter.api.Assertions.*;

class TrackerPanelTest {
    @TempDir Path temporary;

    @Test void importsRequireConfirmationAndErrorsRemainVisible() throws Exception {
        var database = new Database(temporary.resolve("tracker.db")); database.initialize();
        var clock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        var status = new StatusService(projects, work, clock);
        var importer = new ImportService(new SqliteImportRepository(database), clock);
        Path file = temporary.resolve("updates.csv");
        Files.copy(Path.of("fixtures/updates-template.csv"), file);
        var confirm = new AtomicReference<>(false);
        var preview = new AtomicReference<ImportData.Result>();
        var restoreFile = new AtomicReference<Path>();
        DesktopDialogs dialogs = new DesktopDialogs() {
            public Path chooseImport(Component parent) { return file; }
            public Path chooseReportDirectory(Component parent) { return temporary.resolve("reports"); }
            public boolean confirmImport(Component parent, ImportData.Result result) { preview.set(result); return confirm.get(); }
            public boolean confirmCompletion(Component parent, String id) { return confirm.get(); }
            public Path chooseBackup(Component parent) { return restoreFile.get(); }
            public boolean confirmRestore(Component parent, Path file) { return confirm.get(); }
        };
        var reference = new AtomicReference<TrackerPanel>();
        SwingUtilities.invokeAndWait(() -> reference.set(new TrackerPanel(new TrackerPanel.Services(
            projects,work,status,importer,new ReportService(status,new TextSummaryRenderer()),clock,new BackupService(database,clock)), dialogs)));
        var panel = reference.get(); awaitIdle(panel);
        clickLabel(panel,"Import CSV / Excel"); awaitIdle(panel);
        assertEquals(3, preview.get().updatesAdded()); assertTrue(projects.list(true).isEmpty());
        confirm.set(true);
        clickLabel(panel,"Import CSV / Excel"); awaitIdle(panel);
        assertEquals(2, projects.list(true).size());
        clickLabel(panel,"Import CSV / Excel"); awaitIdle(panel);
        assertEquals(3, preview.get().updatesSkipped());
        clickLabel(panel,"Backup"); awaitIdle(panel);
        try (var files = Files.list(temporary.resolve("backups"))) { restoreFile.set(files.findFirst().orElseThrow()); }
        // Completing the selected project retains it in history, but removes it from the board.
        clickLabel(panel,"Complete selected project"); awaitIdle(panel);
        assertEquals(1, projects.list(false).size()); assertEquals(2, projects.list(true).size());
        confirm.set(false); clickLabel(panel,"Restore"); awaitIdle(panel);
        assertEquals(1, projects.list(false).size());
        confirm.set(true); clickLabel(panel,"Restore"); awaitIdle(panel);
        assertEquals(2, projects.list(false).size());
        Files.writeString(file,"broken,header\n"); preview.set(null);
        clickLabel(panel,"Import CSV / Excel"); awaitIdle(panel);
        assertNull(preview.get());
        SwingUtilities.invokeAndWait(() -> assertTrue(((JTextArea)find(panel,"messages")).getText().contains("missing column")));
        assertEquals(2, projects.list(true).size());
    }

    @Test void formsSaveIndependentWorkValidateErrorsAndGenerateReport() throws Exception {
        var database = new Database(temporary.resolve("tracker.db")); database.initialize();
        var clock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        var status = new StatusService(projects, work, clock);
        var importer = new ImportService(new SqliteImportRepository(database), clock);
        var reference = new AtomicReference<TrackerPanel>();
        SwingUtilities.invokeAndWait(() -> reference.set(new TrackerPanel(new TrackerPanel.Services(
            projects,work,status,importer,new ReportService(status,new TextSummaryRenderer()),clock,new BackupService(database,clock)))));
        TrackerPanel panel = reference.get(); awaitIdle(panel);
        set(panel,"project-0","P001"); set(panel,"project-1","Student A"); set(panel,"project-2","Graph Theory");
        click(panel,"project-save"); awaitIdle(panel);
        assertEquals("Student A", projects.get("P001").studentName());
        set(panel,"update-1","Read first paper"); set(panel,"update-2","Draft proof");
        click(panel,"update-save"); awaitIdle(panel);
        assertEquals(1,projects.updates("P001").size());
        set(panel,"task-0","T001"); set(panel,"task-1","Submit draft"); set(panel,"task-2","2026-10-20");
        click(panel,"task-save"); awaitIdle(panel);
        set(panel,"followup-0","F001"); set(panel,"followup-1","Send proof");
        click(panel,"followup-save"); awaitIdle(panel);
        SwingUtilities.invokeAndWait(() -> ((JTable)find(panel,"tasks-table")).setRowSelectionInterval(0,0));
        clickLabel(panel,"Mark selected task done"); awaitIdle(panel);
        assertTrue(work.tasks("P001",false).isEmpty()); assertEquals(1,work.followups("P001",false).size());
        // Invalid form data produces a visible message and no update write.
        set(panel,"update-0","not-a-date"); set(panel,"update-1","Invalid update");
        click(panel,"update-save"); awaitIdle(panel);
        SwingUtilities.invokeAndWait(() -> assertTrue(((JTextArea)find(panel,"messages")).getText().contains("yyyy-MM-dd")));
        assertEquals(1,projects.updates("P001").size());
        Path reports = temporary.resolve("reports"); set(panel,"report-directory",reports.toString());
        clickLabel(panel,"Generate report"); awaitIdle(panel);
        assertTrue(Files.readString(reports.resolve("supervision-summary-2026-10-14.txt")).contains("Send proof"));
        SwingUtilities.invokeAndWait(() -> ((JTable)find(panel,"followups-table")).setRowSelectionInterval(0,0));
        clickLabel(panel,"Resolve selected follow-up"); awaitIdle(panel);
        assertTrue(work.followups("P001",false).isEmpty());
        // Render all four tabs for visual inspection without controlling the desktop.
        SwingUtilities.invokeAndWait(() -> {
            try {
                panel.setSize(1100,780); layout(panel);
                var tabs = (JTabbedPane)find(panel,"main-tabs");
                Files.createDirectories(Path.of("target/ui-preview"));
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    tabs.setSelectedIndex(i); layout(panel);
                    var image = new BufferedImage(1100,780,BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
                    ImageIO.write(image,"png",Path.of("target/ui-preview/tab-"+i+".png").toFile());
                }
                tabs.setSelectedIndex(1);
                var details = (JTabbedPane)find(panel,"project-detail-tabs");
                for (int i = 1; i < details.getTabCount(); i++) {
                    details.setSelectedIndex(i); layout(panel);
                    var image = new BufferedImage(1100,780,BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
                    ImageIO.write(image,"png",Path.of("target/ui-preview/project-detail-"+i+".png").toFile());
                }
            } catch (Exception exception) { throw new RuntimeException(exception); }
        });
    }

    private static void set(TrackerPanel panel,String name,String value) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JTextField)find(panel,name)).setText(value));
    }
    private static void click(TrackerPanel panel,String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> ((JButton)find(panel,name)).doClick());
    }
    private static void clickLabel(TrackerPanel panel,String label) throws Exception {
        SwingUtilities.invokeAndWait(() -> button(panel,label).doClick());
    }
    private static Component find(Container parent,String name) {
        for (var component : parent.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container child) { var result = find(child,name); if(result != null) return result; }
        }
        return null;
    }
    private static JButton button(Container parent,String label) {
        for(var component:parent.getComponents()) {
            if(component instanceof JButton button && label.equals(button.getText())) return button;
            if(component instanceof Container child) { var result=button(child,label); if(result!=null) return result; }
        }
        return null;
    }
    private static void awaitIdle(TrackerPanel panel) throws Exception {
        long deadline=System.nanoTime()+Duration.ofSeconds(15).toNanos();
        while(System.nanoTime()<deadline) {
            var busy=new AtomicReference<Boolean>(); SwingUtilities.invokeAndWait(() -> busy.set(panel.isBusy()));
            if(!busy.get()) return;
            Thread.sleep(20);
        }
        fail("Desktop action did not finish within 15 seconds");
    }
    private static void layout(Container parent) {
        parent.doLayout(); for(var component:parent.getComponents()) if(component instanceof Container child) layout(child);
    }
}
