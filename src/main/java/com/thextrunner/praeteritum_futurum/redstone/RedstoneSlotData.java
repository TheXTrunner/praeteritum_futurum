package com.thextrunner.praeteritum_futurum.redstone;

/**
 * Holds all configuration data for a single slot in the redstone circuit grid.
 * <p>
 * This is the Logic Layer's per-slot data structure. It mirrors how vanilla Minecraft stores
 * block state properties: each block type defines its own properties, and unused properties
 * are simply ignored. For example, a wire doesn't use rotation, but a repeater does.
 * <p>
 * <b>Three-Layer Architecture:</b> This class lives in the Logic Layer. The Inventory Layer
 * stores {@link net.minecraft.world.item.ItemStack} objects in menu slots. When circuit
 * evaluation is triggered, inventory items are synced to this data structure and propagation
 * computes signal strengths. The Render Layer reads from a snapshot of this data via
 * {@link RedstoneCircuitState} — it never touches this class directly.
 * <p>
 * Fields and which components use them:
 * <ul>
 *   <li>{@code type} — all components; null means the slot is empty</li>
 *   <li>{@code rotation} — REPEATER, COMPARATOR, OBSERVER (0=N, 1=E, 2=S, 3=W)</li>
 *   <li>{@code torchRotation} — TORCH support direction (0=N, 1=E, 2=S, 3=W; 4=standing)</li>
 *   <li>{@code torchPowered} — TORCH; currently latched lit state</li>
 *   <li>{@code torchScheduledGameTime} — TORCH; due time of its pending update</li>
 *   <li>{@code torchBurnedOut} / {@code torchBurnoutUntil} — TORCH; burnout state and retry time</li>
 *   <li>{@code torchLastInputStrength} — TORCH; last observed support input for update detection</li>
 *   <li>{@code torchRecentOffTimes} — TORCH; recent turn-off times for burnout detection</li>
 *   <li>{@code repeaterDelay} — REPEATER (1-4 redstone ticks)</li>
 *   <li>{@code repeaterPowered} — REPEATER; the currently latched output state</li>
 *   <li>{@code repeaterScheduledGameTime} — REPEATER; due time of its pending scheduled tick</li>
 *   <li>{@code repeaterLocked} — REPEATER; true when side-locked by another repeater/comparator output</li>
 *   <li>{@code repeaterLockOutput} — REPEATER; the frozen output value (ON/OFF) while locked</li>
 *   <li>{@code comparatorSubtract} — COMPARATOR (false=compare, true=subtract)</li>
 *   <li>{@code comparatorScheduledGameTime} — COMPARATOR; due time of its pending two-game-tick update</li>
 *   <li>{@code comparatorTargetSignal} — COMPARATOR; latest output calculated from current inputs</li>
 *   <li>{@code leverOn} — LEVER (true=on, false=off)</li>
 *   <li>{@code leverRotation} — LEVER support direction or standing mode (0-4)</li>
 *   <li>{@code buttonPowered} — BUTTON; currently pressed state</li>
 *   <li>{@code buttonScheduledGameTime} — BUTTON; due time to release the button</li>
 *   <li>{@code buttonDurationGameTicks} — BUTTON; pulse duration from its stone/wood variant</li>
 *   <li>{@code buttonRotation} — BUTTON; selected support direction or floor-perched mode</li>
 *   <li>{@code observerPowered} — OBSERVER; current output-pulse state</li>
 *   <li>{@code observerScheduledGameTime} — OBSERVER; due time of its next pulse transition</li>
 *   <li>{@code observerLastObservedRotation} / {@code observerObservedStateSignature} — OBSERVER; stable watched-state tracking</li>
 *   <li>{@code signalSourceStrength} — SIGNAL_SOURCE; configured output strength (0-15)</li>
 *   <li>{@code signalStrength} — current strength computed by propagation (0-15)</li>
 *   <li>{@code outputSignal} — current directional output for REPEATER, COMPARATOR, and OBSERVER</li>
 *   <li>{@code inputStrength} — latest rear/support input for REPEATER, COMPARATOR, and attached TORCH</li>
 * </ul>
 */
public class RedstoneSlotData {

    /** Component type (null if slot is empty). The single source of truth for what's in this slot. */
    public RedstoneComponent type;

