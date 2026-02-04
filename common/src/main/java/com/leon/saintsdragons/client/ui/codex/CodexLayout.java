package com.leon.saintsdragons.client.ui.codex;

public final class CodexLayout {
    public static final int GUI_WIDTH = 393;
    public static final int GUI_HEIGHT = 214;
    public static final int TAB_HEIGHT = 22;
    public static final int TAB_WIDTH = 26;
    public static final int TAB_CLOSED_WIDTH = 10;
    public static final int TAB_CLOSED_HEIGHT = 22;
    public static final int MAX_VISIBLE_DRAGONS = 10;
    public static final int LIST_WIDTH = 120;
    public static final int TEXT_COLOR = 0x5B3A12;
    public static final int STAT_ICON_WIDTH = 8;
    public static final int STAT_ICON_HEIGHT = 9;
    public static final int HEALTH_ICON_OFFSET_X = 96;
    public static final int HEALTH_ICON_OFFSET_Y = 147;
    public static final int HUNGER_ICON_OFFSET_X = HEALTH_ICON_OFFSET_X + 67;
    public static final int HAPPINESS_ICON_OFFSET_X = HUNGER_ICON_OFFSET_X;
    public static final int VARIANT_ICON_OFFSET_X = HUNGER_ICON_OFFSET_X;
    public static final int STAT_ICON_GAP_Y = 5;
    public static final int STAT_TEXT_OFFSET_Y = 1;
    public static final int NAME_BOX_X = 110;
    public static final int NAME_BOX_Y = 32;
    public static final int NAME_BOX_WIDTH = 88;
    public static final int NAME_BOX_HEIGHT = 14;
    public static final int DRAGON_RENDER_BOX_X = 113;
    public static final int DRAGON_RENDER_BOX_Y = 51;
    public static final int DRAGON_RENDER_BOX_SIZE = 85;
    public static final int MAX_VISIBLE_ALLIES = 8;

    private CodexLayout() {
    }

    public static int getListLeft(int leftPos) {
        return leftPos + 7;
    }

    public static int getListTop(int topPos) {
        return topPos + 47;
    }

    public static int getListBottom(int topPos) {
        return topPos + GUI_HEIGHT - 18;
    }

    public static int getDetailLeft(int leftPos) {
        return leftPos + 150;
    }

    public static int getDetailTop(int topPos) {
        return topPos + 44;
    }

    public static int getDetailRight(int leftPos) {
        return leftPos + GUI_WIDTH - 18;
    }

    public static int getDetailBottom(int topPos) {
        return topPos + GUI_HEIGHT - 18;
    }

    public static int getActiveTabX(int leftPos) {
        return leftPos + 366;
    }

    public static int getTabY(int topPos, int index) {
        return topPos + 24 + (TAB_HEIGHT + 2) * index;
    }
}
