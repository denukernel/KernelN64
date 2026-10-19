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
import kerneln64.hardware.Audio;
import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cpu;
import kerneln64.debugger.DebuggerCore;
import kerneln64.rom.GameSharkCartridge;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.hardware.Rsp;
import kerneln64.hardware.Video;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.HashMap;
import plugin.AudioPlugin;
import plugin.GfxPlugin;
import kerneln64.savechips.FlashRam;


public class Memory {

	private static final boolean DEBUG_MEMORY = false;

	// RegRI RDRAM Interface (RI) Registers
	static final int RI_MODE_REG = 0;
	static final int RI_CONFIG_REG = 1;
	static final int RI_CURRENT_LOAD_REG = 2;
	static final int RI_SELECT_REG = 3;
	static final int RI_COUNT_REG = 4;
	static final int RI_REFRESH_REG = 4;
	static final int RI_LATENCY_REG = 5;
	static final int RI_RERROR_REG = 6;
	static final int RI_WERROR_REG = 7;

	// regRDRAM
	static final int RDRAM_CONFIG_REG = 0;
	private static final int RDRAM_DEVICE_TYPE_REG = 0;
	static final int RDRAM_DEVICE_ID_REG = 1;
	static final int RDRAM_DELAY_REG = 2;
	static final int RDRAM_MODE_REG = 3;
	static final int RDRAM_REF_INTERVAL_REG = 4;
	static final int RDRAM_REF_ROW_REG = 5;
	static final int RDRAM_RAS_INTERVAL_REG = 6;
	static final int RDRAM_MIN_INTERVAL_REG = 7;
	static final int RDRAM_ADDR_SELECT_REG = 8;
	static final int RDRAM_DEVICE_MANUF_REG = 9;

	private static final int IMEM_START = 0x00001000; // From DMEM //0x04001000;

	// used by Cpu(CheckInterrupts)
	public int[] audioIntrReg = new int[1];

	// used by Main, [Dma, Audio, Video, Rsp]
	public ByteBuffer RDRAM;
	private byte[] rdram;

	// used by Main, [Dma, Audio, Video, Rsp]
	public ByteBuffer DMEM;
	private byte[] dmem;
	public ByteBuffer IMEM;

	Cop0 cop0; // for timer
	Rsp rsp;
	Video video;
	Audio audio;
	Pif pif;
	Dma dma;
	FlashRam flashRam;
	int[] regSP;
	int[] regVI;
	int[] regSI;
	int[] regAI;
	int[] regPI;
	int[] regMI;
	int[] regDPC;
	Registers regs;
	boolean writtenToRom = false;
	int wroteToRom;
	HashMap bMem = new HashMap();
	int[] regRDRAM = new int[10];
	int[] regRI = new int[8];
	Runnable checkInterrupts;
	public ByteBuffer rom;

	public long millisInAudio;

	public static class MemoryException extends Exception {
		public MemoryException(String message) {
			super(message);
		}
	}

	/** Creates a new instance of Memory */
	public Memory(int size) throws MemoryException {
        if (size < 0x400000 || size > 0x4000000 || (size & (size - 1)) != 0) throw new MemoryException("RDRAM must be 4, 8, 16, 32 or 64 MB");


		try {

			RDRAM = ByteBuffer.allocate(size);
			rdram = RDRAM.array();
			DMEM = ByteBuffer.allocate(0x2000); // 8,192b
			dmem = DMEM.array();
			DMEM.position(IMEM_START);
			IMEM = DMEM.slice();
		} catch (OutOfMemoryError ex) {
			throw new MemoryException("Not enough memory for RDRAM!");
		}
	}

	public void map(Runnable checkInterrupts, Rsp rsp, Video video, Audio audio, Pif pif, Dma dma, FlashRam flashRam,
			Registers regs, ByteBuffer rom) {
		this.checkInterrupts = checkInterrupts;
		this.rsp = rsp;
		this.video = video;
		this.audio = audio;
		this.pif = pif;
		this.dma = dma;
		this.flashRam = flashRam;
		this.regAI = audio.regAI;
		this.regDPC = regs.regDPC;
		this.regMI = regs.regMI;
		this.regPI = dma.regPI;
		this.regSI = pif.regSI;
		this.regSP = rsp.regSP;
		this.regVI = video.regVI;
		this.audioIntrReg = audio.audioIntrReg;
		this.regs = regs;
		this.rom = rom;
	}

	// called by Main
	public void setTimer(Cop0 cop0) {
		this.cop0 = cop0;
	}

