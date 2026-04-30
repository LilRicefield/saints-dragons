package com.leon.saintsdragons.client.ui;

public class DragonUIRegistry {
    private static MeleeModeNotification meleeModeNotification;
    private static boolean uiVisible = true;
    public static void init(MeleeModeNotification notification) {
        meleeModeNotification = notification;
    }
    public static MeleeModeNotification getMeleeModeNotification() {
        if (meleeModeNotification == null) {
            meleeModeNotification = new MeleeModeNotification();
        }
        return meleeModeNotification;
    }
    public static void toggleUIVisibility() {
        uiVisible = !uiVisible;
    }
    public static boolean isUIVisible() {
        return uiVisible;
    }

}
