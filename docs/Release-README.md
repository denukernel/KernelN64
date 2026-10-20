# KernelN64

<img src="assets/kerneln64-icon-source.png" width="128" alt="KernelN64 K64 icon">

KernelN64 is a Windows Nintendo 64 emulator based on Jario64, with an Eclipse-friendly Java core, GLN64j graphics, independent Kernel64GL shader graphics, experimental Direct3D 11 graphics, and Azimer HLE audio.

This is a **development preview**. Super Mario 64 and Mario Kart 64 are tested games in the **Good** tier. GameShark Pro 2.0 and 3.3 firmware ROMs are also tested, both in the **Okay** tier. All other games and firmware versions remain untested. No game ROMs, firmware ROMs, saves or game assets are included.

## Game compatibility

| Game | Status | Testing coverage |
| --- | --- | --- |
| Super Mario 64 | Tested — Good | Has received the most local testing. |
| Mario Kart 64 | Tested — Good | USA version tested with GLN64j, Kernel64GL and experimental D3D11 using attract-mode race savestate replays, framebuffer comparisons, save/load checks, and ABI2 audio resampler checks. |
| GameShark Pro 2.0 firmware ROM | Tested — Okay | Standalone firmware support; physical pass-through cartridge behavior is not emulated. |
| GameShark Pro 3.3 firmware ROM | Tested — Okay | Standalone firmware support; physical pass-through cartridge behavior is not emulated. |
| All other games and firmware versions, including Zelda titles | Untested | Compatibility has not been verified. |

Tested does not mean fully compatible or verified through a complete playthrough. Results can vary by renderer, ROM revision, and scene. See the [GLN64j compatibility notes](docs/GLN64j-compatibility.md) for the Mario Kart 64 fixes and testing limits.

## Gameplay screenshots

User-captured scenes from the two tested games. These screenshots show specific scenes; they do not establish compatibility with other games or every renderer.

### Mario Kart 64

![Mario Kart 64 title screen](assets/screenshots/mario-kart-64-title.png)

![Mario racing on Luigi Raceway in Mario Kart 64](assets/screenshots/mario-kart-64-gameplay.png)

### Super Mario 64

![Mario beside a cannon in Bob-omb Battlefield](assets/screenshots/super-mario-64-gameplay.png)

![Super Mario 64 castle exterior running in KernelN64](assets/screenshots/super-mario-64-castle.png)

### Debug Tools
<img width="1103" height="861" alt="image" src="https://github.com/user-attachments/assets/8a40e579-8f28-4b53-b502-59de963caac6" />

### Firmware
<img width="1919" height="1028" alt="image" src="https://github.com/user-attachments/assets/f4747a29-08bf-4d04-b4c5-c439f6f13ab8" />


## Features

* GLN64j, independent Kernel64GL, and experimental native Direct3D 11 graphics, with live graphics/audio/input plugin switching.
* HLE audio, configurable fast mode, controller mapping and window settings.
* Savestates with numbered/named slots, file import/export and rewind.
* GameShark cheats, SM64 presets and address mapping for supported SM64 decomp builds with a matching ELF.
* CPU debugger, memory tools and a guest kernel thread monitor with audio handshake diagnostics.
* Recent ROMs and an EXE launcher for external tools.

## emulation controls

use **emulation > pause emulation** or **f2** to pause and resume. **end emulation**
(**ctrl+e**) closes the current game and saves its battery data while keeping the
application open. reopen a game from the file menu when ready.

**settings > cpu core style** selects **interpreter** or **cached interpreter**
for the next rom open or restart (**ctrl+r**). **pause when window is inactive**
applies immediately; owned settings and debugger dialogs count as part of the
active application. regaining focus only releases the inactive-window pause,
so manual pauses and debugger faults remain paused.

## Kernel64GL graphics

select **Kernel64GL (java opengl)** under **settings > settings > plugins**, apply,
then configure the active graphics plugin. its independent java decoder and GLSL
renderer have separate resolution, filtering, fog, wireframe,
window and vsync settings in `Kernel64GL.config`. saved settings apply on the next rendered frame; resume if paused. see [the plugin guide](docs/Kernel64GL.md) for details and testing limits.

## Run the portable release

Extract the entire Windows x64 ZIP into a writable folder and run `KernelN64.exe`. Its Java runtime and native libraries stay together in the extracted folder. Use **File > Open ROM** to open your own game image.

External tools can launch a ROM with:

```powershell
.\KernelN64.exe "C:\Games\Example.z64"
.\KernelN64.exe --rom "C:\Games\Example.z64" --state "C:\States\scene.j64state"
```

## Open in Eclipse

