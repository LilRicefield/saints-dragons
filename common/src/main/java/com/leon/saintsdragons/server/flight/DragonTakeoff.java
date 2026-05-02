package com.leon.saintsdragons.server.flight;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


public final class DragonTakeoff {

    public interface Host {
        Level level();
        boolean isFlying();
        void setFlying(boolean value);
        void setTakeoff(boolean value);
        void setHovering(boolean value);
        void setLanding(boolean value);
        void switchToAirNavigation();
        Vec3 getDeltaMovement();
        void setDeltaMovement(Vec3 movement);
        void markImpulse();
        default void onTakeoffStarted() {
        }
        default void onTakeoffEnded() {
        }
        default int getTakeoffLiftDelayTicks() {
            return 0;
        }
    }

    private final Host host;
    private int ticksRemaining;
    private int liftDelayTicksRemaining;
    private double sustainUpwardVelocity;
    private boolean launched;

    public DragonTakeoff(Host host) {
        this.host = host;
    }

    public boolean isActive() {
        return ticksRemaining > 0;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void startTakeoff(int animationTicks, double minUpwardVelocity) {
        int clampedTicks = Math.max(0, animationTicks);
        int requestedLiftDelay = Math.max(0, host.getTakeoffLiftDelayTicks());
        this.ticksRemaining = clampedTicks;
        this.liftDelayTicksRemaining = Math.min(requestedLiftDelay, Math.max(0, clampedTicks - 1));
        this.sustainUpwardVelocity = Math.max(0.0D, minUpwardVelocity);
        this.launched = false;

        host.setTakeoff(clampedTicks > 0);
        host.setHovering(false);
        host.setLanding(false);
        host.onTakeoffStarted();

        if (this.liftDelayTicksRemaining <= 0) {
            launch();
        }

        if (clampedTicks == 0) {
            clear();
        }
    }

    public void tick() {
        if (host.level().isClientSide || ticksRemaining <= 0) {
            return;
        }
        if (!launched) {
            ticksRemaining--;
            liftDelayTicksRemaining--;
            if (ticksRemaining <= 0) {
                clear();
                return;
            }
            if (liftDelayTicksRemaining <= 0) {
                launch();
            }
            return;
        }

        if (!host.isFlying()) {
            clear();
            return;
        }

        applyLiftFloor(sustainUpwardVelocity);
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            clear();
        }
    }

    public void clear() {
        boolean wasActive = ticksRemaining > 0 || host.isFlying();
        ticksRemaining = 0;
        liftDelayTicksRemaining = 0;
        sustainUpwardVelocity = 0.0D;
        launched = false;
        host.setTakeoff(false);
        if (wasActive) {
            host.onTakeoffEnded();
        }
    }

    private void launch() {
        launched = true;
        host.setFlying(true);
        host.switchToAirNavigation();
        applyLiftFloor(this.sustainUpwardVelocity);
    }

    private void applyLiftFloor(double minUpwardVelocity) {
        Vec3 current = host.getDeltaMovement();
        if (current.y >= minUpwardVelocity) {
            return;
        }
        host.setDeltaMovement(new Vec3(current.x, minUpwardVelocity, current.z));
        host.markImpulse();
    }
}