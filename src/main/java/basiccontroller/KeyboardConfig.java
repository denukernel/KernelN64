package basiccontroller;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class KeyboardConfig extends JDialog {
    private int[] keys = KeyboardSettings.current.keys();
    private final JButton[] bindings = new JButton[keys.length];
    private int waiting = -1;

    public KeyboardConfig(JFrame parent, Runnable release) {
        super(parent, "Keyboard Controller Settings", true);
        release.run();
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel instructions=new JPanel(new GridLayout(0,1));
        instructions.add(new JLabel("Click a binding, then press a key. Backspace clears it; Escape cancels."));
        instructions.add(new JLabel("Assigned keys override emulator shortcuts while the game view has focus."));
        instructions.add(new JLabel("For overlapping shortcuts (such as Ctrl+X), use the emulator menu."));
        content.add(instructions, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(9, 4, 8, 6));
        String[] names = { "Stick Up", "Stick Down", "Stick Left", "Stick Right", "L", "R", "D-pad Up", "D-pad Down", "D-pad Left", "D-pad Right", "C Up", "C Down", "C Left", "C Right", "Start", "A", "B", "Z" };
        for (int i = 0; i < keys.length; i++) {
            final int index = i;
            grid.add(new JLabel(names[i])); bindings[i] = new JButton();
            bindings[i].addActionListener(e -> { waiting = index; refresh(); bindings[index].requestFocusInWindow(); });
            bindings[i].addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (waiting != index) return;
                    e.consume();
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_ESCAPE) { waiting = -1; refresh(); return; }
                    if (key == KeyEvent.VK_BACK_SPACE) key = 0;
                    for (int j = 0; j < keys.length; j++) if (j != index && key != 0 && keys[j] == key) keys[j] = 0;
                    keys[index] = key; waiting = -1; refresh();
                }
            });
            grid.add(bindings[i]);
        }
        refresh(); content.add(grid, BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JSlider strength = new JSlider(20, 127, KeyboardSettings.current.strength());
        strength.setMajorTickSpacing(20); strength.setPaintTicks(true); strength.setPaintLabels(true);
        bottom.add(new JLabel("Analog strength (80 = default):")); bottom.add(strength);
        JCheckBox circular = new JCheckBox("Limit diagonal stick range to a circle", KeyboardSettings.current.circular()); bottom.add(circular);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reset = new JButton("Restore Defaults"), ok = new JButton("OK"), cancel = new JButton("Cancel");
        reset.addActionListener(e -> { keys = KeyboardSettings.defaults().keys(); strength.setValue(80); circular.setSelected(false); waiting = -1; refresh(); });
        ok.addActionListener(e -> {
            try { KeyboardSettings.save(new KeyboardSettings.Mapping(keys, strength.getValue(), circular.isSelected())); release.run(); dispose(); }
            catch (java.io.IOException error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Save Controls", JOptionPane.ERROR_MESSAGE); }
        });
        cancel.addActionListener(e -> dispose()); buttons.add(reset); buttons.add(ok); buttons.add(cancel); bottom.add(buttons);
        content.add(bottom, BorderLayout.SOUTH); setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); pack(); setResizable(false); setLocationRelativeTo(parent);
    }
    private void refresh() {
        for (int i = 0; i < keys.length; i++) bindings[i].setText(waiting == i ? "Press a key…" : keys[i] == 0 ? "Unassigned" : KeyEvent.getKeyText(keys[i]));
    }
}
