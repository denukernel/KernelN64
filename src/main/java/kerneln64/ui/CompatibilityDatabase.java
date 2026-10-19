package kerneln64.ui;
import kerneln64.Main;


import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Local, user-maintained records keyed by exact ROM SHA-256. */
public final class CompatibilityDatabase {
    private static final Path FILE = Path.of("Compatibility.properties");
    private static final Properties records = new Properties();
    static {
        if (Files.isRegularFile(FILE)) try (InputStream in = Files.newInputStream(FILE)) { records.load(in); }
        catch (IOException error) { System.err.println("Cannot read compatibility database: " + error); }
    }
    public static synchronized void recordRom(File rom, String hash) {
        records.setProperty(hash + ".name", rom.getName());
        records.setProperty(hash + ".lastOpened", Instant.now().toString());
        try { save(); } catch (IOException error) { System.err.println("Cannot save compatibility database: " + error); }
    }
    public static synchronized List<String> hashes() {
        return records.stringPropertyNames().stream().filter(key -> key.endsWith(".name")).map(key -> key.substring(0, key.length() - 5))
            .sorted(Comparator.comparing(hash -> get(hash, "name", ""), String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public static synchronized String get(String hash, String key, String fallback) { return records.getProperty(hash + "." + key, fallback); }
    public static synchronized void update(String hash, String status, String notes, String graphics, String audio, String input) throws IOException {
        records.setProperty(hash + ".status", status); records.setProperty(hash + ".notes", notes);
        records.setProperty(hash + ".graphics", graphics); records.setProperty(hash + ".audio", audio); records.setProperty(hash + ".input", input);
        records.setProperty(hash + ".tested", Instant.now().toString()); save();
    }
    private static void save() throws IOException {
        Path temp = Files.createTempFile(FILE.toAbsolutePath().getParent(), "compatibility-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { records.store(out, "KernelN64 local compatibility notes; exact ROM hashes"); }
            try { Files.move(temp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
