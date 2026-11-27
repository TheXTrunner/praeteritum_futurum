# Redstone Module

The Redstone Module provides a 2D redstone circuit system within a 9×6 grid (54 slots), matching the layout of a double chest. It allows players to build, test, and configure redstone circuits inside a module inventory.

## Architecture

The redstone system uses a **three-layer architecture**:

1. **Inventory Layer** - `RedstoneConfigMenu` stores `ItemStack` objects in 54 slots. Items are mapped to component types via `RedstoneModuleItem.getComponentFromItem()`.
2. **Logic Layer** - `RedstoneCircuit` holds all component properties and runs signal propagation on the server. Each slot has a `RedstoneSlotData` object containing type-specific properties.
3. **Render Layer** - `RedstoneConfigScreen` reads from a server-synced `RedstoneCircuitState` snapshot to display overlays, tooltips, and visual indicators.

### Data Flow

```
Inventory Items → Machine Tick → Circuit Sync → Shared-Wall Settling → State Snapshot → Client Packet → Render
```

The server sends a `ClientboundRedstoneCircuitStatePacket` after each redstone evaluation and configuration action. Packets include the machine position and module slot, and the screen ignores updates that do not match the circuit it has open.

## Grid Layout

- **Dimensions:** 9 columns × 6 rows = 54 slots
- **Slot indexing:** `y * GRID_WIDTH + x` (row-major order)
- **External I/O:** Configured I/O faces gate world inputs and store output values. The circuit model computes signals; `MachineFrameworkBlock` exposes eligible outputs to the world as weak redstone power.
- **Aggregate output channels:** `propagateSignals()` returns the maximum signal strength on each edge in `[top, bottom, left, right]` order. Repeater/comparator outputs contribute when facing outward; observer output contributes through the edge opposite its watched side. Their internal `signalStrength` remains 0.
- **Border walls:** The 4 edges of the grid act as implicit walls. Torches, levers, and buttons at an edge can attach to it. World inputs are merged into one strongest-signal bus and supplied to all four edges, so the basic I/O module does not provide edge-specific world input.
- **Inter-module communication:** Adjacent redstone modules share a metaphorical wall, aligned slot by slot. Each edge position is independent: a signal at row 2 on one module can only power row 2 on its neighbor (and columns align for top/bottom edges). Only power sent into the shared wall by a boundary component is delivered to the matching position; the four aggregate output channels remain separate from this routing. The face-wide I/O input bus is merged with each module's boundary inputs. All redstone modules in a machine are evaluated together in rounds using the same boundary snapshot, until outputs stabilize (up to 28 rounds), so a signal can travel across a chain in one redstone evaluation rather than one module per tick.

### I/O Module Bridge

- **Input modes:** An effective face mode of `INPUT` or `OPEN` reads the neighboring world's redstone signal. `OUTPUT` and `DISABLED` do not feed a signal into the circuit. Minecraft's `SignalGetter.getSignal` reports the signal emitted from a position in the queried direction ([NeoForge 1.21.1 API](https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/minecraft/world/level/signalgetter)).
- **Shared input bus:** The strongest enabled face signal is merged with `max` and supplied to all four redstone grid boundaries. This basic I/O module does not map a particular world face to a particular grid edge; that precision is a possible upgraded-module feature.
- **Processing-module controls:** The same strongest world input from `INPUT`/`OPEN` faces can directly control opted-in processing modules; a redstone module is not required for simple run-when-powered or run-when-unpowered behavior. Adjacent redstone-module border outputs are also eligible, and both sources combine by strongest signal.
- **Output modes:** The strongest value among the four aggregate grid-edge outputs of every redstone module is stored for each I/O face configured as `OUTPUT` or `OPEN`. `INPUT` and `DISABLED` faces store 0. Face channels are `FRONT=0`, `BACK=1`, `LEFT=2`, `RIGHT=3`, `UP=4`, and `DOWN=5` in that I/O module's machine-data slot.
- **World outputs:** `OUTPUT` and `OPEN` faces expose that stored strength as a directional weak redstone signal from the machine block. The machine does not emit direct/strong power into the neighboring block. Changes to the output channels notify neighboring blocks so they can react.
- **Feedback:** `OPEN` is intentionally bidirectional: it both reads the neighboring world's redstone signal and exposes the circuit output. The adapter does not wire a machine's output directly into its own input bus, but a world circuit connected back to an `OPEN` face can return the machine's emitted signal as input and create feedback. This is the current simple behavior, not a claim that it is the best design for every circuit; feedback may be useful for intentional constructions, while other setups may want isolation. We welcome feedback and use cases from players to guide a more considered approach in the future. Conflicting face modes from installed modules resolve through `getEffectiveFaceMode` (including `INPUT` plus `OUTPUT` resolving to `OPEN`).

