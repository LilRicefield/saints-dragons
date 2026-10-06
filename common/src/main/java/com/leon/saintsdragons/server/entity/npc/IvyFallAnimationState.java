package com.leon.saintsdragons.server.entity.npc;

import net.minecraft.util.Mth;

public final class IvyFallAnimationState {
    private static final float FALL_START_DROP = 0.35F;
    private static final float FULL_FALL_DROP = 3.5F;
    private static final float FALL_RESPONSE = 0.4F;
    private static final float MIN_IMPACT_DROP = 0.75F;
    private static final float MIN_IMPACT_SPEED = 0.22F;
    private static final float SMALL_IMPACT_MAX_SPEED = 0.42F;
    private static final float FULL_LIGHT_SPEED = 0.65F;
    private static final float HEAVY_START_SPEED = 0.65F;
    private static final float FULL_HEAVY_SPEED = 1.1F;
    private static final float SMALL_IMPACT_MIN_WEIGHT = 0.2F;
    private static final float SMALL_IMPACT_MAX_WEIGHT = 0.35F;
    private static final float SMALL_COMPRESSION_TICKS = 1.0F;
    private static final float SMALL_RECOVERY_TICKS = 4.0F;
    private static final float SMALL_DURATION_TICKS = 5.0F;
    private static final float LAND_ENTRY_TICKS = 1.5F;
    private static final float LIGHT_COMPRESSION_TICKS = 0.125F * 20.0F;
    private static final float HEAVY_COMPRESSION_TICKS = 0.25F * 20.0F;
    private static final float LIGHT_RECOVERY_TICKS = 0.5F * 20.0F;
    private static final float HEAVY_RECOVERY_TICKS = 0.625F * 20.0F;
    private static final float LIGHT_DURATION_TICKS = 0.625F * 20.0F;
    private static final float HEAVY_DURATION_TICKS = 0.9583F * 20.0F;

    private boolean airborne;
    private double highestY;
    private int descendingTicks;
    private float falling;
    private float previousFalling;
    private float fallTime;
    private float previousFallTime;
    private long lastLandingTick = -1L;
    private long activeLandingTick = -1L;
    private boolean landing;
    private float landingAge;
    private float previousLandingAge;
    private float landingGate;
    private float previousLandingGate;
    private float landingStrength;
    private float lightShare;
    private float heavyShare;

    public static boolean isMeaningfulImpact(float distance, float downwardSpeed) {
        return distance >= MIN_IMPACT_DROP && downwardSpeed >= MIN_IMPACT_SPEED;
    }

    public void tick(IvyTheDragonMerchant ivy) {
        previousFalling = falling;
        previousFallTime = fallTime;
        previousLandingAge = landingAge;
        previousLandingGate = landingGate;
        double dx = ivy.getX() - ivy.xo;
        double dy = ivy.getY() - ivy.yo;
        double dz = ivy.getZ() - ivy.zo;
        if (ivy.tickCount <= 1 || dx * dx + dz * dz > 4.0D || Math.abs(dy) > 6.0D
                || !ivy.canBlendFallAndLanding()) {
            reset(ivy);
            return;
        }

        if (landing) {
            landingAge++;
            landingGate = Mth.lerp(0.5F, landingGate, ivy.onGround() ? 1.0F : 0.0F);
            if (landingAge >= landingDuration() + 1.0F) landing = false;
        }

        long landingTick = ivy.getLandingAnimationTick();
        if (landingTick != lastLandingTick) {
            float age = Math.max(0L, ivy.level().getGameTime() - landingTick);
            // Metadata can precede the position packet; give ground contact a few ticks to arrive.
            if (ivy.onGround() || age >= 4.0F) lastLandingTick = landingTick;
            if (landingTick >= 0L && ivy.onGround() && age < HEAVY_DURATION_TICKS) {
                float speed = ivy.getLandingAnimationSpeed();
                lightShare = smoothstep(SMALL_IMPACT_MAX_SPEED, FULL_LIGHT_SPEED, speed);
                heavyShare = smoothstep(HEAVY_START_SPEED, FULL_HEAVY_SPEED, speed);
                float smallStrength = Mth.lerp(smoothstep(MIN_IMPACT_SPEED, SMALL_IMPACT_MAX_SPEED, speed),
                        SMALL_IMPACT_MIN_WEIGHT, SMALL_IMPACT_MAX_WEIGHT);
                landingStrength = Mth.lerp(lightShare, smallStrength, 1.0F);
                landingAge = age;
                previousLandingAge = Math.max(0.0F, age - 1.0F);
                landingGate = previousLandingGate = 1.0F;
                landing = age < landingDuration();
                activeLandingTick = landingTick;
            }
        }

        float targetFall = 0.0F;
        if (!ivy.onGround()) {
            if (!airborne) {
                airborne = true;
                highestY = Math.max(ivy.yo, ivy.getY());
                descendingTicks = 0;
                if (falling == 0.0F && previousFalling == 0.0F) fallTime = previousFallTime = 0.0F;
            }
            highestY = Math.max(highestY, ivy.getY());
            float downwardSpeed = (float) Math.max(0.0D, -dy);
            descendingTicks = downwardSpeed > 0.01F ? descendingTicks + 1 : 0;
            if (descendingTicks >= 2) {
                float drop = (float) Math.max(0.0D, highestY - ivy.getY());
                targetFall = smoothstep(FALL_START_DROP, FULL_FALL_DROP, drop)
                        * smoothstep(0.02F, 0.18F, downwardSpeed);
            }
            fallTime++;
        } else {
            airborne = false;
            descendingTicks = 0;
        }
        falling = Mth.lerp(ivy.onGround() ? 0.55F : FALL_RESPONSE, falling, targetFall);
        if (falling < 0.0001F) falling = 0.0F;
    }

