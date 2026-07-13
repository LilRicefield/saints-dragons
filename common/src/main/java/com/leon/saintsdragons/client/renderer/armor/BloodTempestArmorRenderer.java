package com.leon.saintsdragons.client.renderer.armor;

import com.leon.saintsdragons.client.model.armor.BloodTempestArmorModel;
import com.leon.saintsdragons.common.item.BloodTempestArmorItem;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BloodTempestArmorRenderer extends GeoArmorRenderer<BloodTempestArmorItem> {
    public BloodTempestArmorRenderer() {
        super(new BloodTempestArmorModel());
    }

    @Override
    public @Nullable GeoBone getHeadBone() {
        return getGeoModel().getBone("armorhead").orElse(null);
    }

    @Override
    public RenderType getRenderType(BloodTempestArmorItem animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (BloodTempestAfterimageRenderContext.isActive()) {
            return RenderType.entityTranslucent(texture);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(BloodTempestArmorItem animatable, float partialTick, int packedLight) {
        if (BloodTempestAfterimageRenderContext.isActive()) {
            return Color.ofRGBA(
                    BloodTempestAfterimageRenderContext.red(),
                    BloodTempestAfterimageRenderContext.green(),
                    BloodTempestAfterimageRenderContext.blue(),
                    BloodTempestAfterimageRenderContext.alpha()
            );
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }
}
