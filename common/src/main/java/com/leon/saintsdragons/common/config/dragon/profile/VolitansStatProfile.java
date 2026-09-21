package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.HashMap;
import java.util.Map;

public final class VolitansStatProfile {
    private VolitansStatProfile() {
    }

    public static final class Config {
        public static final double RIDER_SWIM_SPEED = 1.42D;
        public static final double MAX_HEALTH = 160.0D;
        public static final double ARMOR = 6.0D;
        public static final double FLYING_SPEED = 0.38D;
        public static final double BITE_DAMAGE = 12.0D;
        public static final double CLAW_DAMAGE = 11.0D;
        public static final double HORN_GORE_DAMAGE = 15.0D;
        public static final double ROAR_GROUND_DAMAGE = 10.0D;
        public static final double ROAR_AIR_WATER_DAMAGE = 7.0D;
        public static final double BURROW_DAMAGE = 30.0D;
        public static final double POISON_BALL_DAMAGE = 12.0D;
        public static final double WATER_BREATH_DAMAGE = 1.8D;
        public static final double POISON_BREATH_DAMAGE = 1.4D;
        public static final double TAMING_CHANCE_BASE = 20.0D;
        public static final double TAMING_CHANCE_HEARTY = 30.0D;
        public static final double TAMING_STUN_HEALTH = 60.0D;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 18000.0D;
        public static final double WILD_FLYING_SPEED_MULTIPLIER = 1.0D;
        public static final double BREATH_ACTIVE_TICKS_MAX = 240.0D;
        public static final double BREATH_DRAIN_PER_TICK = 1.0D / (20.0D * 12.0D);
        public static final double BREATH_REGEN_PER_TICK = 0.0025D;
        public static final double POISON_BREATH_POISON_DURATION_TICKS = 80.0D;
        public static final double POISON_BREATH_POISON_LEVEL = 1.0D;
        public static final double POISON_BALL_POISON_DURATION_TICKS = 120.0D;
        public static final double POISON_BALL_POISON_LEVEL = 1.0D;
        public static final double ROAR_GROUND_POISON_DURATION_TICKS = 1200.0D;
        public static final double ROAR_GROUND_POISON_LEVEL = 3.0D;
        public static final double ROAR_AIR_WATER_POISON_DURATION_TICKS = 200.0D;
        public static final double ROAR_AIR_WATER_POISON_LEVEL = 2.0D;
        public static final boolean LEGACY_TAMING = false;
        public static final boolean AGGRESSIVE_WILD = true;

        private Config() {
        }
    }

    public static final class AirCombatBehaviour {
        public static final double MELEE_RANGE = 6.0D;
        public static final double POISON_MAX_RANGE = 32.0D;
        public static final double ROAR_MAX_RANGE = 12.0D;
        public static final double CHASE_HEIGHT_OFFSET = 2.0D;
        public static final double CHASE_SPEED = 2.3D;
        public static final double DIVE_CHASE_SPEED = 3.5D;
        public static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
        public static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
        public static final double POSITION_SPEED = 0.85D;

        private AirCombatBehaviour() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 12.0f;
        public static final double RANGE = 4.5;

        private BiteAbility() {
        }
    }

    public static final class Brain {
        public static final double GROUND_WANDER_SPEED = 0.9D;
        public static final double FIND_WATER_SPEED = 1.0D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.28D;
        public static final float SWIM_FOLLOW_TURN_DEGREES = 8.0F;
        public static final double SWIM_FOLLOW_SPEED = 0.24D;
        public static final float SWIM_WANDER_TURN_DEGREES = 6.0F;
        public static final double SWIM_WANDER_SPEED = 0.20D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;
        public static final double WATER_CHASE_BREATH_SPEED = 0.16D;
        public static final double WATER_CHASE_SPEED = 0.28D;

        private Brain() {
        }
    }

    public static final class BreathAbility {
        public static final int COOLDOWN_TICKS = 20;

        private BreathAbility() {
        }
    }

