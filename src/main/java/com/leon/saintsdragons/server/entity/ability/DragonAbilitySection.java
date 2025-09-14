package com.leon.saintsdragons.server.entity.ability;

/**
 * Ability section system for precise timing control
 */
public abstract class DragonAbilitySection {
    public final AbilitySectionType sectionType;

    public DragonAbilitySection(AbilitySectionType sectionType) {
        this.sectionType = sectionType;
    }

    public enum AbilitySectionType {
        STARTUP,    // Windup/preparation phase
        ACTIVE,     // Main action/damage phase
        RECOVERY    // Cooldown/return to idle phase
    }

    /**
     * Section with a specific duration in ticks
     */
    public static class AbilitySectionDuration extends DragonAbilitySection {
        public final int duration;

        public AbilitySectionDuration(AbilitySectionType sectionType, int duration) {
            super(sectionType);
            this.duration = duration;
        }
    }

    /**
     * Section that completes instantly (single tick)
     */
    public static class AbilitySectionInstant extends DragonAbilitySection {
        public AbilitySectionInstant(AbilitySectionType sectionType) {
            super(sectionType);
        }
    }

    /**
     * Section that continues indefinitely until manually ended
     */
    public static class AbilitySectionInfinite extends DragonAbilitySection {
        public AbilitySectionInfinite(AbilitySectionType sectionType) {
            super(sectionType);
        }
    }
}