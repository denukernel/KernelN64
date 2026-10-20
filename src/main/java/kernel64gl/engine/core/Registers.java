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


package kernel64gl.engine.core;


public class Registers {
    
    // used by Gdp
    public static final int MI_INTR_DP = 0x20; // Bit 5: DP intr
    
    public int MI_INTR;
    public int[] MI_Registers;

    public int DPC_START;
    public int DPC_END;
    public int DPC_CURRENT;
    public int DPC_STATUS;
    public int DPC_CLOCK;
    public int DPC_BUFBUSY;
    public int DPC_PIPEBUSY;
    public int DPC_TMEM;
    public int[] DPC_Registers;

    public int VI_STATUS;
    public int VI_ORIGIN;
    public int VI_WIDTH;
    public int VI_INTR;
    public int VI_V_CURRENT_LINE;
    public int VI_TIMING;
    public int VI_V_SYNC;
    public int VI_H_SYNC;
    public int VI_LEAP;
    public int VI_H_START;
    public int VI_V_START;
    public int VI_V_BURST;
    public int VI_X_SCALE;
    public int VI_Y_SCALE;
    public int[] VI_Registers;

    /** Creates a new instance of N64 */
    public Registers() {
    }
    
}
