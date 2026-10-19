package d3d11.bridge;
import d3d11.bridge.generated.D3DGL;
import d3d11.NativeD3D11;
import d3d11.config.D3D11Settings;
import d3d11.framebuffer.D3DFramebuffers;

import java.nio.*;
import java.util.*;
import javax.media.opengl.GL;

/** Adapts the existing HLE decoder's fixed-function drawing API to D3D11 draws.
 * No OpenGL context or OpenGL driver calls are used by this adapter.
 */
public final class D3DGlBridge {
    public final GL gl = new D3DGL(this);
    public final long device;
    public D3DFramebuffers framebuffers;
    private final int samples;
    final int[] selectors = new int[16];
    public int cycles = 1, width, height;
    private final Set<Integer> enabled = new HashSet<>();
    private int active, client, nextTexture = 1, primitive;
    private int depthFunc = GL.GL_LESS, alphaFunc = GL.GL_ALWAYS, sourceBlend = GL.GL_ONE, destinationBlend = GL.GL_ZERO;
    private int cullFace = GL.GL_BACK;
    private boolean depthWrite = true, wire;
    private float alphaReference;
    private final float[] clear = new float[4], color = {1,1,1,1}, uv = new float[4], fogColor = new float[4];
    private float fogStart = 0, fogEnd = 255;
    private final int[] bound = new int[2], viewport = new int[4], scissor = new int[4];
    private final Map<Integer,int[]> parameters = new HashMap<>();
    private FloatBuffer vertexPointer, colorPointer;
    private final FloatBuffer[] texPointer = new FloatBuffer[2];
    private int vertexStride = 4, colorStride = 4;
    private final int[] texStride = {2,2};
    private boolean colorArray;
    private float[] projection = identity();
    private int matrixMode = GL.GL_MODELVIEW;
    private final ArrayList<float[]> immediate = new ArrayList<>();
    public volatile long drawCalls, triangles;

