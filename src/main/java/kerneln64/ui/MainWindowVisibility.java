package kerneln64.ui;

import kerneln64.Main;
import java.awt.*;
import javax.swing.*;

/** Restore the interactive window independently of launcher console settings. */
public final class MainWindowVisibility {
    private MainWindowVisibility(){}
    public static void showInitial(){
        Runnable show=()->show(true);
        if(SwingUtilities.isEventDispatchThread())show.run();
        else try{SwingUtilities.invokeAndWait(show);}catch(Exception error){throw new IllegalStateException("cannot show emulator window",error);}
    }
    public static void restore(){
        if(SwingUtilities.isEventDispatchThread())show(false);
        else SwingUtilities.invokeLater(()->show(false));
    }
    private static void show(boolean center){
        JFrame window=Main.hMainWindow;if(window==null)return;
        if(window.getWidth()<=0||window.getHeight()<=0)window.pack();
        window.setExtendedState(window.getExtendedState()&~Frame.ICONIFIED);
        if(center||!onScreen(window.getBounds())){
            window.setExtendedState(Frame.NORMAL);window.setLocationRelativeTo(null);
        }
        window.setVisible(true);
        window.toFront();window.requestFocus();
    }
    static boolean onScreen(Rectangle bounds){
        for(GraphicsDevice device:GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()){
            Rectangle visible=bounds.intersection(device.getDefaultConfiguration().getBounds());
            if(visible.width>=Math.min(120,bounds.width)&&visible.height>=Math.min(80,bounds.height))return true;
        }
        return false;
    }
}
