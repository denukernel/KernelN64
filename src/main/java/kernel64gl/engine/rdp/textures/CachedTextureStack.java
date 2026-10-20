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


package kernel64gl.engine.rdp.textures;

import javax.media.opengl.GL;


public class CachedTextureStack {
    
    private GL gl;
    
    public CachedTexture top;
    public CachedTexture bottom;
    private int numCached;
    
    public void init(GL gl) {
        this.gl = gl;
        top = null;
        bottom = null;
        numCached = 0;
    }
    
    public void removeBottom() {
        gl.glDeleteTextures(1, bottom.glName, 0);
        if (bottom == top)
            top = null;
        
        bottom = bottom.higher;
        
        if (bottom != null)
            bottom.lower = null;
        
        numCached--;
    }
    
    public void remove(CachedTexture texture) {
        if ((texture == bottom) && (texture == top)) {
            top = null;
            bottom = null;
        } else if (texture == bottom) {
            bottom = texture.higher;
            
            if (bottom != null)
                bottom.lower = null;
        } else if (texture == top) {
            top = texture.lower;
            
            if (top != null)
                top.higher = null;
        } else {
            texture.higher.lower = texture.lower;
            texture.lower.higher = texture.higher;
        }
        
        numCached--;
        gl.glDeleteTextures(1, texture.glName, 0);
    }
    
    public void addTop(CachedTexture newtop) {
        gl.glGenTextures(1, newtop.glName, 0);
        
        newtop.lower = top;
        newtop.higher = null;
        
        if (top != null)
            top.higher = newtop;
        
        if (bottom == null)
            bottom = newtop;
        
        top = newtop;
        
        numCached++;
    }
    
    public void moveToTop(CachedTexture newtop) {
        if (newtop == top)
            return;
        
        if (newtop == bottom) {
            bottom = newtop.higher;
            bottom.lower = null;
        } else {
            newtop.higher.lower = newtop.lower;
            newtop.lower.higher = newtop.higher;
        }
        
        newtop.higher = null;
        newtop.lower = top;
        top.higher = newtop;
        top = newtop;
    }


}
