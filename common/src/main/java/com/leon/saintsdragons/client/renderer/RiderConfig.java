package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;

public final class RiderConfig {

    // ===== RAEVYX TUNING =====
    public static final String RAEVYX_BONE = "passengerBone";
    public static final float RAEVYX_SEAT_X = 0.0f;
    public static final float RAEVYX_SEAT_Y = -0.595f;
    public static final float RAEVYX_SEAT_Z = 0.59375f;
    public static final float RAEVYX_YAW_OFFSET_DEG = -180.0f;

    // ===== IGNIVORUS TUNING =====
    public static final String IGNIVORUS_BONE = "passengerBone";
    public static final float IGNIVORUS_SEAT_X = -0.004126875f;
    public static final float IGNIVORUS_SEAT_Y = -0.703125f;
    public static final float IGNIVORUS_SEAT_Z = 0.690625f;
    public static final float IGNIVORUS_YAW_OFFSET_DEG = -180;

    // ===== CINDERVANE TUNING =====
    public static final String CINDERVANE_SEAT0_BONE = "passengerBone1";
    public static final String CINDERVANE_SEAT1_BONE = "passengerBone2";
    public static final float CINDERVANE_SEAT0_X = 0.0f;
    public static final float CINDERVANE_SEAT0_Y = -0.5194225f;
    public static final float CINDERVANE_SEAT0_Z = 0.45941937f;
    public static final float CINDERVANE_SEAT1_X = 0.0f;
    public static final float CINDERVANE_SEAT1_Y = -0.5194225f;
    public static final float CINDERVANE_SEAT1_Z = 0.9131694f;
    public static final float CINDERVANE_SEAT0_YAW_OFFSET_DEG = -180.0f;
    public static final float CINDERVANE_SEAT1_YAW_OFFSET_DEG = -180.0f;

    // ===== STEGONAUT TUNING =====
    public static final String STEGONAUT_BONE = "passengerBone";
    public static final float STEGONAUT_SEAT_X = 0.0f;
    public static final float STEGONAUT_SEAT_Y = -0.575f;
    public static final float STEGONAUT_SEAT_Z = 0.0625f;
    public static final float STEGONAUT_YAW_OFFSET_DEG = -180.0f;

    // ===== VOLITANS TUNING =====
    public static final String VOLITANS_BONE = "passengerBone";
    public static final float VOLITANS_SEAT_X = 0.0f;
    public static final float VOLITANS_SEAT_Y = -0.5375f;
    public static final float VOLITANS_SEAT_Z = 0.725f;
    public static final float VOLITANS_YAW_OFFSET_DEG = -180.0f;

    // ===== NULLJAW TUNING =====
    public static final String NULLJAW_BONE = "passengerBone";
    public static final float NULLJAW_SEAT_X = 0.0f;
    public static final float NULLJAW_SEAT_Y = -0.9f;
    public static final float NULLJAW_SEAT_Z = 0.14375f;
    public static final float NULLJAW_YAW_OFFSET_DEG = -180.0f;

    // ===== VARASUCHUS TUNING =====
    public static final String VARASUCHUS_BONE = "passengerBone";
    public static final float VARASUCHUS_SEAT_X = 0.0f;
    public static final float VARASUCHUS_SEAT_Y = -0.568065f;
    public static final float VARASUCHUS_SEAT_Z = 0.231365f;
    public static final float VARASUCHUS_YAW_OFFSET_DEG = -180.0f;

    // ===== ATROXIIA TUNING =====
    public static final String ATROXIIA_BONE = "passengerBone";
    public static final float ATROXIIA_SEAT_X = 0.0f;
    public static final float ATROXIIA_SEAT_Y = -0.66875f;
    public static final float ATROXIIA_SEAT_Z = 0.2625f;
    public static final float ATROXIIA_YAW_OFFSET_DEG = -180.0f;

    private static final Map<Class<?>, RiderSpec> RIDER_CONFIGS = new ConcurrentHashMap<>(createConfigs());

