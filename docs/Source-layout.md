# KernelN64 source layout

`src/main/java/kerneln64/Main.java` remains the application entry point and shared emulation coordinator. Eclipse launches and external launchers continue to use `kerneln64.Main`.

| Package | Responsibility |
| --- | --- |
| `kerneln64.cpu` | R4300 CPU, coprocessors, registers, instruction dispatch and handlers |
| `kerneln64.memory` | Memory bus, mapped reads/writes and DMA |
| `kerneln64.cheats` | GameShark engine, saved cheat catalog, SM64 address mapping and audio presets |
| `kerneln64.debugger` | Breakpoints, memory search, crash diagnostics and kernel thread inspection |
| `kerneln64.debugger.trace` | Legacy instruction tracing helpers |
| `kerneln64.ui` | Startup, configuration, menus, settings, keyboard shortcuts and dialogs |
| `kerneln64.hardware` | Audio/video interfaces, RSP and controller interface |
| `kerneln64.rom` | ROM decoding, boot initialization and GameShark firmware support |
| `kerneln64.state` | Savestate serialization, numbered slots and rewind |
| `kerneln64.savechips` | Cartridge save devices |

This prepared source snapshot omits automated testing files and local fixtures.

## Graphics and audio plugins

| Package | Responsibility |
| --- | --- |
| `gln64j.core` | Shared graphics decoder context, GBI constants and VI registers |
| `gln64j.opengl` | OpenGL initialization, drawing and render state |
| `gln64j.rsp`, `gln64j.opcodes` | RSP transforms and microcode command handlers |
| `gln64j.rdp` | RDP state, rectangles and framebuffer tracking |
| `gln64j.rdp.textures` | Texture decoding, transfers, caching and uploads |
| `gln64j.rdp.combiners` | Combiner state, mux tables and stage simplification |
| `gln64j.config`, `gln64j.debug` | Settings and graphics tracing |
| `kernel64gl`, `kernel64gl.engine`, `kernel64gl.renderer` | Independent Kernel64GL plugin, decoder, graphics state, texture cache, batching and GLSL color combiner; source fork with inherited credits |
| `d3d11.bridge` | HLE drawing API translation and native combiner selection |
| `d3d11.framebuffer` | GPU targets, RDRAM feedback and framebuffer invalidation |
| `d3d11.config` | Direct3D settings |
| `d3d11.bridge.generated` | Generated JOGL interface forwarding; regenerate rather than edit |
| `aziaudio.hle` | Audio microcode detection and dispatch |
| `aziaudio.hle.abi1`, `.abi2`, `.abi3` | Per-ABI state, ADPCM, buffers, mixing and resampling |
| `aziaudio.playback` | Saved DMA/ring state (`AudioCode`), worker (`AudioPlayer`) and PCM processing (`AudioSamples`) |
| `aziaudio.config` | Audio settings persistence and dialog |

The root `GLN64jPlugin`, `D3D11Plugin`, and `AziAudioPlugin` classes remain stable plugin entry points. `NativeD3D11` and `NativeAudio` retain their JNI class names so native DLL bindings remain compatible. The small Basic Audio fallback keeps its existing implementation.

`jario64plus` was an unregistered GLN64j adapter and has been removed. A saved selection of that adapter migrates to `gln64j.GLN64jPlugin`.

For combiner changes, edit `CombinerMux` for operand tables or `CombinerStages` for cycle simplification. Native D3D11 shader code lives in `native/d3d11/rdp_shader.h`; resource management and JNI entry points remain in `renderer.cpp`.

The large `D3DGL.java` consists entirely of generated interface forwarding. Its generator is `native/d3d11/GenerateGLAdapter.java`; drawing behavior belongs in `D3DGlBridge`. Regenerate with the JOGL JAR on the classpath and the output path `src/main/java/d3d11/bridge/generated/D3DGL.java`.

