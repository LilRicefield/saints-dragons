package com.leon.saintsdragons.server.entity.component;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared transient takeoff sequencer for rideable flying dragons.
 * Owns the short-lived animation window and lift assist without persisting
 * dragon-specific takeoff timers to NBT.
 */
public final class DragonTakeoffComponent {

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
    }

    private final Host host;
    private int ticksRemaining;
    private double sustainUpwardVelocity;

    public DragonTakeoffComponent(Host host) {
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
        this.ticksRemaining = clampedTicks;
        this.sustainUpwardVelocity = Math.max(0.0D, minUpwardVelocity);

        host.setTakeoff(clampedTicks > 0);
        host.setFlying(true);
        host.setHovering(false);
        host.setLanding(false);
        host.switchToAirNavigation();
        host.onTakeoffStarted();
        applyLiftFloor(this.sustainUpwardVelocity);

        if (clampedTicks == 0) {
            clear();
        }
    }

    public void tick() {
        if (host.level().isClientSide || ticksRemaining <= 0) {
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
        sustainUpwardVelocity = 0.0D;
        host.setTakeoff(false);
        if (wasActive) {
            host.onTakeoffEnded();
        }
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
