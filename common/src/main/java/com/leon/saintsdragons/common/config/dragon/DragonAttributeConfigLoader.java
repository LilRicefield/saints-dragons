package com.leon.saintsdragons.common.config.dragon;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Datapack-driven loader that exposes dragon attribute overrides via JSON.
 */
public final class DragonAttributeConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    public static final ResourceLocation CINDERVANE_ID = SaintsDragonsCommon.rl("cindervane");
    public static final ResourceLocation RAEVYX_ID = SaintsDragonsCommon.rl("raevyx");
    public static final ResourceLocation VARASUCHUS_ID = SaintsDragonsCommon.rl("varasuchus");
    public static final ResourceLocation IGNIVORUS_ID = SaintsDragonsCommon.rl("ignivorus");
    public static final ResourceLocation STEGONAUT_ID = SaintsDragonsCommon.rl("stegonaut");
    public static final ResourceLocation VOLITANS_ID = SaintsDragonsCommon.rl("volitans");
    public static final ResourceLocation NULLJAW_ID = SaintsDragonsCommon.rl("nulljaw");

    private static final DragonAttributeConfigLoader INSTANCE = new DragonAttributeConfigLoader();
    private static final boolean IS_FORGE = "forge".equals(Services.PLATFORM.getPlatformId());

    private final Map<ResourceLocation, DragonAttributeConfig> defaults;
    private final Path configDirectory;
    private volatile Map<ResourceLocation, DragonAttributeConfig> configs;

    private DragonAttributeConfigLoader() {
        super(GSON, "dragon_attributes");
        this.configDirectory = Services.PLATFORM.getConfigDirectory()
                .resolve(SaintsDragonsCommon.MOD_ID)
                .resolve("server")
                .resolve("dragon_attributes");
        this.defaults = ImmutableMap.copyOf(buildDefaultConfigs());
        this.configs = IS_FORGE ? ImmutableMap.copyOf(buildDefaultConfigs()) : this.defaults;
    }

    private static DragonAttributeConfig cindervaneDefaults() {
        double maxHealth = 80.0D;
        double armor = 4.0D;
        double flyingSpeed = 0.60D;
        double biteDamage = 12.0D;
        double slashGrabHit1Damage = 5.0D;
        double slashGrabHit2Damage = 7.0D;
        double magmaVolleyDamage = 20.0D;
        double fireBodyDamage = 3.0D;
        double tamingChanceBase = 25.0D;
        double tamingChanceChicken = 33.3333D;
        double tamingChanceHearty = 50.0D;
        double eggHatchTimeTicksNormal = 12000.0D;
        double fireBodyExplosionDamage = 200.0D;
        double fireBodySelfDamageOnCrash = 40.0D;
        double wildFlyingSpeedMultiplier = 1.0D;
        boolean aggressiveWild = false;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("CINDERVANE_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("CINDERVANE_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("CINDERVANE_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("CINDERVANE_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_BITE_DAMAGE").get(null));
                slashGrabHit1Damage = (double) configClass.getField("CINDERVANE_SLASH_GRAB_HIT1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_SLASH_GRAB_HIT1_DAMAGE").get(null));
                slashGrabHit2Damage = (double) configClass.getField("CINDERVANE_SLASH_GRAB_HIT2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_SLASH_GRAB_HIT2_DAMAGE").get(null));
                magmaVolleyDamage = (double) configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null));
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
                        "fire_body_explosion_damage", fireBodyExplosionDamage,
                        "fire_body_self_damage_on_crash", fireBodySelfDamageOnCrash,
                        "wild_flying_speed_multiplier", wildFlyingSpeedMultiplier
                ),
                Map.of("aggressive_wild", aggressiveWild)
        );
    }

    private static DragonAttributeConfig raevyxDefaults() {
        double maxHealth = 180.0D;
        double armor = 8.0D;
        double flyingSpeed = 0.5D;
        double biteDamage = 15.0D;
        double lightningBeamDamage = 35.0D;
        double hornGoreDamage = 15.0D;
        double dashDamage = 10.0D;
        double tamingChanceBase = 20.0D;
        double tamingChanceHearty = 33.3333D;
        double beamDrainPerTick = 0.014D;
        double beamRegenPerTick = 0.0025D;
        double summonStormCooldownTicks = 4800.0D;
        double summonStormSuperchargeTicks = 1200.0D;
        double summonStormSuperchargeDamageMultiplier = 2.0D;
        double summonStormDurationTicks = 1200.0D;
        boolean legacyTaming = false;
        double eggHatchTimeTicksNormal = 18000.0D;
        double eggHatchTimeTicksThunder = 9600.0D;
        double tamingStunHealth = maxHealth * (1.0D / 3.0D);
        double wildFlyingSpeedMultiplier = 1.0D;
        boolean diveLoopEnabled = true;
        boolean aggressiveWild = false;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("RAEVYX_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("RAEVYX_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("RAEVYX_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("RAEVYX_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BITE_DAMAGE").get(null));
                lightningBeamDamage = (double) configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null));
                hornGoreDamage = (double) configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null));
                dashDamage = (double) configClass.getField("RAEVYX_DASH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_DASH_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null));
                beamDrainPerTick = (double) configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null));
                beamRegenPerTick = (double) configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null));
                summonStormCooldownTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_COOLDOWN_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_COOLDOWN_TICKS").get(null));
                summonStormSuperchargeTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS").get(null));
                summonStormSuperchargeDamageMultiplier = (double) configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER").get(null));
                summonStormDurationTicks = (double) configClass.getField("RAEVYX_SUMMON_STORM_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_SUMMON_STORM_DURATION_TICKS").get(null));
                legacyTaming = (boolean) configClass.getField("RAEVYX_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LEGACY_TAMING").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL").get(null));
                eggHatchTimeTicksThunder = (double) configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER").get(null));
                tamingStunHealth = (double) configClass.getField("RAEVYX_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_STUN_HEALTH").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("RAEVYX_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                diveLoopEnabled = (boolean) configClass.getField("RAEVYX_DIVE_LOOP_ENABLED").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_DIVE_LOOP_ENABLED").get(null));
                aggressiveWild = (boolean) configClass.getField("RAEVYX_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("beam_drain_per_tick", beamDrainPerTick);
        extras.put("beam_regen_per_tick", beamRegenPerTick);
        extras.put("summon_storm_cooldown_ticks", summonStormCooldownTicks);
        extras.put("summon_storm_supercharge_ticks", summonStormSuperchargeTicks);
        extras.put("summon_storm_supercharge_damage_multiplier", summonStormSuperchargeDamageMultiplier);
        extras.put("summon_storm_duration_ticks", summonStormDurationTicks);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("egg_hatch_time_ticks_thunder", eggHatchTimeTicksThunder);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "lightning_beam", DragonAbilityOverride.ofDamage(lightningBeamDamage),
                        "horn_gore", DragonAbilityOverride.ofDamage(hornGoreDamage),
                        "dash", DragonAbilityOverride.ofDamage(dashDamage)
                ),
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "dive_loop_enabled", diveLoopEnabled,
                        "aggressive_wild", aggressiveWild
                )
        );
    }

    private static DragonAttributeConfig varasuchusDefaults() {
        double maxHealth = 200.0D;
        double armor = 8.0D;
        double bitePhase1Damage = 15.0D;
        double bitePhase2Damage = 25.0D;
        double tailAttackDamage = 7.0D;
        double tailguardParryDamage = 10.0D;
        double dashTailSwipeDamage = 10.0D;
        double dashClawDamage = 15.0D;
        double clawAttackDamage = 8.0D;
        double hornPhase1Damage = 8.0D;
        double hornPhase2Damage = 15.8D;
        double swimSpeed = 1.45D;
        double tamingChance = 16.6667D;
        double tamingChanceTropical = 25.0D;
        boolean legacyTaming = false;
        double eggHatchTimeTicksNormal = 24000.0D;
        boolean aggressiveWild = true;

        if (IS_FORGE) {
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
                        "taming_chance_tropical", tamingChanceTropical,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal
                ),
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }

    private static DragonAttributeConfig ignivorusDefaults() {
        double maxHealth = 450.0D;
        double armor = 4.0D;
        double flyingSpeed = 0.35D;
        double biteDamage = 50.0D;
        double bodySlamDamage = 40.0D;
        double leapSlamDamage = 50.0D;
        double fireBreathDamage = 80.0D;
        double fireballDamage = 70.0D;
        double magmaPillarDamage = 18.0D;
        double wingSwipeDamage = 15.0D;
        double stompDamage = 18.0D;
        double bulldozeDamage = 10.0D;
        double ultimateDamage = 200.0D;
        double ultimatePenaltyHealth = 50.0D;
        double ultimateTriggerHealthFraction = 0.5D;
        double tamingChanceBase = 14.2857D;
        double tamingChanceBeef = 20.0D;
        double tamingChanceHearty = 25.0D;
        boolean legacyTaming = false;
        double fireBreathDrainPerTick = 0.00625D;
        double fireBreathRegenPerTick = 0.0025D;
        double fireBreathFlameSpawnMultiplier = 1.0D;
        double fireBreathFlameSpeedMultiplier = 1.0D;
        double fireBreathFlameLifetimeMultiplier = 1.0D;
        double fireBreathIgniteBlockChance = 1.0D;
        double phase2ToggleOnChance = 0.85D;
        double phase2ToggleOffChance = 0.05D;
        double phase2DecisionMinTicks = 60.0D;
        double phase2DecisionMaxTicks = 120.0D;
        double eggHatchTimeTicksNormal = 36000.0D;
        double tamingStunHealth = maxHealth * (1.0D / 3.0D);
        double wildFlyingSpeedMultiplier = 1.0D;
        boolean aggressiveWild = false;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("IGNIVORUS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("IGNIVORUS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("IGNIVORUS_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null));
                bodySlamDamage = (double) configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null));
                leapSlamDamage = (double) configClass.getField("IGNIVORUS_LEAP_SLAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_LEAP_SLAM_DAMAGE").get(null));
                fireBreathDamage = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null));
                fireballDamage = (double) configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null));
                magmaPillarDamage = (double) configClass.getField("IGNIVORUS_MAGMA_PILLAR_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_MAGMA_PILLAR_DAMAGE").get(null));
                wingSwipeDamage = (double) configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null));
                stompDamage = (double) configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null));
                bulldozeDamage = (double) configClass.getField("IGNIVORUS_BULLDOZE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BULLDOZE_DAMAGE").get(null));
                ultimateDamage = (double) configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null));
                ultimatePenaltyHealth = (double) configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null));
                ultimateTriggerHealthFraction = (double) configClass.getField("IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION").get(null));
                tamingChanceBase = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null));
                tamingChanceBeef = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_BEEF").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_BEEF").get(null));
                tamingChanceHearty = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null));
                legacyTaming = (boolean) configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null));
                fireBreathDrainPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null));
                fireBreathRegenPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null));
                fireBreathFlameSpawnMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER").get(null));
                fireBreathFlameSpeedMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER").get(null));
                fireBreathFlameLifetimeMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER").get(null));
                fireBreathIgniteBlockChance = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE").get(null));
                phase2ToggleOnChance = (double) configClass.getField("IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE").get(null));
                phase2ToggleOffChance = (double) configClass.getField("IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE").get(null));
                phase2DecisionMinTicks = (double) configClass.getField("IGNIVORUS_PHASE2_DECISION_MIN_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_PHASE2_DECISION_MIN_TICKS").get(null));
                phase2DecisionMaxTicks = (double) configClass.getField("IGNIVORUS_PHASE2_DECISION_MAX_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_PHASE2_DECISION_MAX_TICKS").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("IGNIVORUS_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_EGG_HATCH_CHANCE_NORMAL").get(null));
                tamingStunHealth = (double) configClass.getField("IGNIVORUS_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_STUN_HEALTH").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                aggressiveWild = (boolean) configClass.getField("IGNIVORUS_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, Double> extras = new HashMap<>();
        extras.put("ultimate_penalty_health", ultimatePenaltyHealth);
        extras.put("ultimate_trigger_health_fraction", ultimateTriggerHealthFraction);
        extras.put("fire_breath_drain_per_tick", fireBreathDrainPerTick);
        extras.put("fire_breath_regen_per_tick", fireBreathRegenPerTick);
        extras.put("fire_breath_flame_spawn_multiplier", fireBreathFlameSpawnMultiplier);
        extras.put("fire_breath_flame_speed_multiplier", fireBreathFlameSpeedMultiplier);
        extras.put("fire_breath_flame_lifetime_multiplier", fireBreathFlameLifetimeMultiplier);
        extras.put("fire_breath_ignite_block_chance", fireBreathIgniteBlockChance);
        extras.put("phase2_toggle_on_chance", phase2ToggleOnChance);
        extras.put("phase2_toggle_off_chance", phase2ToggleOffChance);
        extras.put("phase2_decision_min_ticks", phase2DecisionMinTicks);
        extras.put("phase2_decision_max_ticks", phase2DecisionMaxTicks);
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_beef", tamingChanceBeef);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "body_slam", DragonAbilityOverride.ofDamage(bodySlamDamage),
                        "leap_slam", DragonAbilityOverride.ofDamage(leapSlamDamage),
                        "fire_breath", DragonAbilityOverride.ofDamage(fireBreathDamage),
                        "fireball", DragonAbilityOverride.ofDamage(fireballDamage),
                        "magma_pillar", DragonAbilityOverride.ofDamage(magmaPillarDamage),
                        "wing_swipe", DragonAbilityOverride.ofDamage(wingSwipeDamage),
                        "stomp", DragonAbilityOverride.ofDamage(stompDamage),
                        "bulldoze", DragonAbilityOverride.ofDamage(bulldozeDamage),
                        "ultimate", DragonAbilityOverride.ofDamage(ultimateDamage)
                ),
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }

    private static DragonAttributeConfig stegonautDefaults() {
        double maxHealth = 100.0D;
        double armor = 15.0D;
        double biteDamage = 5.0D;
        double chinSlamDamage = 8.0D;
        double groundEatingDamage = 10.0D;
        double tamingChanceBase = 100.0D;
        double tamingChanceHearty = 100.0D;
        double eggHatchTimeTicksNormal = 30000.0D;
        boolean aggressiveWild = false;

        if (IS_FORGE) {
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
                        "ground_eating", DragonAbilityOverride.ofDamage(groundEatingDamage)
                ),
                Map.of(
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_hearty", tamingChanceHearty,
                        "egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal
                ),
                Map.of("aggressive_wild", aggressiveWild)
        );
    }

    private static DragonAttributeConfig nulljawDefaults() {
        double maxHealth = 70.0D;
        double armor = 4.0D;
        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("NULLJAW_MAX_HEALTH").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("NULLJAW_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("NULLJAW_ARMOR").get(null).getClass().getMethod("get")
                        .invoke(configClass.getField("NULLJAW_ARMOR").get(null));
            } catch (Exception ignored) {
            }
        }
        return new DragonAttributeConfig(
                maxHealth,
                armor,
                0.42D,
                Map.of(),
                Map.of(
                        "taming_chance_base", 20.0D,
                        "wild_flying_speed_multiplier", 1.0D
                ),
                Map.of(
                        "legacy_taming", true,
                        "aggressive_wild", false
                )
        );
    }

    private static DragonAttributeConfig volitansDefaults() {
        double maxHealth = 160.0D;
        double armor = 6.0D;
        double flyingSpeed = 0.38D;
        double biteDamage = 12.0D;
        double clawDamage = 11.0D;
        double hornGoreDamage = 15.0D;
        double roarGroundDamage = 10.0D;
        double roarAirWaterDamage = 7.0D;
        double burrowDamage = 30.0D;
        double poisonBallDamage = 12.0D;
        double waterBreathDamage = 1.8D;
        double poisonBreathDamage = 1.4D;
        double tamingChanceBase = 20.0D;
        double tamingChanceHearty = 30.0D;
        double tamingStunHealth = 60.0D;
        double eggHatchTimeTicksNormal = 18000.0D;
        double wildFlyingSpeedMultiplier = 1.0D;
        double breathActiveTicksMax = 240.0D;
        double breathDrainPerTick = 1.0D / (20.0D * 12.0D);
        double breathRegenPerTick = 0.0025D;
        double breathProjectileSpread = 0.20D;
        double breathProjectileSpeed = 1.60D;
        double breathProjectileLifetime = 28.0D;
        double poisonBreathPoisonDurationTicks = 80.0D;
        double poisonBreathPoisonLevel = 1.0D;
        double poisonBallPoisonDurationTicks = 120.0D;
        double poisonBallPoisonLevel = 1.0D;
        double roarGroundPoisonDurationTicks = 1200.0D;
        double roarGroundPoisonLevel = 3.0D;
        double roarAirWaterPoisonDurationTicks = 200.0D;
        double roarAirWaterPoisonLevel = 2.0D;
        boolean legacyTaming = false;
        boolean aggressiveWild = true;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("VOLITANS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("VOLITANS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("VOLITANS_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("VOLITANS_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BITE_DAMAGE").get(null));
                clawDamage = (double) configClass.getField("VOLITANS_CLAW_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_CLAW_DAMAGE").get(null));
                hornGoreDamage = (double) configClass.getField("VOLITANS_HORN_GORE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_HORN_GORE_DAMAGE").get(null));
                roarGroundDamage = (double) configClass.getField("VOLITANS_ROAR_GROUND_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_DAMAGE").get(null));
                roarAirWaterDamage = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_DAMAGE").get(null));
                burrowDamage = (double) configClass.getField("VOLITANS_BURROW_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BURROW_DAMAGE").get(null));
                poisonBallDamage = (double) configClass.getField("VOLITANS_POISON_BALL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_DAMAGE").get(null));
                waterBreathDamage = (double) configClass.getField("VOLITANS_WATER_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_WATER_BREATH_DAMAGE").get(null));
                poisonBreathDamage = (double) configClass.getField("VOLITANS_POISON_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("VOLITANS_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("VOLITANS_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_CHANCE_HEARTY").get(null));
                tamingStunHealth = (double) configClass.getField("VOLITANS_TAMING_STUN_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_TAMING_STUN_HEALTH").get(null));
                eggHatchTimeTicksNormal = (double) configClass.getField("VOLITANS_EGG_HATCH_CHANCE_NORMAL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_EGG_HATCH_CHANCE_NORMAL").get(null));
                wildFlyingSpeedMultiplier = (double) configClass.getField("VOLITANS_WILD_FLYING_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_WILD_FLYING_SPEED_MULTIPLIER").get(null));
                breathActiveTicksMax = (double) configClass.getField("VOLITANS_BREATH_ACTIVE_TICKS_MAX").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_ACTIVE_TICKS_MAX").get(null));
                breathDrainPerTick = (double) configClass.getField("VOLITANS_BREATH_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_DRAIN_PER_TICK").get(null));
                breathRegenPerTick = (double) configClass.getField("VOLITANS_BREATH_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_REGEN_PER_TICK").get(null));
                breathProjectileSpread = (double) configClass.getField("VOLITANS_BREATH_PROJECTILE_SPREAD").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_PROJECTILE_SPREAD").get(null));
                breathProjectileSpeed = (double) configClass.getField("VOLITANS_BREATH_PROJECTILE_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_PROJECTILE_SPEED").get(null));
                breathProjectileLifetime = (double) configClass.getField("VOLITANS_BREATH_PROJECTILE_LIFETIME").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_BREATH_PROJECTILE_LIFETIME").get(null));
                poisonBreathPoisonDurationTicks = (double) configClass.getField("VOLITANS_POISON_BREATH_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_POISON_DURATION_TICKS").get(null));
                poisonBreathPoisonLevel = (double) configClass.getField("VOLITANS_POISON_BREATH_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BREATH_POISON_LEVEL").get(null));
                poisonBallPoisonDurationTicks = (double) configClass.getField("VOLITANS_POISON_BALL_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_POISON_DURATION_TICKS").get(null));
                poisonBallPoisonLevel = (double) configClass.getField("VOLITANS_POISON_BALL_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_POISON_BALL_POISON_LEVEL").get(null));
                roarGroundPoisonDurationTicks = (double) configClass.getField("VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS").get(null));
                roarGroundPoisonLevel = (double) configClass.getField("VOLITANS_ROAR_GROUND_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_GROUND_POISON_LEVEL").get(null));
                roarAirWaterPoisonDurationTicks = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS").get(null));
                roarAirWaterPoisonLevel = (double) configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_LEVEL").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_ROAR_AIR_WATER_POISON_LEVEL").get(null));
                legacyTaming = (boolean) configClass.getField("VOLITANS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_LEGACY_TAMING").get(null));
                aggressiveWild = (boolean) configClass.getField("VOLITANS_AGGRESSIVE_WILD").get(null).getClass().getMethod("get").invoke(configClass.getField("VOLITANS_AGGRESSIVE_WILD").get(null));
            } catch (Exception ignored) {
            }
        }

        Map<String, DragonAbilityOverride> abilities = new HashMap<>();
        abilities.put("bite", DragonAbilityOverride.ofDamage(biteDamage));
        abilities.put("claw", DragonAbilityOverride.ofDamage(clawDamage));
        abilities.put("horn_gore", DragonAbilityOverride.ofDamage(hornGoreDamage));
        abilities.put("roar_ground", DragonAbilityOverride.ofDamage(roarGroundDamage));
        abilities.put("roar_air_water", DragonAbilityOverride.ofDamage(roarAirWaterDamage));
        abilities.put("burrow", DragonAbilityOverride.ofDamage(burrowDamage));
        abilities.put("poison_ball", DragonAbilityOverride.ofDamage(poisonBallDamage));
        abilities.put("water_breath", DragonAbilityOverride.ofDamage(waterBreathDamage));
        abilities.put("poison_breath", DragonAbilityOverride.ofDamage(poisonBreathDamage));

        Map<String, Double> extras = new HashMap<>();
        extras.put("taming_chance_base", tamingChanceBase);
        extras.put("taming_chance_hearty", tamingChanceHearty);
        extras.put("taming_stun_health", tamingStunHealth);
        extras.put("egg_hatch_time_ticks_normal", eggHatchTimeTicksNormal);
        extras.put("wild_flying_speed_multiplier", wildFlyingSpeedMultiplier);
        extras.put("breath_active_ticks_max", breathActiveTicksMax);
        extras.put("breath_drain_per_tick", breathDrainPerTick);
        extras.put("breath_regen_per_tick", breathRegenPerTick);
        extras.put("breath_projectile_spread", breathProjectileSpread);
        extras.put("breath_projectile_speed", breathProjectileSpeed);
        extras.put("breath_projectile_lifetime", breathProjectileLifetime);
        extras.put("poison_breath_poison_duration_ticks", poisonBreathPoisonDurationTicks);
        extras.put("poison_breath_poison_level", poisonBreathPoisonLevel);
        extras.put("poison_ball_poison_duration_ticks", poisonBallPoisonDurationTicks);
        extras.put("poison_ball_poison_level", poisonBallPoisonLevel);
        extras.put("roar_ground_poison_duration_ticks", roarGroundPoisonDurationTicks);
        extras.put("roar_ground_poison_level", roarGroundPoisonLevel);
        extras.put("roar_air_water_poison_duration_ticks", roarAirWaterPoisonDurationTicks);
        extras.put("roar_air_water_poison_level", roarAirWaterPoisonLevel);

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                abilities,
                extras,
                Map.of(
                        "legacy_taming", legacyTaming,
                        "aggressive_wild", aggressiveWild
                )
        );
    }

    public static DragonAttributeConfigLoader getInstance() {
        return INSTANCE;
    }

    public static void bootstrap() {
        // Ensures the class is loaded and defaults are ready
        getInstance();
    }

    public DragonAttributeConfig getConfig(ResourceLocation id) {
        if (IS_FORGE) {
            DragonAttributeConfig config = configs.get(id);
            return config != null ? config : DragonAttributeConfig.EMPTY;
        }
        DragonAttributeConfig config = configs.get(id);
        if (config != null) {
            return config;
        }
        DragonAttributeConfig fallback = defaults.get(id);
        return fallback != null ? fallback : DragonAttributeConfig.EMPTY;
    }

    public DragonAttributeConfig getDefaultConfig(ResourceLocation id) {
        if (IS_FORGE) {
            DragonAttributeConfig config = configs.get(id);
            return config != null ? config : DragonAttributeConfig.EMPTY;
        }
        return defaults.getOrDefault(id, DragonAttributeConfig.EMPTY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        if (IS_FORGE) {
            this.configs = ImmutableMap.copyOf(buildDefaultConfigs());
            SaintsDragonsCommon.LOGGER.info("Loaded {} dragon attribute configuration(s) from Forge config",
                    this.configs.size());
            return;
        }
        Map<ResourceLocation, DragonAttributeConfig> merged = new HashMap<>(defaults);
        Map<ResourceLocation, JsonObject> rawJson = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                DragonAttributeConfig fallback = merged.getOrDefault(id, DragonAttributeConfig.EMPTY);
                JsonObject data = GsonHelper.convertToJsonObject(entry.getValue(), id.toString());
                rawJson.put(id, data);
                DragonAttributeConfig parsed = DragonAttributeConfig.merge(data, fallback);
                merged.put(id, parsed);
            } catch (Exception exception) {
                SaintsDragonsCommon.LOGGER.error("Failed to parse dragon attribute config {}", entry.getKey(), exception);
            }
        }

        applyConfigOverrides(merged, rawJson);

        this.configs = ImmutableMap.copyOf(merged);
        SaintsDragonsCommon.LOGGER.info("Loaded {} dragon attribute configuration(s)", this.configs.size());
    }

    private void applyConfigOverrides(Map<ResourceLocation, DragonAttributeConfig> merged,
                                      Map<ResourceLocation, JsonObject> rawJson) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to create dragon attribute config directory {}", configDirectory, e);
        }

        for (Map.Entry<ResourceLocation, DragonAttributeConfig> entry : merged.entrySet()) {
            Path path = configPath(entry.getKey());
            // Always serialize the merged config to ensure all default keys are present
            JsonObject source = serializeConfig(entry.getKey(), entry.getValue());
            ensureLegacyTamingFlag(entry.getKey(), source);

            if (Files.exists(path)) {
                // Backfill important changes when migrating older configs
                backfillIgnivorusFireBreathDamage(path, entry.getKey(), entry.getValue());
                backfillLegacyTaming(path, entry.getKey());
                backfillExtraBooleans(path, entry.getKey());
                backfillRaevyxDiveLoopEnabled(path, entry.getKey(), entry.getValue());
                backfillBeamEnergyTuning(path, entry.getKey(), entry.getValue());
                backfillRaevyxSummonStormTuning(path, entry.getKey(), entry.getValue());
                backfillNormalEggTimerTuning(path, entry.getKey(), entry.getValue());
                backfillRaevyxEggTimerTuning(path, entry.getKey(), entry.getValue());
                backfillTamingStunHealth(path, entry.getKey(), entry.getValue());
                backfillCindervaneFireBodyExplosionDamage(path, entry.getKey(), entry.getValue());
                backfillWildFlyingSpeedMultiplier(path, entry.getKey(), entry.getValue());
                continue;
            }
            if (!source.has("hints")) {
                JsonObject hints = defaultHints(entry.getKey());
                if (hints != null && !hints.entrySet().isEmpty()) {
                    source.add("hints", hints);
                }
            }
            writeConfigFile(path, source);
        }

        for (Map.Entry<ResourceLocation, DragonAttributeConfig> entry : merged.entrySet()) {
            DragonAttributeConfig override = readOverride(entry.getKey(), entry.getValue());
            merged.put(entry.getKey(), override);
        }
    }

    private DragonAttributeConfig readOverride(ResourceLocation id, DragonAttributeConfig fallback) {
        Path path = configPath(id);
        if (!Files.exists(path)) {
            return fallback;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            return DragonAttributeConfig.merge(json, fallback);
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.error("Failed to read dragon attribute config {} from {}", id, path, e);
            return fallback;
        }
    }

    private void writeConfigFile(Path path, JsonObject json) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(GSON.toJson(json));
            }
        } catch (IOException e) {
            SaintsDragonsCommon.LOGGER.error("Failed to write dragon attribute config {}", path, e);
        }
    }

    private Path configPath(ResourceLocation id) {
        return configDirectory.resolve(id.getPath() + ".json");
    }

    private static JsonObject serializeConfig(ResourceLocation id, DragonAttributeConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("max_health", config.maxHealth());
        json.addProperty("armor", config.armor());
        json.addProperty("flying_speed", config.flyingSpeed());

        if (!config.abilities().isEmpty()) {
            JsonObject abilitiesJson = new JsonObject();
            config.abilities().forEach((key, override) -> {
                JsonObject abilityJson = new JsonObject();
                Double damage = override.damage();
                if (damage != null) {
                    abilityJson.addProperty("damage", damage);
                }
                abilitiesJson.add(key, abilityJson);
            });
            json.add("abilities", abilitiesJson);
        }
        JsonObject extraJson = new JsonObject();
        config.extraDoubles().forEach(extraJson::addProperty);
        config.extraBooleans().forEach(extraJson::addProperty);
        // Ensure legacy_taming is present for the three special dragons
        if (requiresLegacyTamingFlag(id) && !extraJson.has("legacy_taming")) {
            extraJson.addProperty("legacy_taming", false);
        }
        if (!extraJson.entrySet().isEmpty()) {
            json.add("extra", extraJson);
        }

        // Friendly hints for players editing the Forge JSON files
        if (!json.has("hints")) {
            JsonObject hints = defaultHints(id);
            if (hints != null && !hints.entrySet().isEmpty()) {
                json.add("hints", hints);
            }
        }

        return json;
    }

    public void overwriteConfig(ResourceLocation id, DragonAttributeConfig config) {
        if (IS_FORGE) {
            return;
        }
        writeConfigFile(configPath(id), serializeConfig(id, config));
        Map<ResourceLocation, DragonAttributeConfig> updated = new HashMap<>(this.configs);
        updated.put(id, config);
        this.configs = ImmutableMap.copyOf(updated);
    }

    public void refreshFromForgeConfig() {
        if (!IS_FORGE) {
            return;
        }
        this.configs = ImmutableMap.copyOf(buildDefaultConfigs());
    }

    private static void ensureLegacyTamingFlag(ResourceLocation id, JsonObject json) {
        if (!requiresLegacyTamingFlag(id)) {
            return;
        }
        boolean changed = false;
        JsonObject extraJson;
        if (json.has("extra")) {
            extraJson = GsonHelper.getAsJsonObject(json, "extra");
        } else {
            extraJson = new JsonObject();
            json.add("extra", extraJson);
            changed = true;
        }
        if (!extraJson.has("legacy_taming")) {
            extraJson.addProperty("legacy_taming", false);
            changed = true;
        }
        if (changed && !json.has("hints")) {
            JsonObject hints = defaultHints(id);
            if (hints != null && !hints.entrySet().isEmpty()) {
                json.add("hints", hints);
            }
        }
    }

    private void backfillLegacyTaming(Path path, ResourceLocation id) {
        if (!requiresLegacyTamingFlag(id)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            boolean needsUpdate = !json.has("extra")
                    || !GsonHelper.getAsJsonObject(json, "extra").has("legacy_taming");
            if (needsUpdate) {
                ensureLegacyTamingFlag(id, json);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill legacy_taming flag for {} at {}", id, path, e);
        }
    }

    private void backfillExtraBooleans(Path path, ResourceLocation id) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            if (!json.has("extra_booleans")) {
                return;
            }
            JsonObject extraJson = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            JsonObject booleansJson = GsonHelper.getAsJsonObject(json, "extra_booleans");
            for (Map.Entry<String, JsonElement> entry : booleansJson.entrySet()) {
                if (!extraJson.has(entry.getKey())) {
                    extraJson.add(entry.getKey(), entry.getValue());
                }
            }
            json.remove("extra_booleans");
            json.add("extra", extraJson);
            writeConfigFile(path, json);
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill extra booleans at {}", path, e);
        }
    }

    private void backfillRaevyxDiveLoopEnabled(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(RAEVYX_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extraJson = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            if (extraJson.has("dive_loop_enabled")) {
                return;
            }
            extraJson.addProperty("dive_loop_enabled", mergedConfig.extraBoolean("dive_loop_enabled", true));
            json.add("extra", extraJson);
            writeConfigFile(path, json);
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill Raevyx dive loop toggle at {}", path, e);
        }
    }

    /**
     * Migration: bump Ignivorus fire_breath damage if the config still has the old 4.0 default
     * or is missing the field entirely. This prevents the Fabric side from sticking to legacy
     * values when the bundled datapack (and Forge) now use 80.0.
     */
    private void backfillIgnivorusFireBreathDamage(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(IGNIVORUS_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject abilities = json.has("abilities") ? GsonHelper.getAsJsonObject(json, "abilities") : new JsonObject();
            JsonObject fireBreath = abilities.has("fire_breath")
                    ? GsonHelper.getAsJsonObject(abilities, "fire_breath")
                    : new JsonObject();

            boolean hasDamage = fireBreath.has("damage");
            double current = hasDamage ? GsonHelper.getAsDouble(fireBreath, "damage") : Double.NaN;
            double newDefault = mergedConfig.abilityDamage("fire_breath", 80.0D);

            // Only update if missing OR stuck on the legacy default (4.0)
            if (!hasDamage || current <= 4.0001D) {
                fireBreath.addProperty("damage", newDefault);
                abilities.add("fire_breath", fireBreath);
                json.add("abilities", abilities);
                writeConfigFile(path, json);
                SaintsDragonsCommon.LOGGER.info("Updated Ignivorus fire_breath damage in {} to {}", path, newDefault);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill ignivorus fire_breath damage at {}", path, e);
        }
    }

    private void backfillBeamEnergyTuning(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        boolean isRaevyx = id.equals(RAEVYX_ID);
        boolean isIgnivorus = id.equals(IGNIVORUS_ID);
        if (!isRaevyx && !isIgnivorus) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;

            if (isRaevyx) {
                if (!extra.has("beam_drain_per_tick")) {
                    extra.addProperty("beam_drain_per_tick",
                            mergedConfig.extraDouble("beam_drain_per_tick", 0.014D));
                    updated = true;
                }
                if (!extra.has("beam_regen_per_tick")) {
                    extra.addProperty("beam_regen_per_tick",
                            mergedConfig.extraDouble("beam_regen_per_tick", 0.0025D));
                    updated = true;
                }
            }

            if (isIgnivorus) {
                if (!extra.has("fire_breath_drain_per_tick")) {
                    extra.addProperty("fire_breath_drain_per_tick",
                            mergedConfig.extraDouble("fire_breath_drain_per_tick", 0.00625D));
                    updated = true;
                }
                if (!extra.has("fire_breath_regen_per_tick")) {
                    extra.addProperty("fire_breath_regen_per_tick",
                            mergedConfig.extraDouble("fire_breath_regen_per_tick", 0.0025D));
                    updated = true;
                }
                if (!extra.has("fire_breath_flame_spawn_multiplier")) {
                    extra.addProperty("fire_breath_flame_spawn_multiplier",
                            mergedConfig.extraDouble("fire_breath_flame_spawn_multiplier", 1.0D));
                    updated = true;
                }
                if (!extra.has("fire_breath_flame_speed_multiplier")) {
                    extra.addProperty("fire_breath_flame_speed_multiplier",
                            mergedConfig.extraDouble("fire_breath_flame_speed_multiplier", 1.0D));
                    updated = true;
                }
                if (!extra.has("fire_breath_flame_lifetime_multiplier")) {
                    extra.addProperty("fire_breath_flame_lifetime_multiplier",
                            mergedConfig.extraDouble("fire_breath_flame_lifetime_multiplier", 1.0D));
                    updated = true;
                }
                if (!extra.has("fire_breath_ignite_block_chance")) {
                    extra.addProperty("fire_breath_ignite_block_chance",
                            mergedConfig.extraDouble("fire_breath_ignite_block_chance", 1.0D));
                    updated = true;
                }
                if (!extra.has("ultimate_trigger_health_fraction")) {
                    extra.addProperty("ultimate_trigger_health_fraction",
                            mergedConfig.extraDouble("ultimate_trigger_health_fraction", 0.5D));
                    updated = true;
                }
                if (!extra.has("phase2_toggle_on_chance")) {
                    extra.addProperty("phase2_toggle_on_chance",
                            mergedConfig.extraDouble("phase2_toggle_on_chance", 0.85D));
                    updated = true;
                }
                if (!extra.has("phase2_toggle_off_chance")) {
                    extra.addProperty("phase2_toggle_off_chance",
                            mergedConfig.extraDouble("phase2_toggle_off_chance", 0.05D));
                    updated = true;
                }
                if (!extra.has("phase2_decision_min_ticks")) {
                    extra.addProperty("phase2_decision_min_ticks",
                            mergedConfig.extraDouble("phase2_decision_min_ticks", 60.0D));
                    updated = true;
                }
                if (!extra.has("phase2_decision_max_ticks")) {
                    extra.addProperty("phase2_decision_max_ticks",
                            mergedConfig.extraDouble("phase2_decision_max_ticks", 120.0D));
                    updated = true;
                }
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill beam/fire energy tuning at {}", path, e);
        }
    }

    private void backfillRaevyxSummonStormTuning(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(RAEVYX_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;

            if (!extra.has("summon_storm_cooldown_ticks")) {
                extra.addProperty("summon_storm_cooldown_ticks",
                        mergedConfig.extraDouble("summon_storm_cooldown_ticks", 4800.0D));
                updated = true;
            }
            if (!extra.has("summon_storm_supercharge_ticks")) {
                extra.addProperty("summon_storm_supercharge_ticks",
                        mergedConfig.extraDouble("summon_storm_supercharge_ticks", 1200.0D));
                updated = true;
            }
            if (!extra.has("summon_storm_supercharge_damage_multiplier")) {
                extra.addProperty("summon_storm_supercharge_damage_multiplier",
                        mergedConfig.extraDouble("summon_storm_supercharge_damage_multiplier", 2.0D));
                updated = true;
            }
            if (!extra.has("summon_storm_duration_ticks")) {
                extra.addProperty("summon_storm_duration_ticks",
                        mergedConfig.extraDouble("summon_storm_duration_ticks", 1200.0D));
                updated = true;
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill raevyx summon storm tuning at {}", path, e);
        }
    }

    private void backfillNormalEggTimerTuning(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        boolean applies = id.equals(CINDERVANE_ID)
                || id.equals(VARASUCHUS_ID)
                || id.equals(IGNIVORUS_ID)
                || id.equals(STEGONAUT_ID)
                || id.equals(VOLITANS_ID);
        if (!applies) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;

            if (!extra.has("egg_hatch_time_ticks_normal")) {
                extra.addProperty("egg_hatch_time_ticks_normal",
                        mergedConfig.extraDouble("egg_hatch_time_ticks_normal", 18000.0D));
                updated = true;
            }
            if (extra.has("egg_hatch_chance_normal")) {
                extra.remove("egg_hatch_chance_normal");
                updated = true;
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill normal egg timer tuning at {}", path, e);
        }
    }

    private void backfillRaevyxEggTimerTuning(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(RAEVYX_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;

            if (!extra.has("egg_hatch_time_ticks_normal")) {
                extra.addProperty("egg_hatch_time_ticks_normal",
                        mergedConfig.extraDouble("egg_hatch_time_ticks_normal", 18000.0D));
                updated = true;
            }
            if (!extra.has("egg_hatch_time_ticks_thunder")) {
                extra.addProperty("egg_hatch_time_ticks_thunder",
                        mergedConfig.extraDouble("egg_hatch_time_ticks_thunder", 9600.0D));
                updated = true;
            }
            if (extra.has("egg_hatch_chance_normal")) {
                extra.remove("egg_hatch_chance_normal");
                updated = true;
            }
            if (extra.has("egg_hatch_chance_thunder")) {
                extra.remove("egg_hatch_chance_thunder");
                updated = true;
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill raevyx egg timer tuning at {}", path, e);
        }
    }

    private void backfillTamingStunHealth(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        boolean isRaevyx = id.equals(RAEVYX_ID);
        boolean isIgnivorus = id.equals(IGNIVORUS_ID);
        if (!isRaevyx && !isIgnivorus) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            if (!extra.has("taming_stun_health")) {
                extra.addProperty("taming_stun_health",
                        mergedConfig.extraDouble("taming_stun_health", mergedConfig.maxHealth() / 3.0D));
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill taming_stun_health at {}", path, e);
        }
    }

    private void backfillCindervaneFireBodyExplosionDamage(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(CINDERVANE_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;
            if (!extra.has("fire_body_explosion_damage")) {
                extra.addProperty("fire_body_explosion_damage",
                        mergedConfig.extraDouble("fire_body_explosion_damage", 200.0D));
                updated = true;
            }
            if (!extra.has("fire_body_self_damage_on_crash")) {
                extra.addProperty("fire_body_self_damage_on_crash",
                        mergedConfig.extraDouble("fire_body_self_damage_on_crash", 40.0D));
                updated = true;
            }
            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill cindervane fire body explosion damage at {}", path, e);
        }
    }

    private void backfillWildFlyingSpeedMultiplier(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        boolean applies = id.equals(CINDERVANE_ID) || id.equals(RAEVYX_ID) || id.equals(IGNIVORUS_ID) || id.equals(VOLITANS_ID) || id.equals(NULLJAW_ID);
        if (!applies) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject json = GsonHelper.convertToJsonObject(element, id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            if (!extra.has("wild_flying_speed_multiplier")) {
                extra.addProperty("wild_flying_speed_multiplier",
                        mergedConfig.extraDouble("wild_flying_speed_multiplier", 1.0D));
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill wild_flying_speed_multiplier at {}", path, e);
        }
    }

    private static boolean requiresLegacyTamingFlag(ResourceLocation id) {
        return id.equals(VARASUCHUS_ID) || id.equals(RAEVYX_ID) || id.equals(IGNIVORUS_ID) || id.equals(VOLITANS_ID) || id.equals(NULLJAW_ID);
    }

    private static Map<ResourceLocation, DragonAttributeConfig> buildDefaultConfigs() {
        Map<ResourceLocation, DragonAttributeConfig> base = new HashMap<>();
        base.put(CINDERVANE_ID, cindervaneDefaults());
        base.put(RAEVYX_ID, raevyxDefaults());
        base.put(VARASUCHUS_ID, varasuchusDefaults());
        base.put(IGNIVORUS_ID, ignivorusDefaults());
        base.put(STEGONAUT_ID, stegonautDefaults());
        base.put(VOLITANS_ID, volitansDefaults());
        base.put(NULLJAW_ID, nulljawDefaults());
        return base;
    }

    private static JsonObject defaultHints(ResourceLocation id) {
        JsonObject hints = new JsonObject();
        // Shared taming guidance
        hints.addProperty("taming_chance_base", "Percent chance per feed, from 0 to 100");
        hints.addProperty("taming_chance_chicken", "Percent chance per feed, from 0 to 100");
        hints.addProperty("taming_chance_beef", "Percent chance per feed, from 0 to 100");
        hints.addProperty("taming_chance_hearty", "Percent chance per feed, from 0 to 100");
        hints.addProperty("taming_chance", "Percent chance base used by Varasuchus taming. Legacy food taming rolls it directly; rodeo taming converts it into a smaller per-tick chance");
        hints.addProperty("taming_chance_tropical", "Percent chance per feed for Varasuchus tropical fish taming, from 0 to 100");
        hints.addProperty("legacy_taming", "true = simple food taming, false = special mechanics (rodeo/low-health)");
        hints.addProperty("egg_hatch_chance_normal", "Legacy random-tick hatch roll. Kept only for older egg configs");
        hints.addProperty("egg_hatch_chance_thunder", "Legacy thunder random-tick hatch roll. Kept only for older egg configs");
        hints.addProperty("egg_hatch_time_ticks_normal", "How long the egg takes to hatch in normal weather (20 ticks = 1 second)");
        hints.addProperty("egg_hatch_time_ticks_thunder", "How long the egg takes to hatch during thunderstorms (20 ticks = 1 second)");
        hints.addProperty("aggressive_wild", "true = wild dragons aggro on sight, false = only retaliate");
        hints.addProperty("taming_stun_health", "Health threshold for taming stun (0 = disable stun)");
        hints.addProperty("flying_speed", "Base rider flying speed. Wild flight uses this too before any wild-only multiplier is applied");
        hints.addProperty("wild_flying_speed_multiplier", "Multiplier for wild flying speed only (1 = default, ridden flight unchanged)");
        hints.addProperty("summon_storm_cooldown_ticks", "Cooldown for Summon Storm (20 ticks = 1 second)");
        hints.addProperty("summon_storm_supercharge_ticks", "How long Summon Storm supercharge lasts (20 ticks = 1 second)");
        hints.addProperty("summon_storm_supercharge_damage_multiplier", "Damage multiplier applied while supercharged (1 = normal damage)");
        hints.addProperty("summon_storm_duration_ticks", "How long thunderstorm weather is enforced (20 ticks = 1 second)");

        if (id.equals(VARASUCHUS_ID)) {
            hints.addProperty("swim_speed", "Min 0.1, Max 5.0");
        } else if (id.equals(CINDERVANE_ID)) {
            hints.addProperty("fire_body_explosion_damage", "Direct blast damage on Fire Body crash impact");
            hints.addProperty("fire_body_self_damage_on_crash", "Self-damage applied to Cindervane after Fire Body crash impact");
        } else if (id.equals(VOLITANS_ID)) {
            hints.addProperty("breath_active_ticks_max", "Maximum active breath duration in ticks before auto-stop (20 ticks = 1 second)");
            hints.addProperty("breath_drain_per_tick", "Breath energy drained per active tick");
            hints.addProperty("breath_regen_per_tick", "Breath energy regenerated per tick while not breathing");
            hints.addProperty("breath_projectile_spread", "Random spread applied to each breath projectile");
            hints.addProperty("breath_projectile_speed", "Base speed multiplier for breath projectiles");
            hints.addProperty("breath_projectile_lifetime", "Lifetime in ticks for each breath projectile");
            hints.addProperty("poison_breath_poison_duration_ticks", "Poison duration from poison breath in ticks (0 = disable)");
            hints.addProperty("poison_breath_poison_level", "Poison level from 0 to 4 applied by poison breath (0 = disable)");
            hints.addProperty("poison_ball_poison_duration_ticks", "Poison duration from poison ball in ticks (0 = disable)");
            hints.addProperty("poison_ball_poison_level", "Poison level from 0 to 4 applied by poison ball (0 = disable)");
            hints.addProperty("roar_ground_poison_duration_ticks", "Poison duration from grounded roar in ticks (0 = disable)");
            hints.addProperty("roar_ground_poison_level", "Poison level from 0 to 4 applied by grounded roar (0 = disable)");
            hints.addProperty("roar_air_water_poison_duration_ticks", "Poison duration from air/water roar in ticks (0 = disable)");
            hints.addProperty("roar_air_water_poison_level", "Poison level from 0 to 4 applied by air/water roar (0 = disable)");
        } else if (id.equals(IGNIVORUS_ID)) {
            hints.addProperty("ultimate_penalty_health", "Typical 1-10000");
            hints.addProperty("ultimate_trigger_health_fraction", "Health fraction from 0 to 1. Example: 0.5 = trigger at 50% max health");
            hints.addProperty("fire_breath_flame_spawn_multiplier", "Multiplier for flame entity count (0 = disable, 1 = default)");
            hints.addProperty("fire_breath_flame_speed_multiplier", "Multiplier for flame projectile speed (1 = default)");
            hints.addProperty("fire_breath_flame_lifetime_multiplier", "Multiplier for flame lifetime ticks (1 = default)");
            hints.addProperty("fire_breath_ignite_block_chance", "Chance from 0 to 1 for igniting blocks");
            hints.addProperty("phase2_toggle_on_chance", "Chance from 0 to 1 to switch from phase 1 to phase 2 when grounded");
            hints.addProperty("phase2_toggle_off_chance", "Chance from 0 to 1 to switch from phase 2 back to phase 1 when grounded");
            hints.addProperty("phase2_decision_min_ticks", "Minimum ticks between phase switch checks (20 ticks = 1 second)");
            hints.addProperty("phase2_decision_max_ticks", "Maximum ticks between phase switch checks (20 ticks = 1 second)");
        }
        return hints;
    }
}
