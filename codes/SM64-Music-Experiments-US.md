# SM64 US music experiments

Open **Tools > Cheats > Audio > Music experiments**, then enable one preset:

- **Fast music - 240 BPM (approximation)**: a fixed fast music tempo.
- **Frenzied music - 288 BPM (approximation)**: a fixed faster music tempo.
- **Gag me with a rake - dissonance (approximation)**: scrambles the note scale.
- **Gag me with a rake - fast dissonance (approximation)**: combines that scale with 240 BPM.

The user supplied [Vinesauce's April 21, 2019 stream](https://www.youtube.com/watch?v=FkEyF8hibWQ),
at **10:23** and **11:49**, as references. These are explicitly requested
approximations from the described effects. The video audio was not measured or
matched. The rake presets change note pitches; they do not recreate corrupted
instrument samples, sample clipping, or the original ROM corruption.

the companion `.cheats.txt` file contains the twelve pitch/tempo presets for import, disabled.
They appear automatically in existing SM64 cheat lists on the next load, with
custom entries and enabled selections retained. they use the existing `sound`
category. up to four audio effects can be selected within the operation budget.
supported pitch tables mix their relative changes; different tempo values conflict.

## eight additional variations

| preset | note pitch | music tempo |
| --- | --- | --- |
| slow music - 72 bpm | unchanged | 72 bpm |
| steady pulse - 180 bpm | unchanged | 180 bpm |
| deep space bass - 72 bpm | two octaves down | 72 bpm |
| tiny arcade - 216 bpm | multiply by 1.5 | 216 bpm |
| whole tone staircase | odd note numbers move down one semitone | current tempo |
| tritone seesaw | alternate six semitones down and up by note number | current tempo |
| octave carousel | repeat minus twelve, zero and plus twelve semitones by note number | current tempo |
| detuned clockwork - 168 bpm | alternate a quarter semitone flat and sharp by note number | 168 bpm |

these are deterministic pitch and tempo variations. alternating patterns depend
on note number, not elapsed time. they do not modify sample data, reproduce a
reference recording, or add an audio processing filter. pitched sound effects
that read the same frequency table can change too; sound-effect tempo is untouched.

the added tempo halfwords are `0D80`, `21C0`, `2880` and `1F80` for 72, 180,
216 and 168 bpm respectively, each multiplied by 48. they use the same verified
player offsets and reader guards described below. the pitch-only variants write
the existing 128-float table at guest address `80333994`, with the same guards.
no new absolute addresses or instruction patches are introduced.

catalog version 2 adds the eight entries to saved version 1 lists by name,
preserving existing entries and selections. repeated loads do not duplicate them.

the eight additions were tested in kerneln64 with gln64j and azimer hle audio,
using the verified us-based extended rom and an 8 mib bob-omb battlefield state.
each replay ran for 180 vi events, with 131072 bytes captured from guest ai dma
buffers after the first 30 vi events. all eight captures were nonzero and differed
from the baseline and one another. live pitch and tempo writes matched the preset,
and disabling restored the note table and music tempo. the input state was preserved.
this checks live synthesis and memory behavior, not listening quality, full-game
coverage, other regions, physical gamesharks, or other emulator conditional scopes.

local checks also cover write bounds, finite positive pitch values, every reader
guard halfword, sound-effect tempo isolation, undo, catalog migration, supported
pitch mixing and conflicting tempos. testing files and captures are excluded from
the source checkout and release archives.

## Target and address evidence

SM64 US v1.0, big-endian, SHA-256
`17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`,
and the tested US-based extended ROM, SHA-256
`5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0`.
Other layouts need independent verification. These chained D1 conditions use
KernelN64's conditional scope; other emulators and physical GameSharks are untested.

Source revision: n64decomp/sm64
`9921382a68bb0c865e5e45eb594d9c64db59b1af`:

- [internal.h](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/internal.h):
  US `SequencePlayer` size `0x140`; unsigned 16-bit `tempo` at offset `0xA`;
  `TATUMS_PER_BEAT` is 48.
- [seqplayer.c](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/seqplayer.c):
  `process_sequences` iterates the players; `sequence_player_process_sequence`
  adds `tempo` to `tempoAcc` and advances the script on threshold crossing.
  The tempo command converts BPM to BPM * 48.

Matching captured US RAM establishes `gSequencePlayers = 80222618`:

| Guest instruction | Word | Meaning |
| --- | --- | --- |
| `8031E258` | `3C118022` | LUI s1,8022 |
| `8031E278` | `26312618` | ADDIU s1,s1,2618 |
| `8031E2A8` | `26310140` | ADDIU s1,s1,0140 (next player) |
| `8031DC18` | `962F000A` | LHU t7,000A(s1) (tempo) |
| `8031DC20` | `01AF5821` | ADDU t3,t5,t7 (accumulate) |
| `8031DC28` | `84636B7C` | LH v1,6B7C(v1) (threshold) |

Every tempo write checks both halves of all six unchanged words. The music
tempo halfwords are guest **80222622** (player 0, background music) and
**80222762** (player 1, secondary music). Physical RDRAM offsets are `00222622`
and `00222762`. Player 2's tempo at `802228A2` is left alone.
Values are `2D00` for 240 * 48 and `3600` for 288 * 48. The observed threshold
at `80226B7C` is 14360, approximately 299 BPM; both presets stay below it.
These are absolute tempos, not 2x/3x multipliers. Tempo does not raise note
pitch, change CPU speed, or change VI rate. Songs can issue their own tempo
commands between cheat applications.

The rake scale changes the verified 128-float `gNoteFrequencies` table at
`80333994`. For note index i, the original ratio is multiplied by
`2^(offset[i mod 12]/12)`, with offsets
`[-12,7,-5,12,-7,1,10,-10,5,-2,8,-6]` semitones. Ratios remain finite and
positive. The existing four-word note-reader guards accompany every float
halfword write; see `SM64-Audio-Distortion-US.md` for those addresses.

## Activation and restoration

Enable after boot. New notes use the changed scale; percussion that bypasses
the note table can remain unchanged. Disabling restores owned tempo/table
bytes through KernelN64's undo system. Existing notes may finish at their old
pitch or duration. If the game writes a new tempo, the undo baseline follows
that newer value; disabling does not rewind the song position.

The presets change data only. They do not patch branches, script opcodes,
instrument pointers, M64 data, or the audio thread's control flow.

## Reproducible checks

Local music-code checks cover write bounds, SFX-tempo isolation, finite note
ratios, each tempo-layout guard, undo, and old named-catalog migration.

Local music-code ROM checks (not included in the prepared source snapshot) must run in an isolated working
directory. Its optional scene defaults to 32 MiB RDRAM; specify 8 for a retail
8 MiB state. `--start-course` presses A to enter the course from the supplied
retail star-select fixture, avoiding its short non-looping jingle. It counts real background
music tick crossings at `8031DC44` over 60 VI, then captures 262144 bytes of
actual HLE PCM per preset from a common state. It checks that both fast tempos
advance music faster, every preset changes nonzero PCM, libultra has no fault,
and tempo/pitch data return after disabling. Logs and PCM captures remain in
that test directory. This verifies guest behavior and synthesis changes, not
an exact match to the reference videos or hardware compatibility.

The extended-ROM Bob-omb Battlefield scene produced **90 / 190 / 228** music
ticks per 60 VI for baseline / 240 BPM / 288 BPM. The slide scene produced
**133 / 190 / 228**. All four presets produced different nonzero PCM and passed
undo/fault checks in both extended scenes and the retail Bob-omb course.


## delayed notes and catch-up bursts

catalog version 3 adds **delayed notes and catch-up bursts (approximation)**,
disabled, while preserving custom entries and existing selections. this repeats
several seconds of delayed music progression followed by several seconds of fast
progression. it changes tempo over time without changing pitch or note gates.
see `SM64-Delay-Bursts-US.md` and its companion import file for the timer and
audio-reader derivation, activation phase and verification limits.


catalog version 4 adds **faster bursts** and **extremely fast bursts** to the delay
preset family. their delay length and burst length remain about 4.27 seconds each;
only the burst music progression speeds up. select one version at a time. these
variants also patch shared sequencer countdowns during bursts, so sound-effect
timing can change then. see `SM64-Delay-Bursts-US.md` for the verified instruction
words, nonlinear speed behavior, restoration and test measurements.
