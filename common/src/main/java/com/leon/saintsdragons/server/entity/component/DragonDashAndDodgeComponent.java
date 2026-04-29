package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DragonDashAndDodgeComponent {
    private final DragonEntity dragon;
    @Nullable
    private final Consumer<Boolean> activeStateSink;
    private boolean active;
    private int ticksLeft;
    private int elapsedTicks;
    private int cooldownTicks;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 lastVelocity = Vec3.ZERO;
    private MotionConfig config = MotionConfig.DEFAULT;

    public DragonDashAndDodgeComponent(DragonEntity dragon) {
        this(dragon, null);
    }

    public DragonDashAndDodgeComponent(DragonEntity dragon, @Nullable Consumer<Boolean> activeStateSink) {
        this.dragon = dragon;
        this.activeStateSink = activeStateSink;
    }

    public boolean start(Vec3 velocity, MotionConfig config) {
        if (active || cooldownTicks > 0 || velocity.lengthSqr() < 1.0E-6D) {
            return false;
        }
        this.config = config;
        this.active = true;
        this.ticksLeft = Math.max(1, config.durationTicks());
        this.elapsedTicks = 0;
        this.cooldownTicks = Math.max(0, config.cooldownTicks());
        this.velocity = velocity;
        this.lastVelocity = velocity;
        syncActiveState(true);
        dragon.setDeltaMovement(velocity);
        dragon.getNavigation().stop();
        dragon.hasImpulse = true;
        return true;
    }

    public void tickMovement() {
        if (!active) {
            return;
        }

        double yVelocity = config.useGroundBurstVertical()
                ? getGroundBurstVerticalVelocity()
                : dragon.getDeltaMovement().y;
        double horizontalX = velocity.x;
        double horizontalZ = velocity.z;
        if (!dragon.onGround()) {
            horizontalX *= config.airborneHorizontalScale();
            horizontalZ *= config.airborneHorizontalScale();
        }

        dragon.setDeltaMovement(horizontalX, yVelocity, horizontalZ);
        dragon.hasImpulse = true;
        velocity = velocity.multiply(config.horizontalDrag(), config.verticalDrag(), config.horizontalDrag());
        lastVelocity = velocity;

        if (--ticksLeft <= 0) {
            clearActive();
        }
    }

    public void tickState() {
        tickCooldown();
        if (active) {
            elapsedTicks++;
        } else {
            elapsedTicks = 0;
        }
    }

    public void tickCooldown() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public void clear() {
        clearActive();
        cooldownTicks = 0;
        elapsedTicks = 0;
    }

    public void cancelActive() {
        clearActive();
        elapsedTicks = 0;
    }

    public boolean isActive() {
        return active;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public Vec3 getLastVelocity() {
        return lastVelocity;
    }

    public static double speedForIntegratedDistance(double distance, double horizontalDrag, int durationTicks) {
        double dragScale = 1.0D - Math.pow(horizontalDrag, Math.max(1, durationTicks));
        if (dragScale <= 1.0E-6D) {
            return 0.0D;
        }
        return distance * (1.0D - horizontalDrag) / dragScale;
    }

    public static Vec3 horizontalForward(float yawDegrees) {
        float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians)).normalize();
    }

    public static Vec3 horizontalRight(float yawDegrees) {
        float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yawRadians), 0.0D, Mth.sin(yawRadians)).normalize();
    }

    private void clearActive() {
        active = false;
        ticksLeft = 0;
        velocity = Vec3.ZERO;
        syncActiveState(false);
    }

    private void syncActiveState(boolean active) {
        if (activeStateSink != null) {
            activeStateSink.accept(active);
        }
    }

    private double getGroundBurstVerticalVelocity() {
        double yVelocity = dragon.getDeltaMovement().y;
        if (!dragon.onGround()) {
            return Math.min((yVelocity - 0.22D) * 0.98D, -0.45D);
        }
        return Math.max(0.0D, yVelocity);
    }

    public record MotionConfig(
            int durationTicks,
            int cooldownTicks,
            double horizontalDrag,
            double verticalDrag,
            double airborneHorizontalScale,
            boolean useGroundBurstVertical
    ) {
        public static final MotionConfig DEFAULT = new MotionConfig(1, 0, 0.9D, 0.95D, 0.9D, true);
    }
}
