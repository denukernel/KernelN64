package kerneln64.scripting;

import kerneln64.Main;
import kerneln64.cpu.Cpu;
import kerneln64.memory.Memory;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseMathLib;
import java.util.*;
import java.util.concurrent.*;
import plugin.ControllerPlugin.Buttons;

/** One explicitly started Lua session. Guest APIs execute only at CPU boundaries. */
public final class LuaScripts {
    public static final int MAX_SOURCE = 262144;
    private static volatile Session active;
    private static final java.util.concurrent.atomic.AtomicLong requests = new java.util.concurrent.atomic.AtomicLong();
    private static volatile String status = "no script running";
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    public record Snapshot(String status, String output) {}
    public static synchronized Snapshot snapshot() { return new Snapshot(status, String.join("\n", lines)); }
    public static synchronized void clearLog() { lines.clear(); }
    static synchronized void log(String text) {
        String bounded = text.length() > 1024 ? text.substring(0,1024) + "..." : text;
        if (lines.size() == 500) lines.removeFirst();
        lines.addLast(bounded);
    }
    public static boolean running() { return active != null; }
    public static Future<?> run(String source, String name) {
        if (source.length() > MAX_SOURCE) throw new IllegalArgumentException("script is too large (maximum 256k characters)");
        Cpu cpu = Main.cpu;
        if (cpu == null || !cpu.running || Main.currentRomHash == null) throw new IllegalStateException("open a rom first");
        long ticket=requests.incrementAndGet();
        return cpu.submitCommand(() -> {
            if (Main.cpu != cpu || !cpu.running || ticket!=requests.get()) return;
            if (active != null) finish(active,"replaced");
            Session session = new Session(cpu, name == null ? "editor.lua" : name);
            active = session;
            status = "running: " + session.name;
            session.execute(() -> session.globals.load(source,session.name).call());
        });
    }
    /** Immediately visible to the VM instruction hook, even while the CPU is inside Lua. */
    public static void requestStop() { requests.incrementAndGet();Session session=active; if(session!=null)session.stopRequested=true; }
    public static void service(Cpu cpu) {
        Session session=active;
        if(session==null || session.cpu!=cpu)return;
        if(session.stopRequested){finish(session,"stopped");return;}
        if(session.stateRestored){
            session.stateRestored=false;session.lastFrame=Main.video.currentFrame;
            if(!session.onLoad.isnil())session.execute(()->session.onLoad.call());
        }
        if(active!=session || session.stateRestored || cpu.isPaused())return;
        int frame=Main.video.currentFrame;
        if(frame!=session.lastFrame){session.lastFrame=frame;if(!session.onFrame.isnil())session.execute(()->session.onFrame.call(LuaValue.valueOf(frame)));}
    }
    public static void stateRestored() {
        Session session=active;
        if(session!=null){session.stateRestored=true;Arrays.fill(session.inputs,null);}
    }
    public static void stopped(Cpu cpu) { Session s=active;if(s!=null&&s.cpu==cpu)finish(s,"rom ended"); }
    public static void applyInput(int controller, Buttons keys) {
        Session session=active;
        if(session==null || session.stopRequested || controller<0 || controller>=4)return;
        Input input=session.inputs[controller];
        if(input!=null){keys.value=(keys.value&~input.mask)|input.value;if(input.x!=null)keys.setXAxis(input.x.byteValue());if(input.y!=null)keys.setYAxis(input.y.byteValue());}
    }
    record Input(int mask,int value,Integer x,Integer y) {}
    private static void finish(Session session,String why) {
        if(active!=session)return;
        active=null;Arrays.fill(session.inputs,null);session.stopRequested=false;
        session.prints=session.ioCalls=0;
        if(!session.onStop.isnil())try{session.budget.begin();session.onStop.call();}catch(RuntimeException|ScriptAbort|StackOverflowError error){log("onstop: "+error.getMessage());}
        status=why+": "+session.name;log(status);
    }
    private static final class ScriptAbort extends Error { ScriptAbort(String message){super(message);} }
    static final class Session {
        final Cpu cpu;final Memory memory;final String name;final Globals globals=new Globals();
        final Budget budget=new Budget(this);final Input[] inputs=new Input[4];
        LuaValue onFrame=LuaValue.NIL,onLoad=LuaValue.NIL,onStop=LuaValue.NIL;
        volatile boolean stopRequested;
        boolean stateRestored;int lastFrame,prints,ioCalls;
        Session(Cpu cpu,String name){
            this.cpu=cpu;memory=Main.mem;this.name=name;lastFrame=Main.video.currentFrame;
            globals.load(new BaseLib());globals.load(new PackageLib());globals.load(new TableLib());globals.load(new StringLib());globals.load(new JseMathLib());globals.load(new Bit32Lib());globals.load(budget);
            LoadState.install(globals);LuaC.install(globals);
            for(String unavailable:new String[]{"io","os","luajava","debug","package","require","dofile","loadfile","load","collectgarbage","coroutine"})globals.set(unavailable,LuaValue.NIL);
            LuaApi.install(this);
        }
        void execute(Runnable action){
            budget.begin();prints=ioCalls=0;
            try {action.run();budget.verify();if(stopRequested)finish(this,"stopped");}
            catch(ScriptAbort error){log(error.getMessage());finish(this,stopRequested?"stopped":"execution limit reached");}
            catch(RuntimeException|StackOverflowError error){log("script error: "+error.getMessage());finish(this,"script error");}
        }
    }
    static final class Budget extends DebugLib {
        final Session session;int remaining;long deadline;
        Budget(Session session){this.session=session;}
        void begin(){remaining=100000;deadline=System.nanoTime()+150_000_000L;}
        void verify(){if(remaining<0 || System.nanoTime()>deadline)throw new ScriptAbort("lua callback exceeded its execution budget");}
        void hostIo(Runnable action){
            if(++session.ioCalls>2)throw new LuaError("at most two save-slot operations per callback");
            long before=System.nanoTime();try{action.run();}finally{deadline+=System.nanoTime()-before;}
        }
        @Override public void onInstruction(int pc,Varargs values,int top){
            if(session.stopRequested)throw new ScriptAbort("script stop requested");
            if(--remaining<0 || ((remaining&255)==0 && System.nanoTime()>deadline))throw new ScriptAbort("lua callback exceeded its execution budget");
            super.onInstruction(pc,values,top);
        }
    }
    private LuaScripts() {}
}
