package com.leon.saintsdragons.common.block.crucible;

public final class DraconicCrucibleUiLayout {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 208;
    public static final int RECIPE_VIEW_HEIGHT = 125;

    public static final int OUTPUT_SLOT_X = 80;
    public static final int OUTPUT_SLOT_Y = 21;
    public static final int FUEL_SLOT_X = 30;
    public static final int FUEL_SLOT_Y = 91;
    public static final int INPUT_GRID_X = 62;
    public static final int INPUT_GRID_Y = 60;
    public static final int SLOT_SPACING = 18;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 126;
    public static final int HOTBAR_Y = 184;

    public static final int BUTTON_X = 145;
    public static final int BUTTON_Y = 99;
    public static final int BUTTON_WIDTH = 24;
    public static final int BUTTON_HEIGHT = 14;
    public static final int BUTTON_U = 232;
    public static final int BUTTON_DEFAULT_V = 0;
    public static final int BUTTON_HIGHLIGHTED_V = 14;
    public static final int BUTTON_CLICKED_V = 28;

    public static final int PANEL_X = 58;
    public static final int PANEL_Y = 56;
    public static final int PANEL_U = 176;
    public static final int PANEL_V = 87;
    public static final int PANEL_WIDTH = 70;
    public static final int PANEL_HEIGHT = 60;
    public static final int BAR_U = 245;
    public static final int BAR_V = 92;
    public static final int BAR_WIDTH = 5;
    public static final int BAR_HEIGHT = 40;
    public static final int BAR_LEFT_SHIFT = 9;
    public static final int BAR_X = BAR_U - PANEL_U - BAR_LEFT_SHIFT;
    public static final int BAR_Y = BAR_V - PANEL_V;

    public static final int ARROW_X = 80;
    public static final int ARROW_Y = 46;
    public static final int ARROW_U = 180;
    public static final int ARROW_V = 3;
    public static final int ARROW_WIDTH = 17;
    public static final int ARROW_HEIGHT = 9;

    public static final int GAUGE_X = 35;
    public static final int GAUGE_Y = 28;
    public static final int GAUGE_U = 182;
    public static final int GAUGE_V = 25;
    public static final int GAUGE_WIDTH = 7;
    public static final int GAUGE_HEIGHT = 56;
    public static final int GAUGE_LEVEL_1_HEIGHT = 21;
    public static final int GAUGE_LEVEL_2_HEIGHT = 42;
    public static final int GAUGE_LEVEL_3_HEIGHT = 56;

    public static int gaugeFillHeight(int charge, int level1Capacity,
                                      int level2Capacity, int level3Capacity) {
        if (charge <= 0) {
            return 0;
        }
        if (charge <= level1Capacity) {
            return scaleGaugeSegment(charge, 0, level1Capacity, 0, GAUGE_LEVEL_1_HEIGHT);
        }
        if (charge <= level2Capacity) {
            return scaleGaugeSegment(charge, level1Capacity, level2Capacity,
                    GAUGE_LEVEL_1_HEIGHT, GAUGE_LEVEL_2_HEIGHT);
        }
        return scaleGaugeSegment(Math.min(charge, level3Capacity), level2Capacity, level3Capacity,
                GAUGE_LEVEL_2_HEIGHT, GAUGE_LEVEL_3_HEIGHT);
    }

    public static GaugeAnimation gaugeAnimationForHeatLevel(int heatLevel) {
        DraconicCrucibleFuelTier tier = DraconicCrucibleFuelTier.fromHeatLevel(heatLevel);
        int initialCharge = tier.chargeCapacity();
        int remainingCharge = Math.max(0, initialCharge - tier.processingCost(heatLevel));
        int level1Capacity = DraconicCrucibleFuelTier.LEVEL_1.chargeCapacity();
        int level2Capacity = DraconicCrucibleFuelTier.LEVEL_2.chargeCapacity();
        int level3Capacity = DraconicCrucibleFuelTier.LEVEL_3.chargeCapacity();
        return new GaugeAnimation(
                gaugeFillHeight(initialCharge, level1Capacity, level2Capacity, level3Capacity),
                gaugeFillHeight(remainingCharge, level1Capacity, level2Capacity, level3Capacity));
    }

    private static int scaleGaugeSegment(int value, int valueStart, int valueEnd,
                                         int pixelStart, int pixelEnd) {
        int valueRange = Math.max(1, valueEnd - valueStart);
        int pixelRange = pixelEnd - pixelStart;
        int progress = Math.max(0, value - valueStart);
        return Math.min(pixelEnd, pixelStart + (pixelRange * progress + valueRange - 1) / valueRange);
    }

    public record GaugeAnimation(int initialHeight, int finalHeight) {
    }

    private DraconicCrucibleUiLayout() {
    }
}
