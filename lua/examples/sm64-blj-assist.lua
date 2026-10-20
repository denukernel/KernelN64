-- sm64 us extended main-test build; stop other cheats before running.
-- verified layout: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af.
local function guard()
    assert(emu.romhash() == "5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0",
        "unsupported rom: preset needs the verified sm64 us extended build")
end
guard()
-- blj assist: initiate a long jump manually, then hold L + Z and steer backward.
-- pulses A once per two game frames (four VI); does not force action or speed.
-- release L or Z to return A to physical input. stairs/low ceilings still matter.
local tick=0
local lastVi=nil
event.onloadstate(function() tick=0;lastVi=nil;joypad.clear(1) end)
event.onstop(function() joypad.clear(1) end)
event.onframe(function(frame)
    guard()
    local input=joypad.get(1)
    local action=memory.read_u32(0x8033b17c)
    local active=action==0x03000888 or action==0x00000479 or action==0x0800023b
    if input.L and input.Z and active then
        local elapsed=lastVi and math.max(1,frame-lastVi) or 1
        tick=tick+elapsed
        joypad.set({A=math.floor((tick-1)/2)%2==0},1)
    else
        tick=0;joypad.clear(1)
    end
    lastVi=frame
end)
print("blj assist ready: long jump, then hold L + Z and steer backward; release L to disengage")