### Module Redstone Control Hook

- `BaseModuleItem` provides optional `IGNORE`, `HIGH`, and `LOW` redstone-control policies. Regular processing modules opt in; the Jukebox deliberately does not, because its playlist and playback behavior should remain governed by its own controls. The Ejection Module opts in with an additional `PULSE` policy. The basic Filter Module, I/O Module, Inventory Module, and Redstone Module do not opt in; redstone control for a future Advanced Filter is a separate feature.
- The control signal is the strongest of (1) world redstone entering any effective I/O face configured as `INPUT` or `OPEN`, and (2) outputs from immediately adjacent redstone modules' borders facing the controlled module. `OUTPUT` and `DISABLED` faces do not provide control input. The basic I/O module merges enabled world faces into a shared input bus, and the individual circuit slot producing an adjacent module's border signal is intentionally irrelevant.
- `IGNORE` is the default and always permits normal module ticks; `HIGH` permits them only while powered; `LOW` permits them only while unpowered. A blocked tick pauses processing without clearing the module's saved progress. The setting is stored per module stack.
- The Ejection Module additionally supports `PULSE`: each off-to-on signal edge queues one normal ejection pass, held power does not retrigger it, and a falling signal rearms it. Pending pulses wait for the next ejection cadence and are stored with the module stack.
- In the machine configuration overlay, click the redstone-dust button in the bottom-right corner, opposite the close button, to cycle the policies available to that module; Shift-click cycles backward. Regular processing modules cycle `IGNORE → HIGH → LOW`, while the Ejection Module cycles `IGNORE → HIGH → LOW → PULSE`. The tooltip names the current behavior and signal sources. A world face configured as `OPEN` can receive a machine's own output if the external redstone wiring connects it back, so use `INPUT` when a one-way world-to-machine connection is desired.
- `supportsRedstoneControl()` remains the opt-in hook for other module types. The control affects a module's normal processing tick only; separately scheduled systems such as I/O transfers are not globally paused. Jukebox playback and playlist handling are not gated by redstone control.

## Components

### Wire
- **Behavior:** Transmits signal to adjacent wire; strength decays by 1 only when moving from one wire to another
- **Signal:** A direct source or strongly powered wall provides its full strength; adjacent dust provides `max(input - 1, 0)`
- **Power type:** Weak power; it powers an adjacent wall only in a direction represented by its connection shape
- **Connections:** Wire always connects to adjacent wire and connects to power sources. It connects to a repeater on its input/output axis; comparator connections include both side-input positions. Repeater/comparator sides are not treated as wire connections except for comparator side inputs.
- **Shape:** With no eligible neighbors the wire displays a cross; with one eligible neighbor it extends into a straight line; with multiple eligible neighbors it displays only those connections. Connection masks are server-synced to the screen.
- **Properties:** None

### Repeater
- **Behavior:** Delayed, one-way repeater that boosts input to full strength (15)
- **Signal:** Outputs 15 to facing direction only after its scheduled delay; own signalStrength stays 0
- **Power type:** Strong power (directional output)
- **Properties:**
  - `rotation` (0-3): Facing direction (N/E/S/W)
  - `repeaterDelay` (1-4): Delay in redstone ticks, equivalent to 2, 4, 6, or 8 game ticks
  - `repeaterPowered`: Latched output state, persisted between evaluations
  - `repeaterScheduledGameTime`: Due game time of the single pending transition, or -1
  - `repeaterLocked`: True when side-locked by another repeater/comparator output
  - `repeaterLockOutput`: Frozen output value (ON/OFF) while locked