    public static final class BurrowAbility {
        public static final double BURROW_MOVEMENT_PARTICLE_SPEED_SQR = 0.015D;
        public static final int COOLDOWN_TICKS = 50;
        public static final float EXIT_DAMAGE = 30.0F;
        public static final double EXIT_RADIUS = 12.0D;
        public static final double EXIT_DUST_COLUMN_SPEED = 0.32D;
        public static final double EXIT_LATE_DUST_RADIUS = 4.2D;
        public static final double EXIT_LATE_DUST_SPEED = 0.18D;
        public static final double EXIT_FALLING_BLOCK_RADIUS = 3.8D;
        public static final double EXIT_FALLING_BLOCK_SPEED_MIN = 0.32D;
        public static final double EXIT_FALLING_BLOCK_SPEED_MAX = 0.62D;
        public static final double EXIT_FALLING_BLOCK_UP_SPEED_MIN = 0.32D;
        public static final double EXIT_FALLING_BLOCK_UP_SPEED_MAX = 0.58D;

        private BurrowAbility() {
        }
    }

    public static final class ClawAbility {
        public static final float BASE_DAMAGE = 11.0f;
        public static final double RANGE = 3.5;

        private ClawAbility() {
        }
    }

    public static final class Entity {
        public static final double BABY_MAX_HEALTH = 60.0D;
        public static final double BABY_ARMOR = 0.0D;
        public static final double RIDER_WALK_SPEED = 0.24D;
        public static final double RIDER_RUN_SPEED = 0.34D;
        public static final double RIDER_BURROW_SPEED = 0.40D;
        public static final double RIDER_SWIM_SPEED = 1.42D;
        public static final int EAT_SOUND_DURATION_TICKS = 34;
        public static final int RIDER_BACK_DASH_COOLDOWN_TICKS = 30;
        public static final int RIDER_DASH_SOUND_TICKS = 60;
        public static final int FLEX_COOLDOWN_TICKS = 65;
        public static final int RIDER_BACK_DASH_LOCK_TICKS = 0;
        public static final int RIDER_BACK_DASH_DURATION_TICKS = 8;
        public static final double RIDER_BACK_DASH_DISTANCE_BLOCKS = 12.0D;
        public static final double RIDER_BACK_DASH_HORIZONTAL_DRAG = 0.90D;
        public static final double RIDER_BACK_DASH_VERTICAL_DRAG = 0.95D;
        public static final int RIDER_BACK_DASH_RECOVERY_TICKS = 5;
        public static final double RIDER_BACK_DASH_RECOVERY_DRAG = 0.82D;
        public static final int RIDER_BACK_DASH_SPIKE_COUNT = 3;
        public static final float RIDER_BACK_DASH_SPIKE_DAMAGE = 5.0F;
        public static final int RIDER_BACK_DASH_SPIKE_POISON_DURATION_TICKS = 100;
        public static final int RIDER_BACK_DASH_SPIKE_POISON_AMPLIFIER = 0;
        public static final float RIDER_BACK_DASH_SPIKE_SPEED = 2.9F;
        public static final float RIDER_BACK_DASH_SPIKE_INACCURACY = 0.10F;
        public static final int RIDER_BACK_DASH_SPIKE_DELAY_TICKS = 8;
        public static final double RIDER_BACK_DASH_SPIKE_Y_OFFSET = -3.0D;
        public static final float REACTIVE_HIT_EVADE_CHANCE = 0.35F;
        public static final int RIDER_FORWARD_DASH_DURATION_TICKS = 25;
        public static final double RIDER_FORWARD_DASH_DISTANCE_BLOCKS = 26.0D;
        public static final double RIDER_FORWARD_DASH_HORIZONTAL_DRAG = 0.90D;
        public static final int RIDER_FORWARD_DASH_DAMAGE_TICK = 14;
        public static final float RIDER_FORWARD_DASH_DAMAGE = 16.0F;
        public static final double RIDER_FORWARD_DASH_DAMAGE_RADIUS = 8.0D;
        public static final int RIDER_SIDE_DODGE_DURATION_TICKS = 7;
        public static final double RIDER_SIDE_DODGE_VERTICAL_DRAG = 0.95D;
        public static final double RIDER_SIDE_DODGE_RECOVERY_DRAG = 0.82D;
        public static final int RIDER_NUDGE_FORWARD_DASH = 1;
        public static final int RIDER_NUDGE_BACK_DASH = 2;
        public static final int SPINE_DROP_COOLDOWN_TICKS = 30;
        public static final double BREED_PARTNER_RANGE = 20.0D;
        public static final double BREED_DISTANCE_SQR = 16.0D;
        public static final double ATTRIBUTE_MOVEMENT_SPEED = 0.30D;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 64.0D;
        public static final double FALLBACK_TAMING_STUN_HEALTH = 60.0D;

