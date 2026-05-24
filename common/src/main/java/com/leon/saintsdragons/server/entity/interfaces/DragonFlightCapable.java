package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.animal.FlyingAnimal;

public interface DragonFlightCapable {
    default boolean isFlying() {
        if (this instanceof RideableDragonBase rideable) {
            return rideable.isFlying();
        }
        if (this instanceof FlyingAnimal flyingAnimal) {
            return flyingAnimal.isFlying();
        }
        return false;
    }
    void setFlying(boolean flying);
    boolean isTakeoff();
    void setTakeoff(boolean takeoff);
    boolean isHovering();
    void setHovering(boolean hovering);
    boolean isLanding();
    void setLanding(boolean landing);
    float getFlightSpeed();
    double getPreferredFlightAltitude();
    boolean canTakeoff();
    void startTakeoffSequence(double minUpwardVelocity, int animationTicks);
    void markLandedNow();
    default void beginAiTakeoff(int animationTicks) {
        if (this instanceof RideableDragonBase rideable) {
            rideable.setGoingUp(true);
            rideable.setGoingDown(false);
        }
        startTakeoffSequence(0.12D, animationTicks);
    }
    default void beginAiFlight() {
        setFlying(true);
        boolean takeoffActive = isTakeoff();
        if (!takeoffActive) {
            setTakeoff(false);
        }
        setLanding(false);
        setHovering(false);
        if (this instanceof RideableDragonBase rideable) {
            if (!takeoffActive) {
                rideable.setGoingUp(false);
            }
            rideable.setGoingDown(false);
        }
    }
    default void beginAiLanding() {
        setTakeoff(false);
        setHovering(false);
        setLanding(true);
        setFlying(false);
        if (this instanceof RideableDragonBase rideable) {
            rideable.setGoingUp(false);
            rideable.setGoingDown(false);
        }
    }
}
