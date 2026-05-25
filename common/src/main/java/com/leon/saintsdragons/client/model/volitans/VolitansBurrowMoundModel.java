package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansBurrowMoundEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VolitansBurrowMoundModel extends GeoModel<VolitansBurrowMoundEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/blocks/burrow_mound.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/blocks/burrow_mound.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/blocks/burrow_mound.animation.json");

    @Override
    public ResourceLocation getModelResource(VolitansBurrowMoundEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VolitansBurrowMoundEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VolitansBurrowMoundEntity animatable) {
        return ANIMATION;
    }
}
