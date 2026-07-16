package com.leon.saintsdragons.fabric.config;

import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "saintsdragons/server/tools_and_armor")
public final class SaintsDragonsFabricToolsArmorConfig implements ConfigData {
    public double worldrootSwordAttackDamage = ToolsArmorConfig.WORLDROOT_SWORD_DAMAGE_DEFAULT;
    public double worldrootSwordAttackSpeed = ToolsArmorConfig.WORLDROOT_SWORD_SPEED_DEFAULT;
    public double worldrootPickaxeAttackDamage = ToolsArmorConfig.WORLDROOT_PICKAXE_DAMAGE_DEFAULT;
    public double worldrootPickaxeAttackSpeed = ToolsArmorConfig.WORLDROOT_PICKAXE_SPEED_DEFAULT;
    public double worldrootAxeAttackDamage = ToolsArmorConfig.WORLDROOT_AXE_DAMAGE_DEFAULT;
    public double worldrootAxeAttackSpeed = ToolsArmorConfig.WORLDROOT_AXE_SPEED_DEFAULT;
    public double worldrootShovelAttackDamage = ToolsArmorConfig.WORLDROOT_SHOVEL_DAMAGE_DEFAULT;
    public double worldrootShovelAttackSpeed = ToolsArmorConfig.WORLDROOT_SHOVEL_SPEED_DEFAULT;
    public double worldrootHoeAttackDamage = ToolsArmorConfig.WORLDROOT_HOE_DAMAGE_DEFAULT;
    public double worldrootHoeAttackSpeed = ToolsArmorConfig.WORLDROOT_HOE_SPEED_DEFAULT;

    public double bloodTempestKatanaAttackDamage = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_DAMAGE_DEFAULT;
    public double bloodTempestKatanaAttackSpeed = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_SPEED_DEFAULT;
    public double bloodTempestKatanaEntityReach = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_REACH_DEFAULT;
    public double bloodTempestKatanaCriticalDamageBonus = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_CRITICAL_BONUS_DEFAULT;
    public double bloodTempestKatanaAbilityDamageMultiplier = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER_DEFAULT;
    public int bloodTempestKatanaAbilityCooldownTicks = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS_DEFAULT;
    public double bloodTempestKatanaAbilityMaxDistance = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE_DEFAULT;
    public boolean bloodTempestKatanaAbilityEnabled = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_ENABLED_DEFAULT;
    public double bloodTempestHelmetArmor = ToolsArmorConfig.BLOOD_TEMPEST_HELMET_ARMOR_DEFAULT;
    public double bloodTempestChestplateArmor = ToolsArmorConfig.BLOOD_TEMPEST_CHESTPLATE_ARMOR_DEFAULT;
    public double bloodTempestLeggingsArmor = ToolsArmorConfig.BLOOD_TEMPEST_LEGGINGS_ARMOR_DEFAULT;
    public double bloodTempestBootsArmor = ToolsArmorConfig.BLOOD_TEMPEST_BOOTS_ARMOR_DEFAULT;
    public double bloodTempestArmorToughness = ToolsArmorConfig.BLOOD_TEMPEST_TOUGHNESS_DEFAULT;
    public double bloodTempestKnockbackResistance = ToolsArmorConfig.BLOOD_TEMPEST_KNOCKBACK_RESISTANCE_DEFAULT;
    public boolean bloodTempestDodgeEnabled = ToolsArmorConfig.BLOOD_TEMPEST_DODGE_ENABLED_DEFAULT;
    public int bloodTempestDodgeCooldownTicks = ToolsArmorConfig.BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS_DEFAULT;
    public double bloodTempestForwardDodgeSpeed = ToolsArmorConfig.BLOOD_TEMPEST_FORWARD_DODGE_SPEED_DEFAULT;
    public double bloodTempestSideBackDodgeSpeed = ToolsArmorConfig.BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED_DEFAULT;

