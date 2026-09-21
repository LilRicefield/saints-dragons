package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.HashMap;
import java.util.Map;

public final class RaevyxStatProfile {
    private RaevyxStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 180.0D;
        public static final double ARMOR = 8.0D;
        public static final double FLYING_SPEED = 0.5D;
        public static final double BITE_DAMAGE = 15.0D;
        public static final double LIGHTNING_BEAM_DAMAGE = 35.0D;
        public static final double HORN_GORE_DAMAGE = 15.0D;
        public static final double DASH_DAMAGE = 10.0D;
        public static final double TAMING_CHANCE_BASE = 20.0D;
        public static final double TAMING_CHANCE_MUTTON = 20.0D;
        public static final double TAMING_CHANCE_PORKCHOP = 20.0D;
        public static final double TAMING_CHANCE_HEARTY = 33.3333D;
        public static final double BEAM_DRAIN_PER_TICK = 0.014D;
        public static final double BEAM_REGEN_PER_TICK = 0.0025D;
        public static final double SUMMON_STORM_COOLDOWN_TICKS = 4800.0D;
        public static final double SUMMON_STORM_SUPERCHARGE_TICKS = 1200.0D;
        public static final double SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER = 2.0D;
        public static final double SUMMON_STORM_DURATION_TICKS = 1200.0D;
        public static final boolean LEGACY_TAMING = false;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 18000.0D;
        public static final double EGG_HATCH_TIME_TICKS_THUNDER = 9600.0D;
        public static final double TAMING_STUN_HEALTH = MAX_HEALTH * (1.0D / 3.0D);
        public static final double WILD_FLYING_SPEED_MULTIPLIER = 1.0D;
        public static final boolean DIVE_LOOP_ENABLED = true;
        public static final boolean AGGRESSIVE_WILD = false;

