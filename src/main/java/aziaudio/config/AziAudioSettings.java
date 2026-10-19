package aziaudio.config;
import aziaudio.playback.AudioCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class AziAudioSettings {

    private static final File FILE = new File("./AziAudio.config");

    public static volatile int volume = 100;
    public static volatile int balance = 0;
    public static volatile boolean mono = false, swapChannels = false, muteFastForward = false;
    // 0 = cubic (default), 1 = linear, 2 = nearest.
    public static volatile int interpolation = 0;
    public static boolean syncAudio = true;
    public static boolean forceOldSync = false;
    public static boolean aiEmulation = true;
    public static boolean hle = true;
    public static int bufferDivider = 10;
    
    // Non-persistent runtime config? Or save it? Original saved it.
    public static volatile boolean mute = false;

    static {
        load();
    }

    public static void save() {
        try {
            Properties prop = new Properties();
            prop.setProperty("balance", Integer.toString(balance));
            prop.setProperty("mono", Boolean.toString(mono));
            prop.setProperty("swapChannels", Boolean.toString(swapChannels));
            prop.setProperty("muteFastForward", Boolean.toString(muteFastForward));
            prop.setProperty("interpolation", Integer.toString(interpolation));
            prop.setProperty("volume", Integer.toString(volume));
            prop.setProperty("syncAudio", Boolean.toString(syncAudio));
            prop.setProperty("forceOldSync", Boolean.toString(forceOldSync));
            prop.setProperty("aiEmulation", Boolean.toString(aiEmulation));
            prop.setProperty("hle", Boolean.toString(hle));
            prop.setProperty("mute", Boolean.toString(mute));
            prop.setProperty("bufferDivider", Integer.toString(bufferDivider));
            
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                prop.store(out, "AziAudio Plugin Settings");
            }
        } catch (Exception ignored) {
        }
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
            volume = Math.max(0, Math.min(100, Integer.parseInt(prop.getProperty("volume", "100"))));
            balance = Math.max(-100, Math.min(100, Integer.parseInt(prop.getProperty("balance", "0"))));
            mono = Boolean.parseBoolean(prop.getProperty("mono", "false"));
            swapChannels = Boolean.parseBoolean(prop.getProperty("swapChannels", "false"));
            muteFastForward = Boolean.parseBoolean(prop.getProperty("muteFastForward", "false"));
            interpolation = Math.max(0, Math.min(2, Integer.parseInt(prop.getProperty("interpolation", "0"))));
            syncAudio = Boolean.parseBoolean(prop.getProperty("syncAudio", "true"));
            forceOldSync = Boolean.parseBoolean(prop.getProperty("forceOldSync", "false"));
            aiEmulation = Boolean.parseBoolean(prop.getProperty("aiEmulation", "true"));
            hle = Boolean.parseBoolean(prop.getProperty("hle", "true"));
            mute = Boolean.parseBoolean(prop.getProperty("mute", "false"));
            bufferDivider = Integer.parseInt(prop.getProperty("bufferDivider", "10"));
        } catch (Exception ignored) {
        }
    }
    public static double interpolate(short previous, short first, short second, short next, double fraction) {
        return aziaudio.playback.AudioSamples.interpolate(previous, first, second, next, fraction);
    }

    public static void processOutput(byte[] pcm, int length) {
        aziaudio.playback.AudioSamples.processOutput(pcm, length);
    }

}
