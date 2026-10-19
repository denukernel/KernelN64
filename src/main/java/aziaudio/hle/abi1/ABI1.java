/**
 * Copyright 2006 Jason LaDere
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
 * Originally based on Azimer's Audio Plugin.
 *
 */

package aziaudio.hle.abi1;
import aziaudio.hle.HLEMain;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;

/******** DMEM Memory Map for ABI 1 ***************
Address/Range		Description
-------------		-------------------------------
0x000..0x2BF		UCodeData
0x000-0x00F		Constants  - 0000 0001 0002 FFFF 0020 0800 7FFF 4000
0x010-0x02F		Function Jump Table (16 Functions * 2 bytes each = 32) 0x20
0x030-0x03F		Constants  - F000 0F00 00F0 000F 0001 0010 0100 1000
0x040-0x03F		Used by the Envelope Mixer (But what for?)
0x070-0x07F		Used by the Envelope Mixer (But what for?)
0x2C0..0x31F		<Unknown>
0x320..0x35F		Segments
0x360				Audio In Buffer (Location)
0x362				Audio Out Buffer (Location)
0x364				Audio Buffer Size (Location)
0x366				Initial Volume for Left Channel
0x368				Initial Volume for Right Channel
0x36A				Auxillary Buffer #1 (Location)
0x36C				Auxillary Buffer #2 (Location)
0x36E				Auxillary Buffer #3 (Location)
0x370				Loop Value (shared location)
0x370				Target Volume (Left)
0x372				Ramp?? (Left)
0x374				Rate?? (Left)
0x376				Target Volume (Right)
0x378				Ramp?? (Right)
0x37A				Rate?? (Right)
0x37C				Dry??
0x37E				Wet??
0x380..0x4BF		Alist data
0x4C0..0x4FF		ADPCM CodeBook
0x500..0x5BF		<Unknown>
0x5C0..0xF7F		Buffers...
0xF80..0xFFF		<Unknown>
 ***************************************************/

/** Audio state and opcode table; command implementations are in ABI1 helpers. */
public class ABI1 {

    /*
     * Audio flags
     */
    public static final int A_INIT = 0x01;
    public static final int A_CONTINUE = 0x00;
    public static final int A_LOOP = 0x02;
    public static final int A_OUT = 0x02;
    public static final int A_LEFT = 0x02;
    public static final int A_RIGHT = 0x00;
    public static final int A_VOL = 0x04;
    public static final int A_RATE = 0x00;
    public static final int A_AUX = 0x08;
    public static final int A_NOAUX = 0x00;
    public static final int A_MAIN = 0x00;
    public static final int A_MIX = 0x10;

    public static Runnable SPNOOP = new Runnable() {
        public void run() {

        }
    };

    // T8 = 0x360
    public static int AudioInBuffer; // 0x0000(T8)
    public static int AudioOutBuffer; // 0x0002(T8)
    public static int AudioCount; // 0x0004(T8)
    public static short Vol_Left; // 0x0006(T8)
    public static short Vol_Right; // 0x0008(T8)
    public static int AudioAuxA; // 0x000A(T8)
    public static int AudioAuxC; // 0x000C(T8)
    public static int AudioAuxE; // 0x000E(T8)
    public static int loopval; // 0x0010(T8) // Value set by A_SETLOOP : Possible conflict with SETVOLUME???
    public static short VolTrg_Left; // 0x0010(T8)
    public static int VolRamp_Left; // m_LeftVolTarget
    public static short VolTrg_Right; // m_RightVol
    public static int VolRamp_Right; // m_RightVolTarget
    public static short Env_Dry; // 0x001C(T8)
    public static short Env_Wet; // 0x001E(T8)

    public static ByteBuffer BufferSpace = ByteBuffer.allocate(0x10000); // 65536
    public static ShortBuffer BufferSpaceShort = BufferSpace.asShortBuffer();
    public static ByteBuffer hleMixerWorkArea = ByteBuffer.allocate(512);
    public static short[] adpcmtable = new short[0x88];
    
    // OPTIMIZATION: Pre-allocate scratch buffers for ADPCM to avoid 
    // creating 3 array objects per microcode execution (thousands per second).
    public static final int[] adpcm_a = new int[8];
    public static final int[] adpcm_inp1 = new int[8];
    public static final int[] adpcm_inp2 = new int[8];

