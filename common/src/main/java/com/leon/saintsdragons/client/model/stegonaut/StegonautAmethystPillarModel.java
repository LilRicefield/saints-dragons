package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautAmethystPillarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StegonautAmethystPillarModel extends GeoModel<StegonautAmethystPillarEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/objects/stegonaut/amethyst_pillar.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/objects/stegonaut/amethyst_pillar.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/objects/stegonaut/amethyst_pillar.animation.json");

    @Override
    public ResourceLocation getModelResource(StegonautAmethystPillarEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StegonautAmethystPillarEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StegonautAmethystPillarEntity animatable) {
        return ANIMATION;
    }
}
