package com.leon.saintsdragons.client.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RiderBullcrap {
    private static final Map<Long, Matrix4f> MATRICES = new ConcurrentHashMap<>();
    private static final Map<Long, Vec3> CAMERA_OFFSETS = new ConcurrentHashMap<>();
    private static final Map<Long, Long> MATRIX_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> RENDER_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Set<Long> FRAME_EXTRACTED_SEATS = ConcurrentHashMap.newKeySet();
    private static long lastFrameNanoTime = 0L;
    private static final long FRAME_TIME_THRESHOLD_NS = 1_000_000L;

    private RiderBullcrap() {
    }

    public static boolean tryLockForFrame(int entityId) {
        return tryLockForFrame(entityId, 0);
    }

    public static boolean tryLockForFrame(int entityId, int seatIndex) {
        long currentNanoTime = System.nanoTime();
        if (currentNanoTime - lastFrameNanoTime > FRAME_TIME_THRESHOLD_NS) {
            FRAME_EXTRACTED_SEATS.clear();
            lastFrameNanoTime = currentNanoTime;
        }
        return FRAME_EXTRACTED_SEATS.add(makeSeatKey(entityId, seatIndex));
    }

    public static void store(int entityId, Matrix4f matrix) {
        store(entityId, 0, matrix);
    }

    public static void store(int entityId, int seatIndex, Matrix4f matrix) {
        long key = makeSeatKey(entityId, seatIndex);
        MATRICES.put(key, new Matrix4f((Matrix4fc) matrix));
        MATRIX_TIMESTAMPS.put(key, System.currentTimeMillis());
    }

    public static void storeCameraOffset(int entityId, Vec3 cameraOffset) {
        storeCameraOffset(entityId, 0, cameraOffset);
    }

    public static void storeCameraOffset(int entityId, int seatIndex, Vec3 cameraOffset) {
        CAMERA_OFFSETS.put(makeSeatKey(entityId, seatIndex), cameraOffset);
    }

    @Nullable
    public static Matrix4f get(int entityId) {
        return get(entityId, 0);
    }

    @Nullable
    public static Matrix4f get(int entityId, int seatIndex) {
        return MATRICES.get(makeSeatKey(entityId, seatIndex));
    }

    @Nullable
    public static Vec3 getCameraOffset(int entityId) {
        return getCameraOffset(entityId, 0);
    }

    @Nullable
    public static Vec3 getCameraOffset(int entityId, int seatIndex) {
        return CAMERA_OFFSETS.get(makeSeatKey(entityId, seatIndex));
    }

    public static long getTimestamp(int entityId) {
        return getTimestamp(entityId, 0);
    }

    public static long getTimestamp(int entityId, int seatIndex) {
        return MATRIX_TIMESTAMPS.getOrDefault(makeSeatKey(entityId, seatIndex), 0L);
    }

    public static void notifyRendered(int entityId) {
        RENDER_TIMESTAMPS.put(entityId, System.currentTimeMillis());
    }

    public static long getLastRenderTime(int entityId) {
        return RENDER_TIMESTAMPS.getOrDefault(entityId, 0L);
    }

    public static void remove(int entityId) {
        for (int seat = 0; seat < 4; seat++) {
            long key = makeSeatKey(entityId, seat);
            MATRICES.remove(key);
            CAMERA_OFFSETS.remove(key);
            MATRIX_TIMESTAMPS.remove(key);
        }
        RENDER_TIMESTAMPS.remove(entityId);
    }

    private static long makeSeatKey(int entityId, int seatIndex) {
        return ((long) entityId << 32) ^ (seatIndex & 0xffffffffL);
    }
}
