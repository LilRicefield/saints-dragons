package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VolitansSpineModel extends GeoModel<VolitansSpineEntity> {
    private static final ResourceLocation MODEL =
            SaintsDragonsCommon.rl("geo/entity/volitans_spine.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/volitans/volitans_spine.png");
    private static final ResourceLocation ANIMATION =
            SaintsDragonsCommon.rl("animations/entity/volitans_spine.animation.json");

    @Override
    public ResourceLocation getModelResource(VolitansSpineEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VolitansSpineEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VolitansSpineEntity animatable) {
        return ANIMATION;
    }
}
