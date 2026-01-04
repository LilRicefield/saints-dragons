package com.leon.saintsdragons.client.particle.raevyx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Lazy sprite cache for Raevyx lightning particles.
 */
final class RaevyxParticleSprites {
    private static final ResourceLocation[] STORM_SPRITES = resourceArray(
            "raevyx/lightning_storm_0",
            "raevyx/lightning_storm_1",
            "raevyx/lightning_storm_2",
            "raevyx/lightning_storm_3",
            "raevyx/lightning_storm_4",
            "raevyx/lightning_storm_5",
            "raevyx/lightning_storm_6",
            "raevyx/lightning_storm_7"
    );

    private static TextureAtlasSprite[] stormSprites;

    private RaevyxParticleSprites() {
    }

    static TextureAtlasSprite[] storm() {
        return getStorm();
    }

    static int frameIndexByProgress(TextureAtlasSprite[] frames, float progress) {
        if (frames.length == 0) {
            return 0;
        }
        float clamped = Mth.clamp(progress, 0.0F, 0.999F);
        return Mth.clamp((int) (clamped * frames.length), 0, frames.length - 1);
    }

    private static TextureAtlasSprite[] getStorm() {
        if (stormSprites == null) {
            stormSprites = resolveSprites(STORM_SPRITES);
        }
        return stormSprites;
    }

    private static TextureAtlasSprite[] resolveSprites(ResourceLocation[] resources) {
        try {
            // Access particle atlas through ParticleEngine instead of ModelManager
            TextureAtlas atlas = Minecraft.getInstance()
                    .particleEngine
                    .textureAtlas;
            if (atlas == null) {
                return new TextureAtlasSprite[0];
            }
            TextureAtlasSprite[] sprites = new TextureAtlasSprite[resources.length];
            for (int i = 0; i < resources.length; i++) {
                sprites[i] = atlas.getSprite(resources[i]);
            }
            return sprites;
        } catch (Exception e) {
            SaintsDragonsCommon.LOGGER.error("Failed to resolve particle sprites", e);
            return new TextureAtlasSprite[0];
        }
    }

    private static ResourceLocation[] resourceArray(String... paths) {
        ResourceLocation[] array = new ResourceLocation[paths.length];
        for (int i = 0; i < paths.length; i++) {
            array[i] = SaintsDragonsCommon.rl(paths[i]);
        }
        return array;
    }
}
