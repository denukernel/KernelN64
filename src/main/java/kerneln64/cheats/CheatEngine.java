package kerneln64.cheats;
import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cpu;
import kerneln64.Main;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** N64 GameShark and selected encrypted Xplorer64 codes, evaluated on the emulation thread at VI. */
public final class CheatEngine {
    private record Code(int command, int value) {}
    private static final List<Code> HEAD_PROGRAM=Arrays.stream(Sm64HeadStretch.PAIRS).map(pair->new Code(pair[0],pair[1])).toList();
    private static final List<Code> DISPLAY_LIST_GUARDS=Arrays.stream(Sm64DisplayListCodes.GUARDS).map(g->new Code(0xD1000000|g[0],g[1])).toList();
    private volatile List<List<Code>> programs = List.of();
    private volatile boolean button;
    public volatile int musicBurstShift;
    public int musicCountdown(Cpu cpu,int instruction,int normal){return Sm64BurstMusicCodes.countdown(cpu,trackedRam,musicBurstShift,instruction,normal);}
    private boolean runtimeReady;
    private boolean sm64Legacy;
    private Sm64DecompCodes build;
    private int[][] sampleGuardSignature=Sm64SampleCodes.SIGNATURE;
    private List<Code> headProgram=HEAD_PROGRAM;
    public void setSm64Build(Sm64DecompCodes profile) {
        build=profile;headProgram=HEAD_PROGRAM;sampleGuardSignature=Sm64SampleCodes.SIGNATURE;
        if(profile!=null)try{sampleGuardSignature=profile.signature(Sm64SampleCodes.SIGNATURE);}
        catch(IllegalArgumentException unsupported){sampleGuardSignature=null;}
        if(profile!=null)try {
            CheatEngine converted=new CheatEngine();converted.setSm64Compatibility(true);converted.setCodes(profile.port(Sm64HeadStretch.CODES));
            headProgram=converted.programs.get(0);
        }catch(IllegalArgumentException unsupported){headProgram=List.of();}
    }
    private ByteBuffer trackedRam;
    private final Map<Integer,Patch> patches=new LinkedHashMap<>();
    private static final class Patch {
        int original,last;
        final Map<List<Code>,Integer> owners=new LinkedHashMap<>();
        Patch(int original){this.original=this.last=original;}
    }
    public void setSm64Compatibility(boolean enabled){sm64Legacy=enabled;}
    static final String MARIO_HEAD_PROBLEM = "Use 811954DB 7644 as a standalone SM64 cheat, or select Unlimited head stretching (title screen). KernelN64 replaces the old malformed write with guarded spring-force patches; it cannot be embedded in a repeater or a larger instruction block.";
    static String knownProblem(String text) {
        return java.util.regex.Pattern.compile("(?im)^\\s*(?:CODE=)?(?:81|83|85|A1|A3|A5|89|F1|E1|E5)1954DB\\s+7644(?:\\s*(?:[#;].*)?)?$").matcher(text).find() ? MARIO_HEAD_PROBLEM : null;
    }
    static boolean replacesHeadCode(String text){return knownProblem(text)!=null && text.lines().filter(line->!line.isBlank()&&!line.strip().startsWith("#")&&!line.strip().startsWith(";")).count()==1;}
    private static boolean oldHeadCode(Code candidate){int type=candidate.command>>>24;return (candidate.command&0xFFFFFF)==0x1954DB && candidate.value==0x7644 && (type==0x81||type==0xA1||type==0x89||type==0xF1);}


