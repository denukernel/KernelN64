package basiccontroller;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import plugin.ControllerPlugin.Buttons;

/** Capture before Swing traversal/accelerators, with an isolated staged-input preview. */
public final class KeyboardConfig extends JDialog {
    private static final String[] NAMES={"stick up","stick down","stick left","stick right","L","R","d-pad up","d-pad down","d-pad left","d-pad right","C up","C down","C left","C right","start","A","B","Z"};
    private int[] keys=KeyboardSettings.current.keys();
    private final JButton[] bindings=new JButton[keys.length];
    private int waiting=-1,swallow;
    private final JSlider strength=new JSlider(20,127,KeyboardSettings.current.strength());
    private final JCheckBox circular=new JCheckBox("limit diagonal stick range to a circle"),invertX=new JCheckBox("invert horizontal stick"),invertY=new JCheckBox("invert vertical stick"),
        slow=new JCheckBox("enable precision modifier"),sides=new JCheckBox("capture left / right and numpad keys separately"),duplicates=new JCheckBox("allow one key to control multiple buttons");
    private final JComboBox<String> opposites=new JComboBox<>(new String[]{"neutral (opposites cancel)","most recently pressed wins"});
    private final JSpinner slowStrength=new JSpinner(new SpinnerNumberModel(40,1,127,1));
    private int slowKey=KeyboardSettings.current.slowKey();
    private final JButton slowBinding=new JButton(),preview=new JButton("click here, then hold keys to test input");
    private final JLabel status=new JLabel("changes are staged until apply or ok"),output=new JLabel("x: 0   y: 0   buttons: none");
    private final BasicKeyboardPlugin testPad=new BasicKeyboardPlugin();
    private final KeyEventDispatcher capture=this::capture;
    private final Timer timer;
    private final Runnable release;

