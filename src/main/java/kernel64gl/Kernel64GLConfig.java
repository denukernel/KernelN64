package kernel64gl;

import java.awt.*;
import javax.swing.*;

/** Gameplay and diagnostic settings are staged until saved, then applied live. */
public final class Kernel64GLConfig extends JDialog {
    private static JSpinner spinner(int value,int min,int max,int step){return new JSpinner(new SpinnerNumberModel(value,min,max,step));}
    private static int number(JSpinner value)throws java.text.ParseException{value.commitEdit();return ((Number)value.getValue()).intValue();}
    private static void row(JPanel page,String label,JComponent value){page.add(new JLabel(label));page.add(value);}
    public Kernel64GLConfig(JFrame parent){
        super(parent,"Kernel64GL settings",true);
        var current=Kernel64GLSettings.load();var a=current.advanced();
        JPanel gameplay=new JPanel(new GridLayout(0,2,10,10)),debug=new JPanel(new GridLayout(0,2,10,10));
        for(JPanel page:new JPanel[]{gameplay,debug})page.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JSpinner width=spinner(current.width(),320,3840,80),height=spinner(current.height(),240,2160,60);
        JComboBox<String> resolution=new JComboBox<>(new String[]{"custom","640 x 480","800 x 600","960 x 720","1280 x 960","1600 x 1200"});
        resolution.addActionListener(e->{int i=resolution.getSelectedIndex();int[] sizes={0,640,800,960,1280,1600};if(i>0){width.setValue(sizes[i]);height.setValue(sizes[i]*3/4);}});
        JCheckBox lock=new JCheckBox("lock window size",current.lockWindow()),top=new JCheckBox("always on top",current.alwaysOnTop());
        JCheckBox fog=new JCheckBox("enable fog",current.fog()),filter=new JCheckBox("bilinear filtering",current.bilinear());
        JCheckBox wire=new JCheckBox("wireframe",current.wireframe()),sync=new JCheckBox("vsync at normal speed",current.vsync());
        JSpinner cacheSize=spinner(a.cacheMiB(),16,256,16),batch=spinner(a.batchTriangles(),16,512,16);
        JComboBox<String> precision=new JComboBox<>(new String[]{"16-bit","automatic","32-bit (experimental)"});precision.setSelectedIndex(a.textureDepth());
        JCheckBox force=new JCheckBox("force bilinear over guest choice",a.forceBilinear()),cache=new JCheckBox("reuse cached textures",a.cacheTextures());
        row(gameplay,"resolution preset:",resolution);row(gameplay,"width:",width);row(gameplay,"height:",height);
        gameplay.add(lock);gameplay.add(top);gameplay.add(fog);gameplay.add(filter);gameplay.add(sync);gameplay.add(force);
        row(gameplay,"texture cache budget (MiB):",cacheSize);row(gameplay,"texture output:",precision);row(gameplay,"triangles per batch:",batch);gameplay.add(cache);gameplay.add(new JLabel("automatic textures recommended"));
        JCheckBox textures=new JCheckBox("render textures",a.textures()),depth=new JCheckBox("depth testing",a.depthTest()),culling=new JCheckBox("face culling",a.culling());
        JComboBox<String> view=new JComboBox<>(new String[]{"normal","vertex colors","texture 0","fog amount","depth"});view.setSelectedIndex(a.debugView());
        JSpinner commands=spinner(a.commandLimit(),4096,1048576,4096),messages=spinner(a.messageLimit(),1,256,16);
        JCheckBox pause=new JCheckBox("pause after display-list recovery",a.pauseOnInvalid()),logs=new JCheckBox("graphics diagnostic messages",a.diagnostics());
        debug.add(wire);debug.add(textures);debug.add(depth);debug.add(culling);row(debug,"visualization:",view);row(debug,"command budget per task:",commands);
        debug.add(pause);debug.add(logs);row(debug,"maximum messages per task:",messages);
        debug.add(new JLabel("lower budgets recover loops sooner"));debug.add(new JLabel("but may truncate valid complex scenes"));
        debug.add(new JLabel("debug views change game appearance"));debug.add(new JLabel("bounds checks always stay enabled"));
        java.util.function.Consumer<Kernel64GLSettings.Advanced> advanced=option->{
            cacheSize.setValue(option.cacheMiB());precision.setSelectedIndex(option.textureDepth());force.setSelected(option.forceBilinear());textures.setSelected(option.textures());depth.setSelected(option.depthTest());culling.setSelected(option.culling());cache.setSelected(option.cacheTextures());view.setSelectedIndex(option.debugView());commands.setValue(option.commandLimit());pause.setSelected(option.pauseOnInvalid());logs.setSelected(option.diagnostics());messages.setValue(option.messageLimit());batch.setValue(option.batchTriangles());
        };
        JComboBox<String> profile=new JComboBox<>(new String[]{"custom","gameplay defaults","corruption recovery","graphics inspection"});
        profile.addActionListener(e->{switch(profile.getSelectedIndex()){
            case 1->{advanced.accept(Kernel64GLSettings.advancedDefaults());fog.setSelected(true);filter.setSelected(true);wire.setSelected(false);}
            case 2->advanced.accept(Kernel64GLSettings.corruptionDefaults());
            case 3->advanced.accept(Kernel64GLSettings.inspectionDefaults());
            default->{}
        }});
        JPanel profiles=new JPanel(new FlowLayout(FlowLayout.LEFT));profiles.add(new JLabel("preset:"));profiles.add(profile);
        JTabbedPane tabs=new JTabbedPane();tabs.addTab("gameplay",gameplay);tabs.addTab("advanced / corruption / debug",debug);
        JLabel hint=new JLabel("save applies settings to the next rendered frame; resume if paused");hint.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        JPanel center=new JPanel(new BorderLayout());center.add(tabs,BorderLayout.CENTER);center.add(hint,BorderLayout.SOUTH);
        JButton save=new JButton("save"),cancel=new JButton("cancel"),reset=new JButton("defaults");
        reset.addActionListener(e->{profile.setSelectedIndex(0);resolution.setSelectedIndex(0);width.setValue(640);height.setValue(480);lock.setSelected(false);top.setSelected(false);fog.setSelected(true);filter.setSelected(true);wire.setSelected(false);sync.setSelected(false);advanced.accept(Kernel64GLSettings.advancedDefaults());});
        save.addActionListener(e->{try{
            var options=new Kernel64GLSettings.Advanced(number(cacheSize),precision.getSelectedIndex(),force.isSelected(),textures.isSelected(),depth.isSelected(),culling.isSelected(),cache.isSelected(),view.getSelectedIndex(),number(commands),pause.isSelected(),logs.isSelected(),number(messages),number(batch));
            Kernel64GLSettings.save(new Kernel64GLSettings.Options(number(width),number(height),lock.isSelected(),top.isSelected(),fog.isSelected(),filter.isSelected(),wire.isSelected(),sync.isSelected(),options));dispose();
        }catch(Exception error){JOptionPane.showMessageDialog(this,"could not save settings: "+error.getMessage(),"Kernel64GL settings",JOptionPane.ERROR_MESSAGE);}});
        cancel.addActionListener(e->dispose());JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(reset);buttons.add(cancel);buttons.add(save);
        add(profiles,BorderLayout.NORTH);add(center,BorderLayout.CENTER);add(buttons,BorderLayout.SOUTH);getRootPane().setDefaultButton(save);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);pack();setResizable(false);setLocationRelativeTo(parent);
    }
}
