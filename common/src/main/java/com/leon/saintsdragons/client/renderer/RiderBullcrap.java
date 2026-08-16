package com.leon.saintsdragons.client.renderer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived snapshots that bridge GeckoLib's dragon render with vanilla's later player render.
 *
 * <p>Entries are keyed by entity UUID rather than the recyclable client entity id. A matrix and
 * its matching camera offset are published together so consumers can never observe a half-updated
 * attachment. The cache is also cleared on client-level changes and pruned while frames advance.</p>
 */
public final class RiderBullcrap {
    private static final long CACHE_RETENTION_NANOS = TimeUnit.SECONDS.toNanos(2L);
    private static final long PRUNE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final long FALLBACK_FRAME_THRESHOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);

    private static final Map<SeatKey, AttachmentSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Set<SeatKey> FRAME_EXTRACTED_SEATS = ConcurrentHashMap.newKeySet();

    private static volatile long currentRenderFrameId;
    private static long lastFallbackFrameNanos;
    private static long lastPruneNanos;
    @Nullable
    private static Object activeLevelIdentity;

    private RiderBullcrap() {
    }

    /**
     * Starts one client render frame and performs bounded cache maintenance.
     *
     * @param levelIdentity the current ClientLevel, or {@code null} outside a world
     */
    public static synchronized void beginRenderFrame(@Nullable Object levelIdentity) {
        if (activeLevelIdentity != levelIdentity) {
            clearInternal();
            activeLevelIdentity = levelIdentity;
        }

        currentRenderFrameId++;
        FRAME_EXTRACTED_SEATS.clear();

        long now = System.nanoTime();
        if (now - lastPruneNanos >= PRUNE_INTERVAL_NANOS) {
            SNAPSHOTS.entrySet().removeIf(entry -> now - entry.getValue().capturedAtNanos > CACHE_RETENTION_NANOS);
            lastPruneNanos = now;
        }
    }

    public static boolean tryLockForFrame(Entity entity, int seatIndex) {
        SeatKey key = SeatKey.of(entity, seatIndex);
        if (currentRenderFrameId != 0L) {
            return FRAME_EXTRACTED_SEATS.add(key);
        }

        // Compatibility fallback if a loader or renderer replaces the normal GameRenderer hook.
        synchronized (RiderBullcrap.class) {
            long now = System.nanoTime();
            if (now - lastFallbackFrameNanos > FALLBACK_FRAME_THRESHOLD_NANOS) {
                FRAME_EXTRACTED_SEATS.clear();
                lastFallbackFrameNanos = now;
            }
            return FRAME_EXTRACTED_SEATS.add(key);
        }
    }

    public static void store(Entity entity, int seatIndex, Matrix4f matrix, Vec3 cameraOffset) {
        if (entity == null || matrix == null || cameraOffset == null) {
            return;
        }

        SNAPSHOTS.put(
                SeatKey.of(entity, seatIndex),
                new AttachmentSnapshot(new Matrix4f((Matrix4fc) matrix), cameraOffset, System.nanoTime())
        );
    }

    @Nullable
    public static Matrix4f getMatrix(Entity entity, int seatIndex, long maxAgeMillis) {
        AttachmentSnapshot snapshot = getFreshSnapshot(entity, seatIndex, maxAgeMillis);
        return snapshot == null ? null : new Matrix4f((Matrix4fc) snapshot.matrix);
    }

    @Nullable
    public static Vec3 getCameraOffset(Entity entity, int seatIndex, long maxAgeMillis) {
        AttachmentSnapshot snapshot = getFreshSnapshot(entity, seatIndex, maxAgeMillis);
        return snapshot == null ? null : snapshot.cameraOffset;
    }

    public static void remove(Entity entity) {
        if (entity == null) {
            return;
        }

        UUID entityUuid = entity.getUUID();
        SNAPSHOTS.keySet().removeIf(key -> key.entityUuid.equals(entityUuid));
        FRAME_EXTRACTED_SEATS.removeIf(key -> key.entityUuid.equals(entityUuid));
    }

    public static synchronized void clear() {
        clearInternal();
        activeLevelIdentity = null;
    }

    @Nullable
    private static AttachmentSnapshot getFreshSnapshot(Entity entity, int seatIndex, long maxAgeMillis) {
        if (entity == null || maxAgeMillis < 0L) {
            return null;
        }

        SeatKey key = SeatKey.of(entity, seatIndex);
        AttachmentSnapshot snapshot = SNAPSHOTS.get(key);
        if (snapshot == null) {
            return null;
        }

        long maxAgeNanos = TimeUnit.MILLISECONDS.toNanos(maxAgeMillis);
        if (System.nanoTime() - snapshot.capturedAtNanos > maxAgeNanos) {
            SNAPSHOTS.remove(key, snapshot);
            return null;
        }
        return snapshot;
    }

    private static void clearInternal() {
        SNAPSHOTS.clear();
        FRAME_EXTRACTED_SEATS.clear();
        currentRenderFrameId = 0L;
        lastFallbackFrameNanos = 0L;
        lastPruneNanos = 0L;
    }

    private record SeatKey(UUID entityUuid, int seatIndex) {
        private static SeatKey of(Entity entity, int seatIndex) {
            return new SeatKey(entity.getUUID(), seatIndex);
        }
    }

    private static final class AttachmentSnapshot {
        private final Matrix4f matrix;
        private final Vec3 cameraOffset;
        private final long capturedAtNanos;

        private AttachmentSnapshot(Matrix4f matrix, Vec3 cameraOffset, long capturedAtNanos) {
            this.matrix = matrix;
            this.cameraOffset = cameraOffset;
            this.capturedAtNanos = capturedAtNanos;
        }
    }
}
