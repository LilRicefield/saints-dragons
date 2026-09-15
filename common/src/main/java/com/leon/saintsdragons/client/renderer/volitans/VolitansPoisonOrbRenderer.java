package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansPoisonOrbModel;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansPoisonOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolitansPoisonOrbRenderer extends GeoEntityRenderer<VolitansPoisonOrbEntity> {
    public VolitansPoisonOrbRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansPoisonOrbModel());
        shadowRadius = 0;
    }
    @Override
    public void render(VolitansPoisonOrbEntity entity, float yaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, poses, buffers, LightTexture.FULL_BRIGHT);
    }
    @Override
    protected void applyRotations(VolitansPoisonOrbEntity entity, PoseStack poses,
                                  float age, float yaw, float partialTick) {
        float scale = entity.getVisualScale() * 0.9F;
        poses.scale(scale, scale, scale);
        poses.translate(0, -0.25, 0);
    }
    @Override
    public RenderType getRenderType(VolitansPoisonOrbEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