    /** Rotation 0-3 (N/E/S/W) — used by REPEATER, COMPARATOR. 0=NORTH, 1=EAST, 2=SOUTH, 3=WEST. */
    public int rotation;

    /** Torch support direction 0-4 (N/E/S/W/Standing) — 0-3 = wall-mounted, 4 = standing. Default is 4. */
    public int torchRotation;

    /** Current latched torch state; standing torches remain lit regardless of this field. */
    public boolean torchPowered;

    /** Due world game time for the torch's pending state transition or burnout retry, or -1. */
    public long torchScheduledGameTime;

    /** Whether recent turn-offs currently prevent this torch from changing state. */
    public boolean torchBurnedOut;

    /** Due game time for its scheduled burnout retry, or -1 when no retry is pending. */
    public long torchBurnoutUntil;

    /** Game times of recent transitions to OFF, used by the rolling burnout window. */
    public long[] torchRecentOffTimes;

    /** Last support input strength observed during propagation; used to detect support updates. */
    public int torchLastInputStrength;

    /** Repeater delay in redstone ticks (1-4), each equivalent to two game ticks. */
    public int repeaterDelay;

    /** Current latched repeater output state; unlike the computed outputSignal, this persists between ticks. */
    public boolean repeaterPowered;

    /** Due world game time for the repeater's single pending scheduled tick, or -1 when none is pending. */
    public long repeaterScheduledGameTime;

    /** Whether the repeater is currently locked by side power from a repeater/comparator output. */
    public boolean repeaterLocked;

    /** The frozen output value when locked (true=ON/15, false=OFF/0) — preserved while locked. */
    public boolean repeaterLockOutput;


    /** Comparator mode — used by COMPARATOR (false=compare, true=subtract). */
    public boolean comparatorSubtract;

    /** Due world game time for the comparator's single pending update, or -1 when none is pending. */
    public long comparatorScheduledGameTime;

    /** Latest comparator output calculated from its current inputs; not the latched output. */
    public int comparatorTargetSignal;

    /** Lever on/off state — used by LEVER (true=on, false=off). */
    public boolean leverOn;

    /** Lever attachment direction 0-4 (N/E/S/W/Standing) — used by LEVER. 0-3 = wall-mounted, 4 = standing (no wall). Default is 4 (standing). */
    public int leverRotation;

    /** Whether the button is currently pressed. */
    public boolean buttonPowered;

    /** Due world game time for the button's scheduled release, or -1 when not pressed. */
    public long buttonScheduledGameTime;

    /** Button pulse duration in game ticks (20 for stone/blackstone, 30 for wood). */
    public int buttonDurationGameTicks;

    /** Button attachment direction 0-4 (N/E/S/W/floor-perched); 0-3 attach to adjacent walls. */
    public int buttonRotation;

    /** Whether the observer is currently emitting its two-game-tick output pulse. */
    public boolean observerPowered;

    /** Due world game time for the observer's next pulse transition, or -1 when none is pending. */
    public long observerScheduledGameTime;

    /** Last observer facing direction used to establish the watched-slot baseline. */
    public int observerLastObservedRotation;

    /** Last stable observed block-state signature; {@link Long#MIN_VALUE} means not initialized. */
    public long observerObservedStateSignature;

    /** Configured output strength (0-15) — used by SIGNAL_SOURCE. */
    public int signalSourceStrength;

    /** Current signal strength (0-15) — computed by propagation; not persisted in NBT. */
    public int signalStrength;

    /** Current output signal strength (0-15) for repeaters, comparators, and observers. */
    public int outputSignal;

    /** Latest rear/support input strength for repeaters, comparators, and attached torches. */
    public int inputStrength;

    /**
     * Creates an empty slot data.
     */
    public RedstoneSlotData() {
        this.type = null;
        this.rotation = 0;
        this.torchRotation = 4; // Default to standing
        this.torchPowered = true;
        this.torchScheduledGameTime = -1L;
        this.torchBurnedOut = false;
        this.torchBurnoutUntil = -1L;
        this.torchRecentOffTimes = new long[0];
        this.torchLastInputStrength = 0;
        this.repeaterDelay = 1;
        this.repeaterPowered = false;
        this.repeaterScheduledGameTime = -1L;
        this.repeaterLocked = false;
        this.repeaterLockOutput = false;
        this.comparatorSubtract = false;
        this.comparatorScheduledGameTime = -1L;
        this.comparatorTargetSignal = 0;
        this.leverOn = false;
        this.leverRotation = 4; // Default to standing
        this.buttonPowered = false;
        this.buttonScheduledGameTime = -1L;
        this.buttonDurationGameTicks = 20;
        this.buttonRotation = 4;
        this.observerPowered = false;
        this.observerScheduledGameTime = -1L;
        this.observerLastObservedRotation = -1;
        this.observerObservedStateSignature = Long.MIN_VALUE;
        this.signalSourceStrength = 0;
        this.signalStrength = 0;
    }

