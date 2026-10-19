# SM64 US audio distortion presets

Open **Tools > Cheats > Audio > Generated distortion**. Choose one effect.
The eight presets shift pitch down/up an octave, detune by half a semitone,
split octaves by note number, bend the scale, flatten note pitches, mirror
melodies within each octave, or alternate flat/sharp tuning. Instrument sample
tuning still applies. Percussion or effects that bypass the note table can remain
unchanged. New notes pick up the effect; held notes can finish at their old pitch.

`SM64-Audio-Distortion-US.cheats.txt` contains the same presets for import, all
disabled. These are generated Jario64+ presets. Their chained D1 guards use
Jario64+'s conditional scope; other engines/hardware require separate testing.
Disabling restores the original table through this emulator's cheat undo system.
All Audio presets share one exclusive group, including the historical Alien code.

## Address evidence

Target tested: SM64 US v1.0, big-endian `80371240`, CRC `635A2BFF 8B022326`,
SHA-256 `17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`.
US-based ROM extensions must retain the verified audio reader instructions.
PAL, Japanese, relocated audio engines, and recompiled hacks are not assumed
compatible. Guards suppress writes if the known reader instructions differ.

The complete 128-float `gNoteFrequencies` table from
[n64decomp/sm64 data.c at 9921382](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/data.c)
matches all 512 bytes at guest virtual address **80333994**, physical RDRAM
**00333994**, in the tested ROM. The sequence-layer code reads it for note pitch
and portamento in
[seqplayer.c](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/seqplayer.c).
The US binary at `8031CB84` contains `3C038033` (`LUI v1,8033`), followed by
`24633994` (`ADDIU v1,v1,3994`), which establishes the table address. A second
audio-layout signature, `3C088033` at `8031B4AC` and `25083BB4` at `8031B4C0`,
identifies the adjacent vibrato-table initializer. Every generated halfword write
is guarded by both halves of all four unchanged signature words.

`Sm64AudioCodes` derives finite positive float ratios from the original table.
It encodes each float as two aligned big-endian 81 halfword writes. Each preset
is applied at VI on the CPU thread, before execution resumes; it changes audio
data, not branch targets, pointers, or the M64 parser.

## Validation

Synthetic checks cover wrong-layout suppression, positive finite values, table
bounds, exclusive selection, disable restoration, and migration of old named
lists without enabling new entries or replacing custom codes. Runtime checks
start each effect from the same guest savestate, capture 262144 bytes of actual
HLE PCM, require non-silent output different from baseline, check for libultra
thread faults, and verify the original pitch table returns after disabling.
All eight generated effects changed PCM on both stock US and the 24 MiB US
extension (SHA-256 `5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0`).
This verifies synthesis changes, not a physical console or a subjective promise
that every instrument will sound equally different.

The additional documented gameplay presets were checked against a matching
32 MiB-RDRAM extended-ROM gameplay state. The test clears saved cheat programs
and restores their undo records before taking its baseline. It verifies target
writes for all 22 additions, resumes the guest for 30 VI per case, and checks
for thread faults. Cap effects and the cap timer remain enabled while it runs;
level-selector checks verify destination writes but do not enter all fourteen
destination scenes. Original user saves/states are only read, never rewritten.

The historical NES byte patch was evaluated but left out: it changed no captured
PCM with the HLE backend. Vibrato-table-only experiments were also left out
because the tested music did not use that modulation. The existing historical
Alien parser patch remains marked experimental in **Audio > Documented**.
