package com.leon.saintsdragons.client.renderer.volitans;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;
import com.leon.saintsdragons.client.model.volitans.VolitansModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.renderer.layer.volitans.VolitansNightEmissiveLayer;
import com.leon.saintsdragons.client.renderer.vfx.DragonDiveTrailRenderer;
import com.leon.saintsdragons.client.renderer.vfx.VolitansBreathIntroRenderer;
import com.leon.saintsdragons.client.renderer.vfx.VolitansWaterRingRenderer;
import com.leon.saintsdragons.client.renderer.vfx.VolitansPoisonBallChargeRenderer;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public class VolitansRenderer extends DragonGeoEntityRenderer<Volitans> {
    private Vec3 renderedMouthOffset;
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String BREATH_BONE = "breathBone";
    private static final int SYNC_INTERVAL_TICKS = 2;
    private final Map<Volitans, Integer> lastBreathSnapshotHashes = new WeakHashMap<>();

    public VolitansRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansModel());
        this.addRenderLayer(new VolitansNightEmissiveLayer(this));
    }

    @Override
    public void render(Volitans entity, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        renderedMouthOffset = null;
        try {
            super.render(entity, yaw, partialTick, poses, buffers, packedLight);
        } finally {
            renderedMouthOffset = null;
        }
    }

    @Override
    public void renderRecursively(PoseStack poses, Volitans entity, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.renderRecursively(poses, entity, bone, type, buffers, buffer, isReRender,
                partialTick, light, overlay, red, green, blue, alpha);
        if (isReRender || !BREATH_BONE.equals(bone.getName())) return;
        poses.pushPose();
        try {
            RenderUtils.translateMatrixToBone(poses, bone);
            RenderUtils.translateToPivotPoint(poses, bone);
            RenderUtils.rotateMatrixAroundBone(poses, bone);
            RenderUtils.scaleMatrixForBone(poses, bone);
            var matrix = RenderUtils.invertAndMultiplyMatrices(poses.last().pose(), this.entityRenderTranslations);
            var point = matrix.transformPosition(new org.joml.Vector3f());
            renderedMouthOffset = new Vec3(point.x, point.y, point.z);
        } finally {
            poses.popPose();
        }
    }

    @Override
    protected float getBabyShadowRadius(Volitans entity) {
        return 1.1f;
    }

    @Override
    protected float getAdultShadowRadius(Volitans entity) {
        return 2.4f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE, BREATH_BONE, DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE, DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Volitans entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z,
                        "passengerLocator", "passengerSeat0"),
                new LocatorSpec(BREATH_BONE, 0.0f, 0.0f, 0.0f, "breathBoneOrigin")
        };
    }

    @Override
    protected void afterDragonRender(Volitans entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
        sendBreathLocatorToServer(entity);
        VolitansWaterRingRenderer.render(entity, poseStack, bufferSource, partialTick);
        VolitansBreathIntroRenderer.render(entity, poseStack, bufferSource, partialTick);
        VolitansPoisonBallChargeRenderer.render(entity, renderedMouthOffset, poseStack, bufferSource, partialTick);
        DragonDiveTrailRenderer.render(entity,
                getBoneWorldPosition(DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE),
                bufferSource,
                poseStack.last());
    }

    private void sendBreathLocatorToServer(Volitans entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !entity.isAlive()) {
            return;
        }
        if (minecraft.player.distanceToSqr(entity) > 96.0D * 96.0D) {
            return;
        }
        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        java.util.Map<String, Vec3> positions = new java.util.HashMap<>(1);
        Vec3 breath = entity.getClientLocatorPosition("breathBoneOrigin");
        if (breath != null) {
            positions.put("breathBoneOrigin", breath);
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBreathSnapshotHashes.put(entity, snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        NetworkHandler.sendToServer(new MessageDragonBonePositions(entity.getId(), positions));
    }

    private static int computeSnapshotHash(java.util.Map<String, Vec3> positions) {
        int hash = 1;
        for (String boneName : new String[] {"breathBoneOrigin"}) {
            Vec3 pos = positions.get(boneName);
            if (pos == null) {
                continue;
            }
            hash = 31 * hash + boneName.hashCode();
            hash = 31 * hash + quantize(pos.x);
            hash = 31 * hash + quantize(pos.y);
            hash = 31 * hash + quantize(pos.z);
        }
        return hash;
    }

    private static int quantize(double value) {
        return (int) Math.round(value * 1000.0D);
    }
}
