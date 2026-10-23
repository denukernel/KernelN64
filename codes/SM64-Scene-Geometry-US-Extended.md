# scene vertex, triangle, lighting and geometry codes

24 disabled presets are bundled under GFX > Display lists for:

- bob-omb battlefield
- castle interior
- the secret aquarium

these complement the four earlier bob-omb battlefield texture/combiner presets.
within each scene there are eight choices:

| category | presets | guest target |
| --- | --- | --- |
| vertices | raised; height doubled | up to 64 signed Y coordinates in 16-byte N64 vertices referenced by executed 04 vertex-load commands |
| triangles | winding reversed; batch removed | up to 32 BF triangle commands; swapped valid indices or zero-area triangles |
| lighting | red; blue | RGB and duplicated RGB in executed 03 light-load data; directions and padding preserved |
| geometry | two-sided; flat shading | B6/B7 masks for original Fast3D culling (0x3000) and smooth shading (0x200) |

sampling is spread across captured batches. effects are visual: collision and
level scripts stay unchanged. later state commands can override geometry flags,
and occluded samples may have no obvious effect from a particular camera.
choose one variant in each category. do not combine captures with unrelated
custom writes to the same data.

## build codes for another level

1. use Kernel64GL with a supported sm64 us layout and original Fast3D microcode.
2. enter the level, aim the camera at the desired scene and resume gameplay.
3. disable enabled GFX entries. open the cheats window and choose Build GFX Codes.
4. the new entries appear disabled under GFX > Captured scenes and are saved.

capture observes actual dispatched commands for about 0.4 seconds. it records
only bounded level-segment commands, validates them again after capture and
uses their real segmented vertex/light pointers. it does not scan arbitrary
RAM for bytes resembling opcodes. up to 2,048 distinct commands are retained.
non-Fast3D dialects, missing vertex anchors and changing scenes are rejected.
other original-Fast3D levels can be captured without inventing static addresses;
only the three bundled scenes have been exercised in this test session.
capture labels distinguish different observed command sets at the same allocation.

## guards and restoration

these are 81 halfword writes with D1 guards. every write checks the current
level and two immutable executed vertex-load commands, including their pointers.
the repeated level guard identifies this generated family for allocation-aware
undo. the emulator restores owned bytes when disabling a code while its guarded
allocation survives. a guard mismatch discards stale ownership instead of
restoring bytes into a different scene. game/DMA writes can replace baselines.
load a clean state with the codes disabled if restoration is uncertain.

chained conditions and allocation-aware undo require KernelN64. other engines,
physical gamesharks, other regions and relocated static asset allocations are
untested. bundled addresses are established for sm64 us extended SHA-256
5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0.
matched decomp builds omit static presets whose asset addresses cannot be mapped.

## evidence and checks

source contract: kernel64gl/engine/opcodes/F3d.java (F3D_Vtx, F3D_Tri1,
F3D_MoveMem, set/clear geometry mode), Gsp.gSPLight and the original Fast3D
constants in kernel64gl/engine/core/Gbi.java. source reference is n64decomp/sm64
commit 9921382a68bb0c865e5e45eb594d9c64db59b1af, include/PR/gbi.h.
all target addresses come from executed command capture and segmented pointer
resolution, not ROM offsets or a whole-ROM delta.

runtime started from an isolated copy of the required main savestate. the game
exit-course menu reached castle interior. the aquarium was reached using its
actual warp object after positioning mario at the source-defined warp location
(1963, 819, 1280); no alternate savestate was substituted. all 24 effects were
exercised with the native emulator visible. captures showed terrain deformation,
triangle holes and lighting tints. synthetic checks verified every generated
write byte, every guard, full undo, scene-reuse ownership, catalog migration and
reload. a live cheats-window check verified that Build GFX Codes saves eight
disabled entries. test windows were closed afterward.

ROMs, states, baseline data, traces and test captures are excluded from the
bundled catalog, source sync and portable archive.
