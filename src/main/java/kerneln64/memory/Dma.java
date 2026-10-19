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


package kerneln64.memory;
import kerneln64.cpu.Cpu;
import kerneln64.rom.GameSharkCartridge;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.rom.Rom;
import kerneln64.hardware.Rsp;
import kerneln64.Main;


import java.nio.ByteBuffer;
import kerneln64.savechips.FlashRam;
import kerneln64.savechips.Sram;


public class Dma {

	// RegPI Peripheral Interface (PI) Registers (Sram, FlashRam, Rom)
	public static final int PI_DRAM_ADDR_REG = 0;
	public static final int PI_CART_ADDR_REG = 1;
	public static final int PI_RD_LEN_REG = 2;
	public static final int PI_WR_LEN_REG = 3;
	public static final int PI_STATUS_REG = 4;
	public static final int PI_BSD_DOM1_LAT_REG = 5;
	public static final int PI_DOMAIN1_REG = 5;
	public static final int PI_BSD_DOM1_PWD_REG = 6;
	public static final int PI_BSD_DOM1_PGS_REG = 7;
	public static final int PI_BSD_DOM1_RLS_REG = 8;
	public static final int PI_BSD_DOM2_LAT_REG = 9;
	public static final int PI_DOMAIN2_REG = 9;
	public static final int PI_BSD_DOM2_PWD_REG = 10;
	public static final int PI_BSD_DOM2_PGS_REG = 11;
	public static final int PI_BSD_DOM2_RLS_REG = 12;

	public static final int PI_STATUS_DMA_BUSY = 0x01;
	public static final int PI_STATUS_IO_BUSY = 0x02;
	public static final int PI_STATUS_ERROR = 0x04;

	public static final int PI_SET_RESET = 0x01;
	public static final int PI_CLR_INTR = 0x02;

	// used by Memory, Dma, Cpu(TimerDone)
	public int[] regPI = new int[13];

	private boolean dmaUsed;
	private Runnable checkInterrupts;
	private int[] regMI;
	private int[] regSI;
	private int[] regSP;
	private Registers regs;
	private ByteBuffer rDram;
	private ByteBuffer dMem;
	private ByteBuffer rom;
	private ByteBuffer pifRam;
	private Pif pif;
	private Sram sram;
	private FlashRam flashRam;
	private boolean showUnhandledMemory;

	public Runnable timerInterrupt = new Runnable() {
		public void run() {
			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
			checkInterrupts.run();
		}
	};

	/** Creates a new instance of Dma */
	public Dma(Runnable checkInterrupts, Registers regs, ByteBuffer rDram, ByteBuffer dMem) {
		this.checkInterrupts = checkInterrupts;
		this.regs = regs;
		this.regMI = regs.regMI;
		this.rDram = rDram;
		this.dMem = dMem;
	}

	public void showUnhandledMemory(boolean show) {
		showUnhandledMemory = show;
	}

	// called by Main
	public void setSaveChips(Sram sram, FlashRam flashRam) {
		this.sram = sram;
		this.flashRam = flashRam;
	}

	// called by Main
	public void connect(Pif pif, Rsp rsp, ByteBuffer rom) {
		this.pif = pif;
		this.pifRam = pif.pifRam;
		this.regSI = pif.regSI;
		this.regSP = rsp.regSP;
		this.rom = rom;
	}

