package kernel64gl;

import kernel64gl.engine.config.RendererSettings;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Independent host settings; guest graphics state belongs to the independent Kernel64GL engine. */
public final class Kernel64GLSettings {
    public static final Path FILE = Path.of("Kernel64GL.config");
    public record Advanced(int cacheMiB,int textureDepth,boolean forceBilinear,boolean textures,
            boolean depthTest,boolean culling,boolean cacheTextures,int debugView,int commandLimit,
            boolean pauseOnInvalid,boolean diagnostics,int messageLimit,int batchTriangles) {
        public Advanced {
            if(cacheMiB<16||cacheMiB>256||textureDepth<0||textureDepth>2||debugView<0||debugView>4
                    ||commandLimit<4096||commandLimit>1048576||messageLimit<1||messageLimit>256||batchTriangles<16||batchTriangles>512)
                throw new IllegalArgumentException("advanced renderer option outside supported range");
        }
    }
    public static Advanced advancedDefaults(){return new Advanced(32,1,false,true,true,true,true,0,262144,false,true,64,512);}
    public static Advanced corruptionDefaults(){return new Advanced(32,1,false,true,true,true,true,0,65536,false,false,16,512);}
    public static Advanced inspectionDefaults(){return new Advanced(32,1,false,true,true,false,false,1,65536,true,true,32,256);}
    public record Options(int width, int height, boolean lockWindow, boolean alwaysOnTop,
                          boolean fog, boolean bilinear, boolean wireframe, boolean vsync, Advanced advanced) {
        public Options(int width,int height,boolean lockWindow,boolean alwaysOnTop,boolean fog,boolean bilinear,boolean wireframe,boolean vsync){this(width,height,lockWindow,alwaysOnTop,fog,bilinear,wireframe,vsync,advancedDefaults());}
        public Options {
            if(width<320 || width>3840 || height<240 || height>2160)
                throw new IllegalArgumentException("resolution must be between 320 x 240 and 3840 x 2160");
            java.util.Objects.requireNonNull(advanced);
        }
        public void applyRenderer() {
            RendererSettings.windowWidth=width;RendererSettings.windowHeight=height;
            RendererSettings.lockWindow=lockWindow;RendererSettings.alwaysOnTop=alwaysOnTop;
            RendererSettings.enableFog=fog;RendererSettings.bilinear=bilinear;
            RendererSettings.wireframe=wireframe;
            RendererSettings.cacheMiB=advanced.cacheMiB;RendererSettings.textureDepth=advanced.textureDepth;
            RendererSettings.forceBilinear=bilinear&&advanced.forceBilinear;RendererSettings.textures=advanced.textures;
            RendererSettings.depthTest=advanced.depthTest;RendererSettings.culling=advanced.culling;
            RendererSettings.cacheTextures=advanced.cacheTextures;RendererSettings.debugView=advanced.debugView;
            RendererSettings.commandLimit=advanced.commandLimit;RendererSettings.pauseOnInvalid=advanced.pauseOnInvalid;
            RendererSettings.diagnostics=advanced.diagnostics;RendererSettings.messageLimit=advanced.messageLimit;
            RendererSettings.batchTriangles=advanced.batchTriangles;
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
            flag(p,"bilinear",true),flag(p,"wireframe",false),flag(p,"vsync",false),
            new Advanced(number(p,"cacheMiB",32,16,256),number(p,"textureDepth",1,0,2),flag(p,"forceBilinear",false),
                flag(p,"textures",true),flag(p,"depthTest",true),flag(p,"culling",true),flag(p,"cacheTextures",true),
                number(p,"debugView",0,0,4),number(p,"commandLimit",262144,4096,1048576),flag(p,"pauseOnInvalid",false),
                flag(p,"diagnostics",true),number(p,"messageLimit",64,1,256),number(p,"batchTriangles",512,16,512)));
    }
    public static void save(Options options)throws IOException {
        Properties p=new Properties();
        p.setProperty("width",Integer.toString(options.width()));p.setProperty("height",Integer.toString(options.height()));
        p.setProperty("lockWindow",Boolean.toString(options.lockWindow()));p.setProperty("alwaysOnTop",Boolean.toString(options.alwaysOnTop()));
        p.setProperty("fog",Boolean.toString(options.fog()));p.setProperty("bilinear",Boolean.toString(options.bilinear()));
        p.setProperty("wireframe",Boolean.toString(options.wireframe()));p.setProperty("vsync",Boolean.toString(options.vsync()));
        Advanced a=options.advanced();
        p.setProperty("cacheMiB",Integer.toString(a.cacheMiB));p.setProperty("textureDepth",Integer.toString(a.textureDepth));
        p.setProperty("forceBilinear",Boolean.toString(a.forceBilinear));p.setProperty("textures",Boolean.toString(a.textures));
        p.setProperty("depthTest",Boolean.toString(a.depthTest));p.setProperty("culling",Boolean.toString(a.culling));
        p.setProperty("cacheTextures",Boolean.toString(a.cacheTextures));p.setProperty("debugView",Integer.toString(a.debugView));
        p.setProperty("commandLimit",Integer.toString(a.commandLimit));p.setProperty("pauseOnInvalid",Boolean.toString(a.pauseOnInvalid));
        p.setProperty("diagnostics",Boolean.toString(a.diagnostics));p.setProperty("messageLimit",Integer.toString(a.messageLimit));
        p.setProperty("batchTriangles",Integer.toString(a.batchTriangles));
        Path destination=FILE.toAbsolutePath(),temp=Files.createTempFile(destination.getParent(),"kernel64gl-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Kernel64GL settings");}
            try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
        if(kerneln64.Main.video!=null && kerneln64.Main.video.gfxPlugin instanceof Kernel64GLPlugin plugin)
            plugin.applyLive(options);
    }
    private Kernel64GLSettings() {}
}
