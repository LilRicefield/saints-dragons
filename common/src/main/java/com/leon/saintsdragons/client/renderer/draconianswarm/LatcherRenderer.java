package com.leon.saintsdragons.client.renderer.draconianswarm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.leon.saintsdragons.client.model.draconianswarm.LatcherModel;
import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LatcherRenderer extends GeoEntityRenderer<Latcher> {
    public LatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new LatcherModel());
        this.shadowRadius = 0.45F;
    }

    @Override
    public void render(@NotNull Latcher entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public RenderType getRenderType(Latcher animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

    @Override
    protected float getDeathMaxRotation(Latcher entity) {
        return 0.0F;
    }
}
