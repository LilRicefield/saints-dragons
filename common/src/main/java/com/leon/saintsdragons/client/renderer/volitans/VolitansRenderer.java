package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansModel;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class VolitansRenderer extends GeoEntityRenderer<Volitans> {
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String BREATH_BONE = "breathBone";
    private static final String MOUTH_LOCATOR_BONE = "mouth_origin";
    private static final int SYNC_INTERVAL_TICKS = 2;
    private final java.util.Map<Integer, Integer> lastBreathSnapshotHashes = new java.util.HashMap<>();

    private BakedGeoModel lastBakedModel;

    public VolitansRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansModel());
    }

    @Override
    public float getMotionAnimThreshold(Volitans animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Volitans entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Volitans entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {
        this.shadowRadius = entity.isBaby() ? 1.1f : 2.4f;
        this.lastBakedModel = model;
        if (model != null) {
            model.getBone(PASSENGER_BONE).ifPresent(b -> b.setTrackingMatrices(true));
            model.getBone(BREATH_BONE).ifPresent(b -> b.setTrackingMatrices(true));
            model.getBone(MOUTH_LOCATOR_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        }

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(@NotNull Volitans entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (ShaderPassCompatibility.isIrisShadowPass()) {
            return;
        }
        RenderPassContext.beginExtraction(entity.getId());
        RiderBullcrap.notifyRendered(entity.getId());
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            RenderPassContext.endExtraction();
        }

        if (this.lastBakedModel == null) {
            return;
        }

        this.lastBakedModel.getBone(PASSENGER_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) {
                entity.setClientLocatorPosition("passengerLocator", world);
                entity.setClientLocatorPosition("passengerSeat0", world);
            }
        });

        this.lastBakedModel.getBone(BREATH_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f);
            if (world != null) {
                entity.setClientLocatorPosition("breathBoneOrigin", world);
            }
        });

        this.lastBakedModel.getBone(MOUTH_LOCATOR_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f);
            if (world != null) {
                entity.setClientLocatorPosition("mouth_origin", world);
            }
        });

        sendBreathLocatorToServer(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Volitans animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(animatable);
        if (riderSpec == null || !bone.getName().equals(riderSpec.boneName)) {
            return;
        }
        if (!RenderPassContext.isExtractionAllowed(animatable.getId())) {
            return;
        }

        Matrix4f viewMatrix = new Matrix4f((Matrix4fc) poseStack.last().pose());
        Vector4f boneViewPos4 = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul((Matrix4fc) viewMatrix);
        double viewSpaceDistance = Math.sqrt(
                boneViewPos4.x() * boneViewPos4.x()
                        + boneViewPos4.y() * boneViewPos4.y()
                        + boneViewPos4.z() * boneViewPos4.z()
        );
        if (viewSpaceDistance >= riderSpec.maxCaptureDistance) {
            return;
        }
        if (!RiderBullcrap.tryLockForFrame(animatable.getId())) {
            return;
        }

        RiderBullcrap.store(animatable.getId(), viewMatrix);
        Vector3d boneWorldPosJoml = bone.getWorldPosition();
        net.minecraft.world.phys.Vec3 boneWorldPos = new net.minecraft.world.phys.Vec3(
                boneWorldPosJoml.x,
                boneWorldPosJoml.y,
                boneWorldPosJoml.z
        );
        RiderBullcrap.storeCameraOffset(animatable.getId(), boneWorldPos.subtract(animatable.position()));
    }

    private static net.minecraft.world.phys.Vec3 transformLocator(GeoBone bone, float px, float py, float pz) {
        if (bone == null || bone.getWorldSpaceMatrix() == null) {
            return null;
        }

        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());
        org.joml.Vector4f in = new org.joml.Vector4f(lx, ly, lz, 1f);
        org.joml.Vector4f out = worldMat.transform(in);
        return new net.minecraft.world.phys.Vec3(out.x(), out.y(), out.z());
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

        java.util.Map<String, net.minecraft.world.phys.Vec3> positions = new java.util.HashMap<>(2);
        net.minecraft.world.phys.Vec3 breath = entity.getClientLocatorPosition("breathBoneOrigin");
        if (breath != null) {
            positions.put("breathBoneOrigin", breath);
        }
        net.minecraft.world.phys.Vec3 mouth = entity.getClientLocatorPosition("mouth_origin");
        if (mouth != null) {
            positions.put("mouth_origin", mouth);
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBreathSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        NetworkHandler.sendToServer(new MessageDragonBonePositions(entity.getId(), positions));
    }

    private static int computeSnapshotHash(java.util.Map<String, net.minecraft.world.phys.Vec3> positions) {
        int hash = 1;
        for (String boneName : new String[] {"breathBoneOrigin", "mouth_origin"}) {
            net.minecraft.world.phys.Vec3 pos = positions.get(boneName);
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