        private Entity() {
        }
    }

    public static final class FindSleepDepthBehaviour {
        public static final int HORIZONTAL_RADIUS = 14;
        public static final int COOLDOWN_TICKS = 80;

        private FindSleepDepthBehaviour() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final double BITE_RANGE = 4.1D;
        public static final double CHASE_STOP_RANGE = 3.5D;
        public static final double CLAW_RANGE = 5.1D;
        public static final double GORE_RANGE = 6.2D;
        public static final double POISON_BALL_MIN_RANGE = 8.0D;
        public static final double POISON_BALL_MAX_RANGE = 24.0D;
        public static final double ROAR_OPEN_RANGE = 14.0D;
        public static final float CHASE_SPEED = 1.2F;
        public static final double BURROW_MIN_RANGE = 8.0D;
        public static final double BURROW_MAX_RANGE = 40.0D;
        public static final double BURROW_CHASE_SPEED = 1.55D;
        public static final int INITIAL_CHASE_COMMIT_TICKS = 32;
        public static final int RETREAT_CHASE_COMMIT_TICKS = 36;
        public static final int POST_ABILITY_CHASE_COMMIT_TICKS = 18;
        public static final int FAILED_PRESSURE_CHASE_COMMIT_TICKS = 12;
        public static final double TARGET_STABLE_SPEED = 0.12D;
        public static final double RETREATING_SPEED = 0.07D;

        private GroundCombatBehaviour() {
        }
    }

    public static final class HornGoreAbility {
        public static final float BASE_DAMAGE = 15.0f;
        public static final double RANGE = 4.5;

        private HornGoreAbility() {
        }
    }

    public static final class InteractionHandler {
        public static final double FALLBACK_TAMING_CHANCE_HEARTY = 3.0D;
        public static final double FALLBACK_TAMING_CHANCE_BASE = 5.0D;

        private InteractionHandler() {
        }
    }

    public static final class PoisonBallAbility {
        public static final int COOLDOWN_TICKS = 20;
        public static final double PROJECTILE_SPEED = 3.5D;
        public static final double IMPACT_RADIUS = 5.0D;
        public static final float IMPACT_DAMAGE = 12.0F;
        public static final int POISON_DURATION_TICKS = 120;

        private PoisonBallAbility() {
        }
    }

    public static final class RiderController {
        public static final double SWIM_SPRINT_MULTIPLIER = 1.6D;
        public static final double GROUND_WALK_SPEED = 0.24D;
        public static final double GROUND_RUN_SPEED = 0.34D;
        public static final double BASE_FLIGHT_SPEED_MULT = 4.0;
        public static final double SPRINT_FLIGHT_SPEED_MULT = 5.0;
        public static final double ASCEND_THRUST = 0.45D;
        public static final double DESCEND_THRUST = 0.85D;
        public static final double TERMINAL_VELOCITY = 1.5D;
        public static final double FLIGHT_ACCELERATION = 0.35D;
        public static final double DIVE_SPEED_MULTIPLIER = 2.75D;
        public static final double DIVE_ACCELERATION = 0.30D;
        public static final double SWIM_ASCEND_THRUST = 0.18D;
        public static final double SWIM_DESCEND_THRUST = 0.20D;
        public static final double SWIM_VERTICAL_LIMIT = 0.55D;
        public static final double SWIM_PITCH_VERTICAL_SCALE = 0.65D;

        private RiderController() {
        }
    }

    public static final class RoarAbility {
        public static final int SOUND_DURATION_TICKS = 100;
        public static final int ROAR_EFFECT_DURATION_TICKS = 40;
        public static final float GROUNDED_ROAR_DAMAGE = 10.0F;
        public static final float AIR_WATER_ROAR_DAMAGE = 7.0F;
        public static final int GROUNDED_POISON_DURATION_TICKS = 1200;
        public static final int AIR_WATER_POISON_DURATION_TICKS = 200;
        public static final double GROUNDED_HIT_RADIUS = 20.0D;
        public static final double AIR_WATER_HIT_RADIUS = 12.0D;

        private RoarAbility() {
        }
    }

    public static final class TargetingBehaviour {
        public static final double BABY_PROTECTION_RANGE = 16.0D;

