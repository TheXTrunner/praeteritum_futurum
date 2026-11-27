package com.thextrunner.praeteritum_futurum.redstone;

/**
 * Redstone component types available in the Redstone Module's circuit editor.
 * <p>
 * This enum is shared across all three layers:
 * <ul>
 *   <li><b>Inventory Layer</b> — maps vanilla ItemStacks to component types via
 *   {@link com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem#getComponentFromItem}</li>
 *   <li><b>Logic Layer</b> — stored in {@link RedstoneSlotData#type} to identify each slot's component</li>
 *   <li><b>Render Layer</b> — read from {@link RedstoneCircuitState} to select overlay textures and tooltip text</li>
 * </ul>
 * <p>
 * Each component has a display name, color, and signal behavior:
 * <ul>
 *   <li><b>Wire</b> — passes signal from neighbors, decays by 1 per block</li>
 *   <li><b>Repeater</b> — passes signal in one direction, outputs full strength (15), can be side-locked</li>
 *   <li><b>Comparator</b> — compares or subtracts side inputs from its rear input and outputs a signal strength</li>
 *   <li><b>Torch</b> — wall-mounted torches invert support power; the grid's standing state is always on</li>
 *   <li><b>Lever</b> — toggleable power source, outputs 15 when ON, 0 when OFF</li>
 *   <li><b>Button</b> — momentary power source, outputs 15 for its configured pulse duration</li>
 *   <li><b>Observer</b> — detects stable block-state changes in the slot it faces and emits a delayed pulse</li>
 *   <li><b>Signal Source</b> — configurable analog source, outputs a selected strength from 0 to 15</li>
 *   <li><b>Redstone Block</b> — constant power source, outputs 15</li>
 *   <li><b>Wall</b> — conductive support block, not an independent source; strong power can pass from it to adjacent dust</li>
 * </ul>
 */
public enum RedstoneComponent {
    WIRE("Wire", 0xFFFF0000, 0),
    REPEATER("Repeater", 0xFF00AA00, 1),
    COMPARATOR("Comparator", 0xFF0000AA, 2),
    TORCH("Torch", 0xFFFFFF00, 3),
    LEVER("Lever", 0xFFAA8800, 4),
    REDSTONE_BLOCK("Redstone Block", 0xFFFF0000, 5),
    WALL("Wall", 0xFF888888, 6),
    BUTTON("Button", 0xFFFFAA00, 7),
    OBSERVER("Observer", 0xFFAA0000, 8),
    SIGNAL_SOURCE("Signal Source", 0xFF55AAFF, 9);

    private final String displayName;
    private final int color;
    private final int id;

    RedstoneComponent(String displayName, int color, int id) {
        this.displayName = displayName;
        this.color = color;
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public int getId() {
        return id;
    }

    /**
     * Returns the component type for the given ID.
     */
    public static RedstoneComponent fromId(int id) {
        for (RedstoneComponent comp : values()) {
            if (comp.id == id) return comp;
        }
        return null;
    }
}
