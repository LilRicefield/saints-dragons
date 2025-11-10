package com.leon.saintsdragons.client.model.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaPillarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IgnivorusMagmaPillarModel extends GeoModel<IgnivorusMagmaPillarEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/blocks/ignivorus_magma_pillar.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/blocks/entity/ignivorus/ignivorus_magma_pillar.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/blocks/ignivorus_magma_pillar.animation.json");

    @Override
    public ResourceLocation getModelResource(IgnivorusMagmaPillarEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(IgnivorusMagmaPillarEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(IgnivorusMagmaPillarEntity animatable) {
        return ANIMATION;
    }
}
