package com.leon.saintsdragons.common.config.dragon.profile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;

import java.util.Map;

public final class VarasuchusStatProfile {
    private VarasuchusStatProfile() {
    }

    public static final class Config {
        public static final double MAX_HEALTH = 200.0D;
        public static final double ARMOR = 8.0D;
        public static final double BITE_PHASE1_DAMAGE = 15.0D;
        public static final double BITE_PHASE2_DAMAGE = 25.0D;
        public static final double TAIL_ATTACK_DAMAGE = 7.0D;
        public static final double TAILGUARD_PARRY_DAMAGE = 10.0D;
        public static final double DASH_TAIL_SWIPE_DAMAGE = 10.0D;
        public static final double DASH_CLAW_DAMAGE = 15.0D;
        public static final double CLAW_ATTACK_DAMAGE = 8.0D;
        public static final double HORN_PHASE1_DAMAGE = 8.0D;
        public static final double HORN_PHASE2_DAMAGE = 15.8D;
        public static final double SWIM_SPEED = 1.45D;
        public static final double TAMING_CHANCE = 16.6667D;
        public static final double TAMING_CHANCE_BEEF = 16.6667D;
        public static final double TAMING_CHANCE_TROPICAL = 25.0D;
        public static final boolean LEGACY_TAMING = false;
        public static final double EGG_HATCH_TIME_TICKS_NORMAL = 24000.0D;
        public static final boolean AGGRESSIVE_WILD = true;

        private Config() {
        }
    }

    public static final class Bite2Ability {
        public static final float BASE_DAMAGE = 25.0f;
        public static final float DEFAULT_ATTACK_DAMAGE = 10.0f;
        public static final double RANGE = 6.0;
        public static final double CLOSE_HIT_RANGE = 2.75;

        private Bite2Ability() {
        }
    }

    public static final class BiteAbility {
        public static final float BASE_DAMAGE = 15.0f;
        public static final float DEFAULT_ATTACK_DAMAGE = 10.0f;
        public static final double RANGE = 5.0;
        public static final double CLOSE_HIT_RANGE = 2.5;

        private BiteAbility() {
        }
    }

    public static final class Brain {
        public static final double BREED_SPEED = 1.0D;
        public static final double FOLLOW_PARENT_SPEED = 1.1D;
        public static final double GROUND_WANDER_SPEED = 0.85D;
        public static final double FIND_WATER_SPEED = 1.0D;
        public static final float WATER_ESCAPE_TURN_DEGREES = 8.0F;
        public static final double WATER_ESCAPE_SPEED = 0.30D;
        public static final float SWIM_FOLLOW_TURN_DEGREES = 8.0F;
        public static final double SWIM_FOLLOW_SPEED = 0.25D;
        public static final float SWIM_WANDER_TURN_DEGREES = 6.0F;
        public static final double SWIM_WANDER_SPEED = 0.20D;
        public static final float ROOST_RETURN_GROUND_SPEED = 1.0F;
        public static final double ROOST_RETURN_SWIM_SPEED = 0.25D;
        public static final float ROOST_RETURN_SWIM_TURN_DEGREES = 8.0F;
        public static final double WATER_CHASE_SPEED = 0.30D;
        public static final float WATER_CHASE_TURN_DEGREES = 8.0F;

        private Brain() {
        }
    }

    public static final class ClawAbility {
        public static final float BASE_DAMAGE = 12.0f;
        public static final double RANGE = 6.5;
        public static final double BLOCK_BREAK_RANGE = 6.0;

        private ClawAbility() {
        }
    }

    public static final class CombatBehaviour {
        public static final float CHASE_SPEED = 1.5F;
        public static final double BITE_RANGE = 5.0D;
        public static final double LAND_PREY_BITE_RANGE = 1.45D;
        public static final double HORN_RANGE = 5.0D;
        public static final double CLAW_RANGE = 3.5D;
        public static final float PHASE_TWO_HEALTH_THRESHOLD = 0.5F;

        private CombatBehaviour() {
        }
    }

