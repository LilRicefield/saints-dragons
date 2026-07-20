package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

public final class ToolsArmorConfig {
    public static final String CONFIG_FILE = SaintsDragonsConfig.SERVER_CONFIG_FOLDER + "/tools_and_armor.toml";

    public static final double WORLDROOT_SWORD_DAMAGE_DEFAULT = 7.8D;
    public static final double WORLDROOT_SWORD_SPEED_DEFAULT = 1.6D;
    public static final double WORLDROOT_PICKAXE_DAMAGE_DEFAULT = 5.8D;
    public static final double WORLDROOT_PICKAXE_SPEED_DEFAULT = 1.2D;
    public static final double WORLDROOT_AXE_DAMAGE_DEFAULT = 9.8D;
    public static final double WORLDROOT_AXE_SPEED_DEFAULT = 1.0D;
    public static final double WORLDROOT_SHOVEL_DAMAGE_DEFAULT = 6.3D;
    public static final double WORLDROOT_SHOVEL_SPEED_DEFAULT = 1.0D;
    public static final double WORLDROOT_HOE_DAMAGE_DEFAULT = 0.8D;
    public static final double WORLDROOT_HOE_SPEED_DEFAULT = 4.0D;
    public static final double WORLDROOT_DRAGON_DAMAGE_MULTIPLIER_DEFAULT = 1.5D;

    public static final double BLOOD_TEMPEST_KATANA_DAMAGE_DEFAULT = 9.0D;
    public static final double BLOOD_TEMPEST_KATANA_SPEED_DEFAULT = 3.0D;
    public static final double BLOOD_TEMPEST_KATANA_REACH_DEFAULT = 5.0D;
    public static final double BLOOD_TEMPEST_KATANA_CRITICAL_BONUS_DEFAULT = 0.8D;
    public static final double BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER_DEFAULT = 3.0D;
    public static final double BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER_DEFAULT = 2.65D;
    public static final int BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS_DEFAULT = 60;
    public static final double BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE_DEFAULT = 20.0D;
    public static final boolean BLOOD_TEMPEST_KATANA_ABILITY_ENABLED_DEFAULT = true;
    public static final double BLOOD_TEMPEST_HELMET_ARMOR_DEFAULT = 4.0D;
    public static final double BLOOD_TEMPEST_CHESTPLATE_ARMOR_DEFAULT = 9.0D;
    public static final double BLOOD_TEMPEST_LEGGINGS_ARMOR_DEFAULT = 7.0D;
    public static final double BLOOD_TEMPEST_BOOTS_ARMOR_DEFAULT = 4.0D;
    public static final double BLOOD_TEMPEST_TOUGHNESS_DEFAULT = 4.0D;
    public static final double BLOOD_TEMPEST_KNOCKBACK_RESISTANCE_DEFAULT = 0.15D;
    public static final boolean BLOOD_TEMPEST_DODGE_ENABLED_DEFAULT = true;
    public static final int BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS_DEFAULT = 30;
    public static final double BLOOD_TEMPEST_FORWARD_DODGE_SPEED_DEFAULT = 4.0D;
    public static final double BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED_DEFAULT = 4.0D;

