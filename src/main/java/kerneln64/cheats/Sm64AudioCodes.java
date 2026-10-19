package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.util.*;
import java.util.function.IntToDoubleFunction;

/** US audio data verified against matching retail RAM and n64decomp/sm64
 * 9921382a68bb0c865e5e45eb594d9c64db59b1af, src/audio/data.c.
 * These presets change bounded pitch data, never the sequence parser or pointers.
 */
public final class Sm64AudioCodes {
    public static final int NOTES = 0x333994, VIBRATO = 0x333BB4;
    public static final float[] FREQUENCIES = {
    0.105112f,  0.111362f,  0.117984f,  0.125f, 0.132433f, 0.140308f,  0.148651f,  0.15749f,  0.166855f, 0.176777f, 0.187288f,  0.198425f,
    0.210224f,  0.222725f,  0.235969f,  0.25f,  0.264866f, 0.280616f,  0.297302f,  0.31498f,  0.33371f,  0.353553f, 0.374577f,  0.39685f,
    0.420448f,  0.445449f,  0.471937f,  0.5f,   0.529732f, 0.561231f,  0.594604f,  0.629961f, 0.66742f,  0.707107f, 0.749154f,  0.793701f,
    0.840897f,  0.890899f,  0.943875f,  1.0f,   1.059463f, 1.122462f,  1.189207f,  1.259921f, 1.33484f,  1.414214f, 1.498307f,  1.587401f,
    1.681793f,  1.781798f,  1.887749f,  2.0f,   2.118926f, 2.244924f,  2.378414f,  2.519842f, 2.66968f,  2.828428f, 2.996615f,  3.174803f,
    3.363586f,  3.563596f,  3.775498f,  4.0f,   4.237853f, 4.489849f,  4.756829f,  5.039685f, 5.33936f,  5.656855f, 5.993229f,  6.349606f,
    6.727173f,  7.127192f,  7.550996f,  8.0f,   8.475705f, 8.979697f,  9.513658f,  10.07937f, 10.67872f, 11.31371f, 11.986459f, 12.699211f,
    13.454346f, 14.254383f, 15.101993f, 16.0f,  16.95141f, 17.959394f, 19.027315f, 20.15874f, 21.35744f, 22.62742f, 23.972918f, 25.398422f,
    26.908691f, 28.508766f, 30.203985f, 32.0f,  33.90282f, 35.91879f,  38.05463f,  40.31748f, 42.71488f, 45.25484f, 47.945835f, 50.796844f,
    53.817383f, 57.017532f, 60.40797f,  64.0f,  67.80564f, 71.83758f,  76.10926f,  80.63496f, 85.42976f, 45.25484f, 47.945835f, 50.796844f,
    53.817383f, 57.017532f, 60.40797f,  64.0f,  67.80564f, 71.83758f,  76.10926f,  80.63496f

    };
    static final int[][] SIGNATURE = {{0x31CB84,0x3C038033}, {0x31CB88,0x24633994},
        {0x31B4AC,0x3C088033}, {0x31B4C0,0x25083BB4}};
    private static final String SOURCE = "https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/";
    public static List<CheatRepository.Entry> presets() {
        List<CheatRepository.Entry> list = new ArrayList<>();
        pitch(list,"Alien bass (one octave down)", i -> FREQUENCIES[i]*0.5,"Halves note frequencies; lower and heavier music.");
        pitch(list,"Alien helium (one octave up)", i -> FREQUENCIES[i]*2.0,"Doubles note frequencies; high notes may reach the game's resampling limit.");
        pitch(list,"Alien detune (half semitone up)", i -> FREQUENCIES[i]*Math.pow(2,0.5/12),"Raises notes by half a semitone for an unfamiliar tuning.");
        pitch(list,"Alien split octaves", i -> FREQUENCIES[i]*(i%2==0?0.5:2.0),"Alternates low and high octaves by note number.");
        pitch(list,"Alien crooked scale", i -> FREQUENCIES[i]*Math.pow(2,(i%3-1)*3.0/12),"Repeats a -3, 0, +3 semitone distortion across note numbers.");
        pitch(list,"Alien robot monotone", i -> 0.5,"Replaces the note table with one pitch ratio. Instrument/sample tuning still differs.");
        pitch(list,"Alien mirrored melody", i -> FREQUENCIES[i]*Math.pow(2,(11-2*(i%12))/12.0),"Mirrors the twelve note positions within each octave, scrambling familiar melodies.");
        pitch(list,"Alien alternating detune", i -> FREQUENCIES[i]*Math.pow(2,(i%2==0?-0.5:0.5)/12),"Alternates notes a half-semitone flat and sharp for an uneven alien tuning.");
        return list;
    }
    private static String notes(String behavior) {
        return "SM64 US v1.0 / compatible US-based extensions only. "+behavior
            +" Changes gNoteFrequencies (80333994). New notes use the changed ratios; sampled/percussion sounds may be unaffected."
            +" Up to four Audio effects can be enabled within the code budget. Supported pitch tables mix their relative changes; other writes must agree. Enable after boot. Disabling restores the table; held notes may finish at their current pitch. Guarded against different audio code layouts. Source: "
            +SOURCE+"data.c; readers: "+SOURCE+"seqplayer.c. KernelN64 generated preset; chained guards use this emulator's conditional semantics.";
    }
    private static void pitch(List<CheatRepository.Entry> list,String name,IntToDoubleFunction value,String behavior) {
        list.add(new CheatRepository.Entry("Audio\\Generated distortion\\"+name,notes(behavior),pitchCode(value),false,"sound"));
    }
    static String pitchCode(IntToDoubleFunction value) {
        StringBuilder code=new StringBuilder();
        for(int i=0;i<FREQUENCIES.length;i++) {
            float frequency=(float)value.applyAsDouble(i);
            if(!Float.isFinite(frequency)||frequency<=0)throw new IllegalArgumentException("Invalid note frequency");
            int bits=Float.floatToRawIntBits(frequency);
            write(code,NOTES+i*4,2,bits>>>16); write(code,NOTES+i*4+2,2,bits&65535);
        }
        return code.toString().strip();
    }
    private static void write(StringBuilder out,int address,int width,int value) {
        // This engine chains consecutive D1 conditions until the following write.
        for(int[] site:SIGNATURE) {
            out.append(String.format(Locale.ROOT,"%08X %04X%n",0xD1000000|site[0],site[1]>>>16));
            out.append(String.format(Locale.ROOT,"%08X %04X%n",0xD1000000|site[0]+2,site[1]&65535));
        }
        out.append(String.format(Locale.ROOT,"%08X %04X%n",(width==1?0x80000000:0x81000000)|address,value));
    }
}
