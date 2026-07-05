package com.leon.saintsdragons.client.model.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.IgnivorusArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IgnivorusArmorModel extends GeoModel<IgnivorusArmorItem> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/armor/ignivorus_armor.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/armor/ignivorus_armor.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/armor/ignivorus_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(IgnivorusArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(IgnivorusArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(IgnivorusArmorItem animatable) {
        return ANIMATION;
    }
}
