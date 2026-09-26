package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.HashMap;
import java.util.Map;

public final class IgnivorusStatProfile {
    private IgnivorusStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 450.0D;
        public static final double ARMOR = 4.0D;
        public static final double FLYING_SPEED = 0.35D;
        public static final double BITE_DAMAGE = 50.0D;
        public static final double BODY_SLAM_DAMAGE = 40.0D;
        public static final double LEAP_SLAM_DAMAGE = 50.0D;
        public static final double FIRE_BREATH_DAMAGE = 80.0D;
        public static final double FIREBALL_DAMAGE = 70.0D;
        public static final double MAGMA_PILLAR_DAMAGE = 18.0D;
        public static final double WING_SWIPE_DAMAGE = 15.0D;
        public static final double STOMP_DAMAGE = 18.0D;
        public static final double BULLDOZE_DAMAGE = 10.0D;
        public static final double ULTIMATE_DAMAGE = 200.0D;
        public static final double ULTIMATE_PENALTY_HEALTH = 50.0D;
        public static final double ULTIMATE_TRIGGER_HEALTH_FRACTION = 0.6D;
        public static final double TAMING_CHANCE_BASE = 14.2857D;
        public static final double TAMING_CHANCE_BEEF = 20.0D;
        public static final double TAMING_CHANCE_MUTTON = 14.2857D;
        public static final double TAMING_CHANCE_PORKCHOP = 14.2857D;
        public static final double TAMING_CHANCE_HEARTY = 25.0D;
        public static final boolean LEGACY_TAMING = false;
        public static final double FIRE_BREATH_DRAIN_PER_TICK = 0.004166666666666667D;
        public static final double FIRE_BREATH_REGEN_PER_TICK = 0.0025D;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 36000.0D;
        public static final double TAMING_STUN_HEALTH = MAX_HEALTH * (1.0D / 3.0D);
        public static final double WILD_FLYING_SPEED_MULTIPLIER = 1.0D;
        public static final boolean AGGRESSIVE_WILD = false;

