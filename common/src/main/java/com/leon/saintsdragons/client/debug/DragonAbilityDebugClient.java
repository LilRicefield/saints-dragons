package com.leon.saintsdragons.client.debug;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DragonAbilityDebugClient {
    public record DebugBox(AABB box, float red, float green, float blue, int alpha) {
    }

    private static final List<Entry> ACTIVE_BOXES = new ArrayList<>();
    private static boolean enabled = false;

    private DragonAbilityDebugClient() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        enabled = !enabled;
        if (!enabled) {
            ACTIVE_BOXES.clear();
        }
        return enabled;
    }

    public static void addBox(AABB box, int colorRgb, int lifetimeTicks) {
        if (!enabled || lifetimeTicks <= 0) {
            return;
        }
        float red = ((colorRgb >> 16) & 0xFF) / 255.0F;
        float green = ((colorRgb >> 8) & 0xFF) / 255.0F;
        float blue = (colorRgb & 0xFF) / 255.0F;
        ACTIVE_BOXES.add(new Entry(box, red, green, blue, Math.max(1, lifetimeTicks)));
    }

    public static List<DebugBox> getBoxes() {
        List<DebugBox> boxes = new ArrayList<>(ACTIVE_BOXES.size());
        for (Entry entry : ACTIVE_BOXES) {
            boxes.add(new DebugBox(entry.box, entry.red, entry.green, entry.blue, 255));
        }
        return boxes;
    }

    public static void tick() {
        if (ACTIVE_BOXES.isEmpty()) {
            return;
        }

        Iterator<Entry> iterator = ACTIVE_BOXES.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            entry.ticksRemaining--;
            if (entry.ticksRemaining <= 0) {
                iterator.remove();
            }
        }
    }

    private static final class Entry {
        private final AABB box;
        private final float red;
        private final float green;
        private final float blue;
        private int ticksRemaining;

        private Entry(AABB box, float red, float green, float blue, int ticksRemaining) {
            this.box = box;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
