-- presses a for 30 video interrupts, then releases the script override
local first = emu.framecount()
event.onframe(function(frame)
    if frame - first < 30 then
        joypad.set({A=true}, 1)
    else
        joypad.clear(1)
        emu.stop()
    end
end)
event.onloadstate(function() first = emu.framecount() end)
event.onstop(function() print("input pulse ended; overrides released") end)
