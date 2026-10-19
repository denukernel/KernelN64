package kerneln64.ui;
import gln64j.GLN64jPlugin;
import kerneln64.hardware.Audio;
import kerneln64.hardware.Pif;
import kerneln64.hardware.Video;
import kerneln64.Main;


import java.util.Properties;
import javax.swing.JOptionPane;
import plugin.AudioPlugin;
import plugin.GfxPlugin;

public final class PluginSettings {
    static final String[] KEYS = { Video.GFX_PLUGIN, Audio.AUDIO_PLUGIN, Pif.CONTROLLER_PLUGIN };
    public static final String[][] CLASSES = {
        { "gln64j.GLN64jPlugin", "cfb.CFBPlugin", "d3d11.D3D11Plugin" },
        { "aziaudio.AziAudioPlugin", "basicaudio.BasicAudioPlugin" },
        { "basiccontroller.BasicKeyboardPlugin", "basiccontroller.NoControllerPlugin" }
    };
    static final String[][] NAMES = {
        { "glN64j (recommended)", "CFB (framebuffer only)", "Direct3D 11 (experimental)" },
        { "Azimer HLE Audio (recommended)", "Basic Audio (fallback)" },
        { "Basic Keyboard", "None (controller disconnected)" }
    };

    public static void migrate(Properties config) {
        for (int i = 0; i < KEYS.length; i++) {
            String value = config.getProperty(KEYS[i], "");
            if (value.startsWith("denu64audio.") || value.startsWith("customaudio.") || value.startsWith("bgfx.") || value.startsWith("jario64plus."))
                config.setProperty(KEYS[i], CLASSES[i][0]);
        }
    }

    static int selected(int type) {
        String value = Main.pluginSetting(KEYS[type], CLASSES[type][0]);
        for (int i = 0; i < CLASSES[type].length; i++) if (CLASSES[type][i].equals(value)) return i;
        return 0;
    }

    static void configure(int type) {
        try {
            if (type == 0) {
                GfxPlugin plugin = Main.video == null ? null : Main.video.gfxPlugin;
                if (plugin == null) plugin = (GfxPlugin) Class.forName(CLASSES[0][selected(0)]).getDeclaredConstructor().newInstance();
                if (plugin instanceof cfb.CFBPlugin)
                    JOptionPane.showMessageDialog(Main.hMainWindow, "CFB renders the game's framebuffer and has no configurable options.", "Graphics Plugin", JOptionPane.INFORMATION_MESSAGE);
                else plugin.pluginConfig(Main.hMainWindow);
            } else if (type == 1) {
                AudioPlugin plugin = Main.audio == null ? null : Main.audio.audioPlugin;
                if (plugin == null) plugin = (AudioPlugin) Class.forName(CLASSES[1][selected(1)]).getDeclaredConstructor().newInstance();
                if (plugin instanceof basicaudio.BasicAudioPlugin)
                    JOptionPane.showMessageDialog(Main.hMainWindow, "Basic Audio has no configurable options.", "Audio Plugin", JOptionPane.INFORMATION_MESSAGE);
                else plugin.pluginConfig(Main.hMainWindow);
            } else {
                plugin.ControllerPlugin input = Main.pif == null ? null : Main.pif.controllerPlugin;
                if (input == null) input = (plugin.ControllerPlugin)Class.forName(CLASSES[2][selected(2)]).getDeclaredConstructor().newInstance();
                input.pluginConfig(Main.hMainWindow);
            }
        } catch (Exception error) {
            JOptionPane.showMessageDialog(Main.hMainWindow, error.getMessage(), "Plugin Configuration", JOptionPane.ERROR_MESSAGE);
        }
    }
}
