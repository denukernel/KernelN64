package gln64j.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class GLN64jPluginSettings {
    public static boolean enableFog = true;
    public static boolean bilinear = true;
    public static boolean wireframe = false;

    public static int windowWidth = 640, windowHeight = 480;
    public static boolean lockWindow = false, alwaysOnTop = false;

    public static void applyWindow(javax.swing.JFrame window) {
        if (window == null) return;
        window.setResizable(!lockWindow);
        window.setAlwaysOnTop(alwaysOnTop);
        java.awt.Component center = ((java.awt.BorderLayout) window.getContentPane().getLayout())
                .getLayoutComponent(java.awt.BorderLayout.CENTER);
        if (center != null) {
            center.setPreferredSize(new java.awt.Dimension(windowWidth, windowHeight));
            window.setExtendedState(javax.swing.JFrame.NORMAL);
            window.pack();
        }
    }

    private static final File FILE = new File("./GLN64j.config");

    static { load(); }

    public static void save() {
        try {
            Properties prop = new Properties();
            prop.setProperty("windowWidth", Integer.toString(windowWidth));
            prop.setProperty("windowHeight", Integer.toString(windowHeight));
            prop.setProperty("lockWindow", Boolean.toString(lockWindow));
            prop.setProperty("alwaysOnTop", Boolean.toString(alwaysOnTop));
            prop.setProperty("fog", Boolean.toString(enableFog));
            prop.setProperty("bilinear", Boolean.toString(bilinear));
            prop.setProperty("wireframe", Boolean.toString(wireframe));
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                prop.store(out, "GLN64j Plugin Settings");
            }
        } catch (Exception ignored) {}
    }
    
    public static void load() {
        try {
            if (!FILE.exists()) {
                save();
                return;
            }
            Properties prop = new Properties();
            try (FileInputStream in = new FileInputStream(FILE)) {
                prop.load(in);
            }
            windowWidth = Math.max(320, Math.min(3840, Integer.parseInt(prop.getProperty("windowWidth", "640"))));
            windowHeight = Math.max(240, Math.min(2160, Integer.parseInt(prop.getProperty("windowHeight", "480"))));
            lockWindow = Boolean.parseBoolean(prop.getProperty("lockWindow", "false"));
            alwaysOnTop = Boolean.parseBoolean(prop.getProperty("alwaysOnTop", "false"));
            enableFog = Boolean.parseBoolean(prop.getProperty("fog", "true"));
            bilinear = Boolean.parseBoolean(prop.getProperty("bilinear", "true"));
            wireframe = Boolean.parseBoolean(prop.getProperty("wireframe", "false"));
        } catch (Exception ignored) {}
    }
}
