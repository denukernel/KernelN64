package kerneln64.rom;
import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cpu;
import kerneln64.memory.Memory;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Recognized Datel standalone firmware bootstrap and its cartridge ROM window.
 * Source images remain immutable; this is separate from the RAM cheat engine. */
public final class GameSharkCartridge {
    private record Profile(ByteBuffer rom,boolean supported) {}
    private static volatile Profile cached;
    public static boolean isFirmware(ByteBuffer rom) {
        if(rom==null || rom.capacity()!=0x40000)return false;
        Profile profile=cached;if(profile!=null && profile.rom==rom)return profile.supported;
        byte[] text=new byte[0x18];rom.get(0x20,text);
        String header=new String(text,StandardCharsets.US_ASCII);
        boolean datel=header.startsWith("(C) DATEL D&D NU") || header.startsWith("(C) MUSHROOM &NU");
        boolean v2=rom.getInt(0x1000)==0x04110000 && rom.getInt(0x1008)==0x3C03B0C0
            && rom.getInt(0x100C)==0x3C028040;
        boolean v3=rom.getInt(0x1080)==0x3C08B0C0 && rom.getInt(0x1084)==0x35081000
            && rom.getInt(0x1088)==0x3C098004;
        boolean supported=rom.getInt(0)==0x80371240 && datel && (v2||v3);
        cached=new Profile(rom,supported);return supported;
    }
    public static int romOffset(ByteBuffer rom,int address) {
        int window=address&0xFFF00000;
        if((window==0x10C00000 || window==0x11C00000 || window==0x1EC00000
                || address>=0x11000000 && address<0x11000040) && isFirmware(rom))return address&0x3FFFF;
        return address-0x10000000;
    }
    public static boolean flashWindow(ByteBuffer rom,int address) {
        return (address&0xFFF00000)==0x1EC00000 && isFirmware(rom);
    }
    /** No parallel host is connected and the physical GS button is released.
     * The header/flash windows remain available in standalone firmware mode.
     * Register locations follow RWeick's N64GSVerilog LZ9FC17 implementation. */
    public static Integer readRegister(ByteBuffer rom,int address) {
        if(!isFirmware(rom))return null;
        int window=address&0xFFF00000;
        if(window==0x11E00000 || window==0x11F00000 || window==0x1EE00000 || window==0x1EF00000) {
            int offset=((address&0xFFFFC)+((window&0x100000)!=0?2:0))&0x3FFFF;
            int half=rom.getShort(offset)&65535;return half<<16|half;
        }
        return switch(address) {
            case 0x10400000,0x11400000 -> 0xED00ED00;
            case 0x1E400000 -> 0xFF80FF80;
            case 0x11300220 -> 0x44410000;
            case 0x10300260 -> 0x54450000;
            default -> null;
        };
    }
    public static boolean writeRegister(ByteBuffer rom,int address,int value) {
        if(!isFirmware(rom))return false;
        int block=address&0xFFF00000,offset=address&0xFFFFF;
        return (block==0x10400000 || block==0x11400000 || block==0x1E400000)
                && (offset==0x400 || offset==0x600 || offset==0x800)
                || address==0x1E5FFFFC || address==0x12600400; // Absent alternate hardware probe.
    }
    public static boolean boot(Rom rom,Memory memory,Cpu cpu) {
        if(rom==null || !isFirmware(rom.data))return false;
        int entry=rom.data.getInt(8),target=entry&0x1FFFFFFF,length=rom.data.capacity()-0x1000;
        if((entry&0xE0000000)!=0x80000000 || (entry&3)!=0 || target>memory.RDRAM.capacity()-length)
            throw new IllegalArgumentException("GameShark firmware entry point is outside emulated RDRAM");
        ByteBuffer destination=memory.RDRAM.duplicate(),source=rom.data.duplicate();
        source.position(0x1000);destination.position(target);destination.put(source);
        // IPL3 hand-off globals (TV, reset, cartridge base, NMI and RAM size).
        memory.RDRAM.putInt(0x300,1);memory.RDRAM.putInt(0x304,0);
        memory.RDRAM.putInt(0x308,0xB0000000);memory.RDRAM.putInt(0x30C,0);
        memory.RDRAM.putInt(0x314,0);memory.RDRAM.putInt(0x318,memory.RDRAM.capacity());
        // The skipped cartridge checksum normally consumes the controller power-on delay.
        cpu.cop0.CP0[Cop0.COUNT_REGISTER]=25_000_000;
        cpu.GPR[20]=1;cpu.GPR[22]=0x3F;cpu.GPR[29]=0xFFFFFFFF803FFF00L;
        cpu.pc=entry;
        System.out.printf("GameShark firmware bootstrap at %08X; cartridge window B0C00000%n",entry);
        return true;
    }
}
