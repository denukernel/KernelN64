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
import kerneln64.memory.Dma;
import kerneln64.Main;


import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;


public class Sram {
    
    private RandomAccessFile hSramFile;
    private File file;
    
    /** Creates a new instance of Sram */
    public Sram(File file) {
        this.file = file;
    }
    
    /**
     * 
     * @param name 
     */
    public Sram(String name) {
        this(new File(name));
    }
    
    // called by Main
    public void close() {
        if (hSramFile != null) {
            try {
                hSramFile.close();
                hSramFile = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // called by Dma
    public void dmaFromSram(ByteBuffer dest, int startOffset, int len) {
        if (hSramFile == null) {
            if (!loadSram())
                return;
        }
        try {
            hSramFile.seek(startOffset);
            hSramFile.read(dest.array(), dest.arrayOffset(), len);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // called by Dma
    public void dmaToSram(ByteBuffer source, int startOffset, int len) {
        if (hSramFile == null) {
            if (!loadSram()) {
                return;
            }
        }
        try {
            hSramFile.seek(startOffset);
            hSramFile.write(source.array(), source.arrayOffset(), len);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean loadSram() {
        try {
            hSramFile = new RandomAccessFile(file, "rwd");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
}
