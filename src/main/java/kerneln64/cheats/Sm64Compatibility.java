package kerneln64.cheats;
import kerneln64.Main;


import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** ROM-family identification is separate from proof of a hack's RAM layout. */
public final class Sm64Compatibility {
    public record Profile(boolean family,boolean usLayout,String label) {}
    public static final Profile NONE=new Profile(false,false,"Custom codes");
    public static Profile detect(ByteBuffer image) {
        if(image==null || image.capacity()<64)return NONE;
        ByteBuffer rom=image.duplicate().order(ByteOrder.BIG_ENDIAN);
        boolean original=rom.getInt(0x10)==0x635A2BFF && rom.getInt(0x14)==0x8B022326 && (rom.get(0x3E)&255)==0x45;
        byte[] name=new byte[20];rom.position(0x20);rom.get(name);
        String title=new String(name,StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");
        boolean gameId=rom.get(0x3C)=='S' && rom.get(0x3D)=='M';
        boolean titleId=title.contains("SUPERMARIO64") || title.startsWith("SM64");
        // The US startup loop survives ROM extension and many header-renamed hacks.
        // Match several instructions, not file size, filename, or an arbitrary string.
        int[] startup={0x3C088034,0x3C090002,0x2508A580,0x3529CEE0,0x2129FFF8,0xAD000000,0xAD000004,0x1520FFFC,0x21080008};
        boolean usStartup=rom.capacity()>=0x1024;
        if(usStartup)for(int i=0;i<startup.length;i++)if(rom.getInt(0x1000+i*4)!=startup[i]){usStartup=false;break;}
        boolean family=original || gameId || titleId || usStartup;
        if(!family)return NONE;
        int region=rom.get(0x3E)&255;
        boolean us=original || usStartup || (region=='E' && (gameId || titleId));
        // Recompilation can preserve the retail header CRC while moving BSS.
        // Such a build needs its matching ELF before retail presets are offered.
        if(rom.capacity()>=0x1024 && !usStartup)return new Profile(true,false,"SM64 build: matching .elf needed for US presets");
        return new Profile(true,us,original?"SM64 US / extended ROM":us?"SM64 US-based hack (RAM layout may differ)":"SM64 detected (US code addresses not verified)");
    }
}
