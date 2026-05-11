package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;

import java.util.EnumSet;

public interface DragonMovementCapable {
    EnumSet<DragonMovementCapability> movementCapabilities();

    default boolean hasMovementCapability(DragonMovementCapability capability) {
        return movementCapabilities().contains(capability);
    }

    default boolean canWalk() {
        return hasMovementCapability(DragonMovementCapability.WALK);
    }

    default boolean canFly() {
        return hasMovementCapability(DragonMovementCapability.FLY);
    }

    default boolean canSwim() {
        return hasMovementCapability(DragonMovementCapability.SWIM);
    }

    default boolean isAerial() {
        return this instanceof RideableDragonBase dragon
                && (dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering());
    }

    default boolean isGroundedForAi() {
        return this instanceof RideableDragonBase dragon && dragon.onGround() && !isAerial();
    }

    default boolean isGroundedForTeleport() {
        return isGroundedForAi();
    }

    default void clearAerialState() {
        if (this instanceof DragonFlightCapable flightCapable) {
            flightCapable.setFlying(false);
            flightCapable.setTakeoff(false);
            flightCapable.setLanding(false);
            flightCapable.setHovering(false);
        }
    }
}
