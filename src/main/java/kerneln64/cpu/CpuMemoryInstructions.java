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

package kerneln64.cpu;
import kerneln64.Main;


import kerneln64.memory.Memory.MemoryException;
import kerneln64.cpu.Cop0.TlbException;
import static kerneln64.Main.ShowTLBMisses;
import static kerneln64.cpu.Cpu.*;

/** Integer and floating-point loads/stores and atomic memory instructions. */
final class CpuMemoryInstructions {
    private CpuMemoryInstructions() {}

    private static final long[] LDL_MASK = {
        0x00000000000000L,
        0x000000000000FFL,
        0x0000000000FFFFL,
        0x00000000FFFFFFL,
        0x000000FFFFFFFFL,
        0x0000FFFFFFFFFFL,
        0x00FFFFFFFFFFFFL,
        0xFFFFFFFFFFFFFFL};

    private static final int[] LDL_SHIFT = {0, 8, 16, 24, 32, 40, 48, 56};

    private static final long[] LDR_MASK = {
        0xFFFFFFFFFFFFFF00L,
        0xFFFFFFFFFFFF0000L,
        0xFFFFFFFFFF000000L,
        0xFFFFFFFF00000000L,
        0xFFFFFF0000000000L,
        0xFFFF000000000000L,
        0xFF00000000000000L,
        0x0000000000000000L};

    private static final int[] LDR_SHIFT = {56, 48, 40, 32, 24, 16, 8, 0};

    private static final int[] LWL_MASK = {
        0x000000,
        0x0000FF,
        0x00FFFF,
        0xFFFFFF};

    private static final int[] LWL_SHIFT = {0, 8, 16, 24};

    private static final int[] LWR_MASK = {
        0xFFFFFF00,
        0xFFFF0000,
        0xFF000000,
        0x00000000};

    private static final int[] LWR_SHIFT = {24, 16 ,8, 0};

    private static final int[] SWL_MASK = {
        0x00000000,
        0xFF000000,
        0xFFFF0000,
        0xFFFFFF00};

    private static final int[] SWL_SHIFT = {0, 8, 16, 24};

    private static final long[] SDL_MASK = {
        0x0000000000000000L,
        0xFF00000000000000L,
        0xFFFF000000000000L,
        0xFFFFFF0000000000L,
        0xFFFFFFFF00000000L,
        0xFFFFFFFFFF000000L,
        0xFFFFFFFFFFFF0000L,
        0xFFFFFFFFFFFFFF00L};

    private static final int[] SDL_SHIFT = {0, 8, 16, 24, 32, 40, 48, 56};

    private static final long[] SDR_MASK = {
        0x00FFFFFFFFFFFFFFL,
        0x0000FFFFFFFFFFFFL,
        0x000000FFFFFFFFFFL,
        0x00000000FFFFFFFFL,
        0x0000000000FFFFFFL,
        0x000000000000FFFFL,
        0x00000000000000FFL,
        0x0000000000000000L};

    private static final int[] SDR_SHIFT = {56, 48, 40, 32, 24, 16, 8, 0};

    private static final int[] SWR_MASK = {
        0x00FFFFFF,
        0x0000FFFF,
        0x000000FF,
        0x00000000};

    private static final int[] SWR_SHIFT = {24, 16 , 8, 0};


