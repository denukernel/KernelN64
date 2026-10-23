package kerneln64.cheats;

import kerneln64.Main;
import java.nio.ByteBuffer;
import java.util.*;

/** Bounded capture of executed original Fast3D commands; no RAM opcode scanning. */
public final class Sm64SceneGfxCodes {
    private record Command(int address,int w0,int w1) {}
    private static final class Capture {
        final ByteBuffer ram;final int levelAddress,level,base;final String hash;
        final Map<Integer,Command> commands=new LinkedHashMap<>();
        Capture(ByteBuffer ram,int address,int base){this.ram=ram;levelAddress=address;level=ram.getShort(address)&65535;this.base=base;hash=Main.currentRomHash;}
    }
    private static volatile Capture capture;
    private Sm64SceneGfxCodes() {}
    /** Call on the emulation thread. */
    public static void begin() {
        if(capture!=null)throw new IllegalStateException("a gfx capture is already running");
        if(!Main.sm64UsCheats || !(Main.video.gfxPlugin instanceof kernel64gl.Kernel64GLPlugin))throw new IllegalStateException("gfx capture needs sm64 us and Kernel64GL");
        if(Main.cpu.isPaused())throw new IllegalStateException("resume gameplay before building gfx codes");
        var g=kernel64gl.engine.core.Rsp.gsp;
        int address=Main.decompCheats==null?0x32DDF8:Main.decompCheats.address(0x32DDF8,2);
        if(g.segment[7]<0x1000 || g.segment[7]>=Main.mem.RDRAM.capacity()-16)throw new IllegalStateException("no loaded level segment 7");
        capture=new Capture(Main.mem.RDRAM,address,g.segment[7]);
    }
    /** Called at command dispatch, before execution, with physical addresses. */
    public static void observe(int address,int w0,int w1,int base,boolean fast3d) {
        Capture c=capture;if(c==null || !fast3d || base!=c.base || address<c.base || address-c.base>=0x40000)return;
        int op=w0>>>24;
        if((op==3 || op==4 || op==0xBF || op==0xB6 || op==0xB7) && c.commands.size()<2048)
            c.commands.putIfAbsent(address,new Command(address,w0,w1));
    }
    public static void cancel(){capture=null;}
    /** Call on the emulation thread after rendering several frames. */
    public static List<CheatRepository.Entry> finish() {
        Capture c=capture;capture=null;
        if(c==null)throw new IllegalStateException("no gfx capture");
        if(c.ram!=Main.mem.RDRAM || !Objects.equals(c.hash,Main.currentRomHash) || c.level!=(c.ram.getShort(c.levelAddress)&65535)
            || kernel64gl.engine.core.Rsp.gsp.segment[7]!=c.base)throw new IllegalStateException("the scene changed during capture; try again");
        return generate(c);
    }
    private static List<CheatRepository.Entry> generate(Capture c) {
        var anchors=c.commands.values().stream().filter(v->v.w0>>>24==4).limit(2).toList();
        if(anchors.size()<2)throw new IllegalStateException("not enough original Fast3D vertex loads; move the camera and try again");
        for(Command v:c.commands.values())if(c.ram.getInt(v.address)!=v.w0 || c.ram.getInt(v.address+4)!=v.w1)throw new IllegalStateException("display list changed during capture");
        StringBuilder guards=new StringBuilder();line(guards,0xD1,c.levelAddress,c.level);line(guards,0xD1,c.levelAddress,c.level);
        for(Command v:anchors){guardWord(guards,v.address,v.w0);guardWord(guards,v.address+4,v.w1);}
        Map<Integer,Integer> raised=new LinkedHashMap<>(),stretched=new LinkedHashMap<>(),reversed=new LinkedHashMap<>(),holes=new LinkedHashMap<>(),red=new LinkedHashMap<>(),blue=new LinkedHashMap<>(),twoSided=new LinkedHashMap<>(),flat=new LinkedHashMap<>();
        int vertexStep=Math.max(1,(int)c.commands.values().stream().filter(v->v.w0>>>24==4).count()/32);
        int triangleStep=Math.max(1,(int)c.commands.values().stream().filter(v->v.w0>>>24==0xBF).count()/32);
        int vertexIndex=0,triangleIndex=0;
        for(Command v:c.commands.values()) {
            int op=v.w0>>>24;
            if(op==4 && (v.w1>>>24)==7) {
                if(vertexIndex++%vertexStep!=0)continue;
                int at=c.base+(v.w1&0xFFFFFF),n=((v.w0>>>20)&15)+1,start=(v.w0>>>16)&15;
                if(start+n>16 || at<c.base || at>c.ram.capacity()-n*16)continue;
                for(int i=0;i<Math.min(n,2) && raised.size()<64;i++) {int y=at+i*16+2;int original=c.ram.getShort(y);raised.putIfAbsent(y,Math.min(32767,original+80)&65535);stretched.putIfAbsent(y,Math.max(-32768,Math.min(32767,original*2))&65535);}
            }
            if(op==0xBF && reversed.size()<64) {
                if(triangleIndex++%triangleStep!=0)continue;
                int a=v.w1>>>16&255,b=v.w1>>>8&255,d=v.w1&255;
                if(a%10!=0 || b%10!=0 || d%10!=0 || Math.max(a,Math.max(b,d))>150)continue;
                int flip=v.w1&0xFF000000|a<<16|d<<8|b,zero=v.w1&0xFF000000|a<<16|a<<8|a;
                reversed.put(v.address+4,flip>>>16);reversed.put(v.address+6,flip&65535);holes.put(v.address+4,zero>>>16);holes.put(v.address+6,zero&65535);
            }
            if(op==3 && (v.w1>>>24)==7 && (v.w0>>>16&1)==0 && (v.w0>>>16&255)>=0x86 && (v.w0>>>16&255)<=0x94 && red.size()<24) {
                int at=c.base+(v.w1&0xFFFFFF);if(at<c.base || at>c.ram.capacity()-12)continue;
                for(int offset:new int[]{0,4}){red.put(at+offset,0xFF40);red.put(at+offset+2,0x4000|(c.ram.get(at+offset+3)&255));blue.put(at+offset,0x4040);blue.put(at+offset+2,0xFF00|(c.ram.get(at+offset+3)&255));}
            }
            if((op==0xB6 || op==0xB7) && twoSided.size()<32) {
                int uncull=op==0xB6?v.w1|0x3000:v.w1&~0x3000;
                int smooth=op==0xB6?v.w1|0x200:v.w1&~0x200;
                twoSided.put(v.address+4,uncull>>>16);twoSided.put(v.address+6,uncull&65535);flat.put(v.address+4,smooth>>>16);flat.put(v.address+6,smooth&65535);
            }
        }
        List<CheatRepository.Entry> out=new ArrayList<>();
        add(out,c,guards,"Vertices raised",raised,"adds 80 to up to 64 executed segment-7 vertex Y coordinates; collision stays unchanged","vertices");
        add(out,c,guards,"Vertex height doubled",stretched,"doubles those signed vertex Y coordinates with s16 saturation; collision stays unchanged","vertices");
        add(out,c,guards,"Triangle winding reversed",reversed,"swaps the second and third valid cache indices in up to 32 BF triangle commands; backface culling can hide surfaces","triangles");
        add(out,c,guards,"Triangle batch removed",holes,"collapses up to 32 BF triangles to zero area using an already referenced index; visual holes do not change collision","triangles");
        add(out,c,guards,"Red lighting",red,"changes RGB and duplicated RGB bytes in executed segment-7 light data; directions and padding stay unchanged","lighting");
        add(out,c,guards,"Blue lighting",blue,"changes RGB and duplicated RGB bytes in executed segment-7 light data; directions and padding stay unchanged","lighting");
        add(out,c,guards,"Two-sided geometry",twoSided,"removes F3D cull flags from B7 set commands and adds them to B6 clear commands","geometry");
        add(out,c,guards,"Flat shading geometry",flat,"clears F3D smooth-shading bit 0x200 through captured B6/B7 commands","geometry");
        if(out.isEmpty())throw new IllegalStateException("no supported scene targets");return out;
    }
    private static void add(List<CheatRepository.Entry> out,Capture c,StringBuilder guards,String name,Map<Integer,Integer> writes,String detail,String group) {
        if(writes.isEmpty())return;
        for(Command v:c.commands.values())if(writes.containsKey(v.address) || writes.containsKey(v.address+2))return;
        // A captured vertex/light allocation must not overlap the immutable anchors.
        for(int address:writes.keySet())if(guards.toString().contains(String.format(Locale.ROOT,"%08X ",0xD1000000|address)))return;
        StringBuilder code=new StringBuilder();for(var write:writes.entrySet()){code.append(guards);line(code,0x81,write.getKey(),write.getValue());}
        String tag=String.format(Locale.ROOT,"level %d / %06X",c.level,c.base);
        var entry=new CheatRepository.Entry("GFX\\Captured scenes\\"+tag+String.format(Locale.ROOT," / %08X",c.commands.hashCode())+"\\"+name,
            "captured original Fast3D scene, ROM SHA-256 "+c.hash+". "+detail+". "+writes.size()+" halfword writes. level and two immutable executed vertex-load commands guard every write. only captured batches/materials are covered; effects can be occluded or overridden by later commands. disable before generating again. guards and allocation-aware undo require KernelN64. other engines/hardware untested. source: kernel64gl/engine/opcodes/F3d.java and the executed RSP input; N64 vertices are 16 bytes with signed xyz at offsets 0/2/4.",code.toString().strip(),false,"gfx-"+tag+"-"+group);
        entry.addressing=c.hash;out.add(entry);
    }
    private static void guardWord(StringBuilder out,int at,int word){line(out,0xD1,at,word>>>16);line(out,0xD1,at+2,word&65535);}
    private static void line(StringBuilder out,int type,int at,int value){out.append(String.format(Locale.ROOT,"%08X %04X%n",type<<24|at,value));}
}
