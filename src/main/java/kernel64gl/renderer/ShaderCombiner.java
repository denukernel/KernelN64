package kernel64gl.renderer;

import javax.media.opengl.GL;
import kernel64gl.engine.rdp.combiners.Combiners;
import kernel64gl.engine.opengl.OpenGlGdp;
import kernel64gl.engine.core.Rsp;
import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.config.RendererSettings;

/** Kernel64GL GPU color/alpha combiner; does not reduce equations to texture stages. */
public final class ShaderCombiner extends Combiners {
    private int program,muxLocation,cyclesLocation,primitiveLocation,environmentLocation,lodLocation,fogLocation;
    private final java.util.Map<Long,int[]> expanded=new java.util.LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,int[]> e){return size()>512;}
    };
    private int lastFog=-1;
    private int viewLocation,texturesLocation,lastView=-1,lastTextures=-1;
    private static final String VERTEX="""
        #version 120
        varying vec4 shade;
        varying vec2 uv0,uv1;
        varying float fogAmount;
        void main() {
            gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;
            shade=gl_Color;uv0=gl_MultiTexCoord0.st;uv1=gl_MultiTexCoord1.st;
            fogAmount=clamp(gl_FogCoord/255.0,0.0,1.0);
        }
        """;
    private static final String FRAGMENT="""
        #version 120
        uniform sampler2D texture0,texture1;
        uniform ivec4 selectors[4];
        uniform int cycles,fogEnabled;
        uniform int debugView,texturesEnabled;
        uniform vec4 primitiveColor,environmentColor;
        uniform float primitiveLod;
        varying vec4 shade;
        varying vec2 uv0,uv1;
        varying float fogAmount;
        vec4 source(int id,vec4 combined,vec4 t0,vec4 t1) {
            if(id==0)return combined;if(id==1)return t0;if(id==2)return t1;
            if(id==3)return primitiveColor;if(id==4)return shade;if(id==5)return environmentColor;
            if(id==6||id==7)return vec4(0.5);
            if(id==8)return combined.aaaa;if(id==9)return t0.aaaa;if(id==10)return t1.aaaa;
            if(id==11)return primitiveColor.aaaa;if(id==12)return shade.aaaa;if(id==13)return environmentColor.aaaa;
            if(id==15)return vec4(primitiveLod);
            if(id==16)return vec4(fract(sin(dot(gl_FragCoord.xy,vec2(12.9898,78.233)))*43758.5453));
            if(id==19)return vec4(1.0);return vec4(0.0);
        }
        void main() {
            vec4 t0=texturesEnabled!=0?texture2D(texture0,uv0):vec4(1.0),t1=texturesEnabled!=0?texture2D(texture1,uv1):vec4(1.0),result=vec4(0.0);
            for(int cycle=0;cycle<2;cycle++) {
                if(cycle>=cycles)break;
                ivec4 rgb=selectors[cycle*2],alpha=selectors[cycle*2+1];
                vec3 color=(source(rgb.x,result,t0,t1).rgb-source(rgb.y,result,t0,t1).rgb)*source(rgb.z,result,t0,t1).rgb+source(rgb.w,result,t0,t1).rgb;
                float a=(source(alpha.x,result,t0,t1).a-source(alpha.y,result,t0,t1).a)*source(alpha.z,result,t0,t1).a+source(alpha.w,result,t0,t1).a;
                result=clamp(vec4(color,a),0.0,1.0);
            }
            if(fogEnabled!=0)result.rgb=mix(result.rgb,gl_Fog.color.rgb,clamp(fogAmount,0.0,1.0));
            if(debugView==1)result=vec4(shade.rgb,1.0);
            else if(debugView==2)result=vec4(t0.rgb,1.0);
            else if(debugView==3)result=vec4(vec3(fogAmount),1.0);
            else if(debugView==4)result=vec4(vec3(gl_FragCoord.z),1.0);
            gl_FragColor=result;
        }
        """;
    private static int shader(GL gl,int type,String source) {
        int shader=gl.glCreateShader(type);gl.glShaderSource(shader,1,new String[]{source},(int[])null,0);gl.glCompileShader(shader);
        int[] status=new int[1];gl.glGetShaderiv(shader,GL.GL_COMPILE_STATUS,status,0);
        if(status[0]==0){byte[] log=new byte[8192];gl.glGetShaderInfoLog(shader,log.length,status,0,log,0);gl.glDeleteShader(shader);throw new IllegalStateException("Kernel64GL shader compilation: "+new String(log,java.nio.charset.StandardCharsets.UTF_8).trim());}
        return shader;
    }
    @Override public void init() {
        GL gl=OpenGlGdp.gl;
        lastFog=-1;
        lastView=lastTextures=-1;
        int vs=shader(gl,GL.GL_VERTEX_SHADER,VERTEX),fs=shader(gl,GL.GL_FRAGMENT_SHADER,FRAGMENT);
        program=gl.glCreateProgram();gl.glAttachShader(program,vs);gl.glAttachShader(program,fs);gl.glLinkProgram(program);gl.glDeleteShader(vs);gl.glDeleteShader(fs);
        int[] status=new int[1];gl.glGetProgramiv(program,GL.GL_LINK_STATUS,status,0);
        if(status[0]==0){byte[] log=new byte[8192];gl.glGetProgramInfoLog(program,log.length,status,0,log,0);gl.glDeleteProgram(program);throw new IllegalStateException("Kernel64GL shader linking: "+new String(log,java.nio.charset.StandardCharsets.UTF_8).trim());}
        gl.glUseProgram(program);
        muxLocation=gl.glGetUniformLocation(program,"selectors[0]");cyclesLocation=gl.glGetUniformLocation(program,"cycles");
        primitiveLocation=gl.glGetUniformLocation(program,"primitiveColor");environmentLocation=gl.glGetUniformLocation(program,"environmentColor");
        lodLocation=gl.glGetUniformLocation(program,"primitiveLod");fogLocation=gl.glGetUniformLocation(program,"fogEnabled");
        viewLocation=gl.glGetUniformLocation(program,"debugView");texturesLocation=gl.glGetUniformLocation(program,"texturesEnabled");
        gl.glUniform1i(viewLocation,RendererSettings.debugView);lastView=RendererSettings.debugView;
        gl.glUniform1i(texturesLocation,RendererSettings.textures?1:0);lastTextures=RendererSettings.textures?1:0;
        gl.glUniform1i(gl.glGetUniformLocation(program,"texture0"),0);gl.glUniform1i(gl.glGetUniformLocation(program,"texture1"),1);
        vertex.color=SHADE;vertex.alpha=SHADE_ALPHA;vertex.secondaryColor=ZERO;
    }
    @Override public void setCombine(GL gl,boolean twoCycles,long mux) {
        int[] values=expanded.computeIfAbsent(mux,Combiners::expandMux);gl.glUseProgram(program);gl.glUniform4iv(muxLocation,4,values,0);gl.glUniform1i(cyclesLocation,twoCycles?2:1);
        usesT0=usesT1=usesNoise=false;
        for(int i=0;i<(twoCycles?16:8);i++){int value=values[i];usesT0|=value==TEXEL0||value==TEXEL0_ALPHA;usesT1|=value==TEXEL1||value==TEXEL1_ALPHA;usesNoise|=value==NOISE;}
        if(RendererSettings.debugView==2)usesT0=true;
        if(!RendererSettings.textures)usesT0=usesT1=false;
    }
    @Override public void updateCombineColors(GL gl) {
        gl.glUseProgram(program);gl.glUniform4f(primitiveLocation,primColor.r,primColor.g,primColor.b,primColor.a);
        gl.glUniform4f(environmentLocation,envColor[0],envColor[1],envColor[2],envColor[3]);gl.glUniform1f(lodLocation,primColor.l);
    }
    @Override public void beginTextureUpdate(GL gl) {}
    @Override public void endTextureUpdate(GL gl) {}
    public static void beforeDraw(boolean geometry) {
        if(OpenGlGdp.combiners instanceof ShaderCombiner shader) {
            GL gl=OpenGlGdp.gl;gl.glUseProgram(shader.program);
            int fog=geometry && RendererSettings.enableFog && (Rsp.gsp.geometryMode&Gbi.G_FOG)!=0?1:0;
            if(shader.lastFog!=fog){gl.glUniform1i(shader.fogLocation,fog);shader.lastFog=fog;}
            int textures=RendererSettings.textures?1:0,view=RendererSettings.debugView;
            if(shader.lastView!=view){gl.glUniform1i(shader.viewLocation,view);shader.lastView=view;}
            if(shader.lastTextures!=textures){gl.glUniform1i(shader.texturesLocation,textures);shader.lastTextures=textures;}
        }
    }
}
