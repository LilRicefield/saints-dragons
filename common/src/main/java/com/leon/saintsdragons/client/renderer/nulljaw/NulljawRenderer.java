package com.leon.saintsdragons.client.renderer.nulljaw;

import com.leon.saintsdragons.client.model.nulljaw.NulljawModel;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public final class NulljawRenderer extends GeoEntityRenderer<Nulljaw> {
    public NulljawRenderer(EntityRendererProvider.Context context) {
        super(context, new NulljawModel());
        this.shadowRadius = 1.3F;
    }

    @Override
    public float getMotionAnimThreshold(Nulljaw animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Nulljaw entity) {
        return 0.0F;
    }
}
