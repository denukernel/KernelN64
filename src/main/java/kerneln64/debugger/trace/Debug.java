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


package kerneln64.debugger.trace;
import kerneln64.Main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import javax.swing.JOptionPane;


public class Debug {
    
    public static final int NO_LOG = 0;
    public static final int SYNC_LOG = 1;
    public static final int FILE_LOG = 2;
    public static final int SCREEN_LOG = 3;
    public static final String OPS_SYNC_FILE = "C:/proj64output.txt";
    public static final String EXC_SYNC_FILE = "C:/proj64outputexcep.txt";
    public static final String MEM_SYNC_FILE = "C:/proj64outputmem.txt";
    public static final String COP_SYNC_FILE = "C:/proj64outputcop.txt";
    public static final String GEN_OUT_FILE = "C:/jn64output.txt";
    
    // ** DEBUG OPTIONS ********************************************************
    
    public static final int LOG_TYPE = SCREEN_LOG;
    public static final String SYNC_LOG_FILE = OPS_SYNC_FILE;
    public static final String FILE_LOG_FILE = GEN_OUT_FILE;
    
    public static final boolean DEBUG_OPCODES = false;
    public static final boolean DEBUG_I_OPCODES = false;
    public static final boolean DEBUG_R_OPCODES = false;
    public static final boolean DEBUG_J_OPCODES = false;
    public static final boolean DEBUG_ST_OPCODES = false; // Store
    public static final boolean DEBUG_LD_OPCODES = false; // Load
    public static final boolean DEBUG_CP_OPCODES = false; // Coprocessor
    public static final boolean DEBUG_BR_OPCODES = false; // Branch
    public static final boolean DEBUG_OP_OPCODES = false; // Operations
    
    // ** DEBUG OPTIONS ********************************************************
    
    private static long lineNum = 0;
    private static String logMsg = "";
    private static String prevOp = "none";
    private static String prevFileOp = "none";
    private static String prevOp2 = "none";
    private static String prevFileOp2 = "none";
    private static PrintWriter out;
    private static BufferedReader in;
    private static PrintStream systemOut;
    private static PrintStream systemErr;
    
    /** Creates a new instance of Debug */
    private Debug() {
    }
    
    public static void init() {
        if (LOG_TYPE == FILE_LOG) {
            try {
                out = new PrintWriter(new BufferedWriter(new FileWriter(FILE_LOG_FILE)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (LOG_TYPE == SYNC_LOG) {
            try {
                in = new BufferedReader(new FileReader(SYNC_LOG_FILE));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        systemErr = System.err;
        System.setErr(new PrintStream(System.err) {
            public PrintStream printf(String format, Object... args) {
                String msg = String.format(format, args);
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
                return this;
            }
        });
        
        systemOut = System.out;
        System.setOut(new PrintStream(System.out) {
            public PrintStream printf(String format, Object... args) {
                Log(format, args);
                return this;
            }
        });
        
    }
    
    public static void close() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static void Log(String Message, Object... args) {
        if (LOG_TYPE == NO_LOG)
            return;
        logMsg += String.format(Message, args);
        if (logMsg.endsWith("\n")) {
            if (LOG_TYPE == SYNC_LOG) {
                if (in != null) {
                    try {
                        String msg = logMsg.trim();
                        String line = in.readLine();
                        lineNum++;
                        if (!msg.equals(line)) {
                            systemOut.println("Output mismatch: line=" + lineNum);
                            systemOut.println(prevOp2);
                            systemOut.println(prevFileOp2);
                            systemOut.println(prevOp);
                            systemOut.println(prevFileOp);
                            systemOut.println(msg);
                            systemOut.println(line);
                            systemErr.printf("Output mismatch:\n%s\n%s\n", msg, line);
                        }
                        prevOp2 = prevOp;
                        prevFileOp2 = prevFileOp;
                        prevOp = msg;
                        prevFileOp = line;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else if (LOG_TYPE == FILE_LOG) {
                if (out != null)
                    out.printf(logMsg, args);
            } else if (LOG_TYPE == SCREEN_LOG) {
                systemOut.printf(logMsg, args);
            }
            logMsg = "";
        }
    }
    
}
