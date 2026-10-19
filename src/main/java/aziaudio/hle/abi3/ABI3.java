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


package aziaudio.hle.abi3;
import aziaudio.hle.HLEMain;
import aziaudio.hle.abi1.ABI1;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;

/** Audio state and opcode table; command implementations are in ABI3 helpers. */
public class ABI3 extends ABI1 {

    private static int setaddr;

    public static Runnable SETVOL3 = ABI3Mixer::SETVOL3;

    public static Runnable ENVMIXER3 = ABI3Mixer::ENVMIXER3;

    public static Runnable CLEARBUFF3 = ABI3Buffers::CLEARBUFF3;

    public static Runnable MIXER3 = ABI3Mixer::MIXER3;

    public static Runnable LOADBUFF3 = ABI3Buffers::LOADBUFF3;
    public static Runnable SAVEBUFF3 = ABI3Buffers::SAVEBUFF3;

    public static Runnable LOADADPCM3 = ABI3Adpcm::LOADADPCM3;

    public static Runnable DMEMMOVE3 = ABI3Buffers::DMEMMOVE3;

    public static Runnable SETLOOP3 = ABI3Buffers::SETLOOP3;

    public static Runnable ADPCM3 = ABI3Adpcm::ADPCM3;

    public static Runnable RESAMPLE3 = ABI3Resampler::RESAMPLE3;

    public static Runnable INTERLEAVE3 = ABI3Buffers::INTERLEAVE3;

    public static Runnable WHATISTHIS = new Runnable() {
        public void run() {

        }
    };

    public static Runnable MP3ADDY = new Runnable() {
        public void run() {
            setaddr = inst2 & 0xFFFFFF;
        }
    };

    public static Runnable MP3 = new Runnable() {
        public void run() {

        }
    };

    public static Runnable DISABLE = new Runnable() {
        public void run() {
            //MessageBox (NULL, "Help", "ABI 3 Command 0", MB_OK);
            //ChangeABI (5);
        }
    };

    public static Runnable[] ABI3 = {
        DISABLE, ADPCM3, CLEARBUFF3, ENVMIXER3, LOADBUFF3, RESAMPLE3, SAVEBUFF3, MP3,
        MP3ADDY, SETVOL3, DMEMMOVE3, LOADADPCM3, MIXER3, INTERLEAVE3, WHATISTHIS, SETLOOP3,
        SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP,
        SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP, SPNOOP
    };
    
}
