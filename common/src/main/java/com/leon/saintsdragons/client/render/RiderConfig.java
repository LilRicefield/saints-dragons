package com.leon.saintsdragons.client.render;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RiderConfig {
    // ===== RAEVYX TUNING =====
    public static final String RAEVYX_BONE = "passengerBone";
    public static final float RAEVYX_SEAT_X = 0.0f;
    public static final float RAEVYX_SEAT_Y = 3.75f;
    public static final float RAEVYX_SEAT_Z = -2.5f;
    public static final float RAEVYX_FIRST_PERSON_X = 0.0f;
    public static final float RAEVYX_FIRST_PERSON_Y = 1.2f;
    public static final float RAEVYX_FIRST_PERSON_Z = 0.0f;
    public static final long RAEVYX_STALE_MS = 200L;
    public static final double RAEVYX_CAPTURE_DISTANCE = 80.0;
    public static final float RAEVYX_YAW_OFFSET_DEG = -180.0f;

    private static Map<Class<?>, RiderSpec> riderConfigs;

    private RiderConfig() {
    }

    private static void initializeConfigs() {
        if (riderConfigs != null) {
            return;
        }

        riderConfigs = new HashMap<>();
        riderConfigs.put(Raevyx.class, new RiderSpec(
                RAEVYX_BONE,
                new Vector3f(RAEVYX_SEAT_X, RAEVYX_SEAT_Y, RAEVYX_SEAT_Z),
                new Vector3f(RAEVYX_FIRST_PERSON_X, RAEVYX_FIRST_PERSON_Y, RAEVYX_FIRST_PERSON_Z),
                RAEVYX_STALE_MS,
                RAEVYX_CAPTURE_DISTANCE,
                RAEVYX_YAW_OFFSET_DEG
        ));
    }

    @Nullable
    public static RiderSpec getSpec(Object dragon) {
        if (dragon == null) {
            return null;
        }
        initializeConfigs();
        return riderConfigs.get(dragon.getClass());
    }

    public static RiderSpec getOrDefaultSpec(Object dragon) {
        RiderSpec spec = getSpec(dragon);
        if (spec != null) {
            return spec;
        }
        return new RiderSpec("passengerBone");
    }

    public static Vector3f getSeatOffset(Object dragon) {
        return new Vector3f(getOrDefaultSpec(dragon).offset);
    }

    public static Vector3f getFirstPersonOffset(Object dragon) {
        return new Vector3f(getOrDefaultSpec(dragon).firstPersonOffset);
    }

    public static final class RiderSpec {
        public final String boneName;
        public final Vector3f offset;
        public final Vector3f firstPersonOffset;
        public final long staleMs;
        public final double maxCaptureDistance;
        public final float yawOffsetDeg;

        public RiderSpec(String boneName, Vector3f offset, Vector3f firstPersonOffset, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            this.boneName = boneName;
            this.offset = offset;
            this.firstPersonOffset = firstPersonOffset;
            this.staleMs = staleMs;
            this.maxCaptureDistance = maxCaptureDistance;
            this.yawOffsetDeg = yawOffsetDeg;
        }

        public RiderSpec(String boneName, float x, float y, float z, float fpX, float fpY, float fpZ, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            this(boneName, new Vector3f(x, y, z), new Vector3f(fpX, fpY, fpZ), staleMs, maxCaptureDistance, yawOffsetDeg);
        }

        public RiderSpec(String boneName) {
            this(boneName, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 200L, 80.0, -180.0f);
        }
    }
}
