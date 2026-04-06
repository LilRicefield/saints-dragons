package com.leon.saintsdragons.client.renderer;

public final class RenderPassContext {
    private static int extractingDragonId = -1;

    private RenderPassContext() {
    }

    public static void beginExtraction(int dragonId) {
        extractingDragonId = dragonId;
    }

    public static void endExtraction() {
        extractingDragonId = -1;
    }

    public static boolean isExtractionAllowed(int dragonId) {
        return extractingDragonId == dragonId;
    }
}
