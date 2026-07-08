package com.leon.saintsdragons.client.model.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.BloodTempestArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BloodTempestArmorModel extends GeoModel<BloodTempestArmorItem> {
    private static final ResourceLocation MODEL =
            SaintsDragonsCommon.rl("geo/armor/blood_tempest_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/armor/blood_tempest_armor.png");
    private static final ResourceLocation ANIMATION =
            SaintsDragonsCommon.rl("animations/armor/blood_tempest_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(BloodTempestArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BloodTempestArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BloodTempestArmorItem animatable) {
        return ANIMATION;
    }
}