    public static final double DRAGONLORD_SWORD_DAMAGE_DEFAULT = 13.0D;
    public static final double DRAGONLORD_SWORD_SPEED_DEFAULT = 1.4D;
    public static final double DRAGONLORD_SWORD_REACH_DEFAULT = 7.0D;
    public static final double DRAGONLORD_SWORD_CRITICAL_BONUS_DEFAULT = 0.0D;
    public static final double DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER_DEFAULT = 5.0D;
    public static final double DRAGONLORD_HELMET_ARMOR_DEFAULT = 5.0D;
    public static final double DRAGONLORD_CHESTPLATE_ARMOR_DEFAULT = 10.0D;
    public static final double DRAGONLORD_LEGGINGS_ARMOR_DEFAULT = 8.0D;
    public static final double DRAGONLORD_BOOTS_ARMOR_DEFAULT = 5.0D;
    public static final double DRAGONLORD_TOUGHNESS_DEFAULT = 5.0D;
    public static final double DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE_DEFAULT = 0.15D;
    public static final double DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE_DEFAULT = 0.5D;
    public static final double DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE_DEFAULT = 0.25D;
    public static final double DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE_DEFAULT = 0.1D;
    public static final double DRAGONLORD_HELMET_MAX_HEALTH_BONUS_DEFAULT = 10.0D;
    public static final double DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS_DEFAULT = 15.0D;
    public static final double DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS_DEFAULT = 15.0D;
    public static final double DRAGONLORD_BOOTS_MAX_HEALTH_BONUS_DEFAULT = 10.0D;
    public static final double DRAGONLORD_FIRE_RESISTANCE_DEFAULT = 25.0D;
    public static final double DRAGONLORD_BLAST_RESISTANCE_DEFAULT = 25.0D;
    public static final boolean DRAGONLORD_SWORD_ABILITY_ENABLED_DEFAULT = true;
    public static final double DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE_DEFAULT = 18.0D;
    public static final double DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR_DEFAULT = 4.0D;
    public static final double DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK_DEFAULT = 0.9D;
    public static final double DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR_DEFAULT = 0.2D;
    public static final int DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS_DEFAULT = 50;
    public static final boolean DRAGONLORD_FLIGHT_ENABLED_DEFAULT = true;
    public static final double DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY_DEFAULT = 0.85D;
    public static final double DRAGONLORD_LANDING_MINIMUM_DROP_DEFAULT = 4.0D;
    public static final double DRAGONLORD_LANDING_SHOCKWAVE_RADIUS_DEFAULT = 7.5D;
    public static final double DRAGONLORD_LANDING_KNOCK_UP_STRENGTH_DEFAULT = 0.75D;
    public static final double DRAGONLORD_LANDING_IMPACT_DAMAGE_DEFAULT = 20.0D;
    public static final boolean DRAGONLORD_LAVA_FISSURE_ENABLED_DEFAULT = true;
    public static final double DRAGONLORD_LAVA_FISSURE_DAMAGE_DEFAULT = 10.0D;
    public static final double DRAGONLORD_LAVA_FISSURE_RADIUS_DEFAULT = 9.0D;
    public static final int DRAGONLORD_LAVA_FISSURE_DURATION_TICKS_DEFAULT = 140;

    public static ConfigHelper.DoubleValue WORLDROOT_SWORD_DAMAGE;
    public static ConfigHelper.DoubleValue WORLDROOT_SWORD_SPEED;
    public static ConfigHelper.DoubleValue WORLDROOT_PICKAXE_DAMAGE;
    public static ConfigHelper.DoubleValue WORLDROOT_PICKAXE_SPEED;
    public static ConfigHelper.DoubleValue WORLDROOT_AXE_DAMAGE;
    public static ConfigHelper.DoubleValue WORLDROOT_AXE_SPEED;
    public static ConfigHelper.DoubleValue WORLDROOT_SHOVEL_DAMAGE;
    public static ConfigHelper.DoubleValue WORLDROOT_SHOVEL_SPEED;
    public static ConfigHelper.DoubleValue WORLDROOT_HOE_DAMAGE;
    public static ConfigHelper.DoubleValue WORLDROOT_HOE_SPEED;
    public static ConfigHelper.DoubleValue WORLDROOT_DRAGON_DAMAGE_MULTIPLIER;

    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_DAMAGE;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_SPEED;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_REACH;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_CRITICAL_BONUS;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER;
    public static ConfigHelper.IntValue BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE;
    public static ConfigHelper.BooleanValue BLOOD_TEMPEST_KATANA_ABILITY_ENABLED;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_HELMET_ARMOR;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_CHESTPLATE_ARMOR;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_LEGGINGS_ARMOR;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_BOOTS_ARMOR;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_TOUGHNESS;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_KNOCKBACK_RESISTANCE;
    public static ConfigHelper.BooleanValue BLOOD_TEMPEST_DODGE_ENABLED;
    public static ConfigHelper.IntValue BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_FORWARD_DODGE_SPEED;
    public static ConfigHelper.DoubleValue BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED;

    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_DAMAGE;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_SPEED;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_REACH;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_CRITICAL_BONUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER;
    public static ConfigHelper.DoubleValue DRAGONLORD_HELMET_ARMOR;
    public static ConfigHelper.DoubleValue DRAGONLORD_CHESTPLATE_ARMOR;
    public static ConfigHelper.DoubleValue DRAGONLORD_LEGGINGS_ARMOR;
    public static ConfigHelper.DoubleValue DRAGONLORD_BOOTS_ARMOR;
    public static ConfigHelper.DoubleValue DRAGONLORD_TOUGHNESS;
    public static ConfigHelper.DoubleValue DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE;
    public static ConfigHelper.DoubleValue DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE;
    public static ConfigHelper.DoubleValue DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE;
    public static ConfigHelper.DoubleValue DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE;
    public static ConfigHelper.DoubleValue DRAGONLORD_HELMET_MAX_HEALTH_BONUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_BOOTS_MAX_HEALTH_BONUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_FIRE_RESISTANCE;
    public static ConfigHelper.DoubleValue DRAGONLORD_BLAST_RESISTANCE;
    public static ConfigHelper.BooleanValue DRAGONLORD_SWORD_ABILITY_ENABLED;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK;
    public static ConfigHelper.DoubleValue DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR;
    public static ConfigHelper.IntValue DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS;
    public static ConfigHelper.BooleanValue DRAGONLORD_FLIGHT_ENABLED;
    public static ConfigHelper.DoubleValue DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY;
    public static ConfigHelper.DoubleValue DRAGONLORD_LANDING_MINIMUM_DROP;
    public static ConfigHelper.DoubleValue DRAGONLORD_LANDING_SHOCKWAVE_RADIUS;
    public static ConfigHelper.DoubleValue DRAGONLORD_LANDING_KNOCK_UP_STRENGTH;
    public static ConfigHelper.DoubleValue DRAGONLORD_LANDING_IMPACT_DAMAGE;
    public static ConfigHelper.BooleanValue DRAGONLORD_LAVA_FISSURE_ENABLED;
    public static ConfigHelper.DoubleValue DRAGONLORD_LAVA_FISSURE_DAMAGE;
    public static ConfigHelper.DoubleValue DRAGONLORD_LAVA_FISSURE_RADIUS;
    public static ConfigHelper.IntValue DRAGONLORD_LAVA_FISSURE_DURATION_TICKS;

