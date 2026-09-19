package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.model.ignivorus.IgnivorusModel;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.vfx.*;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusGlowLayer;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusNightEmissiveLayer;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.IgnivorusHitboxes;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public class IgnivorusRenderer extends DragonGeoEntityRenderer<Ignivorus> {
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;
    private static final String FIRE_BONE = "fireBone";
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String HEAD_BONE = "headController";
    private static final String NECK_BONE = "neck3Controller";
    private static final String HIP_BONE = "hip";
    private static final String LEFT_WING_BONE = "leftwing";
    private static final String RIGHT_WING_BONE = "rightwing";
    private static final String LEFT_WING_JOINT_BONE = "leftwingjoint";
    private static final String RIGHT_WING_JOINT_BONE = "rightwingjoint";
    private static final String TAIL1_BONE = "tail1";
    private static final String TAIL2_BONE = "tail2";
    private static final String TAIL3_BONE = "tail3";
    private static final String TAIL4_BONE = "tail4";
    private static final String LEFT_FRONT_LEG_BONE = "leftfrontleg";
    private static final String RIGHT_FRONT_LEG_BONE = "rightfrontleg";
    private static final String LEFT_BACK_LEG_BONE = "leftbackleg";
    private static final String RIGHT_BACK_LEG_BONE = "rightbackleg";

    public IgnivorusRenderer(EntityRendererProvider.Context context) {
        super(context, new IgnivorusModel());
        this.addRenderLayer(new IgnivorusNightEmissiveLayer(this));
        this.addRenderLayer(new IgnivorusGlowLayer(this));
    }

    @Override
    protected float getBabyShadowRadius(Ignivorus entity) {
        return 1.5F;
    }

    @Override
    protected float getAdultShadowRadius(Ignivorus entity) {
        return 5.0f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {
                PASSENGER_BONE, FIRE_BONE, HEAD_BONE, NECK_BONE, HIP_BONE, "middlebody",
                LEFT_WING_BONE, RIGHT_WING_BONE, LEFT_WING_JOINT_BONE, RIGHT_WING_JOINT_BONE,
                TAIL1_BONE, TAIL2_BONE, TAIL3_BONE, TAIL4_BONE,
                LEFT_FRONT_LEG_BONE, RIGHT_FRONT_LEG_BONE, LEFT_BACK_LEG_BONE, RIGHT_BACK_LEG_BONE,
                DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE
        };
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Ignivorus entity) {
        if (entity.isBaby()) {
            return new LocatorSpec[] {
                    new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
            };
        }

        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator"),
                new LocatorSpec(FIRE_BONE, 0.0f, 0.0f, 0.0f, "fireBoneOrigin"),
                new LocatorSpec(HEAD_BONE, 0.0f, 0.0f, 0.0f, "headController"),
                new LocatorSpec(NECK_BONE, 0.0f, 0.0f, 0.0f, "neck3Controller"),
                new LocatorSpec(HIP_BONE, 0.0f, 0.0f, 0.0f, "hip"),
                new LocatorSpec(LEFT_WING_BONE, 0.0f, 0.0f, 0.0f, "leftwing"),
                new LocatorSpec(RIGHT_WING_BONE, 0.0f, 0.0f, 0.0f, "rightwing"),
                new LocatorSpec(LEFT_WING_JOINT_BONE, 0.0f, 0.0f, 0.0f, "leftwingjoint"),
                new LocatorSpec(RIGHT_WING_JOINT_BONE, 0.0f, 0.0f, 0.0f, "rightwingjoint"),
                new LocatorSpec(TAIL1_BONE, 0.0f, 0.0f, 0.0f, "tail1"),
                new LocatorSpec(TAIL2_BONE, 0.0f, 0.0f, 0.0f, "tail2"),
                new LocatorSpec(TAIL3_BONE, 0.0f, 0.0f, 0.0f, "tail3"),
                new LocatorSpec(TAIL4_BONE, 0.0f, 0.0f, 0.0f, "tail4"),
                new LocatorSpec(LEFT_FRONT_LEG_BONE, 0.0f, 0.0f, 0.0f, "leftfrontleg"),
                new LocatorSpec(RIGHT_FRONT_LEG_BONE, 0.0f, 0.0f, 0.0f, "rightfrontleg"),
                new LocatorSpec(LEFT_BACK_LEG_BONE, 0.0f, 0.0f, 0.0f, "leftbackleg"),
                new LocatorSpec(RIGHT_BACK_LEG_BONE, 0.0f, 0.0f, 0.0f, "rightbackleg")
        };
    }

    @Override
    protected void afterDragonRender(Ignivorus entity, PoseStack poseStack,
                                     MultiBufferSource bufferSource, float partialTick) {
        if (!entity.isBaby()) {
            Vec3 mouth = getBoneWorldPosition(FIRE_BONE);
            if (mouth != null && !ShaderPassCompatibility.isIrisShadowPass()) {
                entity.setClientLocatorPosition("fireBreathVisualOrigin",
                        mouth.subtract(entity.position()).add(entity.getPosition(partialTick)));
            }
            IgnivorusSkyfallSphereRenderer.render(entity, poseStack, bufferSource, partialTick);
            IgnivorusSkyfallRaysRenderer.render(entity, getBoneWorldPosition("middlebody"),
                    poseStack, bufferSource, partialTick);
            IgnivorusFireballMouthRenderer.render(
                    entity, getBoneWorldPosition(FIRE_BONE), poseStack, bufferSource, partialTick);
            captureCollisionPose(entity);
            DragonDiveTrailRenderer.render(entity,
                    getBoneWorldPosition(DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE),
                    getBoneWorldPosition(DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE),
                    getBoneWorldPosition(DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE),
                    bufferSource,
                    poseStack.last());
        }
    }

    @Override
    public boolean shouldRender(Ignivorus entity, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) return true;
        return entity.isBreathingFire() && entity.distanceToSqr(camX, camY, camZ) <= 512.0D * 512.0D
                && frustum.isVisible(entity.getBoundingBox().inflate(ExpandingBreathSection.DEFAULT_RANGE + 32.0D));
    }

    private final Map<String, Matrix4f> collisionTransforms = new HashMap<>();

    @Override
    public void render(Ignivorus entity, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        collisionTransforms.clear();
        try {
            super.render(entity, yaw, partialTick, poses, buffers, light);
        } finally {
            collisionTransforms.clear();
        }
    }

    @Override
    public void renderRecursively(PoseStack poses, Ignivorus entity, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer vertices, boolean reRender,
                                  float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.renderRecursively(poses, entity, bone, type, buffers, vertices, reRender, partialTick,
                light, overlay, red, green, blue, alpha);
        if (reRender || entity.isBaby()
                || !RenderPassContext.isExtractionAllowed(entity.getId())
                || !IgnivorusHitboxes.BONES.contains(bone.getName())) return;
        poses.pushPose();
        try {
            RenderUtils.prepMatrixForBone(poses, bone);
            collisionTransforms.put(bone.getName(), RenderUtils.invertAndMultiplyMatrices(
                    poses.last().pose(), this.entityRenderTranslations));
        } finally {
            poses.popPose();
        }
    }

    private void captureCollisionPose(Ignivorus entity) {
        if (lastBakedModel == null || collisionTransforms.isEmpty()) return;
        var regions = IgnivorusHitboxes.REGIONS;
        AABB[] snapshot = new AABB[regions.size()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = DragonBoneSurfaceSampler.bounds(
                    lastBakedModel, collisionTransforms, regions.get(i).bones());
        }
        entity.getCollisionState().captureClientBounds(snapshot);
    }
}
