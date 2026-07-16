package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeToolsArmorScreen extends ForgePagedConfigScreen {
    public ForgeToolsArmorScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.tools_armor"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        ToolsArmorConfig.bootstrap();

        section(entries, "worldroot");
        field(entries, "worldroot.sword_damage", ToolsArmorConfig.WORLDROOT_SWORD_DAMAGE, ToolsArmorConfig.WORLDROOT_SWORD_DAMAGE_DEFAULT);
        field(entries, "worldroot.sword_speed", ToolsArmorConfig.WORLDROOT_SWORD_SPEED, ToolsArmorConfig.WORLDROOT_SWORD_SPEED_DEFAULT);
        field(entries, "worldroot.pickaxe_damage", ToolsArmorConfig.WORLDROOT_PICKAXE_DAMAGE, ToolsArmorConfig.WORLDROOT_PICKAXE_DAMAGE_DEFAULT);
        field(entries, "worldroot.pickaxe_speed", ToolsArmorConfig.WORLDROOT_PICKAXE_SPEED, ToolsArmorConfig.WORLDROOT_PICKAXE_SPEED_DEFAULT);
        field(entries, "worldroot.axe_damage", ToolsArmorConfig.WORLDROOT_AXE_DAMAGE, ToolsArmorConfig.WORLDROOT_AXE_DAMAGE_DEFAULT);
        field(entries, "worldroot.axe_speed", ToolsArmorConfig.WORLDROOT_AXE_SPEED, ToolsArmorConfig.WORLDROOT_AXE_SPEED_DEFAULT);
        field(entries, "worldroot.shovel_damage", ToolsArmorConfig.WORLDROOT_SHOVEL_DAMAGE, ToolsArmorConfig.WORLDROOT_SHOVEL_DAMAGE_DEFAULT);
        field(entries, "worldroot.shovel_speed", ToolsArmorConfig.WORLDROOT_SHOVEL_SPEED, ToolsArmorConfig.WORLDROOT_SHOVEL_SPEED_DEFAULT);
        field(entries, "worldroot.hoe_damage", ToolsArmorConfig.WORLDROOT_HOE_DAMAGE, ToolsArmorConfig.WORLDROOT_HOE_DAMAGE_DEFAULT);
        field(entries, "worldroot.hoe_speed", ToolsArmorConfig.WORLDROOT_HOE_SPEED, ToolsArmorConfig.WORLDROOT_HOE_SPEED_DEFAULT);

        section(entries, "blood_tempest");
        field(entries, "blood_tempest.katana_damage", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_DAMAGE, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_DAMAGE_DEFAULT);
        field(entries, "blood_tempest.katana_speed", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_SPEED, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_SPEED_DEFAULT);
        field(entries, "blood_tempest.katana_reach", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_REACH, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_REACH_DEFAULT);
        field(entries, "blood_tempest.katana_critical_bonus", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_CRITICAL_BONUS, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_CRITICAL_BONUS_DEFAULT);
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.blood_tempest.katana_ability_enabled"),
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_ENABLED::get,
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_ENABLED::set,
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_ENABLED::save
        ));
        field(entries, "blood_tempest.katana_ability_damage_multiplier", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER_DEFAULT);
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.blood_tempest.katana_ability_cooldown"),
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS::get,
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS::set,
                ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS::save,
                () -> ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS_DEFAULT
        ));
        field(entries, "blood_tempest.katana_ability_distance", ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE_DEFAULT);
        field(entries, "blood_tempest.helmet_armor", ToolsArmorConfig.BLOOD_TEMPEST_HELMET_ARMOR, ToolsArmorConfig.BLOOD_TEMPEST_HELMET_ARMOR_DEFAULT);
        field(entries, "blood_tempest.chestplate_armor", ToolsArmorConfig.BLOOD_TEMPEST_CHESTPLATE_ARMOR, ToolsArmorConfig.BLOOD_TEMPEST_CHESTPLATE_ARMOR_DEFAULT);
        field(entries, "blood_tempest.leggings_armor", ToolsArmorConfig.BLOOD_TEMPEST_LEGGINGS_ARMOR, ToolsArmorConfig.BLOOD_TEMPEST_LEGGINGS_ARMOR_DEFAULT);
        field(entries, "blood_tempest.boots_armor", ToolsArmorConfig.BLOOD_TEMPEST_BOOTS_ARMOR, ToolsArmorConfig.BLOOD_TEMPEST_BOOTS_ARMOR_DEFAULT);
        field(entries, "blood_tempest.toughness", ToolsArmorConfig.BLOOD_TEMPEST_TOUGHNESS, ToolsArmorConfig.BLOOD_TEMPEST_TOUGHNESS_DEFAULT);
        field(entries, "blood_tempest.knockback_resistance", ToolsArmorConfig.BLOOD_TEMPEST_KNOCKBACK_RESISTANCE, ToolsArmorConfig.BLOOD_TEMPEST_KNOCKBACK_RESISTANCE_DEFAULT);
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.blood_tempest.dodge_enabled"),
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_ENABLED::get,
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_ENABLED::set,
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_ENABLED::save
        ));
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.blood_tempest.dodge_cooldown"),
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS::get,
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS::set,
                ToolsArmorConfig.BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS::save,
                () -> ToolsArmorConfig.BLOOD_TEMPEST_DODGE_COOLDOWN_TICKS_DEFAULT
        ));
        field(entries, "blood_tempest.forward_dodge_speed", ToolsArmorConfig.BLOOD_TEMPEST_FORWARD_DODGE_SPEED, ToolsArmorConfig.BLOOD_TEMPEST_FORWARD_DODGE_SPEED_DEFAULT);
        field(entries, "blood_tempest.side_back_dodge_speed", ToolsArmorConfig.BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED, ToolsArmorConfig.BLOOD_TEMPEST_SIDE_BACK_DODGE_SPEED_DEFAULT);

        section(entries, "dragonlord");
        field(entries, "dragonlord.sword_damage", ToolsArmorConfig.DRAGONLORD_SWORD_DAMAGE, ToolsArmorConfig.DRAGONLORD_SWORD_DAMAGE_DEFAULT);
        field(entries, "dragonlord.sword_speed", ToolsArmorConfig.DRAGONLORD_SWORD_SPEED, ToolsArmorConfig.DRAGONLORD_SWORD_SPEED_DEFAULT);
        field(entries, "dragonlord.sword_reach", ToolsArmorConfig.DRAGONLORD_SWORD_REACH, ToolsArmorConfig.DRAGONLORD_SWORD_REACH_DEFAULT);
        field(entries, "dragonlord.sword_critical_bonus", ToolsArmorConfig.DRAGONLORD_SWORD_CRITICAL_BONUS, ToolsArmorConfig.DRAGONLORD_SWORD_CRITICAL_BONUS_DEFAULT);
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.dragonlord.sword_ability_enabled"),
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_ENABLED::get,
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_ENABLED::set,
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_ENABLED::save
        ));
        field(entries, "dragonlord.sword_ability_base_damage", ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE, ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE_DEFAULT);
        field(entries, "dragonlord.sword_ability_damage_per_pillar", ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR, ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR_DEFAULT);
        field(entries, "dragonlord.sword_ability_base_knockback", ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK, ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK_DEFAULT);
        field(entries, "dragonlord.sword_ability_knockback_per_pillar", ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR, ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR_DEFAULT);
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.dragonlord.sword_ability_cooldown"),
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS::get,
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS::set,
                ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS::save,
                () -> ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS_DEFAULT
        ));
        field(entries, "dragonlord.helmet_armor", ToolsArmorConfig.DRAGONLORD_HELMET_ARMOR, ToolsArmorConfig.DRAGONLORD_HELMET_ARMOR_DEFAULT);
        field(entries, "dragonlord.chestplate_armor", ToolsArmorConfig.DRAGONLORD_CHESTPLATE_ARMOR, ToolsArmorConfig.DRAGONLORD_CHESTPLATE_ARMOR_DEFAULT);
        field(entries, "dragonlord.leggings_armor", ToolsArmorConfig.DRAGONLORD_LEGGINGS_ARMOR, ToolsArmorConfig.DRAGONLORD_LEGGINGS_ARMOR_DEFAULT);
        field(entries, "dragonlord.boots_armor", ToolsArmorConfig.DRAGONLORD_BOOTS_ARMOR, ToolsArmorConfig.DRAGONLORD_BOOTS_ARMOR_DEFAULT);
        field(entries, "dragonlord.toughness", ToolsArmorConfig.DRAGONLORD_TOUGHNESS, ToolsArmorConfig.DRAGONLORD_TOUGHNESS_DEFAULT);
        field(entries, "dragonlord.helmet_knockback", ToolsArmorConfig.DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE, ToolsArmorConfig.DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE_DEFAULT);
        field(entries, "dragonlord.chestplate_knockback", ToolsArmorConfig.DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE, ToolsArmorConfig.DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE_DEFAULT);
        field(entries, "dragonlord.leggings_knockback", ToolsArmorConfig.DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE, ToolsArmorConfig.DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE_DEFAULT);
        field(entries, "dragonlord.boots_knockback", ToolsArmorConfig.DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE, ToolsArmorConfig.DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE_DEFAULT);
        field(entries, "dragonlord.helmet_health", ToolsArmorConfig.DRAGONLORD_HELMET_MAX_HEALTH_BONUS, ToolsArmorConfig.DRAGONLORD_HELMET_MAX_HEALTH_BONUS_DEFAULT);
        field(entries, "dragonlord.chestplate_health", ToolsArmorConfig.DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS, ToolsArmorConfig.DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS_DEFAULT);
        field(entries, "dragonlord.leggings_health", ToolsArmorConfig.DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS, ToolsArmorConfig.DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS_DEFAULT);
        field(entries, "dragonlord.boots_health", ToolsArmorConfig.DRAGONLORD_BOOTS_MAX_HEALTH_BONUS, ToolsArmorConfig.DRAGONLORD_BOOTS_MAX_HEALTH_BONUS_DEFAULT);
        field(entries, "dragonlord.fire_resistance", ToolsArmorConfig.DRAGONLORD_FIRE_RESISTANCE, ToolsArmorConfig.DRAGONLORD_FIRE_RESISTANCE_DEFAULT);
        field(entries, "dragonlord.blast_resistance", ToolsArmorConfig.DRAGONLORD_BLAST_RESISTANCE, ToolsArmorConfig.DRAGONLORD_BLAST_RESISTANCE_DEFAULT);
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.dragonlord.flight_enabled"),
                ToolsArmorConfig.DRAGONLORD_FLIGHT_ENABLED::get,
                ToolsArmorConfig.DRAGONLORD_FLIGHT_ENABLED::set,
                ToolsArmorConfig.DRAGONLORD_FLIGHT_ENABLED::save
        ));
        field(entries, "dragonlord.double_jump_velocity", ToolsArmorConfig.DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY, ToolsArmorConfig.DRAGONLORD_DOUBLE_JUMP_VERTICAL_VELOCITY_DEFAULT);
        field(entries, "dragonlord.landing_minimum_drop", ToolsArmorConfig.DRAGONLORD_LANDING_MINIMUM_DROP, ToolsArmorConfig.DRAGONLORD_LANDING_MINIMUM_DROP_DEFAULT);
        field(entries, "dragonlord.landing_shockwave_radius", ToolsArmorConfig.DRAGONLORD_LANDING_SHOCKWAVE_RADIUS, ToolsArmorConfig.DRAGONLORD_LANDING_SHOCKWAVE_RADIUS_DEFAULT);
        field(entries, "dragonlord.landing_knock_up_strength", ToolsArmorConfig.DRAGONLORD_LANDING_KNOCK_UP_STRENGTH, ToolsArmorConfig.DRAGONLORD_LANDING_KNOCK_UP_STRENGTH_DEFAULT);
        field(entries, "dragonlord.landing_impact_damage", ToolsArmorConfig.DRAGONLORD_LANDING_IMPACT_DAMAGE, ToolsArmorConfig.DRAGONLORD_LANDING_IMPACT_DAMAGE_DEFAULT);
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.dragonlord.lava_fissure_enabled"),
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_ENABLED::get,
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_ENABLED::set,
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_ENABLED::save
        ));
        field(entries, "dragonlord.lava_fissure_damage", ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DAMAGE, ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DAMAGE_DEFAULT);
        field(entries, "dragonlord.lava_fissure_radius", ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_RADIUS, ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_RADIUS_DEFAULT);
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor.dragonlord.lava_fissure_duration"),
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DURATION_TICKS::get,
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DURATION_TICKS::set,
                ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DURATION_TICKS::save,
                () -> ToolsArmorConfig.DRAGONLORD_LAVA_FISSURE_DURATION_TICKS_DEFAULT
        ));
    }

    private static void section(List<ConfigEntry> entries, String key) {
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.tools_armor." + key)));
    }

    private static void field(List<ConfigEntry> entries, String key, ConfigHelper.DoubleValue value,
                              double defaultValue) {
        entries.add(new DoubleEntry(
                Component.translatable("saintsdragons.config_screen.tools_armor." + key),
                value::get,
                value::set,
                value::save,
                () -> defaultValue
        ));
    }

    @Override
    protected void onSave() {
    }
}
