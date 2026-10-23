# lua scripting

open a game, then choose **tools > lua scripting**. add one or more `.lua` files,
or create scripts in the editor, then choose **run all / restart**. the list shows
each script and its status. selecting a script opens its editor, output and
errors. **clear selected log** affects only that script.

scripts have independent lua globals, callbacks and execution budgets. a script
error or `emu.stop()` stops only that script; the other scripts keep running.
**stop all** releases every script controller override and temporary memory patch. closing the editor keeps
scripts running, and reopening it restores the workspace. ending or reopening a
rom stops every script. files and groups never run automatically.

## saved script groups

**save group** stores the current script names, source text and list order in a
`.luagroup` file, including unsaved editor text. **open group** stops the current
scripts and restores the saved list without executing it. **run saved group**
loads the selected group and starts its scripts together. **run all / restart**
restarts the current list together. groups do not store roms, savestates or file
paths, and must be explicitly opened or run.

up to 32 scripts can run together. each source is limited to 256k characters;
a saved group is limited to 1m source characters and 8 mib on disk. finished
script logs are bounded and retained for the most recent 64 sessions. these
limits do not grant scripts filesystem access.

callbacks, guest memory writes and controller overrides follow start order.
for overlapping controller fields, later scripts win; stopping one script
releases only its fields, allowing earlier scripts to control them again.
use compatible scripts together: guest ram and emulator pause state are shared.

LuaJ 3.0.1 provides lua 5.2-style syntax, tables, strings, math and `bit32`.
this emulator API uses callbacks rather than `emu.frameadvance()` loops from
other emulators. host files, process execution, Java access, dynamic loading
and Lua coroutines are unavailable to scripts. file open/save belongs to the
editor; guest save slots use the explicit API below.

## frame and lifecycle callbacks

```lua
print(emu.romname(), emu.romhash())
event.onframe(function(frame)
    if frame % 60 == 0 then print("vi", frame) end
end)
event.onloadstate(function() print("state loaded") end)
event.onstop(function() print("stopped") end)
```

one callback per event type per script is supported; registering another replaces
that script callback.
pass `nil` to unregister. frame callbacks run at a CPU boundary after the
observed video interrupt, not necessarily once per rendered game frame.
pausing suspends frame callbacks. loading a state queues `onloadstate` and
clears controller overrides; lua globals are host state and are not rewound.
callbacks and memory writes execute on the emulation thread.

`emu.framecount()`, `emu.romname()`, `emu.romhash()` and `emu.ram_size()` identify
the current guest. `emu.getregister("pc")` also accepts `hi`, `lo` and `r0` through
`r31`, returning their low 32 bits as unsigned numbers. `emu.pause()` pauses the
game; use f2 or run a script calling `emu.resume()` to continue. `emu.stop()` ends
only the calling script. scripts can be started while emulation is paused.

## memory

* `memory.read_u8/u16/u32(address)` and `read_s8/s16/s32(address)`
* `memory.write_u8/u16/u32(address, value)`
* `memory.read_float(address)` and `write_float(address, value)`
* `memory.read_bytes(address, count)` returns a 1-based table, up to 4096 bytes
* `memory.patch_u8/u16/u32(address, value)` and `patch_float(address, value)`
* `memory.read_original_float(address)` reads the value beneath temporary patches

reads and writes are big-endian. use physical RDRAM addresses or directly mapped
KSEG0/KSEG1 addresses such as `0x80000000` and `0xA0000000`. devices and arbitrary
TLB translations are excluded. accesses must fit entirely in configured RDRAM.
integer writes accept signed or unsigned values that fit the access width.
code writes invalidate the interpreter cache. framebuffer accesses use the
existing graphics coherency hooks.

use `patch_*` for temporary effects. stopping, restarting or failing a script
releases its patches, even if its `onstop` callback fails. overlapping patches
are tracked per script: the last write wins, and removing one owner reveals the
remaining owner or original value. a newer game write is preserved instead
of restoring stale data. overlapping writes support mixed access widths, while
preserving complete game-written values. total tracked memory is limited to
64 kib. loading a state discards old
patch ownership before callbacks run. `read_original_float` lets audio effects
start from the underlying pitch table instead of another effect.

`write_*` now uses the same automatic cleanup as `patch_*`, including older
saved source and script groups. no script edits or source reload are required.
legacy `onstop` writes to owned bytes defer to this cleanup so they cannot bring
back a stopped overlapping effect. stop all invokes legacy callbacks in reverse
start order. stopping cannot undo game events,
positions or audio already queued as a consequence of a script.

these APIs work with sm64 and other games; game-specific RAM addresses depend
on the exact rom revision or mod. the bundled frame-watch example only reads
the first RDRAM word, without guessing any game layout.

## input and state slots

```lua
joypad.set({A=true, x=0, y=80}, 1)
print(joypad.get(1).A)
joypad.clear(1)
savestate.save(3)
-- savestate.load(3)
```

