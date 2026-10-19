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


package kerneln64.savechips;
import kerneln64.hardware.Pif;
import kerneln64.Main;


import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class Eeprom {
    
    public static final int EEPROM_4K = 1;
    public static final int EEPROM_16K = 2;
    
    private RandomAccessFile hEepromFile;
    private File file;
    private byte[] eeprom = new byte[0x800];
    
    /** Creates a new instance of Eeprom */
    public Eeprom(File file) {
        this.file = file;
    }
    
    public Eeprom(String name) {
        this(new File(name));
    }

    public void prepareState() {
        if (hEepromFile == null) loadEeprom();
    }
    
    // called by Main
    public void close() {
        if (hEepromFile != null) {
            try {
                hEepromFile.close();
                hEepromFile = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // called by Pif
    public void eepromCommand(ByteBuffer command, int saveType, boolean showPifRamErrors) {
        switch (command.get(2)) {
            case 0: // check
                if (saveType != EEPROM_4K && saveType != EEPROM_16K) {
                    command.put(1, (byte)((command.get(1)&0xFF) | 0x80));
                    break;
                }
                if (command.get(1) != 3) {
                    command.put(1, (byte)((command.get(1)&0xFF) | 0x40));
                    if ((command.get(1) & 3) > 0) { command.put(3, (byte)0x00); }
                    if (saveType == EEPROM_4K) {
                        if ((command.get(1) & 3) > 1) { command.put(4, (byte)0x80); }
                    } else {
                        if ((command.get(1) & 3) > 1) { command.put(4, (byte)0xC0); }
                    }
                    if ((command.get(1) & 3) > 2) { command.put(5, (byte)0x00); }
                } else {
                    command.put(3, (byte)0x00);
                    command.put(4, (byte)((saveType == EEPROM_4K)?0x80:0xC0));
                    command.put(5, (byte)0x00);
                }
                break;
            case 4: // Read from Eeprom
                if (command.get(0) != 2)
                    System.err.printf("What am I meant to do with this Eeprom Command\n");
                if (command.get(1) != 8)
                    System.err.printf("What am I meant to do with this Eeprom Command\n");
                command.position(4);
                readFromEeprom(command.slice(), command.get(3));
                break;
            case 5:
                if (command.get(0) != 10)
                    System.err.printf("What am I meant to do with this Eeprom Command\n");
                if (command.get(1) != 1)
                    System.err.printf("What am I meant to do with this Eeprom Command\n");
                command.position(4);
                writeToEeprom(command.slice(), command.get(3));
                break;
            default:
                if (showPifRamErrors)
                    System.err.printf("Unkown EepromCommand %d\n", command.get(2));
        }
    }
    
    private void readFromEeprom(ByteBuffer buffer, int line) {
        if (hEepromFile == null)
            loadEeprom();

        for (int i=0; i<8; i++)
            buffer.put(i, eeprom[line*8+i]);
    }
    
    private void writeToEeprom(ByteBuffer buffer, int line) {
        if (hEepromFile == null)
            loadEeprom();

        for (int i=0; i<8; i++)
            eeprom[line*8+i] = buffer.get(i);
        
        try {
            hEepromFile.seek(line*8);
            hEepromFile.write(buffer.array(), buffer.arrayOffset(), 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadEeprom() {
        try {
            hEepromFile = new RandomAccessFile(file, "rwd");
            Arrays.fill(eeprom, (byte)0);
            hEepromFile.read(eeprom, 0, eeprom.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
