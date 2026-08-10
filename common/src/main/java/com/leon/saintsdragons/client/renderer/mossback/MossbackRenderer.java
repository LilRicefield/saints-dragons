package com.leon.saintsdragons.client.renderer.mossback;

import com.leon.saintsdragons.client.model.mossback.MossbackModel;
import com.leon.saintsdragons.server.entity.dragons.Mossback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MossbackRenderer extends GeoEntityRenderer<Mossback> {
    public MossbackRenderer(EntityRendererProvider.Context context) {
        super(context, new MossbackModel());
        this.shadowRadius = 0.3F;
    }

    @Override
    public float getMotionAnimThreshold(Mossback animatable) {
        return 0.000001F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Mossback entity) {
        return getGeoModel().getTextureResource(entity);
    }
}
