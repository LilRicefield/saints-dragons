package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.phys.AABB;

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

    default boolean isGroundedForAction() {
        if (!(this instanceof RideableDragonBase dragon)
                || dragon.getLocomotionMode() != DragonLocomotionMode.GROUND
                || isAerial()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()) {
            return false;
        }
        if (dragon.onGround()) {
            return true;
        }

        AABB bounds = dragon.getBoundingBox();
        double inset = Math.min(0.5D, Math.max(0.05D, dragon.getBbWidth() * 0.08D));
        AABB supportProbe = new AABB(
                bounds.minX + inset,
                bounds.minY - 0.35D,
                bounds.minZ + inset,
                bounds.maxX - inset,
                bounds.minY + 0.05D,
                bounds.maxZ - inset
        );
        return !dragon.level().noCollision(dragon, supportProbe);
    }

    default boolean isGroundedForAi() {
        return isGroundedForAction();
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