Audio ABI state declarations and playback state field names remain stable. Savestates use the original `aziaudio.HLEMain` and `aziaudio.ABI1`/`ABI2`/`ABI3` field labels regardless of the source packages.

Savestate field identifiers retain their historical names independently of Java packages. Keep those identifiers stable when moving classes; existing `.j64state` files must remain readable. ROM hashes and cheat file paths are unchanged.

numbered savestate slots use readable rom filenames, for example
`Save/States/CorruptedROM.z64/slot01.j64state`. a short rom hash suffix is used
only when the readable folder is already occupied by another rom or unidentified
data. full hashes remain in `slots.properties` and inside states to prevent loading
states for different rom contents. reserved and invalid windows filename characters
are sanitized.

when a rom first opens with the new layout, `StateSlots` copies its old hash-folder
slots, selection and custom names into the readable folder. the old folder stays
intact for older builds; subsequent opens use the readable folder and preserve
newer saves there. legacy quick states in `Save/<rom filename>.j64state` keep their
existing paths. roms with different filenames have separate readable folders,
even if their contents match.

the kernel thread monitor diagnoses fault flags and the libultra faulted-thread
pointer for every validated thread, including sm64 idle, scheduler, sound, game
and additional threads. the thread diagnosis tab includes saved exception details
for all faulted threads and identifies stopped threads separately. a waiting or
stopped state alone is not treated as a crash.

`monitor.pauseOnKernelFault` defaults to true and pauses at a CPU boundary after
a confirmed kernel thread fault is observed. it continues sampling with the
monitor window closed, using the existing 250 ms background snapshot interval.
resume acknowledges the captured fault context; a different fault can pause
again. rescan or toggling the fault option re-arms existing contexts. the optional
thread 4 audio fault/stall preference remains separate and keeps its existing
configuration key. these are guest-kernel checks and require a validated
scheduler; they do not diagnose host renderer deadlocks or an arbitrary guest
loop without fault evidence.

verification includes synthetic fault flags/pointers on multiple thread ids and
isolated sm64 main-test state checks with faults injected into threads 1, 3, 4
and 5. the checks cover pause/resume, successive faults, operation with the main
debugger disabled, background sampling with the monitor closed, and disabling the
fault preference. an unmodified 240-vi main-test replay and state round trip
also pass with fault monitoring enabled.

## emulation controls and cpu selection

`EmulationControl` provides the emulation menu actions: f2 pauses/resumes, and
ctrl+e ends the current session without exiting the application. shutdown runs
on a host worker, waits for the cpu thread, closes plugins and battery-save
handles, clears the active rom identity and leaves the window ready to open
another game. repeated end requests are safe, including while paused.

`Cpu.PauseState` keeps manual and inactive-window pause reasons separate from
debugger pauses and outside the guest savestate schema. commands continue to be
serviced while paused, so state save/load and plugin settings remain usable.
the java audio playback worker flushes host output and stops consuming guest
audio during a pause. monitor sampling treats every pause reason as paused.

the inactive-window preference is optional and persisted as
`emulation.pauseWhenInactive`. owned dialogs belong to the active application.
focus gain releases only that pause reason; it cannot resume a manual pause or
a debugger fault. explicit resume clears manual/debugger pauses.

the cpu style menu and general settings select interpreter or cached interpreter,
using the existing `use.cache` preference. changes apply when a rom is opened or
restarted. cached mode uses the existing decoded-instruction cache. saved host
core selection is retained when loading guest states.

checks start from the required sm64 main state and cover both core styles,
stable pc/vi and audio ring positions during pause, state save/load while paused,
focus/manual/debugger interactions, ending a paused game, reopening, settings
apply/cancel and persistence. kernel thread fault pause regressions also pass.

After refreshing the project in Eclipse, use **Project > Clean** if Eclipse still lists classes at their old locations. `launcher/Build.ps1` rebuilds the distribution and refreshes Eclipse's compiled output, removing stale classes from the old package layout.