    public static final class Entity {
        public static final double RIDDEN_SWIM_SPRINT_MULTIPLIER = 1.6D;
        public static final double ROOST_SLEEP_RADIUS = 3.0D;
        public static final double ROOST_TERRITORY_RADIUS = 48.0D;
        public static final double ROOST_TERRITORY_RETURN_RADIUS = 32.0D;
        public static final double RIDER_WALK_SPEED = 0.15D;
        public static final double RIDER_RUN_SPEED = 0.30D;
        public static final double RIDER_JUMP_STRENGTH = 1.0D;
        public static final double RIDER_JUMP_FORWARD_BOOST = 0.4D;
        public static final double BABY_MAX_HEALTH = 80.0D;
        public static final double BABY_ARMOR = 0.0D;
        public static final double GROUND_MOVEMENT_SPEED = 0.33D;
        public static final double LEAP_HORIZONTAL_DRAG = 0.92D;
        public static final float DEFAULT_DASH_TAIL_SWIPE_DAMAGE = 14.0F;
        public static final float DEFAULT_DASH_CLAW_DAMAGE = 16.0F;
        public static final int WILD_RIDE_BUCK_COOLDOWN_TICKS = 20 * 5;
        public static final int MAX_TAMING_PROGRESS = 400;
        public static final int WILD_RIDE_BUCK_DURATION_TICKS = 90;
        public static final double WILD_RIDE_WALK_SPEED = 0.9D;
        public static final double BREED_PARTNER_RANGE = 30.0D;
        public static final double BREED_DISTANCE_SQR = 16.0D;
        public static final int FLEX_COOLDOWN_TICKS = 60;
        public static final double ATTRIBUTE_FOLLOW_RANGE = 32.0D;
        public static final double ATTRIBUTE_KNOCKBACK_RESISTANCE = 1.0D;
        public static final double ATTRIBUTE_ARMOR = 8.0D;
        public static final double ATTRIBUTE_ATTACK_DAMAGE = 10.0D;
        public static final double FALLBACK_SWIM_SPEED = 1.45D;

        private Entity() {
        }
    }

    public static final class HornGoreAbility {
        public static final float DEFAULT_PHASE1_DAMAGE = 16.0f;
        public static final float DEFAULT_PHASE2_DAMAGE = 20.8f;
        public static final float DEFAULT_ATTACK_DAMAGE = 10.0f;
        public static final double GORE_RANGE = 5.0;

        private HornGoreAbility() {
        }
    }

    public static final class InteractionHandler {
        public static final double FALLBACK_TAMING_CHANCE = 16.6667D;
        public static final double FALLBACK_TAMING_CHANCE_TROPICAL = 25.0D;
        public static final double FALLBACK_TAMING_CHANCE_BEEF = 16.6667D;

        private InteractionHandler() {
        }
    }

    public static final class SlashBarrageAbility {
        public static final float HIT_DAMAGE = 15.0F;
        public static final double CLAW_RANGE = 6.5;

        private SlashBarrageAbility() {
        }
    }

    public static final class TailAttackAbility {
        public static final float DEFAULT_DAMAGE = 8.0f;
        public static final double RANGE = 9.8;
        public static final double KNOCKBACK_STRENGTH = 1.4;

        private TailAttackAbility() {
        }
    }

    public static final class TailguardAbility {
        public static final int GUARD_COOLDOWN_TICKS = 4 * 10;
        public static final int PARRY_COOLDOWN_TICKS = 15 * 20;
        public static final float DEFAULT_PARRY_DAMAGE = 10.0F;
        public static final double PARRY_RANGE_SQR = 8.0D * 8.0D;
        public static final double KNOCKBACK_STRENGTH = 1.5D;

        private TailguardAbility() {
        }
    }

    public static final class TargetingBehaviour {
        public static final double BABY_PROTECTION_RANGE = 16.0D;
        public static final double COMMITTED_RETENTION_MULTIPLIER = 2.0D;

        private TargetingBehaviour() {
        }
    }

    public static final class RiderController {
        public static final double SWIM_SPRINT_MULTIPLIER = 1.3D;
        private RiderController() {
        }
    }