    public D3DGlBridge(long device, int width, int height) {
        this.device = device; this.samples = NativeD3D11.samples(device); this.width = width; this.height = height;
        viewport[2] = scissor[2] = width; viewport[3] = scissor[3] = height;
        Arrays.fill(selectors, 20); selectors[3] = 4; selectors[7] = 12;
    }
    private static float[] identity() { float[] m = new float[16]; for(int i=0;i<4;i++)m[i*5]=1;return m; }
    private static int n(Object[] args,int i) { return ((Number)args[i]).intValue(); }
    private static float f(Object[] args,int i) { return ((Number)args[i]).floatValue(); }
    private static void floats(Object source, int offset, float[] target, int count) {
        if (source instanceof float[] data) System.arraycopy(data,offset,target,0,count);
        else { FloatBuffer data=(FloatBuffer)source; for(int i=0;i<count;i++)target[i]=data.get(data.position()+offset+i); }
    }
    public Object call(String name, Object[] a) {
        switch(name) {
            case "toString": return "D3D11 HLE command adapter";
            case "hashCode": return System.identityHashCode(this);
            case "equals": return gl==a[0];
            case "glGetString": return switch(n(a,0)) { case GL.GL_EXTENSIONS -> "GL_ARB_multitexture"; case GL.GL_VERSION -> "D3D11 HLE adapter"; default -> "KernelN64 Direct3D 11"; };
            case "glGetIntegerv": { int value=n(a,0)==GL.GL_MAX_TEXTURE_UNITS?2:4096; if(a[1] instanceof int[] data)data[n(a,2)]=value;else((IntBuffer)a[1]).put(((IntBuffer)a[1]).position(),value); return null; }
            case "glIsEnabled": return enabled.contains(n(a,0));
            case "glEnable": enabled.add(n(a,0)); return null;
            case "glDisable": enabled.remove(n(a,0)); return null;
            case "glDepthMask": depthWrite=(boolean)a[0]; return null;
            case "glDepthFunc": depthFunc=n(a,0); return null;
            case "glAlphaFunc": alphaFunc=n(a,0); alphaReference=f(a,1); return null;
            case "glBlendFunc": sourceBlend=n(a,0);destinationBlend=n(a,1);return null;
            case "glCullFace": cullFace=n(a,0);return null;
            case "glPolygonMode": wire=n(a,1)==GL.GL_LINE;return null;
            case "glClearColor": for(int i=0;i<4;i++)clear[i]=f(a,i);return null;
            case "glClear": if(framebuffers!=null && (n(a,0)&GL.GL_COLOR_BUFFER_BIT)!=0)framebuffers.beforeDraw();NativeD3D11.clear(device,n(a,0),clear[0],clear[1],clear[2],clear[3]);return null;
            case "glViewport": for(int i=0;i<4;i++)viewport[i]=n(a,i);return null;
            case "glScissor": for(int i=0;i<4;i++)scissor[i]=n(a,i);return null;
            case "glMatrixMode": matrixMode=n(a,0);return null;
            case "glLoadIdentity": if(matrixMode==GL.GL_PROJECTION)projection=identity();return null;
            case "glOrtho": {
                double l=((Number)a[0]).doubleValue(),r=((Number)a[1]).doubleValue(),b=((Number)a[2]).doubleValue(),t=((Number)a[3]).doubleValue(),near=((Number)a[4]).doubleValue(),far=((Number)a[5]).doubleValue();
                projection=new float[]{(float)(2/(r-l)),0,0,0, 0,(float)(2/(t-b)),0,0, 0,0,(float)(-2/(far-near)),0, (float)(-(r+l)/(r-l)),(float)(-(t+b)/(t-b)),(float)(-(far+near)/(far-near)),1};return null;
            }
            case "glActiveTexture": active=Math.min(1,n(a,0)-GL.GL_TEXTURE0);return null;
            case "glClientActiveTexture": client=Math.min(1,n(a,0)-GL.GL_TEXTURE0);return null;
            case "glBindTexture": bound[active]=n(a,1);parameters.computeIfAbsent(n(a,1),id->new int[]{GL.GL_LINEAR,GL.GL_REPEAT,GL.GL_REPEAT});return null;
            case "glGenTextures": { int count=n(a,0);for(int i=0;i<count;i++){int id=nextTexture++;if(a[1] instanceof int[] data)data[n(a,2)+i]=id;else ((IntBuffer)a[1]).put(((IntBuffer)a[1]).position()+i,id);}return null; }
            case "glDeleteTextures": { for(int i=0;i<n(a,0);i++){int id=a[1] instanceof int[] data?data[n(a,2)+i]:((IntBuffer)a[1]).get(((IntBuffer)a[1]).position()+i);parameters.remove(id);NativeD3D11.deleteTexture(device,id);}return null; }
            case "glTexParameteri": {
                int[] p=parameters.computeIfAbsent(bound[active],id->new int[]{GL.GL_LINEAR,GL.GL_REPEAT,GL.GL_REPEAT});
                if(n(a,1)==GL.GL_TEXTURE_MIN_FILTER||n(a,1)==GL.GL_TEXTURE_MAG_FILTER)p[0]=n(a,2);
                else if(n(a,1)==GL.GL_TEXTURE_WRAP_S)p[1]=n(a,2);else if(n(a,1)==GL.GL_TEXTURE_WRAP_T)p[2]=n(a,2);return null;
            }
            case "glTexImage2D": upload(a);return null;
            case "glVertexPointer": vertexPointer=((FloatBuffer)a[3]).slice();vertexStride=n(a,2)==0?n(a,0):n(a,2)/4;return null;
            case "glColorPointer": colorPointer=((FloatBuffer)a[3]).slice();colorStride=n(a,2)==0?n(a,0):n(a,2)/4;return null;
            case "glTexCoordPointer": texPointer[client]=((FloatBuffer)a[3]).slice();texStride[client]=n(a,2)==0?n(a,0):n(a,2)/4;return null;
            case "glEnableClientState": if(n(a,0)==GL.GL_COLOR_ARRAY)colorArray=true;return null;
            case "glDisableClientState": if(n(a,0)==GL.GL_COLOR_ARRAY)colorArray=false;return null;
            case "glColor4f": for(int i=0;i<4;i++)color[i]=f(a,i);return null;
            case "glColor4fv": floats(a[0],a.length>1?n(a,1):0,color,4);return null;
            case "glTexCoord2f": uv[0]=f(a,0);uv[1]=f(a,1);return null;
            case "glMultiTexCoord2f": { int unit=Math.min(1,n(a,0)-GL.GL_TEXTURE0);uv[unit*2]=f(a,1);uv[unit*2+1]=f(a,2);return null; }
            case "glBegin": primitive=n(a,0);immediate.clear();return null;
            case "glVertex4f": immediate.add(vertex(new float[]{f(a,0),f(a,1),f(a,2),f(a,3)},color,uv));return null;
            case "glEnd": submit(immediate,primitive);return null;
            case "glDrawArrays": {
                ArrayList<float[]> list=new ArrayList<>();
                for(int i=n(a,1);i<n(a,1)+n(a,2);i++) {
                    float[] position=new float[4],shade=color.clone(),coords=new float[4];
                    for(int j=0;j<4;j++)position[j]=vertexPointer.get(i*vertexStride+j);
                    if(colorArray&&colorPointer!=null)for(int j=0;j<4;j++)shade[j]=colorPointer.get(i*colorStride+j);
                    for(int unit=0;unit<2;unit++)if(texPointer[unit]!=null)for(int j=0;j<2;j++)coords[unit*2+j]=texPointer[unit].get(i*texStride[unit]+j);
                    list.add(vertex(position,shade,coords));
                }
                submit(list,n(a,0));return null;
            }
            case "glFogfv": if(n(a,0)==GL.GL_FOG_COLOR)floats(a[1],a.length>2?n(a,2):0,fogColor,4);return null;
            case "glFogf": if(n(a,0)==GL.GL_FOG_START)fogStart=f(a,1);if(n(a,0)==GL.GL_FOG_END)fogEnd=f(a,1);return null;
            // HLE transformations already supply clip-space vertices. Unsupported
            // stipple is not implemented; depth bias is supplied in the native raster state.
            case "glFogi", "glDepthRange", "glLineWidth", "glPolygonOffset", "glPolygonStipple", "glTexEnvi", "glTexEnvfv", "glFinish", "glFlush": return null;
            default: throw new UnsupportedOperationException("D3D11 HLE command not implemented: "+name);
        }
    }
    private float[] vertex(float[] position,float[] shade,float[] coords) {
        float[] result=new float[13];
        for(int row=0;row<4;row++)for(int col=0;col<4;col++)result[row]+=projection[col*4+row]*position[col];
        System.arraycopy(shade,0,result,4,4);System.arraycopy(coords,0,result,8,4);
        var gsp=gln64j.core.Rsp.gsp;
        if(framebuffers!=null && gsp!=null && (gsp.geometryMode & gln64j.core.Gbi.G_FOG)!=0)
            result[12]=fogFactor(result[2],result[3],gsp.fog.multiplier,gsp.fog.offset);
        return result;
    }
    static float fogFactor(float z,float w,short multiplier,short offset) {
        return w<=0?1:Math.max(0,Math.min(1,(z/w*multiplier+offset)/255f));
    }
    private void upload(Object[] a) {
        int w=n(a,3),h=n(a,4),type=n(a,7);ByteBuffer rgba=ByteBuffer.allocateDirect(w*h*4);
        ByteBuffer data=((ByteBuffer)a[8]).duplicate().order(((ByteBuffer)a[8]).order());
        if(type==GL.GL_UNSIGNED_BYTE) { for(int i=0;i<w*h*4;i++)rgba.put(data.get(i)); }
        else for(int i=0;i<w*h;i++) {
            int value=data.getShort(i*2)&65535;
            if(type==GL.GL_UNSIGNED_SHORT_4_4_4_4)rgba.put((byte)(((value>>12)&15)*17)).put((byte)(((value>>8)&15)*17)).put((byte)(((value>>4)&15)*17)).put((byte)((value&15)*17));
            else if(type==GL.GL_UNSIGNED_SHORT_5_5_5_1)rgba.put((byte)(((value>>11)&31)*255/31)).put((byte)(((value>>6)&31)*255/31)).put((byte)(((value>>1)&31)*255/31)).put((byte)((value&1)*255));
            else throw new UnsupportedOperationException("Unsupported D3D11 texture upload type: "+type);
        }
        rgba.flip();NativeD3D11.texture(device,bound[active],w,h,rgba);
    }
    private void submit(List<float[]> input,int mode) {
        ArrayList<float[]> output=new ArrayList<>();
        if(mode==GL.GL_QUADS)for(int i=0;i+3<input.size();i+=4)for(int index:new int[]{0,1,2,0,2,3})output.add(input.get(i+index));
        else if(mode==GL.GL_TRIANGLE_STRIP)for(int i=2;i<input.size();i++){output.add(input.get(i%2==0?i-2:i-1));output.add(input.get(i%2==0?i-1:i-2));output.add(input.get(i));}
        else if(mode==GL.GL_TRIANGLES||mode==GL.GL_LINES)output.addAll(input);
        else throw new UnsupportedOperationException("Unsupported D3D11 primitive: "+mode);
        float[] vertices=new float[output.size()*13];for(int i=0;i<output.size();i++)System.arraycopy(output.get(i),0,vertices,i*13,13);
        int[] s=new int[25];s[0]=enabled.contains(GL.GL_DEPTH_TEST)?1:0;s[1]=depthWrite?1:0;s[2]=depthFunc;
        s[3]=enabled.contains(GL.GL_BLEND)?1:0;s[4]=sourceBlend;s[5]=destinationBlend;s[6]=enabled.contains(GL.GL_SCISSOR_TEST)?1:0;
        System.arraycopy(scissor,0,s,7,4);s[11]=wire?1:0;s[12]=enabled.contains(GL.GL_CULL_FACE)?cullFace:0;
        System.arraycopy(viewport,0,s,13,4);System.arraycopy(bound,0,s,17,2);
        for(int unit=0;unit<2;unit++){
            int[] p=parameters.getOrDefault(bound[unit],new int[]{GL.GL_LINEAR,GL.GL_REPEAT,GL.GL_REPEAT});System.arraycopy(p,0,s,19+unit*3,3);
            if(D3D11Settings.filter==1)s[19+unit*3]=GL.GL_LINEAR;
            else if(D3D11Settings.filter==2)s[19+unit*3]=GL.GL_NEAREST;
        }
        float[] c=new float[52];var prim=gln64j.rdp.combiners.Combiners.primColor;
        c[0]=prim.r;c[1]=prim.g;c[2]=prim.b;c[3]=prim.a;System.arraycopy(gln64j.rdp.combiners.Combiners.envColor,0,c,4,4);System.arraycopy(fogColor,0,c,8,4);
        c[12]=cycles;c[13]=enabled.contains(GL.GL_ALPHA_TEST)?alphaFunc:GL.GL_ALWAYS;c[14]=alphaReference;
        c[15]=enabled.contains(GL.GL_FOG)?1:0;for(int i=0;i<16;i++)c[16+i]=selectors[i];c[32]=fogStart;c[33]=fogEnd;c[34]=prim.l;c[35]=enabled.contains(GL.GL_POLYGON_OFFSET_FILL)?1:0;
        if(framebuffers!=null) {
            framebuffers.beforeDraw();
            var gdp=gln64j.core.Rsp.gdp; int rdpMode=gdp.otherMode.w1;
            System.arraycopy(gdp.fogColor,0,c,8,4);System.arraycopy(gdp.blendColor,0,c,48,4);
            boolean fog=(gln64j.core.Rsp.gsp.geometryMode & gln64j.core.Gbi.G_FOG)!=0;
            c[15]=fog?1:0;
            // Copy/fill bypass the RDP blender. Rectangles also bypass geometry fog.
            if(gln64j.rdp.Gdp.RDP_GETOM_CYCLE_TYPE(gdp.otherMode)<gln64j.core.Gbi.G_CYC_COPY) {
                c[36]=1;c[37]=rdpMode & 65535;c[38]=samples;c[39]=fog && projection[0]==1 && projection[5]==1?1:0;
                for(int cycle=0;cycle<2;cycle++) {
                    int shift=cycle*2,index=40+cycle*4;
                    c[index]=(rdpMode >>> (30-shift))&3;c[index+1]=(rdpMode >>> (26-shift))&3;
                    c[index+2]=(rdpMode >>> (22-shift))&3;c[index+3]=(rdpMode >>> (18-shift))&3;
                }
                // Only fog blender inputs select fog; the geometry bit alone is insufficient.
                c[15]=c[39]>0 && (c[40]==3 || c[42]==3 || (cycles==2 && (c[44]==3 || c[46]==3)))?1:0;
                s[3]=0;
            } else {
                c[15]=0;s[0]=s[1]=s[3]=0;
                if(gln64j.rdp.Gdp.RDP_GETOM_CYCLE_TYPE(gdp.otherMode)==gln64j.core.Gbi.G_CYC_FILL)c[13]=GL.GL_ALWAYS;
            }
        }
        NativeD3D11.draw(device,vertices,mode==GL.GL_LINES?1:4,s,c);drawCalls++;triangles+=mode==GL.GL_LINES?0:output.size()/3;
    }
}
