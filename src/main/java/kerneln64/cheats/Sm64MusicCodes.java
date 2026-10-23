package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.util.*;

/** Music data addresses derived from the matching US process_sequences binary.
 * No changes to sequence bytecode, pointers, or instruction flow. */
final class Sm64MusicCodes {
    static final int PLAYERS=0x222618, STRIDE=0x140, TEMPO=0xA;
    static final int[][] SIGNATURE={
        {0x31E258,0x3C118022},{0x31E278,0x26312618},
        {0x31E2A8,0x26310140},{0x31DC18,0x962F000A},
        {0x31DC20,0x01AF5821},{0x31DC28,0x84636B7C}
    };
    private static final int[] RAKE_OFFSETS={-12,7,-5,12,-7,1,10,-10,5,-2,8,-6};
    static List<CheatRepository.Entry> presets() {
        String rake=Sm64AudioCodes.pitchCode(i->Sm64AudioCodes.FREQUENCIES[i]*Math.pow(2,RAKE_OFFSETS[i%12]/12.0));
        return List.of(
            entry("Fast music - 240 BPM (approximation)",tempoCode(240),
                "Locks background and secondary music to 240 BPM. Song tempo changes; note pitch and gameplay speed do not. This is an absolute tempo, not a 2x multiplier."),
            entry("Frenzied music - 288 BPM (approximation)",tempoCode(288),
                "Locks background and secondary music to 288 BPM, close to the tested US engine's 299 BPM tick limit. Absolute tempo, not a speed multiplier."),
            entry("Gag me with a rake - dissonance (approximation)",rake,
                "Scrambles the note scale with repeating -12, +7, -5, +12, -7, +1, +10, -10, +5, -2, +8, -6 semitone offsets. Keeps the current song tempo. This bends the melody; it does not reproduce corrupted instrument samples."),
            entry("Gag me with a rake - fast dissonance (approximation)",rake+"\n"+tempoCode(240),
                "Combines the dissonant note scale with 240 BPM background/secondary music."),
            variation("slow music - 72 bpm",tempoCode(72),
                "sets background and secondary music to 72 bpm without changing note pitch."),
            variation("steady pulse - 180 bpm",tempoCode(180),
                "sets background and secondary music to 180 bpm without changing note pitch."),
            variation("deep space bass - 72 bpm",pitch(i->Sm64AudioCodes.FREQUENCIES[i]*0.25)+"\n"+tempoCode(72),
                "lowers new notes by two octaves and sets background and secondary music to 72 bpm."),
            variation("tiny arcade - 216 bpm",pitch(i->Sm64AudioCodes.FREQUENCIES[i]*1.5)+"\n"+tempoCode(216),
                "multiplies new note pitch by 1.5 and sets background and secondary music to 216 bpm."),
            variation("whole tone staircase",shift(i->-(i%2)),
                "maps odd note numbers down one semitone, pairing adjacent notes into a whole tone scale. keeps the current tempo."),
            variation("tritone seesaw",shift(i->i%2==0?-6:6),
                "alternates new notes six semitones down and up by note number. keeps the current tempo."),
            variation("octave carousel",shift(i->(i%3-1)*12),
                "repeats low, original and high octaves by note number. keeps the current tempo."),
            variation("detuned clockwork - 168 bpm",shift(i->i%2==0?-0.25:0.25)+"\n"+tempoCode(168),
                "alternates new notes a quarter semitone flat and sharp, with background and secondary music at 168 bpm."),
            Sm64BurstMusicCodes.preset(),
            Sm64BurstMusicCodes.fasterPreset(),
            Sm64BurstMusicCodes.extremePreset()
        );
    }
    private static String pitch(java.util.function.IntToDoubleFunction value) {
        return Sm64AudioCodes.pitchCode(value);
    }
    private static String shift(java.util.function.IntToDoubleFunction semitones) {
        return pitch(i->Sm64AudioCodes.FREQUENCIES[i]*Math.pow(2,semitones.applyAsDouble(i)/12.0));
    }
    private static CheatRepository.Entry variation(String name,String code,String behavior) {
        return new CheatRepository.Entry("Audio\\Music experiments\\"+name,
            "sm64 us v1.0 / verified compatible us-based extensions only. "+behavior
            +" these are pitch and tempo variations, not corrupted samples or a match to a reference recording."
            +" enable after boot with music playing. new notes use the changed scale; sampled percussion can remain unchanged and high notes can reach the resampling limit."
            +" tempo writes affect music players 0 and 1, leaving the sound-effect player tempo alone. pitch-table changes can also affect pitched sound effects."
            +" up to four audio effects can mix supported pitch tables; different tempo values conflict. disabling restores owned bytes, while held notes can finish at the changed pitch."
            +" chained layout guards require the verified us audio readers; physical gamesharks and other engines are untested."
            +" source: https://github.com/n64decomp/sm64/tree/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio (data.c, seqplayer.c, internal.h).",
            code,false,"sound");
    }
    private static CheatRepository.Entry entry(String name,String code,String behavior) {
        return new CheatRepository.Entry("Audio\\Music experiments\\"+name,
            "SM64 US v1.0 / verified compatible US-based extensions. "+behavior
            +" Inspired by the requested Vinesauce references at 10:23 and 11:49: https://www.youtube.com/watch?v=FkEyF8hibWQ."
            +" Approximation from the described effects; the reference audio was not measured or matched."
            +" Up to four Audio effects can be enabled within the code budget. Supported pitch tables mix their relative changes; other writes must agree. Tempo affects players 0/1; the sound-effect player's tempo is untouched."
            +" Disabling restores owned bytes. Already scheduled notes keep their duration/pitch until they end; a song change can establish a newer original tempo."
            +" Layout guards suppress incompatible writes. Source: https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/seqplayer.c and src/audio/internal.h."
            +" Generated KernelN64 preset; chained D1 guards use this engine's conditional semantics.",code,false,"sound");
    }
    static String tempoCode(int bpm) {
        if(bpm<1 || bpm>288)throw new IllegalArgumentException("Tempo out of tested bounds");
        StringBuilder out=new StringBuilder();
        for(int player=0;player<2;player++) {
            for(int[] site:SIGNATURE) {
                out.append(String.format(Locale.ROOT,"%08X %04X%n",0xD1000000|site[0],site[1]>>>16));
                out.append(String.format(Locale.ROOT,"%08X %04X%n",0xD1000000|site[0]+2,site[1]&65535));
            }
            out.append(String.format(Locale.ROOT,"%08X %04X%n",0x81000000|PLAYERS+player*STRIDE+TEMPO,bpm*48));
        }
        return out.toString().strip();
    }
}