- **Side-locking:** Only the output of a powered repeater/comparator pointing at the repeater's side can lock it. When locked, the repeater freezes its current output state until side power is removed.
- **Scheduled ticks:** A transition is queued only when no repeater tick is pending. At the due time, the repeater rechecks its input; a short ON pulse can be extended to its configured delay, and a short OFF pulse can be suppressed if power returns before the pending turn-off. A future pending tick survives a temporary lock, but is ignored if it becomes due while locked.
- **Simultaneous transitions:** Repeaters due at the same game time use one shared input snapshot and update together before the circuit is re-settled. This avoids slot-order-dependent behavior in repeater feedback loops.

### Comparator
- **Behavior:** Directional component with compare and subtract modes
- **Signal:** Output depends on mode; own signalStrength stays 0
- **Power type:** Strong power (directional output to facing direction only)
- **Properties:**
  - `rotation` (0-3): Facing direction (N/E/S/W)
  - `comparatorSubtract`: Mode flag (false=compare, true=subtract)
- **Compare mode:** Reads rear input and two side inputs. Outputs rear signal if both side inputs ≤ rear; otherwise outputs 0. Formula: `output = rear × [left ≤ rear AND right ≤ rear]`
- **Subtract mode:** Subtracts the greater side input from the rear input. Formula: `output = max(rear - max(left, right), 0)`
- **Side inputs:** A wire contributes its current signal strength. A repeater/comparator output must point into the side; strongly powered wall cells and adjacent redstone blocks also count. Torches and levers do not count as direct side inputs.
- **Mode toggle:** Left-click in Config Mode toggles between compare and subtract modes.

### Torch
- **Behavior:** Attached torches are directional NOT gates - they invert the signal of their support block
- **Signal:** An attached torch changes state after a two-game-tick delay: outputs 15 when its support is unpowered and 0 when powered
- **Power type:** Weak power to adjacent eligible components; does not power its support
- **Properties:**
  - `torchRotation` (0-4): 0-3 = wall-mounted (N/E/S/W), 4 = standing upright (no wall)
  - Default: 4 (standing)
- **Wall-mounted torches (rotation 0-3):** Read input from the support direction and output to the other adjacent slots. They power adjacent dust at strength 15 but do not power their support or `WALL` cells; excluding `WALL` cells is a grid-specific rule based on the project's observed behavior. These torches burn out after more than eight turn-offs within 60 game ticks. They ignore state changes until fewer than eight turn-offs remain; a subsequent support-input update can then schedule a normal two-game-tick transition. A single retry is also scheduled 160 game ticks after burnout, matching one of vanilla's possible update paths.
- **Standing torches (rotation 4):** Always ON (signal 15), act like a redstone block. Output to all 4 directions. A repeater or comparator aiming at a standing torch has no effect on it.
- **Rotation cycling:** Right-clicking cycles through valid wall directions, border walls, and standing. A torch on the grid edge can attach to the border.

### Lever
- **Behavior:** Toggleable power source - outputs 15 when ON, 0 when OFF
- **Signal:** Constant 15 when ON
- **Power type:** Weakly powers adjacent eligible components; wall-mounted levers also strongly power their attachment block
- **Properties:**
  - `leverOn` (boolean): ON/OFF state
  - `leverRotation` (0-4): 0-3 = wall-mounted (N/E/S/W), 4 = standing (no wall)
  - Default: 4 (standing)
- **Wall-mounted levers (rotation 0-3):** Broadcast to adjacent eligible components and strongly power the attachment block. A strongly powered attachment block can power adjacent dust.
- **Standing levers (rotation 4):** Broadcast omnidirectionally to all 4 neighbors.
- **Rotation cycling:** Right-clicking in Config Mode cycles through valid wall directions, border walls, and standing. A lever on the grid edge can attach to the border.

