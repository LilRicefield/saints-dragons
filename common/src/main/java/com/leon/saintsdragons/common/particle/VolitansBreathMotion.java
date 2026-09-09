package com.leon.saintsdragons.common.particle;

public final class VolitansBreathMotion {
    public static final double SPEED = 1.6;
    public static final double DRAG = 0.997;
    public static final int LIFETIME = 28;
    public static final double SPREAD = 0.20;
    public static final int PARTICLES_PER_SECTION = 8;
    public static final double RANGE = SPEED * (1.0 - Math.pow(DRAG, LIFETIME - 1)) / (1.0 - DRAG);

    private VolitansBreathMotion() {}
}
