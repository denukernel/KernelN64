# Standalone GameShark firmware

Open the extracted `.z64`/`.n64` firmware using **File → Open ROM**, or pass its
full filename to `KernelN64.exe`. Do not pass the containing directory.
The usual controller settings apply (default **X = A**, **C = B**,
**I/K/J/L = D-pad**). Firmware can take several seconds to unpack its menu.

Tested with these user-supplied 256 KiB images:

| Firmware | SHA-256 |
| --- | --- |
| GameShark Pro V3.3, March build | `88b80ef37d73f3382e8c39cdd14a513c2231081d8558ae84cd7f72912d245b22` |
| GameShark V2.0, April build | `ea7cbf3934f02dc6941428d9d8e8d5996a9960ddc686fd437e707978c7cfb9e3` |

This provides **standalone firmware menu emulation**: cold boot, cartridge flash
reads, controller navigation, software framebuffer display with GLN64j/D3D11,
restart, and normal emulator savestates. Other dumps need a matching recognized
bootstrap; a filename alone does not enable this path. No firmware/game ROM is bundled.

The physical top cartridge slot, parallel-PC connection, and flash programming
are not implemented. **Start Game does not launch a separate game**, and changes
in the firmware's code editor/settings are not saved to its flash. Use KernelN64's
normal Cheats window when playing a game ROM. Source firmware files are never
modified. Existing game savestates and cheat files retain their current formats.

## Implementation

`GameSharkCartridge` checks the Datel header and bootstrap instructions, loads
the post-IPL entry point, and exposes standalone cartridge windows. The omitted
IPL checksum time is represented by an initial CP0 Count beyond the controller's
500 ms power-on delay. V2.0 initializes controllers before its VI timer service:
starting Count near zero enters an uninitialized timer list. Standard IPL handoff
globals are also initialized. Recognized firmware receives a minimum of 8 MiB
RDRAM without changing the user's RAM preference; V2.0 executes above 4 MiB.

Flash burst and separate even/odd halfword windows follow
[RWeick's LZ9FC17 replacement hardware](https://github.com/RWeick/REF1329-N64-Gameshark-Clone/blob/main/Verilog/N64GSVerilog.v).
The parallel host is disconnected and the GS button released. The V3.3
alternate-hardware probe at `12600400` is ignored and fails its ROM signature
test, allowing the firmware's own fallback. Its SI busy poll at reserved offset
`04800014` has a firmware-only compatibility alias; this does not describe that
reserved register's behavior on real N64 hardware.

The CPU now dispatches its existing SYNC implementation. PIF parsing supports
the Joybus `FD` reset-channel token described in
[Nintendo's PIF patent](https://patents.justia.com/patent/6190257).
Firmware video uses VI active dimensions/scales and RDRAM stride instead of
assuming that the stride equals the visible image width.
