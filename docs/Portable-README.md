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

This preview has mainly been exercised with Super Mario 64. Other games may have incomplete compatibility. D3D11 and standalone firmware support have documented limitations.

Matching source is provided in the source ZIP distributed with this package. See `COPYING`, `THIRD-PARTY-NOTICES.md`, `licenses/`, and the Java runtime's `runtime/legal/` notices. Original source-file terms remain applicable, including inherited Project64 notices.
