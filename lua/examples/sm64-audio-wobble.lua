-- sm64 us extended main-test build; stop other cheats before running.
-- verified layout: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af.
local function guard()
    assert(emu.romhash() == "5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0",
        "unsupported rom: preset needs the verified sm64 us extended build")
end
guard()
local function layout()
    guard()
    assert(memory.read_u32(0x8031cb84)==0x3c038033 and memory.read_u32(0x8031cb88)==0x24633994,
        "note frequency reader layout mismatch")
end
layout()
local base={}
local function capture()
    layout(); base={}
    for n=0,127 do
        local value=memory.read_original_float(0x80333994+n*4)
        assert(value>0 and value<1000,"invalid note frequency")
        base[n]=value
    end
end
local function pitch(n,value)
    memory.patch_float(0x80333994+n*4,value)
end
capture()
event.onloadstate(capture)
-- bounded tape wobble; new notes and pitched sound effects change.
local tick=0
event.onframe(function()
    layout();tick=tick+1
    local ratio=2 ^ ((math.sin(tick/18)*2.5)/12)
    for n=0,127 do pitch(n,base[n]*ratio) end
end)
print("pitch wobble active; stop releases temporary note-table patches")