        private TargetingBehaviour() {
        }
    }

    public static final class UltimateAbility {
        public static final int COOLDOWN_TICKS = 40;
        public static final double SLAM_INITIAL_SPEED = -2.5D;
        public static final float BASE_DAMAGE = 24.0F;
        public static final double IMPACT_RADIUS = 20.0D;
        public static final int POISON_DURATION_TICKS = 20 * 30;

        private UltimateAbility() {
        }
    }

    public static final class UnderwaterBreedBehaviour {
        public static final int NEST_SEARCH_RADIUS = 8;
        public static final double COURTSHIP_SWIM_SPEED_SCALE = 0.20D;

        private UnderwaterBreedBehaviour() {
        }
    }

    public static final class WaterCombatBehaviour {
        public static final int ROAR_COOLDOWN_TICKS = 200;
        public static final double BITE_RANGE = 4.1D;
        public static final double CLAW_RANGE = 5.1D;
        public static final double GORE_RANGE = 6.2D;
        public static final double ROAR_MIN_RANGE = 4.5D;
        public static final double ROAR_MAX_RANGE = 12.0D;

        private WaterCombatBehaviour() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double riderSwimSpeed = Config.RIDER_SWIM_SPEED;
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double flyingSpeed = Config.FLYING_SPEED;
        double biteDamage = Config.BITE_DAMAGE;
        double clawDamage = Config.CLAW_DAMAGE;
        double hornGoreDamage = Config.HORN_GORE_DAMAGE;
        double roarGroundDamage = Config.ROAR_GROUND_DAMAGE;
        double roarAirWaterDamage = Config.ROAR_AIR_WATER_DAMAGE;
        double burrowDamage = Config.BURROW_DAMAGE;
        double poisonBallDamage = Config.POISON_BALL_DAMAGE;
        double waterBreathDamage = Config.WATER_BREATH_DAMAGE;
        double poisonBreathDamage = Config.POISON_BREATH_DAMAGE;
        double tamingChanceBase = Config.TAMING_CHANCE_BASE;
        double tamingChanceHearty = Config.TAMING_CHANCE_HEARTY;
        double tamingStunHealth = Config.TAMING_STUN_HEALTH;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        double wildFlyingSpeedMultiplier = Config.WILD_FLYING_SPEED_MULTIPLIER;
        double breathActiveTicksMax = Config.BREATH_ACTIVE_TICKS_MAX;
        double breathDrainPerTick = Config.BREATH_DRAIN_PER_TICK;
        double breathRegenPerTick = Config.BREATH_REGEN_PER_TICK;
        double poisonBreathPoisonDurationTicks = Config.POISON_BREATH_POISON_DURATION_TICKS;
        double poisonBreathPoisonLevel = Config.POISON_BREATH_POISON_LEVEL;
        double poisonBallPoisonDurationTicks = Config.POISON_BALL_POISON_DURATION_TICKS;
        double poisonBallPoisonLevel = Config.POISON_BALL_POISON_LEVEL;
        double roarGroundPoisonDurationTicks = Config.ROAR_GROUND_POISON_DURATION_TICKS;
        double roarGroundPoisonLevel = Config.ROAR_GROUND_POISON_LEVEL;
        double roarAirWaterPoisonDurationTicks = Config.ROAR_AIR_WATER_POISON_DURATION_TICKS;
        double roarAirWaterPoisonLevel = Config.ROAR_AIR_WATER_POISON_LEVEL;
        boolean legacyTaming = Config.LEGACY_TAMING;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("VOLITANS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("VOLITANS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("VOLITANS_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_FLYING_SPEED").get(null));
                riderSwimSpeed = (double) configClass.getField("VOLITANS_RIDER_SWIM_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_RIDER_SWIM_SPEED").get(null));
                biteDamage = (double) configClass.getField("VOLITANS_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BITE_DAMAGE").get(null));
                clawDamage = (double) configClass.getField("VOLITANS_CLAW_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_CLAW_DAMAGE").get(null));
                hornGoreDamage = (double) configClass.getField("VOLITANS_HORN_GORE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_HORN_GORE_DAMAGE").get(null));
                roarGroundDamage = (double) configClass.getField("VOLITANS_ROAR_GROUND_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_DAMAGE").get(null));
                roarAirWaterDamage = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_DAMAGE").get(null));
                burrowDamage = (double) configClass.getField("VOLITANS_BURROW_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BURROW_DAMAGE").get(null));
                poisonBallDamage = (double) configClass.getField("VOLITANS_POISON_BALL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_DAMAGE").get(null));
                waterBreathDamage = (double) configClass.getField("VOLITANS_WATER_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_WATER_BREATH_DAMAGE").get(null));
                poisonBreathDamage = (double) configClass.getField("VOLITANS_POISON_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("VOLITANS_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("VOLITANS_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_CHANCE_HEARTY").get(null));
                tamingStunHealth = (double) configClass.getField("VOLITANS_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_STUN_HEALTH").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("VOLITANS_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_EGG_HATCH_CHANCE_NORMAL").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("VOLITANS_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                breathActiveTicksMax = (double) configClass.getField("VOLITANS_BREATH_ACTIVE_TICKS_MAX").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_ACTIVE_TICKS_MAX").get(null));
                breathDrainPerTick = (double) configClass.getField("VOLITANS_BREATH_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_DRAIN_PER_TICK").get(null));
                breathRegenPerTick = (double) configClass.getField("VOLITANS_BREATH_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_REGEN_PER_TICK").get(null));
                poisonBreathPoisonDurationTicks = (double) configClass.getField("VOLITANS_POISON_BREATH_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_POISON_DURATION_TICKS").get(null));
                poisonBreathPoisonLevel = (double) configClass.getField("VOLITANS_POISON_BREATH_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_POISON_LEVEL").get(null));
                poisonBallPoisonDurationTicks = (double) configClass.getField("VOLITANS_POISON_BALL_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_POISON_DURATION_TICKS").get(null));
                poisonBallPoisonLevel = (double) configClass.getField("VOLITANS_POISON_BALL_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_POISON_LEVEL").get(null));
                roarGroundPoisonDurationTicks = (double) configClass.getField("VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS").get(null));
                roarGroundPoisonLevel = (double) configClass.getField("VOLITANS_ROAR_GROUND_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_POISON_LEVEL").get(null));
                roarAirWaterPoisonDurationTicks = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS").get(null));
                roarAirWaterPoisonLevel = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_LEVEL").get(null));
                legacyTaming = (boolean) configClass.getField("VOLITANS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_LEGACY_TAMING").get(null));
                aggressiveWild = (boolean) configClass.getField("VOLITANS_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, DragonAbilityOverride> abilities = new HashMap<>();
        abilities.put("bite", DragonAbilityOverride.ofDamage(biteDamage));
        abilities.put("claw", DragonAbilityOverride.ofDamage(clawDamage));
        abilities.put("horn_gore", DragonAbilityOverride.ofDamage(hornGoreDamage));
        abilities.put("roar_ground", DragonAbilityOverride.ofDamage(roarGroundDamage));
        abilities.put("roar_air_water", DragonAbilityOverride.ofDamage(roarAirWaterDamage));
        abilities.put("burrow", DragonAbilityOverride.ofDamage(burrowDamage));
        abilities.put("poison_ball", DragonAbilityOverride.ofDamage(poisonBallDamage));
        abilities.put("water_breath", DragonAbilityOverride.ofDamage(waterBreathDamage));
        abilities.put("poison_breath", DragonAbilityOverride.ofDamage(poisonBreathDamage));

        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);
        extras.put("rider_swim_speed", riderSwimSpeed);
        extras.put("breath_active_ticks_max", breathActiveTicksMax);
        extras.put("breath_drain_per_tick", breathDrainPerTick);
        extras.put("breath_regen_per_tick", breathRegenPerTick);
        extras.put("poison_breath_poison_duration_ticks", poisonBreathPoisonDurationTicks);
        extras.put("poison_breath_poison_level", poisonBreathPoisonLevel);
        extras.put("poison_ball_poison_duration_ticks", poisonBallPoisonDurationTicks);
        extras.put("poison_ball_poison_level", poisonBallPoisonLevel);
        extras.put("roar_ground_poison_duration_ticks", roarGroundPoisonDurationTicks);
        extras.put("roar_ground_poison_level", roarGroundPoisonLevel);
        extras.put("roar_air_water_poison_duration_ticks", roarAirWaterPoisonDurationTicks);
        extras.put("roar_air_water_poison_level", roarAirWaterPoisonLevel);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                abilities,
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }
}
