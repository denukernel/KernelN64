# Kernel64GL

Kernel64GL is an independent java opengl renderer. it owns its display-list decoder,
Fast3D-family handlers, RSP/RDP state, textures, draw buffers and shader combiner.
it does not inherit the gln64j plugin, call its renderer, or alias its renderer
objects. the engine started as a source fork of the existing gln64j/glN64-derived
implementation; inherited copyright and license headers remain in place.

the GLSL combiner evaluates one or two color/alpha cycles on the gpu instead of
reducing the equations to legacy fixed-function texture stages. JOGL/GlueGen and
the emulator CPU, audio and guest memory remain common host infrastructure.
OpenGL 2.0 / GLSL 1.20 support is required. unsupported shader compilation is
reported as an error rather than silently selecting the old renderer.

## settings

select **Kernel64GL (java opengl)** in **settings > settings > plugins**, apply,
then configure the active graphics plugin. resolution, window locking,
always-on-top, fog, filtering, wireframe and normal-speed vsync remain available.
settings live in `Kernel64GL.config`. save applies drawing options on the next
rendered frame and window options immediately. resume if paused. legacy plugin
settings and files stay untouched.
disabling bilinear filtering forces nearest sampling; enabling it allows bilinear
sampling when requested by the guest. vsync is bypassed during f4 or shift turbo.

## rendering and performance changes

- up to 512 compatible triangles fit in one submission, compared with the previous
  85-triangle limit; guest state changes still flush batches in order
- repeated polygon and depth-test states avoid redundant driver calls
- a bounded cache reuses expanded combiner selectors; unchanged fog uniforms are
  not uploaded again
- texture upload buffer views are retained per upload instead of created per texel
- the vertex payload drops from 17 to 13 floats by removing unused secondary
  color attributes, reducing each uploaded vertex by about 24 percent
- RGBA5551-to-RGBA8888, IA88-to-RGBA4444 and RGBA8888-to-RGBA4444 conversions now
  decode channels rather than returning placeholder colors
- the second texture unit uses its own horizontal tile origin; texture-filter
  changes invalidate texture state and apply to the correct bound texture
- packed combiner words preserve the unsigned low word instead of sign-extending
  it over the high word
- a vertex load ending at the final cache slot is accepted, with negative and
  overflowing ranges rejected

these changes reduce renderer overhead and repair specific compatibility paths.
they do not provide full RDP accuracy: framebuffer feedback, coverage, LOD,
noise and uncommon microcodes still have limitations. no new game is promoted
to tested status by this work.

## state compatibility and local verification

the host selects the active backend when saving/restoring graphics fields.
compatible gln64j/d3d11 states are copied into Kernel64GL-owned objects; they do
not share those objects. screenshots and rewind previews use the active context,
with the shader suspended and restored around preview drawing. a pixel-identical
preview round trip and restored shader binding passed in the main sm64 scene.

sm64 checks start from an isolated copy of the required main test state, using the
matching us-based extended rom and 64 mib rdram. mario kart 64 usa checks use its
race state with 8 mib rdram. both use azimer HLE audio. replays check captures at
60, 120 and 240 vi events and a new save/load round trip. live switching among
Kernel64GL, gln64j and d3d11 is also checked. the original sm64 state is preserved.

real offscreen gpu checks compare one- and two-cycle shader output with known
texture, shade, primitive and environment colors, including alpha and cycle
changes. command-level checks cover fills, clipping fog, viewports, culling and
batch limits. texture conversion and independent settings checks are separate.

representative short uncapped replays measured about 186 versus 194 vi/s in the
sm64 scene. mario kart runs varied; a back-to-back control measured about 139
vi/s with both builds, so no mario kart speedup is established. these short
measurements are not a general speed guarantee. the batch regression shows
512 compatible triangles submitted in one call instead of six; actual game speed
can remain limited by CPU emulation and scene behavior. no full playthrough,
multiplayer or additional game coverage is claimed.

roms, states, captures, settings and local regression tooling remain outside
source sync and release archives.

## corrupted texture read protection

a corrupted sm64 castle state reproduced a host `IndexOutOfBoundsException` in
the RGBA32 texel reader before the first 60-vi capture. the tile coordinates
requested a complete texel beyond the sliced texture buffer. both RGBA32 output
depths now validate the full four-byte read using overflow-safe arithmetic and
return a transparent texel for invalid coordinates. valid reads keep their
existing conversion behavior. this is a defensive fallback, not a claim of
hardware-accurate behavior for corrupted commands.

the supplied corrupted state then completed 240 vi events and a save/load round
trip without that exception. corrupted graphics remain visible. the normal sm64
main test state also completed the same replay. targeted checks cover valid
reads, odd-row addressing, nonzero buffer positions, truncated and empty buffers,
negative coordinates and integer overflow at both output depths.

