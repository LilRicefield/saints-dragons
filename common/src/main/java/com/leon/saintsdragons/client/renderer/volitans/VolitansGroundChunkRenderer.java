package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansGroundChunkModel;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansGroundChunkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class VolitansGroundChunkRenderer extends GeoEntityRenderer<VolitansGroundChunkEntity> {
    private static final ResourceLocation DIRT_TEXTURE = new ResourceLocation("minecraft", "textures/block/dirt.png");
    private static final float MODEL_FORWARD_YAW_OFFSET = 0.0F;
    private MultiBufferSource currentBufferSource;

    public VolitansGroundChunkRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansGroundChunkModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull VolitansGroundChunkEntity entity) {
        return 0.0F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VolitansGroundChunkEntity entity) {
        return DIRT_TEXTURE;
    }

    @Override
    public void render(@NotNull VolitansGroundChunkEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isReady()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getVisualYaw() + MODEL_FORWARD_YAW_OFFSET));
        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, VolitansGroundChunkEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);
        this.currentBufferSource = bufferSource;
        renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    @Override
    public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float red, float green, float blue, float alpha) {
        VolitansGroundChunkEntity entity = getAnimatable();
        if (entity == null) {
            return;
        }

        BlockState state = entity.getBlockState();
        if (state.isAir()) {
            return;
        }

        poseStack.pushPose();
        BlockCenter center = getCubeCenter(cube);
        poseStack.translate(center.x() - 0.5D, center.y() - 0.5D, center.z() - 0.5D);
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(state, poseStack, currentBufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static BlockCenter getCubeCenter(GeoCube cube) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                if (vertex == null) {
                    continue;
                }
                minX = Math.min(minX, vertex.position().x());
                minY = Math.min(minY, vertex.position().y());
                minZ = Math.min(minZ, vertex.position().z());
                maxX = Math.max(maxX, vertex.position().x());
                maxY = Math.max(maxY, vertex.position().y());
                maxZ = Math.max(maxZ, vertex.position().z());
            }
        }

        if (!Double.isFinite(minX)) {
            return new BlockCenter(0.0D, 0.0D, 0.0D);
        }
        return new BlockCenter(
                (minX + maxX) * 0.5D,
                (minY + maxY) * 0.5D,
                (minZ + maxZ) * 0.5D
        );
    }

    private record BlockCenter(double x, double y, double z) {
    }
}
