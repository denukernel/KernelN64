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
import java.util.concurrent.atomic.AtomicLong;
import plugin.ControllerPlugin.Buttons;

/** Independent Lua VMs, dispatched in registration order at CPU boundaries. */
public final class LuaScripts {
    public static final int MAX_SOURCE = 262144, MAX_SCRIPTS = 32;
    private static volatile List<Session> active = List.of();
    private static final LinkedHashMap<String,Session> history = new LinkedHashMap<>();
    private static final AtomicLong stopEpoch = new AtomicLong();
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static long restoreVersion;
    private static volatile boolean pending;
    private static int observedFrame;
    public record Script(String id,String name,String source) {
        public Script {
            Objects.requireNonNull(id);Objects.requireNonNull(name);Objects.requireNonNull(source);
            if(id.isBlank()||id.length()>256||name.isBlank()||name.length()>256)throw new IllegalArgumentException("invalid script name or id");
            if(source.length()>MAX_SOURCE)throw new IllegalArgumentException("script is too large (maximum 256k characters)");
        }
    }
    public record ScriptSnapshot(String id,String name,String status,String output,String errors) {}
    public record ScriptStatus(String id,String name,String status) {}
    public record Snapshot(String status,String output) {}
    public static synchronized Snapshot snapshot(){return new Snapshot(active.size()+" script(s) running",String.join("\n",lines));}
    public static synchronized List<ScriptSnapshot> scripts(){
        return history.values().stream().map(s->new ScriptSnapshot(s.id,s.name,s.status,String.join("\n",s.lines),String.join("\n",s.errors))).toList();
    }
    public static synchronized List<ScriptStatus> statuses(){return history.values().stream().map(s->new ScriptStatus(s.id,s.name,s.status)).toList();}
    public static synchronized ScriptSnapshot script(String id){Session s=history.get(id);return s==null?null:new ScriptSnapshot(s.id,s.name,s.status,String.join("\n",s.lines),String.join("\n",s.errors));}
    public static synchronized List<Script> definitions(){return active.stream().map(s->new Script(s.id,s.name,s.source)).toList();}
    public static synchronized void clearLog(){lines.clear();for(Session s:history.values()){s.lines.clear();s.errors.clear();}}
    public static synchronized void clearLog(String id){Session s=history.get(id);if(s!=null){s.lines.clear();s.errors.clear();}}
    private static void append(ArrayDeque<String> buffer,String text){if(buffer.size()==500)buffer.removeFirst();buffer.addLast(text.length()>1024?text.substring(0,1024)+"...":text);}
    static synchronized void log(String text){append(lines,text);}
    static synchronized void log(Session session,String text){append(session.lines,text);append(lines,"["+session.name+"] "+text);}
    private static synchronized void error(Session session,String text){append(session.errors,text);append(lines,"["+session.name+"] "+text);}
    public static boolean running(){return !active.isEmpty();}
    public static Future<?> run(String source,String name){return run(new Script(UUID.randomUUID().toString(),name==null?"editor.lua":name,source));}
    public static Future<?> run(Script script){
        Cpu cpu=requireCpu();long epoch=stopEpoch.get();
        return cpu.submitCommand(()->{if(Main.cpu==cpu&&cpu.running&&epoch==stopEpoch.get())start(cpu,script,epoch);});
    }
    /** Validate the complete group before stopping anything; start in saved order. */
    public static Future<?> runGroup(List<Script> scripts){
        List<Script> group=validateGroup(scripts);Cpu cpu=requireCpu();requestStop();long epoch=stopEpoch.get();
        return cpu.submitCommand(()->{
            if(Main.cpu!=cpu||!cpu.running||epoch!=stopEpoch.get())return;
            for(Session s:reversedActive())finish(s,"stopped");
            for(Script script:group){if(epoch!=stopEpoch.get())break;start(cpu,script,epoch);}
        });
    }
    static List<Script> validateGroup(List<Script> scripts){
        List<Script> group=List.copyOf(scripts);Set<String> ids=new HashSet<>();long size=0;
        if(group.isEmpty()||group.size()>MAX_SCRIPTS)throw new IllegalArgumentException("use 1-32 scripts per group");
        for(Script script:group){if(!ids.add(script.id()))throw new IllegalArgumentException("duplicate script id");size+=script.source().length();}
        if(size>4L*MAX_SOURCE)throw new IllegalArgumentException("group is too large (maximum 1m source characters)");
        return group;
    }
    private static Cpu requireCpu(){Cpu cpu=Main.cpu;if(cpu==null||!cpu.running||Main.currentRomHash==null)throw new IllegalStateException("open a rom first");return cpu;}
    private static void start(Cpu cpu,Script script,long epoch){
        for(Session existing:active)if(existing.id.equals(script.id()))finish(existing,"restarted");
        if(active.size()>=MAX_SCRIPTS)throw new IllegalStateException("at most 32 scripts may run together");
        Session session=new Session(cpu,script);
        synchronized(LuaScripts.class){
            if(epoch!=stopEpoch.get())return;
            history.remove(session.id);history.put(session.id,session);
            List<Session> next=new ArrayList<>(active);next.add(session);active=List.copyOf(next);
            while(history.size()>64){String oldest=history.entrySet().stream().filter(e->!active.contains(e.getValue())).map(Map.Entry::getKey).findFirst().orElse(null);if(oldest==null)break;history.remove(oldest);}
        }
        session.execute(()->session.globals.load(script.source(),session.name).call());
        pending=true;
    }
    /** Visible immediately to instruction hooks, including a VM currently inside Lua. */
    public static synchronized void requestStop(){stopEpoch.incrementAndGet();for(Session s:active)s.stopRequested=true;pending=true;}
    public static void service(Cpu cpu){
        int currentFrame=Main.video.currentFrame;
        if(!pending&&currentFrame==observedFrame)return;
        pending=false;observedFrame=currentFrame;
        long version=restoreVersion;
        for(Session session:reversedActive())if(session.cpu==cpu&&session.stopRequested)finish(session,"stopped");
        for(Session session:active){
            if(session.cpu!=cpu||!active.contains(session))continue;
            if(session.stopRequested){finish(session,"stopped");continue;}
            if(session.stateRestored){
                session.stateRestored=false;session.lastFrame=Main.video.currentFrame;
                if(!session.onLoad.isnil())session.execute(()->session.onLoad.call());
            }
            if(restoreVersion!=version)break;
            if(!active.contains(session)||session.stateRestored||cpu.isPaused())continue;
            int frame=Main.video.currentFrame;
            if(frame!=session.lastFrame){session.lastFrame=frame;if(!session.onFrame.isnil())session.execute(()->session.onFrame.call(LuaValue.valueOf(frame)));}
            if(restoreVersion!=version)break;
        }
    }
    public static void stateRestored(){LuaPatches.stateRestored();restoreVersion++;pending=true;for(Session s:active){s.stateRestored=true;Arrays.fill(s.inputs,null);}}
    public static void stopped(Cpu cpu){stopEpoch.incrementAndGet();for(Session s:reversedActive())if(s.cpu==cpu)finish(s,"rom ended");}
    public static void applyInput(int controller,Buttons keys){
        if(controller<0||controller>=4)return;
        for(Session s:active){
            if(s.stopRequested)continue;
            Input input=s.inputs[controller];
            if(input!=null){keys.value=(keys.value&~input.mask)|input.value;if(input.x!=null)keys.setXAxis(input.x.byteValue());if(input.y!=null)keys.setYAxis(input.y.byteValue());}
        }
    }
    private static List<Session> reversedActive(){List<Session> result=new ArrayList<>(active);Collections.reverse(result);return result;}
    record Input(int mask,int value,Integer x,Integer y) {}
    private static void finish(Session session,String why){
        synchronized(LuaScripts.class){
            if(!active.contains(session))return;
            List<Session> next=new ArrayList<>(active);next.remove(session);active=List.copyOf(next);
            session.status=why;
        }
        Arrays.fill(session.inputs,null);session.stopRequested=false;session.prints=session.ioCalls=0;session.finishing=true;
        try{if(!session.onStop.isnil()){session.budget.begin();session.onStop.call();session.budget.verify();}}
        catch(RuntimeException|ScriptAbort|StackOverflowError failure){error(session,"onstop: "+failure.getMessage());}
        finally{try{LuaPatches.release(session);}finally{session.finishing=false;}}
        log(session,why);
    }
    private static final class ScriptAbort extends Error{ScriptAbort(String message){super(message);}}
    static final class Session {
        final Cpu cpu;final Memory memory;final String id,name,source;final Globals globals=new Globals();
        final Budget budget=new Budget(this);final Input[] inputs=new Input[4];
        final ArrayDeque<String> lines=new ArrayDeque<>(),errors=new ArrayDeque<>();
        LuaValue onFrame=LuaValue.NIL,onLoad=LuaValue.NIL,onStop=LuaValue.NIL;
        volatile boolean stopRequested;
        volatile String status="running";
        boolean stateRestored,finishing;int lastFrame,prints,ioCalls;
        Session(Cpu cpu,Script script){
            this.cpu=cpu;memory=Main.mem;id=script.id();name=script.name();source=script.source();lastFrame=Main.video.currentFrame;
            globals.load(new BaseLib());globals.load(new PackageLib());globals.load(new TableLib());globals.load(new StringLib());globals.load(new JseMathLib());globals.load(new Bit32Lib());globals.load(budget);
            LoadState.install(globals);LuaC.install(globals);
            for(String unavailable:new String[]{"io","os","luajava","debug","package","require","dofile","loadfile","load","collectgarbage","coroutine"})globals.set(unavailable,LuaValue.NIL);
            LuaApi.install(this);
        }
        void execute(Runnable action){
            budget.begin();prints=ioCalls=0;
            try{action.run();budget.verify();if(stopRequested)finish(this,"stopped");}
            catch(ScriptAbort failure){if(!stopRequested)error(this,failure.getMessage());finish(this,stopRequested?"stopped":"execution limit reached");}
            catch(RuntimeException|StackOverflowError failure){error(this,"script error: "+failure.getMessage());finish(this,"script error");}
        }
    }
    static final class Budget extends DebugLib {
        final Session session;int remaining;long deadline;
        Budget(Session session){this.session=session;}
        void begin(){remaining=100000;deadline=System.nanoTime()+150_000_000L;}
        void verify(){if(remaining<0||System.nanoTime()>deadline)throw new ScriptAbort("lua callback exceeded its execution budget");}
        void hostIo(Runnable action){
            if(++session.ioCalls>2)throw new LuaError("at most two save-slot operations per callback");
            long before=System.nanoTime();try{action.run();}finally{deadline+=System.nanoTime()-before;}
        }
        @Override public void onInstruction(int pc,Varargs values,int top){
            if(session.stopRequested)throw new ScriptAbort("script stop requested");
            if(--remaining<0||((remaining&255)==0&&System.nanoTime()>deadline))throw new ScriptAbort("lua callback exceeded its execution budget");
            super.onInstruction(pc,values,top);
        }
    }
    private LuaScripts(){}
}
