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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ResourceLocation CINDERVANE_ID = SaintsDragonsCommon.rl("cindervane");
    public static final ResourceLocation RAEVYX_ID = SaintsDragonsCommon.rl("raevyx");
    public static final ResourceLocation NULLJAW_ID = SaintsDragonsCommon.rl("nulljaw");
    public static final ResourceLocation IGNIVORUS_ID = SaintsDragonsCommon.rl("ignivorus");

    private static final DragonAttributeConfigLoader INSTANCE = new DragonAttributeConfigLoader();

    private final Map<ResourceLocation, DragonAttributeConfig> defaults;
    private final Path configDirectory;
    private volatile Map<ResourceLocation, DragonAttributeConfig> configs;

    private DragonAttributeConfigLoader() {
        super(GSON, "dragon_attributes");
        this.configDirectory = Services.PLATFORM.getConfigDirectory()
                .resolve(SaintsDragonsCommon.MOD_ID)
                .resolve("dragon_attributes");
        Map<ResourceLocation, DragonAttributeConfig> base = new HashMap<>();
        base.put(CINDERVANE_ID, cindervaneDefaults());
        base.put(RAEVYX_ID, raevyxDefaults());
        base.put(NULLJAW_ID, nulljawDefaults());
        base.put(IGNIVORUS_ID, ignivorusDefaults());
        this.defaults = ImmutableMap.copyOf(base);
        this.configs = this.defaults;
    }

    private static DragonAttributeConfig cindervaneDefaults() {
        return new DragonAttributeConfig(
                80.0D,
                4.0D,
                0.45D,
                0.60D,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(12.0D),
                        "magma_volley", DragonAbilityOverride.ofDamage(20.0D)
                ),
                Map.of(
                        "taming_chance_base", 4.0D,
                        "taming_chance_hearty", 2.0D
                )
        );
    }

    private static DragonAttributeConfig raevyxDefaults() {
        return new DragonAttributeConfig(
                180.0D,
                8.0D,
                0.25D,
                1.0D,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(15.0D),
                        "lightning_beam", DragonAbilityOverride.ofDamage(35.0D),
                        "horn_gore", DragonAbilityOverride.ofDamage(15.0D)
                ),
                Map.of(
                        "taming_chance_base", 5.0D,
                        "taming_chance_hearty", 3.0D
                )
        );
    }

    private static DragonAttributeConfig nulljawDefaults() {
        return new DragonAttributeConfig(
                250.0D,
                8.0D,
                0.28D,
                0.0D,
                Map.of(
                        "bite_phase1", DragonAbilityOverride.ofDamage(40.0D),
                        "bite_phase2", DragonAbilityOverride.ofDamage(50.0D),
                        "horn_gore_phase1", DragonAbilityOverride.ofDamage(16.0D),
                        "horn_gore_phase2", DragonAbilityOverride.ofDamage(20.8D)
                ),
                Map.of(
                        "walk_speed", 0.14D,
                        "swim_speed", 1.45D,
                        "taming_chance", 6.0D
                )
        );
    }

    private static DragonAttributeConfig ignivorusDefaults() {
        return new DragonAttributeConfig(
                300.0D,
                4.0D,
                0.30D,
                0.40D,
                Map.of(
                        "bite", DragonAbilityOverride.ofDamage(50.0D),
                        "body_slam", DragonAbilityOverride.ofDamage(40.0D),
                        "fire_breath", DragonAbilityOverride.ofDamage(4.0D),
                        "ultimate", DragonAbilityOverride.ofDamage(200.0D)
                ),
                Map.of(
                        "attack_damage", 15.0D,
                        "ultimate_penalty_health", 50.0D,
                        "taming_chance_base", 7.0D,
                        "taming_chance_hearty", 4.0D
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
        DragonAttributeConfig config = configs.get(id);
        if (config != null) {
            return config;
        }
        DragonAttributeConfig fallback = defaults.get(id);
        return fallback != null ? fallback : DragonAttributeConfig.EMPTY;
    }

    public DragonAttributeConfig getDefaultConfig(ResourceLocation id) {
        return defaults.getOrDefault(id, DragonAttributeConfig.EMPTY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
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
            if (Files.exists(path)) {
                continue;
            }
            JsonObject source = rawJson.getOrDefault(entry.getKey(), serializeConfig(entry.getValue()));
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

    private static JsonObject serializeConfig(DragonAttributeConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("max_health", config.maxHealth());
        json.addProperty("armor", config.armor());
        json.addProperty("movement_speed", config.movementSpeed());
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
        if (!config.extraDoubles().isEmpty()) {
            JsonObject extraJson = new JsonObject();
            config.extraDoubles().forEach(extraJson::addProperty);
            json.add("extra", extraJson);
        }

        return json;
    }

    public void overwriteConfig(ResourceLocation id, DragonAttributeConfig config) {
        writeConfigFile(configPath(id), serializeConfig(config));
        Map<ResourceLocation, DragonAttributeConfig> updated = new HashMap<>(this.configs);
        updated.put(id, config);
        this.configs = ImmutableMap.copyOf(updated);
    }
}
