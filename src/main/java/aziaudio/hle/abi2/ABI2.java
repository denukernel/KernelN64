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

package aziaudio.hle.abi2;
import aziaudio.hle.HLEMain;
import aziaudio.hle.abi1.ABI1;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;


/** Audio state and opcode table; command implementations are in ABI2 helpers. */
public class ABI2 extends ABI1 {

	static boolean isMKABI = false;
	static boolean isZeldaABI = false;
	static int t3, s5, s6;
	static short[] env = new short[8];

    public static Runnable LOADADPCM2 = ABI2Adpcm::LOADADPCM2;

    public static Runnable SETLOOP2 = ABI2Buffers::SETLOOP2;

    public static Runnable SETBUFF2 = ABI2Buffers::SETBUFF2;

    public static Runnable ADPCM2 = ABI2Adpcm::ADPCM2;

    public static Runnable CLEARBUFF2 = ABI2Buffers::CLEARBUFF2;

    public static Runnable LOADBUFF2 = ABI2Buffers::LOADBUFF2;

    public static Runnable SAVEBUFF2 = ABI2Buffers::SAVEBUFF2;

    public static Runnable MIXER2 = ABI2Mixer::MIXER2;

    public static Runnable RESAMPLE2 = ABI2Resampler::RESAMPLE2;

    public static Runnable DMEMMOVE2 = ABI2Buffers::DMEMMOVE2;

    public static Runnable ENVSETUP1 = ABI2Mixer::ENVSETUP1;

    public static Runnable ENVSETUP2 = ABI2Mixer::ENVSETUP2;

    public static Runnable ENVMIXER2 = ABI2Mixer::ENVMIXER2;

    public static Runnable DUPLICATE2 = ABI2Buffers::DUPLICATE2;

    public static Runnable INTERL2 = ABI2Buffers::INTERL2;

    public static Runnable INTERLEAVE2 = ABI2Buffers::INTERLEAVE2;

    public static Runnable ADDMIXER = ABI2Mixer::ADDMIXER;

    public static Runnable HILOGAIN = ABI2Mixer::HILOGAIN;


    public static Runnable SEGMENT2 = ABI2Buffers::SEGMENT2;

	public static Runnable[] ABI2 = { SPNOOP, ADPCM2, CLEARBUFF2, UNKNOWN, ADDMIXER, RESAMPLE2, UNKNOWN, SEGMENT2,
			SETBUFF2, DUPLICATE2, DMEMMOVE2, LOADADPCM2, MIXER2, INTERLEAVE2, HILOGAIN, SETLOOP2, SPNOOP, INTERL2,
			ENVSETUP1, ENVMIXER2, LOADBUFF2, SAVEBUFF2, ENVSETUP2, SPNOOP, HILOGAIN, SPNOOP, DUPLICATE2, UNKNOWN,
			SPNOOP, SPNOOP, SPNOOP, SPNOOP };

	/*
	 * NOTES:
	 * 
	 * FILTER/SEGMENT - Still needs to be finished up... add FILTER? UNKNOWWN #27 -
	 * Is this worth doing? Looks like a pain in the ass just for WaveRace64
	 */

}
