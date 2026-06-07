package com.leon.saintsdragons.client.model.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.RaevyxArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RaevyxArmorModel extends GeoModel<RaevyxArmorItem> {
    private static final ResourceLocation MODEL =
            SaintsDragonsCommon.rl("geo/armor/raevyx_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/armor/raevyx_armor.png");
    private static final ResourceLocation ANIMATION =
            SaintsDragonsCommon.rl("animations/armor/raevyx_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(RaevyxArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RaevyxArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RaevyxArmorItem animatable) {
        return ANIMATION;
    }
}
