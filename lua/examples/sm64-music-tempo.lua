-- sm64 us extended main-test build; stop other cheats before running.
-- verified layout: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af.
local function guard()
    assert(emu.romhash() == "5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0",
        "unsupported rom: preset needs the verified sm64 us extended build")
end
guard()
-- change current background/secondary music tempo, leaving sfx player 2 alone.
-- editable bpm presets; this changes the current song, not its sequence id.
local bpms={72,120,180,240}
local tick=0
local function layout()
    guard()
    assert(memory.read_u32(0x8031e258)==0x3c118022 and memory.read_u32(0x8031e278)==0x26312618
        and memory.read_u32(0x8031e2a8)==0x26310140 and memory.read_u32(0x8031dc18)==0x962f000a,
        "music player layout mismatch")
end
layout()
event.onloadstate(function() tick=0;layout() end)
event.onframe(function()
    layout();tick=tick+1
    local bpm=bpms[math.floor((tick-1)/240)%#bpms+1]
    assert(bpm>=1 and bpm<=288,"bpm must be between 1 and 288")
    for player=0,1 do
        local address=0x80222618+player*0x140
        if bit32.band(memory.read_u8(address),0x80)~=0 then memory.patch_u16(address+0xa,bpm*48) end
    end
end)
print("music tempo carousel active: 72 / 120 / 180 / 240 bpm, four seconds each at 60 vi/s")
