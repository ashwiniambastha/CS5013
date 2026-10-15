package in.iitm.cs5013.tracker.ui;

import in.iitm.cs5013.tracker.config.AppPaths;
import in.iitm.cs5013.tracker.model.*;
import in.iitm.cs5013.tracker.service.*;
import in.iitm.cs5013.tracker.report.StatusBoardRenderer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/** UI orchestration only. Database and date decisions remain in services. */
public final class TrackerPanel extends JPanel {
    public record Services(ProjectService projects, WorkService work, StatusService status,
                           ImportService importer, ReportService reports, Clock clock) { }
    @FunctionalInterface private interface Job<T> { T run() throws Exception; }
    @FunctionalInterface private interface FormAction { void run(List<String> values, String projectId) throws Exception; }
    private record Choice(String id, String label) { public String toString() { return label; } }
    private record Snapshot(List<Project> projects, String selected, String board,
                            List<WeeklyUpdate> updates, List<Task> tasks, List<Followup> followups) { }

    private final Services services;
    private final DesktopDialogs dialogs;
    private final JComboBox<Choice> project = new JComboBox<>();
    private final JSpinner staleDays = new JSpinner(new SpinnerNumberModel(14, 1, 3650, 1));
    private final JTextArea dashboard = area();
    private final JTextArea messages = area();
    private final JTextArea reportPreview = area();
    private final JTextField reportDirectory = new JTextField(AppPaths.reports().toString());
    private final JTable projectTable = table("ID", "Student", "Project", "Status", "Started");
    private final JTable updatesTable = table("ID", "Date", "Summary", "Next steps");
    private final JTable tasksTable = table("ID", "Description", "Due", "Completed");
    private final JTable followupsTable = table("ID", "Description", "Requested", "Due", "Resolved");
    private final List<JButton> buttons = new ArrayList<>();
    private final List<JTextField> inputFields = new ArrayList<>();
    private boolean busy;
    private boolean applying;

    public TrackerPanel(Services services) {
        this(services, DesktopDialogs.swing());
    }

    public TrackerPanel(Services services, DesktopDialogs dialogs) {
        super(new BorderLayout(12, 12));
        this.services = services;
        this.dialogs = dialogs;
        reportDirectory.setName("report-directory");
        projectTable.setName("projects-table"); tasksTable.setName("tasks-table");
        followupsTable.setName("followups-table"); updatesTable.setName("updates-table");
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(1100, 780));
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        JPanel selection = new JPanel(new BorderLayout(8, 0));
        selection.add(new JLabel("Selected project:"), BorderLayout.WEST);
        project.setPrototypeDisplayValue(new Choice("", "P001 - Student name - ACTIVE"));
        project.setName("selected-project");
        selection.add(project, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(button("Refresh", this::refresh));
        actions.add(button("Import CSV / Excel", this::chooseImport));
        selection.add(actions, BorderLayout.EAST);
        toolbar.add(selection, BorderLayout.NORTH);
        JPanel rules = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rules.add(new JLabel("Flag updates older than (days):  "));
        rules.add(staleDays);
        rules.add(new JLabel("  Change the value, then click Refresh. Dates use yyyy-MM-dd."));
        toolbar.add(rules, BorderLayout.SOUTH);
        add(toolbar, BorderLayout.NORTH);

        var tabs = new JTabbedPane();
        tabs.setName("main-tabs");
        tabs.addTab("Dashboard", new JScrollPane(dashboard));
        tabs.addTab("Projects", projectsPanel());
        tabs.addTab("Follow-ups", followupsPanel());
        tabs.addTab("Reports", reportsPanel());
        add(tabs, BorderLayout.CENTER);
        messages.setRows(3);
        messages.setName("messages");
        messages.setText("Add a project or import a CSV / Excel file to begin. Select a project above to view its history.");
        add(new JScrollPane(messages), BorderLayout.SOUTH);
        project.addActionListener(event -> { if (!applying && !busy) refresh(); });
        refresh();
    }

