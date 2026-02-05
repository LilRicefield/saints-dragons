package com.leon.saintsdragons.client.renderer.stegonaut;

import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautGroundChunkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class StegonautGroundChunkRenderer extends EntityRenderer<StegonautGroundChunkEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public StegonautGroundChunkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(@NotNull StegonautGroundChunkEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        BlockState state = entity.getBlockState();
        if (state.isAir()) {
            return;
        }

        float scale = entity.getVisualScale();
        poseStack.pushPose();
        poseStack.translate(-0.5D * scale, 0.0D, -0.5D * scale);
        poseStack.scale(scale, scale, scale);
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull StegonautGroundChunkEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
