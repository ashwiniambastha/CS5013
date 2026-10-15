package in.iitm.cs5013.tracker.ui;

import in.iitm.cs5013.tracker.importer.ImportData;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.nio.file.Path;

/** File choices and confirmations are separate from the asynchronous workflows. */
public interface DesktopDialogs {
    Path chooseImport(Component parent);
    Path chooseReportDirectory(Component parent);
    boolean confirmImport(Component parent, ImportData.Result preview);
    boolean confirmCompletion(Component parent, String projectId);
    Path chooseBackup(Component parent);
    boolean confirmRestore(Component parent, Path file);

    static DesktopDialogs swing() {
        return new DesktopDialogs() {
            public Path chooseBackup(Component parent) {
                var chooser = new JFileChooser();
                chooser.setDialogTitle("Choose a tracker backup");
                chooser.setFileFilter(new FileNameExtensionFilter("Tracker backup (.db)", "db"));
                return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
            }
            public boolean confirmRestore(Component parent, Path file) {
                return JOptionPane.showConfirmDialog(parent, "Replace current records with this backup?\n" + file
                    + "\nClose other tracker windows first. A recovery copy will be saved.\nReports already exported are kept.",
                    "Restore backup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
            }
            public Path chooseImport(Component parent) {
                var chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Project updates (.csv, .xlsx)", "csv", "xlsx"));
                return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
            }
            public Path chooseReportDirectory(Component parent) {
                var chooser = new JFileChooser(); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
            }
            public boolean confirmImport(Component parent, ImportData.Result preview) {
                String summary = preview.projectsAdded() + " projects and " + preview.updatesAdded()
                    + " updates will be added.\n" + preview.updatesSkipped() + " unchanged updates will be skipped.\nSave this import?";
                return JOptionPane.showConfirmDialog(parent, summary, "Import preview", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
            }
            public boolean confirmCompletion(Component parent, String projectId) {
                return JOptionPane.showConfirmDialog(parent, "Mark " + projectId + " completed? Its history will be retained.",
                    "Complete project", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
            }
        };
    }
}
