package kerneln64.cheats;
import kerneln64.Main;


/** US Goddard grabbable_joint_update_func: remove only the picked spring force.
 * Release damping, picking, cursor bounds and unpicked joints remain guest code.
 */
final class Sm64HeadStretch {
    static final String NAME="Funny Mario Options\\Unlimited head stretching (title screen)";
    static final String NOTES="SM64 US title screen: hold A on the nose, ear, cap or another grabbable feature, then move the stick to stretch it beyond its usual limit. Release A to let it spring back; R keeps its normal hold behavior. Disabling restores the three spring coefficients. Guarded against gameplay overlays and different code layouts.";
    static final int[] SITES={0x18E6FC,0x18E720,0x18E744};
    static final int[][] SIGNATURE={{0x18E660,0x27BDFF38},{0x18E664,0xAFBF0034},{0x18E700,0x44819800}};
    static final int[][] PAIRS=makePairs();
    static final String CODES=codeText();
    private static int[][] makePairs(){
        java.util.List<int[]> pairs=new java.util.ArrayList<>();
        for(int site:SITES){
            for(int[] signature:SIGNATURE){pairs.add(new int[]{0xD1000000|signature[0],signature[1]>>>16});pairs.add(new int[]{0xD1000000|signature[0]+2,signature[1]&65535});}
            pairs.add(new int[]{0x81000000|site+2,0});
        }
        return pairs.toArray(int[][]::new);
    }
    private static String codeText(){StringBuilder text=new StringBuilder();for(int[] pair:PAIRS)text.append(String.format("%08X %04X%n",pair[0],pair[1]));return text.toString().strip();}
    static boolean matches(java.nio.ByteBuffer ram,Sm64DecompCodes build) {
        if(build==null)return matches(ram);
        try {for(int[] signature:build.signature(SIGNATURE))if(ram.capacity()<signature[0]+4 || ram.getInt(signature[0])!=signature[1])return false;return true;}
        catch(IllegalArgumentException unsupported){return false;}
    }
    static boolean matches(java.nio.ByteBuffer ram){for(int[] signature:SIGNATURE)if(ram.capacity()<signature[0]+4 || ram.getInt(signature[0])!=signature[1])return false;return true;}
}