    private JPanel projectsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel overview = new JPanel(new BorderLayout(8, 8));
        overview.add(scrollTable(projectTable), BorderLayout.CENTER);
        overview.add(button("Complete selected project", () -> {
            String id = selectedProject();
            if (id == null) { message("Select a project first."); return; }
            if (dialogs.confirmCompletion(this, id))
                mutate(() -> { services.projects().complete(id); return null; }, "Project completed.");
        }), BorderLayout.SOUTH);
        overview.setPreferredSize(new Dimension(800, 160));
        panel.add(overview, BorderLayout.NORTH);
        projectTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || applying || busy || projectTable.getSelectedRow() < 0) return;
            String id = projectTable.getValueAt(projectTable.getSelectedRow(), 0).toString();
            for (int i = 0; i < project.getItemCount(); i++) if (project.getItemAt(i).id().equals(id)) {
                project.setSelectedIndex(i); break;
            }
        });
        var details = new JTabbedPane();
        details.addTab("Add project", form("project", new String[]{"ID (blank = generate)", "Student name", "Project title", "Start date (yyyy-MM-dd)"},
            new String[]{"", "", "", today()}, false, "Add project", (v, id) ->
                services.projects().add(identifier(v.get(0), "P"), v.get(1), v.get(2), date(v.get(3), false))));
        JPanel updates = new JPanel(new BorderLayout(8, 8));
        updates.add(scrollTable(updatesTable), BorderLayout.CENTER);
        updates.add(form("update", new String[]{"Update date (yyyy-MM-dd)", "Progress summary", "Next steps"},
            new String[]{today(), "", ""}, true, "Save weekly update", (v, id) -> services.projects().addUpdate(
                UUID.randomUUID().toString(), id, date(v.get(0), false), v.get(1), v.get(2))), BorderLayout.SOUTH);
        details.addTab("Weekly updates", updates);
        JPanel tasks = new JPanel(new BorderLayout(8, 8));
        tasks.add(scrollTable(tasksTable), BorderLayout.CENTER);
        JPanel taskActions = new JPanel(new BorderLayout());
        taskActions.add(button("Mark selected task done", () -> closeItem(tasksTable, false)), BorderLayout.NORTH);
        taskActions.add(form("task", new String[]{"Task ID (blank = generate)", "Description", "Due date (optional, yyyy-MM-dd)"},
            new String[]{"", "", ""}, true, "Add task", (v, id) -> services.work().addTask(
                identifier(v.get(0), "T"), id, v.get(1), date(v.get(2), true))), BorderLayout.CENTER);
        tasks.add(taskActions, BorderLayout.SOUTH);
        details.addTab("Tasks", tasks);
        details.setName("project-detail-tabs");
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JPanel followupsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(scrollTable(followupsTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout());
        actions.add(button("Resolve selected follow-up", () -> closeItem(followupsTable, true)), BorderLayout.NORTH);
        actions.add(form("followup", new String[]{"ID (blank = generate)", "Request description", "Requested on (yyyy-MM-dd)", "Due date (optional, yyyy-MM-dd)"},
            new String[]{"", "", today(), ""}, true, "Add follow-up", (v, id) -> services.work().addFollowup(
                identifier(v.get(0), "F"), id, v.get(1), date(v.get(2), false), date(v.get(3), true))), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel reportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel options = new JPanel(new BorderLayout(8, 8));
        options.add(new JLabel("Save reports in:"), BorderLayout.WEST);
        options.add(reportDirectory, BorderLayout.CENTER);
        inputFields.add(reportDirectory);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button("Choose folder", () -> {
            Path directory = dialogs.chooseReportDirectory(this);
            if (directory != null) reportDirectory.setText(directory.toString());
        }));
        actions.add(button("Generate report", () -> {
            String directory = reportDirectory.getText();
            int days = (Integer) staleDays.getValue();
            submit(() -> {
                if (directory.isBlank()) throw new IllegalArgumentException("Choose a report directory.");
                Path output = services.reports().generate(Path.of(directory), days);
                return Map.entry(output, Files.readString(output));
            }, result -> {
                reportPreview.setText(result.getValue()); reportPreview.setCaretPosition(0);
                message("Saved report: " + result.getKey());
            });
        }));
        options.add(actions, BorderLayout.EAST);
        panel.add(options, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportPreview), BorderLayout.CENTER);
        return panel;
    }

    private JPanel form(String prefix, String[] labels, String[] initial, boolean needsProject, String actionName, FormAction action) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        var fields = new ArrayList<JTextField>();
        for (int i = 0; i < labels.length; i++) {
            var field = new JTextField(initial[i], 35);
            field.setName(prefix + "-" + i);
            field.getAccessibleContext().setAccessibleName(labels[i]);
            fields.add(field);
            inputFields.add(field);
            var c = new GridBagConstraints();
            c.gridx = 0; c.gridy = i; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(4, 4, 4, 12);
            var label = new JLabel(labels[i]); label.setLabelFor(field);
            panel.add(label, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, c);
        }
        var save = button(actionName, () -> {
            String id = selectedProject();
            if (needsProject && id == null) { message("Select a project first."); return; }
            // Capture UI values on the event thread before the background job starts.
            List<String> values = fields.stream().map(JTextField::getText).toList();
            submit(() -> { action.run(values, id); return null; }, result -> {
                for (int i = 0; i < fields.size(); i++) fields.get(i).setText(initial[i]);
                message(actionName + " succeeded."); refresh();
            });
        });
        save.setName(prefix + "-save");
        var c = new GridBagConstraints(); c.gridx = 1; c.gridy = labels.length; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 4, 4, 4); panel.add(save, c);
        JPanel anchored = new JPanel(new BorderLayout());
        anchored.add(panel, BorderLayout.NORTH);
        return anchored;
    }

    private void closeItem(JTable table, boolean followup) {
        int row = table.getSelectedRow();
        if (row < 0) { message("Select an item in the table first."); return; }
        String id = table.getValueAt(row, 0).toString();
        mutate(() -> {
            if (followup) services.work().resolveFollowup(id); else services.work().completeTask(id);
            return null;
        }, followup ? "Follow-up resolved." : "Task completed.");
    }

    private void chooseImport() {
        Path file = dialogs.chooseImport(this);
        if (file == null) return;
        submit(() -> services.importer().importFile(file, true), preview -> {
            if (dialogs.confirmImport(this, preview))
                mutate(() -> services.importer().importFile(file, false), "Import completed.");
            else message("Import cancelled. No records saved.");
        });
    }

    public void refresh() {
        String id = selectedProject();
        int days = (Integer) staleDays.getValue();
        submit(() -> {
            var all = services.projects().list(true);
            String selected = all.stream().anyMatch(p -> p.id().equals(id)) ? id : all.isEmpty() ? null : all.get(0).id();
            var buffer = new StringWriter();
            new StatusBoardRenderer().render(services.status().board(days), new PrintWriter(buffer));
            return new Snapshot(all, selected, buffer.toString(), selected == null ? List.of() : services.projects().updates(selected),
                selected == null ? List.of() : services.work().tasks(selected, true),
                selected == null ? List.of() : services.work().followups(selected, true));
        }, this::apply);
    }

    private void apply(Snapshot snapshot) {
        applying = true;
        try {
            project.removeAllItems();
            for (var p : snapshot.projects()) project.addItem(new Choice(p.id(), p.id() + " - " + p.studentName() + " - " + p.status()));
            for (int i = 0; i < project.getItemCount(); i++) if (project.getItemAt(i).id().equals(snapshot.selected())) project.setSelectedIndex(i);
            project.setToolTipText(project.getSelectedItem() == null ? "No project selected" : project.getSelectedItem().toString());
            fill(projectTable, snapshot.projects().stream().map(p -> new Object[]{p.id(),p.studentName(),p.title(),p.status(),p.startedOn()}).toList());
            for (int i = 0; i < projectTable.getRowCount(); i++)
                if (projectTable.getValueAt(i, 0).equals(snapshot.selected())) projectTable.setRowSelectionInterval(i, i);
            fill(updatesTable, snapshot.updates().stream().map(u -> new Object[]{u.id(),u.date(),u.summary(),u.nextSteps()}).toList());
            fill(tasksTable, snapshot.tasks().stream().map(t -> new Object[]{t.id(),t.description(),t.dueDate(),t.completedOn()}).toList());
            fill(followupsTable, snapshot.followups().stream().map(f -> new Object[]{f.id(),f.description(),f.requestedOn(),f.dueDate(),f.resolvedOn()}).toList());
            dashboard.setText(snapshot.board()); dashboard.setCaretPosition(0);
        } finally { applying = false; }
    }

    private <T> void mutate(Job<T> job, String success) {
        submit(job, result -> { message(success); refresh(); });
    }
    private <T> void submit(Job<T> job, Consumer<T> success) {
        if (busy) return;
        setBusy(true);
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return job.run(); }
            protected void done() {
                setBusy(false);
                try { success.accept(get()); }
                catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    message("Could not complete action: " + cause.getMessage());
                }
            }
        }.execute();
    }
    private void setBusy(boolean value) {
        busy = value;
        for (var button : buttons) button.setEnabled(!value);
        for (var field : inputFields) field.setEnabled(!value);
        project.setEnabled(!value); staleDays.setEnabled(!value);
        setCursor(Cursor.getPredefinedCursor(value ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
    public boolean isBusy() { return busy; }
    private String selectedProject() { return project.getSelectedItem() instanceof Choice c ? c.id() : null; }
    private String today() { return LocalDate.now(services.clock()).toString(); }
    private void message(String value) { messages.setText(value); messages.setCaretPosition(0); }
    private JButton button(String label, Runnable action) {
        var button = new JButton(label); buttons.add(button); button.addActionListener(event -> action.run()); return button;
    }
    private static LocalDate date(String value, boolean optional) {
        if (optional && value.isBlank()) return null;
        try { return LocalDate.parse(value.trim()); }
        catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException("Dates must use yyyy-MM-dd."); }
    }
    private static String identifier(String value, String prefix) { return value.isBlank() ? prefix + "-" + UUID.randomUUID() : value.trim(); }
    private static JTextArea area() {
        var area = new JTextArea(); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); area.setMargin(new Insets(12,12,12,12)); return area;
    }
    private static JTable table(String... columns) {
        var model = new DefaultTableModel(columns, 0) { public boolean isCellEditable(int row, int column) { return false; } };
        var table = new JTable(model) {
            public String getToolTipText(java.awt.event.MouseEvent event) {
                int row = rowAtPoint(event.getPoint()), column = columnAtPoint(event.getPoint());
                return row < 0 || column < 0 ? null : Objects.toString(getValueAt(row, column), "");
            }
        };
        table.setRowHeight(28); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true); return table;
    }
    private static void fill(JTable table, List<Object[]> rows) {
        var model = (DefaultTableModel) table.getModel(); model.setRowCount(0); for (var row : rows) model.addRow(row);
    }
    private static JScrollPane scrollTable(JTable table) {
        var pane = new JScrollPane(table);
        pane.setColumnHeaderView(table.getTableHeader());
        return pane;
    }
}
