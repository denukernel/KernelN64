package kernel64gl;

import kernel64gl.engine.config.RendererSettings;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Independent host settings; guest graphics state belongs to the independent Kernel64GL engine. */
public final class Kernel64GLSettings {
    public static final Path FILE = Path.of("Kernel64GL.config");
    public record Options(int width, int height, boolean lockWindow, boolean alwaysOnTop,
                          boolean fog, boolean bilinear, boolean wireframe, boolean vsync) {
        public Options {
            if(width<320 || width>3840 || height<240 || height>2160)
                throw new IllegalArgumentException("resolution must be between 320 x 240 and 3840 x 2160");
        }
        public void applyRenderer() {
            RendererSettings.windowWidth=width;RendererSettings.windowHeight=height;
            RendererSettings.lockWindow=lockWindow;RendererSettings.alwaysOnTop=alwaysOnTop;
            RendererSettings.enableFog=fog;RendererSettings.bilinear=bilinear;
            RendererSettings.wireframe=wireframe;
        }
    }
    public static Options defaults() { return new Options(640,480,false,false,true,true,false,false); }
    private static int number(Properties p,String key,int fallback,int min,int max) {
        try{return Math.max(min,Math.min(max,Integer.parseInt(p.getProperty(key,""))));}
        catch(NumberFormatException invalid){return fallback;}
    }
    private static boolean flag(Properties p,String key,boolean fallback) {
        String value=p.getProperty(key,"");
        return value.equalsIgnoreCase("true")?true:value.equalsIgnoreCase("false")?false:fallback;
    }
    public static Options load() {
        Properties p=new Properties();
        if(Files.exists(FILE))try(InputStream in=Files.newInputStream(FILE)){p.load(in);}
        catch(IOException | IllegalArgumentException error){System.err.println("Kernel64GL settings: "+error.getMessage());}
        return new Options(number(p,"width",640,320,3840),number(p,"height",480,240,2160),
            flag(p,"lockWindow",false),flag(p,"alwaysOnTop",false),flag(p,"fog",true),
            flag(p,"bilinear",true),flag(p,"wireframe",false),flag(p,"vsync",false));
    }
    public static void save(Options options)throws IOException {
        Properties p=new Properties();
        p.setProperty("width",Integer.toString(options.width()));p.setProperty("height",Integer.toString(options.height()));
        p.setProperty("lockWindow",Boolean.toString(options.lockWindow()));p.setProperty("alwaysOnTop",Boolean.toString(options.alwaysOnTop()));
        p.setProperty("fog",Boolean.toString(options.fog()));p.setProperty("bilinear",Boolean.toString(options.bilinear()));
        p.setProperty("wireframe",Boolean.toString(options.wireframe()));p.setProperty("vsync",Boolean.toString(options.vsync()));
        Path destination=FILE.toAbsolutePath(),temp=Files.createTempFile(destination.getParent(),"kernel64gl-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Kernel64GL settings");}
            try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
    private Kernel64GLSettings() {}
}
