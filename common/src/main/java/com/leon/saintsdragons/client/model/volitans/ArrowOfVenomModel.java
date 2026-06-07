package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.ArrowOfVenomEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ArrowOfVenomModel extends GeoModel<ArrowOfVenomEntity> {
    private static final ResourceLocation MODEL =
            SaintsDragonsCommon.rl("geo/entity/arrow_of_venom.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/arrow_of_venom.png");
    private static final ResourceLocation ANIMATION =
            SaintsDragonsCommon.rl("animations/entity/arrow_of_venom.animation.json");

    @Override
    public ResourceLocation getModelResource(ArrowOfVenomEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArrowOfVenomEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArrowOfVenomEntity animatable) {
        return ANIMATION;
    }
}
