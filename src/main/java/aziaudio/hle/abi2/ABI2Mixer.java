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

import java.nio.ShortBuffer;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi2.ABI2.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI2 Volume, gain, and envelope mixing commands. */
final class ABI2Mixer {
    private ABI2Mixer() {}

    static void MIXER2() {
        int dmemin = inst2 >>> 16;
        int dmemout = inst2 & 0xFFFF;
        int count = (inst1 >>> 12) & 0xFF0;
        int gain = (short) (inst1 & 0xFFFF) * 2;
        int temp; // signed

        for (int x = 0; x < count; x += 2) {
        	temp = (BufferSpace.getShort(dmemin + x) * gain) >> 16;
        	temp += BufferSpace.getShort(dmemout + x);

        	if (temp > 32767) {
        		temp = 32767;
        	}
        	if (temp < -32768) {
        		temp = -32768;
        	}

        	BufferSpace.putShort(dmemout + x, (short) temp);
        }
    }

    static void ENVSETUP1() {
        t3 = inst1 & 0xFFFF;
        int tmp = (inst1 >>> 0x8) & 0xFF00;
        env[4] = (short) tmp;
        tmp += t3;
        env[5] = (short) tmp;
        s5 = inst2 >>> 16;
        s6 = inst2 & 0xFFFF;
    }

    static void ENVSETUP2() {
        int tmp = inst2 >>> 16;
        env[0] = (short) tmp;
        tmp += s5;
        env[1] = (short) tmp;
        tmp = inst2 & 0xFFFF;
        env[2] = (short) tmp;
        tmp += s6;
        env[3] = (short) tmp;
    }