    /**
     * Creates slot data for a specific component with default values.
     */
    public RedstoneSlotData(RedstoneComponent type) {
        this.type = type;
        this.rotation = 0;
        this.torchRotation = type == RedstoneComponent.TORCH ? 4 : 0; // Torches default to standing
        this.torchPowered = true;
        this.torchScheduledGameTime = -1L;
        this.torchBurnedOut = false;
        this.torchBurnoutUntil = -1L;
        this.torchRecentOffTimes = new long[0];
        this.torchLastInputStrength = 0;
        this.repeaterDelay = 1;
        this.repeaterPowered = false;
        this.repeaterScheduledGameTime = -1L;
        this.repeaterLocked = false;
        this.repeaterLockOutput = false;
        this.comparatorSubtract = false;
        this.comparatorScheduledGameTime = -1L;
        this.comparatorTargetSignal = 0;
        this.leverOn = false;
        this.leverRotation = type == RedstoneComponent.LEVER ? 4 : 0; // Levers default to standing
        this.buttonPowered = false;
        this.buttonScheduledGameTime = -1L;
        this.buttonDurationGameTicks = 20;
        this.buttonRotation = type == RedstoneComponent.BUTTON ? 4 : 0;
        this.observerPowered = false;
        this.observerScheduledGameTime = -1L;
        this.observerLastObservedRotation = -1;
        this.observerObservedStateSignature = Long.MIN_VALUE;
        this.signalSourceStrength = 0;
        this.signalStrength = 0;
    }

    /**
     * Returns true if this slot is empty (no component placed).
     */
    public boolean isEmpty() {
        return type == null;
    }

    /**
     * Creates a deep copy of this slot data.
     */
    public RedstoneSlotData copy() {
        RedstoneSlotData copy = new RedstoneSlotData();
        copy.type = this.type;
        copy.rotation = this.rotation;
        copy.torchRotation = this.torchRotation;
        copy.torchPowered = this.torchPowered;
        copy.torchScheduledGameTime = this.torchScheduledGameTime;
        copy.torchBurnedOut = this.torchBurnedOut;
        copy.torchBurnoutUntil = this.torchBurnoutUntil;
        copy.torchRecentOffTimes = this.torchRecentOffTimes.clone();
        copy.torchLastInputStrength = this.torchLastInputStrength;
        copy.repeaterDelay = this.repeaterDelay;
        copy.repeaterPowered = this.repeaterPowered;
        copy.repeaterScheduledGameTime = this.repeaterScheduledGameTime;
        copy.repeaterLocked = this.repeaterLocked;
        copy.repeaterLockOutput = this.repeaterLockOutput;
        copy.comparatorSubtract = this.comparatorSubtract;
        copy.comparatorScheduledGameTime = this.comparatorScheduledGameTime;
        copy.comparatorTargetSignal = this.comparatorTargetSignal;
        copy.leverOn = this.leverOn;
        copy.leverRotation = this.leverRotation;
        copy.buttonPowered = this.buttonPowered;
        copy.buttonScheduledGameTime = this.buttonScheduledGameTime;
        copy.buttonDurationGameTicks = this.buttonDurationGameTicks;
        copy.buttonRotation = this.buttonRotation;
        copy.observerPowered = this.observerPowered;
        copy.observerScheduledGameTime = this.observerScheduledGameTime;
        copy.observerLastObservedRotation = this.observerLastObservedRotation;
        copy.observerObservedStateSignature = this.observerObservedStateSignature;
        copy.signalSourceStrength = this.signalSourceStrength;
        copy.signalStrength = this.signalStrength;
        copy.outputSignal = this.outputSignal;
        copy.inputStrength = this.inputStrength;
        return copy;
    }
}
