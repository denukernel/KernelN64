# SM64 instrument/sample distortion

Start a course with music, then open **Tools > Cheats**. The dialog automatically
builds two disabled entries under **Audio > Sample distortion > Bank ...**:

- **Rake residual crunch (approximation)** zeros ADPCM predictor coefficients.
  The original compressed instrument/drum samples then decode largely as coarse
  residual pulses rather than reconstructing their normal waveforms.
- **Rake stepped predictors (approximation)** rounds each signed coefficient
  toward zero in steps of 1024, changing the reconstruction and timbre of those
  original samples.

These are sample-decoding effects. They do not change the note-frequency table,
song tempo, emulator speed, sample pointers, ADPCM dimensions, or sequence
bytecode. They remain approximations of the requested
[Vinesauce reference at 11:49](https://www.youtube.com/watch?v=FkEyF8hibWQ&t=709s).
The reference audio was not measured or matched.

Choose one Audio effect at a time. After changing levels, use **Build Sample
Codes** for the newly loaded music bank. Disable sample effects before rebuilding
them. Generated entries are saved with the named cheat list when you toggle or
edit a cheat. Reopening Cheats regenerates missing entries for the current bank.

Disabling restores owned coefficients while the same bank/book allocation is
still loaded. Queued PCM and already decoded samples can finish before normal
sound returns. If the allocation has been unloaded/reused, the engine discards
its stale undo instead of writing the previous bank's coefficients into it.

## Supported target and code pack

Verified US audio layout in SM64 US v1.0, SHA-256
`17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91`,
and the user's US extension, SHA-256
`5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0`.
Other regions, relocated audio engines, physical GameSharks, and other cheat
engines are untested. These codes use Jario64+'s chained-condition semantics
and its bank-aware undo.

`SM64-Instrument-Sample-Distortion-US.cheats.txt` contains four disabled examples
captured from the extended-ROM Bob-omb and slide fixtures: two effects each for
bank 22 and bank 0D, with the captured instrument table at guest `801FC654`.
They apply only when their captured metadata/pointer guards match. Building
codes in the running game is preferred when its allocation differs.

Bank 22 is used by the grass music shared by Bob-omb Battlefield and Whomp's
Fortress; bank 0D is used by slide music, including Cool, Cool
Mountain's indoor slide. Its outdoor snow music uses bank 0B; build codes while
that music is playing to target it. The fixtures directly exercised Bob-omb and the secret slide, not the
exact corrupted Whomp/CCM scenes in the video.

## Source-to-address evidence

Matching source revision:
[`9921382a68bb0c865e5e45eb594d9c64db59b1af`](https://github.com/n64decomp/sm64/tree/9921382a68bb0c865e5e45eb594d9c64db59b1af).
Relevant files are `src/audio/internal.h`, `seqplayer.c`, `load.c`, `synthesis.c`,
and `sound/sequences.json`. The checked-out source is under
`target/sm64-research` during investigation.

The US `get_instrument` binary establishes the bank table:

| Guest PC | Word | Instruction |
| --- | --- | --- |
| 8031CE64 | 3C028022 | LUI v0,8022 |
| 8031CE68 | 8C426B60 | LW v0,6B60(v0): gCtlEntries pointer |
| 8031CE74 | 0018C080 | SLL t8,t8,2: 12-byte CtlEntry indexing |
| 8031CE90 | 8C690004 | LW t1,4(v1): instrument table |
| 8031DBB8 | 3C048022 | LUI a0,8022 |
| 8031DBBC | 248414D0 | ADDIU a0,a0,14D0: bank load statuses |

The generator reads background player's bank ID at `8022261E`; resolves the
pointer stored at `80226B60`; indexes `CtlEntry` by bank * 12; and follows the
instrument table at entry +4 and drum table at entry +8. These are guest virtual
addresses. Their physical RDRAM offsets omit `80000000`.

US structures and readers establish:

- Instrument: three AudioBankSound fields at +8, +16 and +24.
- Drum: AudioBankSound at +4.
- AudioBankSound: sample pointer at +0.
- AudioBankSample: predictor-book pointer at +12, sample address at +4,
  compressed sample size at +16.
- AdpcmBook: signed 32-bit order at +0 and predictor count at +4, followed by
  `8 * order * predictors` signed 16-bit coefficients at +8. The generator
  accepts order 2, 1–8 predictors, aligned valid RDRAM pointers and bounded tables.

Each aligned 81 halfword write is guarded by all six unchanged reader words,
the bank-table pointer and metadata, loaded status (not 0 or 1), owning
instrument/drum table slot, sample pointer/address/size, book pointer, order and
predictor count. No guessed heap offsets are used. Duplicate books are patched
once. Book guards are independent of the coefficients being changed.

Each book is a separate conditional program inside one named preset. The named
cheat parser preserves its internal blank program boundaries. The engine
recognizes the generated four-condition reader prefix, rechecks the book's
immutable guards before undo, and drops ownership on a failed lifetime check.
This also works with the existing serialized cheat undo format in savestates;
no guest save-state schema changed. Ordinary conditional cheats retain their
existing restoration behavior.

## Verification

`jario64.SampleCodeChecks` verifies coefficient-only writes, named multi-program
roundtrips, unchanged headers/pointers, restoration, rejection of malformed banks,
suppression during loading, and stale-undo invalidation after serialization.

`jario64.SampleCodeRomChecks ROM copied-state [RAM-MB [--start-course]]` runs in
an isolated directory. It builds actual bank codes, captures 262144 bytes of HLE
PCM per effect from the same baseline, requires distinct nonzero output, checks
for libultra faults, checks unchanged note/tempo data, and verifies every original
coefficient after disabling. It also checks automatic menu generation.

Both effects passed on extended-ROM Bob-omb (16 unique books including drums),
extended-ROM slide (20 books), and retail Bob-omb (16 books). Tests use copied
states; the original ROMs, save states and user cheat list are not edited.
