package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.common.config.dragon.profile.CindervaneStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.VarasuchusStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.StegonautStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.NulljawStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.AtroxiiaStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.VolitansStatProfile;

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
    public static ForgeConfigSpec.DoubleValue CINDERVANE_DOUBLE_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_SLASH_GRAB_HIT1_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_SLASH_GRAB_HIT2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_MAGMA_VOLLEY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_MAGMA_VOLLEY_COOLDOWN_SECONDS;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_CHICKEN;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE;
    public static ForgeConfigSpec.DoubleValue CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH;
    public static ForgeConfigSpec.BooleanValue CINDERVANE_AGGRESSIVE_WILD;

    // Raevyx
    public static ForgeConfigSpec.DoubleValue RAEVYX_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue RAEVYX_ARMOR;
    public static ForgeConfigSpec.DoubleValue RAEVYX_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue RAEVYX_WILD_FLYING_SPEED_MULTIPLIER;
    public static ForgeConfigSpec.BooleanValue RAEVYX_DIVE_LOOP_ENABLED;
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
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_MUTTON;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_PORKCHOP;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue RAEVYX_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.BooleanValue RAEVYX_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL;
    public static ForgeConfigSpec.DoubleValue RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER;
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
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAMING_CHANCE_BEEF;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_TAMING_CHANCE_TROPICAL;
    public static ForgeConfigSpec.BooleanValue VARASUCHUS_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue VARASUCHUS_EGG_HATCH_CHANCE_NORMAL;
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
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_BEEF;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_MUTTON;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_PORKCHOP;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.BooleanValue IGNIVORUS_LEGACY_TAMING;
    public static ForgeConfigSpec.DoubleValue IGNIVORUS_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.BooleanValue IGNIVORUS_AGGRESSIVE_WILD;

    // Stegonaut
    public static ForgeConfigSpec.DoubleValue STEGONAUT_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_ARMOR;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_CHIN_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_EATING_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM2_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM2_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_TAMING_CHANCE_BASE;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_TAMING_CHANCE_HEARTY;
    public static ForgeConfigSpec.DoubleValue STEGONAUT_EGG_HATCH_CHANCE_NORMAL;
    public static ForgeConfigSpec.BooleanValue STEGONAUT_AGGRESSIVE_WILD;

    // Volitans
    public static ForgeConfigSpec.DoubleValue VOLITANS_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue VOLITANS_ARMOR;
    public static ForgeConfigSpec.DoubleValue VOLITANS_FLYING_SPEED;
    public static ForgeConfigSpec.DoubleValue VOLITANS_RIDER_SWIM_SPEED;
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
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_ACTIVE_TICKS_MAX;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_DRAIN_PER_TICK;
    public static ForgeConfigSpec.DoubleValue VOLITANS_BREATH_REGEN_PER_TICK;
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
    public static ForgeConfigSpec.DoubleValue NULLJAW_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue NULLJAW_INVISIBILITY_DURATION_TICKS;

    // Atroxiia
    public static ForgeConfigSpec.DoubleValue ATROXIIA_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_TAMING_STUN_HEALTH;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_ARMOR;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_SLAM_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_SWIPE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_UNDERWATER_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_GUNGNIR_STAB_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_SLITHER_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_PRECISE_STRIKE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_PRECISE_STRIKE_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_DEVASTATING_SWEEP_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_HELHEIM_QUAKE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_HELHEIM_QUAKE_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS;
    public static ForgeConfigSpec.DoubleValue ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL;
    public static ForgeConfigSpec.BooleanValue ATROXIIA_FROST_IMPACT_ENABLED;
    public static ForgeConfigSpec.BooleanValue ATROXIIA_AGGRESSIVE_WILD;

    // Draconian Swarm
    public static ForgeConfigSpec.IntValue SWARM_WAVE_1_COUNT;
    public static ForgeConfigSpec.IntValue SWARM_WAVE_2_COUNT;
    public static ForgeConfigSpec.IntValue SWARM_WAVE_3_COUNT;
    public static ForgeConfigSpec.DoubleValue SWARM_LATCHER_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue SWARM_LATCHER_ARMOR;
    public static ForgeConfigSpec.DoubleValue SWARM_LATCHER_CHASE_SPEED;
    public static ForgeConfigSpec.DoubleValue SWARM_LATCHER_BITE_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SWARM_WINGED_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue SWARM_WINGED_ARMOR;
    public static ForgeConfigSpec.DoubleValue SWARM_WINGED_CHASE_SPEED;
    public static ForgeConfigSpec.DoubleValue SWARM_WINGED_HOOK_AND_PULL_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SWARM_WINGED_DIVE_BOMB_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SWARM_WHETTLED_MAX_HEALTH;
    public static ForgeConfigSpec.DoubleValue SWARM_WHETTLED_ARMOR;
    public static ForgeConfigSpec.DoubleValue SWARM_WHETTLED_CHASE_SPEED;
    public static ForgeConfigSpec.DoubleValue SWARM_WHETTLED_CLAW_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SWARM_WHETTLED_LUNGE_DAMAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("cindervane");
        CINDERVANE_MAX_HEALTH = builder.defineInRange("max_health", CindervaneStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        CINDERVANE_ARMOR = builder.defineInRange("armor", CindervaneStatProfile.Config.ARMOR, 0.0, 100000.0);
        CINDERVANE_FLYING_SPEED = builder.defineInRange("flying_speed", CindervaneStatProfile.Config.FLYING_SPEED, 0.0, 2.0);
        CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", CindervaneStatProfile.Config.WILD_FLYING_SPEED_MULTIPLIER, 0.05, 10.0);
        CINDERVANE_BITE_DAMAGE = builder.defineInRange("bite_damage", CindervaneStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        CINDERVANE_DOUBLE_BITE_DAMAGE = builder.defineInRange("double_bite_damage", CindervaneStatProfile.Config.DOUBLE_BITE_DAMAGE, 0.0, 100000.0);
        CINDERVANE_SLASH_GRAB_HIT1_DAMAGE = builder.defineInRange("slash_grab_hit1_damage", CindervaneStatProfile.Config.SLASH_GRAB_HIT1_DAMAGE, 0.0, 100000.0);
        CINDERVANE_SLASH_GRAB_HIT2_DAMAGE = builder.defineInRange("slash_grab_hit2_damage", CindervaneStatProfile.Config.SLASH_GRAB_HIT2_DAMAGE, 0.0, 100000.0);
        CINDERVANE_MAGMA_VOLLEY_DAMAGE = builder.defineInRange("magma_volley_damage", CindervaneStatProfile.Config.MAGMA_VOLLEY_DAMAGE, 0.0, 100000.0);
        CINDERVANE_MAGMA_VOLLEY_COOLDOWN_SECONDS = builder.defineInRange("magma_volley_cooldown_seconds", CindervaneStatProfile.Config.MAGMA_VOLLEY_COOLDOWN_SECONDS, 0.0, 6000.0);
        CINDERVANE_FIRE_BODY_DAMAGE = builder.defineInRange("fire_body_damage", CindervaneStatProfile.Config.FIRE_BODY_DAMAGE, 0.0, 100000.0);
        CINDERVANE_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", CindervaneStatProfile.Config.TAMING_CHANCE_BASE, 0.0, 100.0);
        CINDERVANE_TAMING_CHANCE_CHICKEN = builder.defineInRange("taming_chance_chicken", CindervaneStatProfile.Config.TAMING_CHANCE_CHICKEN, 0.0, 100.0);
        CINDERVANE_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", CindervaneStatProfile.Config.TAMING_CHANCE_HEARTY, 0.0, 100.0);
        CINDERVANE_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", CindervaneStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE = builder.defineInRange("fire_body_explosion_damage", CindervaneStatProfile.Config.FIRE_BODY_EXPLOSION_DAMAGE, 0.0, 100000.0);
        CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH = builder.defineInRange("fire_body_self_damage_on_crash", CindervaneStatProfile.Config.FIRE_BODY_SELF_DAMAGE_ON_CRASH, 0.0, 100000.0);
        CINDERVANE_AGGRESSIVE_WILD = builder.define("aggressive_wild", CindervaneStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("raevyx");
        RAEVYX_MAX_HEALTH = builder.defineInRange("max_health", RaevyxStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        RAEVYX_ARMOR = builder.defineInRange("armor", RaevyxStatProfile.Config.ARMOR, 0.0, 100000.0);
        RAEVYX_FLYING_SPEED = builder.defineInRange("flying_speed", RaevyxStatProfile.Config.FLYING_SPEED, 0.0, 2.0);
        RAEVYX_DIVE_LOOP_ENABLED = builder.define("dive_loop_enabled", RaevyxStatProfile.Config.DIVE_LOOP_ENABLED);
        RAEVYX_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", RaevyxStatProfile.Config.WILD_FLYING_SPEED_MULTIPLIER, 0.05, 10.0);
        RAEVYX_BITE_DAMAGE = builder.defineInRange("bite_damage", RaevyxStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        RAEVYX_LIGHTNING_BEAM_DAMAGE = builder.defineInRange("lightning_beam_damage", RaevyxStatProfile.Config.LIGHTNING_BEAM_DAMAGE, 0.0, 100000.0);
        RAEVYX_HORN_GORE_DAMAGE = builder.defineInRange("horn_gore_damage", RaevyxStatProfile.Config.HORN_GORE_DAMAGE, 0.0, 100000.0);
        RAEVYX_DASH_DAMAGE = builder.defineInRange("dash_damage", RaevyxStatProfile.Config.DASH_DAMAGE, 0.0, 100000.0);
        RAEVYX_BEAM_DRAIN_PER_TICK = builder.defineInRange("beam_drain_per_tick", RaevyxStatProfile.Config.BEAM_DRAIN_PER_TICK, 0.0, 1.0);
        RAEVYX_BEAM_REGEN_PER_TICK = builder.defineInRange("beam_regen_per_tick", RaevyxStatProfile.Config.BEAM_REGEN_PER_TICK, 0.0, 1.0);
        RAEVYX_SUMMON_STORM_COOLDOWN_TICKS = builder.defineInRange("summon_storm_cooldown_ticks", RaevyxStatProfile.Config.SUMMON_STORM_COOLDOWN_TICKS, 20.0, 120000.0);
        RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS = builder.defineInRange("summon_storm_supercharge_ticks", RaevyxStatProfile.Config.SUMMON_STORM_SUPERCHARGE_TICKS, 20.0, 120000.0);
        RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER = builder.defineInRange("summon_storm_supercharge_damage_multiplier", RaevyxStatProfile.Config.SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER, 0.0, 100.0);
        RAEVYX_SUMMON_STORM_DURATION_TICKS = builder.defineInRange("summon_storm_duration_ticks", RaevyxStatProfile.Config.SUMMON_STORM_DURATION_TICKS, 20.0, 120000.0);
        RAEVYX_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", RaevyxStatProfile.Config.TAMING_CHANCE_BASE, 0.0, 100.0);
        RAEVYX_TAMING_CHANCE_MUTTON = builder.defineInRange("taming_chance_mutton", RaevyxStatProfile.Config.TAMING_CHANCE_MUTTON, 0.0, 100.0);
        RAEVYX_TAMING_CHANCE_PORKCHOP = builder.defineInRange("taming_chance_porkchop", RaevyxStatProfile.Config.TAMING_CHANCE_PORKCHOP, 0.0, 100.0);
        RAEVYX_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", RaevyxStatProfile.Config.TAMING_CHANCE_HEARTY, 0.0, 100.0);
        RAEVYX_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", RaevyxStatProfile.ForgeDefaults.TAMING_STUN_HEALTH, 0.0, 1000.0);
        RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", RaevyxStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER = builder.defineInRange("egg_hatch_time_ticks_thunder", RaevyxStatProfile.Config.EGG_HATCH_TIME_TICKS_THUNDER, 20.0, 72000.0);
        RAEVYX_LEGACY_TAMING = builder.define("legacy_taming", RaevyxStatProfile.Config.LEGACY_TAMING);
        RAEVYX_AGGRESSIVE_WILD = builder.define("aggressive_wild", RaevyxStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("varasuchus");
        VARASUCHUS_MAX_HEALTH = builder.defineInRange("max_health", VarasuchusStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        VARASUCHUS_ARMOR = builder.defineInRange("armor", VarasuchusStatProfile.Config.ARMOR, 0.0, 100000.0);
        VARASUCHUS_SWIM_SPEED = builder.defineInRange("swim_speed", VarasuchusStatProfile.Config.SWIM_SPEED, 0.1, 5.0);
        VARASUCHUS_BITE_PHASE1_DAMAGE = builder.defineInRange("bite_phase1_damage", VarasuchusStatProfile.Config.BITE_PHASE1_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_BITE_PHASE2_DAMAGE = builder.defineInRange("bite_phase2_damage", VarasuchusStatProfile.Config.BITE_PHASE2_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_TAIL_ATTACK_DAMAGE = builder.defineInRange("tail_attack_damage", VarasuchusStatProfile.Config.TAIL_ATTACK_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_TAILGUARD_PARRY_DAMAGE = builder.defineInRange("tailguard_parry_damage", VarasuchusStatProfile.Config.TAILGUARD_PARRY_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE = builder.defineInRange("dash_tail_swipe_damage", VarasuchusStatProfile.Config.DASH_TAIL_SWIPE_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_DASH_CLAW_DAMAGE = builder.defineInRange("dash_claw_damage", VarasuchusStatProfile.Config.DASH_CLAW_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_CLAW_ATTACK_DAMAGE = builder.defineInRange("claw_attack_damage", VarasuchusStatProfile.Config.CLAW_ATTACK_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_HORN_GORE_PHASE1_DAMAGE = builder.defineInRange("horn_gore_phase1_damage", VarasuchusStatProfile.Config.HORN_PHASE1_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_HORN_GORE_PHASE2_DAMAGE = builder.defineInRange("horn_gore_phase2_damage", VarasuchusStatProfile.Config.HORN_PHASE2_DAMAGE, 0.0, 100000.0);
        VARASUCHUS_TAMING_CHANCE = builder.defineInRange("taming_chance", VarasuchusStatProfile.Config.TAMING_CHANCE, 0.0, 100.0);
        VARASUCHUS_TAMING_CHANCE_BEEF = builder.defineInRange("taming_chance_beef", VarasuchusStatProfile.Config.TAMING_CHANCE_BEEF, 0.0, 100.0);
        VARASUCHUS_TAMING_CHANCE_TROPICAL = builder.defineInRange("taming_chance_tropical", VarasuchusStatProfile.Config.TAMING_CHANCE_TROPICAL, 0.0, 100.0);
        VARASUCHUS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", VarasuchusStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        VARASUCHUS_LEGACY_TAMING = builder.define("legacy_taming", VarasuchusStatProfile.Config.LEGACY_TAMING);
        VARASUCHUS_AGGRESSIVE_WILD = builder.define("aggressive_wild", VarasuchusStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("ignivorus");
        IGNIVORUS_MAX_HEALTH = builder.defineInRange("max_health", IgnivorusStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        IGNIVORUS_ARMOR = builder.defineInRange("armor", IgnivorusStatProfile.Config.ARMOR, 0.0, 100000.0);
        IGNIVORUS_FLYING_SPEED = builder.defineInRange("flying_speed", IgnivorusStatProfile.Config.FLYING_SPEED, 0.0, 2.0);
        IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", IgnivorusStatProfile.Config.WILD_FLYING_SPEED_MULTIPLIER, 0.05, 10.0);
        IGNIVORUS_BITE_DAMAGE = builder.defineInRange("bite_damage", IgnivorusStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_BODY_SLAM_DAMAGE = builder.defineInRange("body_slam_damage", IgnivorusStatProfile.Config.BODY_SLAM_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_LEAP_SLAM_DAMAGE = builder.defineInRange("leap_slam_damage", IgnivorusStatProfile.Config.LEAP_SLAM_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_FIRE_BREATH_DAMAGE = builder.defineInRange("fire_breath_damage", IgnivorusStatProfile.Config.FIRE_BREATH_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_FIREBALL_DAMAGE = builder.defineInRange("fireball_damage", IgnivorusStatProfile.Config.FIREBALL_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_MAGMA_PILLAR_DAMAGE = builder.defineInRange("magma_pillar_damage", IgnivorusStatProfile.Config.MAGMA_PILLAR_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_WING_SWIPE_DAMAGE = builder.defineInRange("wing_swipe_damage", IgnivorusStatProfile.Config.WING_SWIPE_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_STOMP_DAMAGE = builder.defineInRange("stomp_damage", IgnivorusStatProfile.Config.STOMP_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_BULLDOZE_DAMAGE = builder.defineInRange("bulldoze_damage", IgnivorusStatProfile.Config.BULLDOZE_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_ULTIMATE_DAMAGE = builder.defineInRange("ultimate_damage", IgnivorusStatProfile.Config.ULTIMATE_DAMAGE, 0.0, 100000.0);
        IGNIVORUS_ULTIMATE_PENALTY_HEALTH = builder.defineInRange("ultimate_penalty_health", IgnivorusStatProfile.Config.ULTIMATE_PENALTY_HEALTH, 1.0, 10000.0);
        IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION = builder.defineInRange("ultimate_trigger_health_fraction", IgnivorusStatProfile.Config.ULTIMATE_TRIGGER_HEALTH_FRACTION, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK = builder.defineInRange("fire_breath_drain_per_tick", IgnivorusStatProfile.Config.FIRE_BREATH_DRAIN_PER_TICK, 0.0, 1.0);
        IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK = builder.defineInRange("fire_breath_regen_per_tick", IgnivorusStatProfile.Config.FIRE_BREATH_REGEN_PER_TICK, 0.0, 1.0);
        IGNIVORUS_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", IgnivorusStatProfile.Config.TAMING_CHANCE_BASE, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_BEEF = builder.defineInRange("taming_chance_beef", IgnivorusStatProfile.Config.TAMING_CHANCE_BEEF, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_MUTTON = builder.defineInRange("taming_chance_mutton", IgnivorusStatProfile.Config.TAMING_CHANCE_MUTTON, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_PORKCHOP = builder.defineInRange("taming_chance_porkchop", IgnivorusStatProfile.Config.TAMING_CHANCE_PORKCHOP, 0.0, 100.0);
        IGNIVORUS_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", IgnivorusStatProfile.Config.TAMING_CHANCE_HEARTY, 0.0, 100.0);
        IGNIVORUS_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", IgnivorusStatProfile.ForgeDefaults.TAMING_STUN_HEALTH, 0.0, 1000.0);
        IGNIVORUS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", IgnivorusStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        IGNIVORUS_LEGACY_TAMING = builder.define("legacy_taming", IgnivorusStatProfile.Config.LEGACY_TAMING);
        IGNIVORUS_AGGRESSIVE_WILD = builder.define("aggressive_wild", IgnivorusStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("stegonaut");
        STEGONAUT_MAX_HEALTH = builder.defineInRange("max_health", StegonautStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        STEGONAUT_ARMOR = builder.defineInRange("armor", StegonautStatProfile.Config.ARMOR, 0.0, 100000.0);
        STEGONAUT_BITE_DAMAGE = builder.defineInRange("bite_damage", StegonautStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        STEGONAUT_CHIN_SLAM_DAMAGE = builder.defineInRange("chin_slam_damage", StegonautStatProfile.Config.CHIN_SLAM_DAMAGE, 0.0, 100000.0);
        STEGONAUT_GROUND_EATING_DAMAGE = builder.defineInRange("ground_eating_damage", StegonautStatProfile.Config.GROUND_EATING_DAMAGE, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM_DAMAGE = builder.defineInRange("ground_slam_damage", StegonautStatProfile.Config.GROUND_SLAM_DAMAGE, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM2_DAMAGE = builder.defineInRange("ground_slam2_damage", StegonautStatProfile.Config.GROUND_SLAM2_DAMAGE, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE = builder.defineInRange("ground_slam_pillar_damage", StegonautStatProfile.Config.GROUND_SLAM_PILLAR_DAMAGE, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM_KNOCKBACK = builder.defineInRange("ground_slam_knockback", StegonautStatProfile.Config.GROUND_SLAM_KNOCKBACK, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM2_KNOCKBACK = builder.defineInRange("ground_slam2_knockback", StegonautStatProfile.Config.GROUND_SLAM2_KNOCKBACK, 0.0, 100000.0);
        STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK = builder.defineInRange("ground_slam_pillar_knockback", StegonautStatProfile.Config.GROUND_SLAM_PILLAR_KNOCKBACK, 0.0, 100000.0);
        STEGONAUT_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", StegonautStatProfile.Config.TAMING_CHANCE_BASE, 0.0, 100.0);
        STEGONAUT_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", StegonautStatProfile.Config.TAMING_CHANCE_HEARTY, 0.0, 100.0);
        STEGONAUT_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", StegonautStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        STEGONAUT_AGGRESSIVE_WILD = builder.define("aggressive_wild", StegonautStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("volitans");
        VOLITANS_MAX_HEALTH = builder.defineInRange("max_health", VolitansStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        VOLITANS_ARMOR = builder.defineInRange("armor", VolitansStatProfile.Config.ARMOR, 0.0, 100000.0);
        VOLITANS_FLYING_SPEED = builder.defineInRange("flying_speed", VolitansStatProfile.Config.FLYING_SPEED, 0.0, 2.0);
        VOLITANS_RIDER_SWIM_SPEED = builder.defineInRange("rider_swim_speed", VolitansStatProfile.Config.RIDER_SWIM_SPEED, 0.1, 5.0);
        VOLITANS_WILD_FLYING_SPEED_MULTIPLIER = builder.defineInRange("wild_flying_speed_multiplier", VolitansStatProfile.Config.WILD_FLYING_SPEED_MULTIPLIER, 0.05, 10.0);
        VOLITANS_BITE_DAMAGE = builder.defineInRange("bite_damage", VolitansStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        VOLITANS_CLAW_DAMAGE = builder.defineInRange("claw_damage", VolitansStatProfile.Config.CLAW_DAMAGE, 0.0, 100000.0);
        VOLITANS_HORN_GORE_DAMAGE = builder.defineInRange("horn_gore_damage", VolitansStatProfile.Config.HORN_GORE_DAMAGE, 0.0, 100000.0);
        VOLITANS_ROAR_GROUND_DAMAGE = builder.defineInRange("roar_ground_damage", VolitansStatProfile.Config.ROAR_GROUND_DAMAGE, 0.0, 100000.0);
        VOLITANS_ROAR_AIR_WATER_DAMAGE = builder.defineInRange("roar_air_water_damage", VolitansStatProfile.Config.ROAR_AIR_WATER_DAMAGE, 0.0, 100000.0);
        VOLITANS_BURROW_DAMAGE = builder.defineInRange("burrow_damage", VolitansStatProfile.Config.BURROW_DAMAGE, 0.0, 100000.0);
        VOLITANS_POISON_BALL_DAMAGE = builder.defineInRange("poison_ball_damage", VolitansStatProfile.Config.POISON_BALL_DAMAGE, 0.0, 100000.0);
        VOLITANS_WATER_BREATH_DAMAGE = builder.defineInRange("water_breath_damage", VolitansStatProfile.Config.WATER_BREATH_DAMAGE, 0.0, 100000.0);
        VOLITANS_POISON_BREATH_DAMAGE = builder.defineInRange("poison_breath_damage", VolitansStatProfile.Config.POISON_BREATH_DAMAGE, 0.0, 100000.0);
        VOLITANS_TAMING_CHANCE_BASE = builder.defineInRange("taming_chance_base", VolitansStatProfile.Config.TAMING_CHANCE_BASE, 0.0, 100.0);
        VOLITANS_TAMING_CHANCE_HEARTY = builder.defineInRange("taming_chance_hearty", VolitansStatProfile.Config.TAMING_CHANCE_HEARTY, 0.0, 100.0);
        VOLITANS_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", VolitansStatProfile.Config.TAMING_STUN_HEALTH, 0.0, 100000.0);
        VOLITANS_EGG_HATCH_CHANCE_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", VolitansStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        VOLITANS_BREATH_ACTIVE_TICKS_MAX = builder.defineInRange("breath_active_ticks_max", VolitansStatProfile.Config.BREATH_ACTIVE_TICKS_MAX, 1.0, 24000.0);
        VOLITANS_BREATH_DRAIN_PER_TICK = builder.defineInRange("breath_drain_per_tick", VolitansStatProfile.Config.BREATH_DRAIN_PER_TICK, 0.0, 1.0);
        VOLITANS_BREATH_REGEN_PER_TICK = builder.defineInRange("breath_regen_per_tick", VolitansStatProfile.Config.BREATH_REGEN_PER_TICK, 0.0, 1.0);
        VOLITANS_POISON_BREATH_POISON_DURATION_TICKS = builder.defineInRange("poison_breath_poison_duration_ticks", VolitansStatProfile.Config.POISON_BREATH_POISON_DURATION_TICKS, 0.0, 12000.0);
        VOLITANS_POISON_BREATH_POISON_LEVEL = builder.defineInRange("poison_breath_poison_level", VolitansStatProfile.Config.POISON_BREATH_POISON_LEVEL, 0.0, 4.0);
        VOLITANS_POISON_BALL_POISON_DURATION_TICKS = builder.defineInRange("poison_ball_poison_duration_ticks", VolitansStatProfile.Config.POISON_BALL_POISON_DURATION_TICKS, 0.0, 12000.0);
        VOLITANS_POISON_BALL_POISON_LEVEL = builder.defineInRange("poison_ball_poison_level", VolitansStatProfile.Config.POISON_BALL_POISON_LEVEL, 0.0, 4.0);
        VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS = builder.defineInRange("roar_ground_poison_duration_ticks", VolitansStatProfile.Config.ROAR_GROUND_POISON_DURATION_TICKS, 0.0, 12000.0);
        VOLITANS_ROAR_GROUND_POISON_LEVEL = builder.defineInRange("roar_ground_poison_level", VolitansStatProfile.Config.ROAR_GROUND_POISON_LEVEL, 0.0, 4.0);
        VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS = builder.defineInRange("roar_air_water_poison_duration_ticks", VolitansStatProfile.Config.ROAR_AIR_WATER_POISON_DURATION_TICKS, 0.0, 12000.0);
        VOLITANS_ROAR_AIR_WATER_POISON_LEVEL = builder.defineInRange("roar_air_water_poison_level", VolitansStatProfile.Config.ROAR_AIR_WATER_POISON_LEVEL, 0.0, 4.0);
        VOLITANS_LEGACY_TAMING = builder.define("legacy_taming", VolitansStatProfile.Config.LEGACY_TAMING);
        VOLITANS_AGGRESSIVE_WILD = builder.define("aggressive_wild", VolitansStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("nulljaw");
        NULLJAW_MAX_HEALTH = builder.defineInRange("max_health", NulljawStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        NULLJAW_ARMOR = builder.defineInRange("armor", NulljawStatProfile.Config.ARMOR, 0.0, 100000.0);
        NULLJAW_BITE_DAMAGE = builder.defineInRange("bite_damage", NulljawStatProfile.Config.BITE_DAMAGE, 0.0, 100000.0);
        NULLJAW_INVISIBILITY_DURATION_TICKS = builder.defineInRange("invisibility_duration_ticks", NulljawStatProfile.Config.INVISIBILITY_DURATION_TICKS, 1.0, 72000.0);
        builder.pop();

        builder.push("atroxiia");
        ATROXIIA_MAX_HEALTH = builder.defineInRange("max_health", AtroxiiaStatProfile.Config.MAX_HEALTH, 1.0, 100000.0);
        ATROXIIA_TAMING_STUN_HEALTH = builder.defineInRange("taming_stun_health", AtroxiiaStatProfile.Config.TAMING_STUN_HEALTH, 0.0, 100000.0);
        ATROXIIA_ARMOR = builder.defineInRange("armor", AtroxiiaStatProfile.Config.ARMOR, 0.0, 100000.0);
        ATROXIIA_SLAM_DAMAGE = builder.defineInRange("slam_damage", AtroxiiaStatProfile.Config.SLAM_DAMAGE, 0.0, 100000.0);
        ATROXIIA_SWIPE_DAMAGE = builder.defineInRange("swipe_damage", AtroxiiaStatProfile.Config.SWIPE_DAMAGE, 0.0, 100000.0);
        ATROXIIA_UNDERWATER_BITE_DAMAGE = builder.defineInRange("underwater_bite_damage", AtroxiiaStatProfile.Config.UNDERWATER_BITE_DAMAGE, 0.0, 100000.0);
        ATROXIIA_GUNGNIR_STAB_DAMAGE = builder.defineInRange("gungnir_stab_damage", AtroxiiaStatProfile.Config.GUNGNIR_STAB_DAMAGE, 0.0, 100000.0);
        ATROXIIA_SLITHER_DAMAGE = builder.defineInRange("slither_damage", AtroxiiaStatProfile.Config.SLITHER_DAMAGE, 0.0, 100000.0);
        ATROXIIA_PRECISE_STRIKE_DAMAGE = builder.defineInRange("precise_strike_damage", AtroxiiaStatProfile.Config.PRECISE_STRIKE_DAMAGE, 0.0, 100000.0);
        ATROXIIA_PRECISE_STRIKE_KNOCKBACK = builder.defineInRange("precise_strike_knockback", AtroxiiaStatProfile.Config.PRECISE_STRIKE_KNOCKBACK, 0.0, 1000.0);
        ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS = builder.defineInRange("precise_strike_stun_duration_ticks", AtroxiiaStatProfile.Config.PRECISE_STRIKE_STUN_DURATION_TICKS, 0.0, 12000.0);
        ATROXIIA_DEVASTATING_SWEEP_DAMAGE = builder.defineInRange("devastating_sweep_damage", AtroxiiaStatProfile.Config.DEVASTATING_SWEEP_DAMAGE, 0.0, 100000.0);
        ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK = builder.defineInRange("devastating_sweep_knockback", AtroxiiaStatProfile.Config.DEVASTATING_SWEEP_KNOCKBACK, 0.0, 1000.0);
        ATROXIIA_HELHEIM_QUAKE_DAMAGE = builder.defineInRange("helheim_quake_damage", AtroxiiaStatProfile.Config.HELHEIM_QUAKE_DAMAGE, 0.0, 100000.0);
        ATROXIIA_HELHEIM_QUAKE_KNOCKBACK = builder.defineInRange("helheim_quake_knockback", AtroxiiaStatProfile.Config.HELHEIM_QUAKE_KNOCKBACK, 0.0, 1000.0);
        ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK = builder.defineInRange("helheim_quake_secondary_knockback", AtroxiiaStatProfile.Config.HELHEIM_QUAKE_SECONDARY_KNOCKBACK, 0.0, 1000.0);
        ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS = builder.defineInRange("helheim_quake_stun_duration_ticks", AtroxiiaStatProfile.Config.HELHEIM_QUAKE_STUN_DURATION_TICKS, 0.0, 12000.0);
        ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL = builder.defineInRange("egg_hatch_time_ticks_normal", AtroxiiaStatProfile.Config.EGG_HATCH_TIME_TICKS_NORMAL, 20.0, 72000.0);
        ATROXIIA_FROST_IMPACT_ENABLED = builder.define("frost_impact_enabled", AtroxiiaStatProfile.Config.FROST_IMPACT_ENABLED);
        ATROXIIA_AGGRESSIVE_WILD = builder.define("aggressive_wild", AtroxiiaStatProfile.Config.AGGRESSIVE_WILD);
        builder.pop();

        builder.push("draconian_swarm");
        SWARM_WAVE_1_COUNT = builder.defineInRange("wave_1_count", 6, 1, 50);
        SWARM_WAVE_2_COUNT = builder.defineInRange("wave_2_count", 9, 1, 50);
        SWARM_WAVE_3_COUNT = builder.defineInRange("wave_3_count", 12, 1, 50);
        SWARM_LATCHER_MAX_HEALTH = builder.defineInRange("latcher_max_health", 12.0, 1.0, 100000.0);
        SWARM_LATCHER_ARMOR = builder.defineInRange("latcher_armor", 0.0, 0.0, 100000.0);
        SWARM_LATCHER_CHASE_SPEED = builder.defineInRange("latcher_chase_speed", 0.80, 0.0, 2.0);
        SWARM_LATCHER_BITE_DAMAGE = builder.defineInRange("latcher_bite_damage", 4.0, 0.0, 100000.0);
        SWARM_WINGED_MAX_HEALTH = builder.defineInRange("winged_max_health", 6.0, 1.0, 100000.0);
        SWARM_WINGED_ARMOR = builder.defineInRange("winged_armor", 0.0, 0.0, 100000.0);
        SWARM_WINGED_CHASE_SPEED = builder.defineInRange("winged_chase_speed", 1.0, 0.0, 2.0);
        SWARM_WINGED_HOOK_AND_PULL_DAMAGE = builder.defineInRange("winged_hook_and_pull_damage", 1.5, 0.0, 100000.0);
        SWARM_WINGED_DIVE_BOMB_DAMAGE = builder.defineInRange("winged_dive_bomb_damage", 2.025, 0.0, 100000.0);
        SWARM_WHETTLED_MAX_HEALTH = builder.defineInRange("whettled_max_health", 16.0, 1.0, 100000.0);
        SWARM_WHETTLED_ARMOR = builder.defineInRange("whettled_armor", 0.0, 0.0, 100000.0);
        SWARM_WHETTLED_CHASE_SPEED = builder.defineInRange("whettled_chase_speed", 0.90, 0.0, 2.0);
        SWARM_WHETTLED_CLAW_ATTACK_DAMAGE = builder.defineInRange("whettled_claw_attack_damage", 4.0, 0.0, 100000.0);
        SWARM_WHETTLED_LUNGE_DAMAGE = builder.defineInRange("whettled_lunge_damage", 9.0, 0.0, 100000.0);
        builder.pop();

        ATTRIBUTES_SPEC = builder.build();
    }

    private ForgeDragonAttributesConfig() {
    }
}
