package com.leon.saintsdragons.client.camera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DragonRideCameraTuning {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long RELOAD_POLL_INTERVAL_MS = 500L;
    private static final String FILE_NAME = "camera_tuning.json";

    public static final CameraProfile RAEVYX = new CameraProfile(15.0f, 22.0f, 5.5f, 0.075f, 0.15, 0.12, 1.55, 8.0, 0.0f, 6.0f, 0.15f);
    public static final CameraProfile CINDERVANE = new CameraProfile(7.0f, 25.0f, 5.5f, 0.075f, 0.15, 0.12, 2.4, 9.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile IGNIVORUS = new CameraProfile(25.0f, 30.0f, 6.5f, 0.05f, 0.15, 0.12, 0.0, 10.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile VARASUCHUS = new CameraProfile(10.0f, 20.0f, 0.0f, 0.075f, 0.15, 0.12, 1.0, 6.0, 0.0f, 15.0f, 0.15f);
    public static final CameraProfile STEGONAUT = new CameraProfile(8.0f, 8.0f, 0.0f, 0.05f, 0.15, 0.12, 0.0, 0.0, 0.0f, 0.0f, 0.15f);
    public static final CameraProfile VOLITANS = new CameraProfile(12.0f, 20.0f, 5.5f, 0.05f, 0.15, 0.12, 0.0, 6.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile NULLJAW = new CameraProfile(3.0f, 8.0f, 0.0f, 0.05f, 0.15, 0.12, 0.0, 1.0, 0.0f, 0.0f, 0.15f);
    public static final CameraProfile DEFAULT = new CameraProfile(15.0f, 15.0f, 5.5f, 0.05f, 0.15, 0.12, 0.0, 0.0, 0.0f, 0.0f, 0.15f);

    private static final Map<String, CameraProfile> DEFAULT_PROFILES = new HashMap<>();
    private static final Map<Class<?>, String> PROFILE_KEYS = new HashMap<>();
    private static Map<String, CameraProfile> activeProfiles = new HashMap<>();
    private static Path configPath;
    private static boolean initialized = false;
    private static long lastKnownModifiedTime = Long.MIN_VALUE;
    private static long lastPollTimeMs = 0L;

    private DragonRideCameraTuning() {
    }

    static {
        DEFAULT_PROFILES.put("raevyx", RAEVYX);
        DEFAULT_PROFILES.put("cindervane", CINDERVANE);
        DEFAULT_PROFILES.put("ignivorus", IGNIVORUS);
        DEFAULT_PROFILES.put("varasuchus", VARASUCHUS);
        DEFAULT_PROFILES.put("stegonaut", STEGONAUT);
        DEFAULT_PROFILES.put("volitans", VOLITANS);
        DEFAULT_PROFILES.put("nulljaw", NULLJAW);
        DEFAULT_PROFILES.put("default", DEFAULT);

        PROFILE_KEYS.put(Raevyx.class, "raevyx");
        PROFILE_KEYS.put(Cindervane.class, "cindervane");
        PROFILE_KEYS.put(Ignivorus.class, "ignivorus");
        PROFILE_KEYS.put(Varasuchus.class, "varasuchus");
        PROFILE_KEYS.put(Stegonaut.class, "stegonaut");
        PROFILE_KEYS.put(Volitans.class, "volitans");
        PROFILE_KEYS.put(Nulljaw.class, "nulljaw");
    }

    public static boolean isAirOrWaterMode(Entity vehicle) {
        if (vehicle instanceof Varasuchus varasuchus) {
            return varasuchus.isInWaterOrBubble();
        }
        if (vehicle instanceof Raevyx raevyx) {
            return raevyx.isFlying() || raevyx.isInWaterOrBubble();
        }
        if (vehicle instanceof Cindervane cindervane) {
            return cindervane.isFlying() || cindervane.isInWaterOrBubble();
        }
        if (vehicle instanceof Ignivorus ignivorus) {
            return ignivorus.isFlying() || ignivorus.isInWaterOrBubble();
        }
        if (vehicle instanceof Volitans volitans) {
            return volitans.isFlying() || volitans.isInWaterOrBubble();
        }
        if (vehicle instanceof Nulljaw) {
            return true;
        }
        if (vehicle instanceof Stegonaut) {
            return false;
        }
        return false;
    }

    public static void bootstrap() {
        ensureInitialized();
    }

    public static CameraProfile getProfile(Entity vehicle) {
        maybeReloadFromDisk();
        if (vehicle == null) {
            return activeProfiles.getOrDefault("default", DEFAULT);
        }
        String key = PROFILE_KEYS.get(vehicle.getClass());
        if (key == null) {
            return activeProfiles.getOrDefault("default", DEFAULT);
        }
        return activeProfiles.getOrDefault(key, DEFAULT_PROFILES.getOrDefault(key, DEFAULT));
    }

    public static void maybeReloadFromDisk() {
        ensureInitialized();
        if (configPath == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!Services.PLATFORM.isDevelopmentEnvironment() && lastKnownModifiedTime != Long.MIN_VALUE) {
            return;
        }
        if (now - lastPollTimeMs < RELOAD_POLL_INTERVAL_MS) {
            return;
        }
        lastPollTimeMs = now;

        try {
            long modifiedTime = Files.exists(configPath) ? Files.getLastModifiedTime(configPath).toMillis() : Long.MIN_VALUE;
            if (modifiedTime != lastKnownModifiedTime) {
                loadFromDisk();
            }
        } catch (IOException ignored) {
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        activeProfiles = new HashMap<>(DEFAULT_PROFILES);
        configPath = Services.PLATFORM.getConfigDirectory()
                .resolve("saintsdragons")
                .resolve(FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                writeDefaultConfig();
            }
            loadFromDisk();
        } catch (IOException ignored) {
            activeProfiles = new HashMap<>(DEFAULT_PROFILES);
        }
    }

    private static void loadFromDisk() {
        if (configPath == null || !Files.exists(configPath)) {
            activeProfiles = new HashMap<>(DEFAULT_PROFILES);
            lastKnownModifiedTime = Long.MIN_VALUE;
            return;
        }

        Map<String, CameraProfile> mergedProfiles = new HashMap<>(DEFAULT_PROFILES);
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject root = GsonHelper.convertToJsonObject(element, FILE_NAME);
            for (Map.Entry<String, CameraProfile> entry : DEFAULT_PROFILES.entrySet()) {
                String key = entry.getKey();
                if (!root.has(key) || !root.get(key).isJsonObject()) {
                    continue;
                }
                JsonObject profileJson = GsonHelper.convertToJsonObject(root.get(key), key);
                mergedProfiles.put(key, readProfile(profileJson, entry.getValue()));
            }
            activeProfiles = mergedProfiles;
            lastKnownModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
        } catch (Exception ignored) {
            activeProfiles = new HashMap<>(DEFAULT_PROFILES);
        }
    }

    private static void writeDefaultConfig() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "Edit camera values here. In development, this file hot-reloads while the game is running.");
        for (Map.Entry<String, CameraProfile> entry : DEFAULT_PROFILES.entrySet()) {
            root.add(entry.getKey(), writeProfile(entry.getValue()));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(root));
        }
    }

    private static JsonObject writeProfile(CameraProfile profile) {
        JsonObject json = new JsonObject();
        json.addProperty("grounded_distance", profile.groundedDistance());
        json.addProperty("air_or_water_distance", profile.airOrWaterDistance());
        json.addProperty("bank_shift_max", profile.bankShiftMax());
        json.addProperty("zoom_smoothing", profile.zoomSmoothing());
        json.addProperty("lateral_shift_smoothing", profile.lateralShiftSmoothing());
        json.addProperty("vertical_shift_smoothing", profile.verticalShiftSmoothing());
        json.addProperty("grounded_vertical_shift", profile.groundedVerticalShift());
        json.addProperty("air_or_water_vertical_shift", profile.airOrWaterVerticalShift());
        json.addProperty("grounded_pitch_offset", profile.groundedPitchOffset());
        json.addProperty("air_or_water_pitch_offset", profile.airOrWaterPitchOffset());
        json.addProperty("pitch_smoothing", profile.pitchSmoothing());
        return json;
    }

    private static CameraProfile readProfile(JsonObject json, CameraProfile fallback) {
        return new CameraProfile(
                GsonHelper.getAsFloat(json, "grounded_distance", fallback.groundedDistance()),
                GsonHelper.getAsFloat(json, "air_or_water_distance", fallback.airOrWaterDistance()),
                GsonHelper.getAsFloat(json, "bank_shift_max", fallback.bankShiftMax()),
                GsonHelper.getAsFloat(json, "zoom_smoothing", fallback.zoomSmoothing()),
                GsonHelper.getAsDouble(json, "lateral_shift_smoothing", fallback.lateralShiftSmoothing()),
                GsonHelper.getAsDouble(json, "vertical_shift_smoothing", fallback.verticalShiftSmoothing()),
                GsonHelper.getAsDouble(json, "grounded_vertical_shift", fallback.groundedVerticalShift()),
                GsonHelper.getAsDouble(json, "air_or_water_vertical_shift", fallback.airOrWaterVerticalShift()),
                GsonHelper.getAsFloat(json, "grounded_pitch_offset", fallback.groundedPitchOffset()),
                GsonHelper.getAsFloat(json, "air_or_water_pitch_offset", fallback.airOrWaterPitchOffset()),
                GsonHelper.getAsFloat(json, "pitch_smoothing", fallback.pitchSmoothing())
        );
    }

    public record CameraProfile(
            float groundedDistance,
            float airOrWaterDistance,
            float bankShiftMax,
            float zoomSmoothing,
            double lateralShiftSmoothing,
            double verticalShiftSmoothing,
            double groundedVerticalShift,
            double airOrWaterVerticalShift,
            float groundedPitchOffset,
            float airOrWaterPitchOffset,
            float pitchSmoothing
    ) {
        public float grounded() {
            return groundedDistance;
        }

        public float airOrWater() {
            return airOrWaterDistance;
        }
    }
}