    public String diagnosticCodes() {
        StringBuilder text = new StringBuilder();
        for (List<Code> program : programs) {
            for (Code code : program) text.append(String.format("%08X %04X%n", code.command, code.value));
            text.append('\n');
        }
        return text.toString();
    }
    record WriteFootprint(Map<Integer,Integer> bytes,long operations) {}
    WriteFootprint writeFootprint() {
        Map<Integer,Integer> bytes=new HashMap<>();long operations=0;
        for(List<Code> program:programs)for(int i=0;i<program.size();i++) {
            Code code=program.get(i);int type=code.command>>>24;operations++;
            if(type>=0xD0 && type<=0xD3)continue;
            if(type==0x50) {
                Code target=program.get(++i);int count=code.command>>>8&255;operations+=count;
                for(int n=0;n<count;n++)footprint(bytes,(target.command&0xFFFFFF)+n*(code.command&255),(target.command>>>24&1)+1,target.value+n*code.value);
            }else footprint(bytes,code.command&0xFFFFFF,(type&1)+1,code.value);
        }
        return new WriteFootprint(bytes,operations);
    }
    private static void footprint(Map<Integer,Integer> bytes,int address,int size,int value) {
        for(int i=0;i<size;i++){int b=value>>>((size-1-i)*8)&255;bytes.merge(address+i,b,(old,next)->old.equals(next)?old:-1);}
    }
    public void clear() { programs = List.of(); musicBurstShift=0; button = false; patches.clear();trackedRam=null;resetBootPhase(); }
    public void pressButton() { button = true; }
    public void resetBootPhase() { musicBurstShift=0;runtimeReady = false; }
    public boolean hasSamplePrograms() { return programs.stream().anyMatch(this::sampleProgram); }

    /** VI also runs during IPL3, while the boot code still verifies RDRAM. */
    public void applyRuntime(ByteBuffer ram, Cpu cpu) {
        List<List<Code>> selected=programs;
        reconcile(ram,cpu,selected);
        if (!runtimeReady) {
            if (cpu.cop0 == null || (cpu.cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_IE) == 0) return;
            // Latch the transition: games can temporarily mask interrupts during gameplay.
            runtimeReady = true;
        }
        execute(ram,cpu,false,selected);
    }

    public void load(Path path) throws IOException {
        if (Files.exists(path)) setCodes(Files.readString(path));
    }