    static void r4300i_LDL(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpDouble = cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7));
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("LDL TLB: %X\n", cpu.addr);
            return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
        cpu.GPR[(cpu.inst>>RT)&0x1F] = (cpu.GPR[(cpu.inst>>RT)&0x1F] & LDL_MASK[cpu.addr&7]) + (cpu.tmpDouble<<LDL_SHIFT[cpu.addr&7]);
    }

    static void r4300i_LDR(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpDouble = cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7));
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("LDL TLB: %X\n", cpu.addr);
            return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
        cpu.GPR[(cpu.inst>>RT)&0x1F] = (cpu.GPR[(cpu.inst>>RT)&0x1F] & LDR_MASK[cpu.addr&7]) + (cpu.tmpDouble>>LDR_SHIFT[cpu.addr&7]);
    }

    static void r4300i_LB(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) { return; }
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadByte(cpu.cop0.translateVaddr(cpu.addr));
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LB TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LH(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 1) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadHalfWord(cpu.cop0.translateVaddr(cpu.addr));
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LH TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LWL(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpWord = cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr & ~3));
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("LWL TLB: %X\n", cpu.addr);
            return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
        cpu.GPR[(cpu.inst>>RT)&0x1F] = ((int)cpu.GPR[(cpu.inst>>RT)&0x1F] & LWL_MASK[cpu.addr&3]) + (cpu.tmpWord<<LWL_SHIFT[cpu.addr&3]);
    }

    static void r4300i_LW(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) { return; }
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr));
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LW TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LBU(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadByte(cpu.cop0.translateVaddr(cpu.addr))&0xFFL;
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LBU TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LHU(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 1) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadHalfWord(cpu.cop0.translateVaddr(cpu.addr))&0xFFFFL;
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LHU TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LWR(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpWord = cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr & ~3));
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("LWR TLB: %X\n", cpu.addr);
            return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
        cpu.GPR[(cpu.inst>>RT)&0x1F] = ((int)cpu.GPR[(cpu.inst>>RT)&0x1F] & LWR_MASK[cpu.addr&3]) + (cpu.tmpWord>>LWR_SHIFT[cpu.addr&3]);
    }

    static void r4300i_LWU(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) { return; }
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr))&0xFFFFFFFFL;
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LWU TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SB(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000)
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                cpu.mem.storeByte(pAddr, (byte)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
            else {
                cpu.mem.storeByte(cpu.cop0.translateVaddr(cpu.addr), (byte)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("SB TLB: %X\n", cpu.addr);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SH(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 1) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000)
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                cpu.mem.storeHalfWord(pAddr, (short)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
            else {
                cpu.mem.storeHalfWord(cpu.cop0.translateVaddr(cpu.addr), (short)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("SH TLB: %X\n", cpu.addr);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SWL(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpWord = (cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr & ~3))&SWL_MASK[cpu.addr&3]) + ((int)cpu.GPR[(cpu.inst>>RT)&0x1F]>>>SWL_SHIFT[cpu.addr&3]);
            try {
                if (cpu.cacheInstructions) {
                    int pAddr = cpu.cop0.translateVaddr(cpu.addr) & ~0x03;
                    if (pAddr < 0x400000)
                        cpu.cachedOpcodes[pAddr>>>2].cached = false;
                    else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                        cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                    cpu.mem.storeWord(pAddr, cpu.tmpWord);
                }
                else {
                    cpu.mem.storeWord(cpu.cop0.translateVaddr(cpu.addr) & ~0x03, cpu.tmpWord);
                }
            } catch (TlbException e) {
                e.printStackTrace();
                System.err.printf("SWL TLB: %X\n", cpu.addr);
            } catch (MemoryException e) {
                e.printStackTrace();
                return;
            }
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("SWL TLB: %X\n", cpu.addr);
            return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SW(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000)
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                cpu.mem.storeWord(pAddr, (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
            else {
                cpu.mem.storeWord(cpu.cop0.translateVaddr(cpu.addr), (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
        } catch (TlbException e) {
            e.printStackTrace();
            System.err.printf("SW TLB: %X\n", cpu.addr);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SDL(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpDouble = (cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7))&SDL_MASK[cpu.addr&7]) + (cpu.GPR[(cpu.inst>>RT)&0x1F]>>SDL_SHIFT[cpu.addr&7]);
            try {
                if (cpu.cacheInstructions) {
                    int pAddr = cpu.cop0.translateVaddr(cpu.addr & ~7);
                    if (pAddr < 0x400000) {
                        cpu.cachedOpcodes[pAddr>>>2].cached = false;
                        cpu.cachedOpcodes[(pAddr+4)>>>2].cached = false;
                    }
                    else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length) {
                        cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                        cpu.cachedOpcodes[((pAddr+4)-0x3C00000)>>>2].cached = false;
                    }
                    cpu.mem.storeDoubleWord(pAddr, cpu.tmpDouble);
                }
                else {
                    cpu.mem.storeDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7), cpu.tmpDouble);
                }
            } catch (TlbException e) {
                cpu.handleTlbException(cpu.addr, true);
            } catch (MemoryException e) {
                e.printStackTrace();
                return;
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SDR(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpDouble = (cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7))&SDR_MASK[cpu.addr&7]) + (cpu.GPR[(cpu.inst>>RT)&0x1F]<<SDR_SHIFT[cpu.addr&7]);
            try {
                if (cpu.cacheInstructions) {
                    int pAddr = cpu.cop0.translateVaddr(cpu.addr & ~7);
                    if (pAddr < 0x400000) {
                        cpu.cachedOpcodes[pAddr>>>2].cached = false;
                        cpu.cachedOpcodes[(pAddr+4)>>>2].cached = false;
                    }
                    else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length) {
                        cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                        cpu.cachedOpcodes[((pAddr+4)-0x3C00000)>>>2].cached = false;
                    }
                    cpu.mem.storeDoubleWord(pAddr, cpu.tmpDouble);
                }
                else {
                    cpu.mem.storeDoubleWord(cpu.cop0.translateVaddr(cpu.addr & ~7), cpu.tmpDouble);
                }
            } catch (TlbException e) {
                cpu.handleTlbException(cpu.addr, true);
            } catch (MemoryException e) {
                e.printStackTrace();
                return;
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SWR(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        try {
            cpu.tmpWord = (cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr & ~3))&SWR_MASK[cpu.addr&3]) + ((int)cpu.GPR[(cpu.inst>>RT)&0x1F]<<SWR_SHIFT[cpu.addr&3]);
            try {
                if (cpu.cacheInstructions) {
                    int pAddr = cpu.cop0.translateVaddr(cpu.addr) & ~0x03;
                    if (pAddr < 0x400000)
                        cpu.cachedOpcodes[pAddr>>>2].cached = false;
                    else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                        cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                    cpu.mem.storeWord(pAddr, cpu.tmpWord);
                }
                else {
                    cpu.mem.storeWord(cpu.cop0.translateVaddr(cpu.addr) & ~0x03, cpu.tmpWord);
                }
            } catch (TlbException e) {
                cpu.handleTlbException(cpu.addr, true);
            } catch (MemoryException e) {
                e.printStackTrace();
                return;
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_CACHE(Cpu cpu) {
    }

    static void r4300i_LL(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) { return; }
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr));
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LW TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
        cpu.llBit = 1;
        cpu.llAddr = cpu.addr;
        try {
            cpu.llAddr = cpu.cop0.translateVaddr(cpu.llAddr);
        } catch (TlbException e) {
            e.printStackTrace();
        }
    }

    static void r4300i_LWC1(Cpu cpu) {
        if (cpu.testCop1UsableException()) return;
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.cop1.lwC1((cpu.inst>>RT)&0x1F, cpu.mem.loadWord(cpu.cop0.translateVaddr(cpu.addr)));
        } catch (TlbException e) {
             if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LWC1 TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SC(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        if (cpu.llBit == 1) {
            try {
                if (cpu.cacheInstructions) {
                    int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                    if (pAddr < 0x400000)
                        cpu.cachedOpcodes[pAddr>>>2].cached = false;
                    else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                        cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                    cpu.mem.storeWord(pAddr, (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
                }
                else {
                    cpu.mem.storeWord(cpu.cop0.translateVaddr(cpu.addr), (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
                }
            } catch (TlbException e) {
                cpu.handleTlbException(cpu.addr, true);
            } catch (MemoryException e) {
                e.printStackTrace();
                return;
            }
        }
        cpu.GPR[(cpu.inst>>RT)&0x1F] = (cpu.llBit&0x00000000FFFFFFFFL) | (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFF00000000L);
    }

    static void r4300i_LD(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 7) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr));
        } catch (TlbException e) {
             if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LD TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_LDC1(Cpu cpu) {
        if (cpu.testCop1UsableException()) return;
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 7) != 0) { cpu.addressErrorException(cpu.addr, true); return; }
        try {
            cpu.cop1.ldC1((cpu.inst>>RT)&0x1F, cpu.mem.loadDoubleWord(cpu.cop0.translateVaddr(cpu.addr)));
        } catch (TlbException e) {
            if (ShowTLBMisses) {
                e.printStackTrace();
                System.err.printf("LDC1 TLB: %X\n", cpu.addr);
            }
            cpu.tlbReadException(cpu.addr); return;
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SWC1(Cpu cpu) {
        if (cpu.testCop1UsableException()) return;
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 3) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000)
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length)
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                cpu.mem.storeWord(pAddr, cpu.cop1.swC1((cpu.inst>>RT)&0x1F));
            }
            else {
                cpu.mem.storeWord(cpu.cop0.translateVaddr(cpu.addr), cpu.cop1.swC1((cpu.inst>>RT)&0x1F));
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SDC1(Cpu cpu) {
        if (cpu.testCop1UsableException()) return;
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 7) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000) {
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                    cpu.cachedOpcodes[(pAddr+4)>>>2].cached = false;
                } else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length) {
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                    cpu.cachedOpcodes[((pAddr+4)-0x3C00000)>>>2].cached = false;
                }
                cpu.mem.storeDoubleWord(pAddr, cpu.cop1.sdC1((cpu.inst>>RT)&0x1F));
            }
            else {
                cpu.mem.storeDoubleWord(cpu.cop0.translateVaddr(cpu.addr), cpu.cop1.sdC1((cpu.inst>>RT)&0x1F));
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }

    static void r4300i_SD(Cpu cpu) {
        cpu.addr = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if ((cpu.addr & 7) != 0) { cpu.addressErrorException(cpu.addr, false); return; }
        try {
            if (cpu.cacheInstructions) {
                int pAddr = cpu.cop0.translateVaddr(cpu.addr);
                if (pAddr < 0x400000) {
                    cpu.cachedOpcodes[pAddr>>>2].cached = false;
                    cpu.cachedOpcodes[(pAddr+4)>>>2].cached = false;
                } else if (((pAddr-0x3C00000)>>>2) < cpu.cachedOpcodes.length) {
                    cpu.cachedOpcodes[(pAddr-0x3C00000)>>>2].cached = false;
                    cpu.cachedOpcodes[((pAddr+4)-0x3C00000)>>>2].cached = false;
                }
                cpu.mem.storeDoubleWord(pAddr, cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
            else {
                cpu.mem.storeDoubleWord(cpu.cop0.translateVaddr(cpu.addr), cpu.GPR[(cpu.inst>>RT)&0x1F]);
            }
        } catch (TlbException e) {
            cpu.handleTlbException(cpu.addr, true);
        } catch (MemoryException e) {
            e.printStackTrace();
            return;
        }
    }
}