ports are 1-4. named buttons are `A`, `B`, `Z`, `Start`, `L`, `R`, `Up`, `Down`,
`Left`, `Right`, `CUp`, `CDown`, `CLeft`, `CRight`. axes `x` and `y` accept -128 to
127. a set replaces the previous override; unspecified controls retain physical
input. overrides persist until changed, cleared, state-loaded or script-stopped.
`joypad.get` returns composed input. raw controller-command plugins bypass this
standard libultra input path.

state slots are 1-10 and use the same readable per-rom folders as the menus.
save replaces that slot. at most two slot operations are allowed per callback.
loading a slot does not restart the lua VM. avoid unconditional state loads in
`onloadstate` if forward guest progress is desired.

## limits and examples

each initialization/callback has a 100000-lua-instruction budget and a 150 ms
execution deadline checked between VM instructions. save/load disk time is
excluded from that deadline. the stop button sets an instruction-hook flag,
so an infinite lua loop does not need another CPU command to stop. budget aborts
cannot be caught by lua `pcall`. an error or overrun stops only the script.
these checks bound VM execution, not every individual native library operation.

output is limited to 64 print calls per callback, 1024 characters per line and
500 retained entries. source is limited to 256k characters. the bundled runtime
is configured for emulator scripts rather than unrestricted host automation.

see `lua/examples/frame-watch.lua` and `lua/examples/input-pulse.lua`.
LuaJ source and API: https://github.com/luaj/luaj and
https://luaj.org/luaj/3.0/api/ . license: `licenses/LuaJ.txt`.

## sm64 presets

open a preset from `lua/examples` in **tools > lua scripting**, restore the main
test state, and select **run / restart**. run one preset at a time, with other
cheats disabled. use **stop** before trying the next preset. load the main state
to reset gameplay effects. these scripts never save or replace a state slot.

| file | effect and controls |
| --- | --- |
| `sm64-audio-wobble.lua` | oscillating pitch for newly played notes, within 2.5 semitones |
| `sm64-audio-broken-scale.lua` | alternating bass/fifth with a crooked repeating scale |
| `sm64-music-tempo.lua` | current music cycles 72, 120, 180 and 240 bpm, every 240 VI callbacks |
| `sm64-gfx-squash.lua` | cycles the guest squash/stretch model animation; movement and jumping are also affected |
| `sm64-blj-assist.lua` | manually start a long jump, then hold L + Z and steer backward; pulses A, preserving your stick and camera controls |

release L or Z to disengage the blj assist. it activates only during long-jump,
long-jump landing and landing-stop actions. it does not force speed or teleport;
positioning, stairs or a low ceiling are still required. its A cadence targets
this 30 fps US game running at 60 VI/s. stopping or loading a state clears input.

music presets modify pitch or tempo of the current song, not its sequence id.
tempo leaves the sound-effect player alone; pitch-table changes can also alter
pitched sound effects. held notes can finish at their previous pitch. percussion
can remain unchanged. do not mix these with pitch or tempo cheats.

audio presets use temporary patches so overlapping effects release correctly,
preserving newer game writes. state loads discard old ownership and recapture data.
the visual preset lets the guest finish unsquashing after stop. none of these
scripts writes ROM, pointers, executable instructions or save data.

these presets deliberately accept only the verified US extended main-test ROM
(SHA-256 `5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0`).
a filename or regional title is insufficient evidence for compatible RAM offsets.
other builds need their own verified addresses and guard before enabling writes.

layout evidence: the matching US audio readers already used by the built-in
presets locate `gNoteFrequencies` at `0x80333994` and `gSequencePlayers` at
`0x80222618`, stride `0x140`, tempo offset `0x0a` (BPM times 48). `gMarioStates`
is at `0x8033b170`; action is offset `0x0c`, object pointer `0x88`, and squish timer
`0xb4`. types, actions and consumers are documented in the pinned
[n64decomp source](https://github.com/n64decomp/sm64/tree/9921382a68bb0c865e5e45eb594d9c64db59b1af):
`include/types.h`, `include/sm64.h`, `src/game/mario.c`,
`src/game/mario_actions_airborne.c`, `src/audio/data.c` and `src/audio/seqplayer.c`.

verification used an isolated copy of the required main test state: 180 VI for
each pitch, visual and idle blj preset, plus 780 VI for all four tempo stages.
nonzero synthesized audio changed for both pitch effects and music tempo. pitch
bytes restored on stop; every preset survived a main-state reload. framebuffer
inspection confirmed the squash effect. blj activation, A press/release cadence,
idle release and unchanged speed were checked while paused against the loaded
state; an actual staircase blj completion has not been verified. audio evidence
is synthesized PCM capture, not a subjective listening comparison.


legacy cleanup verification used unchanged older pitch scripts and an older
saved group from the main sm64 test state. overlapping pitch effects restored
all 128 frequency entries, including when one script stopped first. raw writes
without an onstop callback, script failures, failed onstop callbacks and budget
aborts also restored their changes. loading a state discarded earlier ownership;
newer guest-written values survived cleanup. these checks did not require a ROM
restart or replacement of the older script source.
