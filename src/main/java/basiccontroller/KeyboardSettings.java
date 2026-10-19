package basiccontroller;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Properties;

public final class KeyboardSettings {
    public record Mapping(int[] keys, int strength, boolean circular) {
        public Mapping { keys = keys.clone(); }
        @Override public int[] keys() { return keys.clone(); }
    }
    public static volatile Mapping current = defaults();
    private static final File FILE = new File("BasicKeyboard.config");
    static { load(); }

    public static Mapping defaults() {
        return new Mapping(new int[] { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_Q, KeyEvent.VK_CONTROL, KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L,
            KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_ENTER,
            KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_Z }, 80, false);
    }

    public static void save(Mapping settings) throws IOException {
        Properties prop = new Properties();
        int[] keys = settings.keys();
        for (int i = 0; i < keys.length; i++) prop.setProperty("key." + i, Integer.toString(keys[i]));
        prop.setProperty("strength", Integer.toString(settings.strength()));
        prop.setProperty("circular", Boolean.toString(settings.circular()));
        try (OutputStream out = new FileOutputStream(FILE)) { prop.store(out, "Keyboard Controller Settings"); }
        current = settings;
    }

    private static void load() {
        if (!FILE.isFile()) return;
        try (InputStream in = new FileInputStream(FILE)) {
            Properties prop = new Properties(); prop.load(in);
            int[] keys = defaults().keys();
            for (int i = 0; i < keys.length; i++) keys[i] = Math.max(0, Integer.parseInt(prop.getProperty("key." + i, Integer.toString(keys[i]))));
            current = new Mapping(keys, Math.max(20, Math.min(127, Integer.parseInt(prop.getProperty("strength", "80")))), Boolean.parseBoolean(prop.getProperty("circular", "false")));
        } catch (IOException | IllegalArgumentException ignored) {}
    }
}
