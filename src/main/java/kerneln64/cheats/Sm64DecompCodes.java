package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.state.SaveStates;
import kerneln64.Main;


import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Ports known US RAM targets using a ROM-matched ELF, never a global delta. */
public final class Sm64DecompCodes {
    private record Region(int stock,int target,int size) {}
    private record Function(String name,int stock,int size,String fingerprint) {}
    // Fingerprints cross-checked against the supplied US retail ROM and matching
    // n64decomp 9921382a build. Address materialization and J/JAL targets normalize;
    // branch distances, registers, stack/struct offsets and other instructions do not.
    private static final Function[] FUNCTIONS={
        new Function("sequence_channel_process_script",0x31D08C,0x960,"be60477a17c819753ecdf5b36944b9c0f8ec0ac4e3d3448da9e30cfef016448d"),
        new Function("display_and_vsync",0x248090,0x150,"77a13293d0cca3d33bfd3eb9aaafa342f9378cbbd6a65e3b0fa60925787c054a"),
        new Function("note_vibrato_init",0x31B4A0,0xEC,"b0c7c20b4efb2c3d47486e107a87455de3311e651b5f621985affb10d4ec9476"),
        new Function("get_vibrato_freq_scale",0x31B248,0x1F8,"78c0fa916862b123aff4fd6fa051e6f461ae63d1e3184c1b2377cfaa003b3a77"),
        new Function("seq_channel_layer_process_script",0x31C0C4,0xD90,"13d88027ecb42516f86f1c34c6d1eb285e958ce6582f6853ee989971f3c2ac10"),
        new Function("get_instrument",0x31CE54,0x180,"aae241ff524ef0af3ba91dcfe673bbb9fa143cbeabf107c581d2599fc1ef3b56"),
        new Function("sequence_player_process_sequence",0x31D9EC,0x854,"2682486a9bacb3cf5a0b95c184017bd1dd799fff28b1449820349633197061ff"),
        new Function("process_sequences",0x31E240,0xA8,"7049c86149b26543e8a267413bb51fa41833a8e80c75ca8184fda4d655df2ca0"),
        new Function("grabbable_joint_update_func",0x18E660,0x6C8,"1b8af9f58b1a96ccfc4d868becf92b7ad3d836e846a255e9813614b28b3cf8e5"),
        new Function("mario_update_quicksand",0x255414,0x240,"b2739cdcd23ab8361c1e65b7b594d144f94cdeb295b44bbe790395cc74348a18"),
        new Function("mario_update_moving_sand",0x25570C,0x100,"ff6a69e85da7a03cde695dfbc619361b3048717c1327c4429ad32a61db8dca61"),
        new Function("mario_handle_special_floors",0x2507FC,0x140,"b7dd95a7cf61c41df34a6818396d85161ada33d2565e71b42d87f7b518ac03a3")
    };
    private final List<Region> regions=new ArrayList<>();
    private final Map<Integer,Integer> guards=new HashMap<>();
    private static final Map<Integer,Integer> STOCK_GUARDS=stockGuards();
    private static Map<Integer,Integer> stockGuards() {
        Map<Integer,Integer> result=new HashMap<>();
        for(int[][] signature:new int[][][]{Sm64AudioCodes.SIGNATURE,Sm64MusicCodes.SIGNATURE,Sm64BurstMusicCodes.SIGNATURE,Sm64BurstMusicCodes.COUNTDOWN_SIGNATURE,Sm64SampleCodes.SIGNATURE,Sm64HeadStretch.SIGNATURE})
            for(int[] site:signature){result.put(site[0],site[1]>>>16);result.put(site[0]+2,site[1]&65535);}
        return Map.copyOf(result);
    }
    public final String hash;
    public final Path elfPath;
    private Sm64DecompCodes(Path path,ByteBuffer rom)throws IOException {
        elfPath=path;hash=SaveStates.romHash(rom);Sm64Elf elf=new Sm64Elf(path,rom);
        if((rom.get(0x3E)&255)!='E')throw new IOException("Decomp cheat mapping currently supports US SM64 builds");
        data(elf,"gMarioStates",0x33B170,0xC8);
        data(elf,"gSaveBuffer",0x207700,0x200);
        data(elf,"gControllers",0x33AF90,0x54);
        data(elf,"gCurrLevelNum",0x32DDF8,2);
        data(elf,"gNoteFrequencies",0x333994,0x200);
        data(elf,"gVibratoCurve",0x333BB4,0x10);
        data(elf,"gSequencePlayers",0x222618,0x3C0);
        data(elf,"gGlobalTimer",0x32D5D4,4);
        data(elf,"gBankLoadStatus",0x2214D0,0x40);
        data(elf,"gCtlEntries",0x226B60,4);
        for(Function function:FUNCTIONS) {
            var symbol=elf.symbol(function.name);
            if(symbol==null || symbol.size()!=function.size)continue;
            byte[] bytes=elf.bytes(symbol);
            if(!fingerprint(bytes).equals(function.fingerprint))continue;
            int target=symbol.address()&0x1FFFFFFF;
            if(target<0x1000 || target>0x800000-function.size)continue;
            regions.add(new Region(function.stock,target,function.size));
            ByteBuffer words=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            for(int off=0;off<bytes.length;off+=2)guards.put(function.stock+off,words.getShort(off)&65535);
        }
        if(regions.stream().noneMatch(r->r.stock==0x33B170))throw new IOException("ELF lacks the compatible SM64 MarioState layout");
    }
    public static Sm64DecompCodes find(File romFile,ByteBuffer rom)throws IOException {
        String name=romFile.getName();int dot=name.lastIndexOf('.');
        for(String candidate:new String[]{(dot<0?name:name.substring(0,dot))+".elf",name+".elf"}) {
            Path path=romFile.toPath().toAbsolutePath().resolveSibling(candidate);
            if(Files.isRegularFile(path))return new Sm64DecompCodes(path,rom);
        }
        return null;
    }
    private void data(Sm64Elf elf,String name,int stock,int size) {
        var symbol=elf.symbol(name);
        if(symbol!=null && symbol.size()==size) {
            int target=symbol.address()&0x1FFFFFFF;
            if((symbol.address()&0xE0000000)==0x80000000 && target>=0x1000 && target<=0x800000-size)regions.add(new Region(stock,target,size));
        }
    }
    int address(int stock,int width) {
        for(Region region:regions)if(stock>=region.stock && stock-region.stock<=region.size-width)return region.target+stock-region.stock;
        throw new IllegalArgumentException(String.format("No verified SM64 build mapping for %08X",stock|0x80000000));
    }
    int[][] signature(int[][] stock) {
        int[][] result=new int[stock.length][2];
        for(int i=0;i<stock.length;i++) {
            int a=stock[i][0];result[i][0]=address(a,4);
            Integer hi=guards.get(a),lo=guards.get(a+2);
            if(hi==null || lo==null)throw new IllegalArgumentException("Audio reader was changed in this build");
            result[i][1]=hi<<16|lo;
        }
        return result;
    }
    public String port(String code) {
        if(CheatEngine.replacesHeadCode(code))code=Sm64HeadStretch.CODES;
        StringBuilder result=new StringBuilder();
        int repeatCount=0,repeatStep=0;
        for(String raw:code.split("\\R",-1)) {
            String line=raw.strip();
            if(line.isEmpty() || line.startsWith("#") || line.startsWith(";")){result.append(raw).append('\n');continue;}
            if(!line.matches("(?i)[0-9a-f]{8}\\s+[0-9a-f]{4}"))throw new IllegalArgumentException("Porting requires XXXXXXXX YYYY lines");
            String[] parts=line.split("\\s+");int command=(int)Long.parseLong(parts[0],16),value=Integer.parseInt(parts[1],16),type=command>>>24;
            if(type==0x50){repeatCount=command>>>8&255;repeatStep=command&255;result.append(line).append('\n');continue;}
            if(!Set.of(0x80,0x81,0xA0,0xA1,0x88,0x89,0xD0,0xD1,0xD2,0xD3,0xF0,0xF1).contains(type))throw new IllegalArgumentException("Normalize encrypted/legacy codes before porting");
            int stock=command&0xFFFFFF,width=(type&1)+1,target=address(stock,width);
            if(repeatCount>0) {
                int last=stock+(repeatCount-1)*repeatStep;
                if(address(last,width)!=target+(repeatCount-1)*repeatStep)throw new IllegalArgumentException("Repeater crosses a relocated symbol boundary");
                repeatCount=0;
            }
            if(type>=0xD0 && type<=0xD3 && guards.containsKey(stock&~1) && STOCK_GUARDS.containsKey(stock&~1)) {
                // Guard comparisons refer to the new binary, including relocated LUI/ADDIU immediates.
                int half=guards.get(stock&~1),old=STOCK_GUARDS.get(stock&~1);
                int expected=width==2?old:((stock&1)==0?old>>>8:old&255);
                if(value==expected)value=width==2?half:((stock&1)==0?half>>>8:half&255);
            }
            result.append(String.format(Locale.ROOT,"%08X %04X%n",type<<24|target,value));
        }
        String text=result.toString().strip();
        CheatEngine check=new CheatEngine();check.setSm64Compatibility(true);check.setCodes(text);return text;
    }
    List<CheatRepository.Entry> presets(List<CheatRepository.Entry> stock) {
        List<CheatRepository.Entry> result=new ArrayList<>();
        for(var entry:stock)try {
            var mapped=new CheatRepository.Entry(entry.name,entry.notes+"\nRelocated to this ROM's matching ELF; addresses shown are native to this build.",port(entry.codes),entry.enabled,entry.group);
            mapped.addressing=hash;result.add(mapped);
        }catch(IllegalArgumentException unsupported){/* Only verified entries are offered. */}
        return result;
    }
    void prepare(List<CheatRepository.Entry> entries) {
        Map<String,CheatRepository.Entry> canonical=new HashMap<>();
        List<CheatRepository.Entry> unavailable=new ArrayList<>();
        for(var entry:CheatRepository.retailPresets())canonical.put(entry.name,entry);
        for(var entry:entries) {
            if(entry.addressing.equals(hash))continue;
            var stock=canonical.get(entry.name);
            if(entry.addressing.isEmpty() && stock!=null && normalized(entry.codes).equals(normalized(stock.codes)))try {
                entry.codes=port(entry.codes);entry.addressing=hash;
                entry.notes+="\nRelocated to this ROM's matching ELF; addresses shown are native to this build.";continue;
            }catch(IllegalArgumentException unsupported){unavailable.add(entry);continue;}
            entry.enabled=false;entry.addressing=hash;
            entry.notes+="\nDisabled on first use with this build. Review native addresses, or paste US codes into the editor for automatic conversion before enabling.";
        }
        entries.removeAll(unavailable);
    }
    private static String normalized(String code){return code.replace("\r\n","\n").strip();}
    static String fingerprint(byte[] bytes) {
        return Sm64CodeFingerprint.hash(bytes);
    }
}
