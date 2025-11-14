package com.leon.saintsdragons.client.model.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Ignivorus model using GeckoLib's built-in head tracking system
 */
public class IgnivorusModel extends DefaultedEntityGeoModel<Ignivorus> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/ignivorus.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/ignivorus.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus.png");

    public IgnivorusModel() {
        super(SaintsDragonsCommon.rl("ignivorus"), "headLookControl");
    }

    @Override
    public ResourceLocation getModelResource(Ignivorus entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Ignivorus entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Ignivorus entity) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(Ignivorus entity, long instanceId, AnimationState<Ignivorus> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            applyBodyRotationDeviation(entity, partialTick);
            applyBankingRoll(entity, animationState);
            applyNeckFollow(animationState);
            applyNeckBankingLean(entity, partialTick);  // Lean neck into banking direction when ridden (flying)
            applyGroundNeckTurn(entity, partialTick);  // Turn neck based on ground turning
            applyTailDrag(entity, partialTick);
        }
    }

    private void applyBodyRotationDeviation(Ignivorus entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) return;

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);
        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applyBankingRoll(Ignivorus entity, AnimationState<Ignivorus> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) return;

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();
        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        body.setRotZ(snap.getRotZ() + bankAngleRad);
    }

    /**
     * Lean neck into the banking direction when being ridden.
     * Bank right → head turns left (opposite direction).
     * Uses the structural neck bones (neck1-4, head) with keyframes to prevent crazy spinning.
     */
    private void applyNeckBankingLean(Ignivorus entity, float partialTick) {
        // Only apply when being ridden and banking
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }

        // Get the banking angle (-90 to +90 degrees)
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);

        // Convert to radians and scale
        // Bank right → head turns left (opposite direction)
        // Scale: 45° bank = ~32° neck lean total (between Cindervane and Raevyx)
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 32.0f * Mth.DEG_TO_RAD;

        // Apply with increasing intensity toward the head (4 neck segments + head)
        applyNeckBoneRotation("neck1", neckLeanRad * 0.4f);  // Base - subtle
        applyNeckBoneRotation("neck2", neckLeanRad * 0.41f);  // Lower-mid
        applyNeckBoneRotation("neck3", neckLeanRad * 0.42f);  // Upper-mid
        applyNeckBoneRotation("neck4", neckLeanRad * 0.43f);  // Near head
        applyNeckBoneRotation("head", neckLeanRad * 0.44f);   // Head - most pronounced
    }

    /**
     * Turn neck in the direction of ground turning based on yaw velocity.
     * Head leans INTO the turn direction when walking/running on ground.
     */
    private void applyGroundNeckTurn(Ignivorus entity, float partialTick) {
        // Only apply when on ground (not flying)
        if (entity.isFlying()) {
            return;
        }

        // Use yaw velocity to determine turn direction and magnitude
        double velocity = entity.yawVelocity.get(partialTick);

        // Clamp to prevent excessive rotation
        velocity = Mth.clamp(velocity, -25.0, 25.0);

        // Convert to radians - head turns IN the direction of the turn (opposite of tail drag)
        // So we NEGATE the velocity
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);

        // Apply with same values as banking lean (4 neck segments + head)
        applyNeckBoneRotation("neck1", turnRad * 0.4f);
        applyNeckBoneRotation("neck2", turnRad * 0.41f);
        applyNeckBoneRotation("neck3", turnRad * 0.42f);
        applyNeckBoneRotation("neck4", turnRad * 0.43f);
        applyNeckBoneRotation("head", turnRad * 0.44f);
    }

    /**
     * Helper to apply Y-rotation to a neck bone for banking lean.
     * ADDS to current rotation (preserves animation keyframes) instead of replacing it.
     */
    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        // Add to current rotation (which includes animation keyframes) instead of setting from snapshot
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(AnimationState<Ignivorus> state) {
        var controllerOpt = getBone("headLookControl");
        if (controllerOpt.isEmpty()) {
            return;
        }

        EntityModelData modelData = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) {
            return;
        }

        GeoBone controller = controllerOpt.get();
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;

        controller.setRotX(controller.getRotX() - lookPitchRad);
        controller.setRotY(controller.getRotY() - lookYawRad);

        applyNeckBoneFollow("neck1", lookPitchRad, lookYawRad, 0.35f);
        applyNeckBoneFollow("neck2", lookPitchRad, lookYawRad, 0.40f);
        applyNeckBoneFollow("neck3", lookPitchRad, lookYawRad, 0.45f);
        applyNeckBoneFollow("neck4", lookPitchRad, lookYawRad, 0.50f);
        applyNeckBoneFollow("head", lookPitchRad, lookYawRad, 0.60f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        float addX = headDeltaX * weight;
        float addY = headDeltaY * weight;
        bone.setRotX(bone.getRotX() + addX);
        bone.setRotY(bone.getRotY() + addY);
    }

    private void applyTailDrag(Ignivorus entity, float partialTick) {
        double velocity = entity.yawVelocity.get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;

        applyTailBoneRotation("tail1", velocityRad * 0.5f);
        applyTailBoneRotation("tail2", velocityRad * 0.75f);
        applyTailBoneRotation("tail3", velocityRad * 1.0f);
        applyTailBoneRotation("tail4", velocityRad * 1.25f);
    }

    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;
        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }
}


