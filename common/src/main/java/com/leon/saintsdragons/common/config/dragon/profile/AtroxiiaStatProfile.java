package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.Map;

public final class AtroxiiaStatProfile {
    private AtroxiiaStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 200.0D;
        public static final double TAMING_STUN_HEALTH = 60.0D;
        public static final double ARMOR = 10.0D;
        public static final double SLAM_DAMAGE = 16.0D;
        public static final double SWIPE_DAMAGE = 12.0D;
        public static final double UNDERWATER_BITE_DAMAGE = 10.0D;
        public static final double GUNGNIR_STAB_DAMAGE = 40.0D;
        public static final double SLITHER_DAMAGE = 5.0D;
        public static final double PRECISE_STRIKE_DAMAGE = 9.0D;
        public static final double PRECISE_STRIKE_KNOCKBACK = 0.75D;
        public static final double PRECISE_STRIKE_STUN_DURATION_TICKS = 40.0D;
        public static final double DEVASTATING_SWEEP_DAMAGE = 13.0D;
        public static final double DEVASTATING_SWEEP_KNOCKBACK = 1.65D;
        public static final double HELHEIM_QUAKE_DAMAGE = 25.0D;
        public static final double HELHEIM_QUAKE_KNOCKBACK = 0.75D;
        public static final double HELHEIM_QUAKE_SECONDARY_KNOCKBACK = 1.8D;
        public static final double HELHEIM_QUAKE_STUN_DURATION_TICKS = 100.0D;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 24000.0D;
        public static final boolean FROST_IMPACT_ENABLED = true;
        public static final boolean AGGRESSIVE_WILD = false;

