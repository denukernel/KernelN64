package kerneln64.cheats;

import java.util.*;

/** Captured Fast3D input commands in the verified US extended BOB allocation. */
public final class Sm64DisplayListCodes {
    // Immutable scene/material words, excluded from every preset write footprint.
    static final int[][] GUARDS={{0x32DDF8,9},{0xFF350,0xB700},{0xFF352,0},
        {0xFF354,1},{0xFF356,0},{0xFF360,0xF510},{0xFF362,0},
        {0xFF364,0x0700},{0xFF366,0},{0xFF370,0xE800},{0xFF372,0},
        {0xFF374,0},{0xFF376,0}};
    private static final int[] TEXTURES={0xFF368,0x104D60,0x105420,0x105900,0x108CF0,0x109310};
    private static final int[] COMBINERS={0xFF358,0x104D48,0x108CE0,0x109300};
    public static List<CheatRepository.Entry> presets() {
        List<CheatRepository.Entry> list=new ArrayList<>();
        for(int effect=0;effect<3;effect++) {
            StringBuilder code=new StringBuilder();
            for(int at:TEXTURES) {
                // BB000001 FFFFFFFF = gSPTexture(1,1,0,0,G_ON).
                int scale=effect==0?0x03FF03FF:effect==1?0x1FFF1FFF:0x1FFFFFFF;
                half(code,at+4,scale>>>16);half(code,at+6,scale&65535);
            }
            add(list,new String[]{"Texture scale one sixty-fourth","Texture scale one eighth","Texture scale U stretched"}[effect],code.toString(),
                "patches six executed gSPTexture (BB) commands. "+new String[]{"sets both fixed-point coordinate scales to 0x03ff for a greatly enlarged texture pattern.","sets both fixed-point coordinate scales to 0x1fff, enlarging the texture pattern.","sets U scale to 0x1fff and retains V scale 0xffff."}[effect],"bob-texture-command");
        }
        StringBuilder shade=new StringBuilder();
        for(int at:COMBINERS){half(shade,at,0xFCFF);half(shade,at+2,0xFFFF);half(shade,at+4,0xFFFE);half(shade,at+6,0x793C);}
        add(list,"Shade-only color combiners",shade.toString(),"replaces four executed FC127FFF FFFFF838 material commands with FCFFFFFF FFFE793C, the captured G_CC_SHADE restore command. terrain uses vertex shade instead of sampled texture color.","bob-combiner-command");
        return list;
    }
    private static void half(StringBuilder out,int address,int value) {
        for(int[] g:GUARDS)out.append(String.format(Locale.ROOT,"%08X %04X%n",0xD1000000|g[0],g[1]));
        out.append(String.format(Locale.ROOT,"%08X %04X%n",0x81000000|address,value));
    }
    private static void add(List<CheatRepository.Entry> list,String name,String code,String detail,String group) {
        list.add(new CheatRepository.Entry("GFX\\Display lists\\Bob-omb Battlefield\\"+name,
            "verified target: sm64 us extended SHA-256 5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0, main test state, Fast3D. segment 7 physical base 000FAF90. "+detail+
            " writes actual guest display list command halfwords. level 9 and immutable material guards gate each write. only the captured materials/allocation are covered; other ROM layouts and decomp builds are not established. enable after entering BOB. disable to restore owned bytes while the guarded allocation survives. scene mismatch discards undo ownership. source: observed RSP input and kernel64gl/engine/opcodes/F3d.java, F3D_Texture; n64decomp/sm64 include/PR/gbi.h. chained guards and allocation-aware undo require KernelN64; other engines and hardware are untested.",code.strip(),false,group));
    }
}
