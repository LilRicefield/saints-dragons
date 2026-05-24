package com.leon.saintsdragons.client.model.item;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.MossbackItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MossbackItemModel extends GeoModel<MossbackItem> {
    @Override
    public ResourceLocation getModelResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl("geo/entity/mossback.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl("textures/entity/mossback/mossback.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl("animations/entity/mossback.animation.json");
    }
}