package d3d11.config;

import java.awt.*;
import java.io.*;
import java.util.Properties;
import javax.swing.*;

public final class D3D11Settings {
    public static volatile int width=640,height=480;
    public static int filter=0;
    public static volatile boolean matchWindow=false;
    public static boolean locked=false,vsync=false,warp=false;
    private static final File FILE=new File("D3D11.config");
    static { if(FILE.isFile())try(var input=new FileInputStream(FILE)) {
        Properties p=new Properties();p.load(input);width=Math.max(320,Math.min(3840,Integer.parseInt(p.getProperty("width","640"))));height=Math.max(240,Math.min(2160,Integer.parseInt(p.getProperty("height","480"))));
        matchWindow=Boolean.parseBoolean(p.getProperty("matchWindow","false"));locked=Boolean.parseBoolean(p.getProperty("locked","false"));vsync=Boolean.parseBoolean(p.getProperty("vsync","false"));warp=Boolean.parseBoolean(p.getProperty("warp","false"));filter=Math.max(0,Math.min(2,Integer.parseInt(p.getProperty("filter","0"))));
    }catch(Exception error){System.err.println("Cannot read D3D11 settings: "+error);} }
    public static void show(JFrame owner) {
        JPanel panel=new JPanel(new GridLayout(0,2,8,8));
        JSpinner w=new JSpinner(new SpinnerNumberModel(width,320,3840,80)),h=new JSpinner(new SpinnerNumberModel(height,240,2160,60));
        JCheckBox lock=new JCheckBox("Lock window size",locked),sync=new JCheckBox("VSync",vsync),software=new JCheckBox("Use WARP software device",warp);
        JCheckBox nativeSize=new JCheckBox("Render at window resolution (slower)",matchWindow);
        JComboBox<String> filtering=new JComboBox<>(new String[]{"Game controlled","Force bilinear","Force nearest"});filtering.setSelectedIndex(filter);
        panel.add(new JLabel("Render width:"));panel.add(w);panel.add(new JLabel("Render height:"));panel.add(h);panel.add(lock);panel.add(sync);panel.add(software);panel.add(nativeSize);
        panel.add(new JLabel("Texture filtering:"));panel.add(filtering);
        panel.add(new JLabel("Experimental N64 D3D11 renderer."));panel.add(new JLabel("Resolution and filtering apply live. Device changes need restart."));
        if(JOptionPane.showConfirmDialog(owner,panel,"Direct3D 11 Settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            matchWindow=nativeSize.isSelected();width=((Number)w.getValue()).intValue();height=((Number)h.getValue()).intValue();locked=lock.isSelected();vsync=sync.isSelected();warp=software.isSelected();filter=filtering.getSelectedIndex();
            Properties p=new Properties();p.setProperty("matchWindow",""+matchWindow);p.setProperty("width",""+width);p.setProperty("height",""+height);p.setProperty("locked",""+locked);p.setProperty("vsync",""+vsync);p.setProperty("warp",""+warp);p.setProperty("filter",""+filter);
            try(var output=new FileOutputStream(FILE)){p.store(output,"KernelN64 Direct3D 11 settings");}catch(IOException error){JOptionPane.showMessageDialog(owner,error.getMessage());}
        }
    }
}
