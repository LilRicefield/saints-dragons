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
    public static ForgeConfigSpec.DoubleValue CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_SLASH_GRAB_HIT1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_SLASH_GRAB_HIT2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_MAGMA_VOLLEY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_CHICKEN;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH;
    public static ForgeConfigSpec.BooleanValue CINDERVANE_AGGRESSIVE_WILD;

    // Raevyx
    public static ForgeConfigSpec.DoubleValue RAEVYX_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue RAEVYX_ARMOR;
    public static ForgeConfigSpec.DoubleValue RAEVYX_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue RAEVYX_WILD_FLYING_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_LIGHTNING_BEAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_HORN_GORE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_DASH_DAMAGE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BEAM_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue RAEVYX_BEAM_REGEN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue RAEVYX_SUMMON_STORM_COOLDOWN_TICKS;
    public static ForgeConfigSpec.DoubleValue RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS;
    public static ForgeConfigSpec.DoubleValue RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue RAEVYX_SUMMON_STORM_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.BooleanValue RAEVYX_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_LOOT_PILLAGER_OUTPOST;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_LOOT_ANCIENT_CITY;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue RAEVYX_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.BooleanValue RAEVYX_AGGRESSIVE_WILD;

    // Varasuchus
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_ARMOR;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_BITE_PHASE1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_BITE_PHASE2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAIL_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAILGUARD_PARRY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_DASH_CLAW_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_CLAW_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_HORN_GORE_PHASE1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_HORN_GORE_PHASE2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_SWIM_SPEED;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAMING_CHANCE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAMING_CHANCE_TROPICAL;
    public static ForgeConfigSpec.BooleanValue VARASUCHUS_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.BooleanValue VARASUCHUS_AGGRESSIVE_WILD;

    // Ignivorus
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ARMOR;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_BODY_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_LEAP_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIREBALL_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_MAGMA_PILLAR_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_WING_SWIPE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_STOMP_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_BULLDOZE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ULTIMATE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ULTIMATE_PENALTY_HEALTH;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_PHASE2_DECISION_MIN_TICKS;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_PHASE2_DECISION_MAX_TICKS;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_BEEF;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.BooleanValue IGNIVORUS_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_LOOT_BASTION_TREASURE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_LOOT_NETHER_BRIDGE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_LOOT_ANCIENT_CITY;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.BooleanValue IGNIVORUS_AGGRESSIVE_WILD;

    // Stegonaut
    public static ForgeConfigSpec.DoubleValue STEGONAUT_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_ARMOR;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_CHIN_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_EATING_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.BooleanValue STEGONAUT_AGGRESSIVE_WILD;

    // Volitans
    public static ForgeConfigSpec.DoubleValue VOLITANS_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ARMOR;
    public static ForgeConfigSpec.DoubleValue VOLITANS_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue VOLITANS_WILD_FLYING_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_CLAW_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_HORN_GORE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_GROUND_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_AIR_WATER_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BURROW_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BALL_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_WATER_BREATH_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BREATH_DAMAGE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue VOLITANS_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.BooleanValue VOLITANS_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue VOLITANS_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_EGG_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_SCALE_DROP_CHANCE_BRUSH;
    public static ForgeConfigSpec.DoubleValue VOLITANS_SPINE_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_FISH_DROP_CHANCE;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_ACTIVE_TICKS_MAX;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_REGEN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_PROJECTILE_SPREAD;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_PROJECTILE_SPEED;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_PROJECTILE_LIFETIME;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BREATH_POISON_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BREATH_POISON_LEVEL;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BALL_POISON_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue VOLITANS_POISON_BALL_POISON_LEVEL;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_GROUND_POISON_LEVEL;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ROAR_AIR_WATER_POISON_LEVEL;
    public static ForgeConfigSpec.BooleanValue VOLITANS_AGGRESSIVE_WILD;

    // Nulljaw
    public static ForgeConfigSpec.DoubleValue NULLJAW_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue NULLJAW_ARMOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Cindervane Configuration
        builder.comment("Cindervane Dragon Attributes").push("cindervane");
        builder.comment("Core Attributes");
        CINDERVANE_MAX_HEALTH = builder.defineInRange("max_health", 80.0, 1.0, 100000.0);
        CINDERVANE_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 100000.0);
        builder.comment("Base rider flying speed");
        CINDERVANE_FLYING_SPEED = builder.defineInRange("flying_speed", 0.60, 0.0, 2.0);
        builder.comment("Multiplier for wild flying speed only");
        CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", 1.0, 0.05, 10.0);
        builder.comment("Ability Damage");
        CINDERVANE_BITE_DAMAGE = builder.defineInRange("bite_damage", 12.0, 0.0, 100000.0);
        CINDERVANE_SLASH_GRAB_HIT1_DAMAGE = builder.defineInRange("slash_grab_hit1_damage", 5.0, 0.0, 100000.0);
        CINDERVANE_SLASH_GRAB_HIT2_DAMAGE = builder.defineInRange("slash_grab_hit2_damage", 7.0, 0.0, 100000.0);
        CINDERVANE_MAGMA_VOLLEY_DAMAGE = builder.defineInRange("magma_volley_damage", 20.0, 0.0, 100000.0);
        CINDERVANE_FIRE_BODY_DAMAGE = builder.defineInRange("fire_body_damage", 3.0, 0.0, 100000.0);
        builder.comment("Taming chance percent per feed (0-100)");
        CINDERVANE_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 25.0, 0.0, 100.0);
        CINDERVANE_TAMING_CHANCE_CHICKEN = builder.defineInRange("taming_chance_chicken", 33.3333, 0.0, 100.0);
        CINDERVANE_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 50.0, 0.0, 100.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        CINDERVANE_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 12000.0, 20.0, 72000.0);
        builder.comment("Egg drop chance (0-1)");
        CINDERVANE_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Scale drop chance when brushed (0-1)");
        CINDERVANE_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.30, 0.0, 1.0);
        builder.comment("Direct blast damage on Fire Body crash impact");
        CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE = builder.defineInRange("fire_body_explosion_damage", 200.0, 0.0, 100000.0);
        builder.comment("Self-damage applied to Cindervane after Fire Body crash impact");
        CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH = builder.defineInRange("fire_body_self_damage_on_crash", 40.0, 0.0, 100000.0);
        builder.comment("Aggressive wild behavior");
        CINDERVANE_AGGRESSIVE_WILD = builder.define("aggressive_wild", false);
        builder.pop();

        // Raevyx Configuration
        builder.comment("Raevyx Dragon Attributes").push("raevyx");
        builder.comment("Core Attributes");
        RAEVYX_MAX_HEALTH = builder.defineInRange("max_health", 180.0, 1.0, 100000.0);
        RAEVYX_ARMOR = builder.defineInRange("armor", 8.0, 0.0, 100000.0);
        builder.comment("Base rider flying speed");
        RAEVYX_FLYING_SPEED = builder.defineInRange("flying_speed", 0.5, 0.0, 2.0);
        builder.comment("Multiplier for wild flying speed only");
        RAEVYX_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", 1.0, 0.05, 10.0);
        builder.comment("Ability Damage");
        RAEVYX_BITE_DAMAGE = builder.defineInRange("bite_damage", 15.0, 0.0, 100000.0);
        RAEVYX_LIGHTNING_BEAM_DAMAGE = builder.defineInRange("lightning_beam_damage", 35.0, 0.0, 100000.0);
        RAEVYX_HORN_GORE_DAMAGE = builder.defineInRange("horn_gore_damage", 15.0, 0.0, 100000.0);
        RAEVYX_DASH_DAMAGE = builder.defineInRange("dash_damage", 10.0, 0.0, 100000.0);
        builder.comment("Beam Energy Tuning");
        RAEVYX_BEAM_DRAIN_PER_TICK = builder.defineInRange("beam_drain_per_tick", 0.014, 0.0, 1.0);
        RAEVYX_BEAM_REGEN_PER_TICK = builder.defineInRange("beam_regen_per_tick", 0.0025, 0.0, 1.0);
        builder.comment("Summon Storm Tuning");
        RAEVYX_SUMMON_STORM_COOLDOWN_TICKS = builder.defineInRange("summon_storm_cooldown_ticks", 4800.0, 20.0, 120000.0);
        RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS = builder.defineInRange("summon_storm_supercharge_ticks", 1200.0, 20.0, 120000.0);
        RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER = builder.defineInRange("summon_storm_supercharge_damage_multiplier", 2.0, 0.0, 100.0);
        RAEVYX_SUMMON_STORM_DURATION_TICKS = builder.defineInRange("summon_storm_duration_ticks", 1200.0, 20.0, 120000.0);
        builder.comment("Taming chance percent per feed (0-100)");
        RAEVYX_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 20.0, 0.0, 100.0);
        RAEVYX_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 33.3333, 0.0, 100.0);
        builder.comment("Taming stun health threshold (HP)");
        RAEVYX_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", 60.0, 0.0, 1000.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 18000.0, 20.0, 72000.0);
        RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER = builder.defineInRange("egg_hatch_time_ticks_thunder", 9600.0, 20.0, 72000.0);
        builder.comment("Egg loot chances (0-1)");
        RAEVYX_EGG_LOOT_PILLAGER_OUTPOST = builder.defineInRange("egg_loot_pillager_outpost", 0.20, 0.0, 1.0);
        RAEVYX_EGG_LOOT_ANCIENT_CITY = builder.defineInRange("egg_loot_ancient_city", 0.15, 0.0, 1.0);
        builder.comment("Egg drop chance (0-1)");
        RAEVYX_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Scale drop chance when brushed (0-1)");
        RAEVYX_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.35, 0.0, 1.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        RAEVYX_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.comment("Aggressive wild behavior");
        RAEVYX_AGGRESSIVE_WILD = builder.define("aggressive_wild", false);
        builder.pop();

        // Varasuchus Configuration
        builder.comment("Varasuchus Dragon Attributes").push("varasuchus");
        builder.comment("Core Attributes");
        VARASUCHUS_MAX_HEALTH = builder.defineInRange("max_health", 200.0, 1.0, 100000.0);
        VARASUCHUS_ARMOR = builder.defineInRange("armor", 8.0, 0.0, 100000.0);
        VARASUCHUS_SWIM_SPEED = builder.defineInRange("swim_speed", 1.45, 0.1, 5.0);
        builder.comment("Ability Damage");
        VARASUCHUS_BITE_PHASE1_DAMAGE = builder.defineInRange("bite_phase1_damage", 15.0, 0.0, 100000.0);
        VARASUCHUS_BITE_PHASE2_DAMAGE = builder.defineInRange("bite_phase2_damage", 25.0, 0.0, 100000.0);
        VARASUCHUS_TAIL_ATTACK_DAMAGE = builder.defineInRange("tail_attack_damage", 7.0, 0.0, 100000.0);
        VARASUCHUS_TAILGUARD_PARRY_DAMAGE = builder.defineInRange("tailguard_parry_damage", 10.0, 0.0, 100000.0);
        VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE = builder.defineInRange("dash_tail_swipe_damage", 10.0, 0.0, 100000.0);
        VARASUCHUS_DASH_CLAW_DAMAGE = builder.defineInRange("dash_claw_damage", 15.0, 0.0, 100000.0);
        VARASUCHUS_CLAW_ATTACK_DAMAGE = builder.defineInRange("claw_attack_damage", 8.0, 0.0, 100000.0);
        VARASUCHUS_HORN_GORE_PHASE1_DAMAGE = builder.defineInRange("horn_gore_phase1_damage", 8.0, 0.0, 100000.0);
        VARASUCHUS_HORN_GORE_PHASE2_DAMAGE = builder.defineInRange("horn_gore_phase2_damage", 15.8, 0.0, 100000.0);
        builder.comment("Base taming chance percent. Legacy food taming rolls it directly; rodeo taming converts it into a smaller per-tick chance");
        VARASUCHUS_TAMING_CHANCE = builder.defineInRange("taming_chance", 16.6667, 0.0, 100.0);
        VARASUCHUS_TAMING_CHANCE_TROPICAL = builder.defineInRange("taming_chance_tropical", 25.0, 0.0, 100.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        VARASUCHUS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 24000.0, 20.0, 72000.0);
        builder.comment("Egg drop chance (0-1)");
        VARASUCHUS_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Scale drop chance when brushed (0-1)");
        VARASUCHUS_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.30, 0.0, 1.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        VARASUCHUS_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.comment("Aggressive wild behavior");
        VARASUCHUS_AGGRESSIVE_WILD = builder.define("aggressive_wild", true);
        builder.pop();

        // Ignivorus Configuration
        builder.comment("Ignivorus Dragon Attributes").push("ignivorus");
        builder.comment("Core Attributes");
        IGNIVORUS_MAX_HEALTH = builder.defineInRange("max_health", 450.0, 1.0, 100000.0);
        IGNIVORUS_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 100000.0);
        builder.comment("Base rider flying speed");
        IGNIVORUS_FLYING_SPEED = builder.defineInRange("flying_speed", 0.35, 0.0, 2.0);
        builder.comment("Multiplier for wild flying speed only");
        IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", 1.0, 0.05, 10.0);
        builder.comment("Ability Damage");
        IGNIVORUS_BITE_DAMAGE = builder.defineInRange("bite_damage", 50.0, 0.0, 100000.0);
        IGNIVORUS_BODY_SLAM_DAMAGE = builder.defineInRange("body_slam_damage", 40.0, 0.0, 100000.0);
        IGNIVORUS_LEAP_SLAM_DAMAGE = builder.defineInRange("leap_slam_damage", 50.0, 0.0, 100000.0);
        IGNIVORUS_FIRE_BREATH_DAMAGE = builder.defineInRange("fire_breath_damage", 80.0, 0.0, 100000.0);
        IGNIVORUS_FIREBALL_DAMAGE = builder.defineInRange("fireball_damage", 70.0, 0.0, 100000.0);
        IGNIVORUS_MAGMA_PILLAR_DAMAGE = builder.defineInRange("magma_pillar_damage", 18.0, 0.0, 100000.0);
        IGNIVORUS_WING_SWIPE_DAMAGE = builder.defineInRange("wing_swipe_damage", 15.0, 0.0, 100000.0);
        IGNIVORUS_STOMP_DAMAGE = builder.defineInRange("stomp_damage", 18.0, 0.0, 100000.0);
        IGNIVORUS_BULLDOZE_DAMAGE = builder.defineInRange("bulldoze_damage", 10.0, 0.0, 100000.0);
        IGNIVORUS_ULTIMATE_DAMAGE = builder.defineInRange("ultimate_damage", 200.0, 0.0, 100000.0);
        builder.comment("Ultimate ability health penalty");
        IGNIVORUS_ULTIMATE_PENALTY_HEALTH = builder.defineInRange("ultimate_penalty_health", 50.0, 1.0, 10000.0);
        builder.comment("Health fraction threshold for enabling the ultimate ability");
        IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION = builder.defineInRange("ultimate_trigger_health_fraction", 0.5, 0.0, 1.0);
        builder.comment("Fire Breath Tuning");
        IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK = builder.defineInRange("fire_breath_drain_per_tick", 0.00625, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK = builder.defineInRange("fire_breath_regen_per_tick", 0.0025, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER = builder.defineInRange("fire_breath_flame_spawn_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER = builder.defineInRange("fire_breath_flame_speed_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER = builder.defineInRange("fire_breath_flame_lifetime_multiplier", 1.0, 0.0, 5.0);
        IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE = builder.defineInRange("fire_breath_ignite_block_chance", 1.0, 0.0, 1.0);
        builder.comment("AI Phase 2 behavior (grounded-only switching)");
        IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE = builder.defineInRange("phase2_toggle_on_chance", 0.85, 0.0, 1.0);
        IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE = builder.defineInRange("phase2_toggle_off_chance", 0.05, 0.0, 1.0);
        IGNIVORUS_PHASE2_DECISION_MIN_TICKS = builder.defineInRange("phase2_decision_min_ticks", 60.0, 1.0, 1200.0);
        IGNIVORUS_PHASE2_DECISION_MAX_TICKS = builder.defineInRange("phase2_decision_max_ticks", 120.0, 1.0, 1200.0);
        builder.comment("Taming chance percent per feed (0-100)");
        IGNIVORUS_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 14.2857, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_BEEF = builder.defineInRange("taming_chance_beef", 20.0, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 25.0, 0.0, 100.0);
        builder.comment("Taming stun health threshold (HP)");
        IGNIVORUS_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", 100.0, 0.0, 1000.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        IGNIVORUS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 36000.0, 20.0, 72000.0);
        builder.comment("Egg loot chances (0-1)");
        IGNIVORUS_EGG_LOOT_BASTION_TREASURE = builder.defineInRange("egg_loot_bastion_treasure", 0.15, 0.0, 1.0);
        IGNIVORUS_EGG_LOOT_NETHER_BRIDGE = builder.defineInRange("egg_loot_nether_bridge", 0.15, 0.0, 1.0);
        IGNIVORUS_EGG_LOOT_ANCIENT_CITY = builder.defineInRange("egg_loot_ancient_city", 0.10, 0.0, 1.0);
        builder.comment("Egg drop chance (0-1)");
        IGNIVORUS_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Scale drop chance when brushed (0-1)");
        IGNIVORUS_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.35, 0.0, 1.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        IGNIVORUS_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.comment("Aggressive wild behavior");
        IGNIVORUS_AGGRESSIVE_WILD = builder.define("aggressive_wild", false);
        builder.pop();

        // Stegonaut Configuration
        builder.comment("Stegonaut Dragon Attributes").push("stegonaut");
        builder.comment("Core Attributes");
        STEGONAUT_MAX_HEALTH = builder.defineInRange("max_health", 100.0, 1.0, 100000.0);
        STEGONAUT_ARMOR = builder.defineInRange("armor", 15.0, 0.0, 100000.0);
        builder.comment("Ability Damage");
        STEGONAUT_BITE_DAMAGE = builder.defineInRange("bite_damage", 5.0, 0.0, 100000.0);
        STEGONAUT_CHIN_SLAM_DAMAGE = builder.defineInRange("chin_slam_damage", 8.0, 0.0, 100000.0);
        STEGONAUT_GROUND_EATING_DAMAGE = builder.defineInRange("ground_eating_damage", 10.0, 0.0, 100000.0);
        builder.comment("Taming chance percent per feed (0-100)");
        STEGONAUT_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 100.0, 0.0, 100.0);
        STEGONAUT_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 100.0, 0.0, 100.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        STEGONAUT_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 30000.0, 20.0, 72000.0);
        builder.comment("Egg drop chance (0-1)");
        STEGONAUT_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Scale drop chance when brushed (0-1)");
        STEGONAUT_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.30, 0.0, 1.0);
        builder.comment("Aggressive wild behavior");
        STEGONAUT_AGGRESSIVE_WILD = builder.define("aggressive_wild", false);
        builder.pop();

        // Volitans Configuration
        builder.comment("Volitans Dragon Attributes").push("volitans");
        builder.comment("Core Attributes");
        VOLITANS_MAX_HEALTH = builder.defineInRange("max_health", 160.0, 1.0, 100000.0);
        VOLITANS_ARMOR = builder.defineInRange("armor", 6.0, 0.0, 100000.0);
        builder.comment("Base rider flying speed");
        VOLITANS_FLYING_SPEED = builder.defineInRange("flying_speed", 0.38, 0.0, 2.0);
        builder.comment("Multiplier for wild flying speed only");
        VOLITANS_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", 1.0, 0.05, 10.0);
        builder.comment("Ability Damage");
        VOLITANS_BITE_DAMAGE = builder.defineInRange("bite_damage", 12.0, 0.0, 100000.0);
        VOLITANS_CLAW_DAMAGE = builder.defineInRange("claw_damage", 11.0, 0.0, 100000.0);
        VOLITANS_HORN_GORE_DAMAGE = builder.defineInRange("horn_gore_damage", 15.0, 0.0, 100000.0);
        VOLITANS_ROAR_GROUND_DAMAGE = builder.defineInRange("roar_ground_damage", 10.0, 0.0, 100000.0);
        VOLITANS_ROAR_AIR_WATER_DAMAGE = builder.defineInRange("roar_air_water_damage", 7.0, 0.0, 100000.0);
        VOLITANS_BURROW_DAMAGE = builder.defineInRange("burrow_damage", 30.0, 0.0, 100000.0);
        VOLITANS_POISON_BALL_DAMAGE = builder.defineInRange("poison_ball_damage", 12.0, 0.0, 100000.0);
        VOLITANS_WATER_BREATH_DAMAGE = builder.defineInRange("water_breath_damage", 1.8, 0.0, 100000.0);
        VOLITANS_POISON_BREATH_DAMAGE = builder.defineInRange("poison_breath_damage", 1.4, 0.0, 100000.0);
        builder.comment("Combat taming");
        VOLITANS_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", 20.0, 0.0, 100.0);
        VOLITANS_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", 30.0, 0.0, 100.0);
        VOLITANS_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", 60.0, 0.0, 100000.0);
        builder.comment("Egg hatch timing in ticks (20 ticks = 1 second)");
        VOLITANS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", 18000.0, 20.0, 72000.0);
        builder.comment("Egg loot chances (0-1)");
        VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE = builder.defineInRange("egg_loot_shipwreck_treasure", 0.12, 0.0, 1.0);
        builder.comment("Egg drop chance (0-1)");
        VOLITANS_EGG_DROP_CHANCE = builder.defineInRange("egg_drop_chance", 0.12, 0.0, 1.0);
        builder.comment("Loot drop chances (0-1)");
        VOLITANS_SCALE_DROP_CHANCE_BRUSH = builder.defineInRange("scale_drop_chance_brush", 0.30, 0.0, 1.0);
        VOLITANS_SPINE_DROP_CHANCE = builder.defineInRange("spine_drop_chance", 0.3, 0.0, 1.0);
        VOLITANS_FISH_DROP_CHANCE = builder.defineInRange("fish_drop_chance", 0.40, 0.0, 1.0);
        builder.comment("Breath Gauge Tuning");
        VOLITANS_BREATH_ACTIVE_TICKS_MAX = builder.defineInRange("breath_active_ticks_max", 240.0, 1.0, 24000.0);
        VOLITANS_BREATH_DRAIN_PER_TICK = builder.defineInRange("breath_drain_per_tick", 1.0 / (20.0 * 12.0), 0.0, 1.0);
        VOLITANS_BREATH_REGEN_PER_TICK = builder.defineInRange("breath_regen_per_tick", 0.0025, 0.0, 1.0);
        VOLITANS_BREATH_PROJECTILE_SPREAD = builder.defineInRange("breath_projectile_spread", 0.20, 0.0, 5.0);
        VOLITANS_BREATH_PROJECTILE_SPEED = builder.defineInRange("breath_projectile_speed", 1.60, 0.0, 10.0);
        VOLITANS_BREATH_PROJECTILE_LIFETIME = builder.defineInRange("breath_projectile_lifetime", 28.0, 1.0, 1200.0);
        builder.comment("Poison modifiers (0 disables poison, 1-4 = Poison I-IV)");
        VOLITANS_POISON_BREATH_POISON_DURATION_TICKS = builder.defineInRange("poison_breath_poison_duration_ticks", 80.0, 0.0, 12000.0);
        VOLITANS_POISON_BREATH_POISON_LEVEL = builder.defineInRange("poison_breath_poison_level", 1.0, 0.0, 4.0);
        VOLITANS_POISON_BALL_POISON_DURATION_TICKS = builder.defineInRange("poison_ball_poison_duration_ticks", 120.0, 0.0, 12000.0);
        VOLITANS_POISON_BALL_POISON_LEVEL = builder.defineInRange("poison_ball_poison_level", 1.0, 0.0, 4.0);
        VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS = builder.defineInRange("roar_ground_poison_duration_ticks", 1200.0, 0.0, 12000.0);
        VOLITANS_ROAR_GROUND_POISON_LEVEL = builder.defineInRange("roar_ground_poison_level", 3.0, 0.0, 4.0);
        VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS = builder.defineInRange("roar_air_water_poison_duration_ticks", 200.0, 0.0, 12000.0);
        VOLITANS_ROAR_AIR_WATER_POISON_LEVEL = builder.defineInRange("roar_air_water_poison_level", 2.0, 0.0, 4.0);
        builder.comment("Legacy taming (true = simple food taming, false = special mechanics)");
        VOLITANS_LEGACY_TAMING = builder.define("legacy_taming", false);
        builder.comment("Aggressive wild behavior");
        VOLITANS_AGGRESSIVE_WILD = builder.define("aggressive_wild", true);
        builder.pop();

        // Nulljaw Configuration
        builder.comment("Nulljaw Dragon Attributes").push("nulljaw");
        builder.comment("Core Attributes");
        NULLJAW_MAX_HEALTH = builder.defineInRange("max_health", 70.0, 1.0, 100000.0);
        NULLJAW_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 100000.0);
        builder.pop();

        ATTRIBUTES_SPEC = builder.build();
    }

    private ForgeDragonAttributesConfig() {
    }
}
