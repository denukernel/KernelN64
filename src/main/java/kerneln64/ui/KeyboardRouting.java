package kerneln64.ui;
import kerneln64.Main;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import basiccontroller.BasicKeyboardPlugin;

/** Runs before Swing accelerators: a game controller chord belongs to the game. */
public final class KeyboardRouting {
    private static boolean installed;
    private KeyboardRouting() {}
    static void install() {
        if(installed)return;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(KeyboardRouting::dispatch);
        installed=true;
    }
    public static boolean dispatch(KeyEvent event) {
        Component source=event.getComponent();
        if(Main.cpu==null || !Main.cpu.running || Main.hMainWindow==null || Main.pif==null
                || !(source instanceof Canvas || source==Main.hMainWindow)
                || (source!=Main.hMainWindow && SwingUtilities.getWindowAncestor(source)!=Main.hMainWindow)
                || MenuSelectionManager.defaultManager().getSelectedPath().length!=0)return false;
        BasicKeyboardPlugin keyboard=Main.pif.controllerPlugin instanceof BasicKeyboardPlugin pad?pad:null;
        if(keyboard==null)return selectSlot(event);
        boolean mapped=keyboard.usesKey(event.getKeyCode());
        // Releases for an unassigned hotkey must still reach its listener even
        // if the player pressed a controller modifier in the meantime.
        if(event.getID()==KeyEvent.KEY_RELEASED && !mapped)return false;
        boolean controllerModifier=(event.isControlDown() && keyboard.usesKey(KeyEvent.VK_CONTROL))
                || (event.isShiftDown() && keyboard.usesKey(KeyEvent.VK_SHIFT))
                || (event.isAltDown() && keyboard.usesKey(KeyEvent.VK_ALT))
                || (event.isAltGraphDown() && keyboard.usesKey(KeyEvent.VK_ALT_GRAPH))
                || (event.isMetaDown() && keyboard.usesKey(KeyEvent.VK_META));
        if(!mapped && !controllerModifier)return selectSlot(event);
        if(mapped) {
            if(event.getID()==KeyEvent.KEY_PRESSED)keyboard.wmKeyDown(event.getKeyCode(),0);
            else if(event.getID()==KeyEvent.KEY_RELEASED)keyboard.wmKeyUp(event.getKeyCode(),0);
        }
        event.consume();return true;
    }
    private static boolean selectSlot(KeyEvent event) {
        if(event.getID()!=KeyEvent.KEY_PRESSED || event.getModifiersEx()!=0
                || !(Main.hMainWindow.getJMenuBar() instanceof MenuBar menu))return false;
        int key=event.getKeyCode();
        int slot=key>=KeyEvent.VK_1 && key<=KeyEvent.VK_9?key-KeyEvent.VK_0:
                key>=KeyEvent.VK_NUMPAD1 && key<=KeyEvent.VK_NUMPAD9?key-KeyEvent.VK_NUMPAD0:0;
        if(slot==0)return false;
        menu.selectStateSlot(slot);event.consume();return true;
    }
    public static void attach(Component canvas) {
        if(Main.hMainWindow.getKeyListeners().length>0)canvas.addKeyListener(Main.hMainWindow.getKeyListeners()[0]);
        canvas.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){
            release();
        }});
    }
    public static void release() {
        Main.releaseTemporaryKeys();
        if(Main.pif!=null && Main.pif.controllerPlugin instanceof BasicKeyboardPlugin keyboard)keyboard.releaseAll();
    }
}
