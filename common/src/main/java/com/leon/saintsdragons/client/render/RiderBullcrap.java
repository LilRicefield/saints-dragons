package com.leon.saintsdragons.client.render;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RiderBullcrap {
    private static final Map<Integer, Matrix4f> MATRICES = new ConcurrentHashMap<>();
    private static final Map<Integer, Vec3> CAMERA_OFFSETS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> MATRIX_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> RENDER_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Set<Integer> FRAME_EXTRACTED_DRAGONS = ConcurrentHashMap.newKeySet();
    private static long lastFrameNanoTime = 0L;
    private static final long FRAME_TIME_THRESHOLD_NS = 1_000_000L;

    private RiderBullcrap() {
    }

    public static boolean tryLockForFrame(int entityId) {
        long currentNanoTime = System.nanoTime();
        if (currentNanoTime - lastFrameNanoTime > FRAME_TIME_THRESHOLD_NS) {
            FRAME_EXTRACTED_DRAGONS.clear();
            lastFrameNanoTime = currentNanoTime;
        }
        return FRAME_EXTRACTED_DRAGONS.add(entityId);
    }

    public static void store(int entityId, Matrix4f matrix) {
        MATRICES.put(entityId, new Matrix4f((Matrix4fc) matrix));
        MATRIX_TIMESTAMPS.put(entityId, System.currentTimeMillis());
    }

    public static void storeCameraOffset(int entityId, Vec3 cameraOffset) {
        CAMERA_OFFSETS.put(entityId, cameraOffset);
    }

    @Nullable
    public static Matrix4f get(int entityId) {
        return MATRICES.get(entityId);
    }

    @Nullable
    public static Vec3 getCameraOffset(int entityId) {
        return CAMERA_OFFSETS.get(entityId);
    }

    public static long getTimestamp(int entityId) {
        return MATRIX_TIMESTAMPS.getOrDefault(entityId, 0L);
    }

    public static void notifyRendered(int entityId) {
        RENDER_TIMESTAMPS.put(entityId, System.currentTimeMillis());
    }

    public static long getLastRenderTime(int entityId) {
        return RENDER_TIMESTAMPS.getOrDefault(entityId, 0L);
    }

    public static void remove(int entityId) {
        MATRICES.remove(entityId);
        CAMERA_OFFSETS.remove(entityId);
        MATRIX_TIMESTAMPS.remove(entityId);
        RENDER_TIMESTAMPS.remove(entityId);
    }
}
