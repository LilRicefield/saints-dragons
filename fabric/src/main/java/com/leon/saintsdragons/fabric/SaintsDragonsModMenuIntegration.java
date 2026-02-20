package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Custom ModMenu screen wiring spawn tuning and dragon attribute sliders through Cloth Config.
 */
@Environment(EnvType.CLIENT)
public class SaintsDragonsModMenuIntegration implements ModMenuApi {
    private static final Component TITLE = Component.translatable("config.saintsdragons.title");
    private static final Component SPAWN_CATEGORY = Component.translatable("config.saintsdragons.category.spawning");
    private static final Component ATTRIBUTES_CATEGORY = Component.translatable("config.saintsdragons.category.attributes");
    private static final Component OTHERS_CATEGORY = Component.translatable("config.saintsdragons.category.others");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createScreen;
    }

    private Screen createScreen(Screen parent) {
        ConfigHolder<SaintsDragonsFabricConfig> holder = AutoConfig.getConfigHolder(SaintsDragonsFabricConfig.class);
        SaintsDragonsFabricConfig config = holder.getConfig();

        DragonAttributeConfigLoader loader = DragonAttributeConfigLoader.getInstance();
        DragonAttributeConfig cindervaneCurrent = loader.getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        DragonAttributeConfig cindervaneDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        CindervaneAttributeBuffer cindervaneBuffer = new CindervaneAttributeBuffer();
        cindervaneBuffer.maxHealth = cindervaneCurrent.maxHealth();
        cindervaneBuffer.armor = cindervaneCurrent.armor();
        cindervaneBuffer.flyingSpeed = cindervaneCurrent.flyingSpeed();
        cindervaneBuffer.biteDamage = cindervaneCurrent.abilityDamage("bite",
                cindervaneDefaults.abilityDamage("bite", 12.0D));
        cindervaneBuffer.slashGrabHit1Damage = cindervaneCurrent.abilityDamage("slash_grab_hit1",
                cindervaneDefaults.abilityDamage("slash_grab_hit1", 5.0D));
        cindervaneBuffer.slashGrabHit2Damage = cindervaneCurrent.abilityDamage("slash_grab_hit2",
                cindervaneDefaults.abilityDamage("slash_grab_hit2", 7.0D));
        cindervaneBuffer.volleyDamage = cindervaneCurrent.abilityDamage("magma_volley",
                cindervaneDefaults.abilityDamage("magma_volley", 20.0D));
        cindervaneBuffer.fireBodyDamage = cindervaneCurrent.abilityDamage("fire_body",
                cindervaneDefaults.abilityDamage("fire_body", 3.0D));
        cindervaneBuffer.tamingChanceBase = cindervaneCurrent.extraDouble("taming_chance_base", 4.0);
        cindervaneBuffer.tamingChanceHearty = cindervaneCurrent.extraDouble("taming_chance_hearty", 2.0);
        cindervaneBuffer.eggHatchChanceNormal = cindervaneCurrent.extraDouble("egg_hatch_chance_normal", 2.0);
        cindervaneBuffer.eggDropChance = cindervaneCurrent.extraDouble("egg_drop_chance", 0.12D);
        cindervaneBuffer.fireBodyExplosionDamage = cindervaneCurrent.extraDouble("fire_body_explosion_damage", 200.0D);
        cindervaneBuffer.fireBodySelfDamageOnCrash = cindervaneCurrent.extraDouble("fire_body_self_damage_on_crash", 40.0D);
        cindervaneBuffer.wildFlyingSpeedMultiplier = cindervaneCurrent.extraDouble("wild_flying_speed_multiplier",
                cindervaneDefaults.extraDouble("wild_flying_speed_multiplier", 1.0D));
        cindervaneBuffer.aggressiveWild = cindervaneCurrent.extraBoolean("aggressive_wild", false);
        cindervaneBuffer.reactiveTerrainClearingOnDamage = cindervaneCurrent.extraBoolean("reactive_terrain_clearing_on_damage", true);
        cindervaneBuffer.reactiveTerrainClearingOnDamageTamed = cindervaneCurrent.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false);

        DragonAttributeConfig raevyxCurrent = loader.getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        DragonAttributeConfig raevyxDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        RaevyxAttributeBuffer raevyxBuffer = new RaevyxAttributeBuffer();
        raevyxBuffer.maxHealth = raevyxCurrent.maxHealth();
        raevyxBuffer.armor = raevyxCurrent.armor();
        raevyxBuffer.flyingSpeed = raevyxCurrent.flyingSpeed();
        raevyxBuffer.biteDamage = raevyxCurrent.abilityDamage("bite",
                raevyxDefaults.abilityDamage("bite", 15.0D));
        raevyxBuffer.beamDamage = raevyxCurrent.abilityDamage("lightning_beam",
                raevyxDefaults.abilityDamage("lightning_beam", 35.0D));
        raevyxBuffer.hornDamage = raevyxCurrent.abilityDamage("horn_gore",
                raevyxDefaults.abilityDamage("horn_gore", 15.0D));
        raevyxBuffer.dashDamage = raevyxCurrent.abilityDamage("dash",
                raevyxDefaults.abilityDamage("dash", 10.0D));
        raevyxBuffer.tamingChanceBase = raevyxCurrent.extraDouble("taming_chance_base", 5.0);
        raevyxBuffer.tamingChanceHearty = raevyxCurrent.extraDouble("taming_chance_hearty", 3.0);
        raevyxBuffer.tamingStunHealth = raevyxCurrent.extraDouble("taming_stun_health",
                raevyxDefaults.extraDouble("taming_stun_health", 60.0D));
        raevyxBuffer.wildFlyingSpeedMultiplier = raevyxCurrent.extraDouble("wild_flying_speed_multiplier",
                raevyxDefaults.extraDouble("wild_flying_speed_multiplier", 1.0D));
        raevyxBuffer.beamDrainPerTick = raevyxCurrent.extraDouble("beam_drain_per_tick",
                raevyxDefaults.extraDouble("beam_drain_per_tick", 0.014D));
        raevyxBuffer.beamRegenPerTick = raevyxCurrent.extraDouble("beam_regen_per_tick",
                raevyxDefaults.extraDouble("beam_regen_per_tick", 0.0025D));
        raevyxBuffer.summonStormCooldownTicks = raevyxCurrent.extraDouble("summon_storm_cooldown_ticks",
                raevyxDefaults.extraDouble("summon_storm_cooldown_ticks", 4800.0D));
        raevyxBuffer.summonStormSuperchargeTicks = raevyxCurrent.extraDouble("summon_storm_supercharge_ticks",
                raevyxDefaults.extraDouble("summon_storm_supercharge_ticks", 1200.0D));
        raevyxBuffer.summonStormSuperchargeDamageMultiplier = raevyxCurrent.extraDouble("summon_storm_supercharge_damage_multiplier",
                raevyxDefaults.extraDouble("summon_storm_supercharge_damage_multiplier", 2.0D));
        raevyxBuffer.summonStormDurationTicks = raevyxCurrent.extraDouble("summon_storm_duration_ticks",
                raevyxDefaults.extraDouble("summon_storm_duration_ticks", 1200.0D));
        raevyxBuffer.legacyTaming = raevyxCurrent.extraBoolean("legacy_taming", false);
        raevyxBuffer.eggHatchChanceNormal = raevyxCurrent.extraDouble("egg_hatch_chance_normal", 2.0D);
        raevyxBuffer.eggHatchChanceThunder = raevyxCurrent.extraDouble("egg_hatch_chance_thunder", 1.0D);
        raevyxBuffer.eggStormInstantChance = raevyxCurrent.extraDouble("egg_storm_instant_chance", 100.0D);
        raevyxBuffer.eggLootPillagerOutpost = raevyxCurrent.extraDouble("egg_loot_pillager_outpost", 0.20D);
        raevyxBuffer.eggLootShipwreckTreasure = raevyxCurrent.extraDouble("egg_loot_shipwreck_treasure", 0.15D);
        raevyxBuffer.eggLootAncientCity = raevyxCurrent.extraDouble("egg_loot_ancient_city", 0.15D);
        raevyxBuffer.eggDropChance = raevyxCurrent.extraDouble("egg_drop_chance", 0.12D);
        raevyxBuffer.aggressiveWild = raevyxCurrent.extraBoolean("aggressive_wild", false);
        raevyxBuffer.reactiveTerrainClearingOnDamage = raevyxCurrent.extraBoolean("reactive_terrain_clearing_on_damage", true);
        raevyxBuffer.reactiveTerrainClearingOnDamageTamed = raevyxCurrent.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false);

        DragonAttributeConfig nulljawCurrent = loader.getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        DragonAttributeConfig nulljawDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        NulljawAttributeBuffer nulljawBuffer = new NulljawAttributeBuffer();
        nulljawBuffer.maxHealth = nulljawCurrent.maxHealth();
        nulljawBuffer.armor = nulljawCurrent.armor();
        nulljawBuffer.swimSpeed = nulljawCurrent.extraDouble("swim_speed", 1.45D);
        nulljawBuffer.bitePhase1 = nulljawCurrent.abilityDamage("bite_phase1", 40.0D);
        nulljawBuffer.bitePhase2 = nulljawCurrent.abilityDamage("bite_phase2", 50.0D);
        nulljawBuffer.tailAttack = nulljawCurrent.abilityDamage("tail_attack", 8.0D);
        nulljawBuffer.dashTailSwipe = nulljawCurrent.abilityDamage("dash_tail_swipe", 14.0D);
        nulljawBuffer.dashClaw = nulljawCurrent.abilityDamage("dash_claw", 16.0D);
        nulljawBuffer.hornPhase1 = nulljawCurrent.abilityDamage("horn_gore_phase1", 16.0D);
        nulljawBuffer.hornPhase2 = nulljawCurrent.abilityDamage("horn_gore_phase2", 20.8D);
        nulljawBuffer.tamingChance = nulljawCurrent.extraDouble("taming_chance", 6.0);
        nulljawBuffer.legacyTaming = nulljawCurrent.extraBoolean("legacy_taming", false);
        nulljawBuffer.eggHatchChanceNormal = nulljawCurrent.extraDouble("egg_hatch_chance_normal", 3.0D);
        nulljawBuffer.eggDropChance = nulljawCurrent.extraDouble("egg_drop_chance", 0.12D);
        nulljawBuffer.aggressiveWild = nulljawCurrent.extraBoolean("aggressive_wild", false);
        nulljawBuffer.reactiveTerrainClearingOnDamage = nulljawCurrent.extraBoolean("reactive_terrain_clearing_on_damage", true);
        nulljawBuffer.reactiveTerrainClearingOnDamageTamed = nulljawCurrent.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false);

        DragonAttributeConfig stegonautCurrent = loader.getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
        DragonAttributeConfig stegonautDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
        StegonautAttributeBuffer stegonautBuffer = new StegonautAttributeBuffer();
        stegonautBuffer.maxHealth = stegonautCurrent.maxHealth();
        stegonautBuffer.armor = stegonautCurrent.armor();
        stegonautBuffer.biteDamage = stegonautCurrent.abilityDamage("bite",
                stegonautDefaults.abilityDamage("bite", 5.0D));
        stegonautBuffer.chinSlamDamage = stegonautCurrent.abilityDamage("chin_slam",
                stegonautDefaults.abilityDamage("chin_slam", 8.0D));
        stegonautBuffer.groundEatingDamage = stegonautCurrent.abilityDamage("ground_eating",
                stegonautDefaults.abilityDamage("ground_eating", 10.0D));
        stegonautBuffer.tamingChanceBase = stegonautCurrent.extraDouble("taming_chance_base", 1.0);
        stegonautBuffer.tamingChanceHearty = stegonautCurrent.extraDouble("taming_chance_hearty", 1.0);
        stegonautBuffer.eggHatchChanceNormal = stegonautCurrent.extraDouble("egg_hatch_chance_normal", 2.0D);
        stegonautBuffer.eggDropChance = stegonautCurrent.extraDouble("egg_drop_chance", 0.12D);
        stegonautBuffer.aggressiveWild = stegonautCurrent.extraBoolean("aggressive_wild", false);
        stegonautBuffer.reactiveTerrainClearingOnDamage = stegonautCurrent.extraBoolean("reactive_terrain_clearing_on_damage", true);
        stegonautBuffer.reactiveTerrainClearingOnDamageTamed = stegonautCurrent.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false);

        DragonAttributeConfig ignivorusCurrent = loader.getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        DragonAttributeConfig ignivorusDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        IgnivorusAttributeBuffer ignivorusBuffer = new IgnivorusAttributeBuffer();
        ignivorusBuffer.maxHealth = ignivorusCurrent.maxHealth();
        ignivorusBuffer.armor = ignivorusCurrent.armor();
        ignivorusBuffer.flyingSpeed = ignivorusCurrent.flyingSpeed();
        ignivorusBuffer.biteDamage = ignivorusCurrent.abilityDamage("bite",
                ignivorusDefaults.abilityDamage("bite", 50.0D));
        ignivorusBuffer.bodySlamDamage = ignivorusCurrent.abilityDamage("body_slam",
                ignivorusDefaults.abilityDamage("body_slam", 40.0D));
        ignivorusBuffer.leapSlamDamage = ignivorusCurrent.abilityDamage("leap_slam",
                ignivorusDefaults.abilityDamage("leap_slam", 50.0D));
        ignivorusBuffer.fireBreathDamage = ignivorusCurrent.abilityDamage("fire_breath",
                ignivorusDefaults.abilityDamage("fire_breath", 80.0D));
        ignivorusBuffer.fireballDamage = ignivorusCurrent.abilityDamage("fireball",
                ignivorusDefaults.abilityDamage("fireball", 70.0D));
        ignivorusBuffer.magmaPillarDamage = ignivorusCurrent.abilityDamage("magma_pillar",
                ignivorusDefaults.abilityDamage("magma_pillar", 18.0D));
        ignivorusBuffer.wingSwipeDamage = ignivorusCurrent.abilityDamage("wing_swipe",
                ignivorusDefaults.abilityDamage("wing_swipe", 15.0D));
        ignivorusBuffer.stompDamage = ignivorusCurrent.abilityDamage("stomp",
                ignivorusDefaults.abilityDamage("stomp", 18.0D));
        ignivorusBuffer.bulldozeDamage = ignivorusCurrent.abilityDamage("bulldoze",
                ignivorusDefaults.abilityDamage("bulldoze", 10.0D));
        ignivorusBuffer.ultimateDamage = ignivorusCurrent.abilityDamage("ultimate",
                ignivorusDefaults.abilityDamage("ultimate", 200.0D));
        ignivorusBuffer.ultimatePenalty = ignivorusCurrent.extraDouble("ultimate_penalty_health",
                ignivorusDefaults.extraDouble("ultimate_penalty_health", 50.0D));
        ignivorusBuffer.tamingChanceBase = ignivorusCurrent.extraDouble("taming_chance_base", 7.0);
        ignivorusBuffer.tamingChanceHearty = ignivorusCurrent.extraDouble("taming_chance_hearty", 4.0);
        ignivorusBuffer.tamingStunHealth = ignivorusCurrent.extraDouble("taming_stun_health",
                ignivorusDefaults.extraDouble("taming_stun_health", 100.0D));
        ignivorusBuffer.wildFlyingSpeedMultiplier = ignivorusCurrent.extraDouble("wild_flying_speed_multiplier",
                ignivorusDefaults.extraDouble("wild_flying_speed_multiplier", 1.0D));
        ignivorusBuffer.fireBreathDrainPerTick = ignivorusCurrent.extraDouble("fire_breath_drain_per_tick",
                ignivorusDefaults.extraDouble("fire_breath_drain_per_tick", 0.00625D));
        ignivorusBuffer.fireBreathRegenPerTick = ignivorusCurrent.extraDouble("fire_breath_regen_per_tick",
                ignivorusDefaults.extraDouble("fire_breath_regen_per_tick", 0.0025D));
        ignivorusBuffer.fireBreathFlameSpawnMultiplier = ignivorusCurrent.extraDouble("fire_breath_flame_spawn_multiplier",
                ignivorusDefaults.extraDouble("fire_breath_flame_spawn_multiplier", 1.0D));
        ignivorusBuffer.fireBreathFlameSpeedMultiplier = ignivorusCurrent.extraDouble("fire_breath_flame_speed_multiplier",
                ignivorusDefaults.extraDouble("fire_breath_flame_speed_multiplier", 1.0D));
        ignivorusBuffer.fireBreathFlameLifetimeMultiplier = ignivorusCurrent.extraDouble("fire_breath_flame_lifetime_multiplier",
                ignivorusDefaults.extraDouble("fire_breath_flame_lifetime_multiplier", 1.0D));
        ignivorusBuffer.fireBreathIgniteBlockChance = ignivorusCurrent.extraDouble("fire_breath_ignite_block_chance",
                ignivorusDefaults.extraDouble("fire_breath_ignite_block_chance", 1.0D));
        ignivorusBuffer.phase2ToggleOnChance = ignivorusCurrent.extraDouble("phase2_toggle_on_chance",
                ignivorusDefaults.extraDouble("phase2_toggle_on_chance", 0.85D));
        ignivorusBuffer.phase2ToggleOffChance = ignivorusCurrent.extraDouble("phase2_toggle_off_chance",
                ignivorusDefaults.extraDouble("phase2_toggle_off_chance", 0.05D));
        ignivorusBuffer.phase2DecisionMinTicks = ignivorusCurrent.extraDouble("phase2_decision_min_ticks",
                ignivorusDefaults.extraDouble("phase2_decision_min_ticks", 60.0D));
        ignivorusBuffer.phase2DecisionMaxTicks = ignivorusCurrent.extraDouble("phase2_decision_max_ticks",
                ignivorusDefaults.extraDouble("phase2_decision_max_ticks", 120.0D));
        ignivorusBuffer.legacyTaming = ignivorusCurrent.extraBoolean("legacy_taming", false);
        ignivorusBuffer.eggHatchChanceNormal = ignivorusCurrent.extraDouble("egg_hatch_chance_normal", 9.0D);
        ignivorusBuffer.eggLootBastionTreasure = ignivorusCurrent.extraDouble("egg_loot_bastion_treasure", 0.15D);
        ignivorusBuffer.eggLootNetherBridge = ignivorusCurrent.extraDouble("egg_loot_nether_bridge", 0.15D);
        ignivorusBuffer.eggLootAncientCity = ignivorusCurrent.extraDouble("egg_loot_ancient_city", 0.10D);
        ignivorusBuffer.eggDropChance = ignivorusCurrent.extraDouble("egg_drop_chance", 0.12D);
        ignivorusBuffer.aggressiveWild = ignivorusCurrent.extraBoolean("aggressive_wild", false);
        ignivorusBuffer.reactiveTerrainClearingOnDamage = ignivorusCurrent.extraBoolean("reactive_terrain_clearing_on_damage", true);
        ignivorusBuffer.reactiveTerrainClearingOnDamageTamed = ignivorusCurrent.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TITLE);
        builder.setTransparentBackground(true);
        builder.setSavingRunnable(() -> {
            holder.save();
            persistDragonAttributes(cindervaneBuffer, stegonautBuffer, raevyxBuffer, nulljawBuffer, ignivorusBuffer);
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory spawning = builder.getOrCreateCategory(SPAWN_CATEGORY);
        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.raevyx"),
                () -> config.raevyxSpawnWeight, value -> config.raevyxSpawnWeight = value,
                () -> config.raevyxMinGroupSize, value -> config.raevyxMinGroupSize = value,
                () -> config.raevyxMaxGroupSize, value -> config.raevyxMaxGroupSize = value,
                () -> config.raevyxAdditionalBiomes, list -> {
                    config.raevyxAdditionalBiomes.clear();
                    config.raevyxAdditionalBiomes.addAll(list);
                },
                () -> config.raevyxExcludedBiomes, list -> {
                    config.raevyxExcludedBiomes.clear();
                    config.raevyxExcludedBiomes.addAll(list);
                },
                null, null, true,
                1, 1, 2);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.stegonaut"),
                () -> config.stegonautSpawnWeight, value -> config.stegonautSpawnWeight = value,
                () -> config.stegonautMinGroupSize, value -> config.stegonautMinGroupSize = value,
                () -> config.stegonautMaxGroupSize, value -> config.stegonautMaxGroupSize = value,
                () -> config.stegonautAdditionalBiomes, list -> {
                    config.stegonautAdditionalBiomes.clear();
                    config.stegonautAdditionalBiomes.addAll(list);
                },
                () -> config.stegonautExcludedBiomes, list -> {
                    config.stegonautExcludedBiomes.clear();
                    config.stegonautExcludedBiomes.addAll(list);
                },
                null, null, true,
                5, 1, 4);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.cindervane"),
                () -> config.cindervaneSpawnWeight, value -> config.cindervaneSpawnWeight = value,
                () -> config.cindervaneMinGroupSize, value -> config.cindervaneMinGroupSize = value,
                () -> config.cindervaneMaxGroupSize, value -> config.cindervaneMaxGroupSize = value,
                () -> config.cindervaneAdditionalBiomes, list -> {
                    config.cindervaneAdditionalBiomes.clear();
                    config.cindervaneAdditionalBiomes.addAll(list);
                },
                () -> config.cindervaneExcludedBiomes, list -> {
                    config.cindervaneExcludedBiomes.clear();
                    config.cindervaneExcludedBiomes.addAll(list);
                },
                () -> config.cindervaneEggBlockWorldgen, value -> config.cindervaneEggBlockWorldgen = value, true,
                3, 1, 3);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.nulljaw"),
                () -> config.nulljawSpawnWeight, value -> config.nulljawSpawnWeight = value,
                () -> config.nulljawMinGroupSize, value -> config.nulljawMinGroupSize = value,
                () -> config.nulljawMaxGroupSize, value -> config.nulljawMaxGroupSize = value,
                () -> config.nulljawAdditionalBiomes, list -> {
                    config.nulljawAdditionalBiomes.clear();
                    config.nulljawAdditionalBiomes.addAll(list);
                },
                () -> config.nulljawExcludedBiomes, list -> {
                    config.nulljawExcludedBiomes.clear();
                    config.nulljawExcludedBiomes.addAll(list);
                },
                () -> config.nulljawEggBlockWorldgen, value -> config.nulljawEggBlockWorldgen = value, true,
                2, 1, 2);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.ignivorus"),
                () -> config.ignivorusSpawnWeight, value -> config.ignivorusSpawnWeight = value,
                () -> config.ignivorusMinGroupSize, value -> config.ignivorusMinGroupSize = value,
                () -> config.ignivorusMaxGroupSize, value -> config.ignivorusMaxGroupSize = value,
                () -> config.ignivorusAdditionalBiomes, list -> {
                    config.ignivorusAdditionalBiomes.clear();
                    config.ignivorusAdditionalBiomes.addAll(list);
                },
                () -> config.ignivorusExcludedBiomes, list -> {
                    config.ignivorusExcludedBiomes.clear();
                    config.ignivorusExcludedBiomes.addAll(list);
                },
                null, null, true,
                1, 1, 2);

        ConfigCategory attributes = builder.getOrCreateCategory(ATTRIBUTES_CATEGORY);
        addCindervaneAttributes(attributes, entryBuilder, cindervaneBuffer, cindervaneDefaults);
        addStegonautAttributes(attributes, entryBuilder, stegonautBuffer, stegonautDefaults);
        addRaevyxAttributes(attributes, entryBuilder, raevyxBuffer, raevyxDefaults);
        addNulljawAttributes(attributes, entryBuilder, nulljawBuffer, nulljawDefaults);
        addIgnivorusAttributes(attributes, entryBuilder, ignivorusBuffer, ignivorusDefaults);

        ConfigCategory others = builder.getOrCreateCategory(OTHERS_CATEGORY);
        others.addEntry(entryBuilder.startIntSlider(
                Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval"),
                config.ivyRestockInterval,
                20,
                72000
        ).setDefaultValue(24000)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval.tooltip"))
         .setSaveConsumer(value -> config.ivyRestockInterval = value)
         .build());

        return builder.build();
    }

    private void addSpawnEntries(ConfigCategory category,
                                 ConfigEntryBuilder entryBuilder,
                                 Component label,
                                 IntSupplier weightGetter,
                                 IntConsumer weightSetter,
                                 IntSupplier minGetter,
                                 IntConsumer minSetter,
                                 IntSupplier maxGetter,
                                 IntConsumer maxSetter,
                                 Supplier<List<String>> biomesGetter,
                                 Consumer<List<String>> biomesSetter,
                                 Supplier<List<String>> excludedBiomesGetter,
                                 Consumer<List<String>> excludedBiomesSetter,
                                 BooleanSupplier eggBlockWorldgenGetter,
                                 Consumer<Boolean> eggBlockWorldgenSetter,
                                 boolean defaultEggBlockWorldgen,
                                 int defaultWeight,
                                 int defaultMin,
                                 int defaultMax) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.weight"), weightGetter.getAsInt())
                .setDefaultValue(defaultWeight)
                .setMin(0)
                .setMax(5000)
                .setSaveConsumer(weightSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.min_group"), minGetter.getAsInt())
                .setDefaultValue(defaultMin)
                .setMin(1)
                .setMax(10)
                .setSaveConsumer(minSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.max_group"), maxGetter.getAsInt())
                .setDefaultValue(defaultMax)
                .setMin(1)
                .setMax(10)
                .setSaveConsumer(maxSetter::accept)
                .build());
        List<String> listCopy = new ArrayList<>(biomesGetter.get());
        entries.add(entryBuilder.startStrList(Component.translatable("config.saintsdragons.spawn.additional_biomes"), listCopy)
                .setDefaultValue(List.of())
                .setSaveConsumer(values -> biomesSetter.accept(new ArrayList<>(values)))
                .build());
        List<String> excludedListCopy = new ArrayList<>(excludedBiomesGetter.get());
        entries.add(entryBuilder.startStrList(Component.translatable("config.saintsdragons.spawn.excluded_biomes"), excludedListCopy)
                .setDefaultValue(List.of())
                .setSaveConsumer(values -> excludedBiomesSetter.accept(new ArrayList<>(values)))
                .build());
        if (eggBlockWorldgenGetter != null && eggBlockWorldgenSetter != null) {
            entries.add(entryBuilder.startBooleanToggle(
                            Component.translatable("config.saintsdragons.spawn.egg_block_worldgen"),
                            eggBlockWorldgenGetter.getAsBoolean())
                    .setDefaultValue(defaultEggBlockWorldgen)
                    .setSaveConsumer(eggBlockWorldgenSetter::accept)
                    .build());
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(label, rawEntries).setExpanded(false).build());
    }

    private void addCindervaneAttributes(ConfigCategory category,
                                         ConfigEntryBuilder entryBuilder,
                                         CindervaneAttributeBuffer buffer,
                                         DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.0D)
                .setMax(2.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.wild_flying_speed_multiplier"), buffer.wildFlyingSpeedMultiplier)
                .setDefaultValue(defaults.extraDouble("wild_flying_speed_multiplier", 1.0D))
                .setMin(0.05D)
                .setMax(10.0D)
                .setSaveConsumer(value -> buffer.wildFlyingSpeedMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 12.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit1_damage"), buffer.slashGrabHit1Damage)
                .setDefaultValue(defaults.abilityDamage("slash_grab_hit1", 5.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.slashGrabHit1Damage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit2_damage"), buffer.slashGrabHit2Damage)
                .setDefaultValue(defaults.abilityDamage("slash_grab_hit2", 7.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.slashGrabHit2Damage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.volley_damage"), buffer.volleyDamage)
                .setDefaultValue(defaults.abilityDamage("magma_volley", 20.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.volleyDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_damage"), buffer.fireBodyDamage)
                .setDefaultValue(defaults.abilityDamage("fire_body", 3.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.fireBodyDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 4.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 2.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 2.0))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.egg_drop_chance"), buffer.eggDropChance)
                .setDefaultValue(defaults.extraDouble("egg_drop_chance", 0.12D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggDropChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_explosion_damage"), buffer.fireBodyExplosionDamage)
                .setDefaultValue(defaults.extraDouble("fire_body_explosion_damage", 200.0D))
                .setMin(0.0D)
                .setMax(1000.0D)
                .setSaveConsumer(value -> buffer.fireBodyExplosionDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_self_damage_on_crash"), buffer.fireBodySelfDamageOnCrash)
                .setDefaultValue(defaults.extraDouble("fire_body_self_damage_on_crash", 40.0D))
                .setMin(0.0D)
                .setMax(1000.0D)
                .setSaveConsumer(value -> buffer.fireBodySelfDamageOnCrash = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.cindervane.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"), buffer.reactiveTerrainClearingOnDamage)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage", true))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamage = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"), buffer.reactiveTerrainClearingOnDamageTamed)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamageTamed = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.cindervane"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addStegonautAttributes(ConfigCategory category,
                                        ConfigEntryBuilder entryBuilder,
                                        StegonautAttributeBuffer buffer,
                                        DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 5.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.chin_slam_damage"), buffer.chinSlamDamage)
                .setDefaultValue(defaults.abilityDamage("chin_slam", 8.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.chinSlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_eating_damage"), buffer.groundEatingDamage)
                .setDefaultValue(defaults.abilityDamage("ground_eating", 10.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.groundEatingDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 1.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 1.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 2.0))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_drop_chance"), buffer.eggDropChance)
                .setDefaultValue(defaults.extraDouble("egg_drop_chance", 0.12D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggDropChance = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.stegonaut.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"), buffer.reactiveTerrainClearingOnDamage)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage", true))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamage = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"), buffer.reactiveTerrainClearingOnDamageTamed)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamageTamed = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.stegonaut"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addRaevyxAttributes(ConfigCategory category,
                                     ConfigEntryBuilder entryBuilder,
                                     RaevyxAttributeBuffer buffer,
                                     DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.0D)
                .setMax(2.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.wild_flying_speed_multiplier"), buffer.wildFlyingSpeedMultiplier)
                .setDefaultValue(defaults.extraDouble("wild_flying_speed_multiplier", 1.0D))
                .setMin(0.05D)
                .setMax(10.0D)
                .setSaveConsumer(value -> buffer.wildFlyingSpeedMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 15.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.beam_damage"), buffer.beamDamage)
                .setDefaultValue(defaults.abilityDamage("lightning_beam", 35.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.beamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.horn_damage"), buffer.hornDamage)
                .setDefaultValue(defaults.abilityDamage("horn_gore", 15.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.hornDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.dash_damage"), buffer.dashDamage)
                .setDefaultValue(defaults.abilityDamage("dash", 10.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.dashDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 5.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 3.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_stun_health"), buffer.tamingStunHealth)
                .setDefaultValue(defaults.extraDouble("taming_stun_health", 60.0D))
                .setMin(0.0D)
                .setMax(1000.0D)
                .setSaveConsumer(value -> buffer.tamingStunHealth = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.raevyx.legacy_taming"), buffer.legacyTaming)
                .setDefaultValue(defaults.extraBoolean("legacy_taming", false))
                .setTooltip(Component.translatable("config.saintsdragons.attributes.legacy_taming.tooltip"))
                .setSaveConsumer(value -> buffer.legacyTaming = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.beam_drain_per_tick"), buffer.beamDrainPerTick)
                .setDefaultValue(defaults.extraDouble("beam_drain_per_tick", 0.014D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.beamDrainPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.beam_regen_per_tick"), buffer.beamRegenPerTick)
                .setDefaultValue(defaults.extraDouble("beam_regen_per_tick", 0.0025D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.beamRegenPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_cooldown_ticks"), buffer.summonStormCooldownTicks)
                .setDefaultValue(defaults.extraDouble("summon_storm_cooldown_ticks", 4800.0D))
                .setMin(20.0D)
                .setMax(120000.0D)
                .setSaveConsumer(value -> buffer.summonStormCooldownTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_supercharge_ticks"), buffer.summonStormSuperchargeTicks)
                .setDefaultValue(defaults.extraDouble("summon_storm_supercharge_ticks", 1200.0D))
                .setMin(20.0D)
                .setMax(120000.0D)
                .setSaveConsumer(value -> buffer.summonStormSuperchargeTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_supercharge_damage_multiplier"), buffer.summonStormSuperchargeDamageMultiplier)
                .setDefaultValue(defaults.extraDouble("summon_storm_supercharge_damage_multiplier", 2.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.summonStormSuperchargeDamageMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_duration_ticks"), buffer.summonStormDurationTicks)
                .setDefaultValue(defaults.extraDouble("summon_storm_duration_ticks", 1200.0D))
                .setMin(20.0D)
                .setMax(120000.0D)
                .setSaveConsumer(value -> buffer.summonStormDurationTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 2.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_chance_thunder"), buffer.eggHatchChanceThunder)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_thunder", 1.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceThunder = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_storm_instant_chance"), buffer.eggStormInstantChance)
                .setDefaultValue(defaults.extraDouble("egg_storm_instant_chance", 100.0D))
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.eggStormInstantChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_pillager_outpost"), buffer.eggLootPillagerOutpost)
                .setDefaultValue(defaults.extraDouble("egg_loot_pillager_outpost", 0.20D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootPillagerOutpost = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_shipwreck_treasure"), buffer.eggLootShipwreckTreasure)
                .setDefaultValue(defaults.extraDouble("egg_loot_shipwreck_treasure", 0.15D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootShipwreckTreasure = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_ancient_city"), buffer.eggLootAncientCity)
                .setDefaultValue(defaults.extraDouble("egg_loot_ancient_city", 0.15D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootAncientCity = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.egg_drop_chance"), buffer.eggDropChance)
                .setDefaultValue(defaults.extraDouble("egg_drop_chance", 0.12D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggDropChance = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.raevyx.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"), buffer.reactiveTerrainClearingOnDamage)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage", true))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamage = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"), buffer.reactiveTerrainClearingOnDamageTamed)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamageTamed = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.raevyx"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addNulljawAttributes(ConfigCategory category,
                                     ConfigEntryBuilder entryBuilder,
                                     NulljawAttributeBuffer buffer,
                                     DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.swim_speed"), buffer.swimSpeed)
                .setDefaultValue(defaults.extraDouble("swim_speed", 1.45D))
                .setMin(0.1D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.swimSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase1"), buffer.bitePhase1)
                .setDefaultValue(defaults.abilityDamage("bite_phase1", 40.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.bitePhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase2"), buffer.bitePhase2)
                .setDefaultValue(defaults.abilityDamage("bite_phase2", 50.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.bitePhase2 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.tail_attack"), buffer.tailAttack)
                .setDefaultValue(defaults.abilityDamage("tail_attack", 8.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.tailAttack = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.dash_tail_swipe"), buffer.dashTailSwipe)
                .setDefaultValue(defaults.abilityDamage("dash_tail_swipe", 14.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.dashTailSwipe = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.dash_claw"), buffer.dashClaw)
                .setDefaultValue(defaults.abilityDamage("dash_claw", 16.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.dashClaw = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase1"), buffer.hornPhase1)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase1", 16.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.hornPhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase2"), buffer.hornPhase2)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase2", 20.8D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.hornPhase2 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.taming_chance"), buffer.tamingChance)
                .setDefaultValue(defaults.extraDouble("taming_chance", 6.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChance = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.nulljaw.legacy_taming"), buffer.legacyTaming)
                .setDefaultValue(defaults.extraBoolean("legacy_taming", false))
                .setTooltip(Component.translatable("config.saintsdragons.attributes.legacy_taming.tooltip"))
                .setSaveConsumer(value -> buffer.legacyTaming = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 3.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.egg_drop_chance"), buffer.eggDropChance)
                .setDefaultValue(defaults.extraDouble("egg_drop_chance", 0.12D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggDropChance = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.nulljaw.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"), buffer.reactiveTerrainClearingOnDamage)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage", true))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamage = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"), buffer.reactiveTerrainClearingOnDamageTamed)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamageTamed = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.nulljaw"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addIgnivorusAttributes(ConfigCategory category,
                                        ConfigEntryBuilder entryBuilder,
                                        IgnivorusAttributeBuffer buffer,
                                        DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.0D)
                .setMax(2.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.wild_flying_speed_multiplier"), buffer.wildFlyingSpeedMultiplier)
                .setDefaultValue(defaults.extraDouble("wild_flying_speed_multiplier", 1.0D))
                .setMin(0.05D)
                .setMax(10.0D)
                .setSaveConsumer(value -> buffer.wildFlyingSpeedMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 50.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.body_slam_damage"), buffer.bodySlamDamage)
                .setDefaultValue(defaults.abilityDamage("body_slam", 40.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.bodySlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.leap_slam_damage"), buffer.leapSlamDamage)
                .setDefaultValue(defaults.abilityDamage("leap_slam", 50.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.leapSlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_damage"), buffer.fireBreathDamage)
                .setDefaultValue(defaults.abilityDamage("fire_breath", 80.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.fireBreathDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fireball_damage"), buffer.fireballDamage)
                .setDefaultValue(defaults.abilityDamage("fireball", 70.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.fireballDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.magma_pillar_damage"), buffer.magmaPillarDamage)
                .setDefaultValue(defaults.abilityDamage("magma_pillar", 18.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.magmaPillarDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.wing_swipe_damage"), buffer.wingSwipeDamage)
                .setDefaultValue(defaults.abilityDamage("wing_swipe", 15.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.wingSwipeDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.stomp_damage"), buffer.stompDamage)
                .setDefaultValue(defaults.abilityDamage("stomp", 18.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.stompDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.bulldoze_damage"), buffer.bulldozeDamage)
                .setDefaultValue(defaults.abilityDamage("bulldoze", 10.0D))
                .setMin(0.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.bulldozeDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_damage"), buffer.ultimateDamage)
                .setDefaultValue(defaults.abilityDamage("ultimate", 200.0D))
                .setMin(0.0D)
                .setMax(10000.0D)
                .setSaveConsumer(value -> buffer.ultimateDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_penalty"), buffer.ultimatePenalty)
                .setDefaultValue(defaults.extraDouble("ultimate_penalty_health", 50.0D))
                .setMin(1.0D)
                .setMax(10000.0D)
                .setSaveConsumer(value -> buffer.ultimatePenalty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 7.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 4.0))
                .setMin(1.0D)
                .setMax(20.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_stun_health"), buffer.tamingStunHealth)
                .setDefaultValue(defaults.extraDouble("taming_stun_health", 100.0D))
                .setMin(0.0D)
                .setMax(1000.0D)
                .setSaveConsumer(value -> buffer.tamingStunHealth = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.ignivorus.legacy_taming"), buffer.legacyTaming)
                .setDefaultValue(defaults.extraBoolean("legacy_taming", false))
                .setTooltip(Component.translatable("config.saintsdragons.attributes.legacy_taming.tooltip"))
                .setSaveConsumer(value -> buffer.legacyTaming = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_drain_per_tick"), buffer.fireBreathDrainPerTick)
                .setDefaultValue(defaults.extraDouble("fire_breath_drain_per_tick", 0.00625D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.fireBreathDrainPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_regen_per_tick"), buffer.fireBreathRegenPerTick)
                .setDefaultValue(defaults.extraDouble("fire_breath_regen_per_tick", 0.0025D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.fireBreathRegenPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_spawn_multiplier"), buffer.fireBreathFlameSpawnMultiplier)
                .setDefaultValue(defaults.extraDouble("fire_breath_flame_spawn_multiplier", 1.0D))
                .setMin(0.0D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.fireBreathFlameSpawnMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_speed_multiplier"), buffer.fireBreathFlameSpeedMultiplier)
                .setDefaultValue(defaults.extraDouble("fire_breath_flame_speed_multiplier", 1.0D))
                .setMin(0.0D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.fireBreathFlameSpeedMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_lifetime_multiplier"), buffer.fireBreathFlameLifetimeMultiplier)
                .setDefaultValue(defaults.extraDouble("fire_breath_flame_lifetime_multiplier", 1.0D))
                .setMin(0.0D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.fireBreathFlameLifetimeMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_ignite_block_chance"), buffer.fireBreathIgniteBlockChance)
                .setDefaultValue(defaults.extraDouble("fire_breath_ignite_block_chance", 1.0D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.fireBreathIgniteBlockChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_on_chance"), buffer.phase2ToggleOnChance)
                .setDefaultValue(defaults.extraDouble("phase2_toggle_on_chance", 0.85D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.phase2ToggleOnChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_off_chance"), buffer.phase2ToggleOffChance)
                .setDefaultValue(defaults.extraDouble("phase2_toggle_off_chance", 0.05D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.phase2ToggleOffChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_decision_min_ticks"), buffer.phase2DecisionMinTicks)
                .setDefaultValue(defaults.extraDouble("phase2_decision_min_ticks", 60.0D))
                .setMin(1.0D)
                .setMax(1200.0D)
                .setSaveConsumer(value -> buffer.phase2DecisionMinTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_decision_max_ticks"), buffer.phase2DecisionMaxTicks)
                .setDefaultValue(defaults.extraDouble("phase2_decision_max_ticks", 120.0D))
                .setMin(1.0D)
                .setMax(1200.0D)
                .setSaveConsumer(value -> buffer.phase2DecisionMaxTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 9.0D))
                .setMin(1.0D)
                .setMax(300.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_bastion_treasure"), buffer.eggLootBastionTreasure)
                .setDefaultValue(defaults.extraDouble("egg_loot_bastion_treasure", 0.15D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootBastionTreasure = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_nether_bridge"), buffer.eggLootNetherBridge)
                .setDefaultValue(defaults.extraDouble("egg_loot_nether_bridge", 0.15D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootNetherBridge = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_ancient_city"), buffer.eggLootAncientCity)
                .setDefaultValue(defaults.extraDouble("egg_loot_ancient_city", 0.10D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggLootAncientCity = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_drop_chance"), buffer.eggDropChance)
                .setDefaultValue(defaults.extraDouble("egg_drop_chance", 0.12D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.eggDropChance = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.ignivorus.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"), buffer.reactiveTerrainClearingOnDamage)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage", true))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamage = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"), buffer.reactiveTerrainClearingOnDamageTamed)
                .setDefaultValue(defaults.extraBoolean("reactive_terrain_clearing_on_damage_tamed", false))
                .setSaveConsumer(value -> buffer.reactiveTerrainClearingOnDamageTamed = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.ignivorus"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void persistDragonAttributes(CindervaneAttributeBuffer cindervaneBuffer,
                                         StegonautAttributeBuffer stegonautBuffer,
                                         RaevyxAttributeBuffer raevyxBuffer,
                                         NulljawAttributeBuffer nulljawBuffer,
                                         IgnivorusAttributeBuffer ignivorusBuffer) {
        DragonAttributeConfigLoader loader = DragonAttributeConfigLoader.getInstance();
        DragonAttributeConfig current = loader.getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        Map<String, DragonAbilityOverride> abilities = new HashMap<>(current.abilities());
        abilities.put("bite", DragonAbilityOverride.ofDamage(cindervaneBuffer.biteDamage));
        abilities.put("slash_grab_hit1", DragonAbilityOverride.ofDamage(cindervaneBuffer.slashGrabHit1Damage));
        abilities.put("slash_grab_hit2", DragonAbilityOverride.ofDamage(cindervaneBuffer.slashGrabHit2Damage));
        abilities.put("magma_volley", DragonAbilityOverride.ofDamage(cindervaneBuffer.volleyDamage));
        abilities.put("fire_body", DragonAbilityOverride.ofDamage(cindervaneBuffer.fireBodyDamage));
        DragonAttributeConfig updated = new DragonAttributeConfig(
                cindervaneBuffer.maxHealth,
                cindervaneBuffer.armor,
                cindervaneBuffer.flyingSpeed,
                abilities,
                Map.of(
                        "taming_chance_base", cindervaneBuffer.tamingChanceBase,
                        "taming_chance_hearty", cindervaneBuffer.tamingChanceHearty,
                        "egg_hatch_chance_normal", cindervaneBuffer.eggHatchChanceNormal,
                        "egg_drop_chance", cindervaneBuffer.eggDropChance,
                        "fire_body_explosion_damage", cindervaneBuffer.fireBodyExplosionDamage,
                        "fire_body_self_damage_on_crash", cindervaneBuffer.fireBodySelfDamageOnCrash,
                        "wild_flying_speed_multiplier", cindervaneBuffer.wildFlyingSpeedMultiplier
                ),
                Map.of(
                        "aggressive_wild", cindervaneBuffer.aggressiveWild,
                        "reactive_terrain_clearing_on_damage", cindervaneBuffer.reactiveTerrainClearingOnDamage,
                        "reactive_terrain_clearing_on_damage_tamed", cindervaneBuffer.reactiveTerrainClearingOnDamageTamed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.CINDERVANE_ID, updated);

        DragonAttributeConfig stegonautCurrent = loader.getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
        Map<String, DragonAbilityOverride> stegonautAbilities = new HashMap<>(stegonautCurrent.abilities());
        stegonautAbilities.put("bite", DragonAbilityOverride.ofDamage(stegonautBuffer.biteDamage));
        stegonautAbilities.put("chin_slam", DragonAbilityOverride.ofDamage(stegonautBuffer.chinSlamDamage));
        stegonautAbilities.put("ground_eating", DragonAbilityOverride.ofDamage(stegonautBuffer.groundEatingDamage));
        DragonAttributeConfig updatedStegonaut = new DragonAttributeConfig(
                stegonautBuffer.maxHealth,
                stegonautBuffer.armor,
                0.0D,
                stegonautAbilities,
                Map.of(
                        "taming_chance_base", stegonautBuffer.tamingChanceBase,
                        "taming_chance_hearty", stegonautBuffer.tamingChanceHearty,
                        "egg_hatch_chance_normal", stegonautBuffer.eggHatchChanceNormal,
                        "egg_drop_chance", stegonautBuffer.eggDropChance
                ),
                Map.of(
                        "aggressive_wild", stegonautBuffer.aggressiveWild,
                        "reactive_terrain_clearing_on_damage", stegonautBuffer.reactiveTerrainClearingOnDamage,
                        "reactive_terrain_clearing_on_damage_tamed", stegonautBuffer.reactiveTerrainClearingOnDamageTamed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.STEGONAUT_ID, updatedStegonaut);

        DragonAttributeConfig raevyxCurrent = loader.getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        Map<String, DragonAbilityOverride> raevyxAbilities = new HashMap<>(raevyxCurrent.abilities());
        raevyxAbilities.put("bite", DragonAbilityOverride.ofDamage(raevyxBuffer.biteDamage));
        raevyxAbilities.put("lightning_beam", DragonAbilityOverride.ofDamage(raevyxBuffer.beamDamage));
        raevyxAbilities.put("horn_gore", DragonAbilityOverride.ofDamage(raevyxBuffer.hornDamage));
        raevyxAbilities.put("dash", DragonAbilityOverride.ofDamage(raevyxBuffer.dashDamage));
        DragonAttributeConfig updatedRaevyx = new DragonAttributeConfig(
                raevyxBuffer.maxHealth,
                raevyxBuffer.armor,
                raevyxBuffer.flyingSpeed,
                raevyxAbilities,
                buildRaevyxExtras(raevyxBuffer),
                Map.of(
                        "legacy_taming", raevyxBuffer.legacyTaming,
                        "aggressive_wild", raevyxBuffer.aggressiveWild,
                        "reactive_terrain_clearing_on_damage", raevyxBuffer.reactiveTerrainClearingOnDamage,
                        "reactive_terrain_clearing_on_damage_tamed", raevyxBuffer.reactiveTerrainClearingOnDamageTamed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.RAEVYX_ID, updatedRaevyx);

        Map<String, DragonAbilityOverride> nulljawAbilities = new HashMap<>();
        nulljawAbilities.put("bite_phase1", DragonAbilityOverride.ofDamage(nulljawBuffer.bitePhase1));
        nulljawAbilities.put("bite_phase2", DragonAbilityOverride.ofDamage(nulljawBuffer.bitePhase2));
        nulljawAbilities.put("tail_attack", DragonAbilityOverride.ofDamage(nulljawBuffer.tailAttack));
        nulljawAbilities.put("dash_tail_swipe", DragonAbilityOverride.ofDamage(nulljawBuffer.dashTailSwipe));
        nulljawAbilities.put("dash_claw", DragonAbilityOverride.ofDamage(nulljawBuffer.dashClaw));
        nulljawAbilities.put("horn_gore_phase1", DragonAbilityOverride.ofDamage(nulljawBuffer.hornPhase1));
        nulljawAbilities.put("horn_gore_phase2", DragonAbilityOverride.ofDamage(nulljawBuffer.hornPhase2));
        DragonAttributeConfig updatedNulljaw = new DragonAttributeConfig(
                nulljawBuffer.maxHealth,
                nulljawBuffer.armor,
                0.0D,
                nulljawAbilities,
                Map.of(
                        "swim_speed", nulljawBuffer.swimSpeed,
                        "taming_chance", nulljawBuffer.tamingChance,
                        "egg_hatch_chance_normal", nulljawBuffer.eggHatchChanceNormal,
                        "egg_drop_chance", nulljawBuffer.eggDropChance
                ),
                Map.of(
                        "legacy_taming", nulljawBuffer.legacyTaming,
                        "aggressive_wild", nulljawBuffer.aggressiveWild,
                        "reactive_terrain_clearing_on_damage", nulljawBuffer.reactiveTerrainClearingOnDamage,
                        "reactive_terrain_clearing_on_damage_tamed", nulljawBuffer.reactiveTerrainClearingOnDamageTamed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.NULLJAW_ID, updatedNulljaw);

        DragonAttributeConfig ignivorusCurrent = loader.getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        Map<String, DragonAbilityOverride> ignivorusAbilities = new HashMap<>(ignivorusCurrent.abilities());
        ignivorusAbilities.put("bite", DragonAbilityOverride.ofDamage(ignivorusBuffer.biteDamage));
        ignivorusAbilities.put("body_slam", DragonAbilityOverride.ofDamage(ignivorusBuffer.bodySlamDamage));
        ignivorusAbilities.put("leap_slam", DragonAbilityOverride.ofDamage(ignivorusBuffer.leapSlamDamage));
        ignivorusAbilities.put("fire_breath", DragonAbilityOverride.ofDamage(ignivorusBuffer.fireBreathDamage));
        ignivorusAbilities.put("fireball", DragonAbilityOverride.ofDamage(ignivorusBuffer.fireballDamage));
        ignivorusAbilities.put("magma_pillar", DragonAbilityOverride.ofDamage(ignivorusBuffer.magmaPillarDamage));
        ignivorusAbilities.put("wing_swipe", DragonAbilityOverride.ofDamage(ignivorusBuffer.wingSwipeDamage));
        ignivorusAbilities.put("stomp", DragonAbilityOverride.ofDamage(ignivorusBuffer.stompDamage));
        ignivorusAbilities.put("bulldoze", DragonAbilityOverride.ofDamage(ignivorusBuffer.bulldozeDamage));
        ignivorusAbilities.put("ultimate", DragonAbilityOverride.ofDamage(ignivorusBuffer.ultimateDamage));
        DragonAttributeConfig updatedIgnivorus = new DragonAttributeConfig(
                ignivorusBuffer.maxHealth,
                ignivorusBuffer.armor,
                ignivorusBuffer.flyingSpeed,
                ignivorusAbilities,
                buildIgnivorusExtras(ignivorusBuffer),
                Map.of(
                        "legacy_taming", ignivorusBuffer.legacyTaming,
                        "aggressive_wild", ignivorusBuffer.aggressiveWild,
                        "reactive_terrain_clearing_on_damage", ignivorusBuffer.reactiveTerrainClearingOnDamage,
                        "reactive_terrain_clearing_on_damage_tamed", ignivorusBuffer.reactiveTerrainClearingOnDamageTamed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.IGNIVORUS_ID, updatedIgnivorus);
    }

    private static final class CindervaneAttributeBuffer {
        double maxHealth;
        double armor;
        double flyingSpeed;
        double biteDamage;
        double slashGrabHit1Damage;
        double slashGrabHit2Damage;
        double volleyDamage;
        double fireBodyDamage;
        double tamingChanceBase;
        double tamingChanceHearty;
        double eggHatchChanceNormal;
        double eggDropChance;
        double fireBodyExplosionDamage;
        double fireBodySelfDamageOnCrash;
        double wildFlyingSpeedMultiplier;
        boolean aggressiveWild;
        boolean reactiveTerrainClearingOnDamage;
        boolean reactiveTerrainClearingOnDamageTamed;
    }

    private static final class StegonautAttributeBuffer {
        double maxHealth;
        double armor;
        double biteDamage;
        double chinSlamDamage;
        double groundEatingDamage;
        double tamingChanceBase;
        double tamingChanceHearty;
        double eggHatchChanceNormal;
        double eggDropChance;
        boolean aggressiveWild;
        boolean reactiveTerrainClearingOnDamage;
        boolean reactiveTerrainClearingOnDamageTamed;
    }

    private static final class RaevyxAttributeBuffer {
        double maxHealth;
        double armor;
        double flyingSpeed;
        double biteDamage;
        double beamDamage;
        double hornDamage;
        double dashDamage;
        double tamingChanceBase;
        double tamingChanceHearty;
        double tamingStunHealth;
        double wildFlyingSpeedMultiplier;
        double beamDrainPerTick;
        double beamRegenPerTick;
        double summonStormCooldownTicks;
        double summonStormSuperchargeTicks;
        double summonStormSuperchargeDamageMultiplier;
        double summonStormDurationTicks;
        boolean legacyTaming;
        double eggHatchChanceNormal;
        double eggHatchChanceThunder;
        double eggStormInstantChance;
        double eggLootPillagerOutpost;
        double eggLootShipwreckTreasure;
        double eggLootAncientCity;
        double eggDropChance;
        boolean aggressiveWild;
        boolean reactiveTerrainClearingOnDamage;
        boolean reactiveTerrainClearingOnDamageTamed;
    }

    private static final class NulljawAttributeBuffer {
        double maxHealth;
        double armor;
        double swimSpeed;
        double bitePhase1;
        double bitePhase2;
        double tailAttack;
        double dashTailSwipe;
        double dashClaw;
        double hornPhase1;
        double hornPhase2;
        double tamingChance;
        boolean legacyTaming;
        double eggHatchChanceNormal;
        double eggDropChance;
        boolean aggressiveWild;
        boolean reactiveTerrainClearingOnDamage;
        boolean reactiveTerrainClearingOnDamageTamed;
    }

    private static final class IgnivorusAttributeBuffer {
        double maxHealth;
        double armor;
        double flyingSpeed;
        double biteDamage;
        double bodySlamDamage;
        double leapSlamDamage;
        double fireBreathDamage;
        double fireballDamage;
        double magmaPillarDamage;
        double wingSwipeDamage;
        double stompDamage;
        double bulldozeDamage;
        double ultimateDamage;
        double ultimatePenalty;
        double tamingChanceBase;
        double tamingChanceHearty;
        double tamingStunHealth;
        double wildFlyingSpeedMultiplier;
        double fireBreathDrainPerTick;
        double fireBreathRegenPerTick;
        double fireBreathFlameSpawnMultiplier;
        double fireBreathFlameSpeedMultiplier;
        double fireBreathFlameLifetimeMultiplier;
        double fireBreathIgniteBlockChance;
        double phase2ToggleOnChance;
        double phase2ToggleOffChance;
        double phase2DecisionMinTicks;
        double phase2DecisionMaxTicks;
        boolean legacyTaming;
        double eggHatchChanceNormal;
        double eggLootBastionTreasure;
        double eggLootNetherBridge;
        double eggLootAncientCity;
        double eggDropChance;
        boolean aggressiveWild;
        boolean reactiveTerrainClearingOnDamage;
        boolean reactiveTerrainClearingOnDamageTamed;
    }

    private static Map<String, Double> buildRaevyxExtras(RaevyxAttributeBuffer buffer) {
        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", buffer.tamingChanceBase);
        extras.put("taming_chance_hearty", buffer.tamingChanceHearty);
        extras.put("taming_stun_health", buffer.tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", buffer.wildFlyingSpeedMultiplier);
        extras.put("beam_drain_per_tick", buffer.beamDrainPerTick);
        extras.put("beam_regen_per_tick", buffer.beamRegenPerTick);
        extras.put("summon_storm_cooldown_ticks", buffer.summonStormCooldownTicks);
        extras.put("summon_storm_supercharge_ticks", buffer.summonStormSuperchargeTicks);
        extras.put("summon_storm_supercharge_damage_multiplier", buffer.summonStormSuperchargeDamageMultiplier);
        extras.put("summon_storm_duration_ticks", buffer.summonStormDurationTicks);
        extras.put("egg_hatch_chance_normal", buffer.eggHatchChanceNormal);
        extras.put("egg_hatch_chance_thunder", buffer.eggHatchChanceThunder);
        extras.put("egg_storm_instant_chance", buffer.eggStormInstantChance);
        extras.put("egg_loot_pillager_outpost", buffer.eggLootPillagerOutpost);
        extras.put("egg_loot_shipwreck_treasure", buffer.eggLootShipwreckTreasure);
        extras.put("egg_loot_ancient_city", buffer.eggLootAncientCity);
        extras.put("egg_drop_chance", buffer.eggDropChance);
        return extras;
    }

    private static Map<String, Double> buildIgnivorusExtras(IgnivorusAttributeBuffer buffer) {
        Map<String, Double> extras = new HashMap<>();
        extras.put("ultimate_penalty_health", buffer.ultimatePenalty);
        extras.put("taming_chance_base", buffer.tamingChanceBase);
        extras.put("taming_chance_hearty", buffer.tamingChanceHearty);
        extras.put("taming_stun_health", buffer.tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", buffer.wildFlyingSpeedMultiplier);
        extras.put("fire_breath_drain_per_tick", buffer.fireBreathDrainPerTick);
        extras.put("fire_breath_regen_per_tick", buffer.fireBreathRegenPerTick);
        extras.put("fire_breath_flame_spawn_multiplier", buffer.fireBreathFlameSpawnMultiplier);
        extras.put("fire_breath_flame_speed_multiplier", buffer.fireBreathFlameSpeedMultiplier);
        extras.put("fire_breath_flame_lifetime_multiplier", buffer.fireBreathFlameLifetimeMultiplier);
        extras.put("fire_breath_ignite_block_chance", buffer.fireBreathIgniteBlockChance);
        extras.put("phase2_toggle_on_chance", buffer.phase2ToggleOnChance);
        extras.put("phase2_toggle_off_chance", buffer.phase2ToggleOffChance);
        extras.put("phase2_decision_min_ticks", buffer.phase2DecisionMinTicks);
        extras.put("phase2_decision_max_ticks", buffer.phase2DecisionMaxTicks);
        extras.put("egg_hatch_chance_normal", buffer.eggHatchChanceNormal);
        extras.put("egg_loot_bastion_treasure", buffer.eggLootBastionTreasure);
        extras.put("egg_loot_nether_bridge", buffer.eggLootNetherBridge);
        extras.put("egg_loot_ancient_city", buffer.eggLootAncientCity);
        extras.put("egg_drop_chance", buffer.eggDropChance);
        return extras;
    }
}
