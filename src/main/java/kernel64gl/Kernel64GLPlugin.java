package kernel64gl;

import kernel64gl.engine.HlePlugin;

import javax.media.opengl.GL;
import javax.swing.*;
import kerneln64.Main;

/** Independent Java OpenGL renderer with its own decoder, cache and draw state. */
public final class Kernel64GLPlugin extends HlePlugin {
    private Kernel64GLSettings.Options options=Kernel64GLSettings.defaults();
    private int swapInterval=-1;
    private final java.util.concurrent.atomic.AtomicReference<Kernel64GLSettings.Options> pending=new java.util.concurrent.atomic.AtomicReference<>();

    /** Publish from the settings dialog; GL resources change only with their context current. */
    public void applyLive(Kernel64GLSettings.Options next) {
        pending.set(next);
        SwingUtilities.invokeLater(()->{
            if(Main.video==null || Main.video.gfxPlugin!=this || kernel64gl.engine.opengl.OpenGlGdp.hDC==null)return;
            var window=HlePlugin.hWnd;
            var drawable=kernel64gl.engine.opengl.OpenGlGdp.hDC;
            if(window!=null && drawable instanceof java.awt.Component canvas){
                boolean resize=canvas.getWidth()!=next.width() || canvas.getHeight()!=next.height();
                window.setResizable(!next.lockWindow());window.setAlwaysOnTop(next.alwaysOnTop());
                if(resize){canvas.setPreferredSize(new java.awt.Dimension(next.width(),next.height()));window.pack();}
            }
        });
    }

    @Override protected void loadSettings() {options=Kernel64GLSettings.load();options.applyRenderer();}
    @Override public void romOpen() {
        pending.set(null);loadSettings();swapInterval=-1;super.romOpen();
        System.out.println("graphics: Kernel64GL (independent java renderer, GLSL RDP combiner)");
    }
    public static int requestedSwapInterval(boolean vsync,boolean limited,float speed) {
        return vsync && limited && speed<=1?1:0;
    }
    @Override public void configureContext(GL gl,boolean created) {
        if(!created){
            var next=pending.getAndSet(null);
            if(next!=null){
                var gdp=kernel64gl.engine.opengl.OpenGlGdp.cache;
                // Finish any old batch before changing its host drawing policy.
                kernel64gl.engine.opengl.OpenGlGdp.OGL_DrawTriangles();
                boolean rebuild=next.advanced().textureDepth()!=options.advanced().textureDepth()
                    || next.advanced().cacheMiB()!=options.advanced().cacheMiB();
                options=next;next.applyRenderer();
                gdp.config(next.advanced().cacheMiB()*1048576,next.advanced().textureDepth());
                if(rebuild)gdp.invalidateHostTextures();
                // Recompute culling, texture use, filtering and the combiner without resetting guest state.
                kernel64gl.engine.core.Rsp.gsp.changed=-1;
                kernel64gl.engine.core.Rsp.gdp.changed=-1;gdp.changed=-1;
            }
        }
        int interval=requestedSwapInterval(options.vsync(),Main.FrameLimit,Main.SpeedMultiplier);
        if(created || interval!=swapInterval){gl.setSwapInterval(interval);swapInterval=interval;}
    }
    @Override public void pluginConfig(JFrame parent) {new Kernel64GLConfig(parent).setVisible(true);}
    @Override public void pluginAbout(JFrame parent) {
        JOptionPane.showMessageDialog(parent,"Kernel64GL\njava opengl graphics with independent settings.\nindependent decoder, texture cache and GLSL renderer.\nvsync is bypassed during fast mode.\nexperimental; compatibility is scene dependent.","Kernel64GL",JOptionPane.INFORMATION_MESSAGE);
    }
    @Override public void getPluginInfo(PluginInfo info) {
        info.name="Kernel64GL";info.version=0x0103;info.type=PLUGIN_TYPE_GFX;
        info.normalMemory=true;info.memoryBswaped=false;
    }
}
