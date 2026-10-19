package basiccontroller;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import javax.swing.JFrame;
import plugin.ControllerPlugin;

public class BasicKeyboardPlugin implements ControllerPlugin {

    private final java.util.Set<Integer> pressed = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public BasicKeyboardPlugin() {}
    public void releaseAll() { pressed.clear(); }
    public boolean usesKey(int key) {
        if(key==0)return false;
        for(int mapped:KeyboardSettings.current.keys())if(mapped==key)return true;
        return false;
    }
    public void closePlugin() { releaseAll(); }
    public void controllerCommand(int control, ByteBuffer command) {}
    public void pluginAbout(JFrame parent) {}
    public void pluginConfig(JFrame parent) { new KeyboardConfig(parent, this::releaseAll).setVisible(true); }
    public void pluginTest(JFrame parent) {}
    public void getPluginInfo(PluginInfo pluginInfo) {
        pluginInfo.version = 0x0100;
        pluginInfo.type = PLUGIN_TYPE_CONTROLLER;
        pluginInfo.name = "Keyboard Plugin (Arrow Analog)";
    }

    @Override
    public void getKeys(int control, Buttons keys) {
        if (keys == null) return;
        
        KeyboardSettings.Mapping settings = KeyboardSettings.current;
        int[] map = settings.keys();
        int x = ((pressed.contains(map[3]) ? 1 : 0) - (pressed.contains(map[2]) ? 1 : 0)) * settings.strength();
        int y = ((pressed.contains(map[0]) ? 1 : 0) - (pressed.contains(map[1]) ? 1 : 0)) * settings.strength();
        if (settings.circular() && x != 0 && y != 0) { x = (int)Math.round(x / Math.sqrt(2)); y = (int)Math.round(y / Math.sqrt(2)); }
        keys.value = 0;
        int[] masks = { Buttons.L_TRIG, Buttons.R_TRIG, Buttons.U_DPAD, Buttons.D_DPAD,
            Buttons.L_DPAD, Buttons.R_DPAD, Buttons.U_CBUTTON, Buttons.D_CBUTTON,
            Buttons.L_CBUTTON, Buttons.R_CBUTTON, Buttons.START_BUTTON, Buttons.A_BUTTON,
            Buttons.B_BUTTON, Buttons.Z_TRIG };
        for (int i = 0; i < masks.length; i++) if (pressed.contains(map[i + 4])) keys.value |= masks[i];
        keys.setXAxis((byte)x); keys.setYAxis((byte)y);
    }

    @Override
    public void wmKeyDown(int key, int flags) { if (key != 0) pressed.add(key); }
    @Override
    public void wmKeyUp(int key, int flags) { pressed.remove(key); }

    public void initiateControllers(JFrame hMainWindow, Control[] controls) {
        controls[0].present = true;
        controls[0].rawData = false;
        controls[0].plugin = PLUGIN_NONE;
        controls[1].present = false;
        controls[2].present = false;
        controls[3].present = false;
    }

    public void readController(int control, ByteBuffer command) {}
    public void romClosed() { releaseAll(); }
    public void romOpen() { releaseAll(); }
    
}
