package com.leon.saintsdragons.client.renderer.draconianswarm;

import com.leon.saintsdragons.client.model.draconianswarm.WingedModel;
import com.leon.saintsdragons.server.entity.draconianswarm.Winged;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WingedRenderer extends GeoEntityRenderer<Winged> {
    public WingedRenderer(EntityRendererProvider.Context context) {
        super(context, new WingedModel());
        this.shadowRadius = 0.35F;
    }

    @Override
    public void render(@NotNull Winged entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public RenderType getRenderType(Winged animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    protected float getDeathMaxRotation(Winged entity) {
        return 0.0F;
    }
}
