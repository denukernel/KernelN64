-- sm64 us extended main-test build; stop other cheats before running.
-- verified layout: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af.
local function guard()
    assert(emu.romhash() == "5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0",
        "unsupported rom: preset needs the verified sm64 us extended build")
end
guard()
-- bounded graphical corruption through the guest squash/stretch animation.
-- no display-list, texture, code or pointer corruption. affects mario movement while squashed.
local address=0x8033b224 -- gMarioStates[0].squishTimer, offset 0xb4
local original,last
local function capture()
    guard();original=memory.read_u8(address);last=nil
end
capture()
event.onloadstate(capture)
event.onframe(function()
    guard()
    local object=memory.read_u32(0x8033b1f8) -- marioObj, offset 0x88
    if object<0x80000000 or object>=0x80000000+emu.ram_size()-0x260 then return end
    local now=memory.read_u8(address)
    if last==nil or now~=last then original=now end
    local phase=math.floor(emu.framecount()/90)%3
    last=({16,24,30})[phase+1]
    memory.write_u8(address,last)
end)
event.onstop(function()
    if last~=nil and memory.read_u8(address)==last then memory.write_u8(address,original) end
end)
print("squash corruption active; stop or reload the main state to reset")
