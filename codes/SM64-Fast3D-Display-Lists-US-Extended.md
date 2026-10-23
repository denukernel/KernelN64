# fast3d display list gameshark presets

find four disabled presets under GFX > Display lists > Bob-omb Battlefield:

- texture scale one sixty-fourth: BB command coordinate scales become 03ff/03ff.
- texture scale one eighth: BB command coordinate scales become 1fff/1fff.
- texture scale U stretched: BB command scales become 1fff/ffff.
- shade-only color combiners: four FC commands become FCFFFFFF FFFE793C.

these write actual guest display list halfwords using 81 writes and D1 guards.
the scale presets change six executed gSPTexture commands; the shade preset
replaces four executed gDPSetCombine commands. they do not change host renderer
settings, mario state, CPU instructions, command pointers or control flow.
choose one scale preset. the shade preset can be selected separately, although
shade-only materials do not use the sampled texture color.

## verified target and derivation

verified on the sm64 us extended ROM with SHA-256
5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0,
the required main test savestate, bob-omb battlefield (level 9), and Kernel64GL.
the runtime identified original Fast3D, type 0. segment 7 physical base was
000FAF90. addresses below are physical rdram offsets, not ROM offsets.
the gameshark code encodes these as 81 writes at CPU KSEG0 aliases.

| command | physical addresses | original words | replacement |
| --- | --- | --- | --- |
| gSPTexture | 000FF368, 00104D60, 00105420, 00105900, 00108CF0, 00109310 | BB000001 FFFFFFFF | retain BB000001, change scale word |
| gDPSetCombine | 000FF358, 00104D48, 00108CE0, 00109300 | FC127FFF FFFFF838 | FCFFFFFF FFFE793C |

all sites were recorded at command dispatch while rendering the scene.
F3D_Texture in src/main/java/kernel64gl/engine/opcodes/F3d.java decodes the
scale halves as unsigned 0.16 values. the shade command was also observed in
the material restore lists. its encoding agrees with G_CC_SHADE in
n64decomp/sm64 commit 9921382a68bb0c865e5e45eb594d9c64db59b1af,
include/PR/gbi.h. no address was inferred from a ROM offset or a global delta.

## activation and boundaries

enter bob-omb battlefield, then enable a preset. each write checks level 9 and
immutable material words at 000FF350, 000FF360 and 000FF370. these anchor commands
are excluded from all preset writes. the codes stay inactive when those guards
fail. only this captured allocation/material set is established; other ROMs,
regions, relocated builds and other courses are not verified. matched decomp
builds omit these presets when their asset addresses cannot be mapped.

KernelN64 uses chained conditions for each write and recognizes this guard prefix
for undo lifetime. disabling restores owned bytes while the guarded allocation
survives. a scene mismatch discards stale ownership instead of restoring bytes
into reused memory. game or DMA writes can replace undo baselines. load a clean
state with the codes disabled if restoration is uncertain. other emulators and
physical gamesharks are untested.

## verification

runtime tracing confirmed the changed BB/FC command words reached the renderer.
framebuffer captures showed enlarged/stretched textures and shade-only terrain.
all four presets passed undo checks. synthetic memory checks covered each guard,
scene mismatch ownership, migration, duplicate-free reload and disabled exports.
test saves, ROM data, traces and captures are excluded from this catalog and the
portable release.
