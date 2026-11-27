# Redstone Module

The Redstone Module lets you build vanilla-inspired circuits inside a 9-by-6 grid. Use it for clocks, logic, signal routing, and controlling compatible modules in the same machine. The grid works with Minecraft redstone signals, but it does not model every vanilla behavior.

For hands-on checks and issue reporting, see the [Redstone Playtest Guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/REDSTONE-TESTS.md). For module setup and machine controls, see [[Modules]].

## The Circuit Grid

- The grid has 9 columns and 6 rows, for 54 component slots.
- Place redstone items into the grid with Config Mode off. The item stays in its slot while you configure it.
- With Config Mode on, clicks configure components instead of moving items. Turn it off again for normal inventory actions.
- Tooltips show signal strength, inputs, outputs, facing, attachment, or timing where relevant.
- Adjacent Redstone Modules in the same machine share a metaphorical wall. Signals cross at matching row or column positions, and the modules settle together as one circuit.

## Config Mode Controls

| Component | Left-click | Right-click |
|---|---|---|
| Lever | Toggle on or off | Cycle attachment direction |
| Repeater | Cycle delay from 1 to 4 | Rotate facing |
| Comparator | Switch Compare or Subtract mode | Rotate facing |
| Torch | No action | Cycle through nearby supports and standing mode |
| Button | Press | Cycle attachment direction |
| Observer | No action | Rotate watched direction |
| Signal Source | Open or update its strength editor | No action |

The Signal Source editor has a slider and `-` and `+` buttons for choosing an exact strength from 0 to 15. New Signal Sources start at 0, so they do not power the circuit until changed.

## Components

- **Redstone Dust:** Connects automatically to eligible neighboring components. Signal strength decreases by one when passing from one dust slot to another. The grid chooses the wire shape automatically.
- **Repeater:** Sends a full-strength signal in one direction after its selected delay. One setting is one redstone tick, or two game ticks, so the four settings are 2, 4, 6, and 8 game ticks. A repeater can be locked by a powered repeater or comparator output at its side.
- **Comparator:** Reads its rear input and side inputs. Side wire contributes its current strength; a side repeater or comparator must point into the comparator; strongly powered walls and adjacent redstone blocks also count. Torches and levers do not count directly. Compare mode passes the rear strength when it is at least as strong as the side input; Subtract mode outputs the rear strength minus the strongest side input, down to zero. Output changes are scheduled two game ticks later.
- **Redstone Torch:** A standing torch is always on in this grid. An attached torch reads the block or grid border it is attached to and inverts its power state. Attached torches have scheduled transitions and can burn out after repeated rapid toggles.
- **Lever:** A toggleable signal source. When attached to a wall, it also strongly powers that support; it can still power eligible neighboring circuit components.
- **Button:** A momentary signal source. Stone and polished blackstone buttons pulse for 20 game ticks. Wooden buttons pulse for 30 game ticks. The selected support can be a nearby wall or the grid border, or the button can be floor-perched.
- **Observer:** Watches the adjacent slot in its selected direction. A watched change produces a delayed pulse lasting two game ticks. The grid compares a simplified slot state rather than every vanilla block update.
- **Redstone Block:** A constant strength-15 source.
- **Scrap Block wall:** Acts as the grid's abstract wall cell. A powered wall can pass strong power to adjacent dust, but it does not represent every vanilla block's conductivity.
- **Signal Source:** A custom analog source that emits the chosen strength from 0 to 15.

## I/O Module and World Redstone

The I/O Module's face settings govern world redstone exchange as well as item transfer:

- **INPUT** reads redstone from that world face and does not emit the circuit output there.
- **OUTPUT** emits the circuit output as weak redstone power and does not read that face as input.
- **OPEN** reads and emits on the same face.
- **DISABLED** does neither.

The strongest signal from all enabled `INPUT` and `OPEN` faces becomes one shared input bus supplied to all four edges of the circuit grid. The basic I/O Module does not map each world face to a particular grid edge. A future upgraded I/O Module may offer that mapping.

For output, the machine uses the strongest aggregate signal from the Redstone Modules and exposes it on each face set to `OUTPUT` or `OPEN`. The machine provides weak redstone power, not direct strong power into the neighboring block.

`OPEN` is deliberately bidirectional. If you connect a machine's output back to an enabled input through world wiring, its signal can return to the circuit. That may be useful for feedback designs; use `INPUT` or `OUTPUT`, or add a repeater, when you want a one-way connection.

## Redstone Control for Modules

The redstone button in a supported module's configuration overlay controls whether its normal module tick runs:

- **IGNORE:** Always run. This is the default.
- **HIGH:** Run when the strongest eligible signal is above zero.
- **LOW:** Run when the strongest eligible signal is zero.
- **PULSE:** On the Ejection Module only, queue one ejection pass when the signal changes from off to on. Holding the signal on does not repeat the pulse.

Regular processing modules support `IGNORE`, `HIGH`, and `LOW`, except the Jukebox. The Ejection Module supports all four options. A paused processing module keeps its items and progress; separate I/O transfers may continue.

Control signals combine world input from I/O faces set to `INPUT` or `OPEN` with the facing edge signal from any immediately adjacent Redstone Module. A Redstone Module is not required for simple world-signal control. The basic Filter, I/O, Inventory, Redstone, and Jukebox modules do not have this control button.

## Blueprints and Saved Data

Blueprints can copy Redstone Module grids, component positions, and durable settings. Applying a Blueprint in Survival consumes the required clean module items and the exact component items stored in the copied Redstone Module grids. Hold Shift over a Blueprint in your inventory to see missing materials. Creative pasting is free. Active pulses and pending timing transitions are cleared when the Blueprint is applied. See [[Modules]] for the full Blueprint workflow.

The Data Cleaning recipe removes saved custom data. When cleaning a Redstone Module, its embedded components return to your inventory; any items that do not fit are dropped nearby. Cleaning a Blueprint removes its saved circuit data.

## Known Limits

- The grid is two-dimensional. It has no vertical or stair-step dust paths.
- `WALL` is one abstract component, not a complete simulation of every block's power behavior.
- Wire shape selection is automatic; the vanilla manual dot/cross toggle is not implemented.
- Timed components use Minecraft's scheduled block tick system, but the grid does not reproduce every vanilla update-order detail.
- Shared module boundaries settle in a bounded number of rounds. Torch burnout recovery is also a simplified model.
- Custom redstone components cannot currently be registered by other mods. The circuit implementation is internal.
- Multiplayer synchronization between two clients has not yet been verified. Please report your results or problems on the [GitHub Issues page](https://github.com/TheXTrunner/praeteritum_futurum/issues).

## References

- [Minecraft Wiki: Redstone dust](https://minecraft.wiki/w/Redstone_dust)
- [Minecraft Wiki: Redstone repeater](https://minecraft.wiki/w/Redstone_Repeater)
- [Minecraft Wiki: Redstone comparator](https://minecraft.wiki/w/Redstone_Comparator)
- [Minecraft Wiki: Redstone torch](https://minecraft.wiki/w/Redstone_Torch)
- [Minecraft Wiki: Button](https://minecraft.wiki/w/Button)
- [Minecraft Wiki: Observer](https://minecraft.wiki/w/Observer)