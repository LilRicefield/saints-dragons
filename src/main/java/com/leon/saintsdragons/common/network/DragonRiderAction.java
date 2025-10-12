package com.leon.saintsdragons.common.network;

/**
 * Enum for wyvern rider actions to replace magic strings
 */
public enum DragonRiderAction {
    NONE,           // No special action
    TAKEOFF_REQUEST, // Request takeoff from ground
    ACCELERATE,     // Start acceleration (L-Ctrl pressed)
    STOP_ACCELERATE, // Stop acceleration (L-Ctrl released) 
    ABILITY_USE,    // Use a named ability (start/one-shot)
    ABILITY_STOP  // Stop a named ability (for hold-to-use)
}