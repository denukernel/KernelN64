# keyboard controller settings

open the active keyboard controller configuration from the plugins menu.
**basic** contains the eighteen N64 bindings and stick strength (default 80).
**advanced** adds stick behavior and a staged-input test area.

click a binding, then press a key. tab, enter and space are captured before
window navigation and button actions. escape cancels capture; backspace clears
the selected binding. arrow and modifier names use stable labels. new side-specific
assignments display left, right or numpad explicitly.

**apply** saves settings and immediately updates the current keyboard controller.
**ok** applies and closes. **cancel** discards edits since the last apply.
no ROM restart is required. applying clears held input to prevent old keys from
sticking; release held keys, then press them again. settings persist in
`BasicKeyboard.config`, written atomically. existing files retain their bindings,
strength and circular-range preference; missing advanced fields use defaults.

| advanced setting | behavior |
| --- | --- |
| circular diagonal range | divides diagonal components by the square root of two; 80 becomes approximately 57 per axis |
| invert horizontal / vertical | reverses that stick axis |
| opposite stick directions | cancel to neutral by default; optional most-recently-pressed priority ignores key repeat |
| precision modifier | optional held key lowers strength, capped at the normal strength |
| separate left / right and numpad capture | new assignments distinguish physical key location; existing generic bindings accept either side |
| shared keys | allows deliberate multiple-button assignments; otherwise a conflicting capture is rejected without changing the original binding |

click the advanced test area and hold keys to see mapped X/Y and button output
using staged settings. its events stay inside the dialog. click another setting
to leave the test area. focus loss clears its held keys. applying or closing the
dialog clears preview and game input. game-view focus loss also clears input.

up is positive Y and right is positive X before inversion. a strength of 80 gives
80 on a cardinal axis; the circular option controls diagonal scaling. this is a
digital keyboard stick, with no physical analog stick deadzone or calibration.
only controller port 1 is connected by this plugin; other ports remain neutral.

assigned game-view keys take priority over emulator shortcuts, including a mapped
precision modifier. use menus for overlapping emulator commands. both physical
ctrl keys can hold one generic R binding; releasing one leaves the other held.
side-specific bindings let the other modifier side keep its normal shortcut use.

verification covered mapping values, button press/release, two held ctrl keys,
side-specific capture, tab/enter/space capture, duplicate protection, opposite
inputs, auto-repeat, circular/inverted/precision output, old/malformed config,
and focus cleanup. a runtime test loaded an isolated copy of the required SM64
main test state, checked the actual PIF packet and game stick magnitude, then
changed bindings and precision while keeping the same CPU and controller plugin.
these were synthetic Java key events through the emulator routing path; physical
keyboard rollover, OS delivery and hardware latency have not been measured.
