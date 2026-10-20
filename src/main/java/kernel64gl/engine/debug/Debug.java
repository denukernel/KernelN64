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


package kernel64gl.engine.debug;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;


public class Debug {
    
    public static final boolean DEBUG_ALL = false;
    
    public static final boolean DEBUG_GBI = DEBUG_ALL | false;
    public static final boolean DEBUG_GDP = DEBUG_ALL | false;
    public static final boolean DEBUG_GSP = DEBUG_ALL | false;
    public static final boolean DEBUG_RSP = DEBUG_ALL | true;
    public static final boolean DEBUG_GLN64 = DEBUG_ALL | false;
    public static final boolean DEBUG_OGL = DEBUG_ALL | false;
    public static final boolean DEBUG_TEXTURES = DEBUG_ALL | false;
    public static final boolean DEBUG_MICROCODE = DEBUG_ALL | true;
    
    public static final boolean DEBUG_FILE = false;
    
    public static final boolean WIREFRAME = false;
    
    public static final int		DEBUG_LOW=		0x1000;
    public static final int		DEBUG_MEDIUM=           0x2000;
    public static final int		DEBUG_HIGH=		0x4000;
    public static final int		DEBUG_DETAIL=           0x8000;
    
    public static final int		DEBUG_HANDLED=          0x0001;
    public static final int		DEBUG_UNHANDLED=        0x0002;
    public static final int		DEBUG_IGNORED=          0x0004;
    public static final int		DEBUG_UNKNOWN=          0x0008;
    public static final int		DEBUG_ERROR=		0x0010;
    public static final int		DEBUG_COMBINE=          0x0020;
    public static final int		DEBUG_TEXTURE=          0x0040;
    public static final int		DEBUG_VERTEX=           0x0080;
    public static final int		DEBUG_TRIANGLE=         0x0100;
    public static final int		DEBUG_MATRIX=           0x0200;
    
    public static int level = DEBUG_HIGH;
    
    private static boolean DumpMessages;
    private static PrintWriter dumpFile;
    private static int taskMessages;

    public static void beginTask() { taskMessages = 0; }
    
    /** Creates a new instance of Debug */
    public Debug() {
    }
    
    public static void OpenDebugDlg() {
        DumpMessages = false;
        if (DEBUG_FILE)
            StartDump("C:\\gln64output.txt");
    }
    
    public static void CloseDebugDlg() {
        EndDump();
    }
    
    public static void StartDump(String filename) {
        System.out.println("StartDump");
        DumpMessages = true;
        try {
            dumpFile = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void EndDump() {
        DumpMessages = false;
        if (dumpFile != null)
            dumpFile.close();
    }
    
    public static void DebugMsg(int type, String format, Object... args) {
        if (taskMessages++ >= 64) {
            if (taskMessages == 65) System.out.println("Kernel64GL: additional graphics messages suppressed for this task");
            return;
        }
        if (DumpMessages && dumpFile != null) {
            dumpFile.printf(format, args);
        } else {
            System.out.printf(format, args);
        }
    }
    
}