    private void reset(IvyTheDragonMerchant ivy) {
        airborne = landing = false;
        highestY = ivy.getY();
        descendingTicks = 0;
        falling = previousFalling = fallTime = previousFallTime = 0.0F;
        landingGate = previousLandingGate = 0.0F;
        activeLandingTick = -1L;
        // A newly tracked client may join a landing already in progress.
        lastLandingTick = ivy.tickCount <= 1 ? -1L : ivy.getLandingAnimationTick();
    }

    public boolean isFalling() {
        return airborne && (falling > 0.0001F || previousFalling > 0.0001F);
    }

    public float falling(float partialTick) {
        return Mth.lerp(partialTick, previousFalling, falling);
    }

    public float fallTime(float partialTick) {
        return Mth.lerp(partialTick, previousFallTime, fallTime);
    }

    public float heavyShare() {
        return heavyShare;
    }

    public float movingLegInfluence() {
        return Mth.lerp(heavyShare, Mth.lerp(lightShare, 0.15F, 0.35F), 0.8F);
    }

    public boolean isLanding() {
        return landing;
    }

    public long landingTick() {
        return activeLandingTick;
    }

    public float landingWeight(float partialTick) {
        if (!landing) return 0.0F;
        float age = Mth.lerp(partialTick, previousLandingAge, landingAge);
        float compression = landingCompression();
        float entry = smoothstep(0.0F, Math.min(LAND_ENTRY_TICKS, compression), age);
        float recovery = 1.0F - smoothstep(compression, landingDuration(), age);
        return landingStrength * entry * recovery * Mth.lerp(partialTick, previousLandingGate, landingGate);
    }

    public double landingSourceTick(float partialTick, boolean heavy, double clipLength) {
        double age = Mth.lerp(partialTick, previousLandingAge, landingAge);
        double compression = landingCompression();
        double recovery = Mth.lerp(heavyShare,
                Mth.lerp(lightShare, SMALL_RECOVERY_TICKS, LIGHT_RECOVERY_TICKS), HEAVY_RECOVERY_TICKS);
        // Retime the authored contact/compression/recovery stages together, even for a short step-down.
        double clipCompression = Math.min(clipLength, heavy ? HEAVY_COMPRESSION_TICKS : LIGHT_COMPRESSION_TICKS);
        double clipRecovery = Math.min(clipLength, heavy ? HEAVY_RECOVERY_TICKS : LIGHT_RECOVERY_TICKS);
        if (age <= compression) {
            return Mth.clampedLerp(0.0D, clipCompression, age / compression);
        }
        if (age <= recovery) {
            return Mth.clampedLerp(clipCompression, clipRecovery, (age - compression) / (recovery - compression));
        }
        return Mth.clampedLerp(clipRecovery, clipLength, (age - recovery) / (landingDuration() - recovery));
    }

    private float landingDuration() {
        return Mth.lerp(heavyShare,
                Mth.lerp(lightShare, SMALL_DURATION_TICKS, LIGHT_DURATION_TICKS), HEAVY_DURATION_TICKS);
    }

    private float landingCompression() {
        return Mth.lerp(heavyShare,
                Mth.lerp(lightShare, SMALL_COMPRESSION_TICKS, LIGHT_COMPRESSION_TICKS), HEAVY_COMPRESSION_TICKS);
    }

    private static float smoothstep(float low, float high, float value) {
        float weight = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return weight * weight * (3.0F - 2.0F * weight);
    }
}
