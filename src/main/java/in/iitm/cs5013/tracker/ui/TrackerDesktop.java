package in.iitm.cs5013.tracker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class TrackerDesktop {
    private TrackerDesktop() { }
    public static void launch(TrackerPanel.Services services) {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Desktop mode needs a graphical display. Use CLI commands on this machine.");
        SwingUtilities.invokeLater(() -> {
            var frame = new JFrame("Student Project Progress Tracker");
            var panel = new TrackerPanel(services);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    if (panel.isBusy()) JOptionPane.showMessageDialog(frame, "Please wait for the current action to finish before closing.");
                    else frame.dispose();
                }
            });
            frame.setContentPane(panel);
            frame.pack();
            Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setMinimumSize(new Dimension(Math.min(980, available.width), Math.min(720, available.height)));
            frame.setSize(Math.min(frame.getWidth(), available.width), Math.min(frame.getHeight(), available.height));
            frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
}
