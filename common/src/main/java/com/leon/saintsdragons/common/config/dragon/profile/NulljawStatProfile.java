package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.Map;

public final class NulljawStatProfile {
    private NulljawStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 70.0D;
        public static final double ARMOR = 4.0D;
        public static final double BITE_DAMAGE = 8.0D;
        public static final double INVISIBILITY_DURATION_TICKS = 6000.0D;

        private Config() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 8.0F;
        public static final double RANGE = 2.75D;

        private BiteAbility() {
        }
    }

    public static final class BreedBehaviour {
        public static final double BREED_FLIGHT_SPEED = 0.22D;

        private BreedBehaviour() {
        }
    }

    public static final class Entity {
        public static final double NATURAL_SPAWN_NULLJAW_RADIUS = 96.0D;
        public static final double BREED_PARTNER_RANGE = 24.0D;
        public static final double BREED_DISTANCE_SQR = 9.0D;
        public static final int TAME_CHANCE_DENOMINATOR = 5;
        public static final int DEATH_SOUND_DURATION_TICKS = 44;
        public static final double RIDER_FLIGHT_SPEED = 0.32D;
        public static final double RIDER_ASCEND_SPEED = 0.16D;
        public static final double RIDER_DESCEND_SPEED = 0.18D;
        public static final double BABY_MAX_HEALTH = 70.0D;
        public static final double BABY_ARMOR = 4.0D;
        public static final int DEFAULT_CLOAK_DURATION_TICKS = 20 * 60 * 5;
        public static final double PACK_SEARCH_RADIUS = 28.0D;
        public static final double ATTRIBUTE_MOVEMENT_SPEED = 0.20D;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 48.0D;

        private Entity() {
        }
    }

    public static final class FloatWanderBehaviour {
        public static final int HORIZONTAL_RANGE = 16;
        public static final int VERTICAL_RANGE = 8;
        public static final double SPEED = 1.0D;

        private FloatWanderBehaviour() {
        }
    }

    public static final class FollowOwnerBehaviour {
        public static final double FLIGHT_SPEED = 1.0D;

        private FollowOwnerBehaviour() {
        }
    }

    public static final class FollowParentBehaviour {
        public static final double FOLLOW_SPEED = 0.9D;

        private FollowParentBehaviour() {
        }
    }

    public static final class ForwardTeleportAbility {
        public static final int COOLDOWN_TICKS = 40;

        private ForwardTeleportAbility() {
        }
    }

    public static final class PackCombatCoordinator {
        public static final double ORBIT_RADIUS = 8.5D;
        public static final double STAGE_RADIUS = 10.0D;
        public static final double ORBIT_ANGULAR_SPEED = 0.035D;

        private PackCombatCoordinator() {
        }
    }

    public static final class ShulkerBulletSensorBehaviour {
        public static final double SEARCH_RADIUS = 32.0D;
        public static final double MAX_CHASE_DISTANCE_FROM_OWNER_SQR = 40.0D * 40.0D;
        public static final double LAST_CHANCE_INTERCEPT_DISTANCE_SQR = 8.0D * 8.0D;

        private ShulkerBulletSensorBehaviour() {
        }
    }

    public static final class TacticalCombatBehaviour {
        public static final double EXTRA_BITE_REACH = 1.5D;
        public static final double PROJECTILE_EAT_REACH = 1.25D;
        public static final double PROJECTILE_CHASE_SPEED = 1.35D;
        public static final double PROJECTILE_VELOCITY_BLEND = 0.65D;

        private TacticalCombatBehaviour() {
        }
    }

    public static final class TemptBehaviour {
        public static final double START_RANGE = 32.0D;
        public static final double CONTINUE_RANGE = 34.0D;
        public static final double SPEED = 1.0D;

        private TemptBehaviour() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double biteDamage = Config.BITE_DAMAGE;
        double invisibilityDurationTicks = Config.INVISIBILITY_DURATION_TICKS;
        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = StatProfileConfigSupport.forgeDouble(configClass, "NULLJAW_MAX_HEALTH");
                armor = StatProfileConfigSupport.forgeDouble(configClass, "NULLJAW_ARMOR");
                biteDamage = StatProfileConfigSupport.forgeDouble(configClass, "NULLJAW_BITE_DAMAGE");
                invisibilityDurationTicks = StatProfileConfigSupport.forgeDouble(configClass, "NULLJAW_INVISIBILITY_DURATION_TICKS");
            } catch (Exception ignored) {
            }
        }
        return new DragonAttributeConfig(
                maxHealth,
                armor,
                0.42D,
                Map.of("bite", DragonAbilityOverride.ofDamage(biteDamage)),
                Map.of(
                        "taming_chance_base", 20.0D,
                        "wild_flying_speed_multiplier", 1.0D,
                        "invisibility_duration_ticks", invisibilityDurationTicks
                ),
                Map.of(
                        "legacy_taming", true,
                        "aggressive_wild", false
                )
        );
    }
}
