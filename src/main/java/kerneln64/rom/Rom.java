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
import kerneln64.memory.Dma;
import kerneln64.memory.Memory;
import kerneln64.Main;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFileChooser;
import javax.swing.UIManager;


public class Rom {
    
    private static final boolean DEBUG_ROM = false;
    
    private static final int BUFFER = 2048;
    
    // used by Main [Memory, Dma]
    public ByteBuffer data;
    
    /** Creates a new instance of Rom */
    public Rom(File file) {
        if (file == null) {
            System.err.println("ROM Error: Provided file is null.");
            data = null;
            return;
        }
        data = loadDataFromRomFile(file);
        if (data == null) {
            System.err.printf("Not enough memory for rom\n");
            return;
        }
    }
    
    public Rom(String name) {
        this(new File(name));
    }

    private boolean isValidRomImage(ByteBuffer test) {
        if (test.getInt(0) == 0x40123780) { if (DEBUG_ROM) System.out.printf("ROM format: 0x40123780\n"); return true; }
        if (test.getInt(0) == 0x12408037) { if (DEBUG_ROM) System.out.printf("ROM format: 0x12408037\n"); return true; }
        if (test.getInt(0) == 0x80371240) { if (DEBUG_ROM) System.out.printf("ROM format: 0x80371240\n"); return true; }
        return false;
    }
    
    private ByteBuffer loadDataFromRomFile(File fileName) {
    	if (fileName == null) return null; // FIX: Prevent getName() crash

        ByteBuffer test = ByteBuffer.allocate(4);
        test.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer tmpData = null;
        int romSize = 0;
        
        if (fileName.getName().substring(fileName.getName().lastIndexOf('.')).equalsIgnoreCase(".zip")) {
            System.out.println("Opening compressed ROM.");
            try {
                boolean foundRom = false;
                FileInputStream fis = new FileInputStream(fileName);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    int counter;
                    byte[] data = new byte[BUFFER];
                    int size = (int)entry.getSize();
                    byte[] tmpbuff = new byte[size];
                    int pos = 0;
                    while ((counter = zis.read(data, 0, BUFFER)) != -1) {
                        System.arraycopy(data, 0, tmpbuff, pos, counter);
                        pos += counter;
                    }
                    
                    System.arraycopy(tmpbuff, 0, test.array(), 0, 4);
                    if (isValidRomImage(test)) {
                        romSize = tmpbuff.length;
                        tmpData = ByteBuffer.allocate(romSize);
                        tmpData.order(ByteOrder.LITTLE_ENDIAN);
                        System.arraycopy(tmpbuff, 0, tmpData.array(), 0, romSize);
                        foundRom = true;
                        break;
                    }
                }
                zis.close();
                if (!foundRom) {
                    System.err.println("No valid rom image found in zipfile: " + fileName);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Opening UNcompressed ROM.");
            try {
                RandomAccessFile hFile;
                hFile = new RandomAccessFile(fileName, "r");
                hFile.read(test.array(), 0, 4);
                if (!isValidRomImage(test)) {
                    hFile.close();
                    System.err.printf("Not a valid rom image: %X\n", test.getInt(0));
                    return null;
                }
                hFile.seek(0);
                romSize = (int)hFile.length();
                tmpData = ByteBuffer.allocate(romSize);
                tmpData.order(ByteOrder.LITTLE_ENDIAN);
                
                hFile.read(tmpData.array());
                
                hFile.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        
        byte[] data = tmpData.array();
        switch (tmpData.getInt(0)) {
            case 0x12408037:
                for (int count=0 ; count<romSize; count+=4) {
                    data[count] ^= data[count+2];
                    data[count + 2] ^= data[count];
                    data[count] ^= data[count+2];
                    data[count + 1] ^= data[count + 3];
                    data[count + 3] ^= data[count + 1];
                    data[count + 1] ^= data[count + 3];
                }
                break;
            case 0x40123780:
                for (int count=0 ; count<romSize; count+=4) {
                    data[count] ^= data[count+3];
                    data[count + 3] ^= data[count];
                    data[count] ^= data[count+3];
                    data[count + 1] ^= data[count + 2];
                    data[count + 2] ^= data[count + 1];
                    data[count + 1] ^= data[count + 2];
                }
                break;
            case 0x80371240: break;
        }
        
        //TMP
        for (int count=0 ; count<romSize; count+=4) {
            data[count] ^= data[count+3];
            data[count + 3] ^= data[count];
            data[count] ^= data[count+3];
            data[count + 1] ^= data[count + 2];
            data[count + 2] ^= data[count + 1];
            data[count + 1] ^= data[count + 2];
        }
        tmpData.order(ByteOrder.BIG_ENDIAN);
        
        return tmpData;
    }


//        } catch (Exception e) {


}
