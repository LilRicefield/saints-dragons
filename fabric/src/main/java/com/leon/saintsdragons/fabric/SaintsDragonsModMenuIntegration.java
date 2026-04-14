package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricSpawnConfig;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
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
        ConfigHolder<SaintsDragonsFabricSpawnConfig> holder = AutoConfig.getConfigHolder(SaintsDragonsFabricSpawnConfig.class);
        SaintsDragonsFabricSpawnConfig config = holder.getConfig();

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
        cindervaneBuffer.tamingChanceBase = cindervaneCurrent.extraDouble("taming_chance_base", 25.0);
        cindervaneBuffer.tamingChanceChicken = cindervaneCurrent.extraDouble("taming_chance_chicken", 33.3333D);
        cindervaneBuffer.tamingChanceHearty = cindervaneCurrent.extraDouble("taming_chance_hearty", 50.0);
        cindervaneBuffer.eggHatchChanceNormal = cindervaneCurrent.extraDouble("egg_hatch_chance_normal", 2.0);
        cindervaneBuffer.eggDropChance = cindervaneCurrent.extraDouble("egg_drop_chance", 0.12D);
        cindervaneBuffer.scaleDropChanceBrush = cindervaneCurrent.extraDouble("scale_drop_chance_brush",
                cindervaneDefaults.extraDouble("scale_drop_chance_brush", 0.30D));
        cindervaneBuffer.fireBodyExplosionDamage = cindervaneCurrent.extraDouble("fire_body_explosion_damage", 200.0D);
        cindervaneBuffer.fireBodySelfDamageOnCrash = cindervaneCurrent.extraDouble("fire_body_self_damage_on_crash", 40.0D);
        cindervaneBuffer.wildFlyingSpeedMultiplier = cindervaneCurrent.extraDouble("wild_flying_speed_multiplier",
                cindervaneDefaults.extraDouble("wild_flying_speed_multiplier", 1.0D));
        cindervaneBuffer.aggressiveWild = cindervaneCurrent.extraBoolean("aggressive_wild", false);

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
        raevyxBuffer.tamingChanceBase = raevyxCurrent.extraDouble("taming_chance_base", 20.0);
        raevyxBuffer.tamingChanceHearty = raevyxCurrent.extraDouble("taming_chance_hearty", 33.3333D);
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
        raevyxBuffer.eggLootAncientCity = raevyxCurrent.extraDouble("egg_loot_ancient_city", 0.15D);
        raevyxBuffer.eggDropChance = raevyxCurrent.extraDouble("egg_drop_chance", 0.12D);
        raevyxBuffer.scaleDropChanceBrush = raevyxCurrent.extraDouble("scale_drop_chance_brush",
                raevyxDefaults.extraDouble("scale_drop_chance_brush", 0.35D));
        raevyxBuffer.aggressiveWild = raevyxCurrent.extraBoolean("aggressive_wild", false);

        DragonAttributeConfig varasuchusCurrent = loader.getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
        DragonAttributeConfig varasuchusDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
        VarasuchusAttributeBuffer varasuchusBuffer = new VarasuchusAttributeBuffer();
        varasuchusBuffer.maxHealth = varasuchusCurrent.maxHealth();
        varasuchusBuffer.armor = varasuchusCurrent.armor();
        varasuchusBuffer.swimSpeed = varasuchusCurrent.extraDouble("swim_speed", 1.45D);
        varasuchusBuffer.bitePhase1 = varasuchusCurrent.abilityDamage("bite_phase1", 15.0D);
        varasuchusBuffer.bitePhase2 = varasuchusCurrent.abilityDamage("bite_phase2", 25.0D);
        varasuchusBuffer.tailAttack = varasuchusCurrent.abilityDamage("tail_attack", 7.0D);
        varasuchusBuffer.dashTailSwipe = varasuchusCurrent.abilityDamage("dash_tail_swipe", 10.0D);
        varasuchusBuffer.dashClaw = varasuchusCurrent.abilityDamage("dash_claw", 15.0D);
        varasuchusBuffer.clawAttack = varasuchusCurrent.abilityDamage("claw_attack", 8.0D);
        varasuchusBuffer.hornPhase1 = varasuchusCurrent.abilityDamage("horn_gore_phase1", 8.0D);
        varasuchusBuffer.hornPhase2 = varasuchusCurrent.abilityDamage("horn_gore_phase2", 15.8D);
        varasuchusBuffer.tamingChance = varasuchusCurrent.extraDouble("taming_chance", 16.6667D);
        varasuchusBuffer.tamingChanceTropical = varasuchusCurrent.extraDouble("taming_chance_tropical", 25.0D);
        varasuchusBuffer.legacyTaming = varasuchusCurrent.extraBoolean("legacy_taming", false);
        varasuchusBuffer.eggHatchChanceNormal = varasuchusCurrent.extraDouble("egg_hatch_chance_normal", 3.0D);
        varasuchusBuffer.eggDropChance = varasuchusCurrent.extraDouble("egg_drop_chance", 0.12D);
        varasuchusBuffer.scaleDropChanceBrush = varasuchusCurrent.extraDouble("scale_drop_chance_brush",
                varasuchusDefaults.extraDouble("scale_drop_chance_brush", 0.30D));
        varasuchusBuffer.aggressiveWild = varasuchusCurrent.extraBoolean("aggressive_wild", false);

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
        stegonautBuffer.tamingChanceBase = stegonautCurrent.extraDouble("taming_chance_base", 100.0);
        stegonautBuffer.tamingChanceHearty = stegonautCurrent.extraDouble("taming_chance_hearty", 100.0);
        stegonautBuffer.eggHatchChanceNormal = stegonautCurrent.extraDouble("egg_hatch_chance_normal", 2.0D);
        stegonautBuffer.eggDropChance = stegonautCurrent.extraDouble("egg_drop_chance", 0.12D);
        stegonautBuffer.scaleDropChanceBrush = stegonautCurrent.extraDouble("scale_drop_chance_brush",
                stegonautDefaults.extraDouble("scale_drop_chance_brush", 0.30D));
        stegonautBuffer.aggressiveWild = stegonautCurrent.extraBoolean("aggressive_wild", false);

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
        ignivorusBuffer.tamingChanceBase = ignivorusCurrent.extraDouble("taming_chance_base", 14.2857D);
        ignivorusBuffer.tamingChanceBeef = ignivorusCurrent.extraDouble("taming_chance_beef", 20.0D);
        ignivorusBuffer.tamingChanceHearty = ignivorusCurrent.extraDouble("taming_chance_hearty", 25.0);
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
        ignivorusBuffer.scaleDropChanceBrush = ignivorusCurrent.extraDouble("scale_drop_chance_brush",
                ignivorusDefaults.extraDouble("scale_drop_chance_brush", 0.35D));
        ignivorusBuffer.aggressiveWild = ignivorusCurrent.extraBoolean("aggressive_wild", false);

        DragonAttributeConfig volitansCurrent = loader.getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        DragonAttributeConfig volitansDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        VolitansAttributeBuffer volitansBuffer = new VolitansAttributeBuffer();
        volitansBuffer.maxHealth = volitansCurrent.maxHealth();
        volitansBuffer.armor = volitansCurrent.armor();
        volitansBuffer.flyingSpeed = volitansCurrent.flyingSpeed();
        volitansBuffer.wildFlyingSpeedMultiplier = volitansCurrent.extraDouble("wild_flying_speed_multiplier",
                volitansDefaults.extraDouble("wild_flying_speed_multiplier", 1.0D));
        volitansBuffer.biteDamage = volitansCurrent.abilityDamage("bite",
                volitansDefaults.abilityDamage("bite", 12.0D));
        volitansBuffer.clawDamage = volitansCurrent.abilityDamage("claw",
                volitansDefaults.abilityDamage("claw", 11.0D));
        volitansBuffer.hornGoreDamage = volitansCurrent.abilityDamage("horn_gore",
                volitansDefaults.abilityDamage("horn_gore", 15.0D));
        volitansBuffer.roarGroundDamage = volitansCurrent.abilityDamage("roar_ground",
                volitansDefaults.abilityDamage("roar_ground", 10.0D));
        volitansBuffer.roarAirWaterDamage = volitansCurrent.abilityDamage("roar_air_water",
                volitansDefaults.abilityDamage("roar_air_water", 7.0D));
        volitansBuffer.burrowDamage = volitansCurrent.abilityDamage("burrow",
                volitansDefaults.abilityDamage("burrow", 30.0D));
        volitansBuffer.poisonBallDamage = volitansCurrent.abilityDamage("poison_ball",
                volitansDefaults.abilityDamage("poison_ball", 12.0D));
        volitansBuffer.waterBreathDamage = volitansCurrent.abilityDamage("water_breath",
                volitansDefaults.abilityDamage("water_breath", 1.8D));
        volitansBuffer.poisonBreathDamage = volitansCurrent.abilityDamage("poison_breath",
                volitansDefaults.abilityDamage("poison_breath", 1.4D));
        volitansBuffer.tamingChanceBase = volitansCurrent.extraDouble("taming_chance_base",
                volitansDefaults.extraDouble("taming_chance_base", 5.0D));
        volitansBuffer.tamingChanceHearty = volitansCurrent.extraDouble("taming_chance_hearty",
                volitansDefaults.extraDouble("taming_chance_hearty", 3.0D));
        volitansBuffer.tamingStunHealth = volitansCurrent.extraDouble("taming_stun_health",
                volitansDefaults.extraDouble("taming_stun_health", 60.0D));
        volitansBuffer.legacyTaming = volitansCurrent.extraBoolean("legacy_taming", false);
        volitansBuffer.eggHatchChanceNormal = volitansCurrent.extraDouble("egg_hatch_chance_normal", 3.0D);
        volitansBuffer.eggLootShipwreckTreasure = volitansCurrent.extraDouble("egg_loot_shipwreck_treasure",
                volitansDefaults.extraDouble("egg_loot_shipwreck_treasure", 0.12D));
        volitansBuffer.eggDropChance = volitansCurrent.extraDouble("egg_drop_chance", 0.12D);
        volitansBuffer.scaleDropChanceBrush = volitansCurrent.extraDouble("scale_drop_chance_brush",
                volitansDefaults.extraDouble("scale_drop_chance_brush", 0.30D));
        volitansBuffer.spineDropChance = volitansCurrent.extraDouble("spine_drop_chance",
                volitansDefaults.extraDouble("spine_drop_chance", 0.3D));
        volitansBuffer.fishDropChance = volitansCurrent.extraDouble("fish_drop_chance",
                volitansDefaults.extraDouble("fish_drop_chance", 0.40D));
        volitansBuffer.breathActiveTicksMax = volitansCurrent.extraDouble("breath_active_ticks_max",
                volitansDefaults.extraDouble("breath_active_ticks_max", 240.0D));
        volitansBuffer.breathDrainPerTick = volitansCurrent.extraDouble("breath_drain_per_tick",
                volitansDefaults.extraDouble("breath_drain_per_tick", 1.0D / (20.0D * 12.0D)));
        volitansBuffer.breathRegenPerTick = volitansCurrent.extraDouble("breath_regen_per_tick",
                volitansDefaults.extraDouble("breath_regen_per_tick", 0.0025D));
        volitansBuffer.breathProjectileSpread = volitansCurrent.extraDouble("breath_projectile_spread",
                volitansDefaults.extraDouble("breath_projectile_spread", 0.20D));
        volitansBuffer.breathProjectileSpeed = volitansCurrent.extraDouble("breath_projectile_speed",
                volitansDefaults.extraDouble("breath_projectile_speed", 1.60D));
        volitansBuffer.breathProjectileLifetime = volitansCurrent.extraDouble("breath_projectile_lifetime",
                volitansDefaults.extraDouble("breath_projectile_lifetime", 28.0D));
        volitansBuffer.poisonBreathPoisonDurationTicks = volitansCurrent.extraDouble("poison_breath_poison_duration_ticks",
                volitansDefaults.extraDouble("poison_breath_poison_duration_ticks", 80.0D));
        volitansBuffer.poisonBreathPoisonLevel = volitansCurrent.extraDouble("poison_breath_poison_level",
                volitansDefaults.extraDouble("poison_breath_poison_level", 1.0D));
        volitansBuffer.poisonBallPoisonDurationTicks = volitansCurrent.extraDouble("poison_ball_poison_duration_ticks",
                volitansDefaults.extraDouble("poison_ball_poison_duration_ticks", 120.0D));
        volitansBuffer.poisonBallPoisonLevel = volitansCurrent.extraDouble("poison_ball_poison_level",
                volitansDefaults.extraDouble("poison_ball_poison_level", 1.0D));
        volitansBuffer.roarGroundPoisonDurationTicks = volitansCurrent.extraDouble("roar_ground_poison_duration_ticks",
                volitansDefaults.extraDouble("roar_ground_poison_duration_ticks", 1200.0D));
        volitansBuffer.roarGroundPoisonLevel = volitansCurrent.extraDouble("roar_ground_poison_level",
                volitansDefaults.extraDouble("roar_ground_poison_level", 3.0D));
        volitansBuffer.roarAirWaterPoisonDurationTicks = volitansCurrent.extraDouble("roar_air_water_poison_duration_ticks",
                volitansDefaults.extraDouble("roar_air_water_poison_duration_ticks", 200.0D));
        volitansBuffer.roarAirWaterPoisonLevel = volitansCurrent.extraDouble("roar_air_water_poison_level",
                volitansDefaults.extraDouble("roar_air_water_poison_level", 2.0D));
        volitansBuffer.aggressiveWild = volitansCurrent.extraBoolean("aggressive_wild", true);

        DragonAttributeConfig nulljawCurrent = loader.getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        DragonAttributeConfig nulljawDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        NulljawAttributeBuffer nulljawBuffer = new NulljawAttributeBuffer();
        nulljawBuffer.maxHealth = nulljawCurrent.maxHealth();
        nulljawBuffer.armor = nulljawCurrent.armor();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TITLE);
        builder.setTransparentBackground(true);
        builder.setSavingRunnable(() -> {
            holder.save();
            SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.save();
            SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.save();
            SaintsDragonsConfig.BARREL_ROLL_ENABLED.save();
            SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED.save();
            SaintsDragonsConfig.HUNGER_DECAY_ENABLED.save();
            SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED.save();
            SaintsDragonsConfig.IVY_HOUSE_ENABLED.save();
            persistDragonAttributes(cindervaneBuffer, stegonautBuffer, raevyxBuffer, varasuchusBuffer, ignivorusBuffer, volitansBuffer, nulljawBuffer);
            refreshLoadedDragonAttributesOnIntegratedServer();
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

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.varasuchus"),
                () -> config.varasuchusSpawnWeight, value -> config.varasuchusSpawnWeight = value,
                () -> config.varasuchusMinGroupSize, value -> config.varasuchusMinGroupSize = value,
                () -> config.varasuchusMaxGroupSize, value -> config.varasuchusMaxGroupSize = value,
                () -> config.varasuchusAdditionalBiomes, list -> {
                    config.varasuchusAdditionalBiomes.clear();
                    config.varasuchusAdditionalBiomes.addAll(list);
                },
                () -> config.varasuchusExcludedBiomes, list -> {
                    config.varasuchusExcludedBiomes.clear();
                    config.varasuchusExcludedBiomes.addAll(list);
                },
                () -> config.varasuchusEggBlockWorldgen, value -> config.varasuchusEggBlockWorldgen = value, true,
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

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.volitans"),
                () -> config.volitansSpawnWeight, value -> config.volitansSpawnWeight = value,
                () -> config.volitansMinGroupSize, value -> config.volitansMinGroupSize = value,
                () -> config.volitansMaxGroupSize, value -> config.volitansMaxGroupSize = value,
                () -> config.volitansAdditionalBiomes, list -> {
                    config.volitansAdditionalBiomes.clear();
                    config.volitansAdditionalBiomes.addAll(list);
                },
                () -> config.volitansExcludedBiomes, list -> {
                    config.volitansExcludedBiomes.clear();
                    config.volitansExcludedBiomes.addAll(list);
                },
                null, null, false,
                1, 1, 1);

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
                null, null, false,
                20, 1, 2);

        ConfigCategory attributes = builder.getOrCreateCategory(ATTRIBUTES_CATEGORY);
        addCindervaneAttributes(attributes, entryBuilder, cindervaneBuffer, cindervaneDefaults);
        addStegonautAttributes(attributes, entryBuilder, stegonautBuffer, stegonautDefaults);
        addRaevyxAttributes(attributes, entryBuilder, raevyxBuffer, raevyxDefaults);
        addVarasuchusAttributes(attributes, entryBuilder, varasuchusBuffer, varasuchusDefaults);
        addIgnivorusAttributes(attributes, entryBuilder, ignivorusBuffer, ignivorusDefaults);
        addVolitansAttributes(attributes, entryBuilder, volitansBuffer, volitansDefaults);
        addNulljawAttributes(attributes, entryBuilder, nulljawBuffer, nulljawDefaults);

        ConfigCategory others = builder.getOrCreateCategory(OTHERS_CATEGORY);
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.dragon_griefing"),
                SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.dragon_griefing.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.set(value))
         .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.screen_shake"),
                SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.SCREEN_SHAKE_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.screen_shake.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.set(value))
          .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.barrel_roll"),
                SaintsDragonsConfig.BARREL_ROLL_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.BARREL_ROLL_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.barrel_roll.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.BARREL_ROLL_ENABLED.set(value))
          .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.first_person_banking_camera"),
                config.firstPersonBankingCameraEnabled
        ).setDefaultValue(true)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.first_person_banking_camera.tooltip"))
         .setSaveConsumer(value -> config.firstPersonBankingCameraEnabled = value)
          .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.hunger_decay"),
                SaintsDragonsConfig.HUNGER_DECAY_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.HUNGER_DECAY_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.hunger_decay.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.HUNGER_DECAY_ENABLED.set(value))
          .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.happiness_decay"),
                SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.happiness_decay.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED.set(value))
          .build());
        others.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("saintsdragons.config_screen.others.ivy.enabled"),
                SaintsDragonsConfig.IVY_HOUSE_ENABLED.get()
        ).setDefaultValue(SaintsDragonsConfig.IVY_HOUSE_ENABLED_DEFAULT)
         .setTooltip(Component.translatable("saintsdragons.config_screen.others.ivy.enabled.tooltip"))
         .setSaveConsumer(value -> SaintsDragonsConfig.IVY_HOUSE_ENABLED.set(value))
         .build());
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
                .setTooltip(Component.translatable("config.saintsdragons.spawn.weight.tooltip"))
                .setSaveConsumer(weightSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.min_group"), minGetter.getAsInt())
                .setDefaultValue(defaultMin)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("config.saintsdragons.spawn.min_group.tooltip"))
                .setSaveConsumer(minSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.max_group"), maxGetter.getAsInt())
                .setDefaultValue(defaultMax)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("config.saintsdragons.spawn.max_group.tooltip"))
                .setSaveConsumer(maxSetter::accept)
                .build());
        List<String> listCopy = new ArrayList<>(biomesGetter.get());
        entries.add(entryBuilder.startStrList(Component.translatable("config.saintsdragons.spawn.additional_biomes"), listCopy)
                .setDefaultValue(List.of())
                .setTooltip(Component.translatable("config.saintsdragons.spawn.additional_biomes.tooltip"))
                .setSaveConsumer(values -> biomesSetter.accept(new ArrayList<>(values)))
                .build());
        List<String> excludedListCopy = new ArrayList<>(excludedBiomesGetter.get());
        entries.add(entryBuilder.startStrList(Component.translatable("config.saintsdragons.spawn.excluded_biomes"), excludedListCopy)
                .setDefaultValue(List.of())
                .setTooltip(Component.translatable("config.saintsdragons.spawn.excluded_biomes.tooltip"))
                .setSaveConsumer(values -> excludedBiomesSetter.accept(new ArrayList<>(values)))
                .build());
        if (eggBlockWorldgenGetter != null && eggBlockWorldgenSetter != null) {
            entries.add(entryBuilder.startBooleanToggle(
                            Component.translatable("config.saintsdragons.spawn.egg_block_worldgen"),
                            eggBlockWorldgenGetter.getAsBoolean())
                    .setDefaultValue(defaultEggBlockWorldgen)
                    .setTooltip(Component.translatable("config.saintsdragons.spawn.egg_block_worldgen.tooltip"))
                    .setSaveConsumer(eggBlockWorldgenSetter::accept)
                    .build());
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(label, rawEntries).setExpanded(false).build());
    }

    private static AbstractConfigListEntry<Double> buildPercentChanceEntry(ConfigEntryBuilder entryBuilder,
                                                                           Component label,
                                                                           double storedValue,
                                                                           double defaultStoredValue,
                                                                           DoubleConsumer saveConsumer) {
        return entryBuilder.startDoubleField(label, toPercentChance(storedValue))
                .setDefaultValue(toPercentChance(defaultStoredValue))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> saveConsumer.accept(fromPercentChance(value)))
                .build();
    }

    private static double toPercentChance(double storedValue) {
        return clampChance(storedValue) * 100.0D;
    }

    private static double fromPercentChance(double percentValue) {
        return clampChance(percentValue / 100.0D);
    }

    private static double clampChance(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
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
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.rider_flying_speed"), buffer.flyingSpeed)
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
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit1_damage"), buffer.slashGrabHit1Damage)
                .setDefaultValue(defaults.abilityDamage("slash_grab_hit1", 5.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.slashGrabHit1Damage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit2_damage"), buffer.slashGrabHit2Damage)
                .setDefaultValue(defaults.abilityDamage("slash_grab_hit2", 7.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.slashGrabHit2Damage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.volley_damage"), buffer.volleyDamage)
                .setDefaultValue(defaults.abilityDamage("magma_volley", 20.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.volleyDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_damage"), buffer.fireBodyDamage)
                .setDefaultValue(defaults.abilityDamage("fire_body", 3.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.fireBodyDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 25.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.taming_chicken"), buffer.tamingChanceChicken)
                .setDefaultValue(defaults.extraDouble("taming_chance_chicken", 33.3333D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceChicken = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 50.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 2.0))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.cindervane.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.cindervane.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.30D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_explosion_damage"), buffer.fireBodyExplosionDamage)
                .setDefaultValue(defaults.extraDouble("fire_body_explosion_damage", 200.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.fireBodyExplosionDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_self_damage_on_crash"), buffer.fireBodySelfDamageOnCrash)
                .setDefaultValue(defaults.extraDouble("fire_body_self_damage_on_crash", 40.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.fireBodySelfDamageOnCrash = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.cindervane.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
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
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.chin_slam_damage"), buffer.chinSlamDamage)
                .setDefaultValue(defaults.abilityDamage("chin_slam", 8.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.chinSlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_eating_damage"), buffer.groundEatingDamage)
                .setDefaultValue(defaults.abilityDamage("ground_eating", 10.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.groundEatingDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 100.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 100.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 2.0))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.stegonaut.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.stegonaut.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.30D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.stegonaut.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(
                        Component.translatable("saintsdragons.config_screen.others.stegonaut_buffs"),
                        SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED.get())
                .setDefaultValue(SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED_DEFAULT)
                .setTooltip(Component.translatable("saintsdragons.config_screen.others.stegonaut_buffs.tooltip"))
                .setSaveConsumer(value -> SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED.set(value))
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
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.rider_flying_speed"), buffer.flyingSpeed)
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
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.beam_damage"), buffer.beamDamage)
                .setDefaultValue(defaults.abilityDamage("lightning_beam", 35.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.beamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.horn_damage"), buffer.hornDamage)
                .setDefaultValue(defaults.abilityDamage("horn_gore", 15.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.hornDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.dash_damage"), buffer.dashDamage)
                .setDefaultValue(defaults.abilityDamage("dash", 10.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.dashDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 20.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 33.3333D))
                .setMin(0.0D)
                .setMax(100.0D)
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
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_pillager_outpost"),
                buffer.eggLootPillagerOutpost,
                defaults.extraDouble("egg_loot_pillager_outpost", 0.20D),
                value -> buffer.eggLootPillagerOutpost = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_ancient_city"),
                buffer.eggLootAncientCity,
                defaults.extraDouble("egg_loot_ancient_city", 0.15D),
                value -> buffer.eggLootAncientCity = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.raevyx.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.raevyx.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.35D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.raevyx.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.raevyx"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addVarasuchusAttributes(ConfigCategory category,
                                     ConfigEntryBuilder entryBuilder,
                                     VarasuchusAttributeBuffer buffer,
                                     DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.swim_speed"), buffer.swimSpeed)
                .setDefaultValue(defaults.extraDouble("swim_speed", 1.45D))
                .setMin(0.1D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.swimSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.bite_phase1"), buffer.bitePhase1)
                .setDefaultValue(defaults.abilityDamage("bite_phase1", 15.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.bitePhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.bite_phase2"), buffer.bitePhase2)
                .setDefaultValue(defaults.abilityDamage("bite_phase2", 25.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.bitePhase2 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.tail_attack"), buffer.tailAttack)
                .setDefaultValue(defaults.abilityDamage("tail_attack", 7.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.tailAttack = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.dash_tail_swipe"), buffer.dashTailSwipe)
                .setDefaultValue(defaults.abilityDamage("dash_tail_swipe", 10.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.dashTailSwipe = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.dash_claw"), buffer.dashClaw)
                .setDefaultValue(defaults.abilityDamage("dash_claw", 15.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.dashClaw = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.claw_attack"), buffer.clawAttack)
                .setDefaultValue(defaults.abilityDamage("claw_attack", 8.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.clawAttack = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.horn_phase1"), buffer.hornPhase1)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase1", 8.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.hornPhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.horn_phase2"), buffer.hornPhase2)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase2", 15.8D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.hornPhase2 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_chance"), buffer.tamingChance)
                .setDefaultValue(defaults.extraDouble("taming_chance", 16.6667D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChance = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_tropical"), buffer.tamingChanceTropical)
                .setDefaultValue(defaults.extraDouble("taming_chance_tropical", 25.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceTropical = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.varasuchus.legacy_taming"), buffer.legacyTaming)
                .setDefaultValue(defaults.extraBoolean("legacy_taming", false))
                .setTooltip(Component.translatable("config.saintsdragons.attributes.legacy_taming.tooltip"))
                .setSaveConsumer(value -> buffer.legacyTaming = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.varasuchus.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 3.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.varasuchus.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.varasuchus.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.30D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.varasuchus.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.varasuchus"), rawEntries)
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
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.rider_flying_speed"), buffer.flyingSpeed)
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
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.body_slam_damage"), buffer.bodySlamDamage)
                .setDefaultValue(defaults.abilityDamage("body_slam", 40.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.bodySlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.leap_slam_damage"), buffer.leapSlamDamage)
                .setDefaultValue(defaults.abilityDamage("leap_slam", 50.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.leapSlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_damage"), buffer.fireBreathDamage)
                .setDefaultValue(defaults.abilityDamage("fire_breath", 80.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.fireBreathDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fireball_damage"), buffer.fireballDamage)
                .setDefaultValue(defaults.abilityDamage("fireball", 70.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.fireballDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.magma_pillar_damage"), buffer.magmaPillarDamage)
                .setDefaultValue(defaults.abilityDamage("magma_pillar", 18.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.magmaPillarDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.wing_swipe_damage"), buffer.wingSwipeDamage)
                .setDefaultValue(defaults.abilityDamage("wing_swipe", 15.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.wingSwipeDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.stomp_damage"), buffer.stompDamage)
                .setDefaultValue(defaults.abilityDamage("stomp", 18.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.stompDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.bulldoze_damage"), buffer.bulldozeDamage)
                .setDefaultValue(defaults.abilityDamage("bulldoze", 10.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.bulldozeDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_damage"), buffer.ultimateDamage)
                .setDefaultValue(defaults.abilityDamage("ultimate", 200.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.ultimateDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_penalty"), buffer.ultimatePenalty)
                .setDefaultValue(defaults.extraDouble("ultimate_penalty_health", 50.0D))
                .setMin(1.0D)
                .setMax(10000.0D)
                .setSaveConsumer(value -> buffer.ultimatePenalty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 14.2857D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_beef"), buffer.tamingChanceBeef)
                .setDefaultValue(defaults.extraDouble("taming_chance_beef", 20.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBeef = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 25.0D))
                .setMin(0.0D)
                .setMax(100.0D)
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
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_ignite_block_chance"),
                buffer.fireBreathIgniteBlockChance,
                defaults.extraDouble("fire_breath_ignite_block_chance", 1.0D),
                value -> buffer.fireBreathIgniteBlockChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_on_chance"),
                buffer.phase2ToggleOnChance,
                defaults.extraDouble("phase2_toggle_on_chance", 0.85D),
                value -> buffer.phase2ToggleOnChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_off_chance"),
                buffer.phase2ToggleOffChance,
                defaults.extraDouble("phase2_toggle_off_chance", 0.05D),
                value -> buffer.phase2ToggleOffChance = value));
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
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_bastion_treasure"),
                buffer.eggLootBastionTreasure,
                defaults.extraDouble("egg_loot_bastion_treasure", 0.15D),
                value -> buffer.eggLootBastionTreasure = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_nether_bridge"),
                buffer.eggLootNetherBridge,
                defaults.extraDouble("egg_loot_nether_bridge", 0.15D),
                value -> buffer.eggLootNetherBridge = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_ancient_city"),
                buffer.eggLootAncientCity,
                defaults.extraDouble("egg_loot_ancient_city", 0.10D),
                value -> buffer.eggLootAncientCity = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.ignivorus.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.35D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.ignivorus.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", false))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.ignivorus"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addVolitansAttributes(ConfigCategory category,
                                       ConfigEntryBuilder entryBuilder,
                                       VolitansAttributeBuffer buffer,
                                       DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(1.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.rider_flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.0D)
                .setMax(2.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.wild_flying_speed_multiplier"), buffer.wildFlyingSpeedMultiplier)
                .setDefaultValue(defaults.extraDouble("wild_flying_speed_multiplier", 1.0D))
                .setMin(0.05D)
                .setMax(10.0D)
                .setSaveConsumer(value -> buffer.wildFlyingSpeedMultiplier = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 12.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.claw_damage"), buffer.clawDamage)
                .setDefaultValue(defaults.abilityDamage("claw", 11.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.clawDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.horn_gore_damage"), buffer.hornGoreDamage)
                .setDefaultValue(defaults.abilityDamage("horn_gore", 15.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.hornGoreDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_damage"), buffer.roarGroundDamage)
                .setDefaultValue(defaults.abilityDamage("roar_ground", 10.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.roarGroundDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_damage"), buffer.roarAirWaterDamage)
                .setDefaultValue(defaults.abilityDamage("roar_air_water", 7.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.roarAirWaterDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.burrow_damage"), buffer.burrowDamage)
                .setDefaultValue(defaults.abilityDamage("burrow", 30.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.burrowDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_damage"), buffer.poisonBallDamage)
                .setDefaultValue(defaults.abilityDamage("poison_ball", 12.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.poisonBallDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.water_breath_damage"), buffer.waterBreathDamage)
                .setDefaultValue(defaults.abilityDamage("water_breath", 1.8D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.waterBreathDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_damage"), buffer.poisonBreathDamage)
                .setDefaultValue(defaults.abilityDamage("poison_breath", 1.4D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.poisonBreathDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.taming_chance_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 5.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.taming_chance_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 3.0D))
                .setMin(0.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.taming_stun_health"), buffer.tamingStunHealth)
                .setDefaultValue(defaults.extraDouble("taming_stun_health", 60.0D))
                .setMin(0.0D)
                .setMax(100000.0D)
                .setSaveConsumer(value -> buffer.tamingStunHealth = value)
                .build());
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.volitans.legacy_taming"), buffer.legacyTaming)
                .setDefaultValue(defaults.extraBoolean("legacy_taming", false))
                .setTooltip(Component.translatable("config.saintsdragons.attributes.legacy_taming.tooltip"))
                .setSaveConsumer(value -> buffer.legacyTaming = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_active_ticks_max"), buffer.breathActiveTicksMax)
                .setDefaultValue(defaults.extraDouble("breath_active_ticks_max", 240.0D))
                .setMin(1.0D)
                .setMax(24000.0D)
                .setSaveConsumer(value -> buffer.breathActiveTicksMax = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_drain_per_tick"), buffer.breathDrainPerTick)
                .setDefaultValue(defaults.extraDouble("breath_drain_per_tick", 1.0D / (20.0D * 12.0D)))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.breathDrainPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_regen_per_tick"), buffer.breathRegenPerTick)
                .setDefaultValue(defaults.extraDouble("breath_regen_per_tick", 0.0025D))
                .setMin(0.0D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.breathRegenPerTick = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_spread"), buffer.breathProjectileSpread)
                .setDefaultValue(defaults.extraDouble("breath_projectile_spread", 0.20D))
                .setMin(0.0D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.breathProjectileSpread = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_speed"), buffer.breathProjectileSpeed)
                .setDefaultValue(defaults.extraDouble("breath_projectile_speed", 1.60D))
                .setMin(0.0D)
                .setMax(10.0D)
                .setSaveConsumer(value -> buffer.breathProjectileSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_lifetime"), buffer.breathProjectileLifetime)
                .setDefaultValue(defaults.extraDouble("breath_projectile_lifetime", 28.0D))
                .setMin(1.0D)
                .setMax(1200.0D)
                .setSaveConsumer(value -> buffer.breathProjectileLifetime = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_poison_duration_ticks"), buffer.poisonBreathPoisonDurationTicks)
                .setDefaultValue(defaults.extraDouble("poison_breath_poison_duration_ticks", 80.0D))
                .setMin(0.0D)
                .setMax(12000.0D)
                .setSaveConsumer(value -> buffer.poisonBreathPoisonDurationTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_poison_level"), buffer.poisonBreathPoisonLevel)
                .setDefaultValue(defaults.extraDouble("poison_breath_poison_level", 1.0D))
                .setMin(0.0D)
                .setMax(4.0D)
                .setSaveConsumer(value -> buffer.poisonBreathPoisonLevel = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_poison_duration_ticks"), buffer.poisonBallPoisonDurationTicks)
                .setDefaultValue(defaults.extraDouble("poison_ball_poison_duration_ticks", 120.0D))
                .setMin(0.0D)
                .setMax(12000.0D)
                .setSaveConsumer(value -> buffer.poisonBallPoisonDurationTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_poison_level"), buffer.poisonBallPoisonLevel)
                .setDefaultValue(defaults.extraDouble("poison_ball_poison_level", 1.0D))
                .setMin(0.0D)
                .setMax(4.0D)
                .setSaveConsumer(value -> buffer.poisonBallPoisonLevel = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_poison_duration_ticks"), buffer.roarGroundPoisonDurationTicks)
                .setDefaultValue(defaults.extraDouble("roar_ground_poison_duration_ticks", 1200.0D))
                .setMin(0.0D)
                .setMax(12000.0D)
                .setSaveConsumer(value -> buffer.roarGroundPoisonDurationTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_poison_level"), buffer.roarGroundPoisonLevel)
                .setDefaultValue(defaults.extraDouble("roar_ground_poison_level", 3.0D))
                .setMin(0.0D)
                .setMax(4.0D)
                .setSaveConsumer(value -> buffer.roarGroundPoisonLevel = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_poison_duration_ticks"), buffer.roarAirWaterPoisonDurationTicks)
                .setDefaultValue(defaults.extraDouble("roar_air_water_poison_duration_ticks", 200.0D))
                .setMin(0.0D)
                .setMax(12000.0D)
                .setSaveConsumer(value -> buffer.roarAirWaterPoisonDurationTicks = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_poison_level"), buffer.roarAirWaterPoisonLevel)
                .setDefaultValue(defaults.extraDouble("roar_air_water_poison_level", 2.0D))
                .setMin(0.0D)
                .setMax(4.0D)
                .setSaveConsumer(value -> buffer.roarAirWaterPoisonLevel = value)
                .build());
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.volitans.scale_drop_chance_brush"),
                buffer.scaleDropChanceBrush,
                defaults.extraDouble("scale_drop_chance_brush", 0.30D),
                value -> buffer.scaleDropChanceBrush = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.volitans.spine_drop_chance"),
                buffer.spineDropChance,
                defaults.extraDouble("spine_drop_chance", 0.3D),
                value -> buffer.spineDropChance = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.volitans.fish_drop_chance"),
                buffer.fishDropChance,
                defaults.extraDouble("fish_drop_chance", 0.40D),
                value -> buffer.fishDropChance = value));
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.volitans.egg_hatch_chance_normal"), buffer.eggHatchChanceNormal)
                .setDefaultValue(defaults.extraDouble("egg_hatch_chance_normal", 3.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.eggHatchChanceNormal = value)
                .build());
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.volitans.egg_loot_shipwreck_treasure"),
                buffer.eggLootShipwreckTreasure,
                defaults.extraDouble("egg_loot_shipwreck_treasure", 0.12D),
                value -> buffer.eggLootShipwreckTreasure = value));
        entries.add(buildPercentChanceEntry(entryBuilder,
                Component.translatable("config.saintsdragons.attributes.volitans.egg_drop_chance"),
                buffer.eggDropChance,
                defaults.extraDouble("egg_drop_chance", 0.12D),
                value -> buffer.eggDropChance = value));
        entries.add(entryBuilder.startBooleanToggle(Component.translatable("config.saintsdragons.attributes.volitans.aggressive_wild"), buffer.aggressiveWild)
                .setDefaultValue(defaults.extraBoolean("aggressive_wild", true))
                .setSaveConsumer(value -> buffer.aggressiveWild = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.volitans"), rawEntries)
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
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.nulljaw"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void persistDragonAttributes(CindervaneAttributeBuffer cindervaneBuffer,
                                         StegonautAttributeBuffer stegonautBuffer,
                                         RaevyxAttributeBuffer raevyxBuffer,
                                         VarasuchusAttributeBuffer varasuchusBuffer,
                                         IgnivorusAttributeBuffer ignivorusBuffer,
                                         VolitansAttributeBuffer volitansBuffer,
                                         NulljawAttributeBuffer nulljawBuffer) {
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
                        "taming_chance_chicken", cindervaneBuffer.tamingChanceChicken,
                        "taming_chance_hearty", cindervaneBuffer.tamingChanceHearty,
                        "egg_hatch_chance_normal", cindervaneBuffer.eggHatchChanceNormal,
                        "egg_drop_chance", cindervaneBuffer.eggDropChance,
                        "scale_drop_chance_brush", cindervaneBuffer.scaleDropChanceBrush,
                        "fire_body_explosion_damage", cindervaneBuffer.fireBodyExplosionDamage,
                        "fire_body_self_damage_on_crash", cindervaneBuffer.fireBodySelfDamageOnCrash,
                        "wild_flying_speed_multiplier", cindervaneBuffer.wildFlyingSpeedMultiplier
                ),
                Map.of(
                        "aggressive_wild", cindervaneBuffer.aggressiveWild
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
                        "egg_drop_chance", stegonautBuffer.eggDropChance,
                        "scale_drop_chance_brush", stegonautBuffer.scaleDropChanceBrush
                ),
                Map.of(
                        "aggressive_wild", stegonautBuffer.aggressiveWild
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
                        "aggressive_wild", raevyxBuffer.aggressiveWild
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.RAEVYX_ID, updatedRaevyx);

        Map<String, DragonAbilityOverride> varasuchusAbilities = new HashMap<>();
        varasuchusAbilities.put("bite_phase1", DragonAbilityOverride.ofDamage(varasuchusBuffer.bitePhase1));
        varasuchusAbilities.put("bite_phase2", DragonAbilityOverride.ofDamage(varasuchusBuffer.bitePhase2));
        varasuchusAbilities.put("tail_attack", DragonAbilityOverride.ofDamage(varasuchusBuffer.tailAttack));
        varasuchusAbilities.put("dash_tail_swipe", DragonAbilityOverride.ofDamage(varasuchusBuffer.dashTailSwipe));
        varasuchusAbilities.put("dash_claw", DragonAbilityOverride.ofDamage(varasuchusBuffer.dashClaw));
        varasuchusAbilities.put("claw_attack", DragonAbilityOverride.ofDamage(varasuchusBuffer.clawAttack));
        varasuchusAbilities.put("horn_gore_phase1", DragonAbilityOverride.ofDamage(varasuchusBuffer.hornPhase1));
        varasuchusAbilities.put("horn_gore_phase2", DragonAbilityOverride.ofDamage(varasuchusBuffer.hornPhase2));
        DragonAttributeConfig updatedVarasuchus = new DragonAttributeConfig(
                varasuchusBuffer.maxHealth,
                varasuchusBuffer.armor,
                0.0D,
                varasuchusAbilities,
                Map.of(
                        "swim_speed", varasuchusBuffer.swimSpeed,
                        "taming_chance", varasuchusBuffer.tamingChance,
                        "taming_chance_tropical", varasuchusBuffer.tamingChanceTropical,
                        "egg_hatch_chance_normal", varasuchusBuffer.eggHatchChanceNormal,
                        "egg_drop_chance", varasuchusBuffer.eggDropChance,
                        "scale_drop_chance_brush", varasuchusBuffer.scaleDropChanceBrush
                ),
                Map.of(
                        "legacy_taming", varasuchusBuffer.legacyTaming,
                        "aggressive_wild", varasuchusBuffer.aggressiveWild
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.VARASUCHUS_ID, updatedVarasuchus);

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
                        "aggressive_wild", ignivorusBuffer.aggressiveWild
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.IGNIVORUS_ID, updatedIgnivorus);

        DragonAttributeConfig volitansCurrent = loader.getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        Map<String, DragonAbilityOverride> volitansAbilities = new HashMap<>(volitansCurrent.abilities());
        volitansAbilities.put("bite", DragonAbilityOverride.ofDamage(volitansBuffer.biteDamage));
        volitansAbilities.put("claw", DragonAbilityOverride.ofDamage(volitansBuffer.clawDamage));
        volitansAbilities.put("horn_gore", DragonAbilityOverride.ofDamage(volitansBuffer.hornGoreDamage));
        volitansAbilities.put("roar_ground", DragonAbilityOverride.ofDamage(volitansBuffer.roarGroundDamage));
        volitansAbilities.put("roar_air_water", DragonAbilityOverride.ofDamage(volitansBuffer.roarAirWaterDamage));
        volitansAbilities.put("burrow", DragonAbilityOverride.ofDamage(volitansBuffer.burrowDamage));
        volitansAbilities.put("poison_ball", DragonAbilityOverride.ofDamage(volitansBuffer.poisonBallDamage));
        volitansAbilities.put("water_breath", DragonAbilityOverride.ofDamage(volitansBuffer.waterBreathDamage));
        volitansAbilities.put("poison_breath", DragonAbilityOverride.ofDamage(volitansBuffer.poisonBreathDamage));
        DragonAttributeConfig updatedVolitans = new DragonAttributeConfig(
                volitansBuffer.maxHealth,
                volitansBuffer.armor,
                volitansBuffer.flyingSpeed,
                volitansAbilities,
                buildVolitansExtras(volitansBuffer),
                Map.of(
                        "legacy_taming", volitansBuffer.legacyTaming,
                        "aggressive_wild", volitansBuffer.aggressiveWild
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.VOLITANS_ID, updatedVolitans);

        DragonAttributeConfig nulljawCurrent = loader.getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        DragonAttributeConfig updatedNulljaw = new DragonAttributeConfig(
                nulljawBuffer.maxHealth,
                nulljawBuffer.armor,
                nulljawCurrent.flyingSpeed(),
                new HashMap<>(nulljawCurrent.abilities()),
                new HashMap<>(nulljawCurrent.extraDoubles()),
                new HashMap<>(nulljawCurrent.extraBooleans())
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.NULLJAW_ID, updatedNulljaw);
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
        double tamingChanceChicken;
        double tamingChanceHearty;
        double eggHatchChanceNormal;
        double eggDropChance;
        double scaleDropChanceBrush;
        double fireBodyExplosionDamage;
        double fireBodySelfDamageOnCrash;
        double wildFlyingSpeedMultiplier;
        boolean aggressiveWild;
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
        double scaleDropChanceBrush;
        boolean aggressiveWild;
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
        double eggLootAncientCity;
        double eggDropChance;
        double scaleDropChanceBrush;
        boolean aggressiveWild;
    }

    private static final class VarasuchusAttributeBuffer {
        double maxHealth;
        double armor;
        double swimSpeed;
        double bitePhase1;
        double bitePhase2;
        double tailAttack;
        double dashTailSwipe;
        double dashClaw;
        double clawAttack;
        double hornPhase1;
        double hornPhase2;
        double tamingChance;
        double tamingChanceTropical;
        boolean legacyTaming;
        double eggHatchChanceNormal;
        double eggDropChance;
        double scaleDropChanceBrush;
        boolean aggressiveWild;
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
        double tamingChanceBeef;
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
        double scaleDropChanceBrush;
        boolean aggressiveWild;
    }

    private static final class VolitansAttributeBuffer {
        double maxHealth;
        double armor;
        double flyingSpeed;
        double wildFlyingSpeedMultiplier;
        double biteDamage;
        double clawDamage;
        double hornGoreDamage;
        double roarGroundDamage;
        double roarAirWaterDamage;
        double burrowDamage;
        double poisonBallDamage;
        double waterBreathDamage;
        double poisonBreathDamage;
        double tamingChanceBase;
        double tamingChanceHearty;
        double tamingStunHealth;
        boolean legacyTaming;
        double eggHatchChanceNormal;
        double eggLootShipwreckTreasure;
        double eggDropChance;
        double scaleDropChanceBrush;
        double spineDropChance;
        double fishDropChance;
        double breathActiveTicksMax;
        double breathDrainPerTick;
        double breathRegenPerTick;
        double breathProjectileSpread;
        double breathProjectileSpeed;
        double breathProjectileLifetime;
        double poisonBreathPoisonDurationTicks;
        double poisonBreathPoisonLevel;
        double poisonBallPoisonDurationTicks;
        double poisonBallPoisonLevel;
        double roarGroundPoisonDurationTicks;
        double roarGroundPoisonLevel;
        double roarAirWaterPoisonDurationTicks;
        double roarAirWaterPoisonLevel;
        boolean aggressiveWild;
    }

    private static final class NulljawAttributeBuffer {
        double maxHealth;
        double armor;
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
        extras.put("egg_loot_ancient_city", buffer.eggLootAncientCity);
        extras.put("egg_drop_chance", buffer.eggDropChance);
        extras.put("scale_drop_chance_brush", buffer.scaleDropChanceBrush);
        return extras;
    }

    private static Map<String, Double> buildIgnivorusExtras(IgnivorusAttributeBuffer buffer) {
        Map<String, Double> extras = new HashMap<>();
        extras.put("ultimate_penalty_health", buffer.ultimatePenalty);
        extras.put("taming_chance_base", buffer.tamingChanceBase);
        extras.put("taming_chance_beef", buffer.tamingChanceBeef);
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
        extras.put("scale_drop_chance_brush", buffer.scaleDropChanceBrush);
        return extras;
    }

    private static Map<String, Double> buildVolitansExtras(VolitansAttributeBuffer buffer) {
        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", buffer.tamingChanceBase);
        extras.put("taming_chance_hearty", buffer.tamingChanceHearty);
        extras.put("taming_stun_health", buffer.tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", buffer.wildFlyingSpeedMultiplier);
        extras.put("egg_hatch_chance_normal", buffer.eggHatchChanceNormal);
        extras.put("egg_loot_shipwreck_treasure", buffer.eggLootShipwreckTreasure);
        extras.put("egg_drop_chance", buffer.eggDropChance);
        extras.put("scale_drop_chance_brush", buffer.scaleDropChanceBrush);
        extras.put("spine_drop_chance", buffer.spineDropChance);
        extras.put("fish_drop_chance", buffer.fishDropChance);
        extras.put("breath_active_ticks_max", buffer.breathActiveTicksMax);
        extras.put("breath_drain_per_tick", buffer.breathDrainPerTick);
        extras.put("breath_regen_per_tick", buffer.breathRegenPerTick);
        extras.put("breath_projectile_spread", buffer.breathProjectileSpread);
        extras.put("breath_projectile_speed", buffer.breathProjectileSpeed);
        extras.put("breath_projectile_lifetime", buffer.breathProjectileLifetime);
        extras.put("poison_breath_poison_duration_ticks", buffer.poisonBreathPoisonDurationTicks);
        extras.put("poison_breath_poison_level", buffer.poisonBreathPoisonLevel);
        extras.put("poison_ball_poison_duration_ticks", buffer.poisonBallPoisonDurationTicks);
        extras.put("poison_ball_poison_level", buffer.poisonBallPoisonLevel);
        extras.put("roar_ground_poison_duration_ticks", buffer.roarGroundPoisonDurationTicks);
        extras.put("roar_ground_poison_level", buffer.roarGroundPoisonLevel);
        extras.put("roar_air_water_poison_duration_ticks", buffer.roarAirWaterPoisonDurationTicks);
        extras.put("roar_air_water_poison_level", buffer.roarAirWaterPoisonLevel);
        return extras;
    }

    private void refreshLoadedDragonAttributesOnIntegratedServer() {
        var integratedServer = Minecraft.getInstance().getSingleplayerServer();
        if (integratedServer == null) {
            return;
        }

        integratedServer.execute(() -> {
            for (var level : integratedServer.getAllLevels()) {
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof Cindervane dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Stegonaut dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Raevyx dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Varasuchus dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Ignivorus dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Volitans dragon) {
                        dragon.applyConfiguredAttributes();
                    } else if (entity instanceof Nulljaw dragon) {
                        dragon.applyConfiguredAttributes();
                    }
                }
            }
        });
    }
}
