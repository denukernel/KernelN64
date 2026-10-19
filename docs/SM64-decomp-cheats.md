# SM64 US decomp and lightly modified ROMs

Keep the linked build ELF beside its ROM. `sm64.us.z64` uses `sm64.us.elf`;
`MyHack.z64` uses `MyHack.elf` (or `MyHack.z64.elf`). The matching ELF is discovered automatically. No changes to the decomp source, ROM or build are required.
A map file alone is insufficient: the ELF supplies symbol sizes and the matching
machine code. Linked big-endian MIPS ELF32 with unstripped symbols is required.

The Cheats window says **SM64 decomp / matched ELF**. Presets use this build's
addresses. Audio-reader guards use this build's instruction values, too.
Existing unchanged stock presets migrate when loaded; names, groups and enabled
states remain. Untagged custom codes or codes saved for a different ROM build
start disabled for address review. Converted and generated entries carry a ROM
hash so loading the file again cannot shift them twice.

To use an internet code, choose **Add** or **Edit** and paste the US retail
code normally. The paste handler converts the complete block before inserting
it, using the matching ELF. The editor displays native build addresses. Copy/cut
still work, and changing a value does not convert the displayed addresses again.
Choose **Paste addresses: This build** before pasting already converted codes.
Manually typed addresses are native; automatic conversion applies to pasted
complete code lines. Unsupported pastes leave the existing editor text intact
and show the reason below the paste selector.

File import automatically converts untagged retail entries as a batch and keeps
all imported codes disabled. Entries already tagged for this ROM remain native;
a file marked for a different build is rejected. The import chooser also has a
**File already uses this build's addresses** option for untagged native files.
Supported legacy aliases and XP64 encodings normalize through the existing code
parser before relocation. Known data/function ranges are supported; arbitrary
RAM, injected code, pointer values and hooks are not inferred.

Audio entries can be combined: up to **4 effects** and **65,536 code operations
per VI** across their enabled programs, counting condition checks and expanded
repeat writes. Entries belong to Audio when their name starts `Audio\` or their
group is `sound`; use that category for custom audio codes. Conditions are not
writes. Potential writes from repeaters, button and boot codes are included in
conflict checks. Supported overlapping SM64 pitch tables now multiply their relative changes
per note: `original[i] * product(effect[i] / original[i])`. Bass plus detuning
therefore retains both the octave drop and the tuning shift. Bass plus helium
cancels. Full guarded note tables, including the rake presets, are recognized
from their actual code and reader guards rather than their names. The rake fast
preset retains its separate tempo program when its pitch part joins the mix.

The runtime emits one guarded table for the mix. Saved entries, original codes,
checkboxes and native-address markers stay unchanged. Reopening the file does
not multiply an already mixed table again. Disabling a component recompiles the
remaining effects; savestate undo records retain the composed program ownership.
Products use a deterministic order and clamp resulting positive frequencies to
`1/4096 .. 1024` to avoid zero, nonfinite values and unbounded growth.

Other overlapping writes must still agree. Partial tables, altered guards, raw
instruction patches and competing absolute tempo locks do not have a general
composition rule. Conditions are treated conservatively even if two programs
might run at different times. The four-effect limit counts the selected source
entries; the operation budget checks both their original programs and the
compiled mix.
A rejected selection leaves the previous checks unchanged and explains the
conflict/limit in the status line. Compatible selections persist together.
Disabling an effect retains the other effects and restores its owned bytes
subject to the usual guest-write/overlay lifetime rules.

## Compatibility boundaries

This targets US-derived builds retaining the usual MarioState, controller,
save-buffer and audio structures. Symbol sizes must match the supported layout.
Data addresses resolve individually; a whole-ROM address delta is never used.
File-backed CPU segments, including the Goddard overlay, must agree byte for byte
between ELF and ROM. Stale ELF files prevent automatic cheat activation.

Instruction presets additionally require the expected function size and a
normalized instruction fingerprint. Normalization accounts for absolute address
materialization and J/JAL destinations; branch distances, register choices and
ordinary structure offsets remain significant. A changed function omits the
dependent preset. These checks are deliberately conservative, and are not a
semantic proof for an engine overhaul or a different compiler. Same-sized custom
structures with changed field meanings still require a new port. PAL/Japanese
builds and substantially overhauled engines are outside this support.

The ROM header alone does not establish a recompiled RAM layout. A recognized
SM64 image with a different startup layout requires its matching ELF before US
presets are selected automatically. Ordinary US retail and compatible extended
ROMs keep their existing behavior.
