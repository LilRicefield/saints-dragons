package com.leon.saintsdragons.client.ui;

/**
 * Simple registry for dragon UI elements that need to be accessed from multiple places.
 * This replaces the old DragonStatusUIManager for accessing shared UI elements.
 */
public class DragonUIRegistry {
    private static MeleeModeNotification meleeModeNotification;
    private static boolean uiVisible = true;

    /**
     * Initialize the UI registry. Called once during client initialization.
     */
    public static void init(MeleeModeNotification notification) {
        meleeModeNotification = notification;
    }

    /**
     * Get the melee mode notification instance
     */
    public static MeleeModeNotification getMeleeModeNotification() {
        if (meleeModeNotification == null) {
            meleeModeNotification = new MeleeModeNotification();
        }
        return meleeModeNotification;
    }

    /**
     * Toggle UI visibility
     */
    public static void toggleUIVisibility() {
        uiVisible = !uiVisible;
    }

    /**
     * Check if UI is visible
     */
    public static boolean isUIVisible() {
        return uiVisible;
    }

    /**
     * Set UI visibility
     */
    public static void setUIVisible(boolean visible) {
        uiVisible = visible;
    }
}
