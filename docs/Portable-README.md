# KernelN64 — Windows x64 development preview

Extract this entire archive into a writable folder. Run `KernelN64.exe`, then choose **File > Open ROM** and open your own game image. No separate Java installation or manual native-library path is needed.

Keep `app.jar`, `lib/`, and `runtime/` beside the launcher. Configuration, battery saves, savestates, screenshots and logs are created locally when used. No ROMs, personal settings, cheat selections or save files are included.

External launch examples:

```text
KernelN64.exe "C:\Games\Example.z64"
KernelN64.exe --rom "C:\Games\Example.z64" --state "C:\States\scene.j64state"
KernelN64.exe --check
```

Use **Settings > Settings... > Plugins** to select GLN64j, Kernel64GL, or experimental Direct3D 11 graphics. F4 toggles configured fast mode. F5 saves and F7 loads the selected state slot; numbered slots are available from the menu.

The F4 target is a speed ceiling; achievable speed depends on your computer and the game. Hold Shift for temporary uncapped turbo; releasing it restores normal speed or the active F4 mode. The audio plugin's **Mute during fast-forward** option provides silent fast mode, with normal audio restored when fast mode ends.

## Game compatibility

| Game or firmware | Status |
| --- | --- |
| Super Mario 64 | Tested — Good |
| Mario Kart 64 | Tested — Good |
| GameShark Pro 2.0 firmware ROM | Tested — Okay |
| GameShark Pro 3.3 firmware ROM | Tested — Okay |
| All other games and firmware versions | Untested |

Testing does not guarantee full compatibility or a complete playthrough. Mario Kart 64 testing covers the USA version using race savestate replays with GLN64j and experimental D3D11. D3D11 and standalone firmware support have documented limitations; GameShark physical pass-through cartridge behavior is not emulated. See `docs/GLN64j-compatibility.md` for details.

## Gameplay screenshots

![Mario Kart 64 gameplay](assets/screenshots/mario-kart-64-gameplay.png)

![Super Mario 64 gameplay](assets/screenshots/super-mario-64-gameplay.png)

Matching source is provided in the source ZIP distributed with this package. See `COPYING`, `THIRD-PARTY-NOTICES.md`, `licenses/`, and the Java runtime's `runtime/legal/` notices. Original source-file terms remain applicable, including inherited Project64 notices.

## Kernel64GL settings

select the plugin and apply before configuring it. saved settings apply on the next rendered frame; resume if paused. see [the plugin guide](docs/Kernel64GL.md).

## emulation controls

f2 pauses or resumes the game. ctrl+e ends emulation while keeping KernelN64 open.
settings offers interpreter and cached interpreter cpu styles, applied on the next
rom open or restart, plus optional pause when the application window is inactive.
manual and debugger pauses remain in effect when focus returns.

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

## window visibility

the Windows launcher suppresses the console while requesting a normal GUI window.
startup centers the emulator on a connected screen. opening a ROM or launching
again restores a hidden/minimized window, and moves an off-screen window back
onto a connected display. restoration runs on the Swing event thread and retains
an existing on-screen position. launcher logs include the Java child process id
for diagnosing a running process with no visible window.

Windows can apply launcher startup visibility to the first GUI show request;
see [the ShowWindow startup rules](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-showwindow).