    static void ENVMIXER2() {
        ShortBuffer bufft6, bufft7, buffs0, buffs1, buffs3;
        int count;
        int adder;
        short vec9, vec10;
        short[] v2 = new short[8];
        buffs3 = BufferSpaceShort;
        int buffs3_p = ((inst1 >>> 0x0C) & 0x0FF0) / 2;
        bufft6 = BufferSpaceShort;
        int bufft6_p = ((inst2 >>> 0x14) & 0x0FF0) / 2;
        bufft7 = BufferSpaceShort;
        int bufft7_p = ((inst2 >>> 0x0C) & 0x0FF0) / 2;
        buffs0 = BufferSpaceShort;
        int buffs0_p = ((inst2 >>> 0x04) & 0x0FF0) / 2;
        buffs1 = BufferSpaceShort;
        int buffs1_p = ((inst2 << 0x04) & 0x0FF0) / 2;

        v2[0] = (short) (0 - (short) ((inst1 & 0x2) >>> 1));
        v2[1] = (short) (0 - (short) ((inst1 & 0x1)));
        v2[2] = (short) (0 - (short) ((inst1 & 0x8) >>> 1));
        v2[3] = (short) (0 - (short) ((inst1 & 0x4) >>> 1));

        count = (inst1 >>> 8) & 0xFF;

        if (!isMKABI) {
        	s5 *= 2;
        	s6 *= 2;
        	t3 *= 2;
        	adder = 0x10;
        } else {
        	inst1 = 0;
        	adder = 0x8;
        	t3 = 0;
        }

        while (count > 0) {
        	int temp;
        	for (int x = 0; x < 0x8; x++) {
        		vec9 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[0] & 0xFFFF)) >> 16) ^ v2[0]);
        		vec10 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[2] & 0xFFFF)) >> 16)
        				^ v2[1]);
        		temp = bufft6.get(bufft6_p + (x ^ 1)) + vec9;
        		if (temp > 32767) {
        			temp = 32767;
        		}
        		if (temp < -32768) {
        			temp = -32768;
        		}
        		bufft6.put(bufft6_p + (x ^ 1), (short) temp);
        		temp = bufft7.get(bufft7_p + (x ^ 1)) + vec10;
        		if (temp > 32767) {
        			temp = 32767;
        		}
        		if (temp < -32768) {
        			temp = -32768;
        		}
        		bufft7.put(bufft7_p + (x ^ 1), (short) temp);
        		vec9 = (short) ((short) (((int) vec9 * (env[4] & 0xFFFF)) >> 16) ^ v2[2]);
        		vec10 = (short) ((short) (((int) vec10 * (env[4] & 0xFFFF)) >> 16) ^ v2[3]);
        		if ((inst1 & 0x10) != 0) {
        			temp = buffs0.get(buffs0_p + (x ^ 1)) + vec10;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			buffs0.put(buffs0_p + (x ^ 1), (short) temp);
        			temp = buffs1.get(buffs1_p + (x ^ 1)) + vec9;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			buffs1.put(buffs1_p + (x ^ 1), (short) temp);
        		} else {
        			temp = buffs0.get(buffs0_p + (x ^ 1)) + vec9;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			buffs0.put(buffs0_p + (x ^ 1), (short) temp);
        			temp = buffs1.get(buffs1_p + (x ^ 1)) + vec10;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			buffs1.put(buffs1_p + (x ^ 1), (short) temp);
        		}
        	}

        	if (!isMKABI) {
        		for (int x = 0x8; x < 0x10; x++) {
        			vec9 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[1] & 0xFFFF)) >> 16)
        					^ v2[0]);
        			vec10 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[3] & 0xFFFF)) >> 16)
        					^ v2[1]);
        			temp = bufft6.get(bufft6_p + (x ^ 1)) + vec9;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			bufft6.put(bufft6_p + (x ^ 1), (short) temp);
        			temp = bufft7.get(bufft7_p + (x ^ 1)) + vec10;
        			if (temp > 32767) {
        				temp = 32767;
        			}
        			if (temp < -32768) {
        				temp = -32768;
        			}
        			bufft7.put(bufft7_p + (x ^ 1), (short) temp);
        			vec9 = (short) ((short) (((int) vec9 * (env[5] & 0xFFFF)) >> 16) ^ v2[2]);
        			vec10 = (short) ((short) (((int) vec10 * (env[5] & 0xFFFF)) >> 16) ^ v2[3]);
        			if ((inst1 & 0x10) != 0) {
        				temp = buffs0.get(buffs0_p + (x ^ 1)) + vec10;
        				if (temp > 32767) {
        					temp = 32767;
        				}
        				if (temp < -32768) {
        					temp = -32768;
        				}
        				buffs0.put(buffs0_p + (x ^ 1), (short) temp);
        				temp = buffs1.get(buffs1_p + (x ^ 1)) + vec9;
        				if (temp > 32767) {
        					temp = 32767;
        				}
        				if (temp < -32768) {
        					temp = -32768;
        				}
        				buffs1.put(buffs1_p + (x ^ 1), (short) temp);
        			} else {
        				temp = buffs0.get(buffs0_p + (x ^ 1)) + vec9;
        				if (temp > 32767) {
        					temp = 32767;
        				}
        				if (temp < -32768) {
        					temp = -32768;
        				}
        				buffs0.put(buffs0_p + (x ^ 1), (short) temp);
        				temp = buffs1.get(buffs1_p + (x ^ 1)) + vec10;
        				if (temp > 32767) {
        					temp = 32767;
        				}
        				if (temp < -32768) {
        					temp = -32768;
        				}
        				buffs1.put(buffs1_p + (x ^ 1), (short) temp);
        			}
        		}
        	}
        	bufft6_p += adder;
        	bufft7_p += adder;
        	buffs0_p += adder;
        	buffs1_p += adder;
        	buffs3_p += adder;
        	count -= adder;

        	env[0] += (s5 & 0xFFFF);
        	env[1] += (s5 & 0xFFFF);
        	env[2] += (s6 & 0xFFFF);
        	env[3] += (s6 & 0xFFFF);
        	env[4] += (t3 & 0xFFFF);
        	env[5] += (t3 & 0xFFFF);
        }
    }

    static void ADDMIXER() {
        int Count = (inst1 >>> 12) & 0x00FF0;
        int InBuffer = inst2 >>> 16;
        int OutBuffer = inst2 & 0xFFFF;
        ShortBuffer inp, outp;
        int inp_p, outp_p;
        int temp;

        inp = BufferSpaceShort;
        inp_p = InBuffer / 2;
        outp = BufferSpaceShort;
        outp_p = OutBuffer / 2;

        for (int cntr = 0; cntr < Count; cntr += 2) {
        	temp = outp.get(outp_p) + inp.get(inp_p);
        	if (temp > 32767) {
        		temp = 32767;
        	}
        	if (temp < -32768) {
        		temp = -32768;
        	}
        	outp_p++;
        	inp_p++;
        }
    }

    static void HILOGAIN() {
        

    }
}
