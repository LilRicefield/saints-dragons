package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.animal.FlyingAnimal;

/**
 * Interface for dragons that can fly.
 * Defines the minimum requirements for flight behaviors.
 */
public interface DragonFlightCapable {
    
    /**
     * Check if the wyvern is currently flying
     */
    /**
     * Check if the wyvern is currently flying.
     * Defaulted to guard against old binaries that may not have compiled against this method.
     */
    default boolean isFlying() {
        if (this instanceof RideableDragonBase rideable) {
            return rideable.isFlying();
        }
        if (this instanceof FlyingAnimal flyingAnimal) {
            return flyingAnimal.isFlying();
        }
        return false;
    }
    
    /**
     * Set the wyvern's flying state
     */
    void setFlying(boolean flying);
    
    /**
     * Check if the wyvern is taking off
     */
    boolean isTakeoff();
    
    /**
     * Set the wyvern's takeoff state
     */
    void setTakeoff(boolean takeoff);
    
    /**
     * Check if the wyvern is hovering
     */
    boolean isHovering();
    
    /**
     * Set the wyvern's hovering state
     */
    void setHovering(boolean hovering);
    
    /**
     * Check if the wyvern is landing
     */
    boolean isLanding();
    
    /**
     * Set the wyvern's landing state
     */
    void setLanding(boolean landing);
    
    /**
     * Get the wyvern's flight speed multiplier
     */
    float getFlightSpeed();
    
    /**
     * Get the wyvern's preferred flight altitude
     */
    double getPreferredFlightAltitude();
    
    /**
     * Check if the wyvern can take off from current position
     */
    boolean canTakeoff();

    /**
     * Trigger the dragon's takeoff sequence.
     */
    void startTakeoffSequence(double minUpwardVelocity, int animationTicks);

    /**
     * Mark that the wyvern has just landed
     */
    void markLandedNow();

    default void beginAiTakeoff(int animationTicks) {
        if (this instanceof RideableDragon rideable) {
            rideable.setGoingUp(true);
            rideable.setGoingDown(false);
        }
        startTakeoffSequence(0.12D, animationTicks);
    }

    default void beginAiFlight() {
        setFlying(true);
        setTakeoff(false);
        setLanding(false);
        setHovering(false);
        if (this instanceof RideableDragon rideable) {
            rideable.setGoingUp(false);
            rideable.setGoingDown(false);
        }
    }

    default void beginAiLanding() {
        setTakeoff(false);
        setHovering(false);
        setLanding(true);
        setFlying(false);
        if (this instanceof RideableDragon rideable) {
            rideable.setGoingUp(false);
            rideable.setGoingDown(false);
        }
    }
}