    public void setCodes(String text) {
        text=Sm64BurstMusicCodes.upgrade(text);
        if(text.lines().anyMatch(line->line.strip().equalsIgnoreCase("[CHEAT]"))) {
            var entries=CheatRepository.parseStructure(text);
            var mixed=Sm64AudioMixer.compile(entries,build,sm64Legacy);
            if(mixed!=entries)text=CheatRepository.serialize(mixed);
        }
        List<List<Code>> parsed = new ArrayList<>();
        List<Code> block = new ArrayList<>();
        boolean enabled = true;
        boolean named = text.lines().anyMatch(line -> line.strip().equalsIgnoreCase("[CHEAT]"));
        int lineNumber = 0;
        for (String raw : text.split("\\R")) {
            lineNumber++;
            String line = raw.split("[#;]", 2)[0].trim();
            if (line.isEmpty() || line.equalsIgnoreCase("[CHEAT]")) {
                if (!block.isEmpty() && enabled) parsed.add(List.copyOf(block));
                block.clear();
                // Blank lines separate programs within a named entry. Its checkbox
                // applies to every program, until the next [CHEAT] header.
                if (!named || line.equalsIgnoreCase("[CHEAT]")) enabled = true;
                continue;
            }
            if (line.startsWith("NAME=") || line.startsWith("NOTE=") || line.startsWith("GROUP=") || line.startsWith("ADDRESSING=")) continue;
            if (line.startsWith("ENABLED=")) { enabled = Boolean.parseBoolean(line.substring(8)); continue; }
            if (line.startsWith("CODE=")) line = line.substring(5).trim();
            if(sm64Legacy && line.matches("(?i)^EO[0-9a-f]{6}\\s+[0-9a-f]{4}.*"))line="E0"+line.substring(2);
            if(sm64Legacy && line.matches("(?i)[0-9a-f]{8}\\s+[0-9a-f]{4}\\s+.+"))line=line.replaceFirst("(?i)^([0-9a-f]{8}\\s+[0-9a-f]{4})\\s+.*$","$1");
            if(line.matches("(?i)[0-9a-f]{8}\\s+[0-9a-fx?]{4}.*") && (line.toLowerCase(java.util.Locale.ROOT).contains("x") || line.contains("?")))
                throw new IllegalArgumentException("Replace xx/?? with a concrete hexadecimal value on line "+lineNumber);
            if (!line.matches("(?i)[0-9a-f]{8}\\s+[0-9a-f]{4}"))
                throw new IllegalArgumentException("Invalid GameShark code on line " + lineNumber);
            String[] parts = line.split("\\s+");
            int command = (int) Long.parseLong(parts[0], 16);
            int value=Integer.parseInt(parts[1],16);
            int type = command >>> 24;
            // Legacy SM64 lists use E0/E4 at literal US RAM addresses; these
            // cannot be valid XP64 encrypted RDRAM addresses (whose encoded
            // low 24 bits start at 0x550000). Handle that dialect explicitly.
            boolean literalLegacy=sm64Legacy && (command&0xFFFFFF)>=0x100000 && (command&0xFFFFFF)<0x400000;
            if(literalLegacy && (type==0xE0 || type==0xE1 || type==0xE4 || type==0xE5)) {
                command=((type&1)==0?0xA0000000:0xA1000000)|(command&0xFFFFFF);type=command>>>24;
            }
            if(literalLegacy && (type==0xA8 || type==0xA9)) {command=0x88000000|((type&1)<<24)|(command&0xFFFFFF);type=command>>>24;}
            if(sm64Legacy && (type==0x82 || type==0x83 || type==0x84 || type==0x85 || type==0xA2 || type==0xA3 || type==0xA4 || type==0xA5)) {
                command=((type&0xF0)==0xA0?0xA0000000:0x80000000)|((type&1)<<24)|(command&0xFFFFFF);type=command>>>24;
            }
            // Xplorer64 encrypted byte/halfword writes and XP-button variants.
            if(type==0xE0 || type==0xE1 || type==0xE8 || type==0xE9 || type==0xC8 || type==0xC9) {
                command=decodeAddress(command);value=decodeValue(value);type=command>>>24;
            }
            if (type != 0xB0 && type != 0x80 && type != 0x81 && type != 0xA0 && type != 0xA1
                    && type != 0xD0 && type != 0xD1 && type != 0xD2 && type != 0xD3
                    && type != 0x50 && type != 0x88 && type != 0x89 && type != 0xF0 && type != 0xF1)
                throw new IllegalArgumentException(String.format("Unsupported GameShark type %02X on line %d", type, lineNumber));
            if(type==0x50 && (command&0xFFFF0000)!=0x50000000)throw new IllegalArgumentException("Invalid repeat header on line "+lineNumber+": expected 5000CCSS (the old 5032 camera prefix is not a standard repeater)");
            if(type==0xB0 && (command!=0xB0222618 || (value!=1 && value!=2)))throw new IllegalArgumentException("Invalid KernelN64 music countdown extension");
            if (type != 0x50 && ((command & 0xFFFFFF) >= 0x800000
                    || (!sm64Legacy && (type & 1) != 0 && (command & 1) != 0)))
                throw new IllegalArgumentException("Invalid RDRAM address or unaligned halfword on line " + lineNumber);
            block.add(new Code(command, value));
        }
        if (!block.isEmpty() && enabled) parsed.add(List.copyOf(block));
        if(sm64Legacy)for(int i=0;i<parsed.size();i++)if(parsed.get(i).size()==1 && oldHeadCode(parsed.get(i).get(0))){if(headProgram.isEmpty())throw new IllegalArgumentException("The title-screen code changed in this build");parsed.set(i,headProgram);}
        for (List<Code> program : parsed) {
            for(Code candidate:program) {
                int type=candidate.command>>>24;
                if(sm64Legacy && oldHeadCode(candidate))
                    throw new IllegalArgumentException(MARIO_HEAD_PROBLEM);
            }
            for (int i = 0; i < program.size(); i++) {
                int type = program.get(i).command >>> 24;
                if ((type == 0x50 || (type >= 0xD0 && type <= 0xD3)) && i + 1 == program.size())
                    throw new IllegalArgumentException("Repeat/conditional code needs a following code in the same block");
                if (type == 0x50) {
                    Code repeat = program.get(i), write = program.get(++i);
                    int writeType = write.command >>> 24;
                    int count = (repeat.command >>> 8) & 0xFF;
                    int step = repeat.command & 0xFF;
                    long end = (write.command & 0xFFFFFFL) + Math.max(0, count - 1) * step + ((writeType & 1) + 1);
                    if ((writeType != 0x80 && writeType != 0x81 && writeType != 0xA0 && writeType != 0xA1)
                            || end > 0x800000 || (!sm64Legacy && (writeType & 1) != 0 && (step & 1) != 0))
                        throw new IllegalArgumentException("Invalid repeat target or RDRAM range");
                }
            }
        }
        musicBurstShift=0;
        programs = List.copyOf(parsed); // Publish only after the entire input validates.
    }

