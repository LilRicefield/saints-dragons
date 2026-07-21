package com.leon.saintsdragons.client.renderer.block;

import com.leon.saintsdragons.client.model.block.DraconicCrucibleEntity;
import com.leon.saintsdragons.client.model.block.DraconicCrucibleAnimations;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.DraconicCrucibleBlock;
import com.leon.saintsdragons.common.block.DraconicCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class DraconicCrucibleRenderer implements BlockEntityRenderer<DraconicCrucibleBlockEntity> {
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/block/draconic_crucible.png");
    private final DraconicCrucibleEntity model;

    public DraconicCrucibleRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new DraconicCrucibleEntity(context.bakeLayer(DraconicCrucibleEntity.LAYER_LOCATION));
    }

    @Override
    public void render(@NotNull DraconicCrucibleBlockEntity crucible, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        boolean active = crucible.getBlockState().getValue(DraconicCrucibleBlock.LIT);
        boolean open = crucible.hasAnimationState() ? crucible.isOpen() : !active;
        long animationTime = crucible.hasAnimationState()
                ? crucible.getAnimationTimeMillis(partialTick)
                : 1_000L;
        this.model.animate(
                open ? DraconicCrucibleAnimations.OPEN : DraconicCrucibleAnimations.CLOSE,
                animationTime);

        Direction facing = crucible.getBlockState().getValue(DraconicCrucibleBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull DraconicCrucibleBlockEntity blockEntity) {
        return true;
    }
}