        private Config() {
        }
    }

    public static final class ForgeDefaults {
        public static final double TAMING_STUN_HEALTH = 60.0;

        private ForgeDefaults() {
        }
    }

    public static final class AirCombatBehaviour {
        public static final double MELEE_RANGE = 7.0D;
        public static final double RANGED_MIN_RANGE = 16.0D;
        public static final double ROAR_MIN_RANGE = 6.0D;
        public static final double ROAR_MAX_RANGE = 40.0D;
        public static final double CHASE_CONTAIN_RANGE = 18.0D;
        public static final double ORBIT_ABORT_RANGE = 32.0D;
        public static final double TACTICAL_ESCAPE_RANGE = 46.0D;
        public static final double ORBIT_RADIUS = 22.0D;
        public static final double TARGET_FLEE_SPEED = 0.10D;
        public static final double ORBIT_SPEED = 4.0D;
        public static final double DIRECT_CHASE_SPEED = 11.0D;
        public static final double DIVE_CHASE_SPEED = 12.5D;
        public static final double DIRECT_COMMIT_SPEED = 6.0D;
        public static final double DIVE_COMMIT_SPEED = 7.5D;
        public static final double BEAM_PASS_SPEED = 4.5D;
        public static final double ROAR_PASS_SPEED = 4.75D;
        public static final double BREAKAWAY_SPEED = 6.5D;
        public static final int MELEE_ATTACK_COOLDOWN_TICKS = 20;
        public static final int BEAM_ATTACK_COOLDOWN_TICKS = 12;
        public static final int ROAR_ATTACK_COOLDOWN_TICKS = 24;
        public static final int ROAR_COOLDOWN_TICKS = 160;
        public static final int CHASE_CAPTURE_TICKS = 12;
        public static final int CHASE_MINIMUM_TICKS = 12;

        private AirCombatBehaviour() {
        }
    }

    public static final class AutonomousFlightBehaviour {
        public static final double CRUISE_SPEED = 2.6D;
        public static final double LANDING_SPEED = 1.45D;

        private AutonomousFlightBehaviour() {
        }
    }

    public static final class BeamAbility {
        public static final float AI_BEAM_MERCY_HEALTH_FRACTION = 0.25F;
        public static final double AI_TARGET_HIT_RADIUS = 0.55D;
        public static final double RIDER_BEAM_RADIUS = 1.2D;
        public static final double AI_BEAM_RADIUS = 0.75D;
        public static final float DEFAULT_BEAM_DAMAGE = 20.0f;

        private BeamAbility() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 15.0f;
        public static final double RANGE = 6.0;

        private BiteAbility() {
        }
    }

    public static final class Brain {
        public static final double BREED_SPEED = 1.0D;
        public static final double FOLLOW_PARENT_SPEED = 1.15D;
        public static final double GROUND_WANDER_SPEED = 1.0D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.12D;
        public static final double WATER_CHASE_SPEED = 0.12D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;

        private Brain() {
        }
    }

    public static final class ChainLightningAbility {
        public static final float BITE_CHAIN_DAMAGE = 10.0F;
        public static final double BITE_CHAIN_RADIUS = 8.0D;
        public static final int BITE_CHAIN_JUMPS = 5;
        public static final float IMPACT_CHAIN_DAMAGE_MIN = 7.0F;
        public static final float IMPACT_CHAIN_DAMAGE_MAX = 15.0F;
        public static final double IMPACT_CHAIN_RADIUS = 12.0D;
        public static final int IMPACT_CHAIN_JUMPS = 4;

        private ChainLightningAbility() {
        }
    }

    public static final class DiveImpactAbility {
        public static final double MIN_IMPACT_SPEED = 1.45D;
        public static final double FULL_POWER_SPEED = 3.25D;
        public static final float BASE_DAMAGE = 8.0F;
        public static final float DAMAGE_PER_EXCESS_SPEED = 20.0F;
        public static final float MAX_DAMAGE = 40.0F;
        public static final double MIN_RADIUS = 7.0D;
        public static final double MAX_RADIUS = 12.0D;
        public static final double MIN_KNOCKBACK = 0.8D;
        public static final double MAX_KNOCKBACK = 2.0D;

        private DiveImpactAbility() {
        }
    }

    public static final class Entity {
        public static final double RIDER_WALK_SPEED = 0.20D;
        public static final double RIDER_RUN_SPEED = 0.35D;
        public static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;
        public static final float DEFAULT_DASH_DAMAGE = 10.0F;
        public static final double GROUND_REND_BOLT_LINK_START_REACH = 1.1D;
        public static final double BREED_PARTNER_RANGE = 8.0D;
        public static final double BREED_DISTANCE_SQR = 16.0D;
        public static final int DODGE_DURATION_TICKS = 12;
        public static final int RIDER_DODGE_COOLDOWN_TICKS = 30;
        public static final int FLEX_COOLDOWN_TICKS = 120;
        public static final int AI_DODGE_COOLDOWN_TICKS = 60;
        public static final double AIR_DODGE_DISTANCE_MULTIPLIER = 3.0D;
        public static final double DASH_NUDGE_DRAG = 0.9D;
        public static final float REACTIVE_HIT_DODGE_CHANCE = 0.35F;
        public static final double BEAM_RANGE = 64.0D;
        public static final int RIDER_LANDING_BLEND_DURATION = 5;
        public static final double BABY_MAX_HEALTH = 60.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.30D;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 64.0D;
        public static final double ATTRIBUTE_KNOCKBACK_RESISTANCE = 1.0D;
        public static final double FALLBACK_BEAM_REGEN_PER_TICK = 0.0025D;
        public static final double FALLBACK_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER = 2.0D;
        public static final double FALLBACK_WILD_FLYING_SPEED_MULTIPLIER = 1.0D;

        private Entity() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final double BITE_ONLY_PREY_RANGE = 1.35D;
        public static final double GORE_RANGE = 4.5D;
        public static final float CHASE_SPEED = 1.45F;
        public static final double BITE_RANGE = 3.0D;
        public static final double GROUND_REND_RANGE = 8.5D;
        public static final double GROUND_REND_MIN_RANGE = 3.4D;
        public static final int GROUND_REND_COOLDOWN_TICKS = 400;
        public static final int DAMAGE_MEMORY_TICKS = 30;
        public static final double DASH_MIN_RANGE = 8.0D;
        public static final double DASH_MAX_RANGE = 26.0D;
        public static final float BUDGET_REGEN_PER_TICK = 0.025F;
        public static final float DASH_COST = 0.62F;

        private GroundCombatBehaviour() {
        }
    }

    public static final class GroundRendAbility {
        public static final int COOLDOWN_TICKS = 32;
        public static final double AI_STEER_BACK_RANGE = 6.0D;
        public static final float RIDER_SURGE_SPEED = 2.0F;
        public static final float RIDER_RECOVERY_END_SPEED = 0.25F;
        public static final double AI_FORWARD_SPEED = 0.9D;
        public static final float HIT_DAMAGE = 5.0F;
        public static final double HIT_KNOCKBACK = 0.55D;
        public static final int HIT_COOLDOWN_TICKS = 5;

        private GroundRendAbility() {
        }
    }

    public static final class HornGoreAbility {
        public static final float DEFAULT_GORE_DAMAGE = 15.0f;
        public static final double GORE_RANGE = 4.0;

        private HornGoreAbility() {
        }
    }

    public static final class InteractionHandler {
        public static final double FALLBACK_TAMING_CHANCE_HEARTY = 33.3333D;
        public static final double FALLBACK_TAMING_CHANCE_MUTTON = 20.0D;
        public static final double FALLBACK_TAMING_CHANCE_PORKCHOP = 20.0D;
        public static final double FALLBACK_TAMING_CHANCE_BASE = 20.0D;

        private InteractionHandler() {
        }
    }

    public static final class RiderController {
        public static final double BASE_FLIGHT_SPEED_MULT = 4.0;
        public static final double SPRINT_FLIGHT_SPEED_MULT = 6.0;
        public static final double DRAG_NO_INPUT = 0.5;
        public static final double ASCEND_THRUST = 1.2D;
        public static final double DESCEND_THRUST = 1.0D;
        public static final double TERMINAL_VELOCITY = 1.5D;
        public static final double FLIGHT_ACCELERATION = 0.45D;
        public static final double DIVE_SPEED_MULTIPLIER = 3.0D;
        public static final double DIVE_ACCELERATION = 0.35D;

        private RiderController() {
        }
    }

    public static final class RoarAbility {
        public static final float LIGHTNING_DAMAGE = 5.0F;

        private RoarAbility() {
        }
    }

    public static final class SummonStormAbility {
        public static final int DEFAULT_COOLDOWN_TICKS = 20 * 240;
        public static final int MIN_COOLDOWN_TICKS = 20;

        private SummonStormAbility() {
        }
    }

    public static final class TargetingBehaviour {
        public static final double BABY_PROTECTION_RANGE = 16.0D;

        private TargetingBehaviour() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double flyingSpeed = Config.FLYING_SPEED;
        double biteDamage = Config.BITE_DAMAGE;
        double lightningBeamDamage = Config.LIGHTNING_BEAM_DAMAGE;
        double hornGoreDamage = Config.HORN_GORE_DAMAGE;
        double dashDamage = Config.DASH_DAMAGE;
        double tamingChanceBase = Config.TAMING_CHANCE_BASE;
        double tamingChanceMutton = Config.TAMING_CHANCE_MUTTON;
        double tamingChancePorkchop = Config.TAMING_CHANCE_PORKCHOP;
        double tamingChanceHearty = Config.TAMING_CHANCE_HEARTY;
        double beamDrainPerTick = Config.BEAM_DRAIN_PER_TICK;
        double beamRegenPerTick = Config.BEAM_REGEN_PER_TICK;
        double summonStormCooldownTicks = Config.SUMMON_STORM_COOLDOWN_TICKS;
        double summonStormSuperchargeTicks = Config.SUMMON_STORM_SUPERCHARGE_TICKS;
        double summonStormSuperchargeDamageMultiplier = Config.SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER;
        double summonStormDurationTicks = Config.SUMMON_STORM_DURATION_TICKS;
        boolean legacyTaming = Config.LEGACY_TAMING;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        double eggHatchTimeTicksThunder = Config.EGG_HATCH_TIME_TICKS_THUNDER;
        double tamingStunHealth = Config.TAMING_STUN_HEALTH;
        double wildFlyingSpeedMultiplier = Config.WILD_FLYING_SPEED_MULTIPLIER;
        boolean diveLoopEnabled = Config.DIVE_LOOP_ENABLED;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("RAEVYX_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("RAEVYX_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("RAEVYX_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("RAEVYX_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BITE_DAMAGE").get(null));
                lightningBeamDamage = (double) configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null));
                hornGoreDamage = (double) configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null));
                dashDamage = (double) configClass.getField("RAEVYX_DASH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_DASH_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null));
                tamingChanceMutton = (double) configClass.getField("RAEVYX_TAMING_CHANCE_MUTTON").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_MUTTON").get(null));
                tamingChancePorkchop = (double) configClass.getField("RAEVYX_TAMING_CHANCE_PORKCHOP").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_PORKCHOP").get(null));
                tamingChanceHearty = (double) configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null));
                beamDrainPerTick = (double) configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null));
                beamRegenPerTick = (double) configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null));
                summonStormCooldownTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_COOLDOWN_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_COOLDOWN_TICKS").get(null));
                summonStormSuperchargeTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS").get(null));
                summonStormSuperchargeDamageMultiplier = (double) configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER").get(null));
                summonStormDurationTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_DURATION_TICKS").get(null));
                legacyTaming = (boolean) configClass.getField("RAEVYX_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LEGACY_TAMING").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL").get(null));
                eggHatchTimeTicksThunder = (double) configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER").get(null));
                tamingStunHealth = (double) configClass.getField("RAEVYX_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_STUN_HEALTH").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("RAEVYX_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                diveLoopEnabled = (boolean) configClass.getField("RAEVYX_DIVE_LOOP_ENABLED").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_DIVE_LOOP_ENABLED").get(null));
                aggressiveWild = (boolean) configClass.getField("RAEVYX_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_mutton", tamingChanceMutton);
        extras.put("taming_chance_porkchop", tamingChancePorkchop);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("beam_drain_per_tick", beamDrainPerTick);
        extras.put("beam_regen_per_tick", beamRegenPerTick);
        extras.put("summon_storm_cooldown_ticks", summonStormCooldownTicks);
        extras.put("summon_storm_supercharge_ticks", summonStormSuperchargeTicks);
        extras.put("summon_storm_supercharge_damage_multiplier", summonStormSuperchargeDamageMultiplier);
        extras.put("summon_storm_duration_ticks", summonStormDurationTicks);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("egg_hatch_time_ticks_thunder", eggHatchTimeTicksThunder);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "lightning_beam", DragonAbilityOverride.ofDamage(lightningBeamDamage),
                        "horn_gore", DragonAbilityOverride.ofDamage(hornGoreDamage),
                        "dash", DragonAbilityOverride.ofDamage(dashDamage)
                ),
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "dive_loop_enabled", diveLoopEnabled,
                        "aggressive_wild", aggressiveWild
                )
        );
    }
}
