package d3d11.bridge;

import javax.media.opengl.GL;
import gln64j.rdp.combiners.Combiners;

/** Runs the original RDP (A-B)*C+D equations in the native pixel shader. */
public final class D3DCombiner extends Combiners {
    private final D3DGlBridge renderer;
    public D3DCombiner(D3DGlBridge renderer) { this.renderer=renderer; }
    @Override public void init() { vertex.color=SHADE;vertex.alpha=SHADE_ALPHA;vertex.secondaryColor=ZERO; }
    @Override public void setCombine(GL gl,boolean twoCycles,long mux) {
        int[] expanded=expandMux(mux);System.arraycopy(expanded,0,renderer.selectors,0,16);renderer.cycles=twoCycles?2:1;
        usesT0=usesT1=usesNoise=renderer.usesLod=false;
        for(int i=0;i<(twoCycles?16:8);i++) {
            int input=expanded[i];usesT0|=input==TEXEL0||input==TEXEL0_ALPHA;usesT1|=input==TEXEL1||input==TEXEL1_ALPHA;usesNoise|=input==NOISE;
            renderer.usesLod|=input==LOD_FRACTION;
        }
    }
    @Override public void updateCombineColors(GL gl) {}
    @Override public void beginTextureUpdate(GL gl) {}
    @Override public void endTextureUpdate(GL gl) {}
}
