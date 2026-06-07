package com.leon.saintsdragons.client.renderer.armor;

import com.leon.saintsdragons.client.model.armor.RaevyxArmorModel;
import com.leon.saintsdragons.common.item.RaevyxArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class RaevyxArmorRenderer extends GeoArmorRenderer<RaevyxArmorItem> {
    public RaevyxArmorRenderer() {
        super(new RaevyxArmorModel());
    }

    @Override
    public @Nullable GeoBone getHeadBone() {
        return getGeoModel().getBone("armorhead").orElse(null);
    }
}
