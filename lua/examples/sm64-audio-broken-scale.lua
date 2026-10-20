-- sm64 us extended main-test build; stop other cheats before running.
-- verified layout: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af.
local function guard()
    assert(emu.romhash() == "5cd3a174dede8bd9dc1cac671a7a5459600db1dc6d2c556a09de11f1374bc5a0",
        "unsupported rom: preset needs the verified sm64 us extended build")
end
guard()
local owned = {}
local function put(address, value, width)
    local read = width == 2 and memory.read_u16 or memory.read_u32
    local write = width == 2 and memory.write_u16 or memory.write_u32
    local now = read(address)
    local previous = owned[address]
    if not previous or now ~= previous.last then
        previous = {original=now, width=width}; owned[address]=previous
    end
    write(address,value); previous.last=read(address)
end
local function restore()
    for address, patch in pairs(owned) do
        local read = patch.width == 2 and memory.read_u16 or memory.read_u32
        local write = patch.width == 2 and memory.write_u16 or memory.write_u32
        if read(address) == patch.last then write(address,patch.original) end
    end
    owned={}
end
event.onstop(restore)
local function layout()
    guard()
    assert(memory.read_u32(0x8031cb84)==0x3c038033 and memory.read_u32(0x8031cb88)==0x24633994,
        "note frequency reader layout mismatch")
end
layout()
local base={}
local function capture()
    layout(); owned={}; base={}
    for n=0,127 do
        local value=memory.read_float(0x80333994+n*4)
        assert(value>0 and value<1000,"invalid note frequency")
        base[n]=value
    end
end
local function pitch(n,value)
    local address=0x80333994+n*4
    local now=memory.read_u32(address)
    local previous=owned[address]
    if not previous or now~=previous.last then previous={original=now,width=4};owned[address]=previous end
    memory.write_float(address,value);previous.last=memory.read_u32(address)
end
capture()
event.onloadstate(capture)
-- alternating octaves and a repeating crooked scale; percussion may stay unchanged.
event.onframe(function()
    layout()
    for n=0,127 do
        local shift=(n%2==0 and -12 or 7)+(n%3-1)*2
        pitch(n,base[n]*2^(shift/12))
    end
end)
print("broken scale active; stop restores owned note-table bytes")