    // Hardware registers retain priority inside the experimental 64 MB address range.
    public boolean isRam(int address, int length) {
        if(address < 0 || length < 0 || (long)address+length>RDRAM.capacity())return false;
        if((long)address+length<=0x03F00000L)return true;
        if(address<0x03F00028 && (long)address+length>0x03F00000L)return false;
        for(int register:RDRAM_EXTRA_REGISTERS)if(address<(long)register+4 && (long)address+length>register)return false;
        return true;
    }
    private static final int[] RDRAM_EXTRA_REGISTERS={0x03F04004,0x03F08004,0x03F80004,0x03F80008,0x03F8000C,0x03F80014};
    public DebuggerCore debugger;
    public byte debugByte(int address) {
        if(isRam(address,1)) { d3d11.framebuffer.D3DFramebuffers.read(address,1); return RDRAM.get(address); }
        if(address>=0x04000000 && address<0x04002000)return DMEM.get(address-0x04000000);
        if(rom!=null && address>=0x10000000 && (long)address<0x10000000L+rom.capacity())return rom.get(address-0x10000000);
        if(GameSharkCartridge.isFirmware(rom)) {
            Integer register=GameSharkCartridge.readRegister(rom,address&~3);
            if(register!=null)return (byte)(register>>>((3-(address&3))*8));
            int offset=GameSharkCartridge.romOffset(rom,address);
            if(offset>=0 && offset<rom.capacity())return rom.get(offset);
        }
        throw new IllegalArgumentException(String.format("Unmapped debugger address %08X (RAM, RSP memory or ROM only)",address));
    }
    private void access(int address, int length, boolean write) {
        if (debugger != null) debugger.memoryAccess(address, length, write);
    }
    public byte loadByte(int address) throws MemoryException {
        access(address,1,false); d3d11.framebuffer.D3DFramebuffers.read(address,1);
        if(isRam(address,1)) return RDRAM.get(address);
        if(address>=0 && address<0x03F00000) return 0;
        if(address>=0x04000000 && address<0x04002000) return DMEM.get(address-0x04000000);
        return lbNonMemory(address);
    }
    public short loadHalfWord(int address) throws MemoryException {
        access(address,2,false); d3d11.framebuffer.D3DFramebuffers.read(address,2);
        if(isRam(address,2)) return RDRAM.getShort(address);
        if(address>=0 && address<0x03F00000) return 0;
        if(address>=0x04000000 && address<=0x04001FFE) return DMEM.getShort(address-0x04000000);
        return lhNonMemory(address);
    }
    public int loadWord(int address) throws MemoryException {
        access(address,4,false); d3d11.framebuffer.D3DFramebuffers.read(address,4);
        if(isRam(address,4)) return RDRAM.getInt(address);
        if(address>=0x04000000 && address<=0x04001FFC) return DMEM.getInt(address-0x04000000);
        if(address>=0 && address<0x03F00000) throw new MemoryException("LW outside configured RDRAM: " + Integer.toHexString(address));
        return lwNonMemory(address);
    }
    public long loadDoubleWord(int address) throws MemoryException {
        return ((long)loadWord(address)<<32) | (loadWord(address+4)&0xFFFFFFFFL);
    }
    public void storeByte(int address,byte value) throws MemoryException {
        access(address,1,true); d3d11.framebuffer.D3DFramebuffers.write(address,1);
        if(isRam(address,1)) RDRAM.put(address,value);
        else if(address>=0 && address<0x03F00000) throw new MemoryException("SB outside configured RDRAM");
        else if(address>=0x04000000 && address<0x04002000) DMEM.put(address-0x04000000,value);
        else sbNonMemory(address,value);
    }
    public void storeHalfWord(int address,short value) throws MemoryException {
        access(address,2,true); d3d11.framebuffer.D3DFramebuffers.write(address,2);
        if(isRam(address,2)) RDRAM.putShort(address,value);
        else if(address>=0 && address<0x03F00000) throw new MemoryException("SH outside configured RDRAM");
        else if(address>=0x04000000 && address<=0x04001FFE) DMEM.putShort(address-0x04000000,value);
        else shNonMemory(address,value);
    }
    public void storeWord(int address,int value) throws MemoryException {
        access(address,4,true); d3d11.framebuffer.D3DFramebuffers.write(address,4);
        if(isRam(address,4)) RDRAM.putInt(address,value);
        else if(address>=0 && address<0x03F00000) throw new MemoryException("SW outside configured RDRAM");
        else if(address>=0x04000000 && address<=0x04001FFC) DMEM.putInt(address-0x04000000,value);
        else swNonMemory(address,value);
    }
    public void storeDoubleWord(int address,long value) throws MemoryException {
        storeWord(address,(int)(value>>>32)); storeWord(address+4,(int)value);
    }

// Private Methods /////////////////////////////////////////////////////////////

	private byte lbNonMemory(int pAddr) throws MemoryException {
		return MemoryMappedReads.lbNonMemory(this, pAddr);
	}

	private short lhNonMemory(int pAddr) throws MemoryException {
		return MemoryMappedReads.lhNonMemory(this, pAddr);
	}

	private int lwNonMemory(int pAddr) throws MemoryException {
		return MemoryMappedReads.lwNonMemory(this, pAddr);
	}

	private void sbNonMemory(int pAddr, byte value) throws MemoryException {
		MemoryMappedWrites.sbNonMemory(this, pAddr, value);
	}

	private void shNonMemory(int pAddr, short value) throws MemoryException {
		MemoryMappedWrites.shNonMemory(this, pAddr, value);
	}

	private void swNonMemory(int pAddr, int value) throws MemoryException {
		MemoryMappedWrites.swNonMemory(this, pAddr, value);
	}

}