a later corrupted rom reported `arraycopy: length -408 is negative` in
`TextureTransfers.loadTile`. tile loads now reject reversed dimensions and
invalid coordinates, sizes, addresses or strides before copying. validation
covers each strided source row, destination row and odd-row interleave footprint,
using wide arithmetic. rejected transfers leave texture memory unchanged.

a focused regression reproduces the exact negative-length exception with the
old code and passes with the validation. additional checks cover truncated
strided ranges, address and dimension overflow, odd-row interleaving, valid
16/32-bit transfers and exact buffer-end transfers. the supplied slot 1 and 2
states did not reproduce the original exception during short idle replays, so
the report and focused regression establish the failure rather than a complete
runtime reproduction of the original input sequence.

the updated castle-entry slot 2 reproduced a nonterminating display list in
`Gsp.RSP_ProcessDList`: the graphics thread repeatedly decoded the same malformed
commands while the emulation thread waited for it. Kernel64GL now shares a
262144-command budget across each graphics task and its DMA lists, limits DMA
recursion and checks command and lookahead addresses. an exhausted task returns
normally so the host can finish its interrupt handling. this is a recovery guard
for malformed or excessively long lists, not a repair to the corrupted game.
normal finite lists below the budget keep their existing behavior.

graphics diagnostics are capped at 64 messages per task, with budget warnings
limited to one per five seconds. continuing past the loop also exposed an IA16
texel overflow; byte and halfword texture/palette reads now validate buffer
bounds, extending the earlier RGBA32 protection.

the updated state completes 720 vi events and a separate 240-vi save/load replay
with the guards. normal sm64 and mario kart saved scenes also pass 240-vi replays
and save/load checks. targeted cases cover self-branching lists, recursive DMA,
200000-command finite lists, translated DMA addresses, end-of-buffer lookahead
and truncated texture reads. corrupted graphics remain and unrelated guest
failures are still possible.

## gameplay and advanced options

settings are saved independently in `Kernel64GL.config`. save applies them to
the running rom on the next rendered frame. resume if paused. old configuration
files gain compatible defaults. no rom restart is required.
the dialog has gameplay and advanced / corruption / debug tabs.

| option | effect |
| --- | --- |
| texture cache budget | 16-256 MiB; default 32; controls GPU texture eviction |
| texture output | automatic by default; optional 16-bit or experimental 32-bit conversion |
| force bilinear | overrides guest point filtering when bilinear filtering is enabled |
| reuse cached textures | disable to re-decode textures on texture-state updates |
| triangles per batch | 16-512; default 512; lower values issue more draw calls |
| render textures | disable texture uploads/use in the combiner for geometry inspection |
| depth testing / face culling | disable either independently to inspect obscured geometry |
| visualization | normal, vertex colors, texture 0, fog amount or normalized depth |
| command budget | 4096-1048576 commands per task; default 262144; always bounded |
| pause after display-list recovery | pauses after the task guard detects a looping/overlong list or invalid command fetch |
| diagnostic messages | enable/disable decoder diagnostics, with a 1-256 message limit per task |

**gameplay defaults** restores the normal rendering choices. **corruption
recovery** uses a smaller 65536-command budget and quieter logging while keeping
normal graphics. **graphics inspection** selects vertex colors, disables culling
and texture reuse, reduces the budget and pauses on recovered tasks. presets
stage edits; save persists them, cancel discards them. the defaults button also
restores resolution and the original window/fog/filter/vsync options.

debug options intentionally change game appearance. a lower command budget may
truncate a valid complex display list. 32-bit output still inherits unsupported
texture-format conversion cases; automatic is recommended for gameplay. these
options do not disable memory bounds checks or repair corrupted guest data.

real GPU tests verify vertex-color, texture, fog-factor and depth visualization,
texture disabling and restoration of normal output. configuration tests cover
all option fields, presets, live application, old files, malformed values
and isolation from legacy renderer settings. lua scripts use the same active
renderer and can observe or alter guest RAM without replacing its draw path.

## live settings

saving publishes one complete options snapshot for the active renderer. the next
display flushes the previous triangle batch before changing drawing policy and
marks culling, combiner, texture use and filtering for refresh. precision or cache
budget changes delete cached GPU uploads without resetting guest tiles or TMEM.
window size, locking and always-on-top update through the window event thread.
vsync is updated with the current GL context. settings for the other renderers
remain independent. paused games consume pending drawing changes when resumed.

main-state runtime checks changed all five debug views and returned to normal
without replacing the CPU or graphics plugin. they also changed resolution,
texture enable, cache budget/precision, culling, depth testing, wireframe and batch
size in the same running ROM, with no GL errors. host cache cleanup preserves
the dummy texture as well as the guest texture descriptors.
