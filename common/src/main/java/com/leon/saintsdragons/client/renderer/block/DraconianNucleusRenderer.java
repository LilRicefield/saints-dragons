package com.leon.saintsdragons.client.renderer.block;

import com.leon.saintsdragons.client.model.block.DraconianNucleusAnimations;
import com.leon.saintsdragons.client.model.block.DraconianNucleusModel;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.DraconianNucleusBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DraconianNucleusRenderer implements BlockEntityRenderer<DraconianNucleusBlockEntity> {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/block/draconic_nucleus.png");
    private final DraconianNucleusModel model;

    public DraconianNucleusRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new DraconianNucleusModel(context.bakeLayer(DraconianNucleusModel.LAYER_LOCATION));
    }

    @Override
    public void render(@NotNull DraconianNucleusBlockEntity nucleus, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        this.model.animate(DraconianNucleusAnimations.IDLE, nucleus.getAnimationTimeMillis(partialTick));
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