    private static volatile boolean initialized;

    public static void bootstrap() {
        if (initialized) {
            return;
        }
        synchronized (ToolsArmorConfig.class) {
            if (!initialized) {
                initializeConfig();
                initialized = true;
            }
        }
    }

    private static void initializeConfig() {
        ConfigHelper.ConfigBuilder builder = Services.PLATFORM.getConfigHelper().commonBuilder(CONFIG_FILE);

        builder.push("worldroot_tools");
        WORLDROOT_SWORD_DAMAGE = value(builder, "worldrootSwordAttackDamage", WORLDROOT_SWORD_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        WORLDROOT_SWORD_SPEED = value(builder, "worldrootSwordAttackSpeed", WORLDROOT_SWORD_SPEED_DEFAULT, 0.0D, 100.0D);
        WORLDROOT_PICKAXE_DAMAGE = value(builder, "worldrootPickaxeAttackDamage", WORLDROOT_PICKAXE_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        WORLDROOT_PICKAXE_SPEED = value(builder, "worldrootPickaxeAttackSpeed", WORLDROOT_PICKAXE_SPEED_DEFAULT, 0.0D, 100.0D);
        WORLDROOT_AXE_DAMAGE = value(builder, "worldrootAxeAttackDamage", WORLDROOT_AXE_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        WORLDROOT_AXE_SPEED = value(builder, "worldrootAxeAttackSpeed", WORLDROOT_AXE_SPEED_DEFAULT, 0.0D, 100.0D);
        WORLDROOT_SHOVEL_DAMAGE = value(builder, "worldrootShovelAttackDamage", WORLDROOT_SHOVEL_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        WORLDROOT_SHOVEL_SPEED = value(builder, "worldrootShovelAttackSpeed", WORLDROOT_SHOVEL_SPEED_DEFAULT, 0.0D, 100.0D);
        WORLDROOT_HOE_DAMAGE = value(builder, "worldrootHoeAttackDamage", WORLDROOT_HOE_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        WORLDROOT_HOE_SPEED = value(builder, "worldrootHoeAttackSpeed", WORLDROOT_HOE_SPEED_DEFAULT, 0.0D, 100.0D);
        WORLDROOT_DRAGON_DAMAGE_MULTIPLIER = value(builder, "worldrootDragonDamageMultiplier", WORLDROOT_DRAGON_DAMAGE_MULTIPLIER_DEFAULT, 0.0D, 1000.0D);
        builder.pop();

        builder.push("blood_tempest");
        BLOOD_TEMPEST_KATANA_DAMAGE = value(builder, "bloodTempestKatanaAttackDamage", BLOOD_TEMPEST_KATANA_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_KATANA_SPEED = value(builder, "bloodTempestKatanaAttackSpeed", BLOOD_TEMPEST_KATANA_SPEED_DEFAULT, 0.0D, 100.0D);
        BLOOD_TEMPEST_KATANA_REACH = value(builder, "bloodTempestKatanaEntityReach", BLOOD_TEMPEST_KATANA_REACH_DEFAULT, 0.0D, 100.0D);
        BLOOD_TEMPEST_KATANA_CRITICAL_BONUS = value(builder, "bloodTempestKatanaCriticalDamageBonus", BLOOD_TEMPEST_KATANA_CRITICAL_BONUS_DEFAULT, 0.0D, 1000.0D);
        BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER = value(builder, "bloodTempestRaevyxDamageMultiplier", BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER_DEFAULT, 0.0D, 1000.0D);
        BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER = value(builder, "bloodTempestKatanaAbilityDamageMultiplier", BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER_DEFAULT, 0.0D, 1000.0D);
        BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS = builder.defineInt("bloodTempestKatanaAbilityCooldownTicks", BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS_DEFAULT, 0, 72000);
        BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE = value(builder, "bloodTempestKatanaAbilityMaxDistance", BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE_DEFAULT, 0.75D, 100.0D);
        BLOOD_TEMPEST_KATANA_ABILITY_ENABLED = builder.defineBoolean("bloodTempestKatanaAbilityEnabled", BLOOD_TEMPEST_KATANA_ABILITY_ENABLED_DEFAULT);
        BLOOD_TEMPEST_HELMET_ARMOR = value(builder, "bloodTempestHelmetArmor", BLOOD_TEMPEST_HELMET_ARMOR_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_CHESTPLATE_ARMOR = value(builder, "bloodTempestChestplateArmor", BLOOD_TEMPEST_CHESTPLATE_ARMOR_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_LEGGINGS_ARMOR = value(builder, "bloodTempestLeggingsArmor", BLOOD_TEMPEST_LEGGINGS_ARMOR_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_BOOTS_ARMOR = value(builder, "bloodTempestBootsArmor", BLOOD_TEMPEST_BOOTS_ARMOR_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_TOUGHNESS = value(builder, "bloodTempestArmorToughness", BLOOD_TEMPEST_TOUGHNESS_DEFAULT, 0.0D, 100000.0D);
        BLOOD_TEMPEST_KNOCKBACK_RESISTANCE = value(builder, "bloodTempestKnockbackResistance", BLOOD_TEMPEST_KNOCKBACK_RESISTANCE_DEFAULT, 0.0D, 1.0D);
        BLOOD_TEMPEST_DODGE_ENABLED = builder.defineBoolean("bloodTempestDodgeEnabled", BLOOD_TEMPEST_DODGE_ENABLED_DEFAULT);
        BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS = builder.defineInt("bloodTempestDodgeCooldownTicks", BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS_DEFAULT, 0, 72000);
        BLOOD_TEMPEST_FORWARD_DODGE_SPEED = value(builder, "bloodTempestForwardDodgeSpeed", BLOOD_TEMPEST_FORWARD_DODGE_SPEED_DEFAULT, 0.0D, 100.0D);
        BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED = value(builder, "bloodTempestSideBackDodgeSpeed", BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED_DEFAULT, 0.0D, 100.0D);
        builder.pop();

        builder.push("dragonlord");
        DRAGONLORD_SWORD_DAMAGE = value(builder, "dragonlordSwordAttackDamage", DRAGONLORD_SWORD_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_SWORD_SPEED = value(builder, "dragonlordSwordAttackSpeed", DRAGONLORD_SWORD_SPEED_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_SWORD_REACH = value(builder, "dragonlordSwordEntityReach", DRAGONLORD_SWORD_REACH_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_SWORD_CRITICAL_BONUS = value(builder, "dragonlordSwordCriticalDamageBonus", DRAGONLORD_SWORD_CRITICAL_BONUS_DEFAULT, 0.0D, 1000.0D);
        DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER = value(builder, "dragonlordIgnivorusDamageMultiplier", DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER_DEFAULT, 0.0D, 1000.0D);
        DRAGONLORD_HELMET_ARMOR = value(builder, "dragonlordHelmetArmor", DRAGONLORD_HELMET_ARMOR_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_CHESTPLATE_ARMOR = value(builder, "dragonlordChestplateArmor", DRAGONLORD_CHESTPLATE_ARMOR_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_LEGGINGS_ARMOR = value(builder, "dragonlordLeggingsArmor", DRAGONLORD_LEGGINGS_ARMOR_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_BOOTS_ARMOR = value(builder, "dragonlordBootsArmor", DRAGONLORD_BOOTS_ARMOR_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_TOUGHNESS = value(builder, "dragonlordArmorToughness", DRAGONLORD_TOUGHNESS_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE = value(builder, "dragonlordHelmetKnockbackResistance", DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE_DEFAULT, 0.0D, 1.0D);
        DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE = value(builder, "dragonlordChestplateKnockbackResistance", DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE_DEFAULT, 0.0D, 1.0D);
        DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE = value(builder, "dragonlordLeggingsKnockbackResistance", DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE_DEFAULT, 0.0D, 1.0D);
        DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE = value(builder, "dragonlordBootsKnockbackResistance", DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE_DEFAULT, 0.0D, 1.0D);
        DRAGONLORD_HELMET_MAX_HEALTH_BONUS = value(builder, "dragonlordHelmetMaxHealthBonusPercent", DRAGONLORD_HELMET_MAX_HEALTH_BONUS_DEFAULT, 0.0D, 10000.0D);
        DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS = value(builder, "dragonlordChestplateMaxHealthBonusPercent", DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS_DEFAULT, 0.0D, 10000.0D);
        DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS = value(builder, "dragonlordLeggingsMaxHealthBonusPercent", DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS_DEFAULT, 0.0D, 10000.0D);
        DRAGONLORD_BOOTS_MAX_HEALTH_BONUS = value(builder, "dragonlordBootsMaxHealthBonusPercent", DRAGONLORD_BOOTS_MAX_HEALTH_BONUS_DEFAULT, 0.0D, 10000.0D);
        DRAGONLORD_FIRE_RESISTANCE = value(builder, "dragonlordFireResistancePerPiece", DRAGONLORD_FIRE_RESISTANCE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_BLAST_RESISTANCE = value(builder, "dragonlordBlastResistancePerPiece", DRAGONLORD_BLAST_RESISTANCE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_SWORD_ABILITY_ENABLED = builder.defineBoolean("dragonlordSwordAbilityEnabled", DRAGONLORD_SWORD_ABILITY_ENABLED_DEFAULT);
        DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE = value(builder, "dragonlordSwordAbilityBaseDamage", DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR = value(builder, "dragonlordSwordAbilityDamagePerPillar", DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK = value(builder, "dragonlordSwordAbilityBaseKnockback", DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR = value(builder, "dragonlordSwordAbilityKnockbackPerPillar", DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS = builder.defineInt("dragonlordSwordAbilityCooldownTicks", DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS_DEFAULT, 0, 72000);
        DRAGONLORD_FLIGHT_ENABLED = builder.defineBoolean("dragonlordFlightEnabled", DRAGONLORD_FLIGHT_ENABLED_DEFAULT);
        DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY = value(builder, "dragonlordDoubleJumpVerticalVelocity", DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY_DEFAULT, 0.0D, 10.0D);
        DRAGONLORD_LANDING_MINIMUM_DROP = value(builder, "dragonlordLandingMinimumDrop", DRAGONLORD_LANDING_MINIMUM_DROP_DEFAULT, 0.0D, 1000.0D);
        DRAGONLORD_LANDING_SHOCKWAVE_RADIUS = value(builder, "dragonlordLandingShockwaveRadius", DRAGONLORD_LANDING_SHOCKWAVE_RADIUS_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_LANDING_KNOCK_UP_STRENGTH = value(builder, "dragonlordLandingKnockUpStrength", DRAGONLORD_LANDING_KNOCK_UP_STRENGTH_DEFAULT, 0.0D, 100.0D);
        DRAGONLORD_LANDING_IMPACT_DAMAGE = value(builder, "dragonlordLandingImpactDamage", DRAGONLORD_LANDING_IMPACT_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_LAVA_FISSURE_ENABLED = builder.defineBoolean("dragonlordLavaFissureEnabled", DRAGONLORD_LAVA_FISSURE_ENABLED_DEFAULT);
        DRAGONLORD_LAVA_FISSURE_DAMAGE = value(builder, "dragonlordLavaFissureDamage", DRAGONLORD_LAVA_FISSURE_DAMAGE_DEFAULT, 0.0D, 100000.0D);
        DRAGONLORD_LAVA_FISSURE_RADIUS = value(builder, "dragonlordLavaFissureRadius", DRAGONLORD_LAVA_FISSURE_RADIUS_DEFAULT, 0.5D, 100.0D);
        DRAGONLORD_LAVA_FISSURE_DURATION_TICKS = builder.defineInt("dragonlordLavaFissureDurationTicks", DRAGONLORD_LAVA_FISSURE_DURATION_TICKS_DEFAULT, 1, 72000);
        builder.pop();

        builder.build();
    }

    private static ConfigHelper.DoubleValue value(ConfigHelper.ConfigBuilder builder, String key,
                                                   double defaultValue, double min, double max) {
        return builder.defineDouble(key, defaultValue, min, max);
    }

    private ToolsArmorConfig() {
    }
}
