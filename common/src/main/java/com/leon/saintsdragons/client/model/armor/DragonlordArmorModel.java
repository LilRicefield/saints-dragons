package com.leon.saintsdragons.client.model.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.DragonlordArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DragonlordArmorModel extends GeoModel<DragonlordArmorItem> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/armor/dragonlord_armor.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/armor/dragonlord_armor.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/armor/dragonlord_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(DragonlordArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DragonlordArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DragonlordArmorItem animatable) {
        return ANIMATION;
    }
}
