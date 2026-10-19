package kerneln64.debugger;
import kerneln64.debugger.trace.R4300iCommands;
import kerneln64.Main;


/** R4300i instruction text for the debugger, with branch/jump targets and operands. */
final class MipsDisassembler {
    private static final String[] REG={"zero","at","v0","v1","a0","a1","a2","a3","t0","t1","t2","t3","t4","t5","t6","t7","s0","s1","s2","s3","s4","s5","s6","s7","t8","t9","k0","k1","gp","sp","fp","ra"};
    static String format(int word,int pc) {
        if(word==0)return "NOP";
        int op=word>>>26,rs=(word>>>21)&31,rt=(word>>>16)&31,rd=(word>>>11)&31,sa=(word>>>6)&31,fn=word&63;
        String name=kerneln64.debugger.trace.R4300iCommands.R4300iOpcodeName(word,pc).replace("SPECIAL_","").replace("REGIMM_","");
        String a="$"+REG[rs],b="$"+REG[rt],d="$"+REG[rd],target=String.format("%08X",pc+4+((short)word<<2));
        if(op==0) {
            return name+switch(fn){
                case 0,2,3,56,58,59,60,62,63 -> " "+d+", "+b+", "+sa;
                case 4,6,7,20,22,23 -> " "+d+", "+b+", "+a;
                case 8 -> " "+a;
                case 9 -> " "+d+", "+a;
                case 16,18 -> " "+d;
                case 17,19 -> " "+a;
                case 24,25,26,27,28,29,30,31 -> " "+a+", "+b;
                case 12,13,15 -> "";
                default -> " "+d+", "+a+", "+b;
            };
        }
        if(op==2||op==3)return name+" "+String.format("%08X",((pc+4)&0xF0000000)|((word&0x3FFFFFF)<<2));
        if(op==1)return name+" "+a+", "+target;
        if(op==4||op==5||op==20||op==21)return name+" "+a+", "+b+", "+target;
        if(op==6||op==7||op==22||op==23)return name+" "+a+", "+target;
        if(op==15)return "LUI "+b+String.format(", 0x%04X",word&65535);
        if(op>=8&&op<=14||op==24||op==25)return name+" "+b+", "+a+", "+(op>=12&&op<=14?String.format("0x%04X",word&65535):Short.toString((short)word));
        if(op==16||op==17){
            if(rs==0||rs==1||rs==2||rs==4||rs==5||rs==6)return name+" "+b+", "+(op==17?"$f":"$c0r")+rd;
            if(rs==8)return name+" "+target;
            if(op==17&&rs>=16){int ft=rt,fs=rd,fd=sa;return name+" $f"+fd+", $f"+fs+(fn<=3?", $f"+ft:"");}
            return name;
        }
        if(op>=26){String reg=(op==49||op==53||op==57||op==61)?"$f"+rt:b;return name+" "+reg+", "+(short)word+"("+a+")";}
        return name+String.format(" 0x%08X",word);
    }
}