Install a **64-bit JDK 21 or newer**. Import this folder as an existing Eclipse project named **KernelN64**, refresh it, and run `kerneln64.Main` or the shared **KernelN64** launch configuration. Use the project folder as the working directory for saves and settings. GLN64j and D3D11 find their bundled native DLLs automatically; no manual library-path setup is required.

## Build the Windows package

Requirements: Windows x64, a JDK 21+, Visual Studio or Build Tools with **Desktop development with C++**, the Windows 10/11 SDK, and the Windows .NET Framework C# compiler.

```powershell
$env:JAVA_HOME = "C:\Path\To\Your\JDK"
.\launcher\Build.ps1
```

Alternatively, pass `-Jdk "C:\Path\To\Your\JDK"`. The build discovers Visual C++ with `vswhere` and writes the portable application to `dist/KernelN64`. It also refreshes Eclipse's compiled classes. The included JOGL/GlueGen dependencies are retained in `KernelN64winportable` for compatibility with existing project configuration.

The main application and native renderer sources are included. Testing fixtures and automated testing files are intentionally omitted from this prepared source snapshot.

## Documentation and limitations

* [Source layout](docs/Source-layout.md)
* [SM64 decomp cheat mapping](docs/SM64-decomp-cheats.md)
* [Standalone GameShark firmware support](docs/GameShark-firmware.md)

Direct3D 11 is experimental; some N64 rasterization behavior is approximated. Standalone GameShark support provides firmware menu emulation, not a physical pass-through cartridge. Audio distortion presets are experiments and can destabilize a game; documented approximations are labeled in the preset notes.

## Notices

Read [third-party notices](THIRD-PARTY-NOTICES.md) and [release preparation](docs/Release-preparation.md) before publishing this snapshot. GPL text is retained in `COPYING`, and original copyright/license notices remain in source files. Some inherited files also retain Project64 noncommercial-use terms; this snapshot does not replace or resolve those terms with a blanket license.

## lua and graphics debugging

open **tools > lua scripting** for the editor and console. scripts can observe
frames, access RDRAM, override controller input and use numbered state slots.
see [the lua guide](docs/Lua-scripting.md) and `lua/examples/`.

Kernel64GL settings now include gameplay and advanced / corruption / debug pages,
with texture/cache controls, diagnostic views and display-list recovery presets.
save settings to apply them live; resume if paused. see [the graphics guide](docs/Kernel64GL.md).

## keyboard controller

configure the keyboard controller plugin for **basic** bindings and stick strength,
or **advanced** inversion, diagonal range, precision modifier, opposite-direction
handling and side-specific keys. **apply** and **ok** save and update the running
game. release held keys after applying. the advanced test area previews staged
input without sending it to the game. see [controller settings](docs/Controller-settings.md).

## blue welcome screen

with no ROM open, the main window shows an extruded K64 model on a black
background, with blue face/side shading and a fast 360-degree turn every 3
seconds. the silhouette follows the supplied drawing: angular K strokes, a tall
looped 6 and an open 4 with an extended lower stem. this uses
a separate OpenGL context and the bundled Java OpenGL libraries. opening a ROM
stops and removes the idle renderer; ending emulation restores it. animation
runs at about 30 fps while the window is active and skips hidden, minimized or
inactive windows. resizing keeps the model proportions. no ROM or external
model asset is required. if OpenGL initialization fails, the window keeps a
static KernelN64 title so ROM loading remains available.

## one emulator window

only one KernelN64 instance runs per user, including separate portable copies and
direct Java launches. opening it again restores and focuses the existing window.
a ROM path and optional state passed to another launch are forwarded to that
window. launch requests wait until startup is ready and are handled in order.
an OS file lock is released automatically on exit or crash; a leftover endpoint
file does not prevent the next launch. the handoff uses an authenticated local
loopback connection. help and launcher runtime checks do not open another window.

## nonfinite comparison crash fix

single-precision COP1 comparisons involving NaN no longer crash the host while
formatting a diagnostic. unordered single/double comparisons use bounded,
nonmodal logging with raw operand bits; repeated warnings stop after eight.
the existing guest comparison condition logic is preserved. regression checks
cover NaN payloads, infinities, signed zero, both register modes and the reported
comparison instruction against the SM64 main test state. the exact superfast
BLJ route has not been replayed; this fixes the recorded Java formatter failure.

## window visibility

the Windows launcher suppresses the console while requesting a normal GUI window.
startup centers the emulator on a connected screen. opening a ROM or launching
again restores a hidden/minimized window, and moves an off-screen window back
onto a connected display. restoration runs on the Swing event thread and retains
an existing on-screen position. launcher logs include the Java child process id
for diagnosing a running process with no visible window.

Windows can apply launcher startup visibility to the first GUI show request;
see [the ShowWindow startup rules](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-showwindow).
