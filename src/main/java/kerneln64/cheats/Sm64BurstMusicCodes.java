package kerneln64.cheats;

import java.util.Locale;

/** Repeating music stalls and fast bursts, using the verified game-frame clock. */
final class Sm64BurstMusicCodes {
    static final String NAME="Audio\\Music experiments\\delayed notes and catch-up bursts (approximation)";
    static final String FASTER_NAME="Audio\\Music experiments\\delayed notes and catch-up bursts - faster bursts (approximation)";
    static final String EXTREME_NAME="Audio\\Music experiments\\delayed notes and catch-up bursts - extremely fast bursts (approximation)";
    static final int[][] COUNTDOWN_SIGNATURE={
        {0x31C0EC,0x8483003C},{0x31C0F8,0x28610002},{0x31C108,0xA498003C},
        {0x31D11C,0x96430016},{0x31D128,0x10600004},{0x31D134,0xA64D0016},{0x31D138,0x31A2FFFF},
        {0x31DC48,0x28810002},{0x31DC5C,0xA6380012}
    };
    // address, original ADDIU word, source register, destination register.
    static final int[][] COUNTDOWNS={
        {0x31C100,0x2478FFFF,3,24},
        {0x31D130,0x246DFFFF,3,13},
        {0x31DC54,0x2498FFFF,4,24}
    };
    static final int TIMER=0x32D5D4;
    static final int[][] SIGNATURE={
        {0x2481B4,0x3C198033},{0x2481B8,0x8F39D5D4},
        {0x2481BC,0x3C018033},{0x2481C0,0x272F0001},{0x2481C4,0xAC2FD5D4}
    };
    static CheatRepository.Entry preset(){
        return new CheatRepository.Entry(NAME,
            "SM64 US v1.0 / verified compatible US extensions. Repeats about 4.27 seconds of stalled music progression and 4.27 seconds at 288 BPM at the normal 30 game frames/s. "
            +"Applies immediately using the running game clock, so activation may begin in either phase. A song can change during a phase. "
            +"Changes only music-player 0/1 tempo: no pitch, note-gate, sample, code or pointer writes. Sound-effect player tempo stays unchanged. "
            +"Already playing notes or sample/reverb tails may continue during a stall; it is not a hard mute or a queued-audio replay. "
            +"An approximation of the supplied delayed-then-fast music reference, not an exact reconstruction or MIDI import. "
            +"Disable to restore owned tempo bytes without a ROM restart; music position is not rewound. Conflicts with other tempo locks, but separate pitch effects can be used. "
            +"KernelN64 chained D1/D2 condition semantics required; other engines and physical GameSharks are untested. "
            +"Source: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af src/game/game_init.c display_and_vsync, src/audio/seqplayer.c and internal.h. "
            +"Verified timer at 8032D5D4, incremented by code at 802481B4..802481C4. Timer is read only.",code(),false,"sound");
    }
    static CheatRepository.Entry fasterPreset(){return speedPreset(FASTER_NAME,1);}
    static CheatRepository.Entry extremePreset(){return speedPreset(EXTREME_NAME,2);}
    private static CheatRepository.Entry speedPreset(String name,int shift){
        return new CheatRepository.Entry(name,
            "SM64 US v1.0 / verified compatible extensions. Keeps the original approximately 4.27-second delay and 4.27-second burst phases. "
            +"During bursts only, music-player 0/1 layer, channel and player delays are divided by "+(1<<shift)+" per sequencer update instead of subtracting one. "
            +"This makes bursts faster through compressed countdowns, not a higher BPM or uniform speed multiplier. Music tick tempo remains 288 BPM. "
            +"Guest instructions remain original throughout. No pitch, sample or pointer changes. "
            +"Sound-effect player 2 always executes the original countdowns. "
            +"Use one delay/burst preset at a time. Conflicts with other tempo/countdown patches; code needs the KernelN64 B0 music-countdown extension and chained conditions. Other engines and hardware GameSharks are unsupported. "
            +"An approximation, not an exact corruption match. Already queued audio can finish; the music position is not rewound. "
            +"Source: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af src/audio/seqplayer.c and src/game/game_init.c. "
            +"Verified US countdown decrements at 8031C100, 8031D130 and 8031DC54. The B0 extension scopes execution to verified music ownership; it does not patch those instructions.",speedCode(shift),false,"sound");
    }
    static int shifted(int[] site,int shift){return site[2]<<16|site[3]<<11|shift<<6|2;}
    static String legacySpeedCode(int shift){
        if(shift<1||shift>2)throw new IllegalArgumentException("burst shift must be 1 or 2");
        StringBuilder out=new StringBuilder(code()).append('\n');
        for(int[] site:COUNTDOWNS)for(int phase=0;phase<2;phase++){
            int desired=phase==0?site[1]:shifted(site,shift);
            for(int input:new int[]{site[1],shifted(site,1),shifted(site,2)}){
                // Staged halfwords accept only a known original/variant word.
                // All stores finish at a VI boundary before guest execution.
                speedGuards(out,phase);condition(out,site[0],input>>>16);condition(out,site[0]+2,input&65535);
                line(out,0x81000000|site[0]+2,desired&65535);
                speedGuards(out,phase);condition(out,site[0],input>>>16);condition(out,site[0]+2,desired&65535);
                line(out,0x81000000|site[0],desired>>>16);
            }
        }
        return out.toString().strip();
    }
    // KernelN64 extension: enable a scoped countdown transform, without guest code writes.
    static String speedCode(int shift){
        if(shift<1||shift>2)throw new IllegalArgumentException("burst shift must be 1 or 2");
        StringBuilder out=new StringBuilder(code()).append('\n');
        speedGuards(out,1);
        for(int[] site:COUNTDOWNS)guard(out,new int[]{site[0],site[1]});
        line(out,0xB0222618,shift);
        return out.toString().strip();
    }
    private static final class Legacy {
        static final String[] CODES={legacySpeedCode(1).replace("\r\n","\n"),legacySpeedCode(2).replace("\r\n","\n")};
    }
    static String upgrade(String text){
        text=text.replace("\r\n","\n");
        for(int i=0;i<2;i++)text=text.replace(Legacy.CODES[i],speedCode(i+1).replace("\r\n","\n"));
        return text;
    }
    /** Run only at the three proven ADDIU sites; unknown contexts retain normal MIPS behavior. */
    static int countdown(kerneln64.cpu.Cpu cpu,java.nio.ByteBuffer ram,int shift,int instruction,int normal){
        int pc=cpu.pc;
        if(pc!=0x8031C100 && pc!=0x8031D130 && pc!=0x8031DC54)return normal;
        if(shift==0 || ram==null || ram.capacity()<0x334000 || (ram.getInt(TIMER)&128)==0)return normal;
        int source,player;
        if(pc==0x8031C100 && instruction==COUNTDOWNS[0][1]){
            int layer=pointer(cpu.GPR[4],ram,0x80);if(layer<0)return normal;
            int channel=pointer(ram.getInt(layer+0x50),ram,0xC0);if(channel<0)return normal;
            player=ram.getInt(channel+0x40);source=3;
        }else if(pc==0x8031D130 && instruction==COUNTDOWNS[1][1]){
            int channel=pointer(cpu.GPR[18],ram,0xC0);if(channel<0)return normal;
            player=ram.getInt(channel+0x40);source=3;
        }else if(pc==0x8031DC54 && instruction==COUNTDOWNS[2][1]){
            player=(int)cpu.GPR[17];source=4;
        }else return normal;
        if(player!=0x80222618 && player!=0x80222758)return normal;
        int value=(int)cpu.GPR[source];
        return value>1 && value<=65535 ? value>>>shift : normal;
    }
    static boolean supported(java.nio.ByteBuffer ram){
        if(ram.capacity()<0x334000)return false;
        for(int[][] signature:new int[][][]{Sm64MusicCodes.SIGNATURE,SIGNATURE,COUNTDOWN_SIGNATURE,COUNTDOWNS})
            for(int[] site:signature)if(ram.getInt(site[0])!=site[1])return false;
        return true;
    }
    private static int pointer(long value,java.nio.ByteBuffer ram,int size){
        int address=(int)value;
        if((address&0xE0000000)!=0x80000000)return -1;
        int offset=address&0x1FFFFFFF;
        return offset>0 && (offset&3)==0 && offset<=ram.capacity()-size?offset:-1;
    }
    private static void speedGuards(StringBuilder out,int phase){
        for(int[] site:Sm64MusicCodes.SIGNATURE)guard(out,site);
        for(int[] site:SIGNATURE)guard(out,site);
        for(int[] site:COUNTDOWN_SIGNATURE)guard(out,site);
        int excluded=phase==0?128:0;
        for(int i=excluded;i<excluded+128;i++)line(out,0xD2000000|TIMER+3,i);
    }
    private static void condition(StringBuilder out,int address,int value){line(out,0xD1000000|address,value);}
    static String code(){
        StringBuilder out=new StringBuilder();
        for(int player=0;player<2;player++)for(int phase=0;phase<2;phase++){
            for(int[] site:Sm64MusicCodes.SIGNATURE)guard(out,site);
            for(int[] site:SIGNATURE)guard(out,site);
            // D2 chains are ANDed. Exclude the opposite half of the byte cycle
            // rather than relying on undocumented compare/mask code types.
            int excluded=phase==0?128:0;
            for(int i=excluded;i<excluded+128;i++)line(out,0xD2000000|TIMER+3,i);
            line(out,0x81000000|Sm64MusicCodes.PLAYERS+player*Sm64MusicCodes.STRIDE+Sm64MusicCodes.TEMPO,phase==0?0:288*48);
        }
        return out.toString().strip();
    }
    private static void guard(StringBuilder out,int[] word){line(out,0xD1000000|word[0],word[1]>>>16);line(out,0xD1000000|word[0]+2,word[1]&65535);}
    private static void line(StringBuilder out,int command,int value){out.append(String.format(Locale.ROOT,"%08X %04X%n",command,value));}
    private Sm64BurstMusicCodes(){}
}
