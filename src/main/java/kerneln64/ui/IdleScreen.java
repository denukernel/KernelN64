package kerneln64.ui;

import kerneln64.Main;
import java.awt.*;
import javax.media.opengl.*;
import javax.swing.*;

/** Owns a separate idle context, never guest renderer state or emulation timing. */
public final class IdleScreen {
    private static IdleScreen active;
    private final JPanel view=new JPanel(new BorderLayout());
    private GLCanvas canvas;
    private Timer timer;
    private RainbowLogo logo;
    private final long start=System.nanoTime();
    private boolean failed;
    private IdleScreen(){
        view.setBackground(Color.BLACK);view.setPreferredSize(new Dimension(640,480));
        JLabel hint=new JLabel("open a rom to begin",SwingConstants.CENTER);hint.setForeground(new Color(155,166,184));hint.setBorder(BorderFactory.createEmptyBorder(8,8,16,8));view.add(hint,BorderLayout.SOUTH);
        try{
            kernel64gl.engine.opengl.NativeLibraries.install();
            logo=new RainbowLogo();GLCapabilities capabilities=new GLCapabilities();capabilities.setDepthBits(24);capabilities.setDoubleBuffered(true);
            canvas=new GLCanvas(capabilities);canvas.setBackground(Color.BLACK);
            canvas.addGLEventListener(new GLEventListener(){
                public void init(GLAutoDrawable drawable){drawable.getGL().setSwapInterval(0);}
                public void display(GLAutoDrawable drawable){logo.render(drawable.getGL(),drawable.getWidth(),drawable.getHeight(),(System.nanoTime()-start)/1e9);}
                public void reshape(GLAutoDrawable d,int x,int y,int w,int h){}
                public void displayChanged(GLAutoDrawable d,boolean mode,boolean device){}
            });
            view.add(canvas,BorderLayout.CENTER);
            timer=new Timer(33,e->{
                if(active!=this||failed||canvas==null||!canvas.isShowing()||!Main.hMainWindow.isActive()||(Main.hMainWindow.getExtendedState()&Frame.ICONIFIED)!=0)return;
                try{canvas.display();}catch(RuntimeException|LinkageError error){fallback(error);}
            });
        }catch(RuntimeException|LinkageError error){fallback(error);}
    }
    private void fallback(Throwable error){
        if(failed)return;failed=true;if(timer!=null)timer.stop();
        System.err.println("idle opengl logo unavailable: "+error.getMessage());
        if(canvas!=null){view.remove(canvas);canvas=null;}
        JLabel label=new JLabel("KernelN64",SwingConstants.CENTER);label.setFont(new Font(Font.SANS_SERIF,Font.BOLD,42));label.setForeground(new Color(0,190,245));view.add(label,BorderLayout.CENTER);view.revalidate();view.repaint();
    }
    private static void edt(Runnable action){
        if(SwingUtilities.isEventDispatchThread()){action.run();return;}
        try{SwingUtilities.invokeAndWait(action);}catch(Exception e){throw new IllegalStateException("idle screen window update failed",e);}
    }
    public static void show(){edt(()->{
        if(Main.hMainWindow==null||Main.currentRomFile!=null||active!=null)return;
        IdleScreen next=new IdleScreen();active=next;
        var content=Main.hMainWindow.getContentPane();content.removeAll();content.setLayout(new BorderLayout());content.add(next.view,BorderLayout.CENTER);
        if(Main.emulationStatus!=null)content.add(Main.emulationStatus,BorderLayout.SOUTH);
        content.revalidate();content.repaint();if(next.timer!=null&&!next.failed)next.timer.start();
    });}
    public static void hide(){edt(()->{
        IdleScreen old=active;active=null;if(old==null)return;
        if(old.timer!=null)old.timer.stop();
        if(old.canvas!=null){old.view.remove(old.canvas);old.canvas=null;}
        if(old.view.getParent()!=null)old.view.getParent().remove(old.view);
        if(Main.hMainWindow!=null){Main.hMainWindow.getContentPane().revalidate();Main.hMainWindow.getContentPane().repaint();}
    });}
}
