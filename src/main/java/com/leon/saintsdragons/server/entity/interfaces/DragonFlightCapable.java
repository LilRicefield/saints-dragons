package com.leon.saintsdragons.server.entity.interfaces;

/**
 * Interface for dragons that can fly.
 * Defines the minimum requirements for flight behaviors.
 */
public interface DragonFlightCapable {
    
    /**
     * Check if the wyvern is currently flying
     */
    boolean isFlying();
    
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
     * Mark that the wyvern has just landed
     */
    void markLandedNow();
}
