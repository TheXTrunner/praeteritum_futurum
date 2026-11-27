package com.thextrunner.praeteritum_futurum.api.module;

/**
 * Display metadata and generic slot behavior for a processing module's locally defined mode.
 */
public interface ProcessingMode {
    /** Generic slot roles used by the shared processing framework. */
    enum Role {
        DISABLED,
        INPUT,
        OUTPUT,
        OTHER
    }

    /** Returns the character rendered on the mode button. */
    String getLabelChar();

    /** Returns the button's ARGB color. */
    int getColor();

    /** Returns the module-specific tooltip description. */
    String getDescription();

    /** Returns the generic input/output role used for shared slot handling. */
    Role getRole();

    /** Returns the persisted/displayed name; enum implementations inherit their enum name. */
    default String getName() {
        if (this instanceof Enum<?> mode) {
            return mode.name();
        }
        return toString();
    }
}