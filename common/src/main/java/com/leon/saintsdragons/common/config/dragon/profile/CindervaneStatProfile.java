package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.Map;

public final class CindervaneStatProfile {
    private CindervaneStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 80.0D;
        public static final double ARMOR = 4.0D;
        public static final double FLYING_SPEED = 0.60D;
        public static final double BITE_DAMAGE = 12.0D;
        public static final double DOUBLE_BITE_DAMAGE = 15.0D;
        public static final double SLASH_GRAB_HIT1_DAMAGE = 5.0D;
        public static final double SLASH_GRAB_HIT2_DAMAGE = 7.0D;
        public static final double MAGMA_VOLLEY_DAMAGE = 20.0D;
        public static final double MAGMA_VOLLEY_COOLDOWN_SECONDS = 20.0D;
        public static final double FIRE_BODY_DAMAGE = 3.0D;
        public static final double TAMING_CHANCE_BASE = 25.0D;
        public static final double TAMING_CHANCE_CHICKEN = 33.3333D;
        public static final double TAMING_CHANCE_HEARTY = 50.0D;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 12000.0D;
        public static final double FIRE_BODY_EXPLOSION_DAMAGE = 200.0D;
        public static final double FIRE_BODY_SELF_DAMAGE_ON_CRASH = 40.0D;
        public static final double WILD_FLYING_SPEED_MULTIPLIER = 1.0D;
        public static final boolean AGGRESSIVE_WILD = false;

