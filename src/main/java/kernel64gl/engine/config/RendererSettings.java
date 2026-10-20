package kernel64gl.engine.config;
/** Runtime settings owned exclusively by Kernel64GL. Persistence lives in Kernel64GLSettings. */
public final class RendererSettings {
 public static boolean enableFog=true,bilinear=true,wireframe=false;
 public static int windowWidth=640,windowHeight=480;
 public static boolean lockWindow=false,alwaysOnTop=false;
 public static void load() {}
 public static void save() {}
}
