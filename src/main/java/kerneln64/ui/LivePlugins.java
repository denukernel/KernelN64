package kerneln64.ui;
import kerneln64.cpu.Cpu;
import kerneln64.state.Rewind;
import kerneln64.state.SaveStates;
import kerneln64.Main;


import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import plugin.*;

/** Replaces host plugins at an instruction boundary without resetting guest hardware. */
public final class LivePlugins {
    private LivePlugins() {}
    public static CompletableFuture<Void> apply(String[] choices) {
        String[] requested=choices.clone();
        CompletableFuture<Void> completion=new CompletableFuture<>();
        // Resolve classes before closing anything. Constructors can own static resources,
        // so instantiate only after the old backend has been closed on the CPU thread.
        try {
            for(int i=0;i<3;i++) {
                Class<?> type=Class.forName(requested[i]);
                Class<?> expected=i==0?GfxPlugin.class:i==1?AudioPlugin.class:ControllerPlugin.class;
                if(!expected.isAssignableFrom(type))throw new IOException("Invalid plugin: "+requested[i]);
                type.getDeclaredConstructor();
            }
        } catch(Exception error){completion.completeExceptionally(error);return completion;}
        if(Main.cpu==null || !Main.cpu.running) {
            for(int i=0;i<3;i++)Main.setPluginSetting(PluginSettings.KEYS[i],requested[i]);
            Main.saveConfig();completion.complete(null);return completion;
        }
        Cpu cpu=Main.cpu;
        var command=cpu.submitCommand(()->{
            try {
                for(int i=0;i<3;i++) {
                    Object current=i==0?Main.video.gfxPlugin:i==1?Main.audio.audioPlugin:Main.pif.controllerPlugin;
                    if(current!=null && current.getClass().getName().equals(requested[i]))continue;
                    replace(i,requested[i]);
                    Main.setPluginSetting(PluginSettings.KEYS[i],requested[i]);
                }
                Rewind.clear();Main.video.resetTimer();Main.saveConfig();completion.complete(null);
            } catch(Exception error){Main.saveConfig();completion.completeExceptionally(error);}
        });
        // A queued command can be cancelled when a ROM is closed before it executes.
        CompletableFuture.runAsync(()->{try{command.get();}catch(Exception error){completion.completeExceptionally(error);}});
        return completion;
    }
    private static void replace(int type,String name)throws Exception {
        String old=type==0?Main.video.gfxPlugin.getClass().getName():type==1?Main.audio.audioPlugin.getClass().getName():Main.pif.controllerPlugin.getClass().getName();
        Map<String,byte[]> graphics=null;long mux=0;
        float[] environment=null,primitive=null;int minimum=0;
        if(type==0) {
            d3d11.framebuffer.D3DFramebuffers.capture();graphics=SaveStates.graphicsState();
            var combiner=gln64j.opengl.OpenGlGdp.combiners; mux=combiner.combine.getMux();
            environment=gln64j.rdp.combiners.Combiners.envColor.clone();
            var p=gln64j.rdp.combiners.Combiners.primColor;primitive=new float[]{p.r,p.g,p.b,p.a,p.l};minimum=p.m;
        }
        try { close(type);open(type,name); }
        catch(Exception | LinkageError error) {
            try{close(type);open(type,old);}catch(Exception rollback){error.addSuppressed(rollback);Main.cpu.running=false;}
            if(type==0 && graphics!=null)restoreGraphics(graphics,mux,environment,primitive,minimum);
            Main.rsp.setupPlugin(Main.hMainWindow,new Properties(),Main.video.gfxPlugin,Main.audio.audioPlugin);
            throw error;
        }
        if(type==0)restoreGraphics(graphics,mux,environment,primitive,minimum);
        Main.rsp.setupPlugin(Main.hMainWindow,new Properties(),Main.video.gfxPlugin,Main.audio.audioPlugin);
    }
    private static void restoreGraphics(Map<String,byte[]> graphics,long mux,float[] environment,float[] primitive,int minimum)throws IOException {
        SaveStates.graphicsState(graphics);
        gln64j.opengl.OpenGlGdp.combiners.combine.setMuxs0((int)(mux>>>32));gln64j.opengl.OpenGlGdp.combiners.combine.setMuxs1((int)mux);
        System.arraycopy(environment,0,gln64j.rdp.combiners.Combiners.envColor,0,4);
        var p=gln64j.rdp.combiners.Combiners.primColor;p.r=primitive[0];p.g=primitive[1];p.b=primitive[2];p.a=primitive[3];p.l=primitive[4];p.m=minimum;
    }
    private static void close(int type) {
        if(type==0){Main.video.gfxPlugin.romClosed();Main.video.gfxPlugin.closePlugin();}
        else if(type==1){Main.audio.audioPlugin.romClosed();Main.audio.audioPlugin.closePlugin();}
        else {Main.pif.controllerPlugin.romClosed();Main.pif.controllerPlugin.closePlugin();}
    }
    private static void open(int type,String name)throws IOException {
        Properties config=new Properties();config.setProperty(PluginSettings.KEYS[type],name);
        if(type==0) {
            if(!Main.video.setupPlugin(Main.hMainWindow,config))throw new IOException("Cannot initialize graphics plugin "+name);
            Main.video.gfxPlugin.romOpen();
        } else if(type==1) {
            if(!Main.audio.setupPlugin(Main.hMainWindow,config))throw new IOException("Cannot initialize audio plugin "+name);
            Main.audio.audioPlugin.aiDacrateChanged(AudioPlugin.SYSTEM_NTSC);
        } else {
            if(!Main.pif.setupPlugin(Main.hMainWindow,config))throw new IOException("Cannot initialize input plugin "+name);
            Main.pif.controllerPlugin.romOpen();
        }
    }
}