    private RiderConfig() {
    }

    private static Map<Class<?>, RiderSpec> createConfigs() {
        Map<Class<?>, RiderSpec> riderConfigs = new HashMap<>();
        riderConfigs.put(Raevyx.class, new RiderSpec(
                RAEVYX_BONE,
                new Vector3f(RAEVYX_SEAT_X, RAEVYX_SEAT_Y, RAEVYX_SEAT_Z),
                RAEVYX_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Ignivorus.class, new RiderSpec(
                IGNIVORUS_BONE,
                new Vector3f(IGNIVORUS_SEAT_X, IGNIVORUS_SEAT_Y, IGNIVORUS_SEAT_Z),
                IGNIVORUS_YAW_OFFSET_DEG
        ));
        RiderSpec cindervaneSpec = new RiderSpec(
                CINDERVANE_SEAT0_BONE,
                new Vector3f(CINDERVANE_SEAT0_X, CINDERVANE_SEAT0_Y, CINDERVANE_SEAT0_Z),
                CINDERVANE_SEAT0_YAW_OFFSET_DEG
        );
        cindervaneSpec.setSeat(1, new SeatSpec(
                CINDERVANE_SEAT1_BONE,
                new Vector3f(CINDERVANE_SEAT1_X, CINDERVANE_SEAT1_Y, CINDERVANE_SEAT1_Z),
                CINDERVANE_SEAT1_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Cindervane.class, cindervaneSpec);
        riderConfigs.put(Stegonaut.class, new RiderSpec(
                STEGONAUT_BONE,
                new Vector3f(STEGONAUT_SEAT_X, STEGONAUT_SEAT_Y, STEGONAUT_SEAT_Z),
                STEGONAUT_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Volitans.class, new RiderSpec(
                VOLITANS_BONE,
                new Vector3f(VOLITANS_SEAT_X, VOLITANS_SEAT_Y, VOLITANS_SEAT_Z),
                VOLITANS_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Nulljaw.class, new RiderSpec(
                NULLJAW_BONE,
                new Vector3f(NULLJAW_SEAT_X, NULLJAW_SEAT_Y, NULLJAW_SEAT_Z),
                NULLJAW_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Varasuchus.class, new RiderSpec(
                VARASUCHUS_BONE,
                new Vector3f(VARASUCHUS_SEAT_X, VARASUCHUS_SEAT_Y, VARASUCHUS_SEAT_Z),
                VARASUCHUS_YAW_OFFSET_DEG
        ));
        riderConfigs.put(Atroxiia.class, new RiderSpec(
                ATROXIIA_BONE,
                new Vector3f(ATROXIIA_SEAT_X, ATROXIIA_SEAT_Y, ATROXIIA_SEAT_Z),
                ATROXIIA_YAW_OFFSET_DEG
        ));
        riderConfigs.get(Nulljaw.class).withCamera(false);
        riderConfigs.replaceAll((type, spec) -> spec.snapshot());
        return riderConfigs;
    }

    public static void register(Class<? extends RideableDragonBase> dragonClass, RiderSpec spec) {
        Objects.requireNonNull(dragonClass, "dragonClass");
        RiderSpec snapshot = Objects.requireNonNull(spec, "spec").snapshot();
        if (RIDER_CONFIGS.putIfAbsent(dragonClass, snapshot) != null) {
            throw new IllegalArgumentException("Rider attachments already registered for " + dragonClass.getName());
        }
    }

    @Nullable
    public static RiderSpec getSpec(Object dragon) {
        if (dragon == null) {
            return null;
        }
        for (Class<?> type = dragon.getClass(); type != null; type = type.getSuperclass()) {
            RiderSpec spec = RIDER_CONFIGS.get(type);
            if (spec != null) {
                return spec;
            }
        }
        return null;
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

    /** Legacy registration metadata; cameras now use the seated player eye position. */
    @Deprecated
    public static Vector3f getFirstPersonOffset(Object dragon, int seatIndex) {
        return new Vector3f(getOrDefaultSpec(dragon).getSeatSpec(seatIndex).firstPersonOffset);
    }

    public static final class RiderSpec {
        public final String boneName;
        private final Vector3f offset;
        private final Vector3f firstPersonOffset;
        /** Legacy metadata; visible attachments have no distance cutoff or stale-pose fallback. */
        @Deprecated public final long staleMs;
        @Deprecated public final double maxCaptureDistance;
        public final float yawOffsetDeg;
        private final Map<Integer, SeatSpec> seatSpecs = new HashMap<>();
        private Map<Integer, SeatSpec> registeredSeats;
        private boolean frozen;
        private boolean cameraEnabled = true;
        private boolean rawGroundedCameraAnchor = true;
        private double groundedCameraLift = 1.2D;

        public RiderSpec(String boneName, Vector3f offset, float yawOffsetDeg) {
            this(boneName, offset, new Vector3f(), 0, Double.MAX_VALUE, yawOffsetDeg);
        }

        /** Retained for addon registrations; first-person offsets, age and distance no longer select attachment behavior. */
        @Deprecated
        public RiderSpec(String boneName, Vector3f offset, Vector3f firstPersonOffset, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            if (staleMs < 0L || !Double.isFinite(maxCaptureDistance) || maxCaptureDistance <= 0.0D) {
                throw new IllegalArgumentException("Invalid attachment lifetime or capture distance");
            }
            this.boneName = boneName;
            this.offset = new Vector3f(offset);
            this.firstPersonOffset = new Vector3f(firstPersonOffset);
            this.staleMs = staleMs;
            this.maxCaptureDistance = maxCaptureDistance;
            this.yawOffsetDeg = yawOffsetDeg;
            seatSpecs.put(0, new SeatSpec(boneName, new Vector3f(offset), yawOffsetDeg, new Vector3f(firstPersonOffset)));
        }

        @Deprecated
        public RiderSpec(String boneName, float x, float y, float z, float fpX, float fpY, float fpZ, long staleMs, double maxCaptureDistance, float yawOffsetDeg) {
            this(boneName, new Vector3f(x, y, z), new Vector3f(fpX, fpY, fpZ), staleMs, maxCaptureDistance, yawOffsetDeg);
        }

        public RiderSpec(String boneName) {
            this(boneName, new Vector3f(), -180.0F);
        }

        public SeatSpec getSeatSpec(int seatIndex) {
            return seatSpecs.getOrDefault(seatIndex, seatSpecs.get(0));
        }

        public RiderSpec setSeat(int seatIndex, SeatSpec spec) {
            checkMutable();
            if (seatIndex <= 0) {
                throw new IllegalArgumentException("Configure seat zero in the RiderSpec constructor");
            }
            seatSpecs.put(seatIndex, Objects.requireNonNull(spec, "spec"));
            return this;
        }

        public RiderSpec withLocator(int seatIndex, String locatorName, Vector3f modelPixelOffset) {
            checkMutable();
            SeatSpec seat = seatSpecs.get(seatIndex);
            if (seat == null) {
                throw new IllegalArgumentException("Unknown seat " + seatIndex);
            }
            seatSpecs.put(seatIndex, new SeatSpec(seat.boneName, seat.offset, seat.yawOffsetDeg,
                    seat.firstPersonOffset, locatorName, modelPixelOffset));
            return this;
        }

        public RiderSpec withCamera(boolean enabled) {
            checkMutable();
            cameraEnabled = enabled;
            return this;
        }

        /** Legacy camera offsets are retained as metadata; all enabled cameras share the seat pose. */
        @Deprecated
        public RiderSpec withCamera(boolean enabled, boolean rawGroundedAnchor, double groundedLift) {
            checkMutable();
            if (!Double.isFinite(groundedLift)) {
                throw new IllegalArgumentException("Camera lift must be finite");
            }
            cameraEnabled = enabled;
            rawGroundedCameraAnchor = rawGroundedAnchor;
            groundedCameraLift = groundedLift;
            return this;
        }

        public boolean cameraEnabled() {
            return cameraEnabled;
        }

        @Deprecated
        public boolean rawGroundedCameraAnchor() {
            return rawGroundedCameraAnchor;
        }

        @Deprecated
        public double groundedCameraLift() {
            return groundedCameraLift;
        }

        public Map<Integer, SeatSpec> seats() {
            return frozen ? registeredSeats : Map.copyOf(seatSpecs);
        }

        public int seatIndexForBone(String name) {
            for (Map.Entry<Integer, SeatSpec> seat : seatSpecs.entrySet()) {
                if (seat.getValue().boneName.equals(name)) {
                    return seat.getKey();
                }
            }
            return -1;
        }

        private void checkMutable() {
            if (frozen) {
                throw new IllegalStateException("Registered rider attachments are immutable");
            }
        }

        private RiderSpec snapshot() {
            RiderSpec copy = new RiderSpec(boneName, offset, firstPersonOffset,
                    staleMs, maxCaptureDistance, yawOffsetDeg);
            copy.seatSpecs.clear();
            copy.seatSpecs.putAll(seatSpecs);
            java.util.Set<String> bones = new java.util.HashSet<>();
            java.util.Set<String> locators = new java.util.HashSet<>();
            for (int index = 0; index < seatSpecs.size(); index++) {
                SeatSpec seat = seatSpecs.get(index);
                if (seat == null || !bones.add(seat.boneName)
                        || (seat.locatorName != null && !locators.add(seat.locatorName))) {
                    throw new IllegalArgumentException("Seats must be contiguous with unique bones and locators");
                }
            }
            copy.withCamera(cameraEnabled, rawGroundedCameraAnchor, groundedCameraLift);
            copy.registeredSeats = Map.copyOf(copy.seatSpecs);
            copy.frozen = true;
            return copy;
        }
    }

    public static final class SeatSpec {
        private final String boneName;
        private final Vector3f offset;
        private final float yawOffsetDeg;
        private final Vector3f firstPersonOffset;
        private final String locatorName;
        private final Vector3f locatorOffset;


        public SeatSpec(String boneName, Vector3f offset, float yawOffsetDeg) {
            this(boneName, offset, yawOffsetDeg, new Vector3f());
        }

        public SeatSpec(String boneName, Vector3f offset, float yawOffsetDeg, Vector3f firstPersonOffset) {
            this(boneName, offset, yawOffsetDeg, firstPersonOffset, null, new Vector3f());
        }

        public SeatSpec(String boneName, Vector3f offset, float yawOffsetDeg, Vector3f firstPersonOffset,
                        @Nullable String locatorName, Vector3f modelPixelOffset) {
            if (Objects.requireNonNull(boneName, "boneName").isBlank()
                    || !Float.isFinite(yawOffsetDeg) || (locatorName != null && locatorName.isBlank())) {
                throw new IllegalArgumentException("Invalid seat bone, locator, or yaw");
            }
            this.boneName = boneName;
            this.offset = finiteCopy(offset);
            this.yawOffsetDeg = yawOffsetDeg;
            this.firstPersonOffset = finiteCopy(firstPersonOffset);
            this.locatorName = locatorName;
            this.locatorOffset = finiteCopy(modelPixelOffset);
        }

        public String boneName() {
            return boneName;
        }

        @Nullable
        public String locatorName() {
            return locatorName;
        }

        public Vector3f locatorOffset() {
            return new Vector3f(locatorOffset);
        }

        private static Vector3f finiteCopy(Vector3f vector) {
            Objects.requireNonNull(vector, "offset");
            if (!Float.isFinite(vector.x()) || !Float.isFinite(vector.y()) || !Float.isFinite(vector.z())) {
                throw new IllegalArgumentException("Seat offsets must be finite");
            }
            return new Vector3f(vector);
        }
    }
}
