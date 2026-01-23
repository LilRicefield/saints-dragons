package com.leon.saintsdragons.forge.platform;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge dragon attributes configuration.
 * Builds a ForgeConfigSpec for all dragon attributes including health, armor, speeds, abilities, and taming.
 */
public final class ForgeDragonAttributesConfig {
    public static ForgeConfigSpec ATTRIBUTES_SPEC;

    // Cindervane
    public static ForgeConfigSpec.DoubleValue CINDERVANE_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_ARMOR;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_MAGMA_VOLLEY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_HEARTY;

    // Raevyx
    public static ForgeConfigSpec.DoubleValue RAEVYX_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue RAEVYX_ARMOR;
    public static ForgeConfigSpec.DoubleValue RAEVYX_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_LIGHTNING_BEAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_HORN_GORE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BEAM_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BEAM_REGEN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.BooleanValue RAEVYX_LEGACY_TAMING;

    // Nulljaw
    public static ForgeConfigSpec.DoubleValue NULLJAW_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue NULLJAW_ARMOR;
    public static ForgeConfigSpec.DoubleValue NULLJAW_BITE_PHASE1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue NULLJAW_BITE_PHASE2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue NULLJAW_HORN_GORE_PHASE1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue NULLJAW_HORN_GORE_PHASE2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue NULLJAW_SWIM_SPEED;
    public static ForgeConfigSpec.DoubleValue NULLJAW_TAMING_CHANCE;
    public static ForgeConfigSpec.BooleanValue NULLJAW_LEGACY_TAMING;

