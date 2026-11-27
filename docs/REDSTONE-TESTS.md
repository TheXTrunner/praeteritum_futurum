# Redstone Player Playtest Guide

This guide is for players who want to check redstone behavior in game and report useful edge cases. The circuit is vanilla-inspired, not a complete replacement for every vanilla redstone rule. Each scenario gives a setup and expected result; use a test world or backup when checking destructive actions.

For a feature overview and component controls, see the [Redstone Module Wiki page](https://github.com/TheXTrunner/praeteritum_futurum/wiki/Redstone).

## Legend

- `W` = Wire (redstone dust)
- `R→` = Repeater (arrow shows facing direction)
- `C→` = Comparator (arrow shows facing direction)
- `T` = Torch standing (no wall, always ON)
- `T↓` = Torch attached to block below (South)
- `T↑` = Torch attached to block above (North)
- `T←` = Torch attached to block left (West)
- `T→` = Torch attached to block right (East)
- `L` = Lever OFF
- `L*` = Lever ON
- `P` = Button (released; left-click in Config Mode to press)
- `O↑` = Observer (watching upward; outputs downward)
- `S(n)` = Signal Source configured to strength `n`
- `B` = Redstone Block
- `#` = Wall
- `.` = Empty slot
- `|` = Border wall (grid edge)

Signal strength is shown in parentheses, e.g., `W(15)` means wire with signal strength 15.

---

## 1. Power Sources

### Test 1.1: Redstone Block
**Setup:**
```
. . .
. B .
. . .
```
**Expected:** Redstone block shows signal strength 15. All adjacent empty slots should receive signal if wires are placed.
**Vanilla match:** ✓ Redstone blocks always output 15.

### Test 1.2: Standing Torch
**Setup:**
```
. . .
. T .
. . .
```
(Torch with no adjacent walls, default standing state)
**Expected:** Torch shows ON texture, signal strength 15. Acts like a redstone block.
**Vanilla match:** This is a creative addition. Vanilla torches must be attached to a block.

### Test 1.3: Lever ON
**Setup:**
```
. . .
. L* .
. . .
```
(Lever toggled ON in Config Mode)
**Expected:** Lever shows ON texture, signal strength 15.
**Vanilla match:** ✓ Levers output 15 when ON.

### Test 1.4: Lever OFF
**Setup:**
```
. . .
. L .
. . .
```
(Lever toggled OFF in Config Mode)
**Expected:** Lever shows OFF texture, signal strength 0.
**Vanilla match:** ✓ Levers output 0 when OFF.

### Test 1.5: Button Pulse
**Setup:** Place a button next to a wire and enable Config Mode. Left-click the button once, then repeat with a wooden button.
**Expected:** The stone button powers the adjacent wire at strength 15 immediately and releases after 20 game ticks. The wooden button does the same for 30 game ticks. Clicking while a button is already pressed does not extend its pulse.
**Vanilla match:** ✓ Stone/blackstone buttons use a 20-game-tick pulse; wooden buttons use 30 game ticks. The tooltip should show the matching duration.

### Test 1.6: Button Attachment and Support Power
**Setup:** Place a button with a `WALL` immediately below it and a wire immediately below that wall. In Config Mode, right-click the button until its tooltip reports `South`, then left-click it.
**Expected:** The button's small material-texture crop sits at the bottom edge and moves into the support when pressed. The support wall reaches strength 15 and powers the wire below it; both return to 0 when the pulse expires. Repeat with a wall on another side to check the crop and attachment direction rotate together.
**Vanilla match:** ✓ A pressed button powers nearby redstone components and strongly powers its attached block. Floor-perched mode is a 2D grid simplification.

### Test 1.7: Observer Detects a Watched-Slot Change
**Setup:** Place an observer, a lever directly on its watched side, and a wire directly on its opposite/output side. In Config Mode, rotate the observer so its gold marker points toward the lever. Toggle the watched lever; then toggle a lever beside the observer as a negative check.
**Expected:** The side lever does not trigger it. A watched-slot change starts a strength-15 pulse after 2 game ticks and the output returns to 0 after another 2 game ticks. The output-side marker lights while the pulse is active; the tooltip identifies watched and output directions.
**Vanilla match:** ✓ Java observers detect block-state changes and emit a 2-game-tick pulse after a 1-redstone-tick delay. The grid samples its simplified state signature after connected modules settle; it does not model every vanilla neighbor/shape update.

### Test 1.8: Configurable Signal Source
**Setup:** Place a Signal Source, a wire directly beside it, a second wire branching off the first, and a comparator facing through the first wire. Turn Config Mode on and left-click the source.
**Steps:** Use the `+` and `-` buttons and the slider to set strength to 7, then close the editor by clicking outside or pressing Esc. Set it to 0 and then back to 15.
**Expected:** The source starts at 0 and emits nothing. The tile displays the selected value. At strength 7, adjacent dust and the comparator's rear input read 7 while the next dust wire reads 6. At strength 15, adjacent dust reads 15. The item stays in its grid slot while editing.
**Vanilla match:** Not applicable. This is a grid-native analog source that follows the grid's normal direct-source and dust-decay rules.

---

## 2. Wire Propagation

### Test 2.1: Wire from Redstone Block
**Setup:**
```
. . .
B W W
. . .
```
**Expected:** 
- Wire adjacent to block: signal 15
- Second wire: signal 14 (decay occurs on dust-to-dust transmission)
**Vanilla match:** ✓ Direct power keeps its strength at the first wire; dust loses 1 per dust-to-dust step.

### Test 2.2: Wire from Standing Torch
**Setup:**
```
. . .
T W W
. . .
```
**Expected:**
- Wire adjacent to torch: signal 15
- Second wire: signal 14
**Vanilla match:** ✓ The dust strength is vanilla behavior; the standing torch is a grid-only source.

### Test 2.3: Wire Decay Chain
**Setup:**
```
B W W W W W
```
**Expected:** `B(15) W(15) W(14) W(13) W(12) W(11)`
**Vanilla match:** ✓ Signal decays by 1 per block.

### Test 2.4: Wire from Multiple Sources
**Setup:**
```
B W B
```
**Expected:** Wire receives signal 15 from both adjacent redstone blocks.
**Vanilla match:** ✓ Wires take the maximum input; direct source power does not lose strength.

### Test 2.5: Wire Strong Power from Repeater
**Setup:**
```
. . .
B R→ W
. . .
```
**Expected:** Wire receives strong power from repeater output. Signal strength = 15 (no decay).
**Vanilla match:** ✓ Strong power bypasses decay.

### Test 2.6: Automatic Wire Connections
**Setup:** Place one wire next to one power source, then add a second wire or component in another direction.
**Expected:** An isolated wire defaults to a four-way cross. With one eligible neighbor it forms a straight line through the wire; with multiple eligible neighbors it shows only those connections. Wire connects to adjacent dust and power sources, repeater input/output axes, and comparator side inputs, but not to a repeater's side.
**Vanilla match:** ✓ Same-level connection behavior; the grid omits vertical/stair-step connections and the optional Java dot toggle.

### Test 2.7: Dust Only Powers Walls Along Its Arms
**Setup:** Put a redstone block west of a wire, another wire east of it, and a wall north of it.
**Expected:** The powered wire forms an east-west line and the north wall stays unpowered. Replace the east wire with a wall: with only the source connection, the wire's opposite arm points east and weakly powers that wall.
**Vanilla match:** ✓ Dust only weakly powers a block it points into; wire-to-wire signal decay is unchanged.

### Test 2.8: Directional Component Connections
**Setup:** Place wire at the rear or front of a repeater, then move it to the repeater's side. Also place wire at a comparator's side.
**Expected:** Wire connects along a repeater's input/output axis but not its sides; comparator side dust is connected and remains a valid comparator side input.
**Vanilla match:** ✓ Repeater directionality and comparator dust side inputs.

---

## 3. Torch Behavior

### Test 3.1: Torch ON (Unpowered Input)
**Setup:**
```
. # .
. T↑ .
. . .
```
(Torch attached to the unpowered wall above)
**Expected:** Torch shows ON texture, signal strength 15. Outputs to all directions except the back (North).
**Vanilla match:** ✓ Torch is ON when its attachment block is unpowered.

### Test 3.2: Torch OFF (Powered Support)
**Setup:**
```
. . . . .
B W R→ # .
. . . T↑ .
```
(Torch is attached to the wall above; a powered repeater sends power into that wall)
**Expected:** Torch shows OFF texture, signal strength 0. Wall should show signal strength > 0.
**Vanilla match:** ✓ Torch turns OFF when its attachment block is powered.

### Test 3.3: Torch NOT Gate Chain
**Setup:**
```
. # . . .
. T↑ W # .
. . . T↑ .
. . . . .
```
(The first torch powers dust, which weakly powers the wall supporting the second torch)
**Expected:** 
- First torch: ON (15), with its support unpowered
- Wire and second torch's support: powered
- Second torch: OFF (0)
**Vanilla match:** ✓ Classic NOT gate behavior.

### Test 3.4: Torch Output Directions
**Setup:**
```
. W .
W T↓ W
. # .
```
(Torch attached to the wall below, with wires on its other three sides)
**Expected:** 
- Wire above (North): receives signal (torch outputs North)
- Wire left (West): receives signal (torch outputs West)
- Wire right (East): receives signal (torch outputs East)
- The support block below is not powered by the torch
**Vanilla match:** ✓ A torch does not power its support and powers adjacent dust; the 2D grid omits vertical output behavior.

### Test 3.5: Torch Skipping Wall Blocks
**Setup:**
```
. # .
# T↓ #
. # .
```
(Torch attached to wall below, surrounded by walls on all sides)
**Expected:** Torch is ON (15) but adjacent walls should NOT be powered by the torch.
**Vanilla match:** Grid-specific rule: `WALL` cells are excluded from torch output in this model.

### Test 3.6: Border-Attached Torch
**Setup:**
```
| (top border)
T↑ (torch on top row, attached to border)
.
```
**Expected:** Torch reads the top border input signal. If top input is 0, torch is ON. If top input > 0, torch is OFF.
**Vanilla match:** Not applicable. Border walls are a creative addition.

### Test 3.7: Standing Torch vs Repeater Input
**Setup:**
```
. . .
B R→ T
. . .
```
(Repeater pointing at standing torch)
**Expected:** Standing torch remains ON (15) regardless of repeater output. Repeater output has no effect on standing torch.
**Vanilla match:** The standing state is creative; for attached vanilla torches, their support block controls the torch state.

### Test 3.8: Attached Torch Transition Delay
**Setup:** Attach a torch to a wall block, then power the wall with a repeater output. Switch the repeater input off after the torch changes state.
**Expected:** The torch does not change immediately; it turns OFF two game ticks after its support becomes powered and turns ON two game ticks after the support becomes unpowered. A pulse ending before the torch's scheduled transition may be missed.
**Vanilla match:** ✓ The two-game-tick torch delay matches Java behavior.

### Test 3.9: Attached Torch Burnout
**Setup:** Attach a torch to a wall block and use a fast repeater clock or feedback circuit to power and unpower that support repeatedly. Observe the torch over at least nine turn-offs within 60 game ticks, then leave its support unpowered.
**Expected:** The first eight turn-offs do not burn out the torch; a ninth turn-off in the rolling window burns it out. It ignores state changes while eight or more turn-offs remain. Once fewer than eight remain, a support-input update can let it relight after the normal two-game-tick transition; if no such update occurs, the grid also attempts one retry 160 game ticks after burnout. Save/reopen the module while burnt out to confirm its history and pending retry persist.
**Vanilla match:** Partial. The turn-off threshold and rolling 60-game-tick window match Java behavior. Vanilla's 160-game-tick retry depends on the update chain, and the torch can also relight after other adjacent block updates. The grid models support-input updates plus one deterministic scheduled attempt.

---

## 4. Repeater Behavior

### Test 4.1: Repeater Basic Pass-Through
**Setup:**
```
. . .
B R→ W
. . .
```
**Expected:** After the repeater's delay elapses, it shows ON and outputs 15 to the wire. The default 1 setting is 2 game ticks; the wire then shows signal 15.
**Vanilla match:** ✓ Repeaters boost signal to full strength after their configured scheduled delay.

### Test 4.2: Repeater OFF (No Input)
**Setup:**
```
. . .
. R→ W
. . .
```
**Expected:** Repeater shows OFF texture, outputs 0. Wire shows signal 0.
**Vanilla match:** ✓ Repeater outputs 0 when unpowered.

### Test 4.3: Repeater Directional Output
**Setup:**
```
. W .
W R→ W
. W .
```
(Repeater facing East, surrounded by wires)
**Expected:** Only the wire to the East (facing direction) receives signal. Wires on North, South, and West should show 0.
**Vanilla match:** ✓ Repeaters only output to their facing direction.

### Test 4.4: Repeater Side-Locking (Lock ON)
**Setup:**
```
. . .
B R1→ R2→ W
B R3→ . .
```
(R1 faces East, R2 faces East, R3 faces East pointing at R2's side)
**Expected:** R2 is side-locked by R3's output. R2 freezes its output state. If R2's input was powered when locked, it stays ON.
**Vanilla match:** ✓ Side-locking freezes the output.

### Test 4.5: Repeater Side-Locking (Lock OFF)
**Setup:**
```
. . .
. R1→ R2→ W
B R3→ . .
```
(R1 has no input, R2 faces East, R3 faces East pointing at R2's side)
**Expected:** R2 is side-locked by R3's output. R2 freezes to OFF since its input was unpowered.
**Vanilla match:** ✓ Side-locking preserves the OFF state.

### Test 4.6: Repeater Unlocking
**Setup:** (Same as 4.4, then turn off the side-locking repeater)
**Expected:** When side power is removed, repeater resumes normal operation and responds to its input.
**Vanilla match:** ✓ Repeaters unlock when side power is removed.

### Test 4.7: Repeater Chain
**Setup:**
```
B R→ R→ R→ W
```
**Expected:** Repeaters turn ON one delay after the previous repeater; with the default setting, allow at least 6 game ticks for the signal to pass through 3 repeaters. The final wire shows 15.
**Vanilla match:** ✓ Repeaters can chain.

### Test 4.8: Repeater Delay Settings
**Setup:** Place a lever before a repeater and an output wire after it. Use Left-click in Config Mode to cycle delay settings.
**Expected:** Settings cycle 1→2→3→4→1. Turn-on and turn-off delays should be 2, 4, 6, and 8 game ticks respectively; the tooltip displays both redstone ticks and equivalent game ticks.
**Vanilla match:** ✓ Delay values match Java Edition repeater settings.

### Test 4.9: Short Input Pulses
**Setup:** With a 1-setting repeater, turn the input ON for one game tick, then OFF. Separately, establish an ON output, turn input OFF briefly, then restore it before the pending turn-off.
**Expected:** The short ON pulse is extended through the delayed turn-on and output pulse; the short OFF pulse is suppressed if the pending turn-off finds the input powered again.
**Vanilla match:** ✓ Timed transitions run in the world scheduled-block-tick phase and neighbor changes trigger immediate reevaluation. Per-block vanilla tick priorities and every block-update ordering edge case are not fully modeled.

### Test 4.10: Two-Repeater Clock
**Setup:** Arrange two default-delay repeaters and dust in a closed loop, then inject and remove a short startup pulse.
**Expected:** After startup, the repeater outputs alternate every 2 game ticks, producing a 4-game-tick clock period.
**Vanilla match:** ✓ [Minecraft Wiki: Redstone circuits/Clock](https://minecraft.wiki/w/Redstone_circuits/Clock) documents a 4-game-tick two-repeater loop clock with 2-game-tick ON and OFF phases.
**Automated check:** The two-tick clock GameTest starts the machine and vanilla circuits from same-tick world inputs, then requires their world-output signal timelines to match on every sampled game tick.

---

## 5. Comparator Behavior

Comparator calculations use the current compare/subtract rules, but their output changes are scheduled two game ticks after an input change. Allow that delay before checking an output in the cases below; a pending update samples the latest input when it runs.

### Test 5.1: Comparator Pass-Through (No Side Inputs)
**Setup:**
```
. . .
B C→ W
. . .
```
**Expected:** Comparator shows ON texture, outputs the input signal strength (15) to the wire.
**Vanilla match:** ✓ Comparators pass through signal strength when no side inputs are present.

### Test 5.2: Comparator OFF (No Input)
**Setup:**
```
. . .
. C→ W
. . .
```
**Expected:** Comparator shows OFF texture, outputs 0. Wire shows 0.
**Vanilla match:** ✓ Comparators output 0 when unpowered.

### Test 5.3: Comparator Directional Output
**Setup:**
```
. W .
W C→ W
. W .
```
(Comparator facing East, surrounded by wires)
**Expected:** Only the wire to the East receives signal. Other wires show 0.
**Vanilla match:** ✓ Comparators only output to their facing direction.

### Test 5.4: Comparator Signal Strength Preservation
**Setup:**
```
B W C→ W
```
(Redstone block, wire (signal 15), comparator, output wire)
**Expected:** Comparator reads input strength and outputs the same value.
**Vanilla match:** ✓ Comparators maintain signal strength.

### Test 5.5: Comparator Compare Mode: Side Blocks Output
**Setup:**
```
. R↑ .
B C→ W
. R↓ .
```
(Comparator facing East, rear input from redstone block (15). Two repeaters on the sides pointing at the comparator, outputting signal 15. Comparator in compare mode.)
**Expected:** Side inputs are 15 (strong power from repeaters). Rear input is 15. Since side (15) ≤ rear (15), output = 15.
**Vanilla match:** ✓ Compare mode outputs rear when sides ≤ rear.

### Test 5.6: Comparator Compare Mode: Side Stronger Than Rear
**Setup:**
```
. R↑ .
B W W C→ W
. R↓ .
```
(Comparator facing East. Rear input from the second wire (signal 14); side repeaters output 15. Comparator in compare mode.)
**Expected:** Side inputs are 15. Rear input is 14. Since side (15) > rear (14), output = 0.
**Vanilla match:** ✓ Compare mode outputs 0 when any side > rear.

### Test 5.7: Comparator Subtract Mode
**Setup:**
```
. R↑ .
B W W C→ W
. . .
```
(Comparator facing East in subtract mode. Rear input from the second wire (signal 14). One side repeater outputs 15.)
**Expected:** Output = max(14 - 15, 0) = 0.
**Vanilla match:** ✓ Subtract mode clamps to 0 when side > rear.

### Test 5.8: Comparator Subtract Mode: Positive Result
**Setup:**
```
. . . .
S(10) W C→ W
. S(3) W .
```
(Comparator facing East in subtract mode. The rear wire receives strength 10 from one Signal Source; the south-side wire receives strength 3 from another.)
**Expected:** Output = max(10 - 3, 0) = 7.
**Vanilla match:** ✓ Subtract mode subtracts side from rear.

### Test 5.9: Comparator Side Inputs: Power Components Ignored
**Setup:**
```
. T .
B C→ W
. . .
```
(Comparator facing East. A torch or lever is directly beside the comparator; rear input is 15. Comparator in compare mode.)
**Expected:** Torches and levers are power components, not transmission components, so they do not count as side inputs. Side input = 0. Rear = 15. Output = 15. Dust, repeaters, comparators, and a directly adjacent redstone block are accepted.
**Vanilla match:** ✓ Java comparator side input sources are transmission components or a directly adjacent redstone block; other power components are ignored.

### Test 5.10: Comparator Mode Toggle
**Setup:** Single comparator in grid.
**Expected:** Left-clicking in Config Mode toggles between compare and subtract modes. Tooltip shows current mode.
**Vanilla match:** Not applicable. This UI interaction is specific to this system.

### Test 5.11: Comparator Delay and Short Pulse
**Setup:** Place a lever (or another source) behind a comparator and an output wire in front. Turn the source on, then off again before the comparator's scheduled update runs.
**Expected:** A normal input change appears at the comparator output after 2 game ticks. The output remains latched during that delay, and a pulse that ends before the scheduled sample can be missed. Comparator signal-strength changes are delayed the same way.
**Vanilla match:** ✓ Fixed 2-game-tick delay and scheduled input sampling; exact world tick-phase/order edge cases are simplified in the grid.

---

## 6. Lever Behavior

### Test 6.1: Standing Lever ON
**Setup:**
```
. . .
. L* .
. . .
```
(Standing lever, no wall attachment, toggled ON)
**Expected:** Lever shows standing ON texture, signal 15. Broadcasts to all 4 adjacent neighbors.
**Vanilla match:** This standing state is a creative addition. Vanilla levers must be attached to a block.

### Test 6.2: Wall-Mounted Lever ON
**Setup:**
```
. # .
. L* .
. . .
```
(Lever attached to wall above, toggled ON)
**Expected:** Lever shows wall-mounted ON texture, strongly powers the attachment block (wall). Also broadcasts weak power to other neighbors.
**Vanilla match:** ✓ Levers strongly power their attachment block.

### Test 6.3: Lever Skipping Wall Blocks
**Setup:**
```
. # .
# L* #
. # .
```
(Standing lever surrounded by walls, toggled ON)
**Expected:** Lever is ON (15) but adjacent walls should NOT be powered.
**Vanilla match:** A vanilla lever powers its attachment block; this standing-lever layout is a grid-only state.

### Test 6.4: Lever Toggle
**Setup:** Single lever in empty grid.
**Expected:** Left-click in Config Mode toggles between ON and OFF states. Texture and signal change accordingly.
**Vanilla match:** ✓ Levers are toggleable.

---

## 7. Wall Behavior

### Test 7.1: Wall Powered by Repeater
**Setup:**
```
. . .
B R→ #
. . .
```
**Expected:** Wall receives strong power from repeater. Wall shows signal strength 15.
**Vanilla match:** ✓ Repeaters strongly power blocks in their facing direction.

### Test 7.2: Wall Powered by Wire
**Setup:**
```
. . .
B W #
. . .
```
**Expected:** Wall receives weak power from wire. Wall shows signal strength > 0 (no decay for walls).
**Vanilla match:** ✓ Walls can be weakly powered.

### Test 7.3: Wall Not Powered by Redstone Block
**Setup:**
```
. . .
B # .
. . .
```
**Expected:** Wall should NOT be powered by the redstone block. Redstone blocks do not power wall blocks.
**Vanilla match:** ✓ Redstone blocks do not power adjacent wall blocks.

### Test 7.4: Wall Not Powered by Torch
**Setup:**
```
. . .
T # .
. . .
```
**Expected:** Wall should NOT be powered by the standing torch.
**Vanilla match:** Grid-specific rule: `WALL` cells are excluded from torch output.

### Test 7.5: Strongly Powered Wall Powers Dust
**Setup:** Use Test 7.1 and add a wire adjacent to the powered wall, on a side not occupied by the repeater.
**Expected:** The wall is strongly powered at 15 and the adjacent wire receives 15 without a dust-decay step.
**Vanilla match:** ✓ A strongly powered solid block can power adjacent redstone dust.

---

## 8. Inter-Module Communication

### Test 8.1: Module-to-Module Signal
**Setup:** (Two redstone modules side by side in the machine)
- Module A: Lever "ON" on right edge, attached to border
- Module B: Torch on left edge, attached to border
**Expected:** Module A's right output powers Module B's left input. Torch on Module B's left edge should turn OFF.
**Vanilla match:** Not applicable. Inter-module communication is a creative addition.

### Test 8.2: Module Output Propagation
**Setup:** (Single module)
- Right edge: Wire with signal > 0
**Expected:** The module's aggregate right output channel carries the strongest signal on that edge. The basic I/O module later merges aggregate edge outputs into its configured output-face channels; adjacent redstone modules still use per-position shared-wall signals instead.
**Vanilla match:** Not applicable. Module I/O is a creative addition.

### Test 8.3: Shared Wall Slot Alignment
**Setup:** Place two redstone modules side by side in the machine. In Module A, place a redstone block immediately left of a repeater in the rightmost column, with the repeater facing right into the shared wall. In Module B, place repeaters facing right in the leftmost column on two different rows; one matches Module A's repeater row and one does not.
**Expected:** Only the repeater aligned with Module A's output row receives the shared-wall input and outputs 15 during the same redstone evaluation. The repeater on the other row remains unpowered.
**Vanilla match:** Not applicable. Slot-aligned module walls are a creative addition.

### Test 8.4: Multi-Module Circuit Settles Together
**Setup:** Place three redstone modules in a horizontal machine row, with the signal flowing from the highest machine slot index toward the lowest. In the rightmost module, put a redstone block immediately behind a repeater at the left edge, facing left into the shared wall. In the middle module, put a left-facing repeater on the right edge to receive that signal, connect its output across the row with dust, and put another left-facing repeater at the left edge. Put a left-facing repeater on the right edge of the leftmost module.
**Expected:** After one configured redstone evaluation, the repeaters in all three modules are ON. There is no extra evaluation interval for each module hop, and the result is the same regardless of machine slot order.
**Vanilla match:** Not applicable. Bounded shared-wall settling is a creative addition.

### Test 8.5: Border Component Input Tooltip
**Setup:** Place two redstone modules side by side. In Module A, place a powered repeater at the right edge pointing right into the shared wall. In the aligned left-edge slot of Module B, place a repeater facing right; then repeat with a comparator facing right.
**Expected:** The receiving component's tooltip reports the aligned shared-wall input strength (15 here), not 0. The repeater/comparator output remains consistent with that input.
**Vanilla match:** Not applicable. Shared-wall tooltip data is a creative addition.

### Test 8.6: I/O Face Input Modes
**Setup:** Install an I/O module and a redstone module with a torch attached to one grid border. For each of the six machine faces, place a redstone block next to the machine on that face and configure only the corresponding I/O face as `INPUT`; repeat with `OPEN`, `OUTPUT`, and `DISABLED`.
**Expected:** `INPUT` and `OPEN` supply that adjacent world signal to the shared circuit input bus; the border torch turns off after its two-game-tick delay. `OUTPUT` and `DISABLED` do not feed the signal in, so the torch remains on. Repeat with all six relative faces to check machine-facing rotation.
**Vanilla match:** Not applicable. The face-to-grid bus is a module integration feature. World inputs query the signal emitted toward the machine by the neighboring block.

### Test 8.7: I/O Face World Outputs
**Setup:** Install a redstone module with a powered circuit edge and an I/O module. Place a redstone lamp next to the machine's FRONT face; configure FRONT as `OUTPUT`, then repeat with `OPEN`. Test the other five faces by moving the lamp and configuring only that face. Finally set a face to `INPUT` and `DISABLED`.
**Expected:** `OUTPUT` and `OPEN` expose the strongest aggregate edge signal as weak power only on their configured world face; the adjacent lamp turns on. `INPUT` and `DISABLED` do not emit power. Turning the output off or disabling the face causes the lamp to turn off after its normal update. The automated GameTests `ioOutputSignalUsesConfiguredWorldFace` and `ioOutputUpdatesAdjacentRedstoneLamp` verify all six direction mappings and neighbor reactions.
**Vanilla match:** Uses Minecraft's directional block signal API; the I/O face bridge itself is specific to this machine.

### Test 8.8: Outside Lever Starts a One-Tick Repeater Clock
**Setup:** Install an I/O module with one face set to `INPUT`, and place an outside lever adjacent to that face. In the redstone grid, build this loop (`W` = wire, `R→`/`R↓` = repeater facing right/down); set both repeaters to delay 1:
```
. . . W . . . . .
. W R→ W W W . . .
. W . . . W . . .
. W . . . R↓ . . .
. W W W W W . . .
. . . . . . . . .
```
Toggle the outside lever ON briefly, then OFF while watching both repeater states in the circuit screen.
**Expected:** The external signal starts the loop, and the two repeaters continue alternating every 2 game ticks (a 4-game-tick period) after the outside lever is OFF. The automated GameTest `externalLeverStartsMinimumDelayRepeaterClock` covers the signal path and continued cycling.
**Manual status:** Confirmed working in-game by the project owner.
**Vanilla match:** The repeater clock timing is modeled; using machine I/O to inject the startup signal is specific to this system.

### Test 8.9: Adjacent Machines Exchange Redstone
**Setup:** Place two machines directly next to each other. In the source machine, put a powered circuit edge in the direction facing the receiver and configure that I/O face as `OUTPUT`. In the receiver, configure the face toward the source as `INPUT` and attach a torch to a grid border.
**Expected:** The receiver's border torch turns off from the adjacent machine's output. Changing the source to `INPUT` or `DISABLED` stops emitting; changing the receiver face to `OUTPUT` or `DISABLED` stops reading. The automated GameTest `adjacentMachineOutputFeedsConfiguredInputFace` covers a front-to-back connection.
**Vanilla match:** The weak world redstone signal is vanilla-facing behavior; machine-face configuration and the circuit bus are mod features.

### Test 8.10: Fast Repeater Clock Through an Output Lamp
**Setup:** Build the same two-repeater clock in vanilla and in the module, set both repeaters to the same delay, and connect each output to a lamp. Compare the repeater/output timing and lamp behavior separately for several cycles.
**Expected:** The module's scheduled repeater output should change on the same game ticks as the vanilla clock. A lamp can hide short pulses because Java Edition redstone lamps take 4 game ticks to turn off; do not use the lamp alone to judge whether the signal is pulsing.
**Automated status:** The two-tick repeater clock GameTest compares output signal timelines; automated lamp parity is not currently asserted because the test fixture did not produce an equivalent vanilla lamp connection.
**Manual status:** The project owner tested the updated timing in game and reported that it worked much better. Exact lamp parity for this side-by-side setup has not been confirmed.
**Vanilla match:** Lamp deactivation timing is vanilla behavior ([Minecraft Wiki: Redstone Lamp](https://minecraft.wiki/w/Redstone_Lamp)); the machine output connection is mod-specific.

---

## 9. Border Wall Behavior

### Test 9.1: Torch on Border Edge
**Setup:** Torch on top row (y=0), attached to top border
**Expected:** Torch reads the top border input signal. With no external input, torch should be ON.
**Vanilla match:** Not applicable. Border walls are a creative addition.

### Test 9.2: Lever on Border Edge
**Setup:** Lever on right edge (x=8), attached to right border
**Expected:** Lever can be toggled ON/OFF. When ON, it should power the border (output to adjacent module).
**Vanilla match:** Not applicable. Border walls are a creative addition.

### Test 9.3: Corner Slot Walls
**Setup:** Torch in top-left corner (0, 0)
**Expected:** Torch can attach to top border OR left border (2 valid wall directions). Right-clicking cycles through available walls + standing.
**Vanilla match:** Not applicable. Border walls are a creative addition.

---

## 10. Edge Cases

### Test 10.1: Empty Grid
**Setup:** All slots empty.
**Expected:** No signals, no errors.
**Vanilla match:** ✓

### Test 10.2: Full Grid of Wires
**Setup:** All 54 slots filled with wires, one redstone block in center.
**Expected:** The adjacent wires receive 15; each additional dust-to-dust step loses 1 strength, forming a diamond-shaped range.
**Vanilla match:** ✓ Direct power retains its strength at the first dust; subsequent dust steps decay by 1.

### Test 10.3: Torch Self-Power Loop
**Setup:**
```
. # .
. T↓ .
. # .
. T↓ .
. # .
```
(Two torches alternating, each attached to a wall that the other torch powers)
**Expected:** Attached torches toggle on scheduled updates; after a torch is forced off more than eight times within 60 game ticks, it burns out and follows the grid's deterministic retry cooldown. The bounded shared-module settling limit still prevents unending computation.
**Vanilla match:** Partial. The burnout threshold is modeled, but the deterministic retry does not reproduce every vanilla update-chain outcome.

### Test 10.4: Repeater Facing Empty Slot
**Setup:**
```
. . .
B R→ .
. . .
```
**Expected:** Repeater shows ON (input powered) but output goes to empty slot. No error.
**Vanilla match:** ✓

### Test 10.5: Comparator Facing Empty Slot
**Setup:**
```
. . .
B C→ .
. . .
```
**Expected:** Comparator shows ON (input powered) but output goes to empty slot. No error.
**Vanilla match:** ✓

### Test 10.6: Multiple Repeaters Side-Locking Same Target
**Setup:**
```
B R1→ .
. R2→ R3→ W
B R4→ .
```
(R2 and R4 both point at R3's sides)
**Expected:** R3 is side-locked. If either side-locking repeater is powered, R3 stays locked.
**Vanilla match:** ✓ Any side power locks the repeater.

### Test 10.7: Torch Rotation Cycling
**Setup:** Torch with walls on North and East sides.
**Expected:** Right-click cycles: North → East → Standing → North → ...
**Vanilla match:** Not applicable. Rotation cycling is a UI feature.

### Test 10.8: Lever Rotation Cycling
**Setup:** Lever with walls on South and West sides.
**Expected:** Right-click cycles: South → West → Standing → South → ...
**Vanilla match:** Not applicable. Rotation cycling is a UI feature.

### Test 10.9: Component Without Walls
**Setup:** Torch in center of empty grid (no adjacent walls).
**Expected:** Torch stays in standing mode. Right-clicking does not change its state.
**Vanilla match:** Standing torches are a creative addition and do not exist in vanilla.

### Test 10.10: Signal Strength Display
**Setup:** Various components with different signal strengths.
**Expected:** Tooltips show correct signal strengths. ON/OFF textures match signal state.
**Vanilla match:** Not applicable. This is a UI feature.

---

## 11. Known Vanilla Differences

These are intentional differences from vanilla Minecraft:

| # | Difference | Status |
|---|-----------|--------|
| 1 | Standing torches (always ON, no wall) | Creative addition |
| 2 | Standing levers (omnidirectional, no wall) | Creative addition |
| 3 | Border walls (grid edges as attachment points) | Creative addition |
| 4 | Inter-module communication | Creative addition |
| 5 | Wire shape is computed automatically; the Java cross/dot interaction is not implemented | Simplification |
| 6 | Attached torches use a deterministic 160-game-tick burnout retry; vanilla retry timing can depend on update order | Partial implementation |
| 7 | Four-direction 2D grid; no vertical or stepped dust connections | Simplification |
| 8 | Repeater delays and basic scheduled-tick pulse behavior are modeled; exact world tick priority/update order is simplified | Partial simplification |
| 9 | Comparator outputs use a fixed 2-game-tick scheduled update and sample current inputs at firing; exact vanilla tick-phase/order nuances are simplified | Partial implementation |
| 10 | Configured I/O faces read world redstone inputs and expose aggregate circuit outputs as weak world signals; direct strong power from the machine is not implemented | Partial implementation |

---

## 12. Quick Reference: Signal Strengths

| Component | Signal Strength | Output Type |
|-----------|----------------|-------------|
| Redstone Block | Always 15 | Powers adjacent components/dust; skips wall cells |
| Signal Source | Configurable 0-15 (defaults to 0) | Weak source to adjacent eligible components/dust; skips wall cells |
| Standing Torch | Always 15 | Weak source (omnidirectional; creative state) |
| Lever ON | 15 | Weak (omnidirectional) + strong (if wall-mounted) |
| Lever OFF | 0 | None |
| Button pressed | 15 | Weak to adjacent components; strong to attached support |
| Observer pulse | 15 while active | Strong (one output direction) |
| Torch ON | 15 | Weak output to adjacent eligible slots, excluding support |
| Torch OFF | 0 | None |
| Repeater ON | 0 (pass-through) | Strong (1 direction) |
| Repeater OFF | 0 (pass-through) | None |
| Comparator ON | 0 (pass-through) | Strong (1 direction) |
| Comparator OFF | 0 (pass-through) | None |
| Wire | Varies (0-15) | Weak; loses 1 only when powering another wire |
| Wall | Varies (0-15) | Strongly powered walls can power adjacent dust |

---

## In-game Playtesting

1. Use a test world (creative mode is convenient) and open a machine with a redstone module.
2. With Config Mode OFF, place or remove components in the circuit grid as shown in a test. Keep track of grid coordinates: columns run left-to-right, rows top-to-bottom, and the diagrams show the circuit from above.
3. Turn Config Mode ON to configure components without moving or picking up their items. Left-click toggles levers, cycles repeater delay, toggles comparator compare/subtract mode, presses buttons, or opens the Signal Source editor. Right-click changes repeater, comparator, or observer direction, or cycles torch/lever/button attachment.
4. Read signal strengths and component state in the tooltips, and compare the display and behavior with each test's Expected result. Allow the configured repeater delay to pass before judging repeater output.
5. Turn Config Mode OFF when you need normal inventory interactions again.
6. Mark each applicable test as **Pass**, **Fail**, or **Not tested**. Use **N/A** only for tests explicitly marked as a grid-only/creative feature; a failure is still useful even when it reflects a known vanilla difference.

### Report a result

Please report failures, unexpected behavior, and useful edge cases on the [GitHub Issues page](https://github.com/TheXTrunner/praeteritum_futurum/issues). Include the test number, Minecraft/mod version, a screenshot or exact component layout, the expected and observed result, and whether it happens consistently. For multiplayer reports, say which client made the change and which screens were open. Include relevant client/server log excerpts when available, but remove private information before sharing them. Successful results are also useful feedback, especially for checks marked not yet verified.

## 13. Client and Multiplayer Checks

These checks require a running Minecraft client. **Multiplayer synchronization has not yet been verified with multiple clients**; this is an unverified area, not a claim that a bug is known to exist. Please report either successful results or problems using the report format above.

### Test 13.1: Config Mode Interaction and Visual State

**Setup:** Use one redstone module with a lever, repeater, comparator, torch, button, observer, and Signal Source. Open its config screen.

**Steps:**
1. With Config Mode OFF, verify normal item pickup/placement works and does not change component settings.
2. Turn Config Mode ON. Left-click the lever and verify its state changes; left-click the repeater and verify its delay cycles; left-click the comparator and verify its mode changes; left-click the button to press it.
3. Right-click the repeater, comparator, and observer to verify facing changes. Right-click the torch, lever, and button to verify attachment cycles only through available adjacent walls and the standing or floor-perched state.
4. Left-click the Signal Source to open its editor. Check the number tile, slider, and step buttons, then close the editor.
5. Check that component items stay in their slots during configuration and that tooltips and artwork update to match the resulting states, including the repeater delay marker and locked repeater bar.

**Expected:** Configuration actions affect only the targeted component, do not move items, and show the resulting state. With Config Mode OFF, inventory behavior is restored.

### Test 13.2: Two Clients Editing the Same Module *(not yet verified)*

**Setup:** Two players join the same multiplayer world and open the same machine and redstone module on both clients. Keep both config screens open.

**Steps:**
1. On Client A, enable Config Mode and make a visible change, such as toggling a lever or rotating a repeater.
2. Without closing or reopening the screen, check Client B for the updated component state, tooltip, and signal display.
3. If possible, make a different change from Client B and verify Client A receives it as well.

**Expected:** Both open views of that module converge on the latest server state without requiring a reopen. Record whether updates are immediate or delayed and whether either client can overwrite or miss a change.

### Test 13.3: Separate Modules and Machines Do Not Cross-Update *(not yet verified)*

**Setup:** Put at least two redstone modules in one machine and, if possible, a second machine nearby in the same chunk. Open different module screens from the two clients (or use separate screens where available).

**Steps:** Make a distinct change in each open module, one at a time, and watch every other open screen. Repeat with the two clients swapping which module they edit.

**Expected:** Each screen displays only the circuit identified by its machine and module slot. Updates from another module or machine must not blank, replace, or alter the displayed component state.

### Test 13.4: State After Closing and Reopening

**Setup:** Configure a lever, repeater, comparator, and torch in one module, then close and reopen its screen. For an additional persistence check, save and reload the test world.

**Expected:** Component configuration and displayed states remain associated with the correct module after reopening and world reload. Note any state that resets or appears on a different module.

### Test 13.5: Signal Source Editor and Readout

**Setup:** Place a Signal Source beside dust and a comparator input path, then open the redstone config screen.

**Steps:** Turn Config Mode ON and left-click the source. Try the `-` and `+` buttons and drag or click at several positions on the slider; close with Esc and by clicking outside. Reopen the editor, then close and reopen the whole module screen.

**Expected:** The popup shows the exact 0-15 value; each step button changes it by one, the slider snaps to integer strengths, and the square tile updates to the selected number. The selected signal affects the circuit and persists when the editor or screen is reopened. No item can be picked up while configuring.

### Test 13.6: Processing Module Redstone Control

**Setup:** Place a regular processing module (not the Jukebox) beside a redstone module and open its machine configuration overlay. Make the adjacent redstone module's border facing the processing module switch between signal 0 and a nonzero signal.

**Steps:**
1. Hover the redstone-dust button in the bottom-right corner, opposite the close button, and confirm the tooltip explains the current policy.
2. Click the button to cycle `IGNORE → HIGH → LOW`; Shift-click to cycle backward. Confirm the button's accent color changes with the policy.
3. Set `HIGH` and verify the processing module only advances while its adjacent redstone signal is nonzero. Set `LOW` and verify it only advances while that signal is zero.
4. While processing is blocked, verify its progress/items are preserved. Return to `IGNORE` and confirm normal processing resumes regardless of signal.
5. Close and reopen the machine screen; confirm the selected policy remains on that module.

**Expected:** The setting is per module, defaults to `IGNORE`, and does not move or consume items. The strongest signal from an immediately adjacent redstone-module border or an enabled I/O world-input face controls it. I/O transfers are separate and may continue while processing is blocked.

The Jukebox and basic Filter Module do not expose this control. Redstone control for an Advanced Filter is reserved for a future module variant.

### Test 13.7: Direct I/O Redstone Control

**Setup:** Place a processing module with redstone control set to `HIGH`, and an I/O module configured with one world-facing side set to `INPUT`. Place a lever next to that machine face. No redstone module is needed for this test.

**Steps:**
1. Turn the lever on and verify the processing module's redstone-control indicator shows that its policy is allowing operation; confirm processing advances.
2. Change that I/O face to `OPEN` and verify the world signal still enables the module.
3. Change the face to `OUTPUT`, then `DISABLED`; verify neither mode accepts the lever signal and the `HIGH`-controlled module pauses.
4. Set the processing control to `LOW` and confirm it allows operation while the face is `OUTPUT` or `DISABLED`, but pauses when the face is `INPUT` or `OPEN` and the lever is on.

**Expected:** `INPUT` and `OPEN` provide direct world-to-module control; `OUTPUT` and `DISABLED` do not. This path uses the strongest signal across enabled I/O faces and does not require a redstone module. `OPEN` remains bidirectional, so external wiring can intentionally or unintentionally return the machine's own output as input.

### Test 13.8: Ejection Module Redstone Control

**Setup:** Place an Ejection Module beside an I/O Module and one or more Inventory Modules. Configure at least one inventory direction as `DROP`, set an I/O face to `OUTPUT` or `OPEN`, and provide a world signal through an enabled `INPUT` or `OPEN` I/O face.

**Steps:**
1. Hover the Ejection Module's bottom-right redstone button and confirm the tooltip explains the selected policy.
2. Cycle `IGNORE → HIGH → LOW → PULSE`; Shift-click to move backward. Close and reopen the machine screen to verify the setting persists.
3. In `IGNORE`, verify normal ejection continues regardless of input. In `HIGH`, verify it repeats only while powered; in `LOW`, verify it repeats only while unpowered.
4. In `PULSE`, briefly switch the input from off to on, then back off before the next ejection attempt. Verify one ejection still occurs, holding the signal on does not repeat, and a later off-to-on transition triggers one more pass.

**Expected:** Ejection control defaults to `IGNORE`; `HIGH` and `LOW` repeat at the normal ejection cadence. `PULSE` triggers one normal ejection pass per rising edge and queues a brief pulse until the next cadence. No pulse is generated merely by holding a signal high.

### Test 13.9: Blueprint Redstone Component Costs and Restore

**Setup:** In a test world, configure a machine with multiple modules, including a Redstone Module containing several component types. Give one component a non-default setting, such as a repeater delay, comparator mode, torch attachment, or Signal Source value.

**Steps:**
1. Shift-right-click the configured source machine with a Blueprint to copy its layout. Hold Shift while hovering over the Blueprint in your inventory and inspect the missing-material list.
2. Try to apply it to an empty target machine without all required materials. Note your inventory before and after the rejected attempt.
3. Supply the clean module items and the exact component items required by the saved Redstone Module grids, then right-click the empty target machine with the Blueprint.
4. Compare the installed module positions, component positions, and durable settings with the source. Check whether active pulses or pending timing transitions were copied.
5. In Creative mode, repeat on another empty machine and confirm the paste does not consume materials.

**Expected:** A failed Survival paste leaves both the target and inventory unchanged. A successful Survival paste consumes the required modules and embedded component items, restores the circuit layout and durable settings, and does not carry over in-progress runtime state. The same Blueprint can be reused when the required materials are provided again. Creative pasting is free. Cleaning a Blueprint removes its saved circuit data.

### Test 13.10: Data Cleaning Returns Redstone Components

**Setup:** Configure a Redstone Module with several different component items and place it alone in a crafting grid. Keep enough inventory space for the first attempt.

**Steps:**
1. Use the Data Cleaning recipe and collect the clean Redstone Module.
2. Verify every component from the module's grid is returned to your inventory, including item variants and stack counts.
3. Repeat with a full inventory and check that components which do not fit are dropped near the player instead of disappearing.

**Expected:** The Redstone Module is cleaned, and none of its embedded components are lost. Components go into available inventory slots first; only overflow is dropped nearby.

## References

- [Minecraft Wiki: Redstone mechanics](https://minecraft.wiki/w/Redstone_mechanics)
- [Minecraft Wiki: Redstone dust](https://minecraft.wiki/w/Redstone_dust)
- [Minecraft Wiki: Redstone comparator](https://minecraft.wiki/w/Redstone_Comparator)
- [Minecraft Wiki: Redstone repeater](https://minecraft.wiki/w/Redstone_Repeater)
- [Minecraft Wiki: Redstone torch](https://minecraft.wiki/w/Redstone_Torch)
- [Minecraft Wiki: Button](https://minecraft.wiki/w/Button)
- [Minecraft Wiki: Observer](https://minecraft.wiki/w/Observer)
- [Minecraft Wiki: Redstone components](https://minecraft.wiki/w/Redstone_components)
