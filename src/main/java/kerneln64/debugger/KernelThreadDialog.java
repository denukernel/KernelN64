package kerneln64.debugger;
import kerneln64.hardware.Audio;
import kerneln64.ui.CoreOptions;
import kerneln64.cpu.Cpu;
import kerneln64.Main;


import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Modeless monitor. RAM is read only in queued CPU-boundary commands. */
public final class KernelThreadDialog extends JDialog {
    private static KernelThreadDialog current;
    public static volatile boolean monitorOpen;
    private final JTextArea display = new JTextArea(19, 118);
    private final JCheckBox generic = new JCheckBox("Generic libultra (experimental)");
    private final JCheckBox pauseOnFreeze = new JCheckBox("Pause on thread 4 audio fault/stall", CoreOptions.pauseOnAudioHandshake);
    private final JCheckBox pauseOnFault = new JCheckBox("Pause on any kernel thread fault", CoreOptions.pauseOnKernelFault);
    private boolean displayedAutoPause;
    private final Timer timer;
    private final JTextArea inspection = new JTextArea(16, 100);
    private final JScrollPane inspectionScroll = new JScrollPane(inspection);
    private final JScrollPane displayScroll = new JScrollPane(display);
    private final JTextArea audio = new JTextArea(19, 118);
    private final JScrollPane audioScroll = new JScrollPane(audio);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel freshness = new JLabel("Waiting for a CPU snapshot");
    private java.util.Map<Integer, Integer> rows = java.util.Map.of();
    private int selected;
    private int pendingSelection;
    private long submitted;
    private CompletableFuture<KernelThreadMonitor.View> pending;
    private Future<?> command;
    private Cpu owner;
    private boolean rescan = true;
    private volatile boolean closed;
    public static void showMonitor() {
        if (current == null || !current.isDisplayable()) current = new KernelThreadDialog();
        current.setVisible(true); current.toFront(); current.refresh();
    }
    private KernelThreadDialog() {
        super(Main.hMainWindow, "KernelN64 Kernel Thread Monitor", false);
        monitorOpen = true;
        rescan = KernelThreadMonitor.active == null || KernelThreadMonitor.active.cpu != Main.cpu;
        if (!rescan) generic.setSelected(KernelThreadMonitor.active.generic);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        display.setEditable(false); display.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        display.setBackground(new Color(18, 24, 30)); display.setForeground(new Color(183, 231, 200));
        display.setMargin(new Insets(12, 12, 12, 12));
        inspection.setEditable(false); inspection.setFont(display.getFont());
        inspection.setBackground(display.getBackground()); inspection.setForeground(display.getForeground());
        inspection.setMargin(display.getMargin());
        audio.setEditable(false); audio.setFont(display.getFont());
        audio.setBackground(display.getBackground()); audio.setForeground(display.getForeground());
        audio.setMargin(display.getMargin());
        tabs.addTab("Threads", displayScroll); tabs.addTab("Thread diagnosis", audioScroll);
        display.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try {
                    int line = display.getLineOfOffset(display.viewToModel2D(e.getPoint()));
                    Integer address = rows.get(line);
                    if (address != null) {
                        selected = address; inspection.setText("Reading selected thread...");
                        inspection.setCaretPosition(0);
                    }
                } catch (javax.swing.text.BadLocationException ignored) { }
            }
        });
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton scan = new JButton("Rescan"); scan.addActionListener(e -> rescan = true);
        generic.addActionListener(e -> { owner = null; rescan = true; KernelThreadMonitor.active = null; });
        toolbar.add(scan); toolbar.add(generic); toolbar.add(new JLabel("Read-only • refresh 250 ms • click a thread row to inspect"));
        pauseOnFreeze.setToolTipText("Continues in the background and opens this monitor on a freeze. Rescan re-arms after Resume.");
        pauseOnFreeze.addActionListener(e -> { CoreOptions.pauseOnAudioHandshake = pauseOnFreeze.isSelected(); Main.saveConfig(); });
        pauseOnFault.setToolTipText("Detects faults in every validated guest thread, including while this window is closed. Waiting or stopping alone is not a crash.");
        pauseOnFault.addActionListener(e -> { CoreOptions.pauseOnKernelFault = pauseOnFault.isSelected(); Main.saveConfig(); });
        JButton resume = new JButton("Resume emulation");
        resume.addActionListener(e -> {
            Cpu cpu = Main.cpu;
            if (cpu != null && cpu.running) cpu.submitCommand(() -> { if (Main.cpu == cpu) cpu.debugger.resume(); });
        });
        JPanel pauseControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pauseControls.add(pauseOnFault); pauseControls.add(pauseOnFreeze); pauseControls.add(resume);
        pauseControls.add(new JLabel("Runs in background; opens this monitor on a freeze"));
        JPanel controls = new JPanel(new GridLayout(0, 1)); controls.add(toolbar); controls.add(pauseControls);
        add(controls, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, inspectionScroll);
        split.setResizeWeight(0.5); add(split, BorderLayout.CENTER); add(freshness, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(Main.hMainWindow);
        timer = new Timer(250, e -> refresh()); timer.start();
    }
    @Override public void dispose() {
        if (timer != null) timer.stop();
        synchronized (KernelThreadMonitor.class) {
            closed = true; monitorOpen = false;
            if (!CoreOptions.pauseOnAudioHandshake && !CoreOptions.pauseOnKernelFault) KernelThreadMonitor.active = null;
        }
        if (command != null) command.cancel(false);
        super.dispose();
    }
    private void refresh() {
        Cpu cpu = Main.cpu;
        if (cpu == null || !cpu.running) {
            KernelThreadMonitor.active = null; owner = null;
            if (command != null) command.cancel(false);
            pending = null; rows = java.util.Map.of(); selected = 0;
            freshness.setText("Emulation stopped - retained snapshot is stale. Open a ROM to refresh."); return;
        }
        if (pending != null) {
            if (owner != cpu || (command != null && command.isCancelled())) { pending = null; }
            else if (!pending.isDone()) {
                if (System.nanoTime() - submitted > 2_000_000_000L)
                    freshness.setText("STALE - CPU has not serviced the snapshot request for over 2 seconds; busy or stalled.");
                return;
            }
            else {
                try {
                    var view = pending.join(); rows = view.rows();
                    Point overviewPosition = displayScroll.getViewport().getViewPosition();
                    display.setText(view.text()); display.setCaretPosition(0);
                    SwingUtilities.invokeLater(() -> displayScroll.getViewport().setViewPosition(overviewPosition));
                    Point audioPosition = audioScroll.getViewport().getViewPosition();
                    audio.setText(view.diagnosis()); audio.setCaretPosition(0);
                    SwingUtilities.invokeLater(() -> audioScroll.getViewport().setViewPosition(audioPosition));
                    boolean warning = view.diagnosis().contains("SUSPECTED") || view.diagnosis().contains("THREAD FAULT:");
                    tabs.setTitleAt(1, warning ? "Thread diagnosis (!)" : "Thread diagnosis");
                    boolean autoPaused = view.diagnosis().startsWith("AUTO-PAUSED:");
                    if (autoPaused && !displayedAutoPause) tabs.setSelectedIndex(1);
                    displayedAutoPause = autoPaused;
                    if (pendingSelection == selected) {
                        Point position = inspectionScroll.getViewport().getViewPosition();
                        inspection.setText(view.inspection()); inspection.setCaretPosition(0);
                        SwingUtilities.invokeLater(() -> inspectionScroll.getViewport().setViewPosition(position));
                    }
                    freshness.setText("Snapshot received " + java.time.LocalTime.now().withNano(0) + " - read-only");
                }
                catch (CompletionException error) { display.setText("Monitor unavailable: " + error.getCause().getMessage()); }
                pending = null;
            }
        }
        if (owner != cpu) { selected = 0; rows = java.util.Map.of(); }
        boolean discover = rescan, allowGeneric = generic.isSelected(), autoPause = pauseOnFreeze.isSelected(); rescan = false; owner = cpu;
        int requested = selected; pendingSelection = requested; submitted = System.nanoTime();
        CompletableFuture<KernelThreadMonitor.View> result = new CompletableFuture<>(); pending = result;
        command = cpu.submitCommand(() -> {
            if (closed || Main.cpu != cpu) { result.complete(new KernelThreadMonitor.View("ROM changed; reconnecting...", java.util.Map.of(), "", "")); return; }
            try {
                KernelThreadMonitor session;
                synchronized (KernelThreadMonitor.class) {
                    if (closed) { result.complete(new KernelThreadMonitor.View("Monitor closed", java.util.Map.of(), "", "")); return; }
                    session = KernelThreadMonitor.active;
                    if (session == null || session.cpu != cpu || session.generic != allowGeneric)
                        KernelThreadMonitor.active = session = new KernelThreadMonitor(cpu, Main.mem, allowGeneric);
                }
                session.setPauseOnFreeze(autoPause);
                session.setPauseOnFault(CoreOptions.pauseOnKernelFault);
                result.complete(session.view(discover, requested));
            } catch (RuntimeException error) { result.completeExceptionally(error); }
        });
    }
}
