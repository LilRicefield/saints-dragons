package com.leon.saintsdragons.server.flight;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DragonRiderFlight {

    public interface Host {
        Entity asEntity();
        Level level();
        AABB getBoundingBox();
        boolean isVehicle();
        boolean isFlying();
        boolean isTakeoff();
        boolean isGoingUp();
        boolean isUnderWater();
        boolean isInWaterOrBubble();
        boolean isTame();
        boolean hasControllingRider();
        boolean canTakeoff();
        void setFlying(boolean value);
        void setHovering(boolean value);
        void setLanding(boolean value);
        void switchToAirNavigation();
        void setGoingUp(boolean value);
        void setGoingDown(boolean value);
        void stopNavigation();
        void startTakeoffSequence(double minUpwardVelocity, int animationTicks);
        Vec3 getDeltaMovement();
        void setDeltaMovement(Vec3 movement);
        void markImpulse();
        long getGameTime();
        default long getLastLandingGameTime() {
            return Long.MIN_VALUE;
        }
        default boolean isTakeoffLocked() {
            return false;
        }
        default void onManualTakeoffStart() {
        }

        default void setRiderTakeoffTicks(int ticks) {
        }
    }

    public record Config(
            boolean allowWaterBreachTakeoff,
            int landingCooldownTicks,
            double manualUpwardMin,
            int riderTakeoffTicksOnManual,
            double breachUpwardMin,
            int riderTakeoffTicksOnBreach
    ) {
        public Config {
            if (landingCooldownTicks < 0) {
                throw new IllegalArgumentException("landingCooldownTicks must be >= 0");
            }
            if (manualUpwardMin < 0.0D) {
                throw new IllegalArgumentException("manualUpwardMin must be >= 0");
            }
            if (riderTakeoffTicksOnManual < 0) {
                throw new IllegalArgumentException("riderTakeoffTicksOnManual must be >= 0");
            }
            if (breachUpwardMin < 0.0D) {
                throw new IllegalArgumentException("breachUpwardMin must be >= 0");
            }
            if (riderTakeoffTicksOnBreach < 0) {
                throw new IllegalArgumentException("riderTakeoffTicksOnBreach must be >= 0");
            }
        }
    }

    private final Host host;
    private final Config config;

    public DragonRiderFlight(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    public boolean shouldPreserveWaterFlightState(int riderTakeoffTicks) {
        return host.isVehicle()
                && (host.isTakeoff()
                || riderTakeoffTicks > 0
                || (host.isGoingUp() && !host.isUnderWater()));
    }

    public boolean shouldClearFlightStateInWater(int riderTakeoffTicks) {
        return host.isFlying() && !shouldPreserveWaterFlightState(riderTakeoffTicks);
    }

    public boolean requestRiderTakeoff() {
        if (host.level().isClientSide) {
            return false;
        }
        if (!host.isTame() || !host.hasControllingRider() || host.isFlying()) {
            return false;
        }
        if (host.isTakeoffLocked()) {
            return false;
        }
        if (config.landingCooldownTicks() > 0) {
            long lastLanding = host.getLastLandingGameTime();
            if (lastLanding != Long.MIN_VALUE && (host.getGameTime() - lastLanding) < config.landingCooldownTicks()) {
                return false;
            }
        }

        boolean breachAttempt = config.allowWaterBreachTakeoff()
                && host.isInWaterOrBubble()
                && !host.isUnderWater();
        if (!host.canTakeoff() && !breachAttempt) {
            return false;
        }

        host.stopNavigation();
        host.setGoingDown(false);
        host.setGoingUp(true);
        host.onManualTakeoffStart();
        applyTakeoffState(config.manualUpwardMin(), config.riderTakeoffTicksOnManual());
        return true;
    }

    public boolean tryAutoBreachTakeoff() {
        if (host.level().isClientSide) {
            return false;
        }
        if (host.isFlying()) {
            return false;
        }
        if (!host.isInWaterOrBubble() || host.isUnderWater()) {
            return false;
        }
        if (!host.isGoingUp()) {
            return false;
        }
        if (!hasBreachTakeoffClearance()) {
            return false;
        }
        applyTakeoffState(config.breachUpwardMin(), config.riderTakeoffTicksOnBreach());
        return true;
    }

    public boolean hasBreachTakeoffClearance() {
        AABB liftedBody = host.getBoundingBox().move(0.0D, 1.0D, 0.0D);
        return host.level().noCollision(host.asEntity(), liftedBody);
    }

    private void applyTakeoffState(double minUpwardVelocity, int riderTakeoffTicks) {
        host.startTakeoffSequence(minUpwardVelocity, riderTakeoffTicks);

        if (riderTakeoffTicks > 0) {
            host.setRiderTakeoffTicks(riderTakeoffTicks);
        }
    }
}