        private Config() {
        }
    }

    public static final class ForgeDefaults {
        public static final double TAMING_STUN_HEALTH = 100.0;

        private ForgeDefaults() {
        }
    }

    public static final class AirCombatBehaviour {
        public static final double CHASE_SPEED = 6.25D;
        public static final double DIVE_SPEED = 7.0D;
        public static final double APPROACH_SPEED = 2.4D;
        public static final double BREATH_PASS_SPEED = 2.8D;
        public static final double EGRESS_SPEED = 3.5D;

        private AirCombatBehaviour() {
        }
    }

    public static final class Landing {
        public static final double APPROACH_SPEED = 2.4D;

        private Landing() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 50.0f;
        public static final float ARMOR_PENETRATION = 5.0f;
        public static final double RANGE = 6.0;
        public static final double AIR_RANGE_BONUS = 2.0;

        private BiteAbility() {
        }
    }

    public static final class BodySlamAbility {
        public static final int COOLDOWN_TICKS = 20;
        public static final float BASE_DAMAGE = 40.0f;

        private BodySlamAbility() {
        }
    }

    public static final class Brain {
        public static final double BREED_SPEED = 1.0D;
        public static final double FOLLOW_PARENT_SPEED = 1.1D;
        public static final double GROUND_WANDER_SPEED = 1.2D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.12D;
        public static final float ROOST_RETURN_GROUND_SPEED = 1.0F;
        public static final double ROOST_RETURN_SWIM_SPEED = 0.25D;
        public static final float ROOST_RETURN_SWIM_TURN_DEGREES = 8.0F;
        public static final double WATER_CHASE_SPEED = 0.12D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;

        private Brain() {
        }
    }

    public static final class Entity {
        public static final double BASE_FOLLOW_RANGE = 128.0D;
        public static final float FIRE_BREATH_ENERGY_REGEN = 0.0025f;
        public static final float BARREL_ROLL_INPUT_SPEED = 0.235f;
        public static final int RIDER_LANDING_BLEND_DURATION = 5;
        public static final double BREED_PARTNER_RANGE = 20.0D;
        public static final double BREED_DISTANCE_SQR = 2500.0D;
        public static final double RIDER_WALK_SPEED = 0.25D;
        public static final double RIDER_RUN_SPEED = 0.55D;
        public static final double RIDER_BULLDOZE_SPEED = 0.55D;
        public static final double RIDER_PHASE2_WALK_SPEED = 0.15D;
        public static final double RIDER_PHASE2_RUN_SPEED = 0.32D;
        public static final double BULLDOZE_TUNNEL_REACH = 2.5D;
        public static final double BULLDOZE_BODY_COLLISION_REACH = 8.0D;
        public static final double BULLDOZE_DAMAGE_FORWARD_REACH = 4.5D;
        public static final double BULLDOZE_DAMAGE_HALF_WIDTH = 7.0D;
        public static final double BULLDOZE_DAMAGE_HALF_HEIGHT = 2.5D;
        public static final double LEAP_ARC_FORWARD_DISTANCE = 42.0D;
        public static final double LEAP_ARC_HEIGHT = 15.0D;
        public static final int LEAP_ARC_ASCENT_TICKS = 20;
        public static final int LEAP_ARC_DESCENT_TICKS = 10;
        public static final float LEAP_SLAM_DAMAGE = 50.0F;
        public static final float DEFAULT_BULLDOZE_DAMAGE = 10.0F;
        public static final double LEAP_SLAM_RADIUS = 20.0D;
        public static final double LEAP_KNOCKBACK = 5.5D;
        public static final double LEAP_LIFT = 0.8D;
        public static final double LEAP_IMPACT_TRIGGER_HEIGHT = 7.0D;
        public static final int LEAP_GROUNDED_FAILSAFE_TICKS = 6;
        public static final int LEAP_COOLDOWN_TICKS = 140;
        public static final int LEAP_WINDUP_TICKS = 20;
        public static final int LEAP_STATE_NONE = 0;
        public static final int LEAP_STATE_TAKEOFF = 1;
        public static final int LEAP_IMPACT_RECOVERY_DURATION = 18;
        public static final int FLEX_COOLDOWN_TICKS = 200;
        public static final double BABY_MAX_HEALTH = 90.0D;
        public static final double BABY_ARMOR = 0.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.30D;
        public static final double ROOST_SLEEP_RADIUS = 6.0D;
        public static final double ROOST_TERRITORY_RADIUS = 64.0D;
        public static final double ROOST_TERRITORY_RETURN_RADIUS = 48.0D;
        public static final double ROOST_WANDER_RADIUS = 56.0D;
        public static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;
        public static final double ATTRIBUTE_KNOCKBACK_RESISTANCE = 2.0D;
        public static final double FALLBACK_BITE = 15.0D;
        public static final double FALLBACK_ULTIMATE_TRIGGER_HEALTH_FRACTION = 0.6D;

        private Entity() {
        }
    }

    public static final class FireballAbility {
        public static final int COOLDOWN_TICKS = 20;
        public static final double FIREBALL_SPEED = 5.0D;
        public static final double BASE_IMPACT_RADIUS = 8.0D;
        public static final float DEFAULT_IMPACT_DAMAGE = 70.0F;
        public static final float LEVEL_2_MULTIPLIER = 1.5F;
        public static final float LEVEL_3_MULTIPLIER = 2.0F;

        private FireballAbility() {
        }
    }

    public static final class FireBreathAbility {
        public static final int COOLDOWN_TICKS = 40;

        private FireBreathAbility() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final double MELEE_ENGAGE_RANGE = 6.0D;
        public static final float CHASE_SPEED = 1.75F;
        public static final int FIREBALL_DECISION_COOLDOWN_TICKS = 140;
        public static final int FIREBALL_POST_COOLDOWN_TICKS = 200;
        public static final double AI_PHASE2_LEAP_TRIGGER_GAP = 24.0;
        public static final double AI_PHASE2_LEAP_MAX_GAP = 56.0;
        public static final int AI_PHASE2_LEAP_POST_COOLDOWN = 30;

        private GroundCombatBehaviour() {
        }
    }

    public static final class InteractionHandler {
        public static final double FALLBACK_TAMING_CHANCE_HEARTY = 25.0D;
        public static final double FALLBACK_TAMING_CHANCE_BEEF = 20.0D;
        public static final double FALLBACK_TAMING_CHANCE_MUTTON = 14.2857D;
        public static final double FALLBACK_TAMING_CHANCE_PORKCHOP = 14.2857D;
        public static final double FALLBACK_TAMING_CHANCE_BASE = 14.2857D;

        private InteractionHandler() {
        }
    }

    public static final class RiderController {
        public static final double BASE_FLIGHT_SPEED_MULT = 3.95;
        public static final double SPRINT_FLIGHT_SPEED_MULT = 4.75;
        public static final double DRAG_NO_INPUT = 0.5;
        public static final double ASCEND_THRUST = 0.45D;
        public static final double DESCEND_THRUST = 1.0D;
        public static final double TERMINAL_VELOCITY = 1.5D;
        public static final double FLIGHT_ACCELERATION = 0.35D;
        public static final double DIVE_SPEED_MULTIPLIER = 2.0D;
        public static final double DIVE_ACCELERATION = 0.30D;

        private RiderController() {
        }
    }

    public static final class RoarAbility {
        public static final float BASE_DAMAGE = 18.0f;
        public static final float DAMAGE_PER_WAVE = 4.0f;
        public static final double BASE_KNOCKBACK = 0.9D;
        public static final double KNOCKBACK_PER_WAVE = 0.2D;

        private RoarAbility() {
        }
    }

    public static final class StompAbility {
        public static final float DEFAULT_DAMAGE = 18.0f;
        public static final double AOE_RADIUS = 18.0;

        private StompAbility() {
        }
    }

    public static final class TargetingBehaviour {
        public static final double BABY_PROTECTION_RANGE = 16.0D;

        private TargetingBehaviour() {
        }
    }

    public static final class UltimateAbility {
        public static final int COOLDOWN_TICKS_RIDER = 0;
        public static final int COOLDOWN_TICKS_AI = 6000;
        public static final float EXPLOSION_DAMAGE = 200.0F;
        public static final float PENALTY_HEALTH = 50.0F;

        private UltimateAbility() {
        }
    }

    public static final class WingSwipeAbility {
        public static final float DEFAULT_DAMAGE = 15.0f;
        public static final double AOE_RADIUS = 22.0;
        public static final double KNOCKBACK_STRENGTH = 4.0;

        private WingSwipeAbility() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double flyingSpeed = Config.FLYING_SPEED;
        double biteDamage = Config.BITE_DAMAGE;
        double bodySlamDamage = Config.BODY_SLAM_DAMAGE;
        double leapSlamDamage = Config.LEAP_SLAM_DAMAGE;
        double fireBreathDamage = Config.FIRE_BREATH_DAMAGE;
        double fireballDamage = Config.FIREBALL_DAMAGE;
        double magmaPillarDamage = Config.MAGMA_PILLAR_DAMAGE;
        double wingSwipeDamage = Config.WING_SWIPE_DAMAGE;
        double stompDamage = Config.STOMP_DAMAGE;
        double bulldozeDamage = Config.BULLDOZE_DAMAGE;
        double ultimateDamage = Config.ULTIMATE_DAMAGE;
        double ultimatePenaltyHealth = Config.ULTIMATE_PENALTY_HEALTH;
        double ultimateTriggerHealthFraction = Config.ULTIMATE_TRIGGER_HEALTH_FRACTION;
        double tamingChanceBase = Config.TAMING_CHANCE_BASE;
        double tamingChanceBeef = Config.TAMING_CHANCE_BEEF;
        double tamingChanceMutton = Config.TAMING_CHANCE_MUTTON;
        double tamingChancePorkchop = Config.TAMING_CHANCE_PORKCHOP;
        double tamingChanceHearty = Config.TAMING_CHANCE_HEARTY;
        boolean legacyTaming = Config.LEGACY_TAMING;
        double fireBreathDrainPerTick = Config.FIRE_BREATH_DRAIN_PER_TICK;
        double fireBreathRegenPerTick = Config.FIRE_BREATH_REGEN_PER_TICK;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        double tamingStunHealth = Config.TAMING_STUN_HEALTH;
        double wildFlyingSpeedMultiplier = Config.WILD_FLYING_SPEED_MULTIPLIER;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("IGNIVORUS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("IGNIVORUS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("IGNIVORUS_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null));
                bodySlamDamage = (double) configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null));
                leapSlamDamage = (double) configClass.getField("IGNIVORUS_LEAP_SLAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_LEAP_SLAM_DAMAGE").get(null));
                fireBreathDamage = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null));
                fireballDamage = (double) configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null));
                magmaPillarDamage = (double) configClass.getField("IGNIVORUS_MAGMA_PILLAR_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_MAGMA_PILLAR_DAMAGE").get(null));
                wingSwipeDamage = (double) configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null));
                stompDamage = (double) configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null));
                bulldozeDamage = (double) configClass.getField("IGNIVORUS_BULLDOZE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BULLDOZE_DAMAGE").get(null));
                ultimateDamage = (double) configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null));
                ultimatePenaltyHealth = (double) configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null));
                ultimateTriggerHealthFraction = (double) configClass.getField("IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION").get(null));
                tamingChanceBase = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null));
                tamingChanceBeef = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_BEEF").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_BEEF").get(null));
                tamingChanceMutton = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_MUTTON").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_MUTTON").get(null));
                tamingChancePorkchop = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_PORKCHOP").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_PORKCHOP").get(null));
                tamingChanceHearty = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null));
                legacyTaming = (boolean) configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null));
                fireBreathDrainPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null));
                fireBreathRegenPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("IGNIVORUS_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_EGG_HATCH_CHANCE_NORMAL").get(null));
                tamingStunHealth = (double) configClass.getField("IGNIVORUS_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_STUN_HEALTH").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                aggressiveWild = (boolean) configClass.getField("IGNIVORUS_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, Double> extras = new HashMap<>();
        extras.put("ultimate_penalty_health", ultimatePenaltyHealth);
        extras.put("ultimate_trigger_health_fraction", ultimateTriggerHealthFraction);
        extras.put("fire_breath_drain_per_tick", fireBreathDrainPerTick);
        extras.put("fire_breath_regen_per_tick", fireBreathRegenPerTick);
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_beef", tamingChanceBeef);
        extras.put("taming_chance_mutton", tamingChanceMutton);
        extras.put("taming_chance_porkchop", tamingChancePorkchop);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "body_slam", DragonAbilityOverride.ofDamage(bodySlamDamage),
                        "leap_slam", DragonAbilityOverride.ofDamage(leapSlamDamage),
                        "fire_breath", DragonAbilityOverride.ofDamage(fireBreathDamage),
                        "fireball", DragonAbilityOverride.ofDamage(fireballDamage),
                        "magma_pillar", DragonAbilityOverride.ofDamage(magmaPillarDamage),
                        "wing_swipe", DragonAbilityOverride.ofDamage(wingSwipeDamage),
                        "stomp", DragonAbilityOverride.ofDamage(stompDamage),
                        "bulldoze", DragonAbilityOverride.ofDamage(bulldozeDamage),
                        "ultimate", DragonAbilityOverride.ofDamage(ultimateDamage)
                ),
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }
}
