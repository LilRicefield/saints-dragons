package com.leon.saintsdragons.server.flight;

import net.minecraft.world.phys.Vec3;

/**
 * Stable neutral rider anchors for server side positioning (raevyx's rider got shot up 9 blocks above lmao)
 */

public final class DragonRiderSeatOffsets {
    // passengerBone pivot 0, 66, -49.5 plus RiderConfig correction 0, -0.595, 0.59375
    public static final Vec3 RAEVYX = fromGeckoModelSpace(0.0D, 3.53D, -2.5D);
    // passengerBone pivot -0.06603, 94.45, -211.05 plus its RiderConfig correction
    public static final Vec3 IGNIVORUS = fromGeckoModelSpace(0.0D, 5.20D, -12.5D);
    // passengerBone pivot 0, 34, -29 plus RiderConfig correction 0, -0.575, 0.0625
    public static final Vec3 STEGONAUT = fromGeckoModelSpace(0.0D, 1.55D, -1.75D);
    // passengerBone pivot 0, 47, -46 plus RiderConfig correction [0, -0.5375, 0.725].
    public static final Vec3 VOLITANS = fromGeckoModelSpace(0.0D, 2.40D, -2.15D);
    // passengerBone pivot 0, 53.56904, -55.70184 plus its RiderConfig correction.
    public static final Vec3 VARASUCHUS = fromGeckoModelSpace(0.0D, 2.78D, -3.25D);
    // passengerBone pivot 0, 71.5, -13 plus RiderConfig correction 0, -0.66875, 0.2625]
    public static final Vec3 ATROXIIA = fromGeckoModelSpace(0.0D, 3.80D, -0.55D);

    // passengerBone1/2 pivots plus their respective RiderConfig corrections
    private static final Vec3 CINDERVANE_SEAT_0 = fromGeckoModelSpace(0.0D, 2.0D, -1.0D);
    private static final Vec3 CINDERVANE_SEAT_1 = fromGeckoModelSpace(0.0D, 2.0D, 0.25D);

    private DragonRiderSeatOffsets() {
    }

    public static Vec3 cindervane(int seatIndex) {
        return seatIndex == 1 ? CINDERVANE_SEAT_1 : CINDERVANE_SEAT_0;
    }

    private static Vec3 fromGeckoModelSpace(double x, double y, double z) {
        return new Vec3(-x, y, -z);
    }
}
