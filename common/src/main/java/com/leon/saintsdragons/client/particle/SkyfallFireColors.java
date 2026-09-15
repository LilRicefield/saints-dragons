package com.leon.saintsdragons.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;

final class SkyfallFireColors {
    private static final int[] COLORS = {
            0xFFFFF3, 0xFDFBA5, 0xFFC300, 0xFF8816,
            0xC4422D, 0x8F3527, 0x632618, 0x46201E
    };

    static void apply(Particle particle, float progress) {
        float position = Mth.clamp(progress, 0, 1) * (COLORS.length - 1);
        int index = Math.min((int) position, COLORS.length - 2);
        float blend = position - index;
        int from = COLORS[index], to = COLORS[index + 1];
        particle.setColor(Mth.lerp(blend, (from >> 16) & 255, (to >> 16) & 255) / 255.0F,
                Mth.lerp(blend, (from >> 8) & 255, (to >> 8) & 255) / 255.0F,
                Mth.lerp(blend, from & 255, to & 255) / 255.0F);
    }
}
