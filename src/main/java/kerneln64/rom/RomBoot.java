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

package kerneln64.rom;
import kerneln64.cpu.Cpu;
import kerneln64.memory.Memory;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.Main;


import static kerneln64.Main.*;

/** Initial N64 register and memory state for the selected ROM and CIC. */
public final class RomBoot {
    private RomBoot() {}

	public static void initialize(Pif pif, Rom rom, Memory mem, Cpu cpu)  {
		if(GameSharkCartridge.boot(rom,mem,cpu))return;
		boolean usePif = pif.loadPifRom(rom.data.get(0x3E));
		if (rom == null || rom.data == null) {
	        System.err.println("Initialize Error: ROM data is null.");
	        return; 
	    }
	    
	    // Now it is safe to read the header
	    int country = rom.data.get(0x3E);
	    int cicChip = pif.getCicChipID(rom.data);
	    
	    if (cicChip < 0) {
			System.err.printf("Unknown Cic Chip: %d", cicChip);
			cicChip = 2;
		}

		if (usePif) {
			System.out.println("Using PIF");
			cpu.pc = 0xBFC00000;
			pif.initalizeRegisters(cicChip);
		} else {
			if (rom.data != null) {
				rom.data.position(0x040);
				mem.DMEM.position(0x040);
				for (int i = 0; i < 0xFBC; i++)
					mem.DMEM.put(rom.data.get());
			}
			cpu.pc = 0xA4000040;


			cpu.GPR[0] = 0x00000000;
			cpu.GPR[6] = 0xA4001F0C;
			cpu.GPR[7] = 0xA4001F08;
			cpu.GPR[8] = 0x000000C0;
			cpu.GPR[9] = 0x00000000;
			cpu.GPR[10] = 0x00000040;
			cpu.GPR[11] = 0xA4000040;
			cpu.GPR[16] = 0x00000000;
			cpu.GPR[17] = 0x00000000;
			cpu.GPR[18] = 0x00000000;
			cpu.GPR[19] = 0x00000000;
			cpu.GPR[21] = 0x00000000;
			cpu.GPR[26] = 0x00000000;
			cpu.GPR[27] = 0x00000000;
			cpu.GPR[28] = 0x00000000;
			cpu.GPR[29] = 0xA4001FF0;
			cpu.GPR[30] = 0x00000000;

			switch (country) {
			case 0x44: // Germany
			case 0x46: // french
			case 0x49: // Italian
			case 0x50: // Europe
			case 0x53: // Spanish
			case 0x55: // Australia
			case 0x58: // ????
			case 0x59: // X (PAL)
				switch (cicChip) {
				case 2:

					cpu.GPR[5] = 0xC0F1D859;
					cpu.GPR[14] = 0x2DE108EA;
					cpu.GPR[24] = 0x00000000;
					break;
				case 3:

					cpu.GPR[5] = 0xD4646273;
					cpu.GPR[14] = 0x1AF99984;
					cpu.GPR[24] = 0x00000000;
					break;
				case 5:
					mem.DMEM.putInt(0x1004, 0xBDA807FC);

					cpu.GPR[5] = 0xDECAAAD1;
					cpu.GPR[14] = 0x0CF85C13;
					cpu.GPR[24] = 0x00000002;
					break;
				case 6:

					cpu.GPR[5] = 0xB04DC903;
					cpu.GPR[14] = 0x1AF99984;
					cpu.GPR[24] = 0x00000002;
					break;
				}


				cpu.GPR[20] = 0x00000000;
				cpu.GPR[23] = 0x00000006;
				cpu.GPR[31] = 0xA4001554;
				break;
			case 0x37: // 7 (Beta)
			case 0x41: // ????
			case 0x45: // USA
			case 0x4A: // Japan
			default:
				switch (cicChip) {
				case 2:

					cpu.GPR[5] = 0xC95973D5;
					cpu.GPR[14] = 0x2449A366;
					break;
				case 3:

					cpu.GPR[5] = 0x95315A28;
					cpu.GPR[14] = 0x5BACA1DF;
					break;
				case 5:
					mem.DMEM.putInt(0x1004, 0x8DA807FC);

					cpu.GPR[5] = 0x5493FB9A;
					cpu.GPR[14] = 0xC2C20384;
				case 6:

					cpu.GPR[5] = 0xE067221F;
					cpu.GPR[14] = 0x5CD2B70F;
					break;
				}

				cpu.GPR[20] = 0x00000001;
				cpu.GPR[23] = 0x00000000;
				cpu.GPR[24] = 0x00000003;
				cpu.GPR[31] = 0xA4001550;
			}

			switch (cicChip) {
			case 1:

				cpu.GPR[22] = 0x0000003F;
				break;
			case 2:

				cpu.GPR[1] = 0x00000001;
				cpu.GPR[2] = 0x0EBDA536;
				cpu.GPR[3] = 0x0EBDA536;
				cpu.GPR[4] = 0x0000A536;
				cpu.GPR[12] = 0xED10D0B3;
				cpu.GPR[13] = 0x1402A4CC;
				cpu.GPR[15] = 0x3103E121;
				cpu.GPR[22] = 0x0000003F;
				cpu.GPR[25] = 0x9DEBB54F;
				break;
			case 3:

				cpu.GPR[1] = 0x00000001;
				cpu.GPR[2] = 0x49A5EE96;
				cpu.GPR[3] = 0x49A5EE96;
				cpu.GPR[4] = 0x0000EE96;
				cpu.GPR[12] = 0xCE9DFBF7;
				cpu.GPR[13] = 0xCE9DFBF7;
				cpu.GPR[15] = 0x18B63D28;
				cpu.GPR[22] = 0x00000078;
				cpu.GPR[25] = 0x825B21C9;
				break;
			case 5:
				mem.DMEM.putInt(0x1000, 0x3C0DBFC0);
				mem.DMEM.putInt(0x1008, 0x25AD07C0);
				mem.DMEM.putInt(0x100C, 0x31080080);
				mem.DMEM.putInt(0x1010, 0x5500FFFC);
				mem.DMEM.putInt(0x1014, 0x3C0DBFC0);
				mem.DMEM.putInt(0x1018, 0x8DA80024);
				mem.DMEM.putInt(0x101C, 0x3C0BB000);


				cpu.GPR[1] = 0x00000000;
				cpu.GPR[2] = 0xF58B0FBF;
				cpu.GPR[3] = 0xF58B0FBF;
				cpu.GPR[4] = 0x00000FBF;
				cpu.GPR[12] = 0x9651F81E;
				cpu.GPR[13] = 0x2D42AAC5;
				cpu.GPR[15] = 0x56584D60;
				cpu.GPR[22] = 0x00000091;
				cpu.GPR[25] = 0xCDCE565F;
				break;
			case 6:

				cpu.GPR[1] = 0x00000000;
				cpu.GPR[2] = 0xA95930A4;
				cpu.GPR[3] = 0xA95930A4;
				cpu.GPR[4] = 0x000030A4;
				cpu.GPR[12] = 0xBCB59510;
				cpu.GPR[13] = 0xBCB59510;
				cpu.GPR[15] = 0x7A3C07F4;
				cpu.GPR[22] = 0x00000085;
				cpu.GPR[25] = 0x465E3F72;
				break;
			}
		}
		if (DEBUG)
			System.out.printf("R4300i Registers Initialized\n");
	}
}
