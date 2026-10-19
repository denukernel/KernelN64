package kerneln64.cheats;
import kerneln64.Main;


import java.nio.*;
import java.security.*;
import java.util.*;

/** Conservative straight-line MIPS address normalization for known SM64 functions. */
final class Sm64CodeFingerprint {
    static String hash(byte[] bytes) {
        ByteBuffer data=ByteBuffer.wrap(bytes.clone()).order(ByteOrder.BIG_ENDIAN);
        int[] lui=new int[32];Arrays.fill(lui,-1000);
        for(int i=0;i<bytes.length/4;i++) {
            int original=data.getInt(i*4),word=original,op=word>>>26,rs=word>>>21&31,rt=word>>>16&31,imm=word&65535;
            if(op==15 && imm>=0x8000 && imm<=0x803F){lui[rt]=i;word&=0xFFFF0000;}
            else {
                boolean low=switch(op){case 9,13,32,33,35,36,37,40,41,43,49,53,57,61->true;default->false;};
                if(low && i-lui[rs]<=20)word&=0xFFFF0000;
                else if(op==2 || op==3)word&=0xFC000000;
                int destination=-1,propagate=-1000;
                if(op==0) {
                    boolean writes=switch(original&63){case 0,2,3,4,6,7,9,16,18,32,33,34,35,36,37,38,39,42,43->true;default->false;};
                    if(writes)destination=original>>>11&31;
                    // LUI + indexed ADDU still needs its absolute low address.
                    if((original&63)==33) {
                        if(lui[rs]>=0 && lui[rt]<0)propagate=lui[rs];
                        else if(lui[rt]>=0 && lui[rs]<0)propagate=lui[rt];
                    }
                } else if(switch(op){case 8,9,10,11,12,13,14,15,32,33,34,35,36,37,38->true;default->false;})destination=rt;
                else if((op==16 || op==17 || op==18) && (rs==0 || rs==2))destination=rt;
                else if(op==3)destination=31;
                // Once ADDIU/ORI or a pointer load completes an address, later
                // offsets through that register are struct fields, not relocations.
                if(destination>=0)lui[destination]=propagate;
            }
            data.putInt(i*4,word);
        }
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.array()));}
        catch(NoSuchAlgorithmException error){throw new AssertionError(error);}
    }
}