    static int decodeAddress(int value) {
        int result=(value^0x68000000)&0xFF000000;
        for(int shift=16,key=0x81;shift>=0;shift-=8,key++)result|=(((((value>>>shift)&255)+0x2B)^key)&255)<<shift;
        return result;
    }
    static int decodeValue(int value) {
        return (((((value>>>8)+0x2B)^0x84)&255)<<8)|(((value+0x2B)^0x85)&255);
    }
    public void apply(ByteBuffer ram, Cpu cpu, boolean boot) {
        List<List<Code>> selected=programs;reconcile(ram,cpu,selected);execute(ram,cpu,boot,selected);
    }
    /** Guest memory is modified only by this method/VI on the emulation thread. */
    public void synchronize(ByteBuffer ram,Cpu cpu) { musicBurstShift=0;reconcile(ram,cpu,programs); }
    private void reconcile(ByteBuffer ram,Cpu cpu,List<List<Code>> selected) {
        if(trackedRam!=ram){musicBurstShift=0;patches.clear();trackedRam=ram;}
        Set<List<Code>> wanted=new HashSet<>(selected);
        Map<List<Code>,Boolean> sampleBooks=new IdentityHashMap<>();
        for(Patch patch:patches.values())for(List<Code> owner:patch.owners.keySet())
            if((sampleProgram(owner) || displayListProgram(owner) || sceneProgram(owner)) && !sampleBooks.containsKey(owner))sampleBooks.put(owner,sampleBookAlive(owner,ram));
        boolean headOverlay=Sm64HeadStretch.matches(ram,build);
        var iterator=patches.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();Patch patch=entry.getValue();
            // When DMA replaces the title overlay, discard its undo history even
            // if an individual new byte happens to equal the patched zero byte.
            int address=entry.getKey();
            // Generated ADPCM programs have immutable bank/book guards. Once
            // DMA reuses that allocation, its old coefficients no longer belong
            // here, even if a new byte happens to equal our last patched byte.
            patch.owners.keySet().removeIf(owner->Boolean.FALSE.equals(sampleBooks.get(owner)));
            if(patch.owners.isEmpty()){iterator.remove();continue;}
            if(!headOverlay && patch.owners.containsKey(headProgram)){iterator.remove();continue;}
            d3d11.framebuffer.D3DFramebuffers.read(entry.getKey(),1);
            // Overlay/DMA/game updates replace the old baseline. Do not later
            // restore bytes captured from a different overlay or player state.
            if((ram.get(entry.getKey())&255)!=patch.last){iterator.remove();continue;}
            boolean removed=patch.owners.keySet().removeIf(owner->!wanted.contains(owner));
            if(removed && (ram.get(entry.getKey())&255)==patch.last) {
                int value=patch.original;
                for(List<Code> program:selected)if(patch.owners.containsKey(program))value=patch.owners.get(program);
                store(ram,cpu,entry.getKey(),1,value);patch.last=value;
            }
            if(patch.owners.isEmpty())iterator.remove();
        }
    }
    private boolean sampleProgram(List<Code> program) {
        if(build!=null) {
            int[][] signature=sampleGuardSignature;if(signature==null)return false;
            return program.size()>4 && program.get(0).command==(0xD1000000|signature[0][0]) && program.get(0).value==(signature[0][1]>>>16)
                && program.get(1).command==(0xD1000000|signature[0][0]+2) && program.get(1).value==(signature[0][1]&65535)
                && program.get(2).command==(0xD1000000|signature[1][0]) && program.get(2).value==(signature[1][1]>>>16)
                && program.get(3).command==(0xD1000000|signature[1][0]+2) && program.get(3).value==(signature[1][1]&65535);
        }
        return program.size()>4 && program.get(0).command==0xD131CE64 && program.get(0).value==0x3C02
            && program.get(1).command==0xD131CE66 && program.get(1).value==0x8022
            && program.get(2).command==0xD131CE68 && program.get(2).value==0x8C42
            && program.get(3).command==0xD131CE6A && program.get(3).value==0x6B60;
    }
    private static boolean displayListProgram(List<Code> program) {
        return program.size()>DISPLAY_LIST_GUARDS.size()
            && program.subList(0,DISPLAY_LIST_GUARDS.size()).equals(DISPLAY_LIST_GUARDS);
    }
    private boolean sceneProgram(List<Code> program) {
        int levelAddress=0x32DDF8;
        if(build!=null)try{levelAddress=build.address(levelAddress,2);}catch(IllegalArgumentException unsupported){return false;}
        if(program.size()<=10 || !program.get(0).equals(program.get(1)) || program.get(0).command!=(0xD1000000|levelAddress))return false;
        for(int start:new int[]{2,6}) {
            if(program.get(start).value>>>8!=4)return false;
            for(int i=0;i<4;i++)if(program.get(start+i).command!=program.get(start).command+i*2 || program.get(start+i).command>>>24!=0xD1)return false;
        }
        return true;
    }
    private static boolean sampleBookAlive(List<Code> program,ByteBuffer ram) {
        for(Code code:program) {
            int type=code.command>>>24;if(type<0xD0 || type>0xD3)return true;
            int address=code.command&0xFFFFFF,size=(type&1)+1;
            if(address>ram.capacity()-size)return false;
            int value=size==1?ram.get(address)&255:ram.getShort(address)&65535;
            if(type<0xD2?value!=code.value:value==code.value)return false;
        }
        return false;
    }
    private void execute(ByteBuffer ram,Cpu cpu,boolean boot,List<List<Code>> selected) {
        musicBurstShift=0;
        boolean pressed = button;
        button = false;
        for (List<Code> program : selected) {
            boolean execute = true;
            for (int i = 0; i < program.size(); i++) {
                Code code = program.get(i);
                int type = code.command >>> 24;
                int address = code.command & 0xFFFFFF;
                if (type >= 0xD0 && type <= 0xD3) {
                    int size = (type & 1) + 1;
                    int actual = address <= ram.capacity() - size
                            ? (size == 1 ? ram.get(address) & 0xFF : ram.getShort(address) & 0xFFFF) : -1;
                    boolean equal = actual == (code.value & (size == 1 ? 0xFF : 0xFFFF));
                    execute &= (type < 0xD2 ? equal : !equal);
                    continue;
                }
                if (type == 0x50) {
                    Code target = program.get(++i);
                    if (execute && !boot) {
                        for (int n = 0; n < ((code.command >>> 8) & 0xFF); n++)
                            write(ram, cpu, program, (target.command & 0xFFFFFF) + n * (code.command & 0xFF),
                                    (target.command >>> 24 & 1) + 1, target.value + n * code.value);
                    }
                } else if (type==0xB0) {
                    if(execute && !boot && Sm64BurstMusicCodes.supported(ram))musicBurstShift=code.value;
                } else if (execute && ((type == 0xF0 || type == 0xF1) == boot)
                        && ((type != 0x88 && type != 0x89) || pressed)) {
                    write(ram, cpu, program, address, (type & 1) + 1, code.value);
                }
                execute = true;
            }
        }
    }

    private void write(ByteBuffer ram, Cpu cpu,List<Code> owner,int address,int size,int value) {
        if(address<0 || address>ram.capacity()-size)return;
        // Read any GPU-owned bytes before recording their unmodified baseline.
        d3d11.framebuffer.D3DFramebuffers.read(address,size);
        for(int i=0;i<size;i++) {
            int offset=address+i,b=(value>>>((size-1-i)*8))&255;
            Patch patch=patches.computeIfAbsent(offset,key->new Patch(ram.get(key)&255));
            patch.owners.put(owner,b);patch.last=b;
        }
        store(ram,cpu,address,size,value);
    }
    private static void store(ByteBuffer ram,Cpu cpu,int address,int size,int value) {
        d3d11.framebuffer.D3DFramebuffers.write(address,size);
        if (size == 1) ram.put(address, (byte) value);
        else ram.putShort(address, (short) value);
        if (cpu != null) {cpu.invalidateCode(address);cpu.invalidateCode(address+size-1);}
    }
    public static final class UndoState {
        final Map<Integer,Patch> patches=new LinkedHashMap<>();
    }
    public void writeUndo(DataOutputStream out,ByteBuffer ram)throws IOException {
        if(trackedRam!=ram){out.writeInt(0);out.writeInt(0);return;}
        LinkedHashMap<List<Code>,Integer> ids=new LinkedHashMap<>();
        for(Patch patch:patches.values())for(List<Code> owner:patch.owners.keySet())ids.computeIfAbsent(owner,key->ids.size());
        out.writeInt(ids.size());
        for(List<Code> owner:ids.keySet()){out.writeInt(owner.size());for(Code code:owner){out.writeInt(code.command);out.writeShort(code.value);}}
        out.writeInt(patches.size());
        for(var entry:patches.entrySet()) {
            Patch p=entry.getValue();out.writeInt(entry.getKey());out.writeByte(p.original);out.writeByte(p.last);out.writeInt(p.owners.size());
            for(var owner:p.owners.entrySet()){out.writeInt(ids.get(owner.getKey()));out.writeByte(owner.getValue());}
        }
    }
    public static UndoState readUndo(DataInputStream in)throws IOException {
        int count=in.readInt();if(count<0 || count>16384)throw new IOException("Invalid cheat undo programs");
        List<List<Code>> owners=new ArrayList<>();int total=0;
        for(int i=0;i<count;i++) {
            int length=in.readInt();total+=length;if(length<1 || length>65536 || total>262144)throw new IOException("Invalid cheat undo code count");
            List<Code> codes=new ArrayList<>();for(int j=0;j<length;j++)codes.add(new Code(in.readInt(),in.readUnsignedShort()));owners.add(List.copyOf(codes));
        }
        UndoState state=new UndoState();int bytes=in.readInt();if(bytes<0 || bytes>0x100000)throw new IOException("Invalid cheat undo size");
        long references=0;
        for(int i=0;i<bytes;i++) {
            int address=in.readInt();if(address<0 || address>=0x800000 || state.patches.containsKey(address))throw new IOException("Invalid cheat undo address");
            Patch p=new Patch(in.readUnsignedByte());p.last=in.readUnsignedByte();int layers=in.readInt();references+=layers;
            if(layers<1 || layers>count || references>0x100000)throw new IOException("Invalid cheat undo owners");
            for(int j=0;j<layers;j++){int owner=in.readInt();if(owner<0 || owner>=count)throw new IOException("Invalid cheat undo reference");p.owners.put(owners.get(owner),in.readUnsignedByte());}
            state.patches.put(address,p);
        }
        return state;
    }
    public void restoreUndo(UndoState state,ByteBuffer ram) {
        musicBurstShift=0;patches.clear();trackedRam=ram;
        if(state!=null)patches.putAll(state.patches);
        resetBootPhase();
    }

}
