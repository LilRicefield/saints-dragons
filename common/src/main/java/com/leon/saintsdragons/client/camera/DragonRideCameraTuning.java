package com.leon.saintsdragons.client.camera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.ConfigStorageLayout;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DragonRideCameraTuning {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long RELOAD_POLL_INTERVAL_MS = 500L;
    private static final String FILE_NAME = "camera_tuning.json";
    private static final String CONFIG_NOTE = "Client-side dragon rider camera distances. Edit them here or in the in-game config screen.";

    public static final double MIN_CAMERA_DISTANCE = 0.0D;
    public static final double MAX_CAMERA_DISTANCE = 100.0D;
    private static final List<String> CONFIGURABLE_PROFILE_KEYS = List.of(
            "cindervane",
            "raevyx",
            "stegonaut",
            "varasuchus",
            "ignivorus",
            "volitans",
            "nulljaw",
            "atroxiia"
    );

    public static final CameraProfile RAEVYX = new CameraProfile(15.0f, 22.0f, 5.5f, 0.075f, 0.15, 0.12, 1.55, 8.0, 0.0f, 6.0f, 0.15f);
    public static final CameraProfile CINDERVANE = new CameraProfile(7.0f, 25.0f, 5.5f, 0.075f, 0.15, 0.12, 2.4, 9.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile IGNIVORUS = new CameraProfile(25.0f, 30.0f, 6.5f, 0.05f, 0.15, 0.12, 0.0, 10.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile VARASUCHUS = new CameraProfile(10.0f, 20.0f, 0.0f, 0.075f, 0.15, 0.12, 1.0, 6.0, 0.0f, 15.0f, 0.15f);
    public static final CameraProfile STEGONAUT = new CameraProfile(8.0f, 8.0f, 0.0f, 0.05f, 0.15, 0.12, 0.0, 0.0, 0.0f, 0.0f, 0.15f);
    public static final CameraProfile VOLITANS = new CameraProfile(12.0f, 20.0f, 5.5f, 0.05f, 0.15, 0.12, 0.0, 6.0, 0.0f, 10.0f, 0.15f);
    public static final CameraProfile NULLJAW = new CameraProfile(3.0f, 8.0f, 0.0f, 0.05f, 0.15, 0.12, 0.0, 1.0, 0.0f, 0.0f, 0.15f);
    public static final CameraProfile ATROXIIA = new CameraProfile(10.0f, 10.0f, 0.0f, 0.05f, 0.15, 0.12, 1.0, 1.0, 0.0f, 0.0f, 0.15f);
    public static final CameraProfile DEFAULT = new CameraProfile(15.0f, 15.0f, 5.5f, 0.05f, 0.15, 0.12, 0.0, 0.0, 0.0f, 0.0f, 0.15f);

    private static final Map<String, CameraProfile> DEFAULT_PROFILES = new LinkedHashMap<>();
    private static final Map<Class<?>, String> PROFILE_KEYS = new HashMap<>();
    private static Map<String, CameraProfile> activeProfiles = new HashMap<>();
    private static Path configPath;
    private static boolean initialized = false;
    private static long lastKnownModifiedTime = Long.MIN_VALUE;
    private static long lastPollTimeMs = 0L;

    private DragonRideCameraTuning() {
    }

    static {
        DEFAULT_PROFILES.put("cindervane", CINDERVANE);
        DEFAULT_PROFILES.put("raevyx", RAEVYX);
        DEFAULT_PROFILES.put("stegonaut", STEGONAUT);
        DEFAULT_PROFILES.put("varasuchus", VARASUCHUS);
        DEFAULT_PROFILES.put("ignivorus", IGNIVORUS);
        DEFAULT_PROFILES.put("volitans", VOLITANS);
        DEFAULT_PROFILES.put("nulljaw", NULLJAW);
        DEFAULT_PROFILES.put("atroxiia", ATROXIIA);
        DEFAULT_PROFILES.put("default", DEFAULT);

        PROFILE_KEYS.put(Raevyx.class, "raevyx");
        PROFILE_KEYS.put(Cindervane.class, "cindervane");
        PROFILE_KEYS.put(Ignivorus.class, "ignivorus");
        PROFILE_KEYS.put(Varasuchus.class, "varasuchus");
        PROFILE_KEYS.put(Stegonaut.class, "stegonaut");
        PROFILE_KEYS.put(Volitans.class, "volitans");
        PROFILE_KEYS.put(Nulljaw.class, "nulljaw");
        PROFILE_KEYS.put(Atroxiia.class, "atroxiia");
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
        if (vehicle instanceof Atroxiia) {
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

    public static List<String> getConfigurableProfileKeys() {
        return CONFIGURABLE_PROFILE_KEYS;
    }

    public static CameraProfile getProfile(String key) {
        ensureInitialized();
        return activeProfiles.getOrDefault(key, DEFAULT_PROFILES.getOrDefault(key, DEFAULT));
    }

    public static CameraProfile getDefaultProfile(String key) {
        return DEFAULT_PROFILES.getOrDefault(key, DEFAULT);
    }

    public static void setGroundedDistance(String key, double distance) {
        ensureInitialized();
        CameraProfile current = getProfile(key);
        activeProfiles.put(key, withDistances(current, clampDistance(distance), current.airOrWaterDistance()));
    }

    public static void setAirOrWaterDistance(String key, double distance) {
        ensureInitialized();
        CameraProfile current = getProfile(key);
        activeProfiles.put(key, withDistances(current, current.groundedDistance(), clampDistance(distance)));
    }

    public static void save() {
        ensureInitialized();
        try {
            writeConfig(activeProfiles);
            lastKnownModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
        } catch (IOException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not save dragon rider camera config {}", configPath, exception);
        }
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
        ConfigStorageLayout.migrateLegacyFiles();
        configPath = Services.PLATFORM.getConfigDirectory()
                .resolve(SaintsDragonsConfig.DRAGON_RIDER_CAMERA_CONFIG_FOLDER)
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

        JsonObject root;
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            root = GsonHelper.convertToJsonObject(element, FILE_NAME);
        } catch (Exception exception) {
            activeProfiles = new HashMap<>(DEFAULT_PROFILES);
            SaintsDragonsCommon.LOGGER.warn("Could not load dragon rider camera config {}", configPath, exception);
            return;
        }

        Map<String, CameraProfile> mergedProfiles = new HashMap<>(DEFAULT_PROFILES);
        boolean needsRewrite = false;
        try {
            if (!root.has("_note") || !CONFIG_NOTE.equals(GsonHelper.getAsString(root, "_note", ""))) {
                needsRewrite = true;
            }
            for (String key : CONFIGURABLE_PROFILE_KEYS) {
                CameraProfile fallback = DEFAULT_PROFILES.get(key);
                if (!root.has(key) || !root.get(key).isJsonObject()) {
                    needsRewrite = true;
                    continue;
                }
                JsonObject profileJson = GsonHelper.convertToJsonObject(root.get(key), key);
                mergedProfiles.put(key, readProfile(profileJson, fallback));
                if (!isOfficialProfile(profileJson)) {
                    needsRewrite = true;
                }
            }
            for (String key : root.keySet()) {
                if (!"_note".equals(key) && !CONFIGURABLE_PROFILE_KEYS.contains(key)) {
                    needsRewrite = true;
                    break;
                }
            }
            activeProfiles = mergedProfiles;
            if (needsRewrite) {
                try {
                    writeConfig(activeProfiles);
                } catch (IOException exception) {
                    SaintsDragonsCommon.LOGGER.warn("Could not rewrite legacy dragon rider camera config {}", configPath, exception);
                }
            }
            lastKnownModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
        } catch (Exception exception) {
            activeProfiles = new HashMap<>(DEFAULT_PROFILES);
            SaintsDragonsCommon.LOGGER.warn("Could not load dragon rider camera config {}", configPath, exception);
        }
    }

    private static void writeDefaultConfig() throws IOException {
        writeConfig(DEFAULT_PROFILES);
    }

    private static void writeConfig(Map<String, CameraProfile> profiles) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_note", CONFIG_NOTE);
        for (String key : CONFIGURABLE_PROFILE_KEYS) {
            root.add(key, writeProfile(profiles.getOrDefault(key, DEFAULT_PROFILES.get(key))));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(root));
        }
    }

    private static JsonObject writeProfile(CameraProfile profile) {
        JsonObject json = new JsonObject();
        json.addProperty("grounded_distance", profile.groundedDistance());
        json.addProperty("air_or_water_distance", profile.airOrWaterDistance());
        return json;
    }

    private static CameraProfile readProfile(JsonObject json, CameraProfile fallback) {
        return withDistances(
                fallback,
                clampDistance(GsonHelper.getAsDouble(json, "grounded_distance", fallback.groundedDistance())),
                clampDistance(GsonHelper.getAsDouble(json, "air_or_water_distance", fallback.airOrWaterDistance()))
        );
    }

    private static boolean isOfficialProfile(JsonObject json) {
        return json.size() == 2
                && json.has("grounded_distance")
                && json.has("air_or_water_distance");
    }

    private static float clampDistance(double distance) {
        if (!Double.isFinite(distance)) {
            return 0.0F;
        }
        return (float) Math.max(MIN_CAMERA_DISTANCE, Math.min(MAX_CAMERA_DISTANCE, distance));
    }

    private static CameraProfile withDistances(CameraProfile profile, float groundedDistance,
                                               float airOrWaterDistance) {
        return new CameraProfile(
                groundedDistance,
                airOrWaterDistance,
                profile.bankShiftMax(),
                profile.zoomSmoothing(),
                profile.lateralShiftSmoothing(),
                profile.verticalShiftSmoothing(),
                profile.groundedVerticalShift(),
                profile.airOrWaterVerticalShift(),
                profile.groundedPitchOffset(),
                profile.airOrWaterPitchOffset(),
                profile.pitchSmoothing()
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