	// called by Memory
	public void piDmaRead() {
		if (regPI[PI_DRAM_ADDR_REG] + regPI[PI_RD_LEN_REG] + 1 > rDram.capacity()) {
			System.err.printf("PI_DMA_READ not in Memory\n");
			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
			checkInterrupts.run();
			return;
		}

		if (regPI[PI_CART_ADDR_REG] >= 0x08000000 && regPI[PI_CART_ADDR_REG] <= 0x08010000) {
			if (regs.saveUsing == Registers.AUTO)
				regs.saveUsing = Registers.SRAM;

			if (regs.saveUsing == Registers.SRAM) {
				rDram.position(regPI[PI_DRAM_ADDR_REG]);
				sram.dmaToSram(rDram.slice(), regPI[PI_CART_ADDR_REG] - 0x08000000, regPI[PI_RD_LEN_REG] + 1);
				regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
				regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
				checkInterrupts.run();
				return;
			}
			if (regs.saveUsing == Registers.FLASHRAM) {
				rDram.position(regPI[PI_DRAM_ADDR_REG]);
				flashRam.dmaToFlashram(rDram.slice(), regPI[PI_CART_ADDR_REG] - 0x08000000,
//                        regPI[PI_WR_LEN_REG] + 1
						regPI[PI_RD_LEN_REG] + 1);
				regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
				regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
				checkInterrupts.run();
				return;
			}
		}
		if (regs.saveUsing == Registers.FLASHRAM) {
			System.err.printf("**** FLashRam DMA Read address %X *****\n", regPI[PI_CART_ADDR_REG]);
			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
			checkInterrupts.run();
			return;
		}
		System.err.printf("PI_DMA_READ where are you dmaing to ?\n");
		regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
		regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
		checkInterrupts.run();
		return;
	}

	// called by Memory
	public void piDmaWrite() {
		regPI[PI_STATUS_REG] |= PI_STATUS_DMA_BUSY;

		int dramAddr = regPI[PI_DRAM_ADDR_REG] & 0x1FFFFFFF;
		int cartAddr = regPI[PI_CART_ADDR_REG];
		int length = regPI[PI_WR_LEN_REG] + 1;

		// --- CORRUPTION-SAFE CLAMPING ---

		// clamp DRAM
		if (dramAddr < 0)
			dramAddr = 0;
		if (dramAddr >= rDram.capacity())
			dramAddr = rDram.capacity() - 1;

		// clamp CART ADDR
		if (cartAddr < 0)
			cartAddr = 0;
		cartAddr &= 0x1FFFFFFF;

		// special cases: SRAM & FLASH
		if (cartAddr >= 0x08000000 && cartAddr <= 0x08010000) {
			if (regs.saveUsing == Registers.AUTO)
				regs.saveUsing = Registers.SRAM;

			int offset = cartAddr - 0x08000000;

			rDram.position(dramAddr);
			ByteBuffer slice = rDram.slice();
			slice.limit(length);

			if (regs.saveUsing == Registers.SRAM) {
				sram.dmaFromSram(slice, offset, length);
			} else if (regs.saveUsing == Registers.FLASHRAM) {
				flashRam.dmaFromFlashram(slice, offset, length);
			}

			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
			checkInterrupts.run();
			return;
		}

		// ROM range 0x10000000 - 0x1FBFFFFF
		if (cartAddr >= 0x10000000 && cartAddr <= 0x1FBFFFFF) {

			int romAddr = GameSharkCartridge.romOffset(rom,cartAddr);

			// CORRUPTION-SAFE: clamp ROM address
			if (romAddr < 0)
				romAddr = 0;
			if (romAddr >= rom.capacity())
				romAddr = rom.capacity() - 1;

			// clamp length
			int safeLen = length;
			if (romAddr + safeLen > rom.capacity())
				safeLen = rom.capacity() - romAddr;

			if (dramAddr + safeLen > rDram.capacity())
				safeLen = rDram.capacity() - dramAddr;

			// ensure safe length
			if (safeLen < 0)
				safeLen = 0;

			// copy ROM â†’ RDRAM
			for (int i = 0; i < safeLen; i++) {
				byte val = rom.get(romAddr + i);
				rDram.put(dramAddr + i, val);
			}

			// fill remaining with zeros (like PJ64)
			for (int i = safeLen; i < length; i++) {
				if (dramAddr + i < rDram.capacity())
					rDram.put(dramAddr + i, (byte) 0);
			}

			if (!dmaUsed)
				firstDma();

			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
			checkInterrupts.run();
			return;
		}

		// If corrupted address is COMPLETELY invalid:
		// we silently drop DMA but DO NOT error or freeze.
		// This keeps corruption playable.
		regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
		regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_PI;
		checkInterrupts.run();
	}

