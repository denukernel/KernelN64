package kerneln64.scripting;

import kerneln64.Main;
import kerneln64.state.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import java.io.*;
import java.util.*;
import plugin.ControllerPlugin.Buttons;

/** Explicit emulator APIs. No device reads or Java/file-system bridge is exposed. */
final class LuaApi {
    @FunctionalInterface private interface Call { Varargs invoke(Varargs args) throws Exception; }
    private static void bind(LuaTable table,String name,Call call){table.set(name,new VarArgFunction(){@Override public Varargs invoke(Varargs args){try{return call.invoke(args);}catch(LuaError error){throw error;}catch(Exception error){throw new LuaError(error);}}});}
    private static final String[] BUTTONS={"A","B","Z","Start","Up","Down","Left","Right","L","R","CUp","CDown","CLeft","CRight"};
    private static final int[] MASKS={Buttons.A_BUTTON,Buttons.B_BUTTON,Buttons.Z_TRIG,Buttons.START_BUTTON,Buttons.U_DPAD,Buttons.D_DPAD,Buttons.L_DPAD,Buttons.R_DPAD,Buttons.L_TRIG,Buttons.R_TRIG,Buttons.U_CBUTTON,Buttons.D_CBUTTON,Buttons.L_CBUTTON,Buttons.R_CBUTTON};
    static void install(LuaScripts.Session s){
        LuaTable memory=new LuaTable(),emu=new LuaTable(),event=new LuaTable(),joypad=new LuaTable(),states=new LuaTable();
        s.globals.set("memory",memory);s.globals.set("emu",emu);s.globals.set("event",event);s.globals.set("joypad",joypad);s.globals.set("savestate",states);
        bind(s.globals,"print",args->{if(s.prints++<64){StringBuilder line=new StringBuilder();for(int i=1;i<=Math.min(args.narg(),32);i++){if(i>1)line.append('\t');String part=args.arg(i).tojstring();line.append(part,0,Math.min(1024,part.length()));if(line.length()>1024)break;}LuaScripts.log(s,line.toString());}return LuaValue.NONE;});
        bind(emu,"framecount",a->LuaValue.valueOf(Main.video.currentFrame));
        bind(emu,"romname",a->LuaValue.valueOf(Main.currentRomFile.getName()));
        bind(emu,"romhash",a->LuaValue.valueOf(Main.currentRomHash));
        bind(emu,"ram_size",a->LuaValue.valueOf(s.memory.RDRAM.capacity()));
        bind(emu,"pause",a->{s.cpu.pauseState.manual(true);kerneln64.ui.EmulationControl.refreshTitle();return LuaValue.NONE;});
        bind(emu,"resume",a->{s.cpu.debugger.resume();kerneln64.ui.EmulationControl.refreshTitle();return LuaValue.NONE;});
        bind(emu,"stop",a->{s.stopRequested=true;return LuaValue.NONE;});
        bind(emu,"getregister",a->{String name=a.checkjstring(1).toLowerCase(Locale.ROOT);int value;
            if(name.equals("pc"))value=s.cpu.pc;else if(name.equals("hi"))value=(int)s.cpu.HI;else if(name.equals("lo"))value=(int)s.cpu.LO;
            else if(name.matches("r([0-9]|[12][0-9]|3[01])"))value=(int)s.cpu.GPR[Integer.parseInt(name.substring(1))];else throw new LuaError("unknown register; use pc, hi, lo or r0-r31");
            return LuaValue.valueOf((double)Integer.toUnsignedLong(value));});
        bind(event,"onframe",a->{s.onFrame=callback(a.arg1());return LuaValue.NONE;});
        bind(event,"onloadstate",a->{s.onLoad=callback(a.arg1());return LuaValue.NONE;});
        bind(event,"onstop",a->{s.onStop=callback(a.arg1());return LuaValue.NONE;});
        for(int bytes:new int[]{1,2,4}){
            bind(memory,"read_u"+(bytes*8),a->{int p=address(s,a.arg1(),bytes);coherentRead(p,bytes);long value=bytes==1?s.memory.RDRAM.get(p)&255:bytes==2?s.memory.RDRAM.getShort(p)&65535:Integer.toUnsignedLong(s.memory.RDRAM.getInt(p));return LuaValue.valueOf((double)value);});
            bind(memory,"read_s"+(bytes*8),a->{int p=address(s,a.arg1(),bytes);coherentRead(p,bytes);int value=bytes==1?s.memory.RDRAM.get(p):bytes==2?s.memory.RDRAM.getShort(p):s.memory.RDRAM.getInt(p);return LuaValue.valueOf(value);});
            bind(memory,"write_u"+(bytes*8),a->{int p=address(s,a.arg1(),bytes);long value=integer(a.arg(2));long max=(1L<<(bytes*8))-1,min=-(1L<<(bytes*8-1));if(value<min||value>max)throw new LuaError("value does not fit access width");LuaPatches.write(s,p,bytes,(int)value);return LuaValue.NONE;});
        }
        for(int bytes:new int[]{1,2,4}){
            bind(memory,"patch_u"+(bytes*8),a->{int p=address(s,a.arg1(),bytes);long value=integer(a.arg(2));long max=(1L<<(bytes*8))-1,min=-(1L<<(bytes*8-1));if(value<min||value>max)throw new LuaError("value does not fit access width");LuaPatches.write(s,p,bytes,(int)value);return LuaValue.NONE;});
        }
        bind(memory,"patch_float",a->{int p=address(s,a.arg1(),4);LuaPatches.write(s,p,4,Float.floatToRawIntBits((float)a.checkdouble(2)));return LuaValue.NONE;});
        bind(memory,"read_original_float",a->{int p=address(s,a.arg1(),4);return LuaValue.valueOf(Float.intBitsToFloat(LuaPatches.original(s,p,4)));});
        bind(memory,"read_float",a->{int p=address(s,a.arg1(),4);coherentRead(p,4);return LuaValue.valueOf(Float.intBitsToFloat(s.memory.RDRAM.getInt(p)));});
        bind(memory,"write_float",a->{int p=address(s,a.arg1(),4);LuaPatches.write(s,p,4,Float.floatToRawIntBits((float)a.checkdouble(2)));return LuaValue.NONE;});
        bind(memory,"read_bytes",a->{int count=a.checkint(2);if(count<1||count>4096)throw new LuaError("read_bytes count must be 1-4096");int p=address(s,a.arg1(),count);coherentRead(p,count);LuaTable values=new LuaTable();for(int i=0;i<count;i++)values.set(i+1,LuaValue.valueOf(s.memory.RDRAM.get(p+i)&255));return values;});
        bind(joypad,"set",a->{int port=port(a.arg(2));LuaTable values=a.checktable(1);int mask=0,value=0;for(int i=0;i<BUTTONS.length;i++){LuaValue entry=values.get(BUTTONS[i]);if(!entry.isnil()){mask|=MASKS[i];if(entry.checkboolean())value|=MASKS[i];}}s.inputs[port]=new LuaScripts.Input(mask,value,axis(values.get("x")),axis(values.get("y")));return LuaValue.NONE;});
        bind(joypad,"clear",a->{s.inputs[port(a.arg1())]=null;return LuaValue.NONE;});
        bind(joypad,"get",a->{int port=port(a.arg1());Buttons keys=new Buttons();if(Main.pif.controllerPlugin!=null)Main.pif.controllerPlugin.getKeys(port,keys);LuaScripts.applyInput(port,keys);LuaTable values=new LuaTable();for(int i=0;i<BUTTONS.length;i++)values.set(BUTTONS[i],LuaValue.valueOf((keys.value&MASKS[i])!=0));values.set("x",LuaValue.valueOf(keys.getXAxis()));values.set("y",LuaValue.valueOf(keys.getYAxis()));return values;});
        bind(states,"save",a->{int slot=slot(a.arg1());s.budget.hostIo(()->{try{SaveStates.save(StateSlots.current().path(slot));}catch(IOException error){throw new UncheckedIOException(error);}});return LuaValue.NONE;});
        bind(states,"load",a->{int slot=slot(a.arg1());s.budget.hostIo(()->{try{SaveStates.load(StateSlots.current().path(slot));}catch(IOException error){throw new UncheckedIOException(error);}});return LuaValue.NONE;});
    }
    private static LuaValue callback(LuaValue value){if(!value.isnil())value.checkfunction();return value;}
    private static long integer(LuaValue value){double n=value.checkdouble();if(!Double.isFinite(n)||n!=Math.rint(n)||n<Long.MIN_VALUE||n>Long.MAX_VALUE)throw new LuaError("expected a finite integer");return (long)n;}
    private static int address(LuaScripts.Session s,LuaValue value,int length){
        long number=integer(value);if(number<Integer.MIN_VALUE||number>0xffffffffL)throw new LuaError("address must fit 32 bits");
        int raw=(int)number;int p=(raw&0xE0000000)==0x80000000||(raw&0xE0000000)==0xA0000000?raw&0x1fffffff:raw;
        if(!s.memory.isRam(p,length))throw new LuaError("address is outside RDRAM; device and TLB-mapped accesses are unavailable");
        return p;
    }
    private static void coherentRead(int p,int length){d3d11.framebuffer.D3DFramebuffers.read(p,length);}
    static int read(LuaScripts.Session s,int p,int bytes){
        coherentRead(p,bytes);
        return bytes==1?s.memory.RDRAM.get(p)&255:bytes==2?s.memory.RDRAM.getShort(p)&65535:s.memory.RDRAM.getInt(p);
    }
    static void write(LuaScripts.Session s,int p,int bytes,int value){
        d3d11.framebuffer.D3DFramebuffers.write(p,bytes);
        if(bytes==1)s.memory.RDRAM.put(p,(byte)value);else if(bytes==2)s.memory.RDRAM.putShort(p,(short)value);else s.memory.RDRAM.putInt(p,value);
        for(int at=p&~3;at<(long)p+bytes;at+=4)s.cpu.invalidateCode(at);
    }
    private static int port(LuaValue value){long port=value.isnil()?1:integer(value);if(port<1||port>4)throw new LuaError("controller must be 1-4");return (int)port-1;}
    private static Integer axis(LuaValue value){if(value.isnil())return null;long n=integer(value);if(n< -128||n>127)throw new LuaError("axis must be -128 to 127");return (int)n;}
    private static int slot(LuaValue value){long slot=integer(value);if(slot<1||slot>10)throw new LuaError("save slot must be 1-10");return (int)slot;}
    private LuaApi() {}
}

