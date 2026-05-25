package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansBurrowMoundModel;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansBurrowMoundEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayDeque;
import java.util.Deque;

public class VolitansBurrowMoundRenderer extends GeoEntityRenderer<VolitansBurrowMoundEntity> {
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/blocks/burrow_mound.png");
    private static final float MODEL_FORWARD_YAW_OFFSET = 0.0F;
    private final Deque<Boolean> blockMaterialStack = new ArrayDeque<>();
    private MultiBufferSource currentBufferSource;

    public VolitansBurrowMoundRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansBurrowMoundModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull VolitansBurrowMoundEntity entity) {
        return 0.0F;
    }

    @Override
    public void render(@NotNull VolitansBurrowMoundEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (entity.getOpacity(partialTick) <= 0.001F) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getVisualYaw() + MODEL_FORWARD_YAW_OFFSET));
        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, VolitansBurrowMoundEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        boolean inheritedBlockMaterial = !blockMaterialStack.isEmpty() && blockMaterialStack.peek();
        boolean blockMaterial = inheritedBlockMaterial || usesBlockMaterial(bone);
        blockMaterialStack.push(blockMaterial);

        if (blockMaterial) {
            float opacity = animatable.getOpacity(partialTick);
            if (opacity <= 0.001F) {
                blockMaterialStack.pop();
                return;
            }
            poseStack.pushPose();
            RenderUtils.prepMatrixForBone(poseStack, bone);
            this.currentBufferSource = bufferSource;
            renderCubesOfBone(poseStack, bone, buffer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, opacity);
            renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, opacity);
            poseStack.popPose();
        } else {
            float fadedAlpha = alpha * animatable.getOpacity(partialTick);
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, fadedAlpha);
        }

        blockMaterialStack.pop();
    }

    @Override
    public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float red, float green, float blue, float alpha) {
        if (blockMaterialStack.isEmpty() || !blockMaterialStack.peek()) {
            super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        VolitansBurrowMoundEntity entity = getAnimatable();
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
        if (alpha < 0.999F) {
            double centerOffset = 0.5D;
            float scale = Math.max(alpha, 0.001F);
            poseStack.translate(centerOffset, centerOffset, centerOffset);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-centerOffset, -centerOffset, -centerOffset);
        }
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(state, poseStack, currentBufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static boolean usesBlockMaterial(GeoBone bone) {
        String name = bone.getName();
        return "pillars".equals(name) || "formations".equals(name);
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

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VolitansBurrowMoundEntity entity) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(VolitansBurrowMoundEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
