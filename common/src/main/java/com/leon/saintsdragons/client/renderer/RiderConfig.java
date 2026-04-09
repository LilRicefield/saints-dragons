package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RiderConfig {
    // ===== RAEVYX TUNING =====
    public static final String RAEVYX_BONE = "passengerBone";
    public static final float RAEVYX_SEAT_X = 0.0f;
    public static final float RAEVYX_SEAT_Y = 3.53f;
    public static final float RAEVYX_SEAT_Z = -2.5f;
    public static final float RAEVYX_FIRST_PERSON_X = 0.0f;
    public static final float RAEVYX_FIRST_PERSON_Y = 1.2f;
    public static final float RAEVYX_FIRST_PERSON_Z = 0.0f;
    public static final long RAEVYX_STALE_MS = 200L;
    public static final double RAEVYX_CAPTURE_DISTANCE = 80.0;
    public static final float RAEVYX_YAW_OFFSET_DEG = -180.0f;

    // ===== IGNIVORUS TUNING =====
    public static final String IGNIVORUS_BONE = "passengerBone";
    public static final float IGNIVORUS_SEAT_X = 0.0f;
    public static final float IGNIVORUS_SEAT_Y = 5.20f;
    public static final float IGNIVORUS_SEAT_Z = -12.5f;
    public static final float IGNIVORUS_FIRST_PERSON_X = 0.0f;
    public static final float IGNIVORUS_FIRST_PERSON_Y = 2.0f;
    public static final float IGNIVORUS_FIRST_PERSON_Z = 0.0f;
    public static final long IGNIVORUS_STALE_MS = 200L;
    public static final double IGNIVORUS_CAPTURE_DISTANCE = 80.0;
    public static final float IGNIVORUS_YAW_OFFSET_DEG = -180;

    // ===== CINDERVANE TUNING =====
    public static final String CINDERVANE_SEAT0_BONE = "passengerBone1";
    public static final String CINDERVANE_SEAT1_BONE = "passengerBone2";
    public static final float CINDERVANE_SEAT0_X = 0.0f;
    public static final float CINDERVANE_SEAT0_Y = 2.0f;
    public static final float CINDERVANE_SEAT0_Z = -1.0f;
    public static final float CINDERVANE_SEAT1_X = 0.0f;
    public static final float CINDERVANE_SEAT1_Y = 2.0f;
    public static final float CINDERVANE_SEAT1_Z = 0.25f;
    public static final float CINDERVANE_SEAT0_FIRST_PERSON_X = 0.0f;
    public static final float CINDERVANE_SEAT0_FIRST_PERSON_Y = 1.5f;
    public static final float CINDERVANE_SEAT0_FIRST_PERSON_Z = 0.0f;
    public static final float CINDERVANE_SEAT1_FIRST_PERSON_X = 0.0f;
    public static final float CINDERVANE_SEAT1_FIRST_PERSON_Y = 1.55f;
    public static final float CINDERVANE_SEAT1_FIRST_PERSON_Z = -0.25f;
    public static final long CINDERVANE_STALE_MS = 200L;
    public static final double CINDERVANE_CAPTURE_DISTANCE = 80.0;
    public static final float CINDERVANE_SEAT0_YAW_OFFSET_DEG = -180.0f;
    public static final float CINDERVANE_SEAT1_YAW_OFFSET_DEG = -180.0f;

    // ===== STEGONAUT TUNING =====
    public static final String STEGONAUT_BONE = "passengerBone";
    public static final float STEGONAUT_SEAT_X = 0.0f;
    public static final float STEGONAUT_SEAT_Y = 1.55f;
    public static final float STEGONAUT_SEAT_Z = -1.75f;
    public static final float STEGONAUT_FIRST_PERSON_X = 0.0f;
    public static final float STEGONAUT_FIRST_PERSON_Y = 1.0f;
    public static final float STEGONAUT_FIRST_PERSON_Z = 0.0f;
    public static final long STEGONAUT_STALE_MS = 200L;
    public static final double STEGONAUT_CAPTURE_DISTANCE = 80.0;
    public static final float STEGONAUT_YAW_OFFSET_DEG = -180.0f;

    // ===== VOLITANS TUNING =====
    public static final String VOLITANS_BONE = "passengerBone";
    public static final float VOLITANS_SEAT_X = 0.0f;
    public static final float VOLITANS_SEAT_Y = 2.40f;
    public static final float VOLITANS_SEAT_Z = -2.15f;
    public static final float VOLITANS_FIRST_PERSON_X = 0.0f;
    public static final float VOLITANS_FIRST_PERSON_Y = 1.4f;
    public static final float VOLITANS_FIRST_PERSON_Z = 0.0f;
    public static final long VOLITANS_STALE_MS = 200L;
    public static final double VOLITANS_CAPTURE_DISTANCE = 80.0;
    public static final float VOLITANS_YAW_OFFSET_DEG = -180.0f;

    // ===== NULLJAW TUNING =====
    public static final String NULLJAW_BONE = "passengerBone";
    public static final float NULLJAW_SEAT_X = 0.0f;
    public static final float NULLJAW_SEAT_Y = -1.6f;
    public static final float NULLJAW_SEAT_Z = -0.2f;
    public static final float NULLJAW_FIRST_PERSON_X = 0.5f;
    public static final float NULLJAW_FIRST_PERSON_Y = 0.0f;
    public static final float NULLJAW_FIRST_PERSON_Z = 0.0f;
    public static final long NULLJAW_STALE_MS = 200L;
    public static final double NULLJAW_CAPTURE_DISTANCE = 80.0;
    public static final float NULLJAW_YAW_OFFSET_DEG = -180.0f;

    // ===== VARASUCHUS TUNING =====
    public static final String VARASUCHUS_BONE = "passengerBone";
    public static final float VARASUCHUS_SEAT_X = 0.0f;
    public static final float VARASUCHUS_SEAT_Y = 2.78f;
    public static final float VARASUCHUS_SEAT_Z = -3.25f;
    public static final float VARASUCHUS_FIRST_PERSON_X = 0.0f;
    public static final float VARASUCHUS_FIRST_PERSON_Y = 1.1f;
    public static final float VARASUCHUS_FIRST_PERSON_Z = -3.0f;
    public static final long VARASUCHUS_STALE_MS = 200L;
    public static final double VARASUCHUS_CAPTURE_DISTANCE = 80.0;
    public static final float VARASUCHUS_YAW_OFFSET_DEG = -180.0f;

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
        riderConfigs.put(Ignivorus.class, new RiderSpec(
                IGNIVORUS_BONE,
                new Vector3f(IGNIVORUS_SEAT_X, IGNIVORUS_SEAT_Y, IGNIVORUS_SEAT_Z),
                new Vector3f(IGNIVORUS_FIRST_PERSON_X, IGNIVORUS_FIRST_PERSON_Y, IGNIVORUS_FIRST_PERSON_Z),
                IGNIVORUS_STALE_MS,
                IGNIVORUS_CAPTURE_DISTANCE,
                IGNIVORUS_YAW_OFFSET_DEG
        ));
        RiderSpec cindervaneSpec = new RiderSpec(
                CINDERVANE_SEAT0_BONE,
                new Vector3f(CINDERVANE_SEAT0_X, CINDERVANE_SEAT0_Y, CINDERVANE_SEAT0_Z),
                new Vector3f(CINDERVANE_SEAT0_FIRST_PERSON_X, CINDERVANE_SEAT0_FIRST_PERSON_Y, CINDERVANE_SEAT0_FIRST_PERSON_Z),
                CINDERVANE_STALE_MS,
                CINDERVANE_CAPTURE_DISTANCE,
                CINDERVANE_SEAT0_YAW_OFFSET_DEG
        );
        cindervaneSpec.setSeat(1, new SeatSpec(
                CINDERVANE_SEAT1_BONE,
                new Vector3f(CINDERVANE_SEAT1_X, CINDERVANE_SEAT1_Y, CINDERVANE_SEAT1_Z),
                CINDERVANE_SEAT1_YAW_OFFSET_DEG,
                new Vector3f(CINDERVANE_SEAT1_FIRST_PERSON_X, CINDERVANE_SEAT1_FIRST_PERSON_Y, CINDERVANE_SEAT1_FIRST_PERSON_Z)
        ));
        riderConfigs.put(Cindervane.class, cindervaneSpec);
        riderConfigs.put(Stegonaut.class, new RiderSpec(
                STEGONAUT_BONE,
                new Vector3f(STEGONAUT_SEAT_X, STEGONAUT_SEAT_Y, STEGONAUT_SEAT_Z),
                new Vector3f(STEGONAUT_FIRST_PERSON_X, STEGONAUT_FIRST_PERSON_Y, STEGONAUT_FIRST_PERSON_Z),
                STEGONAUT_STALE_MS,
                STEGONAUT_CAPTURE_DISTANCE,
                STEGONAUT_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Volitans.class, new RiderSpec(
                VOLITANS_BONE,
                new Vector3f(VOLITANS_SEAT_X, VOLITANS_SEAT_Y, VOLITANS_SEAT_Z),
                new Vector3f(VOLITANS_FIRST_PERSON_X, VOLITANS_FIRST_PERSON_Y, VOLITANS_FIRST_PERSON_Z),
                VOLITANS_STALE_MS,
                VOLITANS_CAPTURE_DISTANCE,
                VOLITANS_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Nulljaw.class, new RiderSpec(
                NULLJAW_BONE,
                new Vector3f(NULLJAW_SEAT_X, NULLJAW_SEAT_Y, NULLJAW_SEAT_Z),
                new Vector3f(NULLJAW_FIRST_PERSON_X, NULLJAW_FIRST_PERSON_Y, NULLJAW_FIRST_PERSON_Z),
                NULLJAW_STALE_MS,
                NULLJAW_CAPTURE_DISTANCE,
                NULLJAW_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Varasuchus.class, new RiderSpec(
                VARASUCHUS_BONE,
                new Vector3f(VARASUCHUS_SEAT_X, VARASUCHUS_SEAT_Y, VARASUCHUS_SEAT_Z),
                new Vector3f(VARASUCHUS_FIRST_PERSON_X, VARASUCHUS_FIRST_PERSON_Y, VARASUCHUS_FIRST_PERSON_Z),
                VARASUCHUS_STALE_MS,
                VARASUCHUS_CAPTURE_DISTANCE,
                VARASUCHUS_YAW_OFFSET_DEG
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


    public static Vector3f getSeatOffset(Object dragon, int seatIndex) {
        return new Vector3f(getOrDefaultSpec(dragon).getSeatSpec(seatIndex).offset);
    }

    public static float getYawOffset(Object dragon, int seatIndex) {
        return getOrDefaultSpec(dragon).getSeatSpec(seatIndex).yawOffsetDeg;
    }

    public static String getSeatBoneName(Object dragon, int seatIndex) {
        return getOrDefaultSpec(dragon).getSeatSpec(seatIndex).boneName;
    }

    public static Vector3f getFirstPersonOffset(Object dragon, int seatIndex) {
        return new Vector3f(getOrDefaultSpec(dragon).getSeatSpec(seatIndex).firstPersonOffset);
    }

    public static final class RiderSpec {
        public final String boneName;
        public final Vector3f offset;
        public final Vector3f firstPersonOffset;
        public final long staleMs;
        public final double maxCaptureDistance;
        public final float yawOffsetDeg;
        private final Map<Integer, SeatSpec> seatSpecs = new HashMap<>();

        public RiderSpec(String boneName, Vector3f offset, Vector3f firstPersonOffset, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            this.boneName = boneName;
            this.offset = offset;
            this.firstPersonOffset = firstPersonOffset;
            this.staleMs = staleMs;
            this.maxCaptureDistance = maxCaptureDistance;
            this.yawOffsetDeg = yawOffsetDeg;
            seatSpecs.put(0, new SeatSpec(boneName, new Vector3f(offset), yawOffsetDeg, new Vector3f(firstPersonOffset)));
        }

        public RiderSpec(String boneName, float x, float y, float z, float fpX, float fpY, float fpZ, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            this(boneName, new Vector3f(x, y, z), new Vector3f(fpX, fpY, fpZ), staleMs, maxCaptureDistance, yawOffsetDeg);
        }

        public RiderSpec(String boneName) {
            this(boneName, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 200L, 80.0, -180.0f);
        }

        public SeatSpec getSeatSpec(int seatIndex) {
            return seatSpecs.getOrDefault(seatIndex, seatSpecs.get(0));
        }

        public void setSeat(int seatIndex, SeatSpec spec) {
            seatSpecs.put(seatIndex, spec);
        }
    }

    public static final class SeatSpec {
        public final String boneName;
        public final Vector3f offset;
        public final float yawOffsetDeg;
        public final Vector3f firstPersonOffset;


        public SeatSpec(String boneName, Vector3f offset, float yawOffsetDeg, Vector3f firstPersonOffset) {
            this.boneName = boneName;
            this.offset = offset;
            this.yawOffsetDeg = yawOffsetDeg;
            this.firstPersonOffset = firstPersonOffset;
        }
    }
}
