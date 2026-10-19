package kerneln64.debugger;
import kerneln64.cpu.Cpu;
import kerneln64.ui.HostMessages;
import kerneln64.memory.Memory;
import kerneln64.Main;


import java.awt.*;
import java.nio.ByteBuffer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class MemorySearchDialog extends JDialog {
    private final JComboBox<String> width = new JComboBox<>(new String[] {"8-bit", "16-bit", "32-bit"});
    private final JComboBox<String> comparison = new JComboBox<>(new String[] {"Equals value", "Not equal to value", "Less than value", "Greater than value", "Changed", "Unchanged", "Increased", "Decreased"});
    private final JTextField value = new JTextField("0", 12);
    private final JLabel status = new JLabel("Start a search to capture RDRAM. Values are unsigned; use decimal or 0xHEX.");
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"Address", "Decimal", "Hex"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton start = new JButton("New Search"), refine = new JButton("Filter"), poke = new JButton("Write Value"), copy = new JButton("Copy GameShark Code");
    private final Cpu cpu;
    private MemorySearch search;
    private int[] addresses = new int[0];

    public MemorySearchDialog() {
        super(Main.hMainWindow, "Memory Search — RDRAM", false);
        cpu = Main.cpu;
        JPanel controls = new JPanel(); controls.add(width); controls.add(comparison); controls.add(value); controls.add(start); controls.add(refine);
        width.addActionListener(e -> { search = null; addresses = new int[0]; model.setRowCount(0); refine.setEnabled(false); poke.setEnabled(false); copy.setEnabled(false); });
        comparison.addActionListener(e -> value.setEnabled(comparison.getSelectedIndex() < 4));
        start.addActionListener(e -> scan(true)); refine.addActionListener(e -> scan(false));
        poke.addActionListener(e -> write()); copy.addActionListener(e -> copy());
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(poke); actions.add(copy); bottom.add(actions, BorderLayout.SOUTH);
        add(controls, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER); add(bottom, BorderLayout.SOUTH);
        refine.setEnabled(false); poke.setEnabled(false); copy.setEnabled(false);
        setSize(760, 470); setLocationRelativeTo(Main.hMainWindow); setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void guard() {
        if (Main.cpu != cpu || !cpu.running) throw new IllegalStateException("The ROM changed or stopped. Reopen Memory Search.");
    }
    private byte[] snapshot() throws Exception {
        guard(); byte[][] result = new byte[1][];
        cpu.submitCommand(() -> { guard(); ByteBuffer ram = Main.mem.RDRAM.duplicate().clear(); result[0] = new byte[ram.capacity()]; ram.get(result[0]); }).get();
        guard(); return result[0];
    }
    private void busy(boolean busy) {
        start.setEnabled(!busy); width.setEnabled(!busy);
        refine.setEnabled(!busy && search != null); poke.setEnabled(!busy && addresses.length > 0); copy.setEnabled(!busy && addresses.length > 0);
    }
    private void scan(boolean fresh) {
        int selectedWidth = 1 << width.getSelectedIndex();
        String mode = (String) comparison.getSelectedItem();
        boolean numeric = comparison.getSelectedIndex() < 4;
        long target;
        try { target = comparison.getSelectedIndex() < 4 ? MemorySearch.parse(value.getText(), selectedWidth) : 0; }
        catch (RuntimeException error) { HostMessages.error("Memory Search", error); return; }
        busy(true);
        new SwingWorker<byte[], Void>() {
            protected byte[] doInBackground() throws Exception {
                byte[] bytes = snapshot();
                if (fresh) search = new MemorySearch(bytes, selectedWidth);
                if (!fresh || numeric) search.refine(bytes, mode, target);
                return bytes;
            }
            protected void done() {
                try { display(get()); }
                catch (Exception error) { HostMessages.error("Memory Search", error); }
                finally { busy(false); }
            }
        }.execute();
    }
    private void display(byte[] bytes) {
        addresses = search.addresses(1000); model.setRowCount(0);
        for (int address : addresses) {
            long number = MemorySearch.value(bytes, address, search.width());
            model.addRow(new Object[] {String.format("%08X", 0x80000000 | address), number, String.format("%0" + (search.width() * 2) + "X", number)});
        }
        status.setText(search.count() + " matches; showing " + addresses.length + ". Comparisons use the previous search snapshot.");
    }
    private void write() {
        int row = table.getSelectedRow(); if (row < 0) return;
        String text = JOptionPane.showInputDialog(this, "New unsigned value (decimal or 0xHEX):", model.getValueAt(row, 1));
        if (text == null) return;
        final long number;
        try { number = MemorySearch.parse(text, search.width()); }
        catch (RuntimeException error) { HostMessages.error("Write Value", error); return; }
        int address = addresses[row], bytes = search.width(); busy(true);
        new SwingWorker<byte[], Void>() {
            protected byte[] doInBackground() throws Exception {
                guard();
                cpu.submitCommand(() -> {
                    guard();
                    for (int i = 0; i < bytes; i++) Main.mem.RDRAM.put(address + i, (byte)(number >>> ((bytes - i - 1) * 8)));
                    cpu.invalidateCode(address & ~3); cpu.invalidateCode((address + bytes - 1) & ~3);
                }).get();
                return snapshot();
            }
            protected void done() {
                try { display(get()); } catch (Exception error) { HostMessages.error("Write Value", error); }
                finally { busy(false); }
            }
        }.execute();
    }
    private void copy() {
        int row = table.getSelectedRow(); if (row < 0) return;
        long number = ((Number)model.getValueAt(row, 1)).longValue();
        String text = MemorySearch.cheat(addresses[row], search.width(), number);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
        status.setText("Copied code. Paste it into Tools > GameShark Codes to keep the value applied.");
    }
}
