package com.leon.saintsdragons.client.renderer.armor;

import com.leon.saintsdragons.client.model.armor.IgnivorusArmorModel;
import com.leon.saintsdragons.common.item.IgnivorusArmorItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class IgnivorusArmorRenderer extends GeoArmorRenderer<IgnivorusArmorItem> {
    public IgnivorusArmorRenderer() {
        super(new IgnivorusArmorModel());
    }

    @Override
    public @Nullable GeoBone getHeadBone() {
        return getGeoModel().getBone("armorHead").orElse(null);
    }
}
