# GLN64j compatibility fixes

## peach to bowser portrait transition

gln64j and d3d11 now support the paired-texture lod blend used by the castle
hallway portrait. its two-cycle material uses `G_CC_TRILERP`, `G_CC_DECALRGB2`,
`G_TL_LOD`, and two 32 by 32 texture tiles. the changing texel footprint makes
peach visible far away, blends the two images during approach, and reveals
bowser nearby. previously the missing `LOD_FRACTION` input left bowser visible
at every distance.

gln64j uses a small GLSL 1.20 shader for this combiner equation while retaining
its legacy combiners for other materials. it matches the equation, not a game
name or a display-list address. the shader requires OpenGL shader support;
older contexts retain the previous fixed-function behavior. d3d11 calculates
the fraction in its existing HLSL combiner. both reverse texture-coordinate
normalization and tile shifts, and convert derivatives to native n64 pixel
units so host resolution does not change the blend.

the extended usa sm64 slot 2 castle scene was replayed with Fast3D SW 2.0D,
64 mib rdram and identical scripted approach input in each renderer. captures
show peach, the intermediate blend, and bowser. at the matching distant capture,
only portrait pixels differ from each renderer baseline. the original states
were preserved, visible test windows had no console, and each closed afterward.

this covers one paired mip interval with maximum level one. longer mip chains,
detail/sharpen modes, and bit-exact lod quantization remain unsupported. this
scene verification does not establish full-game or hardware accuracy.

the material is defined in the matching
[sm64 display list](https://github.com/n64decomp/sm64/blob/9921382a68bb0c865e5e45eb594d9c64db59b1af/levels/castle_inside/areas/1/1/model.inc.c).

## earlier compatibility fixes

The October 2026 compatibility update fixes several shared rendering paths:

- Depth-image fills now clear the depth buffer before any color rendering. Full-screen fills are no longer discarded based on their color, and color rectangles are drawn once.
- Fill-cycle color writes bypass alpha blending, including packed fill colors with zero alpha.
- The guest viewport is applied and restored after solid and textured rectangles, including smaller viewports used for split-screen rendering.
- Fog follows the guest geometry flag, fog color, and per-vertex depth factors. Guest face-culling changes are applied again.
- Fog for vertices behind the near clipping plane uses near-plane depth. Previously, behind-camera vertices were assigned maximum fog, which spread a gray haze across visible foreground terrain when their triangles were clipped.
- Combiner cache entries distinguish one-cycle and two-cycle operation. Switching combiners refreshes texture bindings so a newly used texture unit does not retain a dummy texture.

## Observed coverage

Mario Kart 64 (USA), using F3DEX/F3DLX 0.95 and GLN64j on Windows x64, was replayed from a local savestate. Framebuffer captures at 60, 120, and 240 subsequent VI events show track surfaces, walls, grass, and the player kart restored compared with the previous mostly black race scene. Saving a new state and loading it again also succeeded. These captures cover an attract-mode race, not a complete playthrough or multiplayer validation.

Local command-level regressions cover depth/color fills, viewport restoration, fog, culling, and combiner caching. They reproduce failures in the original renderer and pass with the fixes.

An additional fog correction was verified with a USA-based extended Super Mario 64 ROM using Fast3D SW 2.0D and an 8 MiB RDRAM savestate in Bob-omb Battlefield. Replaying the same state before and after the fix at 60, 120, and 240 VI events removes the erroneous gray haze from the foreground grass and hillside while retaining the scene's configured distance fog. The original state and a newly saved state both load successfully. Near-plane and zero-W fog cases are covered by local command-level regressions. This is scene-specific testing, not a complete playthrough or hardware comparison.

Zelda and other game compatibility remain unverified. These fixes address common renderer behavior; they do not establish compatibility with every game or resolve all framebuffer and microcode limitations. Direct3D 11 gameplay was not retested in this update.

Viewport restoration and near-plane fog handling were checked against the [glN64-derived Wii64 implementation](https://github.com/emukidid/Wii64/blob/master/glN64_GX/OpenGL.cpp). Cycle-dependent combiner behavior is described in the [N64 combine-mode reference](https://ultra64.ca/files/documentation/online-manuals/functions_reference_manual_2.0i/gdp/gDPSetCombineMode.html).

## Performance and Mario Kart audio follow-up

The host VI limiter now keeps a stable deadline instead of accumulating scheduler overshoot. A normal-speed Mario Kart 64 savestate replay measured about 58 VI/s before this change and 60 VI/s afterward. Catch-up is bounded to one VI after a slow frame. This corrects pacing drift; expensive scenes and host scheduling can still cause individual drops.

Triangle submission avoids temporary wrapper arrays. The D3D11 bridge reuses vertex and command storage, and its native draw path retains vertex scratch memory. The Mario Kart replay's Java allocations fell from roughly 329 KB to 212 KB per VI. D3D11 avoids duplicate presentation of unchanged content, skips framebuffer feedback when the shader does not depend on it, and resolves multisample feedback directly into the shader input. Required framebuffer blending and coverage feedback remain enabled.

Mario Kart 64's ABI2 audio resampler now converts the command pitch to the correct fixed-point scale, uses the Java buffers' N64 sample order, and saves all four history samples between commands. This was checked against the [Mupen64Plus NEAD resample command](https://github.com/mupen64plus/mupen64plus-rsp-hle/blob/master/src/alist_nead.c) and [shared resampler state handling](https://github.com/mupen64plus/mupen64plus-rsp-hle/blob/master/src/alist.c). A split-stream regression fails on the previous implementation and passes with the correction. These changes address incorrect pitch and block discontinuities; they do not establish complete audio accuracy or equality with Super Mario 64.

The follow-up used Mario Kart 64 savestate replays with both renderers, including new save/load round trips. D3D11 captures at 60, 120, and 240 VI events remained pixel-identical to the previous release in the tested race. A separate Super Mario 64 D3D11 saved-scene replay with F4 targeting 546 VI/s improved from about 71 to 120 VI/s after the feedback changes. This is a local scene measurement, not a general benchmark. F4 throughput is still limited by the CPU and rendering work; selecting a high target does not guarantee that rate. Other games remain untested.

ROMs, savestates, diagnostic audio/captures, and local regression tooling stay outside the source checkout and release package. The explicitly selected gameplay screenshots in `assets/screenshots` are public documentation assets.


## d3d11 maximized windows and captured previews

d3d11 now keeps the configured render width and height when the window is
maximized, scaling the output to the window. this avoids multiplying MSAA,
framebuffer feedback and readback work just to enlarge the window. the default
640 x 480 render size favors performance. enable **render at window resolution
(slower)** in the d3d11 settings for the previous full window resolution behavior.
render resolution changes apply during emulation.

in a local hardware-device sm64 main-state replay with frame limiting disabled,
120 VI events took about 1.45 seconds in the small window but 4.40 seconds
maximized before this change. after the change, the maximized replay took about
1.47 seconds, or 82 VI/s. this comparison uses a lower internal render resolution
and does not establish performance for every game or device.

captures now read the last presented resolved image instead of selecting a
possibly cleared or unfinished render target. a savestate load previously made
an immediate capture entirely black; the same check now preserves the displayed
image pixel for pixel until a new frame is presented. before the first presented
frame, capture reports no image instead of inventing a black gameplay frame.
