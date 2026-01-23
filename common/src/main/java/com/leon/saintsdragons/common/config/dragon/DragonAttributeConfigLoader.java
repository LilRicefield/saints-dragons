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
    public static final ResourceLocation NULLJAW_ID = SaintsDragonsCommon.rl("nulljaw");
    public static final ResourceLocation IGNIVORUS_ID = SaintsDragonsCommon.rl("ignivorus");

    private static final DragonAttributeConfigLoader INSTANCE = new DragonAttributeConfigLoader();
    private static final boolean IS_FORGE = Services.PLATFORM.isModLoaded("forge");

    private final Map<ResourceLocation, DragonAttributeConfig> defaults;
    private final Path configDirectory;
    private volatile Map<ResourceLocation, DragonAttributeConfig> configs;

    private DragonAttributeConfigLoader() {
        super(GSON, "dragon_attributes");
        this.configDirectory = Services.PLATFORM.getConfigDirectory()
                .resolve(SaintsDragonsCommon.MOD_ID)
                .resolve("dragon_attributes");
        this.defaults = ImmutableMap.copyOf(buildDefaultConfigs());
        this.configs = IS_FORGE ? ImmutableMap.copyOf(buildDefaultConfigs()) : this.defaults;
    }

    private static DragonAttributeConfig cindervaneDefaults() {
        double maxHealth = 80.0D;
        double armor = 4.0D;
        double flyingSpeed = 0.60D;
        double biteDamage = 12.0D;
        double magmaVolleyDamage = 20.0D;
        double tamingChanceBase = 4.0D;
        double tamingChanceHearty = 2.0D;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("CINDERVANE_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("CINDERVANE_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("CINDERVANE_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("CINDERVANE_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_BITE_DAMAGE").get(null));
                magmaVolleyDamage = (double) configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_MAGMA_VOLLEY_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("CINDERVANE_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("CINDERVANE_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("CINDERVANE_TAMING_CHANCE_HEARTY").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "magma_volley", DragonAbilityOverride.ofDamage(magmaVolleyDamage)
                ),
                Map.of(
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_hearty", tamingChanceHearty
                ),
                Map.of()
        );
    }

    private static DragonAttributeConfig raevyxDefaults() {
        double maxHealth = 180.0D;
        double armor = 8.0D;
        double flyingSpeed = 1.0D;
        double biteDamage = 15.0D;
        double lightningBeamDamage = 35.0D;
        double hornGoreDamage = 15.0D;
        double tamingChanceBase = 5.0D;
        double tamingChanceHearty = 3.0D;
        double beamDrainPerTick = 0.014D;
        double beamRegenPerTick = 0.0025D;
        boolean legacyTaming = false;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("RAEVYX_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("RAEVYX_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("RAEVYX_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("RAEVYX_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BITE_DAMAGE").get(null));
                lightningBeamDamage = (double) configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LIGHTNING_BEAM_DAMAGE").get(null));
                hornGoreDamage = (double) configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_HORN_GORE_DAMAGE").get(null));
                tamingChanceBase = (double) configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_TAMING_CHANCE_HEARTY").get(null));
                beamDrainPerTick = (double) configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_DRAIN_PER_TICK").get(null));
                beamRegenPerTick = (double) configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_BEAM_REGEN_PER_TICK").get(null));
                legacyTaming = (boolean) configClass.getField("RAEVYX_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("RAEVYX_LEGACY_TAMING").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "lightning_beam", DragonAbilityOverride.ofDamage(lightningBeamDamage),
                        "horn_gore", DragonAbilityOverride.ofDamage(hornGoreDamage)
                ),
                Map.of(
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_hearty", tamingChanceHearty,
                        "beam_drain_per_tick", beamDrainPerTick,
                        "beam_regen_per_tick", beamRegenPerTick
                ),
                Map.of(
                        "legacy_taming", legacyTaming
                )
        );
    }

    private static DragonAttributeConfig nulljawDefaults() {
        double maxHealth = 250.0D;
        double armor = 8.0D;
        double bitePhase1Damage = 40.0D;
        double bitePhase2Damage = 50.0D;
        double hornPhase1Damage = 16.0D;
        double hornPhase2Damage = 20.8D;
        double swimSpeed = 1.45D;
        double tamingChance = 6.0D;
        boolean legacyTaming = false;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("NULLJAW_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("NULLJAW_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_ARMOR").get(null));
                bitePhase1Damage = (double) configClass.getField("NULLJAW_BITE_PHASE1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_BITE_PHASE1_DAMAGE").get(null));
                bitePhase2Damage = (double) configClass.getField("NULLJAW_BITE_PHASE2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_BITE_PHASE2_DAMAGE").get(null));
                hornPhase1Damage = (double) configClass.getField("NULLJAW_HORN_GORE_PHASE1_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_HORN_GORE_PHASE1_DAMAGE").get(null));
                hornPhase2Damage = (double) configClass.getField("NULLJAW_HORN_GORE_PHASE2_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_HORN_GORE_PHASE2_DAMAGE").get(null));
                swimSpeed = (double) configClass.getField("NULLJAW_SWIM_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_SWIM_SPEED").get(null));
                tamingChance = (double) configClass.getField("NULLJAW_TAMING_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_TAMING_CHANCE").get(null));
                legacyTaming = (boolean) configClass.getField("NULLJAW_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("NULLJAW_LEGACY_TAMING").get(null));
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
                        "horn_gore_phase1", DragonAbilityOverride.ofDamage(hornPhase1Damage),
                        "horn_gore_phase2", DragonAbilityOverride.ofDamage(hornPhase2Damage)
                ),
                Map.of(
                        "swim_speed", swimSpeed,
                        "taming_chance", tamingChance
                ),
                Map.of(
                        "legacy_taming", legacyTaming
                )
        );
    }

    private static DragonAttributeConfig ignivorusDefaults() {
        double maxHealth = 300.0D;
        double armor = 4.0D;
        double flyingSpeed = 0.40D;
        double biteDamage = 50.0D;
        double bodySlamDamage = 40.0D;
        double fireBreathDamage = 80.0D;
        double fireballDamage = 70.0D;
        double wingSwipeDamage = 15.0D;
        double stompDamage = 18.0D;
        double ultimateDamage = 200.0D;
        double ultimatePenaltyHealth = 50.0D;
        double tamingChanceBase = 7.0D;
        double tamingChanceHearty = 4.0D;
        boolean legacyTaming = false;
        double fireBreathDrainPerTick = 0.00625D;
        double fireBreathRegenPerTick = 0.0025D;
        double fireBreathFlameSpawnMultiplier = 1.0D;
        double fireBreathFlameSpeedMultiplier = 1.0D;
        double fireBreathFlameLifetimeMultiplier = 1.0D;
        double fireBreathIgniteBlockChance = 1.0D;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                maxHealth = (double) configClass.getField("IGNIVORUS_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_MAX_HEALTH").get(null));
                armor = (double) configClass.getField("IGNIVORUS_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ARMOR").get(null));
                flyingSpeed = (double) configClass.getField("IGNIVORUS_FLYING_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FLYING_SPEED").get(null));
                biteDamage = (double) configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BITE_DAMAGE").get(null));
                bodySlamDamage = (double) configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_BODY_SLAM_DAMAGE").get(null));
                fireBreathDamage = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DAMAGE").get(null));
                fireballDamage = (double) configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIREBALL_DAMAGE").get(null));
                wingSwipeDamage = (double) configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_WING_SWIPE_DAMAGE").get(null));
                stompDamage = (double) configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_STOMP_DAMAGE").get(null));
                ultimateDamage = (double) configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_DAMAGE").get(null));
                ultimatePenaltyHealth = (double) configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_ULTIMATE_PENALTY_HEALTH").get(null));
                tamingChanceBase = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_BASE").get(null));
                tamingChanceHearty = (double) configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_TAMING_CHANCE_HEARTY").get(null));
                legacyTaming = (boolean) configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_LEGACY_TAMING").get(null));
                fireBreathDrainPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK").get(null));
                fireBreathRegenPerTick = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK").get(null));
                fireBreathFlameSpawnMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER").get(null));
                fireBreathFlameSpeedMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER").get(null));
                fireBreathFlameLifetimeMultiplier = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER").get(null));
                fireBreathIgniteBlockChance = (double) configClass.getField("IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE").get(null).getClass().getMethod("get").invoke(configClass.getField("IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                maxHealth,
                armor,
                flyingSpeed,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(biteDamage),
                        "body_slam", DragonAbilityOverride.ofDamage(bodySlamDamage),
                        "fire_breath", DragonAbilityOverride.ofDamage(fireBreathDamage),
                        "fireball", DragonAbilityOverride.ofDamage(fireballDamage),
                        "wing_swipe", DragonAbilityOverride.ofDamage(wingSwipeDamage),
                        "stomp", DragonAbilityOverride.ofDamage(stompDamage),
                        "ultimate", DragonAbilityOverride.ofDamage(ultimateDamage)
                ),
                Map.of(
                        "ultimate_penalty_health", ultimatePenaltyHealth,
                        "fire_breath_drain_per_tick", fireBreathDrainPerTick,
                        "fire_breath_regen_per_tick", fireBreathRegenPerTick,
                        "fire_breath_flame_spawn_multiplier", fireBreathFlameSpawnMultiplier,
                        "fire_breath_flame_speed_multiplier", fireBreathFlameSpeedMultiplier,
                        "fire_breath_flame_lifetime_multiplier", fireBreathFlameLifetimeMultiplier,
                        "fire_breath_ignite_block_chance", fireBreathIgniteBlockChance,
                        "taming_chance_base", tamingChanceBase,
                        "taming_chance_hearty", tamingChanceHearty
                ),
                Map.of(
                        "legacy_taming", legacyTaming
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
                backfillBeamEnergyTuning(path, entry.getKey(), entry.getValue());
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
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill beam/fire energy tuning at {}", path, e);
        }
    }

    private static boolean requiresLegacyTamingFlag(ResourceLocation id) {
        return id.equals(NULLJAW_ID) || id.equals(RAEVYX_ID) || id.equals(IGNIVORUS_ID);
    }

    private static Map<ResourceLocation, DragonAttributeConfig> buildDefaultConfigs() {
        Map<ResourceLocation, DragonAttributeConfig> base = new HashMap<>();
        base.put(CINDERVANE_ID, cindervaneDefaults());
        base.put(RAEVYX_ID, raevyxDefaults());
        base.put(NULLJAW_ID, nulljawDefaults());
        base.put(IGNIVORUS_ID, ignivorusDefaults());
        return base;
    }

    private static JsonObject defaultHints(ResourceLocation id) {
        JsonObject hints = new JsonObject();
        // Shared taming guidance
        hints.addProperty("taming_chance_base", "Lower is easier: 1 = 100% per feed, 100 = 1% per feed");
        hints.addProperty("taming_chance_hearty", "Lower is easier: 1 = 100% per feed, 100 = 1% per feed");
        hints.addProperty("taming_chance", "Lower is easier: 1 = 100% per attempt, 100 = 1% per attempt");
        hints.addProperty("legacy_taming", "true = simple food taming, false = special mechanics (rodeo/low-health)");

        if (id.equals(NULLJAW_ID)) {
            hints.addProperty("swim_speed", "Min 0.1, Max 5.0");
        } else if (id.equals(IGNIVORUS_ID)) {
            hints.addProperty("ultimate_penalty_health", "Typical 1-500");
            hints.addProperty("fire_breath_flame_spawn_multiplier", "0 = disable flame entities, 1 = default");
            hints.addProperty("fire_breath_flame_speed_multiplier", "Scales flame projectile speed (1 = default)");
            hints.addProperty("fire_breath_flame_lifetime_multiplier", "Scales flame lifetime ticks (1 = default)");
            hints.addProperty("fire_breath_ignite_block_chance", "0 = never ignite, 1 = always ignite");
        }
        return hints;
    }
}
