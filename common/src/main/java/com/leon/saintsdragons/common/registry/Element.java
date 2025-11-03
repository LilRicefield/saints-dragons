package com.leon.saintsdragons.common.registry;

/**
 * Elemental types for dragons.
 * Defines the elemental affinity of a dragon (what type of damage they deal/resist).
 */
public enum Element {
    /**
     * Lightning element - electrical attacks, benefits from storms
     */
    LIGHTNING("lightning", 0x7DD3FC),  // Cyan-blue

    /**
     * Fire element - flame attacks, benefits from heat
     */
    FIRE("fire", 0xFF6B35),  // Orange-red

    /**
     * No elemental affinity - pure physical attacks
     */
    NONE("none", 0xFFFFFF);  // White/neutral

    private final String name;
    private final int color;

    Element(String name, int color) {
        this.name = name;
        this.color = color;
    }

    /**
     * Get the display name of this element
     */
    public String getName() {
        return name;
    }

    /**
     * Get the color associated with this element (for particles, UI, etc.)
     */
    public int getColor() {
        return color;
    }

    /**
     * Check if this element is affected by weather conditions
     */
    public boolean isAffectedByWeather() {
        return this == LIGHTNING;
    }

    /**
     * Check if this element is affected by biome temperature
     */
    public boolean isAffectedByBiome() {
        return this == FIRE;
    }

    /**
     * Check if this is an elemental type (not physical/none)
     */
    public boolean isElemental() {
        return this != NONE;
    }
}
