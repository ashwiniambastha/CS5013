package in.iitm.cs5013.tracker.service;

import java.nio.file.*;
import java.io.*;
import java.util.Properties;

/** Preferences belong to the same data directory as this database, including demos. */
public final class SettingsService {
    private final Path file;
    public SettingsService(Path directory) { file = directory.resolve("preferences.properties"); }
    public synchronized Properties load() throws IOException {
        var result = new Properties();
        if (Files.exists(file)) try (var reader = Files.newBufferedReader(file)) { result.load(reader); }
        return result;
    }
    public int staleDays() throws IOException {
        try { int days = Integer.parseInt(load().getProperty("staleDays","14")); return days >= 1 && days <= 3650 ? days : 14; }
        catch (NumberFormatException exception) { return 14; }
    }
    public synchronized void save(Properties properties) throws IOException {
        int days = Integer.parseInt(properties.getProperty("staleDays","14"));
        if (days < 1 || days > 3650) throw new IllegalArgumentException("Choose 1 to 3650 days.");
        String url = properties.getProperty("submissionUrl","").trim();
        if (!url.isEmpty() && !url.startsWith("https://")) throw new IllegalArgumentException("The submission form must use HTTPS.");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(),".settings-",".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary)) { properties.store(writer,"Student Project Tracker preferences"); }
            Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
