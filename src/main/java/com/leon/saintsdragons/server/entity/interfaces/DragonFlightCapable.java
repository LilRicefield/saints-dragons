package com.leon.saintsdragons.server.entity.interfaces;

/**
 * Interface for dragons that can fly.
 * Defines the minimum requirements for flight behaviors.
 */
public interface DragonFlightCapable {
    
    /**
     * Check if the dragon is currently flying
     */
    boolean isFlying();
    
    /**
     * Set the dragon's flying state
     */
    void setFlying(boolean flying);
    
    /**
     * Check if the dragon is taking off
     */
    boolean isTakeoff();
    
    /**
     * Set the dragon's takeoff state
     */
    void setTakeoff(boolean takeoff);
    
    /**
     * Check if the dragon is hovering
     */
    boolean isHovering();
    
    /**
     * Set the dragon's hovering state
     */
    void setHovering(boolean hovering);
    
    /**
     * Check if the dragon is landing
     */
    boolean isLanding();
    
    /**
     * Set the dragon's landing state
     */
    void setLanding(boolean landing);
    
    /**
     * Get the dragon's flight speed multiplier
     */
    float getFlightSpeed();
    
    /**
     * Get the dragon's preferred flight altitude
     */
    double getPreferredFlightAltitude();
    
    /**
     * Check if the dragon can take off from current position
     */
    boolean canTakeoff();
    
    /**
     * Mark that the dragon has just landed
     */
    void markLandedNow();
}
