package com.leon.saintsdragons.common.config.dragon;

import com.leon.saintsdragons.common.config.dragon.profile.CindervaneStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.VarasuchusStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.StegonautStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.NulljawStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.AtroxiiaStatProfile;
import com.leon.saintsdragons.common.config.dragon.profile.VolitansStatProfile;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.ConfigStorageLayout;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
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
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public static final ResourceLocation ATROXIIA_ID = SaintsDragonsCommon.rl("atroxiia");
    public static final ResourceLocation DRACONIAN_SWARM_ID = SaintsDragonsCommon.rl("draconian_swarm");
    public static final int SWARM_WAVE_MIN_COUNT = 1;
    public static final int SWARM_WAVE_MAX_COUNT = 50;
    public static final int SWARM_WAVE_1_DEFAULT_COUNT = 6;
    public static final int SWARM_WAVE_2_DEFAULT_COUNT = 9;
    public static final int SWARM_WAVE_3_DEFAULT_COUNT = 12;

    private static final DragonAttributeConfigLoader INSTANCE = new DragonAttributeConfigLoader();
    private static final boolean IS_FORGE = "forge".equals(Services.PLATFORM.getPlatformId());

    private final Map<ResourceLocation, DragonAttributeConfig> defaults;
    private final Path configDirectory;
    private volatile Map<ResourceLocation, DragonAttributeConfig> configs;

    private DragonAttributeConfigLoader() {
        super(GSON, "dragon_attributes");
        ConfigStorageLayout.migrateLegacyFiles();
        this.configDirectory = Services.PLATFORM.getConfigDirectory()
                .resolve(SaintsDragonsConfig.DRAGON_ATTRIBUTES_CONFIG_FOLDER);
        this.defaults = ImmutableMap.copyOf(buildDefaultConfigs());
        this.configs = IS_FORGE ? ImmutableMap.copyOf(buildDefaultConfigs()) : this.defaults;
    }

    private static DragonAttributeConfig cindervaneDefaults() {
        return CindervaneStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig raevyxDefaults() {
        return RaevyxStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig varasuchusDefaults() {
        return VarasuchusStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig ignivorusDefaults() {
        return IgnivorusStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig stegonautDefaults() {
        return StegonautStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig nulljawDefaults() {
        return NulljawStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig atroxiiaDefaults() {
        return AtroxiiaStatProfile.defaults(IS_FORGE);
    }

    private static DragonAttributeConfig draconianSwarmDefaults() {
        int wave1Count = SWARM_WAVE_1_DEFAULT_COUNT;
        int wave2Count = SWARM_WAVE_2_DEFAULT_COUNT;
        int wave3Count = SWARM_WAVE_3_DEFAULT_COUNT;
        double latcherMaxHealth = 12.0D;
        double latcherArmor = 0.0D;
        double latcherChaseSpeed = 0.80D;
        double latcherBiteDamage = 4.0D;
        double wingedMaxHealth = 6.0D;
        double wingedArmor = 0.0D;
        double wingedChaseSpeed = 1.0D;
        double wingedHookAndPullDamage = 1.5D;
        double wingedDiveBombDamage = 2.025D;
        double whettledMaxHealth = 16.0D;
        double whettledArmor = 0.0D;
        double whettledChaseSpeed = 0.90D;
        double whettledClawAttackDamage = 4.0D;
        double whettledLungeDamage = 9.0D;

        if (IS_FORGE) {
            try {
                Class<?> configClass = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
                wave1Count = ((Number) configClass.getField("SWARM_WAVE_1_COUNT").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WAVE_1_COUNT").get(null))).intValue();
                wave2Count = ((Number) configClass.getField("SWARM_WAVE_2_COUNT").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WAVE_2_COUNT").get(null))).intValue();
                wave3Count = ((Number) configClass.getField("SWARM_WAVE_3_COUNT").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WAVE_3_COUNT").get(null))).intValue();
                latcherMaxHealth = (double) configClass.getField("SWARM_LATCHER_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_LATCHER_MAX_HEALTH").get(null));
                latcherArmor = (double) configClass.getField("SWARM_LATCHER_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_LATCHER_ARMOR").get(null));
                latcherChaseSpeed = (double) configClass.getField("SWARM_LATCHER_CHASE_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_LATCHER_CHASE_SPEED").get(null));
                latcherBiteDamage = (double) configClass.getField("SWARM_LATCHER_BITE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_LATCHER_BITE_DAMAGE").get(null));
                wingedMaxHealth = (double) configClass.getField("SWARM_WINGED_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WINGED_MAX_HEALTH").get(null));
                wingedArmor = (double) configClass.getField("SWARM_WINGED_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WINGED_ARMOR").get(null));
                wingedChaseSpeed = (double) configClass.getField("SWARM_WINGED_CHASE_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WINGED_CHASE_SPEED").get(null));
                wingedHookAndPullDamage = (double) configClass.getField("SWARM_WINGED_HOOK_AND_PULL_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WINGED_HOOK_AND_PULL_DAMAGE").get(null));
                wingedDiveBombDamage = (double) configClass.getField("SWARM_WINGED_DIVE_BOMB_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WINGED_DIVE_BOMB_DAMAGE").get(null));
                whettledMaxHealth = (double) configClass.getField("SWARM_WHETTLED_MAX_HEALTH").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WHETTLED_MAX_HEALTH").get(null));
                whettledArmor = (double) configClass.getField("SWARM_WHETTLED_ARMOR").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WHETTLED_ARMOR").get(null));
                whettledChaseSpeed = (double) configClass.getField("SWARM_WHETTLED_CHASE_SPEED").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WHETTLED_CHASE_SPEED").get(null));
                whettledClawAttackDamage = (double) configClass.getField("SWARM_WHETTLED_CLAW_ATTACK_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WHETTLED_CLAW_ATTACK_DAMAGE").get(null));
                whettledLungeDamage = (double) configClass.getField("SWARM_WHETTLED_LUNGE_DAMAGE").get(null).getClass().getMethod("get").invoke(configClass.getField("SWARM_WHETTLED_LUNGE_DAMAGE").get(null));
            } catch (Exception ignored) {
            }
        }

        return new DragonAttributeConfig(
                latcherMaxHealth,
                latcherArmor,
                0.0D,
                Map.of(
                        "latcher_bite", DragonAbilityOverride.ofDamage(latcherBiteDamage),
                        "winged_attack", DragonAbilityOverride.ofDamage(wingedHookAndPullDamage),
                        "winged_attack2", DragonAbilityOverride.ofDamage(wingedDiveBombDamage),
                        "whettled_clawattack", DragonAbilityOverride.ofDamage(whettledClawAttackDamage),
                        "whettled_movehornattack", DragonAbilityOverride.ofDamage(whettledLungeDamage)
                ),
                Map.ofEntries(
                        Map.entry("wave_1_count", (double) clampSwarmWaveCount(wave1Count)),
                        Map.entry("wave_2_count", (double) clampSwarmWaveCount(wave2Count)),
                        Map.entry("wave_3_count", (double) clampSwarmWaveCount(wave3Count)),
                        Map.entry("latcher_max_health", latcherMaxHealth),
                        Map.entry("latcher_armor", latcherArmor),
                        Map.entry("latcher_chase_speed", latcherChaseSpeed),
                        Map.entry("winged_max_health", wingedMaxHealth),
                        Map.entry("winged_armor", wingedArmor),
                        Map.entry("winged_chase_speed", wingedChaseSpeed),
                        Map.entry("whettled_max_health", whettledMaxHealth),
                        Map.entry("whettled_armor", whettledArmor),
                        Map.entry("whettled_chase_speed", whettledChaseSpeed)
                ),
                Map.of()
        );
    }

    public static int clampSwarmWaveCount(int count) {
        return Math.max(SWARM_WAVE_MIN_COUNT, Math.min(SWARM_WAVE_MAX_COUNT, count));
    }

    public static int swarmWaveCount(DragonAttributeConfig config, int wave) {
        int fallback = switch (wave) {
            case 1 -> SWARM_WAVE_1_DEFAULT_COUNT;
            case 2 -> SWARM_WAVE_2_DEFAULT_COUNT;
            case 3 -> SWARM_WAVE_3_DEFAULT_COUNT;
            default -> SWARM_WAVE_1_DEFAULT_COUNT;
        };
        return clampSwarmWaveCount((int) Math.round(config.extraDouble("wave_" + wave + "_count", fallback)));
    }

    private static DragonAttributeConfig volitansDefaults() {
        return VolitansStatProfile.defaults(IS_FORGE);
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
                removeRetiredVolitansBreathFields(id, data);
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
                removeHints(path, entry.getKey());
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
                backfillPreyTamingChances(path, entry.getKey(), entry.getValue());
                backfillAtroxiiaAbilityTuning(path, entry.getKey(), entry.getValue());
                backfillCindervaneAbilityTuning(path, entry.getKey(), entry.getValue());
                backfillWildFlyingSpeedMultiplier(path, entry.getKey(), entry.getValue());
                migrateVolitansExtras(path, entry.getKey(), entry.getValue());
                continue;
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
            removeRetiredVolitansBreathFields(id, json);
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
                if (override.knockback() != null) {
                    abilityJson.addProperty("knockback", override.knockback());
                }
                if (override.secondaryKnockback() != null) {
                    abilityJson.addProperty("secondary_knockback", override.secondaryKnockback());
                }
                if (override.stunDurationTicks() != null) {
                    abilityJson.addProperty("stun_duration_ticks", override.stunDurationTicks());
                }
                if (override.enabled() != null) {
                    abilityJson.addProperty("enabled", override.enabled());
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

        removeRetiredVolitansBreathFields(id, json);
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
        JsonObject extraJson;
        if (json.has("extra")) {
            extraJson = GsonHelper.getAsJsonObject(json, "extra");
        } else {
            extraJson = new JsonObject();
            json.add("extra", extraJson);
        }
        if (!extraJson.has("legacy_taming")) {
            extraJson.addProperty("legacy_taming", false);
        }
    }

    private void removeHints(Path path, ResourceLocation id) {
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            json = GsonHelper.convertToJsonObject(element, id.toString());
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to remove hints from dragon attribute config {} at {}", id, path, e);
            return;
        }
        if (json.remove("hints") != null) {
            writeConfigFile(path, json);
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
                if (extra.has("fire_breath_drain_per_tick")
                        && Math.abs(extra.get("fire_breath_drain_per_tick").getAsDouble() - 0.00625D) < 1.0E-10D) {
                    extra.addProperty("fire_breath_drain_per_tick", 1.0D / 240.0D);
                    updated = true;
                }
                if (!extra.has("fire_breath_drain_per_tick")) {
                    extra.addProperty("fire_breath_drain_per_tick",
                            mergedConfig.extraDouble("fire_breath_drain_per_tick", 0.004166666666666667D));
                    updated = true;
                }
                if (!extra.has("fire_breath_regen_per_tick")) {
                    extra.addProperty("fire_breath_regen_per_tick",
                            mergedConfig.extraDouble("fire_breath_regen_per_tick", 0.0025D));
                    updated = true;
                }
                if (extra.remove("fire_breath_flame_spawn_multiplier") != null) {
                    updated = true;
                }
                if (extra.remove("fire_breath_flame_speed_multiplier") != null) {
                    updated = true;
                }
                if (extra.remove("fire_breath_flame_lifetime_multiplier") != null) {
                    updated = true;
                }
                if (extra.remove("fire_breath_ignite_block_chance") != null) {
                    updated = true;
                }
                if (!extra.has("ultimate_trigger_health_fraction")) {
                    extra.addProperty("ultimate_trigger_health_fraction",
                            mergedConfig.extraDouble("ultimate_trigger_health_fraction", 0.6D));
                    updated = true;
                }
                if (extra.remove("phase2_toggle_on_chance") != null) {
                    updated = true;
                }
                if (extra.remove("phase2_toggle_off_chance") != null) {
                    updated = true;
                }
                if (extra.remove("phase2_decision_min_ticks") != null) {
                    updated = true;
                }
                if (extra.remove("phase2_decision_max_ticks") != null) {
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
                || id.equals(ATROXIIA_ID)
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
        boolean supportsTamingStun = id.equals(RAEVYX_ID)
                || id.equals(IGNIVORUS_ID)
                || id.equals(ATROXIIA_ID);
        if (!supportsTamingStun) {
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

    private void backfillCindervaneAbilityTuning(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
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
            if (!extra.has("magma_volley_cooldown_seconds")) {
                extra.addProperty("magma_volley_cooldown_seconds",
                        mergedConfig.extraDouble("magma_volley_cooldown_seconds", 20.0D));
                updated = true;
            }
            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill cindervane ability tuning at {}", path, e);
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

    private static boolean removeRetiredVolitansBreathFields(ResourceLocation id, JsonObject json) {
        if (!id.equals(VOLITANS_ID) || !json.has("extra")) return false;
        JsonObject extra = GsonHelper.getAsJsonObject(json, "extra");
        boolean changed = false;
        for (String key : List.of("breath_projectile_speed", "breath_projectile_lifetime", "breath_projectile_spread")) {
            changed |= extra.remove(key) != null;
        }
        return changed;
    }

    private void migrateVolitansExtras(Path path, ResourceLocation id, DragonAttributeConfig defaults) {
        if (!id.equals(VOLITANS_ID)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean changed = removeRetiredVolitansBreathFields(id, json);
            if (!extra.has("rider_swim_speed")) {
                extra.addProperty("rider_swim_speed", defaults.extraDouble("rider_swim_speed", 1.42D));
                changed = true;
            }
            if (changed) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to migrate Volitans extras at {}", path, e);
        }
    }

    private static boolean requiresLegacyTamingFlag(ResourceLocation id) {
        return id.equals(VARASUCHUS_ID)
                || id.equals(RAEVYX_ID)
                || id.equals(IGNIVORUS_ID)
                || id.equals(VOLITANS_ID)
                || id.equals(NULLJAW_ID)
                || id.equals(ATROXIIA_ID);
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
        base.put(ATROXIIA_ID, atroxiiaDefaults());
        base.put(DRACONIAN_SWARM_ID, draconianSwarmDefaults());
        return base;
    }

    private void backfillPreyTamingChances(Path path, ResourceLocation id, DragonAttributeConfig mergedConfig) {
        if (!id.equals(IGNIVORUS_ID)
                && !id.equals(RAEVYX_ID)
                && !id.equals(VARASUCHUS_ID)
                && !id.equals(ATROXIIA_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), id.toString());
            JsonObject extra = json.has("extra") ? GsonHelper.getAsJsonObject(json, "extra") : new JsonObject();
            boolean updated = false;

            if (id.equals(IGNIVORUS_ID) || id.equals(RAEVYX_ID)) {
                if (!extra.has("taming_chance_mutton")) {
                    extra.addProperty("taming_chance_mutton",
                            mergedConfig.extraDouble("taming_chance_mutton", id.equals(RAEVYX_ID) ? 20.0D : 14.2857D));
                    updated = true;
                }
                if (!extra.has("taming_chance_porkchop")) {
                    extra.addProperty("taming_chance_porkchop",
                            mergedConfig.extraDouble("taming_chance_porkchop", id.equals(RAEVYX_ID) ? 20.0D : 14.2857D));
                    updated = true;
                }
            }
            if (id.equals(VARASUCHUS_ID) && !extra.has("taming_chance_beef")) {
                extra.addProperty("taming_chance_beef",
                        mergedConfig.extraDouble("taming_chance_beef", 16.6667D));
                updated = true;
            }
            if (id.equals(ATROXIIA_ID)) {
                if (!extra.has("taming_chance_base")) {
                    extra.addProperty("taming_chance_base",
                            mergedConfig.extraDouble("taming_chance_base", 20.0D));
                    updated = true;
                }
                if (!extra.has("taming_chance_hearty")) {
                    extra.addProperty("taming_chance_hearty",
                            mergedConfig.extraDouble("taming_chance_hearty", 33.3333D));
                    updated = true;
                }
            }

            if (updated) {
                json.add("extra", extra);
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill prey taming chances at {}", path, e);
        }
    }

    private void backfillAtroxiiaAbilityTuning(Path path, ResourceLocation id,
                                               DragonAttributeConfig mergedConfig) {
        if (!id.equals(ATROXIIA_ID)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), id.toString());
            JsonObject abilities = json.has("abilities")
                    ? GsonHelper.getAsJsonObject(json, "abilities")
                    : new JsonObject();
            JsonObject extra = json.has("extra")
                    ? GsonHelper.getAsJsonObject(json, "extra")
                    : new JsonObject();
            boolean updated = false;

            for (Map.Entry<String, DragonAbilityOverride> entry : mergedConfig.abilities().entrySet()) {
                String abilityId = entry.getKey();
                DragonAbilityOverride defaults = entry.getValue();
                JsonObject ability = abilities.has(abilityId)
                        ? GsonHelper.getAsJsonObject(abilities, abilityId)
                        : new JsonObject();

                if (defaults.damage() != null && !ability.has("damage")) {
                    ability.addProperty("damage", defaults.damage());
                    updated = true;
                }
                if (defaults.knockback() != null && !ability.has("knockback")) {
                    double value = abilityId.equals("helheim_quake") && extra.has("helheim_quake1_knockback")
                            ? GsonHelper.getAsDouble(extra, "helheim_quake1_knockback")
                            : defaults.knockback();
                    ability.addProperty("knockback", value);
                    updated = true;
                }
                if (defaults.secondaryKnockback() != null && !ability.has("secondary_knockback")) {
                    double value = abilityId.equals("helheim_quake") && extra.has("helheim_quake2_knockback")
                            ? GsonHelper.getAsDouble(extra, "helheim_quake2_knockback")
                            : defaults.secondaryKnockback();
                    ability.addProperty("secondary_knockback", value);
                    updated = true;
                }
                if (defaults.stunDurationTicks() != null && !ability.has("stun_duration_ticks")) {
                    ability.addProperty("stun_duration_ticks", defaults.stunDurationTicks());
                    updated = true;
                }
                if (defaults.enabled() != null && !ability.has("enabled")) {
                    ability.addProperty("enabled", defaults.enabled());
                    updated = true;
                }
                if (!abilities.has(abilityId)) {
                    abilities.add(abilityId, ability);
                    updated = true;
                }
            }

            if (extra.remove("helheim_quake1_knockback") != null) {
                updated = true;
            }
            if (extra.remove("helheim_quake2_knockback") != null) {
                updated = true;
            }

            if (updated) {
                json.add("abilities", abilities);
                if (extra.entrySet().isEmpty()) {
                    json.remove("extra");
                } else {
                    json.add("extra", extra);
                }
                writeConfigFile(path, json);
            }
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.warn("Failed to backfill Atroxiia ability tuning at {}", path, e);
        }
    }

}
