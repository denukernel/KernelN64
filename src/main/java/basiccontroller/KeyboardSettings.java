package basiccontroller;

import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class KeyboardSettings {
    public static final int COUNT=18;
    public record Mapping(int[] keys,int strength,boolean circular,boolean invertX,boolean invertY,
            boolean slowEnabled,int slowKey,int slowStrength,boolean lastWins,boolean distinguishSides,boolean allowDuplicates) {
        public Mapping(int[] keys,int strength,boolean circular){this(keys,strength,circular,false,false,false,KeyEvent.VK_SHIFT,40,false,false,false);}
        public Mapping {
            if(keys==null||keys.length!=COUNT||strength<20||strength>127||slowStrength<1||slowStrength>127||!valid(slowKey))
                throw new IllegalArgumentException("invalid keyboard controller mapping");
            keys=keys.clone();for(int key:keys)if(!valid(key))throw new IllegalArgumentException("invalid key binding");
        }
        @Override public int[] keys(){return keys.clone();}
    }
    public static volatile Mapping current=defaults();
    private static final Path FILE=Path.of("BasicKeyboard.config");
    static {load();}
    public static boolean valid(int key){return key==0||key>0&&(key&0xff000000)==0&&(key>>>16)<=KeyEvent.KEY_LOCATION_NUMPAD&&(key&0xffff)!=0;}
    public static int code(int binding){return binding&0xffff;}
    public static int location(int binding){return binding>>>16;}
    public static int binding(int code,int location){return (code&0xffff)|(Math.max(0,Math.min(4,location))<<16);}
    public static boolean overlaps(int a,int b){return a!=0&&b!=0&&code(a)==code(b)&&(location(a)==0||location(b)==0||location(a)==location(b));}
    public static String keyName(int binding){
        if(binding==0)return "unassigned";
        int code=code(binding);
        String name=switch(code){
            case KeyEvent.VK_UP->"up arrow";case KeyEvent.VK_DOWN->"down arrow";
            case KeyEvent.VK_LEFT->"left arrow";case KeyEvent.VK_RIGHT->"right arrow";
            case KeyEvent.VK_CONTROL->"ctrl";case KeyEvent.VK_SHIFT->"shift";case KeyEvent.VK_ALT->"alt";
            case KeyEvent.VK_ALT_GRAPH->"alt gr";case KeyEvent.VK_META->"meta";case KeyEvent.VK_ENTER->"enter";
            case KeyEvent.VK_TAB->"tab";case KeyEvent.VK_SPACE->"space";case KeyEvent.VK_ESCAPE->"escape";
            case KeyEvent.VK_BACK_SPACE->"backspace";case KeyEvent.VK_DELETE->"delete";
            default->KeyEvent.getKeyText(code).toLowerCase(java.util.Locale.ROOT);
        };
        return switch(location(binding)){case KeyEvent.KEY_LOCATION_LEFT->"left "+name;case KeyEvent.KEY_LOCATION_RIGHT->"right "+name;case KeyEvent.KEY_LOCATION_NUMPAD->"numpad "+name;default->name;};
    }
    public static Mapping defaults(){return new Mapping(new int[]{KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,
        KeyEvent.VK_Q,KeyEvent.VK_CONTROL,KeyEvent.VK_I,KeyEvent.VK_K,KeyEvent.VK_J,KeyEvent.VK_L,
        KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_ENTER,KeyEvent.VK_X,KeyEvent.VK_C,KeyEvent.VK_Z},80,false);}
    public static void save(Mapping settings)throws IOException{
        Properties p=new Properties();int[] keys=settings.keys();
        for(int i=0;i<keys.length;i++)p.setProperty("key."+i,Integer.toString(keys[i]));
        p.setProperty("strength",""+settings.strength());p.setProperty("circular",""+settings.circular());
        p.setProperty("invertX",""+settings.invertX());p.setProperty("invertY",""+settings.invertY());
        p.setProperty("slowEnabled",""+settings.slowEnabled());p.setProperty("slowKey",""+settings.slowKey());p.setProperty("slowStrength",""+settings.slowStrength());
        p.setProperty("lastWins",""+settings.lastWins());p.setProperty("distinguishSides",""+settings.distinguishSides());p.setProperty("allowDuplicates",""+settings.allowDuplicates());
        Path dest=FILE.toAbsolutePath(),temp=Files.createTempFile(dest.getParent(),"keyboard-",".tmp");
        try{
            try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"KernelN64 keyboard controller settings");}
            try{Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,dest,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
        if(kerneln64.Main.pif!=null&&kerneln64.Main.pif.controllerPlugin instanceof BasicKeyboardPlugin keyboard)keyboard.applySettings(settings);
        else current=settings;
    }
    private static int number(Properties p,String key,int fallback,int min,int max){try{return Math.max(min,Math.min(max,Integer.parseInt(p.getProperty(key,""))));}catch(NumberFormatException e){return fallback;}}
    private static int key(Properties p,String name,int fallback){int k=number(p,name,fallback,0,Integer.MAX_VALUE);return valid(k)?k:fallback;}
    private static boolean flag(Properties p,String key,boolean fallback){String v=p.getProperty(key,"");return v.equalsIgnoreCase("true")?true:v.equalsIgnoreCase("false")?false:fallback;}
    static void load(){
        if(!Files.isRegularFile(FILE))return;
        try(InputStream in=Files.newInputStream(FILE)){
            Properties p=new Properties();p.load(in);int[] keys=defaults().keys();for(int i=0;i<keys.length;i++)keys[i]=key(p,"key."+i,keys[i]);
            current=new Mapping(keys,number(p,"strength",80,20,127),flag(p,"circular",false),flag(p,"invertX",false),flag(p,"invertY",false),
                flag(p,"slowEnabled",false),key(p,"slowKey",KeyEvent.VK_SHIFT),number(p,"slowStrength",40,1,127),flag(p,"lastWins",false),flag(p,"distinguishSides",false),flag(p,"allowDuplicates",false));
        }catch(IOException|IllegalArgumentException ignored){}
    }
    private KeyboardSettings(){}
}
