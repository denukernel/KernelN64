# sm64 delayed notes and catch-up bursts

open **tools > cheats > audio > music experiments > delayed notes and catch-up
bursts (approximation)**, or import `SM64-Delay-Bursts-US.cheats.txt` from this
folder. the pack contains the original, faster bursts and extremely fast bursts presets,
all disabled. catalog version 4 adds the two variations; version 5 repairs their
sound-effect isolation in existing lists without
replacing custom entries or changing their selections. select one at a time.

## behavior and target

the original preset approximates the requested repeating delay/fast/delay corruption:

- about 4.27 seconds of stalled music sequencing;
- about 4.27 seconds at 288 bpm;
- repeat, without changing pitch, note cutoff, samples or gameplay speed.

the clock already runs in the game, so enabling can start in either phase.
loading a state restores that saved clock phase. actual phase length follows
game-frame progress: it is 128 frames at the usual 30 game frames/s. emulation
pause freezes it; fast-forward changes wall-clock durations.

already sounding notes, percussion samples and reverb tails can continue during
a stall. this does not forcibly mute the output, postpone every decoded sample,
or replay buffered audio faster. it freezes advancement of the two music players,
then advances their remaining score quickly. a song change can occur in either
phase. the sound-effect player retains its normal tempo.

the reference identifies red as star select, yellow as the first ccm section,
and green as the subsequent section. the supplied MIDI has a constant 120 bpm;
its note events do not establish the underlying ROM corruption. this code is a
bounded approximation of the subsequently clarified repeated stall/burst behavior,
not a MIDI import, exact transcription, fixed pitch remapping, or reconstruction
of the original fault. the period and 288 bpm are chosen effect settings.

verified runtime target: the US-based extended sm64 ROM with sha-256
`5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0`,
using the stock US audio and game timer layout. the supplied addresses are for
SM64 US v1.0 and verified compatible extensions. relocated builds need their
matching ELF, gGlobalTimer/gSequencePlayers symbols, and unchanged normalized
reader functions. other regions and changed audio engines require a new derivation.
physical gamesharks and other emulators are untested.

## source and address derivation