### Button
- **Behavior:** Momentary power source. Left-clicking it in Config Mode presses it immediately; the machine's scheduled block tick releases it automatically.
- **Signal:** Outputs 15 while pressed, then 0 when its pulse expires. A second press while it is still held does not extend the pulse.
- **Duration:** Stone and polished blackstone buttons pulse for 20 game ticks (10 redstone ticks); wooden buttons pulse for 30 game ticks (15 redstone ticks).
- **Attachment:** Buttons default to the floor-perched state (rotation 4). Right-click in Config Mode cycles through adjacent `WALL` cells, border walls, and the floor-perched state. The 2D grid does not separately model ceiling mounting.
- **Power:** A pressed button weakly powers adjacent eligible components and dust. When attached to a `WALL` or border wall, it also strongly powers that support; a strongly powered `WALL` passes power to its adjacent dust.
- **Visual:** The grid samples a six-pixel square from the material sprite of the actual button item in the slot. The crop is centered when floor-perched, shifts to the selected edge when attached, and presses toward the support while active; no custom button texture is needed.
- **Tooltip:** Shows material-specific pulse duration, attachment direction, and pressed/released state.

### Observer
- **Behavior:** Detects stable changes in the block state of the adjacent slot it faces. Right-click in Config Mode rotates its watched direction.
- **Timing:** After a detected change, waits 2 game ticks (1 redstone tick), then emits signal strength 15 for 2 game ticks.
- **Direction:** Reads the slot in front of its observing face and outputs only from its opposite side. Dust connects to the observer only at that output side.
- **Detected state:** The grid observes component placement/removal and component-relevant state changes, including powered dust strength/connectivity. It ignores repeater lock-only changes because Java observers do not receive a shape update for that change ([Minecraft Wiki: Observer](https://minecraft.wiki/w/Observer)).
- **Stability:** Changes are sampled after the connected module network settles, so temporary intermediate values during a settling round do not trigger extra pulses. Border observers do not inspect world blocks outside the grid.
- **Visual:** Uses the vanilla observer top-face texture, with a gold watched-side marker and a red output indicator while pulsing.

### Signal Source
- **Behavior:** Custom analog power source with a configurable strength from 0 to 15; new sources default to 0 so inserting one does not unexpectedly energize a circuit.
- **Power:** Broadcasts its configured strength to adjacent eligible components and dust. As with other direct sources, dust directly beside it retains that strength; dust-to-dust connections decay by one. It does not power adjacent `WALL` cells.
- **Comparator use:** Can supply an analog rear input directly or through dust. Direct signal-source power is not accepted as a comparator side input; use dust or the supported vanilla-like side sources.
- **Configuration:** In Config Mode, left-click opens a small editor with a discrete 0-15 slider and `-` / `+` step buttons. The grid tile shows the configured number in a colored square.
- **Persistence:** The selected strength is saved with the circuit and included in synchronized screen state.

### Redstone Block
- **Behavior:** Constant power source that always outputs signal strength 15
- **Signal:** 15
- **Power type:** Powers adjacent eligible circuit components and dust; it does not power adjacent `WALL` cells in this grid
- **Properties:** None

### Wall
- **Behavior:** Abstract solid/conductive block that can receive weak or strong power
- **Signal:** Receives power from adjacent components; used as torch/lever attachment point
- **Power type:** A weakly powered wall does not power dust; a strongly powered wall powers adjacent dust at its full strength
- **Properties:** None
- **Note:** Repeaters/comparators and wall-mounted levers/buttons can strongly power walls. Dust can weakly power them. Redstone blocks, torches, and weak lever/button broadcasts skip wall cells.

## Signal Propagation

Circuits without repeaters, comparators, attached torches, or observers evaluate at the configured redstone evaluation rate. If a machine contains one of those timed components, its circuits are evaluated every game tick. World neighbor changes also trigger an immediate reevaluation, so external input changes are sampled without waiting for the next block-entity tick. Each machine first syncs all its redstone module inventories, then settles their shared-wall connections together:

1. **Save previous signal strengths** - needed for repeater lock freeze logic
2. **Reset all signals to 0** - signal strength is a computed value, not persisted
3. **Inject external input signals** - configured I/O `INPUT`/`OPEN` faces contribute their strongest world signal to each grid boundary; aligned neighboring redstone modules also contribute their shared-wall inputs
4. **Process repeater lock transitions** - server-side only; freezes/unfreezes locked repeaters
5. **Run 16 propagation passes** - one source-initialization pass plus 15 signal-strength steps
6. **Update shared-wall outputs** - repeat propagation rounds until neighboring module outputs stabilize (maximum 28 rounds)
7. **Process due component ticks** - the machine's scheduled block tick processes repeater, comparator, attached-torch, button, and observer transitions due at that game time from one input snapshot, then re-settles after changed outputs
8. **Schedule future transitions** - queue repeater changes using their selected delay, comparator/attached-torch/observer changes using the fixed two-game-tick delay, and button release at its material-specific duration; keep a machine block tick scheduled while transitions are pending

After the combinational signals settle, the machine schedules a one-game-tick block callback while any repeater, comparator, attached-torch, button, or observer transition is pending. Due transitions run from `MachineFrameworkBlock.tick` in the world's scheduled block-tick phase, using the current game time rather than the configurable circuit evaluation interval. Components due together use the same pre-transition inputs, then the shared module network settles once after the batch. Repeaters use their selected delay (`2/4/6/8` game ticks); comparators, attached torches, and observers use a fixed `2`-game-tick delay; buttons release after 20 game ticks for stone/blackstone or 30 for wood. Observers detect watched-slot changes only after the network settles, emit for 2 game ticks, and ignore repeater lock-only changes. A comparator samples its latest calculated input when its scheduled tick runs, so a pulse that ends before that sample can be missed. Attached torches latch their ON/OFF state and count turn-offs in a rolling 60-game-tick window; entries exactly 60 game ticks old remain in the window. Burnout state, history, the last support input, and pending due times persist in circuit NBT. The grid schedules one retry 160 game ticks after burnout and can also retry on a support-input update once fewer than eight turn-offs remain. Vanilla's 160-game-tick retry is update-chain-dependent, and its block-update triggers include neighboring updates beyond this grid's modeled support input, so this is not a complete reproduction of every vanilla retry path. A repeater's pending tick survives a temporary side-lock, but if it becomes due while locked it is consumed without changing the output. This aligns transition timing with the world scheduler, while block-specific vanilla tick priorities and every neighbor-update ordering edge case are not fully emulated.

Each shared-wall settling round runs 16 internal propagation passes per module. Neighboring circuits consume aligned outputs together on the next round. The process stops when every shared-wall output is unchanged, with a 28-round cap (the 27 machine slots plus one confirmation round) to prevent feedback circuits from running indefinitely. If a feedback loop never stabilizes, the final bounded round is used; torch burnout is only modeled for attached torches and does not change this settling cap or standing-torch behavior.

### Each Propagation Pass

1. **Snapshot current signal strengths** - all components read from the same state
2. **Source and dust output** - redstone blocks, levers, buttons, signal sources, and torches provide direct source power; wire-to-wire propagation loses one strength. A wire weakly powers a `WALL` cell only when its shape points toward it. Torch support directions and source-excluded wall cells are skipped.
3. **Directional output** - repeaters/comparators use their current latched output to power only their facing slot; wall-mounted levers and buttons strongly power their attachment block
4. **Strong block transmission** - strongly powered wall cells provide their strength to adjacent dust
5. **Compute new signal strengths:**
   - **Wire:** `max(direct power, strong power, max(adjacent wire strength - 1, 0))`
   - **Repeater/Comparator:** Signal strength stays 0 (pass-through devices); comparator output is latched separately from its newly calculated target
   - **Torch:** 15 if ON, 0 if OFF; attached torches change state on scheduled updates, standing torches stay ON
   - **Wall:** Retains its received level; only strong power is relayed to adjacent dust
   - **Lever:** 15 if ON, 0 if OFF
   - **Button:** 15 while pressed, 0 after its scheduled release
   - **Signal Source:** Its configured strength (0-15)
   - **Redstone Block:** Always 15

### Strong vs. Weak Power

- **Strong power:** Repeater/comparator output to the front, and a wall-mounted lever/button's power to its attachment block. A strongly powered wall can power adjacent dust.
- **Weak power:** Direct source power and dust power that does not strongly power a wall. Source strength is retained into adjacent dust; dust loses one strength only when transmitted to another dust slot. A wire weakly powers a wall only along an active arm.

## Configuration UI

The redstone config screen provides two modes of interaction:

### Normal Mode
- Standard inventory behavior - pick up, place, swap, and drag items
- No component configuration is possible
- Items follow normal Minecraft inventory rules

### Config Mode
- Toggle via the "Config Mode" button in the screen
- All item interaction on grid slots is blocked
- **Left-click (button 0):** Toggle lever ON/OFF, toggle comparator mode (compare ↔ subtract), cycle repeater delay (1→2→3→4→1), press a button, or open the Signal Source strength editor
- **Right-click (button 1):** Rotate repeater/comparator facing and observer watched direction; cycle torch, lever, or button attachment
- Click sound provides feedback for configuration actions

### Visual Indicators
- Component overlays replace vanilla item sprites; buttons use a small crop of their vanilla material sprite, and the Signal Source uses a colored square with its strength number
- Rotation indicators show directional component facing or attachment
- A locked repeater displays a dark bedrock-style bar overlay, rotated with its facing; the tooltip reports whether its frozen output is ON or OFF
- The repeater delay marker uses the standing torch ON/OFF art and moves down by two pixels for each configured delay step
- Custom tooltips show component name, rotation, signal strength, input/output values, and lock state

## GUI Textures

The wire renderer uses Minecraft's own dust sprites from the `minecraft` resource namespace; it does not bundle duplicate vanilla PNGs. Directional line halves and the center dot are tinted by signal strength, with the vanilla overlay drawn untinted.

| Vanilla resource | Purpose |
|------|---------|
| `minecraft:textures/block/redstone_dust_line0.png` | North/south wire-arm texture |
| `minecraft:textures/block/redstone_dust_line1.png` | East/west wire-arm texture |
| `minecraft:textures/block/redstone_dust_dot.png` | Center texture for isolated, corner, and branched wire shapes |
| `minecraft:textures/block/redstone_dust_overlay.png` | Untinted highlights/shading layered over wire arms and center |
| `minecraft:textures/block/redstone_block.png` | Vanilla redstone block sprite used in the circuit grid |

Custom component textures are 16×16 PNGs located in `assets/praeteritum_futurum/textures/gui/redstone/`:

| File | Purpose |
|------|---------|
| `repeater_on.png` | Repeater ON (draw facing North, rotated by code) |
| `repeater_off.png` | Repeater OFF (draw facing North, rotated by code) |
| `repeater_lock_bar.png` | Dark lock overlay for a side-locked repeater (draw across the top, rotated by code) |
| `comparator_compare_on.png` | Comparator ON, compare mode (draw facing North, rotated by code) |
| `comparator_compare_off.png` | Comparator OFF, compare mode (draw facing North, rotated by code) |
| `comparator_subtract_on.png` | Comparator ON, subtract mode (draw facing North, rotated by code) |
| `comparator_subtract_off.png` | Comparator OFF, subtract mode (draw facing North, rotated by code) |
| `torch_attached_on.png` | Torch ON, wall-mounted (draw facing South/attached to block below, rotated by code) |
| `torch_attached_off.png` | Torch OFF, wall-mounted (draw facing South/attached to block below, rotated by code) |
| `torch_standing_on.png` | Torch ON, standing (no wall attachment) |
| `torch_standing_off.png` | Torch OFF, standing (no wall attachment) |
| `lever_on.png` | Lever ON, wall-mounted (draw facing South/attached to block below, rotated by code) |
| `lever_off.png` | Lever OFF, wall-mounted (draw facing South/attached to block below, rotated by code) |
| `lever_on_standing.png` | Lever ON, standing (no wall attachment) |
| `lever_off_standing.png` | Lever OFF, standing (no wall attachment) |
| *(none)* | Buttons sample the placed vanilla item's material texture; observers use the vanilla observer top-face texture; the Signal Source tile is drawn with GUI shapes and text. None needs a custom GUI PNG. |

The Signal Source item uses `assets/praeteritum_futurum/textures/item/signal_source.png`. This icon is only for the inventory item; the grid tile is rendered programmatically.

Directional custom textures are rotated programmatically. Torches and levers have a 180° rotation offset so the base texture faces South (attached to block below). The renderer uses Minecraft's built-in redstone-block sprite, so no custom `redstone_block.png` texture is required.

## Network Protocol

Configuration actions are sent via `ServerboundRedstoneConfigPacket`:
- `ACTION_TOGGLE_LEVER` - Toggle lever ON/OFF
- `ACTION_ROTATE_COMPONENT` - Rotate repeater/comparator/observer 90° clockwise
- `ACTION_ROTATE_TORCH` - Cycle torch attachment direction
- `ACTION_CYCLE_LEVER_ATTACHMENT` - Cycle lever attachment direction
- `ACTION_TOGGLE_COMPARATOR_MODE` - Toggle comparator between compare and subtract modes
- `ACTION_CYCLE_REPEATER_DELAY` - Cycle repeater delay through 1-4 redstone ticks
- `ACTION_PRESS_BUTTON` - Press a button until its material-specific pulse expires
- `ACTION_CYCLE_BUTTON_ATTACHMENT` - Cycle button support direction
- `ACTION_SET_SIGNAL_SOURCE_STRENGTH` - Set the Signal Source output to a validated strength from 0 to 15

The server processes the action, updates the circuit, and sends back a `ClientboundRedstoneCircuitStatePacket` with the new state.

## Persistence

Circuit data is serialized to NBT and stored in the module's `CustomData` component. Computed slot signal strengths are not persisted; they are recomputed on the next tick. Latched comparator output and its pending scheduled time are persisted, while its desired target is recalculated from inputs. Per-position shared-wall output strengths are persisted so adjacent modules can read the last computed boundary state. Each slot stores:
- `Index` - slot position (0-53)
- `Type` - component type ID
- `Rotation` - component rotation (0-3)
- `TorchRotation` - torch attachment direction (0-4)
- `TorchPowered` - latched ON/OFF state for attached torches
- `TorchScheduledGameTime` - due time for its pending transition or burnout retry, or -1
- `TorchBurnedOut` / `TorchBurnoutUntil` - burnout state and retry time
- `TorchRecentOffTimes` - recent turn-off times used for burnout detection
- `TorchLastInputStrength` - last observed support input, used to detect support updates
- `Delay` - repeater delay (1-4)
- `RepeaterPowered` - latched repeater output state
- `RepeaterScheduledGameTime` - due time for its pending scheduled tick, or -1
- `Locked` - repeater lock state
- `LockOutput` - repeater frozen output value
- `Subtract` - comparator subtract mode
- `ComparatorOutputSignal` - currently latched comparator output strength
- `ComparatorScheduledGameTime` - due time for its pending two-game-tick update, or -1
- `LeverOn` - lever ON/OFF state
- `LeverRotation` - lever attachment direction (0-4)
- `ButtonPowered` - whether the button is currently pressed
- `ButtonScheduledGameTime` - due time for its scheduled release, or -1
- `ButtonDurationGameTicks` - pulse duration (20 for stone/blackstone, 30 for wood)
- `ButtonRotation` - attachment direction (0-3 for adjacent walls, 4 for floor-perched)
- `ObserverPowered` - whether the observer output pulse is active
- `ObserverScheduledGameTime` - due time for its next pulse transition, or -1
- `ObserverLastObservedRotation` / `ObserverObservedStateSignature` - watched direction and last stable observed state
- `SignalSourceStrength` - configured analog output strength (0-15)
- `BoundaryWallOutputs` - per-position output strengths for the top, bottom, left, and right shared walls

## Behavior References

These sources describe the vanilla behaviors used as reference points. The grid-specific differences and simplifications are listed separately below.

- [Minecraft Wiki: Redstone dust](https://minecraft.wiki/w/Redstone_dust)
- [Minecraft Wiki: Redstone repeater](https://minecraft.wiki/w/Redstone_Repeater)
- [Minecraft Wiki: Redstone comparator](https://minecraft.wiki/w/Redstone_Comparator)
- [Minecraft Wiki: Redstone torch](https://minecraft.wiki/w/Redstone_Torch)
- [Minecraft Wiki: Button](https://minecraft.wiki/w/Button)
- [Minecraft Wiki: Observer](https://minecraft.wiki/w/Observer)
- [NeoForge 1.21.1: Blocks and scheduled ticks](https://docs.neoforged.net/docs/1.21.1/blocks)

## Automated GameTest Coverage

Run the registered server-side GameTests from the project root with `.\gradlew.bat runGameTestServer`. The suite is registered in `PraeteritumFuturumMod.registerGameTests()` and currently includes:

- `RedstoneCircuitGameTests` for component logic, signal strength, timing, persistence, and circuit edge outputs
- `MachineFrameworkBlockEntityGameTests` for module coordination, shared-wall links, and machine redstone behavior
- `ProcessingModuleGameTests` and `EjectionModuleGameTests` for module control policies and ejection pulse behavior
- `BlueprintItemGameTests` for required materials, paste behavior, and runtime-state clearing
- `ModuleMenuProviderGameTests` for saving Filter and Redstone menu contents

These tests verify server-side logic and data handling. They do not replace the client playtest scenarios in [REDSTONE-TESTS.md](REDSTONE-TESTS.md), which cover artwork, screen interactions, and multi-client synchronization.

## Differences from Vanilla Minecraft

The redstone system is a 2D simplification of vanilla Minecraft redstone with some creative adaptations:

1. **Standing torches, levers, and buttons** - Standing torches are a creative addition; the grid's floor-perched lever/button modes simplify attachment behavior.
2. **2D connectivity** - Dust connection shapes and same-level four-direction links are modeled, but vanilla vertical/stair-step dust, varying block conductivity, and the optional dot toggle are not. `WALL` is one abstract block type, not every vanilla block's exact behavior.
3. **Border walls** - The 4 grid edges act as implicit walls that torches, levers, and buttons can attach to. The basic I/O world input is a shared bus, not four separate edge-specific inputs.
4. **Inter-module communication** - Adjacent redstone modules communicate through slot-aligned shared borders and settle together. Feedback is bounded rather than processed using vanilla update order.
5. **Conductivity simplifications** - The grid skips `WALL` cells when torches and several weak sources emit. `WALL` transmission and source behavior are explicit grid rules, not a complete model of every vanilla block's conductivity.
6. **Component timing and attachment** - Repeater delays, comparator updates, attached-torch transitions, button release, and observer pulses use the world's scheduled block-tick phase. Observers detect a stable grid-cell state signature after shared-module settling rather than Minecraft's full neighbor/shape-update graph. Buttons model horizontal support attachment and strong power to their support; the grid's floor-perched state is a 2D simplification and ceiling placement is not represented. A two-tick repeater-clock GameTest matches the vanilla circuit's per-game-tick output timeline; exact lamp response, per-block tick priorities for complex simultaneous events, and all update-order nuances still need broader validation.
7. **Torch burnout retry** - The grid applies the >8 turn-offs in a 60-game-tick threshold, retaining entries exactly 60 ticks old. It retries on a support-input update once fewer than eight turn-offs remain and also makes one scheduled attempt after 160 game ticks. Vanilla may or may not perform that 160-tick attempt depending on the update chain, and may relight from other adjacent block updates that this grid does not model. Standing torches do not burn out.

## Reference

- [Minecraft Wiki: Redstone Dust](https://minecraft.wiki/w/Redstone_Dust): Dust signal decay, connection shapes, and power transmission.
- [Minecraft Wiki: Redstone Components](https://minecraft.wiki/w/Redstone_components): Which power and transmission components dust connects to.
- [Minecraft Wiki: Redstone Repeater](https://minecraft.wiki/w/Redstone_Repeater): Repeater delay settings, scheduled ticks, and pulse-length behavior.
- [Minecraft Wiki: Redstone Torch](https://minecraft.wiki/w/Redstone_Torch): Torch transition delay and burnout behavior.
- [Minecraft Wiki: Button](https://minecraft.wiki/w/Button): Stone/blackstone and wooden button pulse durations and power behavior.
- [Minecraft Wiki: Observer](https://minecraft.wiki/w/Observer): Watched block-state changes, one-redstone-tick delay, and two-game-tick output pulse.
