/**
 * Copyright 2005 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Project64 code.
 *
 */


package gln64j.rdp.combiners;

import javax.media.opengl.GL;


public class TextureEnv implements Combiners.CompiledCombiner {
    
    private class Fragment {
        int color;
        int alpha;
    };
    
    private int mode = GL.GL_DECAL;
    private Fragment fragment = new Fragment();
    private boolean usesT0, usesT1;
    
    // called by Combiners
    public TextureEnv(Combiners.Combiner c, Combiners.Combiner a) {
        usesT0 = false;
        usesT1 = false;
        
        fragment.color = fragment.alpha = Combiners.COMBINED;
        
        for (int i = 0; i < a.numStages; i++) {
            for (int j = 0; j < a.stage[i].numOps; j++) {
                switch (a.stage[i].op[j].op) {
                    case Combiners.LOAD:
                        if ((a.stage[i].op[j].param1 != Combiners.TEXEL0_ALPHA) && (a.stage[i].op[j].param1 != Combiners.TEXEL1_ALPHA)) {
                            fragment.alpha = a.stage[i].op[j].param1;
                            usesT0 = false;
                            usesT1 = false;
                        } else {
                            mode = GL.GL_REPLACE;
                            usesT0 = a.stage[i].op[j].param1 == Combiners.TEXEL0_ALPHA;
                            usesT1 = a.stage[i].op[j].param1 == Combiners.TEXEL1_ALPHA;
                        }
                        break;
                    case Combiners.SUB:
                        break;
                    case Combiners.MUL:
                        if (((a.stage[i].op[j].param1 == Combiners.TEXEL0_ALPHA) || (a.stage[i].op[j].param1 == Combiners.TEXEL1_ALPHA)) &&
                                ((a.stage[i].op[j - 1].param1 != Combiners.TEXEL0_ALPHA) || (a.stage[i].op[j - 1].param1 != Combiners.TEXEL1_ALPHA))) {
                            mode = GL.GL_MODULATE;
                        } else if (((a.stage[i].op[j].param1 != Combiners.TEXEL0_ALPHA) || (a.stage[i].op[j].param1 != Combiners.TEXEL1_ALPHA)) &&
                                ((a.stage[i].op[j - 1].param1 == Combiners.TEXEL0_ALPHA) || (a.stage[i].op[j - 1].param1 == Combiners.TEXEL1_ALPHA))) {
                            fragment.alpha = a.stage[i].op[j].param1;
                            mode = GL.GL_MODULATE;
                        }
                        break;
                    case Combiners.ADD:
                        break;
                    case Combiners.INTER:
                        break;
                }
            }
        }
        
        for (int i = 0; i < c.numStages; i++) {
            for (int j = 0; j < c.stage[i].numOps; j++) {
                switch (c.stage[i].op[j].op) {
                    case Combiners.LOAD:
                        if ((c.stage[i].op[j].param1 == Combiners.TEXEL0) || (c.stage[i].op[j].param1 == Combiners.TEXEL0_ALPHA)) {
                            if (mode == GL.GL_MODULATE)
                                fragment.color = Combiners.ONE;
                            
                            usesT0 = true;
                            usesT1 = false;
                        } else if ((c.stage[i].op[j].param1 == Combiners.TEXEL1) || (c.stage[i].op[j].param1 == Combiners.TEXEL1_ALPHA)) {
                            if (mode == GL.GL_MODULATE)
                                fragment.color = Combiners.ONE;
                            
                            usesT0 = false;
                            usesT1 = true;
                        } else {
                            fragment.color = c.stage[i].op[j].param1;
                            usesT0 = usesT1 = false;
                        }
                        break;
                    case Combiners.SUB:
                        break;
                    case Combiners.MUL:
                        if ((c.stage[i].op[j].param1 == Combiners.TEXEL0) || (c.stage[i].op[j].param1 == Combiners.TEXEL0_ALPHA)) {
                            if (!usesT0 && !usesT1) {
                                mode = GL.GL_MODULATE;
                                usesT0 = true;
                                usesT1 = false;
                            }
                        } else if ((c.stage[i].op[j].param1 == Combiners.TEXEL1) || (c.stage[i].op[j].param1 == Combiners.TEXEL1_ALPHA)) {
                            if (!usesT0 && !usesT1) {
                                mode = GL.GL_MODULATE;
                                usesT0 = false;
                                usesT1 = true;
                            }
                        } else if (usesT0 || usesT1) {
                            mode = GL.GL_MODULATE;
                            fragment.color = c.stage[i].op[j].param1;
                        }
                        break;
                    case Combiners.ADD:
                        break;
                    case Combiners.INTER:
                        if ((c.stage[i].op[j].param1 == Combiners.TEXEL0) &&
                                ((c.stage[i].op[j].param2 != Combiners.TEXEL0) && (c.stage[i].op[j].param2 != Combiners.TEXEL0_ALPHA) &&
                                (c.stage[i].op[j].param2 != Combiners.TEXEL1) && (c.stage[i].op[j].param2 != Combiners.TEXEL1_ALPHA)) &&
                                (c.stage[i].op[j].param3 == Combiners.TEXEL0_ALPHA)) {
                            mode = GL.GL_DECAL;
                            fragment.color = c.stage[i].op[j].param2;
                            usesT0 = true;
                            usesT1 = false;
                        } else if ((c.stage[i].op[j].param1 == Combiners.TEXEL0) &&
                                ((c.stage[i].op[j].param2 != Combiners.TEXEL0) && (c.stage[i].op[j].param2 != Combiners.TEXEL0_ALPHA) &&
                                (c.stage[i].op[j].param2 != Combiners.TEXEL1) && (c.stage[i].op[j].param2 != Combiners.TEXEL1_ALPHA)) &&
                                (c.stage[i].op[j].param3 == Combiners.TEXEL0_ALPHA)) {
                            mode = GL.GL_DECAL;
                            fragment.color = c.stage[i].op[j].param2;
                            usesT0 = false;
                            usesT1 = true;
                        }
                        break;
                }
            }
        }
    }
    
    // called by Combiners
    public static void init() {
    }


    // called by Combiners
    public void set(GL gl, Combiners combiner) {
//        GL gl = OpenGlGdp.gl;
        combiner.usesT0 = usesT0;
        combiner.usesT1 = usesT1;
        combiner.usesNoise = false;
        combiner.vertex.color = fragment.color;
        combiner.vertex.secondaryColor = Combiners.COMBINED;
        combiner.vertex.alpha = fragment.alpha;
        
        // Shouldn't ever happen, but who knows?
        if (Combiners.ARB_multitexture)
            gl.glActiveTexture(GL.GL_TEXTURE0);
        
        if (usesT0 || usesT1) {
            gl.glEnable(GL.GL_TEXTURE_2D);
        } else {
            gl.glDisable(GL.GL_TEXTURE_2D);
        }
        
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, mode);
    }
    
    // called by Combiners
    public void updateColors(GL gl) {
    }
    
}