    public static DragonAttributeConfig defaults(boolean forge) {
        double maxHealth = Config.MAX_HEALTH;
        double armor = Config.ARMOR;
        double bitePhase1Damage = Config.BITE_PHASE1_DAMAGE;
        double bitePhase2Damage = Config.BITE_PHASE2_DAMAGE;
        double tailAttackDamage = Config.TAIL_ATTACK_DAMAGE;
        double tailguardParryDamage = Config.TAILGUARD_PARRY_DAMAGE;
        double dashTailSwipeDamage = Config.DASH_TAIL_SWIPE_DAMAGE;
        double dashClawDamage = Config.DASH_CLAW_DAMAGE;
        double clawAttackDamage = Config.CLAW_ATTACK_DAMAGE;
        double hornPhase1Damage = Config.HORN_PHASE1_DAMAGE;
        double hornPhase2Damage = Config.HORN_PHASE2_DAMAGE;
        double swimSpeed = Config.SWIM_SPEED;
        double tamingChance = Config.TAMING_CHANCE;
        double tamingChanceBeef = Config.TAMING_CHANCE_BEEF;
        double tamingChanceTropical = Config.TAMING_CHANCE_TROPICAL;
        boolean legacyTaming = Config.LEGACY_TAMING;
        double eggHatchTimeTicksNormal = Config.EGG_HATCH_TIME_TICKS_NORMAL;
        boolean aggressiveWild = Config.AGGRESSIVE_WILD;

        if (forge) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("VARASUCHUS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("VARASUCHUS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_ARMOR").get(null));
                bitePhase1Damage = (double) configClass.getField("VARASUCHUS_BITE_PHASE1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_BITE_PHASE1_DAMAGE").get(null));
                bitePhase2Damage = (double) configClass.getField("VARASUCHUS_BITE_PHASE2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_BITE_PHASE2_DAMAGE").get(null));
                tailAttackDamage = (double) configClass.getField("VARASUCHUS_TAIL_ATTACK_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_TAIL_ATTACK_DAMAGE").get(null));
                tailguardParryDamage = (double) configClass.getField("VARASUCHUS_TAILGUARD_PARRY_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_TAILGUARD_PARRY_DAMAGE").get(null));
                dashTailSwipeDamage = (double) configClass.getField("VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE").get(null));
                dashClawDamage = (double) configClass.getField("VARASUCHUS_DASH_CLAW_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_DASH_CLAW_DAMAGE").get(null));
                clawAttackDamage = (double) configClass.getField("VARASUCHUS_CLAW_ATTACK_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_CLAW_ATTACK_DAMAGE").get(null));
                hornPhase1Damage = (double) configClass.getField("VARASUCHUS_HORN_GORE_PHASE1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_HORN_GORE_PHASE1_DAMAGE").get(null));
                hornPhase2Damage = (double) configClass.getField("VARASUCHUS_HORN_GORE_PHASE2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_HORN_GORE_PHASE2_DAMAGE").get(null));
                swimSpeed = (double) configClass.getField("VARASUCHUS_SWIM_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_SWIM_SPEED").get(null));
                tamingChance = (double) configClass.getField("VARASUCHUS_TAMING_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_TAMING_CHANCE").get(null));
                tamingChanceBeef = (double) configClass.getField("VARASUCHUS_TAMING_CHANCE_BEEF").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_TAMING_CHANCE_BEEF").get(null));
                tamingChanceTropical = (double) configClass.getField("VARASUCHUS_TAMING_CHANCE_TROPICAL").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_TAMING_CHANCE_TROPICAL").get(null));
                legacyTaming = (boolean) configClass.getField("VARASUCHUS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_LEGACY_TAMING").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("VARASUCHUS_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_EGG_HATCH_CHANCE_NORMAL").get(null));
                aggressiveWild = (boolean) configClass.getField("VARASUCHUS_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("VARASUCHUS_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                0.0D,
                Map.of(
                        "bite_phase1", DragonAbilityOverride.ofDamage(bitePhase1Damage),
                        "bite_phase2", DragonAbilityOverride.ofDamage(bitePhase2Damage),
                        "tail_attack", DragonAbilityOverride.ofDamage(tailAttackDamage),
                        "tailguard_parry", DragonAbilityOverride.ofDamage(tailguardParryDamage),
                        "dash_tail_swipe", DragonAbilityOverride.ofDamage(dashTailSwipeDamage),
                        "dash_claw", DragonAbilityOverride.ofDamage(dashClawDamage),
                        "claw_attack", DragonAbilityOverride.ofDamage(clawAttackDamage),
                        "horn_gore_phase1", DragonAbilityOverride.ofDamage(hornPhase1Damage),
                        "horn_gore_phase2", DragonAbilityOverride.ofDamage(hornPhase2Damage)
                ),
                Map.of(
                        "swim_speed", swimSpeed,
                        "taming_chance", tamingChance,
                        "taming_chance_beef", tamingChanceBeef,
                        "taming_chance_tropical", tamingChanceTropical,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal
                ),
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }
}
