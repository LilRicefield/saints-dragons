package com.leon.saintsdragons.client.renderer.atroxiia;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;
import com.leon.saintsdragons.client.renderer.vfx.AtroxiiaQuakeTailRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import com.leon.saintsdragons.client.model.atroxiia.AtroxiiaModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.renderer.layer.atroxiia.AtroxiiaNightEmissiveLayer;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import org.joml.Vector3f;

public class AtroxiiaRenderer extends DragonGeoEntityRenderer<Atroxiia> {
    private Vec3 tailTipPosition;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;

    public AtroxiiaRenderer(EntityRendererProvider.Context context) {
        super(context, new AtroxiiaModel());
        this.addRenderLayer(new AtroxiiaNightEmissiveLayer(this));
    }

    @Override
    public void render(Atroxiia entity, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        tailTipPosition = null;
        try {
            super.render(entity, yaw, partialTick, poses, buffers, packedLight);
        } finally {
            tailTipPosition = null;
        }
    }

    @Override
    public void renderRecursively(PoseStack poses, Atroxiia entity, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.renderRecursively(poses, entity, bone, type, buffers, buffer, isReRender,
                partialTick, light, overlay, red, green, blue, alpha);
        if (isReRender || !bone.getName().equals("tailtip")) return;
        poses.pushPose();
        try {
            RenderUtils.translateMatrixToBone(poses, bone);
            RenderUtils.translateToPivotPoint(poses, bone);
            RenderUtils.rotateMatrixAroundBone(poses, bone);
            RenderUtils.scaleMatrixForBone(poses, bone);
            var matrix = RenderUtils.invertAndMultiplyMatrices(poses.last().pose(), this.entityRenderTranslations);
            var point = matrix.transformPosition(new Vector3f());
            tailTipPosition = new Vec3(point.x + Mth.lerp(partialTick, entity.xOld, entity.getX()),
                    point.y + Mth.lerp(partialTick, entity.yOld, entity.getY()),
                    point.z + Mth.lerp(partialTick, entity.zOld, entity.getZ()));
        } finally {
            poses.popPose();
        }
    }

    @Override
    protected float getBabyShadowRadius(Atroxiia entity) {
        return 0.45F;
    }

    @Override
    protected float getAdultShadowRadius(Atroxiia entity) {
        return 2.0F;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE, "tailtip"};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Atroxiia entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
        };
    }
    @Override
    protected void afterDragonRender(Atroxiia entity, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        AtroxiiaQuakeTailRenderer.render(entity, tailTipPosition, poses, buffers, partialTick);
    }
}