    public static short[] ResampleLUT = {
        (short) 0x0C39, (short) 0x66AD, (short) 0x0D46, (short) 0xFFDF, (short) 0x0B39, (short) 0x6696, (short) 0x0E5F, (short) 0xFFD8,
        (short) 0x0A44, (short) 0x6669, (short) 0x0F83, (short) 0xFFD0, (short) 0x095A, (short) 0x6626, (short) 0x10B4, (short) 0xFFC8,
        (short) 0x087D, (short) 0x65CD, (short) 0x11F0, (short) 0xFFBF, (short) 0x07AB, (short) 0x655E, (short) 0x1338, (short) 0xFFB6,
        (short) 0x06E4, (short) 0x64D9, (short) 0x148C, (short) 0xFFAC, (short) 0x0628, (short) 0x643F, (short) 0x15EB, (short) 0xFFA1,
        (short) 0x0577, (short) 0x638F, (short) 0x1756, (short) 0xFF96, (short) 0x04D1, (short) 0x62CB, (short) 0x18CB, (short) 0xFF8A,
        (short) 0x0435, (short) 0x61F3, (short) 0x1A4C, (short) 0xFF7E, (short) 0x03A4, (short) 0x6106, (short) 0x1BD7, (short) 0xFF71,
        (short) 0x031C, (short) 0x6007, (short) 0x1D6C, (short) 0xFF64, (short) 0x029F, (short) 0x5EF5, (short) 0x1F0B, (short) 0xFF56,
        (short) 0x022A, (short) 0x5DD0, (short) 0x20B3, (short) 0xFF48, (short) 0x01BE, (short) 0x5C9A, (short) 0x2264, (short) 0xFF3A,
        (short) 0x015B, (short) 0x5B53, (short) 0x241E, (short) 0xFF2C, (short) 0x0101, (short) 0x59FC, (short) 0x25E0, (short) 0xFF1E,
        (short) 0x00AE, (short) 0x5896, (short) 0x27A9, (short) 0xFF10, (short) 0x0063, (short) 0x5720, (short) 0x297A, (short) 0xFF02,
        (short) 0x001F, (short) 0x559D, (short) 0x2B50, (short) 0xFEF4, (short) 0xFFE2, (short) 0x540D, (short) 0x2D2C, (short) 0xFEE8,
        (short) 0xFFAC, (short) 0x5270, (short) 0x2F0D, (short) 0xFEDB, (short) 0xFF7C, (short) 0x50C7, (short) 0x30F3, (short) 0xFED0,
        (short) 0xFF53, (short) 0x4F14, (short) 0x32DC, (short) 0xFEC6, (short) 0xFF2E, (short) 0x4D57, (short) 0x34C8, (short) 0xFEBD,
        (short) 0xFF0F, (short) 0x4B91, (short) 0x36B6, (short) 0xFEB6, (short) 0xFEF5, (short) 0x49C2, (short) 0x38A5, (short) 0xFEB0,
        (short) 0xFEDF, (short) 0x47ED, (short) 0x3A95, (short) 0xFEAC, (short) 0xFECE, (short) 0x4611, (short) 0x3C85, (short) 0xFEAB,
        (short) 0xFEC0, (short) 0x4430, (short) 0x3E74, (short) 0xFEAC, (short) 0xFEB6, (short) 0x424A, (short) 0x4060, (short) 0xFEAF,
        (short) 0xFEAF, (short) 0x4060, (short) 0x424A, (short) 0xFEB6, (short) 0xFEAC, (short) 0x3E74, (short) 0x4430, (short) 0xFEC0,
        (short) 0xFEAB, (short) 0x3C85, (short) 0x4611, (short) 0xFECE, (short) 0xFEAC, (short) 0x3A95, (short) 0x47ED, (short) 0xFEDF,
        (short) 0xFEB0, (short) 0x38A5, (short) 0x49C2, (short) 0xFEF5, (short) 0xFEB6, (short) 0x36B6, (short) 0x4B91, (short) 0xFF0F,
        (short) 0xFEBD, (short) 0x34C8, (short) 0x4D57, (short) 0xFF2E, (short) 0xFEC6, (short) 0x32DC, (short) 0x4F14, (short) 0xFF53,
        (short) 0xFED0, (short) 0x30F3, (short) 0x50C7, (short) 0xFF7C, (short) 0xFEDB, (short) 0x2F0D, (short) 0x5270, (short) 0xFFAC,
        (short) 0xFEE8, (short) 0x2D2C, (short) 0x540D, (short) 0xFFE2, (short) 0xFEF4, (short) 0x2B50, (short) 0x559D, (short) 0x001F,
        (short) 0xFF02, (short) 0x297A, (short) 0x5720, (short) 0x0063, (short) 0xFF10, (short) 0x27A9, (short) 0x5896, (short) 0x00AE,
        (short) 0xFF1E, (short) 0x25E0, (short) 0x59FC, (short) 0x0101, (short) 0xFF2C, (short) 0x241E, (short) 0x5B53, (short) 0x015B,
        (short) 0xFF3A, (short) 0x2264, (short) 0x5C9A, (short) 0x01BE, (short) 0xFF48, (short) 0x20B3, (short) 0x5DD0, (short) 0x022A,
        (short) 0xFF56, (short) 0x1F0B, (short) 0x5EF5, (short) 0x029F, (short) 0xFF64, (short) 0x1D6C, (short) 0x6007, (short) 0x031C,
        (short) 0xFF71, (short) 0x1BD7, (short) 0x6106, (short) 0x03A4, (short) 0xFF7E, (short) 0x1A4C, (short) 0x61F3, (short) 0x0435,
        (short) 0xFF8A, (short) 0x18CB, (short) 0x62CB, (short) 0x04D1, (short) 0xFF96, (short) 0x1756, (short) 0x638F, (short) 0x0577,
        (short) 0xFFA1, (short) 0x15EB, (short) 0x643F, (short) 0x0628, (short) 0xFFAC, (short) 0x148C, (short) 0x64D9, (short) 0x06E4,
        (short) 0xFFB6, (short) 0x1338, (short) 0x655E, (short) 0x07AB, (short) 0xFFBF, (short) 0x11F0, (short) 0x65CD, (short) 0x087D,
        (short) 0xFFC8, (short) 0x10B4, (short) 0x6626, (short) 0x095A, (short) 0xFFD0, (short) 0x0F83, (short) 0x6669, (short) 0x0A44,
        (short) 0xFFD8, (short) 0x0E5F, (short) 0x6696, (short) 0x0B39, (short) 0xFFDF, (short) 0x0D46, (short) 0x66AD, (short) 0x0C39,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
        0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
    };

