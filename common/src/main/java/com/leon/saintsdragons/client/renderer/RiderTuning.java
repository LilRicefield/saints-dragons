package com.leon.saintsdragons.client.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RiderTuning {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long RELOAD_POLL_INTERVAL_MS = 500L;
    private static final String FILE_NAME = "rider_tuning.json";

    private static final Map<String, DragonTuning> activeTunings = new HashMap<>();
    private static Path configPath;
    private static boolean initialized = false;
    private static long lastKnownModifiedTime = Long.MIN_VALUE;
    private static long lastPollTimeMs = 0L;

    private RiderTuning() {
    }

    public static void bootstrap() {
        ensureInitialized();
    }

    public static Vector3f getSeatOffset(Object dragon, int seatIndex, Vector3f fallback) {
        SeatTuning seat = getSeatTuning(dragon, seatIndex);
        if (seat == null || seat.offset == null) {
            return fallback;
        }
        return new Vector3f(seat.offset);
    }

    public static Vector3f getFirstPersonOffset(Object dragon, int seatIndex, Vector3f fallback) {
        SeatTuning seat = getSeatTuning(dragon, seatIndex);
        if (seat == null || seat.firstPersonOffset == null) {
            return fallback;
        }
        return new Vector3f(seat.firstPersonOffset);
    }

    public static float getYawOffset(Object dragon, int seatIndex, float fallback) {
        SeatTuning seat = getSeatTuning(dragon, seatIndex);
        if (seat == null || seat.yawOffsetDeg == null) {
            return fallback;
        }
        return seat.yawOffsetDeg;
    }

    private static SeatTuning getSeatTuning(Object dragon, int seatIndex) {
        maybeReloadFromDisk();
        String key = RiderConfig.getTuningKey(dragon);
        if (key == null) {
            return null;
        }
        DragonTuning tuning = activeTunings.get(key);
        if (tuning == null) {
            return null;
        }
        SeatTuning seat = tuning.seats.get(seatIndex);
        if (seat != null) {
            return seat;
        }
        return tuning.seats.get(0);
    }

    private static void maybeReloadFromDisk() {
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
            activeTunings.clear();
        }
    }

    private static void loadFromDisk() {
        if (configPath == null || !Files.exists(configPath)) {
            activeTunings.clear();
            lastKnownModifiedTime = Long.MIN_VALUE;
            return;
        }

        Map<String, DragonTuning> loadedTunings = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject root = GsonHelper.convertToJsonObject(element, FILE_NAME);
            for (String key : RiderConfig.getDefaultTuningSpecs().keySet()) {
                if (!root.has(key) || !root.get(key).isJsonObject()) {
                    continue;
                }
                loadedTunings.put(key, readDragonTuning(GsonHelper.convertToJsonObject(root.get(key), key)));
            }
            activeTunings.clear();
            activeTunings.putAll(loadedTunings);
            lastKnownModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
        } catch (Exception ignored) {
            activeTunings.clear();
        }
    }

    private static void writeDefaultConfig() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_note", "Edit rider/camera offsets here. In development, this file hot-reloads while the game is running.");
        root.addProperty("_offset_note", "offset moves the visible player model on the rider bone. first_person_offset moves the first-person camera anchor from that same rider bone.");

        for (Map.Entry<String, RiderConfig.RiderSpec> entry : RiderConfig.getDefaultTuningSpecs().entrySet()) {
            root.add(entry.getKey(), writeDragonTuning(entry.getValue()));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(root));
        }
    }

    private static JsonObject writeDragonTuning(RiderConfig.RiderSpec spec) {
        JsonObject json = new JsonObject();
        JsonObject seats = new JsonObject();
        for (Map.Entry<Integer, RiderConfig.SeatSpec> entry : new LinkedHashMap<>(spec.getSeatSpecs()).entrySet()) {
            seats.add("seat_" + entry.getKey(), writeSeatTuning(entry.getValue()));
        }
        json.add("seats", seats);
        return json;
    }

    private static JsonObject writeSeatTuning(RiderConfig.SeatSpec spec) {
        JsonObject json = new JsonObject();
        json.add("offset", writeVector(spec.offset));
        json.add("first_person_offset", writeVector(spec.firstPersonOffset));
        json.addProperty("yaw_offset_degrees", spec.yawOffsetDeg);
        return json;
    }

    private static DragonTuning readDragonTuning(JsonObject json) {
        DragonTuning tuning = new DragonTuning();
        if (!json.has("seats") || !json.get("seats").isJsonObject()) {
            tuning.seats.put(0, readSeatTuning(json));
            return tuning;
        }

        JsonObject seats = GsonHelper.convertToJsonObject(json.get("seats"), "seats");
        for (Map.Entry<String, JsonElement> entry : seats.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            int seatIndex = parseSeatIndex(entry.getKey());
            if (seatIndex < 0) {
                continue;
            }
            tuning.seats.put(seatIndex, readSeatTuning(GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey())));
        }
        return tuning;
    }

    private static SeatTuning readSeatTuning(JsonObject json) {
        return new SeatTuning(
                readVector(json, "offset"),
                readVector(json, "first_person_offset"),
                json.has("yaw_offset_degrees") ? GsonHelper.getAsFloat(json, "yaw_offset_degrees") : null
        );
    }

    private static JsonObject writeVector(Vector3f vector) {
        JsonObject json = new JsonObject();
        json.addProperty("x", vector.x());
        json.addProperty("y", vector.y());
        json.addProperty("z", vector.z());
        return json;
    }

    private static Vector3f readVector(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            return null;
        }
        JsonObject vector = GsonHelper.convertToJsonObject(json.get(key), key);
        return new Vector3f(
                GsonHelper.getAsFloat(vector, "x", 0.0f),
                GsonHelper.getAsFloat(vector, "y", 0.0f),
                GsonHelper.getAsFloat(vector, "z", 0.0f)
        );
    }

    private static int parseSeatIndex(String key) {
        String normalized = key.startsWith("seat_") ? key.substring("seat_".length()) : key;
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static final class DragonTuning {
        private final Map<Integer, SeatTuning> seats = new HashMap<>();
    }

    private record SeatTuning(Vector3f offset, Vector3f firstPersonOffset, Float yawOffsetDeg) {
    }
}
