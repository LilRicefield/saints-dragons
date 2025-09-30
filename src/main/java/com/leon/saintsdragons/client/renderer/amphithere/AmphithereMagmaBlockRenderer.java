package com.leon.saintsdragons.client.renderer.amphithere;

import com.leon.saintsdragons.server.entity.effect.amphithere.AmphithereMagmaBlockEntity;
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

public class AmphithereMagmaBlockRenderer extends EntityRenderer<AmphithereMagmaBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AmphithereMagmaBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(@NotNull AmphithereMagmaBlockEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        BlockState state = entity.getBlockState();
        if (state.isAir()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AmphithereMagmaBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

