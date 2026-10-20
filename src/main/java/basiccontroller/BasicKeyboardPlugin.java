package basiccontroller;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.JFrame;
import plugin.ControllerPlugin;

public class BasicKeyboardPlugin implements ControllerPlugin {
    // Physical key locations remain separate even when a binding accepts either side.
    private final Map<Integer,Long> pressed=new HashMap<>();
    private long sequence;
    private static final int[] MASKS={Buttons.L_TRIG,Buttons.R_TRIG,Buttons.U_DPAD,Buttons.D_DPAD,Buttons.L_DPAD,Buttons.R_DPAD,
        Buttons.U_CBUTTON,Buttons.D_CBUTTON,Buttons.L_CBUTTON,Buttons.R_CBUTTON,Buttons.START_BUTTON,Buttons.A_BUTTON,Buttons.B_BUTTON,Buttons.Z_TRIG};
    public synchronized void releaseAll(){pressed.clear();}
    public synchronized void applySettings(KeyboardSettings.Mapping settings){pressed.clear();KeyboardSettings.current=settings;}
    public boolean usesKey(int key){return usesKey(key,0);}
    public boolean usesKey(int key,int location){
        if(key==0)return false;
        var settings=KeyboardSettings.current;int physical=KeyboardSettings.binding(key,location);
        for(int mapped:settings.keys())if(KeyboardSettings.overlaps(mapped,physical))return true;
        return settings.slowEnabled()&&KeyboardSettings.overlaps(settings.slowKey(),physical);
    }
    private long down(int binding){
        if(binding==0)return 0;
        long latest=0;for(var p:pressed.entrySet())if(KeyboardSettings.overlaps(binding,p.getKey()))latest=Math.max(latest,p.getValue());return latest;
    }
    private int axis(int positive,int negative,boolean lastWins){
        long p=down(positive),n=down(negative);
        if(p==0)return n==0?0:-1;if(n==0)return 1;
        return lastWins?Long.compare(p,n):0;
    }
    public synchronized boolean controllerModifierDown(){
        var settings=KeyboardSettings.current;
        int[] keys=settings.keys();
        for(int binding:keys)if(modifier(KeyboardSettings.code(binding))&&down(binding)!=0)return true;
        return settings.slowEnabled()&&modifier(KeyboardSettings.code(settings.slowKey()))&&down(settings.slowKey())!=0;
    }
    private static boolean modifier(int code){return code==KeyEvent.VK_CONTROL||code==KeyEvent.VK_SHIFT||code==KeyEvent.VK_ALT||code==KeyEvent.VK_ALT_GRAPH||code==KeyEvent.VK_META;}
    public void closePlugin(){releaseAll();}
    public void controllerCommand(int control,ByteBuffer command){}
    public void pluginAbout(JFrame parent){javax.swing.JOptionPane.showMessageDialog(parent,"KernelN64 keyboard controller\nbasic bindings and live advanced input settings.","KernelN64 controller",javax.swing.JOptionPane.INFORMATION_MESSAGE);}
    public void pluginConfig(JFrame parent){new KeyboardConfig(parent,this::releaseAll).setVisible(true);}
    public void pluginTest(JFrame parent){pluginConfig(parent);}
    public void getPluginInfo(PluginInfo info){info.version=0x0100;info.type=PLUGIN_TYPE_CONTROLLER;info.name="Keyboard Plugin (Arrow Analog)";}
    @Override public synchronized void getKeys(int control,Buttons keys){
        if(keys==null)return;keys.value=0;if(control!=0)return;
        readMapping(KeyboardSettings.current,keys);
    }
    /** Used by the isolated settings preview as well as the game-facing port. */
    public synchronized void readMapping(KeyboardSettings.Mapping settings,Buttons keys){
        int[] map=settings.keys();
        int strength=settings.slowEnabled()&&down(settings.slowKey())!=0?Math.min(settings.strength(),settings.slowStrength()):settings.strength();
        int x=axis(map[3],map[2],settings.lastWins())*strength,y=axis(map[0],map[1],settings.lastWins())*strength;
        if(settings.invertX())x=-x;if(settings.invertY())y=-y;
        if(settings.circular()&&x!=0&&y!=0){x=(int)Math.round(x/Math.sqrt(2));y=(int)Math.round(y/Math.sqrt(2));}
        keys.value=0;for(int i=0;i<MASKS.length;i++)if(down(map[i+4])!=0)keys.value|=MASKS[i];
        keys.setXAxis((byte)x);keys.setYAxis((byte)y);
    }
    @Override public synchronized void wmKeyDown(int key,int flags){if(key!=0)pressed.putIfAbsent(KeyboardSettings.binding(key,flags),++sequence);}
    @Override public synchronized void wmKeyUp(int key,int flags){
        if(flags==KeyEvent.KEY_LOCATION_UNKNOWN)pressed.keySet().removeIf(k->KeyboardSettings.code(k)==key);
        else {pressed.remove(KeyboardSettings.binding(key,flags));pressed.remove(KeyboardSettings.binding(key,0));}
    }
    public void initiateControllers(JFrame window,Control[] controls){
        for(int i=0;i<controls.length;i++){controls[i].present=i==0;controls[i].rawData=false;controls[i].plugin=PLUGIN_NONE;}
    }
    public void readController(int control,ByteBuffer command){}
    public void romClosed(){releaseAll();}public void romOpen(){releaseAll();}
}
