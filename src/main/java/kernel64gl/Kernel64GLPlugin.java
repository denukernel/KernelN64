package kernel64gl;

import kernel64gl.engine.HlePlugin;

import javax.media.opengl.GL;
import javax.swing.*;
import kerneln64.Main;

/** Independent Java OpenGL renderer with its own decoder, cache and draw state. */
public final class Kernel64GLPlugin extends HlePlugin {
    private Kernel64GLSettings.Options options=Kernel64GLSettings.defaults();
    private int swapInterval=-1;

    @Override protected void loadSettings() {options=Kernel64GLSettings.load();options.applyRenderer();}
    @Override public void romOpen() {
        loadSettings();swapInterval=-1;super.romOpen();
        System.out.println("graphics: Kernel64GL (independent java renderer, GLSL RDP combiner)");
    }
    public static int requestedSwapInterval(boolean vsync,boolean limited,float speed) {
        return vsync && limited && speed<=1?1:0;
    }
    @Override public void configureContext(GL gl,boolean created) {
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
