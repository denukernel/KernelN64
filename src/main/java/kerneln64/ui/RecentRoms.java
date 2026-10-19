package kerneln64.ui;
import kerneln64.Main;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class RecentRoms {
    static final int LIMIT = 10;
    private final List<File> files = new ArrayList<>();

    public synchronized void add(File file) {
        try { file = file.getCanonicalFile(); }
        catch (IOException ignored) { file = file.getAbsoluteFile(); }
        final String path = file.getPath();
        files.removeIf(old -> old.getPath().equalsIgnoreCase(path));
        files.add(0, file);
        while (files.size() > LIMIT) files.remove(files.size() - 1);
    }

    public synchronized List<File> files() { return List.copyOf(files); }
    public synchronized void clear() { files.clear(); }

    public synchronized void load(Properties config) {
        clear();
        for (int i = LIMIT - 1; i >= 0; i--) {
            String path = config.getProperty("recent.rom." + i, "");
            if (!path.isBlank()) add(new File(path));
        }
    }

    public synchronized void save(Properties config) {
        config.keySet().removeIf(key -> key.toString().startsWith("recent.rom."));
        for (int i = 0; i < files.size(); i++) config.setProperty("recent.rom." + i, files.get(i).getPath());
    }
}
