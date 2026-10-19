package kerneln64.ui;
import kerneln64.Main;


import java.util.Properties;

/** Host pacing preference; guest clocks and savestates do not contain this setting. */
public final class FastForwardOptions {
    static final int MIN_VI = 120, MAX_VI = 1200;
    private static volatile int targetVi = MIN_VI;
    public static int targetVi(){return targetVi;}
    public static float multiplier(){return targetVi/60.0f;}
    public static void setTargetVi(int value){
        if(value<MIN_VI || value>MAX_VI)throw new IllegalArgumentException("Choose 120–1200 VI/s.");
        targetVi=value;
        if(Main.isFastForward && !Main.shiftHeld)Main.setFastForward(true);
    }
    public static void load(Properties config){
        int value=MIN_VI;
        try{value=Integer.parseInt(config.getProperty("fastForward.vi","120"));}catch(NumberFormatException ignored){}
        targetVi=value>=MIN_VI && value<=MAX_VI?value:MIN_VI;
    }
    public static void save(Properties config){config.setProperty("fastForward.vi",Integer.toString(targetVi));}
}
