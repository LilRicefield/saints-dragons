package com.leon.saintsdragons.client.renderer.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public final class DraconianArmorTextures {
    private static final int OUTER_FRAME_COUNT = 4;
    private static final long FRAME_TIME_MILLIS = 150L;
    private static final ResourceLocation INNER =
            SaintsDragonsCommon.rl("textures/armor/draconian_armor_layer_2.png");

    private DraconianArmorTextures() {
    }

    public static ResourceLocation texture(boolean innerLayer) {
        if (innerLayer) {
            return INNER;
        }

        int frame = (int) ((Util.getMillis() / FRAME_TIME_MILLIS) % OUTER_FRAME_COUNT);
        return SaintsDragonsCommon.rl("textures/armor/draconian_armor_layer_1_" + frame + ".png");
    }
}
