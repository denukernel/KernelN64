package d3d11.framebuffer;
import d3d11.NativeD3D11;
import d3d11.bridge.D3DGlBridge;
import gln64j.core.Rsp;
import gln64j.opengl.OpenGlGdp;

import gln64j.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;

/** GPU color images backed by the N64's big-endian RDRAM, including texture feedback.
 * All operations run on the emulation thread, at a draw or memory-access boundary.
 */
public final class D3DFramebuffers {
    private static D3DFramebuffers active;
    private static final boolean[] pages=new boolean[16384];
    private final long device;
    private final D3DGlBridge bridge;
    private final ByteBuffer ram;
    private final LinkedHashMap<Integer,Buffer> buffers=new LinkedHashMap<>();
    private int selected;
    private boolean busy;
    private static final class Buffer {
        int address,width,height,size; boolean dirty,cpuChanged; long hash=-1;
        int bytes() { return width*height*(size==3?4:2); }
    }
    public D3DFramebuffers(D3DGlBridge bridge,ByteBuffer ram) { this.bridge=bridge;this.device=bridge.device;this.ram=ram;active=this;Arrays.fill(pages,false); }
    public static void read(int address,int length) {
        var a=active;
        if(a!=null && address>=0 && address<a.ram.capacity() && pages[address>>>12])a.synchronizeRange(address,length,false);
    }
    public static void write(int address,int length) {
        var a=active;
        if(a!=null && address>=0 && address<a.ram.capacity() && pages[address>>>12])a.synchronizeRange(address,length,true);
    }
    public static void textureRead(int address,int length) { if(active!=null)active.synchronizeRange(address,length,false); }
    public static void depthImage(int address) { if(active!=null)NativeD3D11.selectDepth(active.device,address); }
    public static void clearDepth(int left,int top,int right,int bottom) {
        if(active==null)return;
        var a=active;NativeD3D11.selectDepth(a.device,Rsp.gdp.depthImageAddress());
        int w=Math.max(1,OpenGlGdp.screenWidth),h=Math.max(1,OpenGlGdp.screenHeight);
        NativeD3D11.clearDepthRect(a.device,left*a.bridge.width/w,top*a.bridge.height/h,
                (right-left)*a.bridge.width/w,(bottom-top)*a.bridge.height/h,1);
    }
    public static void capture() { if(active!=null)active.flushAll(); }
    public static void restored() { if(active!=null)active.reset(); }
    public static boolean enabled() { return active!=null; }
    public void close() { if(active==this){active=null;Arrays.fill(pages,false);} }
    public void reset() {
        NativeD3D11.resetTargets(device);selected=0;
        buffers.clear();Arrays.fill(pages,false);
    }
    private long hash(Buffer b) {
        CRC32 crc=new CRC32();crc.update(ram.array(),b.address,b.bytes());return crc.getValue();
    }
    public void beginList() {
        // DMA and HLE code may write the backing array without going through Memory.
        for(Buffer b:buffers.values())if(b.hash!=-1 && hash(b)!=b.hash){b.cpuChanged=true;b.dirty=false;}
    }
    private void select(int address) { NativeD3D11.selectTarget(device,address);selected=address; }
    public void beforeDraw() {
        if(busy || Rsp.gdp==null)return;
        NativeD3D11.selectDepth(device,Rsp.gdp.depthImageAddress());
        var image=Rsp.gdp.colorImage;
        if(image.address<0 || image.format!=0 || image.size<2 || image.size>3 || image.width<1)return;
        int h=Math.max(image.height,(int)Math.ceil(Rsp.gdp.scissor.lry));
        h=Math.max(1,Math.min(h,Math.max(1,OpenGlGdp.screenHeight)));
        if((long)image.address+(long)image.width*h*(image.size==3?4:2)>ram.capacity())return;
        Buffer b=buffers.get(image.address);
        if(b!=null && (b.width!=image.width || b.size!=image.size)) {
            flush(b);buffers.remove(b.address);if(selected==b.address)select(0);NativeD3D11.forgetTarget(device,b.address);b=null;
        }
        if(b==null) {
            if(buffers.size()>=4) {
                Buffer old=buffers.values().iterator().next();flush(old);buffers.remove(old.address);
                if(selected==old.address)select(0);NativeD3D11.forgetTarget(device,old.address);
            }
            b=new Buffer();b.address=image.address;b.width=image.width;b.height=h;b.size=image.size;b.cpuChanged=true;
            buffers.put(b.address,b);
        }
        b.height=Math.max(b.height,h);mapPages(b);
        if(selected!=b.address)select(b.address);
        if(b.cpuChanged)upload(b);
        b.dirty=true;
    }
    private void mapPages(Buffer b) {
        for(int p=b.address>>>12;p<=Math.min(pages.length-1,(b.address+b.bytes()-1)>>>12);p++)pages[p]=true;
    }
    private void synchronizeRange(int address,int length,boolean write) {
        if(busy || length<=0)return;
        // Submit geometry while target selection and dirty tracking are still enabled.
        OpenGlGdp.OGL_DrawTriangles();
        busy=true;
        try {
            for(Buffer b:buffers.values())if((long)address+length>b.address && address<(long)b.address+b.bytes()) {
                flush(b);if(write){b.cpuChanged=true;b.hash=-1;}
            }
        } finally { busy=false; }
    }
    private void flushAll() { OpenGlGdp.OGL_DrawTriangles();for(Buffer b:buffers.values())flush(b); }
    private void flush(Buffer b) {
        if(!b.dirty || b.cpuChanged)return;
        // ROM/SP DMA and HLE writers bypass Memory. The game may reuse a color
        // image's RAM for code or heap data before the next display list arrives.
        // Never overwrite those newer bytes with a stale GPU readback.
        if(b.hash!=-1 && hash(b)!=b.hash) {
            b.cpuChanged=true;b.dirty=false;return;
        }
        int previous=selected;select(b.address);
        byte[] pixels=NativeD3D11.read(device);
        int gpuWidth=bridge.width;
        int gpuHeight=pixels.length/4/gpuWidth;
        int logicalWidth=Math.max(1,OpenGlGdp.screenWidth),logicalHeight=Math.max(1,OpenGlGdp.screenHeight);
        for(int y=0;y<b.height;y++)for(int x=0;x<b.width;x++) {
            int sx=Math.min(gpuWidth-1,(int)((x+0.5)*gpuWidth/logicalWidth));
            int sy=Math.min(gpuHeight-1,(int)((y+0.5)*gpuHeight/logicalHeight));
            int i=(sy*gpuWidth+sx)*4,r=pixels[i]&255,g=pixels[i+1]&255,blue=pixels[i+2]&255,coverage=Math.round((pixels[i+3]&255)*7f/255);
            int offset=b.address+(y*b.width+x)*(b.size==3?4:2);
            if(b.size==3)ram.putInt(offset,(r<<24)|(g<<16)|(blue<<8)|(coverage<<5));
            else ram.putShort(offset,(short)(((r>>>3)<<11)|((g>>>3)<<6)|((blue>>>3)<<1)|(coverage>>>2)));
        }
        b.dirty=false;b.hash=hash(b);select(previous);
    }
    private void upload(Buffer b) {
        int w=bridge.width;
        int h=bridge.height;
        ByteBuffer pixels=ByteBuffer.allocateDirect(w*h*4);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int sx=x*Math.max(1,OpenGlGdp.screenWidth)/w,sy=y*Math.max(1,OpenGlGdp.screenHeight)/h;
            if(sx>=b.width || sy>=b.height){pixels.putInt(0);continue;}
            int offset=b.address+(sy*b.width+sx)*(b.size==3?4:2),r,g,blue,a;
            if(b.size==3){int v=ram.getInt(offset);r=v>>>24;g=(v>>>16)&255;blue=(v>>>8)&255;a=Math.round(((v>>>5)&7)*255f/7);}
            else {int v=ram.getShort(offset)&65535;r=((v>>>11)&31)*255/31;g=((v>>>6)&31)*255/31;blue=((v>>>1)&31)*255/31;a=(v&1)!=0?255:0;}
            pixels.put((byte)r).put((byte)g).put((byte)blue).put((byte)a);
        }
        pixels.flip();NativeD3D11.image(device,w,h,pixels);b.cpuChanged=false;b.hash=hash(b);
    }
    public int display(int origin) {
        for(Buffer b:buffers.values())if(origin>=b.address && origin<(long)b.address+b.bytes()) {
            if(b.cpuChanged){int previous=selected;select(b.address);upload(b);select(previous);}
            return b.address;
        }
        return selected;
    }
    public int selected() { return selected; }
}
