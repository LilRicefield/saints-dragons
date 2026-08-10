package com.leon.saintsdragons.client.model.item;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.MossbackItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MossbackItemModel extends GeoModel<MossbackItem> {
    private final boolean baby;

    public MossbackItemModel() {
        this(false);
    }

    public MossbackItemModel(boolean baby) {
        this.baby = baby;
    }

    @Override
    public ResourceLocation getModelResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl(baby
                ? "geo/entity/baby_mossback.geo.json"
                : "geo/entity/mossback.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl(baby
                ? "textures/entity/mossback/baby_mossback.png"
                : "textures/entity/mossback/mossback.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MossbackItem animatable) {
        return SaintsDragonsCommon.rl(baby
                ? "animations/entity/baby_mossback.animation.json"
                : "animations/entity/mossback.animation.json");
    }
}
