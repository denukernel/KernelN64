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

Savestate field identifiers retain their historical names independently of Java packages. Keep those identifiers stable when moving classes; existing `.j64state` files must remain readable. ROM hashes, cheat file paths and save directories are unchanged.

After refreshing the project in Eclipse, use **Project > Clean** if Eclipse still lists classes at their old locations. `launcher/Build.ps1` rebuilds the distribution and refreshes Eclipse's compiled output, removing stale classes from the old package layout.
