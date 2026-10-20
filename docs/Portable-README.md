# KernelN64 — Windows x64 development preview

Extract this entire archive into a writable folder. Run `KernelN64.exe`, then choose **File > Open ROM** and open your own game image. No separate Java installation or manual native-library path is needed.

Keep `app.jar`, `lib/`, and `runtime/` beside the launcher. Configuration, battery saves, savestates, screenshots and logs are created locally when used. No ROMs, personal settings, cheat selections or save files are included.

External launch examples:

```text
KernelN64.exe "C:\Games\Example.z64"
KernelN64.exe --rom "C:\Games\Example.z64" --state "C:\States\scene.j64state"
KernelN64.exe --check
```

Use **Settings > Settings... > Plugins** to select GLN64j or experimental Direct3D 11 graphics. F4 toggles configured fast mode. F5 saves and F7 loads the selected state slot; numbered slots are available from the menu.

## Game compatibility

| Game or firmware | Status |
| --- | --- |
| Super Mario 64 | Tested — Good |
| Mario Kart 64 | Tested — Good |
| GameShark Pro 2.0 firmware ROM | Tested — Okay |
| GameShark Pro 3.3 firmware ROM | Tested — Okay |
| All other games and firmware versions | Untested |

Testing does not guarantee full compatibility or a complete playthrough. Mario Kart 64 testing covers the USA version with GLN64j using race savestate replays. D3D11 and standalone firmware support have documented limitations; GameShark physical pass-through cartridge behavior is not emulated. See `docs/GLN64j-compatibility.md` for details.

Matching source is provided in the source ZIP distributed with this package. See `COPYING`, `THIRD-PARTY-NOTICES.md`, `licenses/`, and the Java runtime's `runtime/legal/` notices. Original source-file terms remain applicable, including inherited Project64 notices.
