package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansGroundChunkEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VolitansGroundChunkModel extends GeoModel<VolitansGroundChunkEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/blocks/ground_chunk.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/dirt.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/blocks/ground_chunk.animation.json");

    @Override
    public ResourceLocation getModelResource(VolitansGroundChunkEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VolitansGroundChunkEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VolitansGroundChunkEntity animatable) {
        return ANIMATION;
    }
}
