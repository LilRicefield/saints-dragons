package com.leon.saintsdragons.server.entity.ability;

public abstract class DragonAbilitySection {
    public final AbilitySectionType sectionType;

    public DragonAbilitySection(AbilitySectionType sectionType) {
        this.sectionType = sectionType;
    }

    public enum AbilitySectionType {
        STARTUP,
        ACTIVE,
        RECOVERY
    }

    public static class AbilitySectionDuration extends DragonAbilitySection {
        public final int duration;

        public AbilitySectionDuration(AbilitySectionType sectionType, int duration) {
            super(sectionType);
            this.duration = duration;
        }
    }

    public static class AbilitySectionInstant extends DragonAbilitySection {
        public AbilitySectionInstant(AbilitySectionType sectionType) {
            super(sectionType);
        }
    }


    public static class AbilitySectionInfinite extends DragonAbilitySection {
        public AbilitySectionInfinite(AbilitySectionType sectionType) {
            super(sectionType);
        }
    }
}