package com.thextrunner.praeteritum_futurum.api.module;

/**
 * Redstone component types available in the Redstone Module's circuit editor.
 * <p>
 * Each component has a display name, color, and signal behavior:
 * - Wire: passes signal from neighbors, decays by 1 per block
 * - Repeater: delays signal by configurable ticks (1-4), maintains signal strength
 * - Comparator: compares input signals, outputs difference
 * - Torch: inverts signal (NOT gate), outputs 15 when input is 0
 */
public enum RedstoneComponent {
    WIRE("Wire", 0xFFFF0000, 0),
    REPEATER("Repeater", 0xFF00AA00, 1),
    COMPARATOR("Comparator", 0xFF0000AA, 2),
    TORCH("Torch", 0xFFFFFF00, 3);

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
        return WIRE;
    }
}
