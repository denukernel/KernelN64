package kerneln64.rom;
import kerneln64.Main;


import java.awt.image.BufferedImage;

/** Datel's menus draw into RDRAM on the CPU rather than submitting HLE display lists. */
public final class GameSharkFramebuffer {
    public static BufferedImage read() {
        if(Main.mem==null || Main.video==null || !GameSharkCartridge.isFirmware(Main.mem.rom))return null;
        int[] vi=Main.video.regVI;int format=vi[0]&3,stride=vi[2],origin=vi[1]&0xFFFFFF;
        int horizontal=(vi[9]&1023)-(vi[9]>>>16&1023);
        int vertical=(vi[10]&1023)-(vi[10]>>>16&1023);
        int width=horizontal*(vi[12]&4095)/1024;
        int height=vertical*(vi[13]&4095)/2048;
        if(width<=0)width=stride;
        if(height<=0)height=width*3/4;
        int bytes=format==3?4:2;
        if((format!=2 && format!=3) || width<1 || width>1024 || height<1
                || stride<width || (long)origin+((long)(height-1)*stride+width)*bytes>Main.mem.RDRAM.capacity())return null;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        int[] pixels=((java.awt.image.DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for(int i=0;i<pixels.length;i++) {
            int address=origin+(i/width*stride+i%width)*bytes;
            if(bytes==4)pixels[i]=Main.mem.RDRAM.getInt(address)>>>8;
            else {
                int value=Main.mem.RDRAM.getShort(address)&65535;
                pixels[i]=((value>>>11)*255/31<<16)|(((value>>>6)&31)*255/31<<8)|(((value>>>1)&31)*255/31);
            }
        }
        return image;
    }
}