    // Ignivorus
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ARMOR;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_BODY_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIREBALL_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_WING_SWIPE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_STOMP_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ULTIMATE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ULTIMATE_PENALTY_HEALTH;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.BooleanValue IGNIVORUS_LEGACY_TAMING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Cindervane Configuration
        builder.comment("Cindervane Dragon Attributes").push("cindervane");
        builder.comment("Core Attributes");
        CINDERVANE_MAX_HEALTH = builder.defineInRange("max_health", 80.0, 1.0, 1000.0);
        CINDERVANE_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 30.0);
        CINDERVANE_FLYING_SPEED = builder.defineInRange("flying_speed", 0.60, 0.0, 2.0);
        builder.comment("Ability Damage");
        CINDERVANE_BITE_DAMAGE = builder.defineInRange("bite_damage", 12.0, 0.0, 100.0);
        CINDERVANE_MAGMA_VOLLEY_DAMAGE = builder.defineInRange("magma_volley_damage", 20.0, 0.0, 100.0);
        builder.comment("Taming Chances (lower = easier)");
        CINDERVANE_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 4.0, 1.0, 20.0);
        CINDERVANE_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 2.0, 1.0, 20.0);
        builder.pop();

        // Raevyx Configuration
        builder.comment("Raevyx Dragon Attributes").push("raevyx");
        builder.comment("Core Attributes");
        RAEVYX_MAX_HEALTH = builder.defineInRange("max_health", 180.0, 1.0, 1000.0);
        RAEVYX_ARMOR = builder.defineInRange("armor", 8.0, 0.0, 30.0);
        RAEVYX_FLYING_SPEED = builder.defineInRange("flying_speed", 1.0, 0.0, 2.0);
        builder.comment("Ability Damage");
        RAEVYX_BITE_DAMAGE = builder.defineInRange("bite_damage", 15.0, 0.0, 100.0);
        RAEVYX_LIGHTNING_BEAM_DAMAGE = builder.defineInRange("lightning_beam_damage", 35.0, 0.0, 100.0);
        RAEVYX_HORN_GORE_DAMAGE = builder.defineInRange("horn_gore_damage", 15.0, 0.0, 100.0);
        builder.comment("Beam Energy Tuning");
        RAEVYX_BEAM_DRAIN_PER_TICK = builder.defineInRange("beam_drain_per_tick", 0.014, 0.0, 1.0);
        RAEVYX_BEAM_REGEN_PER_TICK = builder.defineInRange("beam_regen_per_tick", 0.0025, 0.0, 1.0);
        builder.comment("Taming Chances (lower = easier)");
        RAEVYX_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 5.0, 1.0, 20.0);
        RAEVYX_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 3.0, 1.0, 20.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        RAEVYX_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.pop();

        // Nulljaw Configuration
        builder.comment("Nulljaw Dragon Attributes").push("nulljaw");
        builder.comment("Core Attributes");
        NULLJAW_MAX_HEALTH = builder.defineInRange("max_health", 250.0, 1.0, 1000.0);
        NULLJAW_ARMOR = builder.defineInRange("armor", 8.0, 0.0, 30.0);
        NULLJAW_SWIM_SPEED = builder.defineInRange("swim_speed", 1.45, 0.1, 5.0);
        builder.comment("Ability Damage");
        NULLJAW_BITE_PHASE1_DAMAGE = builder.defineInRange("bite_phase1_damage", 40.0, 0.0, 200.0);
        NULLJAW_BITE_PHASE2_DAMAGE = builder.defineInRange("bite_phase2_damage", 50.0, 0.0, 200.0);
        NULLJAW_HORN_GORE_PHASE1_DAMAGE = builder.defineInRange("horn_gore_phase1_damage", 16.0, 0.0, 200.0);
        NULLJAW_HORN_GORE_PHASE2_DAMAGE = builder.defineInRange("horn_gore_phase2_damage", 20.8, 0.0, 200.0);
        builder.comment("Taming Chance (lower = easier)");
        NULLJAW_TAMING_CHANCE = builder.defineInRange("taming_chance", 6.0, 1.0, 20.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        NULLJAW_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.pop();

        // Ignivorus Configuration
        builder.comment("Ignivorus Dragon Attributes").push("ignivorus");
        builder.comment("Core Attributes");
        IGNIVORUS_MAX_HEALTH = builder.defineInRange("max_health", 300.0, 1.0, 1000.0);
        IGNIVORUS_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 30.0);
        IGNIVORUS_FLYING_SPEED = builder.defineInRange("flying_speed", 0.40, 0.0, 2.0);
        builder.comment("Ability Damage");
        IGNIVORUS_BITE_DAMAGE = builder.defineInRange("bite_damage", 50.0, 0.0, 200.0);
        IGNIVORUS_BODY_SLAM_DAMAGE = builder.defineInRange("body_slam_damage", 40.0, 0.0, 200.0);
        IGNIVORUS_FIRE_BREATH_DAMAGE = builder.defineInRange("fire_breath_damage", 80.0, 0.0, 200.0);
        IGNIVORUS_FIREBALL_DAMAGE = builder.defineInRange("fireball_damage", 70.0, 0.0, 200.0);
        IGNIVORUS_WING_SWIPE_DAMAGE = builder.defineInRange("wing_swipe_damage", 15.0, 0.0, 200.0);
        IGNIVORUS_STOMP_DAMAGE = builder.defineInRange("stomp_damage", 18.0, 0.0, 200.0);
        IGNIVORUS_ULTIMATE_DAMAGE = builder.defineInRange("ultimate_damage", 200.0, 0.0, 500.0);
        builder.comment("Ultimate ability health penalty");
        IGNIVORUS_ULTIMATE_PENALTY_HEALTH = builder.defineInRange("ultimate_penalty_health", 50.0, 1.0, 500.0);
        builder.comment("Fire Breath Tuning");
        IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK = builder.defineInRange("fire_breath_drain_per_tick", 0.00625, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK = builder.defineInRange("fire_breath_regen_per_tick", 0.0025, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER = builder.defineInRange("fire_breath_flame_spawn_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER = builder.defineInRange("fire_breath_flame_speed_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER = builder.defineInRange("fire_breath_flame_lifetime_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE = builder.defineInRange("fire_breath_ignite_block_chance", 1.0, 0.0, 1.0);
        builder.comment("Taming Chances (lower = easier)");
        IGNIVORUS_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 7.0, 1.0, 20.0);
        IGNIVORUS_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 4.0, 1.0, 20.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        IGNIVORUS_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.pop();

        ATTRIBUTES_SPEC = builder.build();
    }

    private ForgeDragonAttributesConfig() {
    }
}
