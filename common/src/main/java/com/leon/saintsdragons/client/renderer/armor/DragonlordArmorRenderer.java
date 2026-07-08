package com.leon.saintsdragons.client.renderer.armor;

import com.leon.saintsdragons.client.model.armor.DragonlordArmorModel;
import com.leon.saintsdragons.common.item.DragonlordArmorItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DragonlordArmorRenderer extends GeoArmorRenderer<DragonlordArmorItem> {
    public DragonlordArmorRenderer() {
        super(new DragonlordArmorModel());
    }

    @Override
    public @Nullable GeoBone getHeadBone() {
        return getGeoModel().getBone("armorHead").orElse(null);
    }
}
