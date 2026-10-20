package gln64j.rdp.combiners;

import gln64j.core.Gbi;
import gln64j.core.Rsp;
import gln64j.opengl.OpenGlGdp;
import gln64j.rdp.Gdp;
import javax.media.opengl.GL;

/** Retains legacy combiners, adding a fragment shader for paired trilinear blending. */
public final class PairedLodCombiner extends Combiners {
    private boolean paired;
    private int program,scaleLocation,enabledLocation,fogLocation;
    private static final String VERTEX="""
        #version 120
        varying vec2 uv0,uv1;
        varying vec4 shade;
        varying float fogAmount;
        void main(){
            gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;
            uv0=gl_MultiTexCoord0.st;uv1=gl_MultiTexCoord1.st;
            shade=gl_Color;fogAmount=clamp(gl_FogCoord/255.0,0.0,1.0);
        }
        """;
    private static final String FRAGMENT="""
        #version 120
        uniform sampler2D texture0,texture1;
        uniform vec4 lodScale;
        uniform int lodEnabled,fogEnabled;
        varying vec2 uv0,uv1;
        varying vec4 shade;
        varying float fogAmount;
        void main(){
            float fraction=0.0;
            if(lodEnabled!=0){
                vec2 texels=uv0*lodScale.xy;
                vec2 dx=abs(dFdx(texels))*lodScale.z,dy=abs(dFdy(texels))*lodScale.w;
                fraction=clamp(max(max(dx.x,dx.y),max(dy.x,dy.y))-1.0,0.0,1.0);
            }
            vec3 color=mix(texture2D(texture0,uv0).rgb,texture2D(texture1,uv1).rgb,fraction);
            if(fogEnabled!=0)color=mix(color,gl_Fog.color.rgb,fogAmount);
            gl_FragColor=vec4(color,shade.a);
        }
        """;

    @Override public void init(){super.init();paired=false;program=0;}
    static boolean pairedMode(boolean twoCycles,long mux){
        if(!twoCycles)return false;
        int[] v=expandMux(mux);
        // (texel1-texel0)*LOD_FRACTION+texel0, then combined RGB / shade alpha.
        // Match the equation, not a ROM name or display-list address.
        return v[0]==TEXEL1&&v[1]==TEXEL0&&v[2]==LOD_FRACTION&&v[3]==TEXEL0
            &&v[8]==ZERO&&v[9]==ZERO&&v[10]==ZERO&&v[11]==COMBINED
            &&v[12]==ZERO&&v[13]==ZERO&&v[14]==ZERO&&v[15]==SHADE_ALPHA;
    }
    @Override public void setCombine(GL gl,boolean twoCycles,long mux){
        if(pairedMode(twoCycles,mux)&&gl.isFunctionAvailable("glCreateShader")){
            if(program==0)createProgram(gl);
            paired=true;usesT0=usesT1=true;usesNoise=false;
            vertex.color=SHADE;vertex.alpha=SHADE_ALPHA;vertex.secondaryColor=ZERO;
            gl.glUseProgram(program);
        }else{
            if(paired)gl.glUseProgram(0);
            paired=false;super.setCombine(gl,twoCycles,mux);
        }
    }
    @Override public void updateCombineColors(GL gl){if(!paired)super.updateCombineColors(gl);}
    @Override public void beginTextureUpdate(GL gl){if(!paired)super.beginTextureUpdate(gl);}
    @Override public void endTextureUpdate(GL gl){if(!paired)super.endTextureUpdate(gl);}
    @Override public void beforeDraw(GL gl,boolean geometry){
        if(!paired)return;
        gl.glUseProgram(program);
        var cache=OpenGlGdp.cache;
        boolean enabled=geometry&&cache.textureLevel()==1&&(Rsp.gdp.otherMode.w0&Gdp.TEX_LOD)!=0
            &&(Rsp.gdp.otherMode.w0&Gdp.TEX_DETAIL)==0&&cache.current[0]!=null&&cache.current[1]!=null;
        gl.glUniform1i(enabledLocation,enabled?1:0);
        gl.glUniform1i(fogLocation,geometry&&gln64j.config.GLN64jPluginSettings.enableFog&&EXT_fog_coord
            &&(Rsp.gsp.geometryMode&Gbi.G_FOG)!=0?1:0);
        if(enabled){
            var texture=cache.current[0];float s=texture.scaleS*texture.shiftScaleS,t=texture.scaleT*texture.shiftScaleT;
            gl.glUniform4f(scaleLocation,s!=0?1/s:0,t!=0?1/t:0,
                OpenGlGdp.hDC.getWidth()/(float)OpenGlGdp.screenWidth,OpenGlGdp.hDC.getHeight()/(float)OpenGlGdp.screenHeight);
        }
    }
    private static int compile(GL gl,int type,String source){
        int shader=gl.glCreateShader(type);gl.glShaderSource(shader,1,new String[]{source},(int[])null,0);gl.glCompileShader(shader);
        int[] ok=new int[1];gl.glGetShaderiv(shader,GL.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0){gl.glDeleteShader(shader);throw new IllegalStateException("GLN64j paired LOD shader compilation failed");}
        return shader;
    }
    private void createProgram(GL gl){
        int vs=compile(gl,GL.GL_VERTEX_SHADER,VERTEX),fs=compile(gl,GL.GL_FRAGMENT_SHADER,FRAGMENT);
        program=gl.glCreateProgram();gl.glAttachShader(program,vs);gl.glAttachShader(program,fs);gl.glLinkProgram(program);
        gl.glDeleteShader(vs);gl.glDeleteShader(fs);
        int[] ok=new int[1];gl.glGetProgramiv(program,GL.GL_LINK_STATUS,ok,0);
        if(ok[0]==0){gl.glDeleteProgram(program);program=0;throw new IllegalStateException("GLN64j paired LOD shader linking failed");}
        gl.glUseProgram(program);
        gl.glUniform1i(gl.glGetUniformLocation(program,"texture0"),0);gl.glUniform1i(gl.glGetUniformLocation(program,"texture1"),1);
        scaleLocation=gl.glGetUniformLocation(program,"lodScale");enabledLocation=gl.glGetUniformLocation(program,"lodEnabled");fogLocation=gl.glGetUniformLocation(program,"fogEnabled");
    }
}