source revision:
[n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af](https://github.com/n64decomp/sm64/tree/9921382a68bb0c865e5e45eb594d9c64db59b1af).

[`src/game/game_init.c`](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/game/game_init.c)
initializes gGlobalTimer and increments it in display_and_vsync after two VI
waits. captured instructions establish its guest virtual address **8032d5d4**:

| guest pc | word | operation |
| --- | --- | --- |
| 802481b4 | 3c198033 | lui t9,8033 |
| 802481b8 | 8f39d5d4 | lw t9,-0x2a2c(t9), gGlobalTimer |
| 802481bc | 3c018033 | lui at,8033 |
| 802481c0 | 272f0001 | addiu t7,t9,1 |
| 802481c4 | ac2fd5d4 | sw t7,-0x2a2c(at), gGlobalTimer |

the timer increased by exactly 60 across a 120-VI main-state replay. the US
binary display_and_vsync function spans 80248090 through 802481df (size 0x150).
the decomp profile accepts it only with the matching normalized instruction
fingerprint. the timer word itself is read only. its low byte is at guest
**8032d5d7**, physical RDRAM offset **0032d5d7**.

[`src/audio/internal.h`](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/internal.h)
and [`seqplayer.c`](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/src/audio/seqplayer.c)
establish the tempo accumulator and US tempo units, bpm * 48. tempo zero stops
adding new ticks after any already pending tick; 288 bpm is 0x3600 and remains
below the observed 14360 tick threshold. the existing verified sequence-player
readers establish gSequencePlayers at guest virtual 80222618, stride 0x140.

the original preset writes only these halfwords:

| guest virtual address | physical RDRAM offset | purpose |
| --- | --- | --- |
| 80222622 | 00222622 | music player 0 tempo |
| 80222762 | 00222762 | music player 1 tempo |

timer byte 00..7f selects tempo 0000; 80..ff selects tempo 3600. each write has
all six established music reader word guards plus all five timer reader/store
word guards. a chain of 128 D2 byte-not-equal conditions excludes the opposite
half of the timer cycle. kerneln64 ANDs consecutive D1/D2 conditions until the
following write. no undocumented range, timer, arithmetic, hook or pointer code
is used. the original preset has four conditional writes totaling 604 operations per VI.

these chained-condition semantics are engine-specific. the complete import
file is the paste-ready program; do not remove its conditions or treat it as a
portable physical-GameShark timer extension.

## activation, conflicts and restoration

enable after boot. use it alone first. it conflicts with other music tempo locks;
a separate pitch-only effect can be combined within the normal audio limits.
it does not conflict with note/sample data by itself because it does not write
those fields. disabling restores owned tempo bytes in kerneln64 without a ROM
restart. the score position is not rewound, and a newer game tempo write can
establish a newer undo baseline. guest stalls during loading can hold a phase
until game-frame updates resume.

## verification limits

all 256 timer-byte values selected the expected tempo in local checks. changing
each of the eleven guarded words suppressed the writes. the write footprint
contained exactly four music-tempo bytes and no game-timer writes.

an isolated copy of the required main sm64 state was replayed for 1100 VI with
kerneln64, kernel64gl and azimer HLE audio. phase transitions occurred at VI
1, 215, 471, 727 and 983: each complete phase lasted 256 VI, or 128 game frames.
background music crossed 1 tick during 588 stall-phase VI and 1972 ticks during
512 fast-phase VI. the single stall-phase crossing can occur at a boundary or
from a pending tick. both phases supplied 262144 bytes of captured synthesis.

pitch-table bytes and the note-cutoff instruction remained unchanged. disabling
restored the original background music tempo. catalog migration retained a
custom entry and added this preset disabled. the test window was visible and
closed afterward; the original savestate was preserved.

these checks establish timing, memory and synthesized-output capture. they do
not establish an exact listening match, the precise star-select/ccm arrangement,
other games, other regions, physical hardware, or relocated decomp runtime
coverage. the reference MIDI, RAM dumps, states, scripts and PCM captures are
excluded from the github source checkout and release archive.


## faster and extremely fast bursts

both variations keep the original approximately 4.27-second stalled and
4.27-second burst phases, including the 288 bpm burst tempo. the original
preset and delay-mode program are unchanged.

version 5 removes the shared instruction patches that accelerated sound effects.
instead, these variations require the **KernelN64 B0 music-countdown extension**.
this is an emulator-specific operation, not a portable hardware GameShark code.
other cheat engines and relocated audio engines are unsupported.

| preset | music countdown above one during bursts | sound effects and delay mode |
| --- | --- | --- |
| faster bursts | remaining logically shifted right by one | normal countdown |
| extremely fast bursts | remaining logically shifted right by two | normal countdown |

these are nonlinear countdown changes, not uniform 2x/4x song-speed multipliers.
the extension leaves guest instructions, note gates, samples, pitch tables and
sound-effect tempo unchanged. faster music can still increase simultaneous music
voice demand; the normal game voice allocator remains in control.

### ownership and instruction evidence

matching US binary instructions and the linked decomp source establish:

| guest pc | original word | owner lookup before the countdown |
| --- | --- | --- |
| 8031c100 | 2478ffff | layer in a0, channel pointer at +50, player pointer at channel +40 |
| 8031d130 | 246dffff | channel in s2, player pointer at +40 |
| 8031dc54 | 2498ffff | sequence player in s1 |

the CPU executes these original instructions throughout. the extension changes
only their result when all of the following hold: the music, timer and countdown
signatures match; the exact PC and original instruction match; the game timer
is in its burst half-cycle; and ownership resolves to music player 0 at
80222618 or music player 1 at 80222758. invalid pointers, other instructions,
countdowns of zero/one, and sound-effect player 2 at 80222898 retain normal
ADDIU behavior. channel/layer pointers are read afresh at each execution so
allocation reuse does not carry music acceleration into a sound effect.

`B0222618 0001` enables the first variation and `B0222618 0002` enables the
second, behind the generated signature and burst-phase conditions. 222618 is
a control selector identifying the verified US player layout, not a RAM write.
the operation is cleared on every VI before evaluating codes, on disabling,
and on state restore. the instruction boundary checks the phase again.
both interpreter and cached CPU dispatch use the same operation.

each generated variation uses 779 operations per VI. use only one delay/burst
preset at once; the control selector participates in overlap checking. do not
remove the conditions from the import file.

### existing codes and states

exact older generated faster/extreme programs upgrade automatically when parsed,
including raw programs and saved lists. custom codes and checkbox selections
are retained. loading an older state with tracked shared instruction patches
restores those original bytes through the existing undo journal before the new
program runs. disabling restores owned tempo values and clears acceleration
without restarting the ROM. already advanced music position is not rewound.

a state with untracked or separately modified audio instructions cannot be
repaired by guessing: the guards refuse acceleration until the original audio
code is restored. preserve the original state when recovering it.

### regression validation

validation uses the verified extended US ROM, isolated copies of the main test
state and castle slot 6, normal sound output, and visible native test windows.
checks cover untouched guest instructions, normal SFX countdown results, music
acceleration only in bursts, phase lengths, disabling, legacy program upgrades,
selection preservation, and both CPU dispatch modes. sequencer event counts are
execution evidence, not a listening-quality guarantee for every music/SFX mix.

castle slot 6 replayed for 1100 VI per preset with identical scripted jump
inputs. music note events were 187 for the original, 1961 for faster, and
3441 for extreme; all three produced 57 SFX note events. each replay checked
3270 SFX countdown executions against the normal result with no differences.
all runs had 512 stalled and 588 burst VI. faster used the interpreter and
extreme used the cached CPU. an additional cached extreme replay from the main
state completed 1100 VI with 4517 music events and 6258 unchanged SFX countdowns.
test windows were visibly verified on the interactive desktop and closed afterward.
