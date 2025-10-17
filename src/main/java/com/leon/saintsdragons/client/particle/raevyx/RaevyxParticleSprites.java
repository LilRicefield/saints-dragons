package com.leon.saintsdragons.client.particle.raevyx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Lazy sprite cache for Raevyx lightning particles.
 * Provides gender-aware frame lists so we can swap textures per dragon gender.
 */
final class RaevyxParticleSprites {
    private static final ResourceLocation[] STORM_MALE = resourceArray(
            "raevyx/lightning_storm_0",
            "raevyx/lightning_storm_1",
            "raevyx/lightning_storm_2",
            "raevyx/lightning_storm_3",
            "raevyx/lightning_storm_4",
            "raevyx/lightning_storm_5",
            "raevyx/lightning_storm_6",
            "raevyx/lightning_storm_7"
    );

    private static final ResourceLocation[] STORM_FEMALE = resourceArray(
            "raevyx/female_lightning_storm_0",
            "raevyx/lightning_storm_1",
            "raevyx/lightning_storm_2",
            "raevyx/female_lightning_storm_3",
            "raevyx/lightning_storm_4",
            "raevyx/female_lightning_storm_5",
            "raevyx/female_lightning_storm_6",
            "raevyx/female_lightning_storm_7"
    );

    private static final ResourceLocation[] ARC_MALE = resourceArray(
            "raevyx/lightning_arc_0",
            "raevyx/lightning_arc_1",
            "raevyx/lightning_arc_2",
            "raevyx/lightning_arc_3",
            "raevyx/lightning_arc_4",
            "raevyx/lightning_arc_5",
            "raevyx/lightning_arc_6",
            "raevyx/lightning_arc_7"
    );

    private static final ResourceLocation[] ARC_FEMALE = resourceArray(
            "raevyx/female_lightning_arc_0",
            "raevyx/lightning_arc_1",      // shared white frame
            "raevyx/lightning_arc_2",      // shared white frame
            "raevyx/female_lightning_arc_3",
            "raevyx/lightning_arc_4",      // shared white frame
            "raevyx/female_lightning_arc_5",
            "raevyx/female_lightning_arc_6",
            "raevyx/female_lightning_arc_7"
    );

    private static TextureAtlasSprite[] stormMaleSprites;
    private static TextureAtlasSprite[] stormFemaleSprites;
    private static TextureAtlasSprite[] arcMaleSprites;
    private static TextureAtlasSprite[] arcFemaleSprites;

    private RaevyxParticleSprites() {
    }

    static TextureAtlasSprite[] storm(boolean female) {
        return female ? getStormFemale() : getStormMale();
    }

    static TextureAtlasSprite[] arc(boolean female) {
        return female ? getArcFemale() : getArcMale();
    }

    static int frameIndexByProgress(TextureAtlasSprite[] frames, float progress) {
        if (frames.length == 0) {
            return 0;
        }
        float clamped = Mth.clamp(progress, 0.0F, 0.999F);
        return Mth.clamp((int) (clamped * frames.length), 0, frames.length - 1);
    }

    private static TextureAtlasSprite[] getStormMale() {
        if (stormMaleSprites == null) {
            stormMaleSprites = resolveSprites(STORM_MALE);
        }
        return stormMaleSprites;
    }

    private static TextureAtlasSprite[] getStormFemale() {
        if (stormFemaleSprites == null) {
            stormFemaleSprites = resolveSprites(STORM_FEMALE);
        }
        return stormFemaleSprites;
    }

    private static TextureAtlasSprite[] getArcMale() {
        if (arcMaleSprites == null) {
            arcMaleSprites = resolveSprites(ARC_MALE);
        }
        return arcMaleSprites;
    }

    private static TextureAtlasSprite[] getArcFemale() {
        if (arcFemaleSprites == null) {
            arcFemaleSprites = resolveSprites(ARC_FEMALE);
        }
        return arcFemaleSprites;
    }

    private static TextureAtlasSprite[] resolveSprites(ResourceLocation[] resources) {
        TextureAtlas atlas = null;
        var texture = Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_PARTICLES);
        if (texture instanceof TextureAtlas ta) {
            atlas = ta;
        }
        if (atlas == null) {
            atlas = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_PARTICLES);
        }
        if (atlas == null) {
            return new TextureAtlasSprite[0];
        }
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[resources.length];
        for (int i = 0; i < resources.length; i++) {
            sprites[i] = atlas.getSprite(resources[i]);
        }
        return sprites;
    }

    private static ResourceLocation[] resourceArray(String... paths) {
        ResourceLocation[] array = new ResourceLocation[paths.length];
        for (int i = 0; i < paths.length; i++) {
            array[i] = ResourceLocation.fromNamespaceAndPath("saintsdragons", paths[i]);
        }
        return array;
    }
}
