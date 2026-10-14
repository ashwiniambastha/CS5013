package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.report.TextSummaryRenderer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;

public final class ReportService {
    private final StatusService status;
    private final TextSummaryRenderer renderer;
    public ReportService(StatusService status, TextSummaryRenderer renderer) {
        this.status = status;
        this.renderer = renderer;
    }

    public Path generate(Path directory, int staleDays) throws SQLException, IOException {
        var board = status.board(staleDays);
        String content = renderer.render(board);
        Path outputDirectory = directory.toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        Path destination = outputDirectory.resolve("supervision-summary-" + board.asOf() + ".txt");
        Path temporary = Files.createTempFile(outputDirectory, ".summary-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
