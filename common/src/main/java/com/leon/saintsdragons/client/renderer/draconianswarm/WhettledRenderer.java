package com.leon.saintsdragons.client.renderer.draconianswarm;

import com.leon.saintsdragons.client.model.draconianswarm.WhettledModel;
import com.leon.saintsdragons.server.entity.draconianswarm.Whettled;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WhettledRenderer extends GeoEntityRenderer<Whettled> {
    public WhettledRenderer(EntityRendererProvider.Context context) {
        super(context, new WhettledModel());
        this.shadowRadius = 0.55F;
    }

    @Override
    public void render(@NotNull Whettled entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public RenderType getRenderType(Whettled animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    protected float getDeathMaxRotation(Whettled entity) {
        return 0.0F;
    }
}
