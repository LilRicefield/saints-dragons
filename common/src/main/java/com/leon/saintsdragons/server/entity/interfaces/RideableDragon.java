package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface RideableDragon {

    void setLastRiderForward(float forward);
    void setLastRiderStrafe(float strafe);
    int getGroundMoveState();
    int getSyncedFlightMode();
    int getEffectiveGroundState();
    boolean isGoingUp();
    void setGoingUp(boolean goingUp);
    boolean isGoingDown();
    void setGoingDown(boolean goingDown);
    boolean isAccelerating();
    void setAccelerating(boolean accelerating);
    void syncAnimState(int groundState, int flightMode);
    void initializeAnimationState();
    void resetAnimationState();
    @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn);
    void removePassenger(@NotNull net.minecraft.world.entity.Entity passenger);
    void tickAnimationStates();
}