	// called by Memory
	public void siDmaRead() {
		if ((int) regSI[Pif.SI_DRAM_ADDR_REG] > (int) rDram.capacity()) {
			System.err.printf("SI DMA READ\nSI_DRAM_ADDR_REG not in RDRam space\n");
			return;
		}

		pif.pifRamRead();
		regSI[Pif.SI_DRAM_ADDR_REG] &= 0xFFFFFFF8;
		if ((int) regSI[Pif.SI_DRAM_ADDR_REG] < 0) {
			int rdramPos = (int) regSI[Pif.SI_DRAM_ADDR_REG];
			for (int count = 0; count < 0x40; count++, rdramPos++) {
				if (rdramPos < 0) {
					continue;
				}
				rDram.put(rdramPos, pifRam.get(count));
			}
		} else {
			int rdramPos = (int) regSI[Pif.SI_DRAM_ADDR_REG];
			for (int i = 0; i < 64; i += 4, rdramPos += 4)
				rDram.putInt(rdramPos, pifRam.getInt(i));
		}

		regSI[Pif.SI_STATUS_REG] |= Pif.SI_STATUS_INTERRUPT;
		regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_SI;
		checkInterrupts.run();
	}

	// called by Memory
	public void siDmaWrite() {
		if ((int) regSI[Pif.SI_DRAM_ADDR_REG] > (int) rDram.capacity()) {
			System.err.printf("SI DMA WRITE\nSI_DRAM_ADDR_REG not in RDRam space\n");
			return;
		}

		regSI[Pif.SI_DRAM_ADDR_REG] &= 0xFFFFFFF8;
		if ((int) regSI[Pif.SI_DRAM_ADDR_REG] < 0) {
			int rdramPos = (int) regSI[Pif.SI_DRAM_ADDR_REG];
			for (int count = 0; count < 0x40; count++, rdramPos++) {
				if (rdramPos < 0) {
					pifRam.put(count, (byte) 0);
					continue;
				}
				pifRam.put(count, rDram.get(rdramPos));
			}
		} else {
			int rdramPos = (int) regSI[Pif.SI_DRAM_ADDR_REG];
			for (int i = 0; i < 64; i += 4, rdramPos += 4)
				pifRam.putInt(i, rDram.getInt(rdramPos));
		}

		pif.pifRamWrite();

		regSI[Pif.SI_STATUS_REG] |= Pif.SI_STATUS_INTERRUPT;
		regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_SI;
		checkInterrupts.run();
	}

	// called by Memory
	public void spDmaRead() {
		regSP[Rsp.SP_DRAM_ADDR_REG] &= 0x1FFFFFFF;

		if (regSP[Rsp.SP_DRAM_ADDR_REG] > rDram.capacity()) {
			System.err.printf("SP DMA READ\nSP_DRAM_ADDR_REG not in RDRam space\n");
			regSP[Rsp.SP_DMA_BUSY_REG] = 0;
			regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_DMA_BUSY;
			return;
		}

		if (regSP[Rsp.SP_RD_LEN_REG] + 1 + (regSP[Rsp.SP_MEM_ADDR_REG] & 0xFFF) > 0x1000) {
			System.err.printf("SP DMA READ\ncould not fit copy in memory segement\n");
			return;
		}

		System.arraycopy(rDram.array(), regSP[Rsp.SP_DRAM_ADDR_REG], dMem.array(), regSP[Rsp.SP_MEM_ADDR_REG] & 0x1FFF,
				regSP[Rsp.SP_RD_LEN_REG] + 1);

		regSP[Rsp.SP_DMA_BUSY_REG] = 0;
		regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_DMA_BUSY;
	}


	private void firstDma() {
        if(GameSharkCartridge.isFirmware(rom)){dmaUsed=true;return;}
		dmaUsed = true;
		switch (pif.getCicChipID(rom)) {
		case 1:
			rDram.putInt(0x318, rDram.capacity());
			break;
		case 2:
			rDram.putInt(0x318, rDram.capacity());
			break;
		case 3:
			rDram.putInt(0x318, rDram.capacity());
			break;
		case 5:
			rDram.putInt(0x3F0, rDram.capacity());
			break;
		case 6:
			rDram.putInt(0x318, rDram.capacity());
			break;
		default:
			System.err.printf("Unhandled CicChip(%d) in first DMA\n", pif.getCicChipID(rom));
		}
	}

}
