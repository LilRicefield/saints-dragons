package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class DragonForwardMovementComponent {
    private static final double MAX_MOVE_STEP = 0.45D;

    private final DragonEntity dragon;
    private final StateAccess state;
    private int dashCooldownTicks;
    private int elapsedTicks;
    private Vec3 lastVelocity = Vec3.ZERO;

    public DragonForwardMovementComponent(DragonEntity dragon, StateAccess state) {
        this.dragon = dragon;
        this.state = state;
    }

    public void startDodge(Vec3 velocity, int durationTicks) {
        start(velocity, durationTicks);
    }

    public boolean startDash(Vec3 velocity, int durationTicks, int cooldownTicks) {
        return startDash(velocity, durationTicks, cooldownTicks, 1.0D);
    }

    public boolean startDash(Vec3 velocity, int durationTicks, int cooldownTicks, double horizontalDrag) {
        if (dashCooldownTicks > 0) {
            return false;
        }
        return start(velocity, durationTicks, cooldownTicks, true, false, horizontalDrag);
    }

    private void start(Vec3 velocity, int durationTicks) {
        start(velocity, durationTicks, 0, false, true, 1.0D);
    }

    private boolean start(Vec3 velocity, int durationTicks, int cooldownTicks, boolean dash, boolean dodge, double horizontalDrag) {
        if (isActive() || velocity.lengthSqr() < 1.0E-6D) {
            return false;
        }
        int ticks = Math.max(1, durationTicks);
        this.elapsedTicks = 0;
        this.dashCooldownTicks = Math.max(this.dashCooldownTicks, cooldownTicks);
        this.lastVelocity = velocity;
        state.start(ticks, velocity, dash, dodge, clampDrag(horizontalDrag));
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(dragon.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        dragon.hasImpulse = true;
        dragon.hurtMarked = true;
        return true;
    }

    public void tickServerState() {
        tickCooldown();
        if (!isActive()) {
            elapsedTicks = 0;
            return;
        }
        elapsedTicks++;
        int ticks = state.ticks() - 1;
        if (ticks <= 0) {
            clearActive();
        } else {
            state.setTicks(ticks);
        }
    }

    public void tickCooldown() {
        if (dashCooldownTicks > 0) {
            dashCooldownTicks--;
        }
    }

    public void tickState() {
        tickServerState();
    }

    public int getCooldownTicks() {
        return dashCooldownTicks;
    }

    public int getDashCooldownTicks() {
        return dashCooldownTicks;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public Vec3 getLastVelocity() {
        return lastVelocity;
    }

    public void clear() {
        clearActive();
        dashCooldownTicks = 0;
        elapsedTicks = 0;
    }

    public void cancelActive() {
        clearActive();
        elapsedTicks = 0;
    }

    public boolean isActive() {
        return state.ticks() > 0;
    }

    public void applyTravelMotion() {
        if (!isActive()) {
            return;
        }
        Vec3 nudge = state.velocity();
        double currentDrag = state.horizontalDrag();
        lastVelocity = nudge;
        moveInStepFriendlySlices(nudge);
        Vec3 currentMotion = dragon.getDeltaMovement();
        if (currentDrag < 1.0D) {
            dragon.setDeltaMovement(0.0D, currentMotion.y, 0.0D);
            state.setVelocity(nudge.multiply(currentDrag, 1.0D, currentDrag));
        } else {
            dragon.setDeltaMovement(nudge.x, currentMotion.y, nudge.z);
        }
        dragon.hasImpulse = true;
        dragon.hurtMarked = true;
    }

    private void moveInStepFriendlySlices(Vec3 nudge) {
        double horizontalLength = Math.sqrt(nudge.x * nudge.x + nudge.z * nudge.z);
        int slices = Math.max(1, (int) Math.ceil(horizontalLength / MAX_MOVE_STEP));
        Vec3 slice = nudge.scale(1.0D / slices);
        for (int i = 0; i < slices; i++) {
            dragon.move(MoverType.SELF, slice);
        }
    }

    private void clearActive() {
        state.clear();
    }

    private static double clampDrag(double horizontalDrag) {
        return Math.max(0.0D, Math.min(1.0D, horizontalDrag));
    }

    public interface StateAccess {
        void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag);

        int ticks();

        void setTicks(int ticks);

        Vec3 velocity();

        void setVelocity(Vec3 velocity);

        double horizontalDrag();

        void clear();
    }
}