        private Config() {
        }
    }

    public static final class Brain {
        public static final double BREED_SPEED = 1.0D;
        public static final double GROUND_WANDER_SPEED = 1.0D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.12D;
        public static final double WATER_CHASE_SPEED = 0.30D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;

        private Brain() {
        }
    }

    public static final class DevastatingSweepAbility {
        public static final float BASE_DAMAGE = 13.0F;
        public static final double RADIUS = 12.0D;
        public static final double VERTICAL_RANGE = 6.0D;
        public static final double KNOCKBACK = 1.65D;
        public static final double KNOCKBACK_Y = 0.32D;

        private DevastatingSweepAbility() {
        }
    }

    public static final class Entity {
        public static final int FLEX_COOLDOWN_TICKS = 120;
        public static final double BREED_PARTNER_RANGE = 20.0D;
        public static final double BREED_DISTANCE_SQR = 36.0D;
        public static final double BABY_MAX_HEALTH = 30.0D;
        public static final double BABY_ARMOR = 0.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.33D;
        public static final double RIDER_JUMP_STRENGTH = 1.15D;
        public static final double RIDER_JUMP_FORWARD_BOOST = 0.7D;
        public static final double RIDER_WALK_SPEED = 0.12D;
        public static final double RIDER_RUN_SPEED = 0.28D;
        public static final double PRECISE_STRIKE_NUDGE_DRAG = 0.78D;
        public static final float DEFAULT_TAMING_STUN_HEALTH = 60.0F;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 32.0D;
        public static final double ATTRIBUTE_KNOCKBACK_RESISTANCE = 1.0D;
        public static final double ATTRIBUTE_ATTACK_DAMAGE = 10.0D;

        private Entity() {
        }
    }

    public static final class FrostWalker {
        public static final int MAX_RADIUS = 84;
        public static final int SHORE_VERTICAL_REACH = 10;

        private FrostWalker() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final float CHASE_SPEED = 1.45F;
        public static final double MELEE_STOP_RANGE = 6.0D;
        public static final double SLAM_RANGE = 2.75D;
        public static final double SWIPE_RANGE = 6.0D;
        public static final double PRECISE_STRIKE_MIN_RANGE = 3.0D;
        public static final double PRECISE_STRIKE_MAX_RANGE = 6.5D;
        public static final double GUNGNIR_MIN_RANGE = 4.0D;
        public static final double GUNGNIR_MAX_RANGE = 8.0D;
        public static final double GUNGNIR_MAX_LATERAL_SPEED = 0.16D;
        public static final double GUNGNIR_RETREAT_SPEED = 0.04D;
        public static final double DEVASTATING_SWEEP_POINT_BLANK_RANGE = 2.5D;
        public static final double QUAKE_RANGE = 18.0D;

        private GroundCombatBehaviour() {
        }
    }

    public static final class GungnirStabAbility {
        public static final int SECOND_NUDGE_AND_DAMAGE_TICK = 17;
        public static final double DAMAGE_RANGE = 20.0D;
        public static final double DAMAGE_HORIZONTAL = 3.0D;
        public static final double DAMAGE_VERTICAL = 4.0D;
        public static final float DEFAULT_DAMAGE = 40.0F;

        private GungnirStabAbility() {
        }
    }

    public static final class HelheimQuakeAbility {
        public static final int COOLDOWN_TICKS = 50;
        public static final float DEFAULT_QUAKE_DAMAGE = 25.0F;
        public static final double QUAKE_RADIUS = 20.0D;
        public static final double QUAKE_VERTICAL_RADIUS = 6.0D;
        public static final double DEFAULT_QUAKE_ONE_KNOCKBACK = 0.75D;
        public static final double DEFAULT_QUAKE_TWO_KNOCKBACK = 1.8D;
        public static final int FROST_WALKER_LEVEL = 10;

        private HelheimQuakeAbility() {
        }
    }

    public static final class InteractionHandler {
        public static final double FALLBACK_TAMING_CHANCE_HEARTY = 33.3333D;
        public static final double FALLBACK_TAMING_CHANCE_BASE = 20.0D;

        private InteractionHandler() {
        }
    }

    public static final class PreciseStrikeAbility {
        public static final float BASE_DAMAGE = 10.0F;
        public static final int FIRST_NUDGE_AND_DAMAGE_TICK = 11;
        public static final int SECOND_DAMAGE_TICK = 45;
        public static final int THIRD_DAMAGE_TICK = 64;
        public static final double RANGE = 8.5D;
        public static final double DAMAGE_KNOCKBACK = 0.75D;
        public static final double DAMAGE_KNOCKBACK_Y = 0.16D;

        private PreciseStrikeAbility() {
        }
    }

    public static final class RiderController {
        public static final double SWIM_SPEED = 0.30D;
        public static final double SPRINT_SWIM_SPEED = 0.42D;
        public static final double SWIM_RESPONSE = 0.28D;
        public static final double SWIM_ASCEND_THRUST = 0.10D;
        public static final double SWIM_DESCEND_THRUST = 0.12D;
        public static final double SWIM_VERTICAL_LIMIT = 0.36D;
        public static final double SWIM_PITCH_VERTICAL_SCALE = 0.65D;

        private RiderController() {
        }
    }

    public static final class SlamAbility {
        public static final float BASE_DAMAGE = 16.0F;
        public static final double RANGE = 5.5D;

        private SlamAbility() {
        }
    }

    public static final class SlitherAbility {
        public static final float DEFAULT_CONTACT_DAMAGE = 5.0F;
        public static final double CONTACT_KNOCKBACK = 0.35D;

        private SlitherAbility() {
        }
    }

    public static final class SwipeAbility {
        public static final float BASE_DAMAGE = 12.0F;
        public static final double RANGE = 6.0D;

        private SwipeAbility() {
        }
    }

    public static final class UnderwaterBiteAbility {
        public static final float BASE_DAMAGE = 10.0F;
        public static final double RANGE = 5.0D;
        public static final double CLOSE_HIT_RANGE = 2.5D;

        private UnderwaterBiteAbility() {
        }
    }

    public static final class WaterCombatBehaviour {
        public static final double BITE_RANGE = 5.0D;

        private WaterCombatBehaviour() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double tamingStunHealth = Config.TAMING_STUN_HEALTH;
        double armor = Config.ARMOR;
        double slamDamage = Config.SLAM_DAMAGE;
        double swipeDamage = Config.SWIPE_DAMAGE;
        double underwaterBiteDamage = Config.UNDERWATER_BITE_DAMAGE;
        double gungnirStabDamage = Config.GUNGNIR_STAB_DAMAGE;
        double slitherDamage = Config.SLITHER_DAMAGE;
        double preciseStrikeDamage = Config.PRECISE_STRIKE_DAMAGE;
        double preciseStrikeKnockback = Config.PRECISE_STRIKE_KNOCKBACK;
        double preciseStrikeStunDurationTicks = Config.PRECISE_STRIKE_STUN_DURATION_TICKS;
        double devastatingSweepDamage = Config.DEVASTATING_SWEEP_DAMAGE;
        double devastatingSweepKnockback = Config.DEVASTATING_SWEEP_KNOCKBACK;
        double helheimQuakeDamage = Config.HELHEIM_QUAKE_DAMAGE;
        double helheimQuakeKnockback = Config.HELHEIM_QUAKE_KNOCKBACK;
        double helheimQuakeSecondaryKnockback = Config.HELHEIM_QUAKE_SECONDARY_KNOCKBACK;
        double helheimQuakeStunDurationTicks = Config.HELHEIM_QUAKE_STUN_DURATION_TICKS;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        boolean frostImpactEnabled = Config.FROST_IMPACT_ENABLED;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_MAX_HEALTH");
                tamingStunHealth = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_TAMING_STUN_HEALTH");
                armor = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_ARMOR");
                slamDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_SLAM_DAMAGE");
                swipeDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_SWIPE_DAMAGE");
                underwaterBiteDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_UNDERWATER_BITE_DAMAGE");
                gungnirStabDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_GUNGNIR_STAB_DAMAGE");
                slitherDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_SLITHER_DAMAGE");
                preciseStrikeDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_PRECISE_STRIKE_DAMAGE");
                preciseStrikeKnockback = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_PRECISE_STRIKE_KNOCKBACK");
                preciseStrikeStunDurationTicks = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS");
                devastatingSweepDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_DEVASTATING_SWEEP_DAMAGE");
                devastatingSweepKnockback = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK");
                helheimQuakeDamage = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_HELHEIM_QUAKE_DAMAGE");
                helheimQuakeKnockback = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_HELHEIM_QUAKE_KNOCKBACK");
                helheimQuakeSecondaryKnockback = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK");
                helheimQuakeStunDurationTicks = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS");
                eggHatchTimeTicksNormal = StatProfileConfigSupport.forgeDouble(configClass, "ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL");
                frostImpactEnabled = StatProfileConfigSupport.forgeBoolean(configClass, "ATROXIIA_FROST_IMPACT_ENABLED");
                aggressiveWild = StatProfileConfigSupport.forgeBoolean(configClass, "ATROXIIA_AGGRESSIVE_WILD");
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                0.0D,
                Map.of(
                        "slam", DragonAbilityOverride.ofDamage(slamDamage),
                        "swipe", DragonAbilityOverride.ofDamage(swipeDamage),
                        "underwater_bite", DragonAbilityOverride.ofDamage(underwaterBiteDamage),
                        "gungnir_stab", DragonAbilityOverride.ofDamage(gungnirStabDamage),
                        "slither", DragonAbilityOverride.ofDamage(slitherDamage),
                        "precise_strike", DragonAbilityOverride.ofTuning(
                                preciseStrikeDamage, preciseStrikeKnockback, null,
                                preciseStrikeStunDurationTicks, null),
                        "devastating_sweep", DragonAbilityOverride.ofTuning(
                                devastatingSweepDamage, devastatingSweepKnockback, null, null, null),
                        "helheim_quake", DragonAbilityOverride.ofTuning(
                                helheimQuakeDamage, helheimQuakeKnockback, helheimQuakeSecondaryKnockback,
                                helheimQuakeStunDurationTicks, null),
                        "frost_impact", DragonAbilityOverride.ofTuning(
                                null, null, null, null, frostImpactEnabled)
                ),
                Map.of(
                        "taming_chance_base", 20.0D,
                        "taming_chance_hearty", 33.3333D,
                        "taming_stun_health", tamingStunHealth,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal
                ),
                Map.of(
                        "legacy_taming", false,
                        "aggressive_wild", aggressiveWild
                )
        );
    }
}