    public static Runnable CLEARBUFF = ABI1Buffers::CLEARBUFF;

    public static Runnable ENVMIXER = ABI1Mixer::ENVMIXER;


    public static Runnable RESAMPLE = ABI1Resampler::RESAMPLE;

    public static Runnable SETVOL = ABI1Mixer::SETVOL;

    public static Runnable UNKNOWN = new Runnable() {
        public void run() {

        }
    };

    public static Runnable SETLOOP = ABI1Buffers::SETLOOP;

    public static Runnable ADPCM = ABI1Adpcm::ADPCM;

    public static Runnable LOADBUFF = ABI1Buffers::LOADBUFF;

    public static Runnable SAVEBUFF = ABI1Buffers::SAVEBUFF;

    public static Runnable SEGMENT = ABI1Buffers::SEGMENT;

    public static Runnable SETBUFF = ABI1Buffers::SETBUFF;

    public static Runnable DMEMMOVE = ABI1Buffers::DMEMMOVE;

    public static Runnable LOADADPCM = ABI1Adpcm::LOADADPCM;

    public static Runnable INTERLEAVE = ABI1Buffers::INTERLEAVE;

    public static Runnable MIXER = ABI1Mixer::MIXER;

    // TOP Performance Hogs:
    //Command: ADPCM    - Calls:  48 - Total Time: 331226 - Avg Time:  6900.54 - Percent: 31.53%
    //Command: ENVMIXER - Calls:  48 - Total Time: 408563 - Avg Time:  8511.73 - Percent: 38.90%
    //Command: LOADBUFF - Calls:  56 - Total Time:  21551 - Avg Time:   384.84 - Percent:  2.05%
    //Command: RESAMPLE - Calls:  48 - Total Time: 225922 - Avg Time:  4706.71 - Percent: 21.51%

    public static Runnable[] ABI1 = { // TOP Performace Hogs: MIXER, RESAMPLE, ENVMIXER
        SPNOOP, ADPCM, CLEARBUFF, ENVMIXER, LOADBUFF, RESAMPLE, SAVEBUFF, UNKNOWN,
        SETBUFF, SETVOL, DMEMMOVE, LOADADPCM, MIXER, INTERLEAVE, UNKNOWN, SETLOOP,
        SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP,
        SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP
    };

}