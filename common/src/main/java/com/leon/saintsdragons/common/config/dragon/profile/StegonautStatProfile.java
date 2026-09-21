package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.Map;

public final class StegonautStatProfile {
    private StegonautStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 100.0D;
        public static final double ARMOR = 15.0D;
        public static final double BITE_DAMAGE = 5.0D;
        public static final double CHIN_SLAM_DAMAGE = 8.0D;
        public static final double GROUND_EATING_DAMAGE = 10.0D;
        public static final double GROUND_SLAM_DAMAGE = 20.0D;
        public static final double GROUND_SLAM_KNOCKBACK = 1.35D;
        public static final double GROUND_SLAM2_DAMAGE = 25.0D;
        public static final double GROUND_SLAM2_KNOCKBACK = 1.8D;
        public static final double GROUND_SLAM_PILLAR_DAMAGE = 10.0D;
        public static final double GROUND_SLAM_PILLAR_KNOCKBACK = 0.9D;
        public static final double TAMING_CHANCE_BASE = 100.0D;
        public static final double TAMING_CHANCE_HEARTY = 100.0D;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 30000.0D;
        public static final boolean AGGRESSIVE_WILD = false;

        private Config() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 5.0f;
        public static final double RANGE = 4.0;

        private BiteAbility() {
        }
    }

    public static final class Brain {
        public static final float GROUND_CHASE_SPEED = 0.75F;
        public static final double BREED_SPEED = 1.0D;
        public static final double FOLLOW_PARENT_SPEED = 0.70D;
        public static final double GROUND_WANDER_SPEED = 0.80D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.12D;
        public static final double WATER_CHASE_SPEED = 0.12D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;

        private Brain() {
        }
    }

    public static final class BuffAbility {
        public static final double BUFF_RANGE = 8.0D;
        public static final int BUFF_DURATION_TICKS = 40;
        public static final int RESISTANCE_AMPLIFIER = 0;

        private BuffAbility() {
        }
    }

    public static final class ChinSlamAbility {
        public static final float BASE_DAMAGE = 8.0f;
        public static final float ARMOR_PENETRATION = 4.0f;
        public static final double RANGE = 5.0;

        private ChinSlamAbility() {
        }
    }

    public static final class Entity {
        public static final double BREED_PARTNER_RANGE = 20.0D;
        public static final double BREED_DISTANCE_SQR = 2500.0D;
        public static final double BABY_MAX_HEALTH = 50.0D;
        public static final double BABY_ARMOR = 5.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.28D;
        public static final double RIDER_JUMP_STRENGTH = 0.75D;
        public static final double RIDER_JUMP_FORWARD_BOOST = 0.7D;
        public static final int FLEX_COOLDOWN_TICKS = 160;
        public static final double RIDER_WALK_SPEED = 0.1D;
        public static final double RIDER_RUN_SPEED = 0.25D;
        public static final double PACK_SEARCH_RADIUS = 48.0D;
        public static final double ATTRIBUTE_MAX_HEALTH = 100.0D;
        public static final double ATTRIBUTE_KNOCKBACK_RESISTANCE = 1.0D;
        public static final double ATTRIBUTE_ATTACK_DAMAGE = 2.0D;
        public static final double ATTRIBUTE_ARMOR = 15.0D;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 32.0D;

        private Entity() {
        }
    }

    public static final class GroundCombatBehaviour {
        public static final double GROUND_ATTACK_RANGE = 3.4D;
        public static final double WATER_ATTACK_RANGE = 6.0D;
        public static final int ATTACK_COOLDOWN_TICKS = 26;

        private GroundCombatBehaviour() {
        }
    }

    public static final class GroundEatingAbility {
        public static final int COOLDOWN_TICKS = 30;
        public static final double PROJECTILE_SPEED = 2.5D;
        public static final double PROJECTILE_RADIUS = 3.2D;
        public static final float PROJECTILE_DAMAGE = 10.0F;

        private GroundEatingAbility() {
        }
    }

    public static final class GroundSlamAbility {
        public static final int COOLDOWN_TICKS = 60;
        public static final float DEFAULT_SLAM_DAMAGE = 20.0F;
        public static final float DEFAULT_PILLAR_DAMAGE = 10.0F;
        public static final double SLAM_RADIUS = 10.0D;
        public static final double DEFAULT_SLAM_KNOCKBACK = 1.35D;
        public static final float DEFAULT_SLAM2_DAMAGE = 25.0F;
        public static final double SLAM2_RADIUS = 20.0D;
        public static final double DEFAULT_SLAM2_KNOCKBACK = 1.8D;
        public static final double DEFAULT_PILLAR_KNOCKBACK = 0.9D;

        private GroundSlamAbility() {
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
        double biteDamage = Config.BITE_DAMAGE;
        double chinSlamDamage = Config.CHIN_SLAM_DAMAGE;
        double groundEatingDamage = Config.GROUND_EATING_DAMAGE;
        double groundSlamDamage = Config.GROUND_SLAM_DAMAGE;
        double groundSlamKnockback = Config.GROUND_SLAM_KNOCKBACK;
        double groundSlam2Damage = Config.GROUND_SLAM2_DAMAGE;
        double groundSlam2Knockback = Config.GROUND_SLAM2_KNOCKBACK;
        double groundSlamPillarDamage = Config.GROUND_SLAM_PILLAR_DAMAGE;
        double groundSlamPillarKnockback = Config.GROUND_SLAM_PILLAR_KNOCKBACK;
        double tamingChanceBase = Config.TAMING_CHANCE_BASE;
        double tamingChanceHearty = Config.TAMING_CHANCE_HEARTY;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("STEGONAUT_MAX_HEALTH").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("STEGONAUT_ARMOR").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_ARMOR").get(null));
                biteDamage = (double) configClass.getField("STEGONAUT_BITE_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_BITE_DAMAGE").get(null));
                chinSlamDamage = (double) configClass.getField("STEGONAUT_CHIN_SLAM_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_CHIN_SLAM_DAMAGE").get(null));
                groundEatingDamage = (double) configClass.getField("STEGONAUT_GROUND_EATING_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_EATING_DAMAGE").get(null));
                groundSlamDamage = (double) configClass.getField("STEGONAUT_GROUND_SLAM_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM_DAMAGE").get(null));
                groundSlamKnockback = (double) configClass.getField("STEGONAUT_GROUND_SLAM_KNOCKBACK").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM_KNOCKBACK").get(null));
                groundSlam2Damage = (double) configClass.getField("STEGONAUT_GROUND_SLAM2_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM2_DAMAGE").get(null));
                groundSlam2Knockback = (double) configClass.getField("STEGONAUT_GROUND_SLAM2_KNOCKBACK").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM2_KNOCKBACK").get(null));
                groundSlamPillarDamage = (double) configClass.getField("STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE").get(null));
                groundSlamPillarKnockback = (double) configClass.getField("STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK").get(null));
                tamingChanceBase = (double) configClass.getField("STEGONAUT_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("STEGONAUT_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_TAMING_CHANCE_HEARTY").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("STEGONAUT_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_EGG_HATCH_CHANCE_NORMAL").get(null));
                aggressiveWild = (boolean) configClass.getField("STEGONAUT_AGGRESSIVE_WILD").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("STEGONAUT_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                0.0D,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "chin_slam", DragonAbilityOverride.ofDamage(chinSlamDamage),
                        "ground_eating", DragonAbilityOverride.ofDamage(groundEatingDamage),
                        "ground_slam", DragonAbilityOverride.ofDamage(groundSlamDamage),
                        "ground_slam2", DragonAbilityOverride.ofDamage(groundSlam2Damage),
                        "ground_slam_pillar", DragonAbilityOverride.ofDamage(groundSlamPillarDamage)
                ),
                Map.of(
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_hearty", tamingChanceHearty,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal,
                        "ground_slam_knockback", groundSlamKnockback,
                        "ground_slam2_knockback", groundSlam2Knockback,
                        "ground_slam_pillar_knockback", groundSlamPillarKnockback
                ),
                Map.of("aggressive_wild", aggressiveWild)
        );
    }
}
