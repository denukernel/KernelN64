-- generic example for sm64 and other games; no game-specific addresses
print("rom", emu.romname(), emu.romhash())
print("ram bytes", emu.ram_size())
event.onframe(function(frame)
    if frame % 60 == 0 then
        print("vi", frame, "pc", string.format("%08X", emu.getregister("pc")),
              "ram[0]", string.format("%08X", memory.read_u32(0x80000000)))
    end
end)
event.onloadstate(function() print("state loaded at vi", emu.framecount()) end)
event.onstop(function() print("frame watch stopped") end)
