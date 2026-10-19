package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.*;

/** Builds ordinary guarded GameShark writes from a validated live US music bank.
 * Only predictor coefficients change, never ADPCM headers or sample pointers. */
public final class Sm64SampleCodes {
    static final int CTL=0x226B60, STATUS=0x2214D0;
    static final int[][] SIGNATURE={{0x31CE64,0x3C028022},{0x31CE68,0x8C426B60},
        {0x31CE74,0x0018C080},{0x31CE90,0x8C690004},
        {0x31DBB8,0x3C048022},{0x31DBBC,0x248414D0}};
    record Book(int address,int count,String guards,short[] coefficients) {}
    record Bank(int id,int instruments,List<Book> books) {}
    static int ptr(ByteBuffer ram,int field,int size) {
        int value=ram.getInt(field),address=value&0x1FFFFFFF;
        if((value&0xE0000000)!=0x80000000 || (address&3)!=0 || address<0x1000
                || address>Math.min(ram.capacity(),0x800000)-size)
            throw new IllegalArgumentException("Invalid audio-bank pointer at "+Integer.toHexString(field));
        return address;
    }
    static Bank read(ByteBuffer ram) {
        return read(ram,Main.decompCheats);
    }
    static Bank read(ByteBuffer ram,Sm64DecompCodes build) {
        int[][] signature=build==null?SIGNATURE:build.signature(SIGNATURE);
        int ctlAddress=build==null?CTL:build.address(CTL,4),statusAddress=build==null?STATUS:build.address(STATUS,0x40);
        for(int[] site:signature)if(ram.getInt(site[0])!=site[1])throw new IllegalArgumentException("Unsupported SM64 audio-bank layout");
        int player=build==null?Sm64MusicCodes.PLAYERS:build.address(Sm64MusicCodes.PLAYERS,Sm64MusicCodes.STRIDE);
        if((ram.get(player)&0x80)==0)throw new IllegalArgumentException("Start a level with background music first.");
        int bank=ram.get(player+6)&255;
        if(bank<0xB || bank>0x25 || (ram.get(statusAddress+bank)&255)<2)
            throw new IllegalArgumentException("No supported music-only sample bank is loaded.");
        int ctl=ptr(ram,ctlAddress,0x26*12),entry=ctl+bank*12;
        int count=ram.get(entry+1)&255,drums=ram.get(entry+2)&255;
        if(count<1 || count>128 || drums>128)throw new IllegalArgumentException("Invalid instrument/drum count");
        int instruments=ptr(ram,entry+4,count*4);
        StringBuilder common=new StringBuilder();
        for(int[] site:signature)word(common,site[0],site[1]);
        word(common,ctlAddress,ram.getInt(ctlAddress));word(common,entry+4,ram.getInt(entry+4));
        condition(common,0xD0,entry+1,count);condition(common,0xD0,entry+2,drums);
        condition(common,0xD2,statusAddress+bank,0);condition(common,0xD2,statusAddress+bank,1);
        Map<Integer,Book> books=new LinkedHashMap<>();
        for(int i=0;i<count;i++) {
            int slot=instruments+i*4;if(ram.getInt(slot)==0)continue;
            int instrument=ptr(ram,slot,32);StringBuilder guard=new StringBuilder(common);word(guard,slot,ram.getInt(slot));
            for(int sound:new int[]{8,16,24})sample(ram,instrument+sound,guard.toString(),books);
        }
        if(drums>0) {
            int table=ptr(ram,entry+8,drums*4);StringBuilder base=new StringBuilder(common);word(base,entry+8,ram.getInt(entry+8));
            for(int i=0;i<drums;i++) {
                int slot=table+i*4;if(ram.getInt(slot)==0)continue;
                int drum=ptr(ram,slot,16);StringBuilder guard=new StringBuilder(base);word(guard,slot,ram.getInt(slot));
                sample(ram,drum+4,guard.toString(),books);
            }
        }
        if(books.isEmpty())throw new IllegalArgumentException("This music bank has no ADPCM predictor books.");
        return new Bank(bank,instruments,List.copyOf(books.values()));
    }
    private static void sample(ByteBuffer ram,int sound,String prefix,Map<Integer,Book> books) {
        if(ram.getInt(sound)==0)return;
        int sample=ptr(ram,sound,20),book=ptr(ram,sample+12,8);
        if(books.containsKey(book))return;
        int order=ram.getInt(book),predictors=ram.getInt(book+4);
        if(order!=2 || predictors<1 || predictors>8)throw new IllegalArgumentException("Unsupported ADPCM predictor dimensions");
        int count=8*order*predictors;
        ptr(ram,sample+12,8+count*2);
        StringBuilder guards=new StringBuilder(prefix);
        word(guards,sound,ram.getInt(sound));word(guards,sample+12,ram.getInt(sample+12));
        word(guards,sample+4,ram.getInt(sample+4));word(guards,sample+16,ram.getInt(sample+16));
        word(guards,book,order);word(guards,book+4,predictors);
        short[] coefficients=new short[count];for(int i=0;i<count;i++)coefficients[i]=ram.getShort(book+8+i*2);
        books.put(book,new Book(book,count,guards.toString(),coefficients));
    }
    public static List<CheatRepository.Entry> generate(ByteBuffer ram) {
        Bank bank=read(ram);List<CheatRepository.Entry> out=new ArrayList<>();
        String folder=String.format(Locale.ROOT,"Audio\\Sample distortion\\Bank %02X @ %08X\\",bank.id,bank.instruments|0x80000000);
        for(int effect=0;effect<2;effect++) {
            StringBuilder code=new StringBuilder();
            for(Book book:bank.books) {
                // Separate programs retain a per-book lifetime guard for undo,
                // including after a saved state restores the undo records.
                if(!code.isEmpty())code.append('\n');
                for(int i=0;i<book.count;i++) {
                    int value=effect==0?0:book.coefficients[i]/1024*1024;
                    code.append(book.guards);condition(code,0x81,book.address+8+i*2,value&65535);
                }
            }
            String name=effect==0?"Rake residual crunch (approximation)":"Rake stepped predictors (approximation)";
            CheatRepository.Entry generated=new CheatRepository.Entry(folder+name,
                "SM64 US / verified compatible US extensions. Generated from the current music bank, "+bank.books.size()+" ADPCM predictor books. "
                +(effect==0?"Zeros prediction coefficients so original compressed samples decode largely as coarse residual pulses. ":"Quantizes prediction coefficients in steps of 1024, distorting reconstruction of the original instrument/drum samples. ")
                +"This changes sample timbre, not the note-frequency table or music tempo. Original sample data, pointers, dimensions and sequence parser stay intact. "
                +"Up to four Audio effects can be enabled within the code budget. Supported pitch tables mix their relative changes; other writes must agree. Disabling restores owned coefficients while their bank is still loaded; reused allocations discard stale undo. "
                +"Only this captured bank/allocation is targeted. Start another level and use Build Sample Codes to generate codes for its bank. "
                +"A different bank/allocation makes these guards inactive. Already queued notes may finish with old decoded data. "
                +"Approximation of https://www.youtube.com/watch?v=FkEyF8hibWQ at 11:49; reference audio was not measured or matched. "
                +"Source: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af src/audio/{internal.h,load.c,seqplayer.c,synthesis.c}. "
                +"Generated KernelN64 GameShark code; chained guards and bank-aware undo require this engine.",code.toString().strip(),false,"sound");
            if(Main.decompCheats!=null)generated.addressing=Main.decompCheats.hash;
            out.add(generated);
        }
        return out;
    }
    private static void word(StringBuilder out,int address,int value) {
        condition(out,0xD1,address,value>>>16);condition(out,0xD1,address+2,value&65535);
    }
    private static void condition(StringBuilder out,int type,int address,int value) {
        out.append(String.format(Locale.ROOT,"%08X %04X%n",type<<24|address,value));
    }
}
