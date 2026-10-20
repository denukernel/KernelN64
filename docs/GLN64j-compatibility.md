# GLN64j compatibility fixes

The October 2026 compatibility update fixes several shared rendering paths:

- Depth-image fills now clear the depth buffer before any color rendering. Full-screen fills are no longer discarded based on their color, and color rectangles are drawn once.
- Fill-cycle color writes bypass alpha blending, including packed fill colors with zero alpha.
- The guest viewport is applied and restored after solid and textured rectangles, including smaller viewports used for split-screen rendering.
- Fog follows the guest geometry flag, fog color, and per-vertex depth factors. Guest face-culling changes are applied again.
- Combiner cache entries distinguish one-cycle and two-cycle operation. Switching combiners refreshes texture bindings so a newly used texture unit does not retain a dummy texture.

## Observed coverage

Mario Kart 64 (USA), using F3DEX/F3DLX 0.95 and GLN64j on Windows x64, was replayed from a local savestate. Framebuffer captures at 60, 120, and 240 subsequent VI events show track surfaces, walls, grass, and the player kart restored compared with the previous mostly black race scene. Saving a new state and loading it again also succeeded. These captures cover an attract-mode race, not a complete playthrough or multiplayer validation.

Local command-level regressions cover depth/color fills, viewport restoration, fog, culling, and combiner caching. They reproduce failures in the original renderer and pass with the fixes.

Zelda and other game compatibility remain unverified. These fixes address common renderer behavior; they do not establish compatibility with every game or resolve all framebuffer and microcode limitations. Direct3D 11 gameplay was not retested in this update.

The viewport restoration was checked against the [glN64-derived Wii64 implementation](https://github.com/emukidid/Wii64/blob/master/glN64_GX/OpenGL.cpp). Cycle-dependent combiner behavior is described in the [N64 combine-mode reference](https://ultra64.ca/files/documentation/online-manuals/functions_reference_manual_2.0i/gdp/gDPSetCombineMode.html).

ROMs, savestates, framebuffer captures, and local regression tooling are kept out of the source checkout and release package.
