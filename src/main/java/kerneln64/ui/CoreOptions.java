package kerneln64.ui;
import kerneln64.Main;


import java.util.Properties;

/** Host preferences; these are not guest state and are never restored from a state. */
public final class CoreOptions {
    public static final Integer[] RAM_MB = {4, 8, 16, 32, 64};
    public static int ramMb = 8;
    public static boolean hideAdvanced, debuggerEnabled = true, pauseOnStart;
    public static volatile boolean pauseOnAudioHandshake;
    public static volatile boolean pauseOnKernelFault = true;
    public static volatile boolean pauseWhenInactive;
    public static void load(Properties p) {
        try { int value = Integer.parseInt(p.getProperty("rdram.mb", "8")); ramMb = java.util.Arrays.asList(RAM_MB).contains(value) ? value : 8; }
        catch (NumberFormatException error) { ramMb = 8; }
        hideAdvanced = Boolean.parseBoolean(p.getProperty("settings.hideAdvanced", "false"));
        debuggerEnabled = Boolean.parseBoolean(p.getProperty("debugger.enabled", "true"));
        pauseOnStart = Boolean.parseBoolean(p.getProperty("debugger.pauseOnStart", "false"));
        pauseOnAudioHandshake = Boolean.parseBoolean(p.getProperty("monitor.pauseOnAudioHandshake", "false"));
        pauseOnKernelFault = Boolean.parseBoolean(p.getProperty("monitor.pauseOnKernelFault", "true"));
        pauseWhenInactive = Boolean.parseBoolean(p.getProperty("emulation.pauseWhenInactive", "false"));
        Main.ShowUnhandledMemory = Boolean.parseBoolean(p.getProperty("debug.memoryErrors", "true"));
        Main.ShowPifRamErrors = Boolean.parseBoolean(p.getProperty("debug.pifErrors", "true"));
        Main.ShowTLBMisses = Boolean.parseBoolean(p.getProperty("debug.tlbMisses", "false"));
    }
    public static void save(Properties p) {
        p.setProperty("rdram.mb", Integer.toString(ramMb));
        p.setProperty("settings.hideAdvanced", Boolean.toString(hideAdvanced));
        p.setProperty("debugger.enabled", Boolean.toString(debuggerEnabled));
        p.setProperty("debugger.pauseOnStart", Boolean.toString(pauseOnStart));
        p.setProperty("monitor.pauseOnAudioHandshake", Boolean.toString(pauseOnAudioHandshake));
        p.setProperty("monitor.pauseOnKernelFault", Boolean.toString(pauseOnKernelFault));
        p.setProperty("emulation.pauseWhenInactive", Boolean.toString(pauseWhenInactive));
        p.setProperty("debug.memoryErrors", Boolean.toString(Main.ShowUnhandledMemory));
        p.setProperty("debug.pifErrors", Boolean.toString(Main.ShowPifRamErrors));
        p.setProperty("debug.tlbMisses", Boolean.toString(Main.ShowTLBMisses));
    }
}
