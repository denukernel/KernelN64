package kernel64gl;

import java.awt.*;
import javax.swing.*;

/** Settings are persisted on save and applied on the next ROM open. */
public final class Kernel64GLConfig extends JDialog {
    public Kernel64GLConfig(JFrame parent) {
        super(parent,"Kernel64GL settings",true);
        var current=Kernel64GLSettings.load();
        JPanel page=new JPanel(new GridLayout(0,2,10,10));
        page.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JSpinner width=new JSpinner(new SpinnerNumberModel(current.width(),320,3840,80));
        JSpinner height=new JSpinner(new SpinnerNumberModel(current.height(),240,2160,60));
        JComboBox<String> resolution=new JComboBox<>(new String[]{"custom","640 x 480","800 x 600","960 x 720","1280 x 960","1600 x 1200"});
        resolution.addActionListener(e->{int i=resolution.getSelectedIndex();int[] sizes={0,640,800,960,1280,1600};if(i>0){width.setValue(sizes[i]);height.setValue(sizes[i]*3/4);}});
        JCheckBox lock=new JCheckBox("lock window size",current.lockWindow()),top=new JCheckBox("always on top",current.alwaysOnTop());
        JCheckBox fog=new JCheckBox("enable fog",current.fog()),filter=new JCheckBox("bilinear filtering",current.bilinear());
        JCheckBox wire=new JCheckBox("wireframe",current.wireframe()),sync=new JCheckBox("vsync at normal speed",current.vsync());
        page.add(new JLabel("resolution preset:"));page.add(resolution);
        page.add(new JLabel("width:"));page.add(width);page.add(new JLabel("height:"));page.add(height);
        page.add(lock);page.add(top);page.add(fog);page.add(filter);page.add(wire);page.add(sync);
        JPanel content=new JPanel(new BorderLayout());content.add(page,BorderLayout.CENTER);
        JLabel hint=new JLabel("saved settings apply when the rom is reopened");hint.setBorder(BorderFactory.createEmptyBorder(0,16,10,16));content.add(hint,BorderLayout.SOUTH);
        JButton save=new JButton("save"),cancel=new JButton("cancel"),reset=new JButton("defaults");
        reset.addActionListener(e->{resolution.setSelectedIndex(0);width.setValue(640);height.setValue(480);lock.setSelected(false);top.setSelected(false);fog.setSelected(true);filter.setSelected(true);wire.setSelected(false);sync.setSelected(false);});
        save.addActionListener(e->{
            try {
                width.commitEdit();height.commitEdit();
                Kernel64GLSettings.save(new Kernel64GLSettings.Options(((Number)width.getValue()).intValue(),((Number)height.getValue()).intValue(),lock.isSelected(),top.isSelected(),fog.isSelected(),filter.isSelected(),wire.isSelected(),sync.isSelected()));
                dispose();
            } catch(Exception error){JOptionPane.showMessageDialog(this,"could not save settings: "+error.getMessage(),"Kernel64GL settings",JOptionPane.ERROR_MESSAGE);}
        });
        cancel.addActionListener(e->dispose());JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(reset);buttons.add(cancel);buttons.add(save);
        add(content,BorderLayout.CENTER);add(buttons,BorderLayout.SOUTH);getRootPane().setDefaultButton(save);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);pack();setResizable(false);setLocationRelativeTo(parent);
    }
}
