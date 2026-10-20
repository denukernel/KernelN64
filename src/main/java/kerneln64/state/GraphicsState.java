package kerneln64.state;

import kerneln64.Main;

/** Select independently owned renderer objects for persistence and live switching. */
public final class GraphicsState {
    public static boolean kernel(){return Main.video!=null && Main.video.gfxPlugin instanceof kernel64gl.Kernel64GLPlugin;}
    public static Object gsp(){return kernel()?kernel64gl.engine.core.Rsp.gsp:gln64j.core.Rsp.gsp;}
    public static Object gdp(){return kernel()?kernel64gl.engine.core.Rsp.gdp:gln64j.core.Rsp.gdp;}
    public static Object cache(){return kernel()?kernel64gl.engine.opengl.OpenGlGdp.cache:gln64j.opengl.OpenGlGdp.cache;}
    public static void restored() {
        if(kernel()) {
            if(kernel64gl.engine.core.Rsp.gsp!=null)kernel64gl.engine.core.Rsp.gsp.changed=-1;
            if(kernel64gl.engine.core.Rsp.gdp!=null)kernel64gl.engine.core.Rsp.gdp.changed=-1;
            kernel64gl.engine.opengl.OpenGlGdp.cache.stateLoaded();
        } else {
            if(gln64j.core.Rsp.gsp!=null)gln64j.core.Rsp.gsp.changed=-1;
            if(gln64j.core.Rsp.gdp!=null)gln64j.core.Rsp.gdp.changed=-1;
            gln64j.opengl.OpenGlGdp.cache.stateLoaded();
        }
    }
    public record Appearance(long mux,float[] environment,float[] primitive,int minimum) {}
    public static Appearance appearance() {
        if(kernel()) {
            var combiner=kernel64gl.engine.opengl.OpenGlGdp.combiners;var p=kernel64gl.engine.rdp.combiners.Combiners.primColor;
            return new Appearance(combiner.combine.getMux(),kernel64gl.engine.rdp.combiners.Combiners.envColor.clone(),new float[]{p.r,p.g,p.b,p.a,p.l},p.m);
        }
        var combiner=gln64j.opengl.OpenGlGdp.combiners;var p=gln64j.rdp.combiners.Combiners.primColor;
        return new Appearance(combiner.combine.getMux(),gln64j.rdp.combiners.Combiners.envColor.clone(),new float[]{p.r,p.g,p.b,p.a,p.l},p.m);
    }
    public static void appearance(Appearance state) {
        if(kernel()) {
            kernel64gl.engine.opengl.OpenGlGdp.combiners.combine.setMux(state.mux());
            System.arraycopy(state.environment(),0,kernel64gl.engine.rdp.combiners.Combiners.envColor,0,4);
            var p=kernel64gl.engine.rdp.combiners.Combiners.primColor;float[] v=state.primitive();p.r=v[0];p.g=v[1];p.b=v[2];p.a=v[3];p.l=v[4];p.m=state.minimum();
        } else {
            gln64j.opengl.OpenGlGdp.combiners.combine.setMux(state.mux());
            System.arraycopy(state.environment(),0,gln64j.rdp.combiners.Combiners.envColor,0,4);
            var p=gln64j.rdp.combiners.Combiners.primColor;float[] v=state.primitive();p.r=v[0];p.g=v[1];p.b=v[2];p.a=v[3];p.l=v[4];p.m=state.minimum();
        }
    }
    private GraphicsState() {}
}
