package kerneln64.debugger;
import kerneln64.ui.CoreOptions;
import kerneln64.cpu.Cpu;
import kerneln64.debugger.trace.Debug;
import kerneln64.Main;


import java.util.*;

/** Debug controls serviced at CPU boundaries. No Swing calls on the CPU thread. */
public final class DebuggerCore {
    final Cpu cpu;
    public final Set<Integer> execute = new TreeSet<>(Integer::compareUnsigned);
    final List<Watch> watches = new ArrayList<>();
    final ArrayDeque<String> trace = new ArrayDeque<>();
    record Watch(int address, int length, boolean read, boolean write) {}
    public volatile boolean paused, tracing;
    volatile String reason = "Running";
    public boolean fetching;
    private int steps, skipPc;
    private boolean skipOnce;
    private boolean monitorPause;
    private Integer runTo;
    public DebuggerCore(Cpu cpu) { this.cpu = cpu; paused = CoreOptions.debuggerEnabled && CoreOptions.pauseOnStart; }
    public void pause(String why) { paused = true; steps = 0; reason = why; }
    void pauseForMonitor(String why) { monitorPause = true; pause(why); }
    public void resume() { monitorPause = false; skipPc = cpu.pc; skipOnce = true; paused = false; steps = 0; runTo = null; reason = "Running"; if(Main.video!=null)Main.video.resetTimer(); }
    void step() { requirePaused(); steps++; reason = "Step"; }
    void runTo(int address) { requirePaused(); resume(); runTo = address; reason = String.format("Running to %08X",address); }
    void requirePaused() { if (!paused) throw new IllegalStateException("Pause emulation before editing or stepping."); }
    public boolean beforeInstruction() {
        if (!CoreOptions.debuggerEnabled && !monitorPause) { paused = false; return true; }
        if (paused) {
            if (steps > 0) { steps--; return true; }
            return false;
        }
        if (!CoreOptions.debuggerEnabled) return true;
        boolean skip = skipOnce && skipPc == cpu.pc; skipOnce = false;
        if (!skip && ((!execute.isEmpty() && execute.contains(cpu.pc)) || (runTo != null && runTo == cpu.pc))) {
            runTo = null; pause(String.format("Execution breakpoint at %08X",cpu.pc)); return false;
        }
        return true;
    }
    public void record(int pc, int opcode) {
        if (!CoreOptions.debuggerEnabled || !tracing) return;
        if(trace.size()==1024)trace.removeFirst();
        trace.addLast(String.format("%08X  %08X  %s",pc,opcode,MipsDisassembler.format(opcode,pc)));
    }
    public void memoryAccess(int address,int length,boolean write) {
        if (!CoreOptions.debuggerEnabled || fetching || watches.isEmpty()) return;
        for (Watch w : watches) if ((write?w.write:w.read) && (long)address < (long)w.address+w.length && (long)address+length>w.address) {
            pause(String.format("%s %08X at PC %08X",write?"Write":"Read",address,cpu.pc)); return;
        }
    }
    static int physical(int address) {
        return (address & 0xC0000000)==0x80000000 ? address & 0x1FFFFFFF : address;
    }
    byte[] read(int address,int length) {
        if(length<0 || length>65536)throw new IllegalArgumentException("Read up to 64 KB at once");
        byte[] result=new byte[length];
        for(int i=0;i<length;i++) result[i]=Main.mem.debugByte(physical(address+i));
        return result;
    }
    void write(int address,byte[] bytes) {
        requirePaused(); int p=physical(address);
        if(!Main.mem.isRam(p,bytes.length))throw new IllegalArgumentException("Writes must stay within RDRAM and outside hardware registers");
        d3d11.framebuffer.D3DFramebuffers.read(p,bytes.length);
        d3d11.framebuffer.D3DFramebuffers.write(p,bytes.length);
        Main.mem.RDRAM.duplicate().clear().position(p).put(bytes);
        for(int i=p&~3;i<(long)p+bytes.length;i+=4)cpu.invalidateCode(i);
    }
    void setPc(int value) { requirePaused(); if((value&3)!=0)throw new IllegalArgumentException("PC must be word aligned"); cpu.pc=value; cpu.inDelaySlot=false; }
}