        private Config() {
        }
    }

    public static final class AirCombatMovementBehaviour {
        public static final double BITE_RANGE = 5.75D;
        public static final double DOUBLE_BITE_RANGE = 5.25D;
        public static final double FIRE_BODY_RANGE = 3.5D;
        public static final double FIRE_BODY_EXIT_RANGE = 11.0D;
        public static final double CHASE_HEIGHT_OFFSET = 0.5D;
        public static final double CHASE_SPEED = 2.3D;
        public static final double DIVE_CHASE_SPEED = 3.5D;
        public static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
        public static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
        public static final int POST_ABILITY_CHASE_TICKS = 8;

        private AirCombatMovementBehaviour() {
        }
    }

    public static final class AutonomousFlightBehaviour {
        public static final double CRUISE_SPEED = 1.25D;
        public static final double AUTONOMOUS_DIVE_SPEED = 2.2D;

        private AutonomousFlightBehaviour() {
        }
    }

    public static final class Landing {
        public static final double APPROACH_SPEED = 2.2D;

        private Landing() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 12.0f;
        public static final double RANGE = 4;
        public static final double AIR_RANGE_BONUS = 0.6;

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

    public static final class DoubleBiteAbility {
        public static final float BASE_DAMAGE_PER_BITE = 15.0F;

        private DoubleBiteAbility() {
        }
    }

    public static final class Entity {
        public static final double BABY_MAX_HEALTH = 40.0D;
        public static final double BABY_ARMOR = 0.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.30D;
        public static final float FIRE_BODY_EXPLOSION_RADIUS = 15.0F;
        public static final double FIRE_BODY_IMPRINT_RADIUS = 9.0D;
        public static final int RIDER_LANDING_BLEND_DURATION = 3;
        public static final float FIRE_BODY_EXPLOSION_DAMAGE = 200.0F;
        public static final float FIRE_BODY_SELF_DAMAGE_ON_CRASH = 40.0F;
        public static final double BREED_PARTNER_RANGE = 20.0D;
        public static final double BREED_DISTANCE_SQR = 2500.0D;
        public static final double PACK_SEARCH_RADIUS = 48.0D;
        public static final int FLEX_COOLDOWN_TICKS = 120;
        public static final double RIDER_WALK_SPEED = 0.18D;
        public static final double RIDER_RUN_SPEED = 0.26D;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 64.0D;

        private Entity() {
        }
    }

    public static final class FireballVolleyAbility {
        public static final double DEFAULT_COOLDOWN_SECONDS = 20.0D;
        public static final double VELOCITY_DOWN = -0.15D;
        public static final double VELOCITY_FORWARD = 0.55D;
        public static final double MAGMA_IMPACT_RADIUS = 7.0D;
        public static final float DEFAULT_IMPACT_DAMAGE = 20.0F;

        private FireballVolleyAbility() {
        }
    }

    public static final class FireBodyAbility {
        public static final double AURA_RADIUS = 3.5D;
        public static final float BASE_DAMAGE = 3.0F;
        public static final double COOKING_RADIUS = 3.5D;
        public static final int ALLY_DAMAGE_RESIST_TICKS = 40;

        private FireBodyAbility() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final float CHASE_SPEED = 1.15F;
        public static final double MELEE_STOP_RANGE = 5.0D;
        public static final double BITE_RANGE = 4.5D;
        public static final double DOUBLE_BITE_RANGE = 4.75D;
        public static final double SLASH_GRAB_MIN_RANGE = 2.0D;
        public static final double SLASH_GRAB_MAX_RANGE = 6.25D;
        public static final double BOMBARDMENT_MIN_RANGE = 8.0D;
        public static final double BOMBARDMENT_MAX_RANGE = 32.0D;
        public static final double FIRE_BODY_POINT_BLANK_RANGE = 3.25D;
        public static final double FIRE_BODY_GROUP_RANGE = 6.5D;
        public static final double FIRE_BODY_EXIT_RANGE = 11.0D;
        public static final int INITIAL_CHASE_COMMIT_TICKS = 12;
        public static final int RETREAT_CHASE_COMMIT_TICKS = 18;
        public static final int POST_ABILITY_CHASE_COMMIT_TICKS = 8;

        private GroundCombatBehaviour() {
        }
    }

    public static final class PackFlightCoordinator {
        public static final double NEIGHBOR_RADIUS = 42.0D;
        public static final double SEPARATION_RANGE = 30.0D;
        public static final double FOLLOW_FORMATION_WEIGHT = 1.20D;
        public static final double FOLLOW_COHESION_WEIGHT = 0.25D;
        public static final double FOLLOW_ALIGNMENT_WEIGHT = 0.35D;
        public static final double FOLLOW_SEPARATION_WEIGHT = 1.55D;

        private PackFlightCoordinator() {
        }
    }

    public static final class RiderController {
        public static final double BASE_FLIGHT_SPEED_MULT = 3.0;
        public static final double SPRINT_FLIGHT_SPEED_MULT = 4.0;
        public static final double DRAG_NO_INPUT = 0.45;
        public static final double ASCEND_THRUST = 0.45D;
        public static final double DESCEND_THRUST = 0.85D;
        public static final double TERMINAL_VELOCITY = 1.2D;
        public static final double FLIGHT_ACCELERATION = 0.35D;
        public static final double DIVE_SPEED_MULTIPLIER = 2.75D;
        public static final double DIVE_ACCELERATION = 0.30D;

        private RiderController() {
        }
    }

    public static final class SlashGrabAbility {
        public static final float DEFAULT_DAMAGE_HIT_1 = 5.0f;
        public static final float DEFAULT_DAMAGE_HIT_2 = 7.0f;
        public static final double GRAB_SEARCH_RADIUS = 3.00D;

        private SlashGrabAbility() {
        }
    }

    public static final class TargetingBehaviour {
        public static final double BABY_PROTECTION_RANGE = 16.0D;
        public static final double PACK_ASSIST_RANGE = 36.0D;

        private TargetingBehaviour() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double flyingSpeed = Config.FLYING_SPEED;
        double biteDamage = Config.BITE_DAMAGE;
        double doubleBiteDamage = Config.DOUBLE_BITE_DAMAGE;
        double slashGrabHit1Damage = Config.SLASH_GRAB_HIT1_DAMAGE;
        double slashGrabHit2Damage = Config.SLASH_GRAB_HIT2_DAMAGE;
        double magmaVolleyDamage = Config.MAGMA_VOLLEY_DAMAGE;
        double magmaVolleyCooldownSeconds = Config.MAGMA_VOLLEY_COOLDOWN_SECONDS;
        double fireBodyDamage = Config.FIRE_BODY_DAMAGE;
        double tamingChanceBase = Config.TAMING_CHANCE_BASE;
        double tamingChanceChicken = Config.TAMING_CHANCE_CHICKEN;
        double tamingChanceHearty = Config.TAMING_CHANCE_HEARTY;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        double fireBodyExplosionDamage = Config.FIRE_BODY_EXPLOSION_DAMAGE;
        double fireBodySelfDamageOnCrash = Config.FIRE_BODY_SELF_DAMAGE_ON_CRASH;
        double wildFlyingSpeedMultiplier = Config.WILD_FLYING_SPEED_MULTIPLIER;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("CINDERVANE_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("CINDERVANE_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("CINDERVANE_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("CINDERVANE_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_BITE_DAMAGE").get(null));
                doubleBiteDamage = (double) configClass.getField("CINDERVANE_DOUBLE_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_DOUBLE_BITE_DAMAGE").get(null));
                slashGrabHit1Damage = (double) configClass.getField("CINDERVANE_SLASH_GRAB_HIT1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_SLASH_GRAB_HIT1_DAMAGE").get(null));
                slashGrabHit2Damage = (double) configClass.getField("CINDERVANE_SLASH_GRAB_HIT2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_SLASH_GRAB_HIT2_DAMAGE").get(null));
                magmaVolleyDamage = (double) configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null));
                magmaVolleyCooldownSeconds = (double) configClass.getField("CINDERVANE_MAGMA_VOLLEY_COOLDOWN_SECONDS").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAGMA_VOLLEY_COOLDOWN_SECONDS").get(null));
                fireBodyDamage = (double) configClass.getField("CINDERVANE_FIRE_BODY_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FIRE_BODY_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("CINDERVANE_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_TAMING_CHANCE_BASE").get(null));
                tamingChanceChicken = (double) configClass.getField("CINDERVANE_TAMING_CHANCE_CHICKEN").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_TAMING_CHANCE_CHICKEN").get(null));
                tamingChanceHearty = (double) configClass.getField("CINDERVANE_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_TAMING_CHANCE_HEARTY").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("CINDERVANE_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_EGG_HATCH_CHANCE_NORMAL").get(null));
                fireBodyExplosionDamage = (double) configClass.getField("CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE").get(null));
                fireBodySelfDamageOnCrash = (double) configClass.getField("CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                aggressiveWild = (boolean) configClass.getField("CINDERVANE_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "double_bite", DragonAbilityOverride.ofDamage(doubleBiteDamage),
                        "slash_grab_hit1", DragonAbilityOverride.ofDamage(slashGrabHit1Damage),
                        "slash_grab_hit2", DragonAbilityOverride.ofDamage(slashGrabHit2Damage),
                        "magma_volley", DragonAbilityOverride.ofDamage(magmaVolleyDamage),
                        "fire_body", DragonAbilityOverride.ofDamage(fireBodyDamage)
                ),
                Map.of(
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_chicken", tamingChanceChicken,
                        "taming_chance_hearty", tamingChanceHearty,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal,
                        "magma_volley_cooldown_seconds", magmaVolleyCooldownSeconds,
                        "fire_body_explosion_damage", fireBodyExplosionDamage,
                        "fire_body_self_damage_on_crash", fireBodySelfDamageOnCrash,
                        "wild_flying_speed_multiplier", wildFlyingSpeedMultiplier
                ),
                Map.of("aggressive_wild", aggressiveWild)
        );
    }
}