    public double dragonlordSwordAttackDamage = ToolsArmorConfig.DRAGONLORD_SWORD_DAMAGE_DEFAULT;
    public double dragonlordSwordAttackSpeed = ToolsArmorConfig.DRAGONLORD_SWORD_SPEED_DEFAULT;
    public double dragonlordSwordEntityReach = ToolsArmorConfig.DRAGONLORD_SWORD_REACH_DEFAULT;
    public double dragonlordSwordCriticalDamageBonus = ToolsArmorConfig.DRAGONLORD_SWORD_CRITICAL_BONUS_DEFAULT;
    public double dragonlordHelmetArmor = ToolsArmorConfig.DRAGONLORD_HELMET_ARMOR_DEFAULT;
    public double dragonlordChestplateArmor = ToolsArmorConfig.DRAGONLORD_CHESTPLATE_ARMOR_DEFAULT;
    public double dragonlordLeggingsArmor = ToolsArmorConfig.DRAGONLORD_LEGGINGS_ARMOR_DEFAULT;
    public double dragonlordBootsArmor = ToolsArmorConfig.DRAGONLORD_BOOTS_ARMOR_DEFAULT;
    public double dragonlordArmorToughness = ToolsArmorConfig.DRAGONLORD_TOUGHNESS_DEFAULT;
    public double dragonlordHelmetKnockbackResistance = ToolsArmorConfig.DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE_DEFAULT;
    public double dragonlordChestplateKnockbackResistance = ToolsArmorConfig.DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE_DEFAULT;
    public double dragonlordLeggingsKnockbackResistance = ToolsArmorConfig.DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE_DEFAULT;
    public double dragonlordBootsKnockbackResistance = ToolsArmorConfig.DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE_DEFAULT;
    public double dragonlordHelmetMaxHealthBonusPercent = ToolsArmorConfig.DRAGONLORD_HELMET_MAX_HEALTH_BONUS_DEFAULT;
    public double dragonlordChestplateMaxHealthBonusPercent = ToolsArmorConfig.DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS_DEFAULT;
    public double dragonlordLeggingsMaxHealthBonusPercent = ToolsArmorConfig.DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS_DEFAULT;
    public double dragonlordBootsMaxHealthBonusPercent = ToolsArmorConfig.DRAGONLORD_BOOTS_MAX_HEALTH_BONUS_DEFAULT;
    public double dragonlordFireResistancePerPiece = ToolsArmorConfig.DRAGONLORD_FIRE_RESISTANCE_DEFAULT;
    public double dragonlordBlastResistancePerPiece = ToolsArmorConfig.DRAGONLORD_BLAST_RESISTANCE_DEFAULT;
    public boolean dragonlordSwordAbilityEnabled = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_ENABLED_DEFAULT;
    public double dragonlordSwordAbilityBaseDamage = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE_DEFAULT;
    public double dragonlordSwordAbilityDamagePerPillar = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR_DEFAULT;
    public double dragonlordSwordAbilityBaseKnockback = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK_DEFAULT;
    public double dragonlordSwordAbilityKnockbackPerPillar = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR_DEFAULT;
    public int dragonlordSwordAbilityCooldownTicks = ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS_DEFAULT;
    public boolean dragonlordFlightEnabled = ToolsArmorConfig.DRAGONLORD_FLIGHT_ENABLED_DEFAULT;
    public double dragonlordDoubleJumpVerticalVelocity = ToolsArmorConfig.DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY_DEFAULT;
    public double dragonlordLandingMinimumDrop = ToolsArmorConfig.DRAGONLORD_LANDING_MINIMUM_DROP_DEFAULT;
    public double dragonlordLandingShockwaveRadius = ToolsArmorConfig.DRAGONLORD_LANDING_SHOCKWAVE_RADIUS_DEFAULT;
    public double dragonlordLandingKnockUpStrength = ToolsArmorConfig.DRAGONLORD_LANDING_KNOCK_UP_STRENGTH_DEFAULT;
    public double dragonlordLandingImpactDamage = ToolsArmorConfig.DRAGONLORD_LANDING_IMPACT_DAMAGE_DEFAULT;
    public boolean dragonlordLavaFissureEnabled = ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_ENABLED_DEFAULT;
    public double dragonlordLavaFissureDamage = ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DAMAGE_DEFAULT;
    public double dragonlordLavaFissureRadius = ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_RADIUS_DEFAULT;
    public int dragonlordLavaFissureDurationTicks = ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DURATION_TICKS_DEFAULT;
}
