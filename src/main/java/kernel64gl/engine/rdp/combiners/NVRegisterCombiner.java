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


package kernel64gl.engine.rdp.combiners;

import javax.media.opengl.GL;


public class NVRegisterCombiner implements Combiners.CompiledCombiner {
    
    /**
     * Creates a new instance of NVRegisterCombiner
     */
    public NVRegisterCombiner(Combiners.Combiner c, Combiners.Combiner a) {
        System.err.println("Trying to compile: NV_register_combiners");
    }
    
    public static void init() {
        System.err.println("Trying to initialize: NV_register_combiners");
    }


    public void set(GL gl, Combiners combiner) {
        System.err.println("Trying to set combine state: NV_register_combiners");
    }
    
    public void updateColors(GL gl) {
        System.err.println("Trying to update combine colors: NV_register_combiners");
    }

}
