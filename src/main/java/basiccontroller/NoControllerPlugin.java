package basiccontroller;
import javax.swing.JFrame;
/** An explicit disconnected-controller choice that can be applied live. */
public final class NoControllerPlugin extends BasicKeyboardPlugin {
    @Override public boolean usesKey(int key){return false;}
    @Override public boolean usesKey(int key,int location){return false;}
    @Override public void initiateControllers(JFrame window,Control[] controls){for(Control c:controls){c.present=false;c.rawData=false;c.plugin=PLUGIN_NONE;}}
    @Override public void getKeys(int control,Buttons keys){if(keys!=null)keys.value=0;}
    @Override public void wmKeyDown(int key,int flags){}
    @Override public void wmKeyUp(int key,int flags){}
    @Override public void pluginConfig(JFrame window){javax.swing.JOptionPane.showMessageDialog(window,"All controllers are disconnected.","Input Plugin",javax.swing.JOptionPane.INFORMATION_MESSAGE);}
}
