package com.thextrunner.praeteritum_futurum.api.module;

/**
 * Interface for mode enums that support forward and backward cycling.
 * <p>
 * Implement this interface on your mode enum to enable universal shift-click
 * reverse cycling. The {@code next()} and {@code previous()} methods allow
 * the server-side packet handler to cycle modes in either direction without
 * knowing the specific enum type.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>{@code
 * public enum MyMode implements CyclingMode {
 *     DISABLED, INPUT, OUTPUT;
 *
 *     @Override public MyMode next() {
 *         return values()[(ordinal() + 1) % values().length];
 *     }
 *
 *     @Override public MyMode previous() {
 *         return values()[(ordinal() - 1 + values().length) % values().length];
 *     }
 * }
 * }</pre>
 * <p>
 * All built-in mode enums implement this: {@code SlotMode}, {@code JukeboxMode},
 * {@code InventoryMode}, {@code IoMode}, and {@code FaceMode}.
 * </p>
 *
 * @param <T> The enum type implementing this interface
 */
public interface CyclingMode<T> {
    /**
     * Returns the next mode in the cycle (forward order).
     * @return The next mode
     */
    T next();

    /**
     * Returns the previous mode in the cycle (reverse order).
     * Used for shift-click to cycle modes in reverse.
     * @return The previous mode
     */
    T previous();
}
