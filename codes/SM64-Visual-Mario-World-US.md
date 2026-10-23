# sm64 graphics, mario, animation and world codes

30 disabled presets are available in GFX, Animations, Mario and World in the cheats window. existing named cheat files receive them on first load. the importable companion file is SM64-Visual-Mario-World-US.cheats.txt.

these target sm64 us v1.0 and compatible us memory layouts. matching decomp elf files relocate only supported symbols. other regions are unsupported.

- gfx: four held squash phases, including the flat model.
- animations: two F9 squash recovery triggers and one repeating recovery. these use the guest scale animation, not skeletal animation replacement.
- mario: three forward speed freezes, an F9 upward velocity trigger and a quicksand depth freeze.
- world: 18 level destinations using the existing hold L transition procedure. enter a painting, press A at star select and hold L during the fade. these redirect level transitions rather than changing geometry.

choose one squash mode, one speed and one destination. world entries share the existing level selector group. disable competing custom codes manually. squish state affects movement and jumping. game updates can change the visible result between cheat writes.

source evidence: n64decomp/sm64 commit 9921382a68bb0c865e5e45eb594d9c64db59b1af, include/types.h MarioState offsets and src/game/mario.c squish_mario_model; src/game/game_init.c gGlobalTimer. the local verified us layout places gMarioStates at 8033B170 and gGlobalTimer at 8032D5D4. destinations reuse the existing documented project64 us level selector and IDs.

verification: all 30 programs parsed and checked for bounded writes; button gating, float encoding, undo and migration/reload passed synthetic memory checks. gameplay runtime testing remains pending. source and synthetic checks do not establish behavior on physical gamesharks or other emulators.

code writes can outlive a button press. disabling releases owned bytes, subject to newer guest writes. load a clean state if the action persists. back up saves before experimenting.