    public KeyboardConfig(JFrame parent,Runnable release){
        super(parent,"KernelN64 keyboard controller",true);this.release=release;release.run();
        JPanel content=new JPanel(new BorderLayout(10,10));content.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel hints=new JPanel(new GridLayout(0,1));hints.add(new JLabel("click a binding, then press a key. escape cancels capture."));
        hints.add(new JLabel("backspace clears a binding. assigned keys take priority in the game view."));content.add(hints,BorderLayout.NORTH);
        JPanel basic=new JPanel(new BorderLayout(8,8)),grid=new JPanel(new GridLayout(9,4,8,6));
        for(int i=0;i<keys.length;i++){
            int index=i;grid.add(new JLabel(NAMES[i]));bindings[i]=new JButton();bindings[i].setFocusTraversalKeysEnabled(false);
            bindings[i].addActionListener(e->{waiting=index;status.setText("press a key for "+NAMES[index]);refresh();bindings[index].requestFocusInWindow();});grid.add(bindings[i]);
        }
        basic.add(grid,BorderLayout.CENTER);JPanel analog=new JPanel(new BorderLayout());analog.add(new JLabel("stick strength (80 is the default):"),BorderLayout.NORTH);
        strength.setMajorTickSpacing(20);strength.setPaintTicks(true);strength.setPaintLabels(true);analog.add(strength);basic.add(analog,BorderLayout.SOUTH);
        JPanel advanced=new JPanel();advanced.setLayout(new BoxLayout(advanced,BoxLayout.Y_AXIS));
        advanced.add(circular);advanced.add(invertX);advanced.add(invertY);advanced.add(new JLabel("opposite stick directions:"));advanced.add(opposites);
        advanced.add(slow);JPanel precision=new JPanel(new FlowLayout(FlowLayout.LEFT));precision.add(new JLabel("modifier:"));precision.add(slowBinding);precision.add(new JLabel("strength:"));precision.add(slowStrength);advanced.add(precision);
        slowBinding.setFocusTraversalKeysEnabled(false);slowBinding.addActionListener(e->{waiting=keys.length;refresh();slowBinding.requestFocusInWindow();});
        advanced.add(sides);advanced.add(duplicates);advanced.add(new JLabel("side capture affects new assignments; existing bindings remain unchanged."));
        advanced.add(Box.createVerticalStrut(12));advanced.add(preview);advanced.add(output);advanced.add(new JLabel("staged input stays here; escape leaves the test area."));
        preview.setFocusTraversalKeysEnabled(false);preview.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){testPad.releaseAll();}});
        JTabbedPane tabs=new JTabbedPane();tabs.addTab("basic",basic);tabs.addTab("advanced",advanced);content.add(tabs);
        JPanel bottom=new JPanel(new BorderLayout());bottom.add(status,BorderLayout.NORTH);JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton defaults=new JButton("defaults"),apply=new JButton("apply"),ok=new JButton("ok"),cancel=new JButton("cancel");
        defaults.addActionListener(e->stage(KeyboardSettings.defaults()));apply.addActionListener(e->apply(false));ok.addActionListener(e->apply(true));cancel.addActionListener(e->dispose());
        buttons.add(defaults);buttons.add(cancel);buttons.add(apply);buttons.add(ok);bottom.add(buttons);content.add(bottom,BorderLayout.SOUTH);setContentPane(content);
        stage(KeyboardSettings.current);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(capture);
        timer=new Timer(50,e->{Buttons value=new Buttons();testPad.readMapping(mapping(),value);StringBuilder names=new StringBuilder();
            String[] labels={"A","B","Z","start","d-up","d-down","d-left","d-right","L","R","C-up","C-down","C-left","C-right"};
            int[] masks={Buttons.A_BUTTON,Buttons.B_BUTTON,Buttons.Z_TRIG,Buttons.START_BUTTON,Buttons.U_DPAD,Buttons.D_DPAD,Buttons.L_DPAD,Buttons.R_DPAD,Buttons.L_TRIG,Buttons.R_TRIG,Buttons.U_CBUTTON,Buttons.D_CBUTTON,Buttons.L_CBUTTON,Buttons.R_CBUTTON};
            for(int i=0;i<masks.length;i++)if((value.value&masks[i])!=0){if(names.length()>0)names.append(" ");names.append(labels[i]);}
            output.setText("x: "+value.getXAxis()+"   y: "+value.getYAxis()+"   buttons: "+(names.length()==0?"none":names));});timer.start();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);pack();setMinimumSize(new Dimension(600,getHeight()));setLocationRelativeTo(parent);
    }
    private KeyboardSettings.Mapping mapping(){return new KeyboardSettings.Mapping(keys,strength.getValue(),circular.isSelected(),invertX.isSelected(),invertY.isSelected(),slow.isSelected(),slowKey,(Integer)slowStrength.getValue(),opposites.getSelectedIndex()==1,sides.isSelected(),duplicates.isSelected());}
    private void stage(KeyboardSettings.Mapping m){keys=m.keys();strength.setValue(m.strength());circular.setSelected(m.circular());invertX.setSelected(m.invertX());invertY.setSelected(m.invertY());slow.setSelected(m.slowEnabled());slowKey=m.slowKey();slowStrength.setValue(m.slowStrength());opposites.setSelectedIndex(m.lastWins()?1:0);sides.setSelected(m.distinguishSides());duplicates.setSelected(m.allowDuplicates());waiting=-1;testPad.releaseAll();refresh();status.setText("changes are staged until apply or ok");}
    private void apply(boolean close){
        if(waiting>=0){status.setText("finish key capture or press escape before applying");return;}
        try{KeyboardSettings.save(mapping());release.run();testPad.releaseAll();status.setText("applied live; release held keys, then press again");if(close)dispose();}
        catch(java.io.IOException e){JOptionPane.showMessageDialog(this,e.getMessage(),"save controls",JOptionPane.ERROR_MESSAGE);}
    }
    private boolean capture(KeyEvent event){
        Component source=event.getComponent();if(source==null||SwingUtilities.getWindowAncestor(source)!=this)return false;
        int code=event.getKeyCode();
        if(swallow!=0&&code==swallow){if(event.getID()==KeyEvent.KEY_RELEASED)swallow=0;event.consume();return true;}
        if(waiting>=0){
            if(event.getID()==KeyEvent.KEY_PRESSED){
                swallow=code;
                if(code==KeyEvent.VK_ESCAPE){waiting=-1;status.setText("capture cancelled");}
                else if(code!=KeyEvent.VK_UNDEFINED){
                    int binding=code==KeyEvent.VK_BACK_SPACE?0:KeyboardSettings.binding(code,sides.isSelected()?captureLocation(event):0);
                    int conflict=-1;
                    if(!duplicates.isSelected()&&binding!=0)for(int i=0;i<keys.length;i++)if(i!=waiting&&KeyboardSettings.overlaps(keys[i],binding)){conflict=i;break;}
                    if(conflict>=0)status.setText(KeyboardSettings.keyName(binding)+" is assigned to "+NAMES[conflict]+"; clear it first or allow shared keys");
                    else{if(waiting==keys.length)slowKey=binding;else keys[waiting]=binding;waiting=-1;status.setText("binding staged; apply to use it live");}
                }
                refresh();
            }
            event.consume();return true;
        }
        if(preview.isFocusOwner()){
            if(code==KeyEvent.VK_ESCAPE){testPad.releaseAll();opposites.requestFocusInWindow();event.consume();return true;}
            if(event.getID()==KeyEvent.KEY_PRESSED)testPad.wmKeyDown(code,event.getKeyLocation());
            else if(event.getID()==KeyEvent.KEY_RELEASED)testPad.wmKeyUp(code,event.getKeyLocation());
            event.consume();return true;
        }
        return false;
    }
    private static int captureLocation(KeyEvent event){int location=event.getKeyLocation();return location==KeyEvent.KEY_LOCATION_STANDARD?0:location;}
    private void refresh(){for(int i=0;i<keys.length;i++)bindings[i].setText(waiting==i?"press a key...":KeyboardSettings.keyName(keys[i]));slowBinding.setText(waiting==keys.length?"press a key...":KeyboardSettings.keyName(slowKey));}
    @Override public void dispose(){KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(capture);if(timer!=null)timer.stop();testPad.releaseAll();release.run();super.dispose();}
}
