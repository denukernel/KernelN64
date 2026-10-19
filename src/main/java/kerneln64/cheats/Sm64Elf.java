package kerneln64.cheats;
import kerneln64.Main;


import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Read-only, bounded ELF32/MIPS reader. Physical load addresses are ROM offsets. */
final class Sm64Elf {
    record Symbol(int address,int size,int section) {}
    private final ByteBuffer data;
    private final int[][] sections;
    private final Map<String,Symbol> symbols=new HashMap<>();
    Sm64Elf(Path path,ByteBuffer rom)throws IOException {
        long size=Files.size(path);
        if(size<52 || size>64*1024*1024)throw new IOException("Invalid SM64 ELF size");
        data=ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.BIG_ENDIAN);
        try {
            if(data.getInt(0)!=0x7F454C46 || data.get(4)!=1 || data.get(5)!=2 || data.getShort(18)!=8 || data.getShort(16)!=2)
                throw new IOException("Expected a linked big-endian MIPS ELF32");
            int ph=data.getInt(28),sh=data.getInt(32),pc=data.getShort(44)&65535,sc=data.getShort(48)&65535;
            if(data.getShort(42)!=32 || data.getShort(46)!=40 || sc<1 || sc>4096 || pc>4096)throw new IOException("Invalid ELF tables");
            range(ph,pc*32);range(sh,sc*40);sections=new int[sc][10];
            for(int i=0;i<sc;i++)for(int j=0;j<10;j++)sections[i][j]=data.getInt(sh+i*40+j*4);
            boolean main=false,goddard=false;
            for(int i=0;i<pc;i++) {
                int p=ph+i*32;if(data.getInt(p)!=1)continue;
                int off=data.getInt(p+4),va=data.getInt(p+8),ro=data.getInt(p+12),length=data.getInt(p+16);
                // Check every file-backed CPU segment, including overlays. Segmented
                // compressed graphics do not describe CPU RAM and are not cheat targets.
                if(length==0 || (va&0xE0000000)!=0x80000000)continue;
                range(off,length);
                if(ro<0 || length<0 || ro>rom.capacity()-length)throw new IOException("ELF load segment lies outside ROM");
                for(int j=0;j<length;j++)if(data.get(off+j)!=rom.get(ro+j))throw new IOException("ELF does not match this ROM; rebuild/copy its matching .elf");
                main|=va==0x80246000;goddard|=va==0x8016F000;
            }
            if(!main || !goddard)throw new IOException("Unsupported SM64 CPU segment layout");
            for(int[] s:sections)if(s[1]==2) {
                if(s[9]!=16 || s[6]<0 || s[6]>=sc || s[5]<0 || s[5]>8*1024*1024)throw new IOException("Invalid ELF symbols");
                range(s[4],s[5]);int[] strings=sections[s[6]];range(strings[4],strings[5]);
                for(int off=s[4];off<s[4]+s[5];off+=16) {
                    int n=data.getInt(off),section=data.getShort(off+14)&65535;
                    if(n<0 || n>=strings[5] || section==0 || section>=sc)continue;
                    StringBuilder name=new StringBuilder();
                    for(int j=n;j<strings[5] && name.length()<256;j++){int c=data.get(strings[4]+j)&255;if(c==0)break;name.append((char)c);}
                    Symbol symbol=new Symbol(data.getInt(off+4),data.getInt(off+8),section);
                    symbols.putIfAbsent(name.toString(),symbol);
                }
            }
        } catch(IndexOutOfBoundsException | IllegalArgumentException error){throw new IOException("Malformed SM64 ELF",error);}
    }
    private void range(int offset,int length)throws IOException {
        if(offset<0 || length<0 || offset>data.capacity()-length)throw new IOException("ELF range outside file");
    }
    Symbol symbol(String name){return symbols.get(name);}
    byte[] bytes(Symbol symbol)throws IOException {
        int[] s=sections[symbol.section];
        if(s[1]!=1 || symbol.size<0 || symbol.address<s[3] || symbol.address-s[3]>s[5]-symbol.size)throw new IOException("Symbol is not file-backed");
        int offset=s[4]+symbol.address-s[3];range(offset,symbol.size);
        byte[] out=new byte[symbol.size];data.get(offset,out);return out;
    }
}
