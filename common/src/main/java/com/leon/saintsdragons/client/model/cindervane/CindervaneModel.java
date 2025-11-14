package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CindervaneModel extends DefaultedEntityGeoModel<Cindervane> {
    public CindervaneModel() {
        super(SaintsDragonsCommon.rl("cindervane"), "head");
    }

    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/cindervane.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/cindervane.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_female.png");


    @Override
    public void setCustomAnimations(Cindervane entity, long instanceId, AnimationState<Cindervane> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            applyBodyRotationDeviation(entity, partialTick);  // Smooth body rotation like Nulljaw/Raevyx
            applyBankingRoll(entity, animationState);
            applyNeckBankingLean(entity, partialTick);  // Lean neck into banking direction when ridden (flying)
            applyGroundNeckTurn(entity, partialTick);  // Turn neck based on ground turning
            applyNeckFollow();
            applyTailDrag(entity, partialTick);
        }
    }

    @Override
    public ResourceLocation getModelResource(Cindervane entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Cindervane entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Cindervane entity) {
        return ANIM;
    }

    /**
     * bodyRotDeviation tracks the difference between head and body rotation.
     * This creates the natural "head leads, body follows" behavior.
     */
    private void applyBodyRotationDeviation(Cindervane entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        // Get the smoothed head-body difference
        double deviation = entity.bodyRotDeviation.get(partialTick);

        // Convert to radians and apply
        // GeckoLib bones rotate left when positive, Minecraft rotates right when positive
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    /**
     * Apply smoothed banking roll straight to the body bone based on mouse drag.
     * Only applies when being ridden - wild Cindervanes don't bank.
     * FIXED: Always calculate from initialSnapshot to prevent cross-entity sync bleeding.
     */
    private void applyBankingRoll(Cindervane entity, AnimationState<Cindervane> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        // Banking right rotates negative around Z, hence the inversion.
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        // Set directly from snapshot + bank angle (no lerp with previous frame's bone rotation)
        // This prevents sync bleeding between multiple dragons rendering in the same frame
        body.setRotZ(snap.getRotZ() + bankAngleRad);
    }

    /**
     * Lean neck and head into the banking direction when being ridden.
     * Similar to tail drag but inverted - neck leans INTO the turn instead of dragging behind.
     * This creates a natural "looking into the turn" effect during flight.
     */
    private void applyNeckBankingLean(Cindervane entity, float partialTick) {
        // Only apply when being ridden and banking
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }

        // Get the banking angle (-90 to +90 degrees)
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);

        // Convert to radians and scale
        // Bank right → head turns left (opposite direction, like counter-steering)
        // So we NEGATE the banking angle
        // Scale: 45° bank = ~30° neck lean total
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        // Apply with increasing intensity toward the head (like tail but reversed hierarchy)
        applyNeckBoneRotation("neck1", neckLeanRad * 0.5f);  // Base of neck - subtle
        applyNeckBoneRotation("neck2", neckLeanRad * 1.0f);  // Mid neck - medium
        applyNeckBoneRotation("skull", neckLeanRad * 1.25f);   // Head - most pronounced
    }

    /**
     * Turn neck in the direction of ground turning based on yaw velocity.
     * Head leans INTO the turn direction when walking/running on ground.
     */
    private void applyGroundNeckTurn(Cindervane entity, float partialTick) {
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

        // Apply with increasing intensity toward the head
        applyNeckBoneRotation("neck1", turnRad * 0.5f);
        applyNeckBoneRotation("neck2", turnRad * 1.0f);
        applyNeckBoneRotation("skull", turnRad * 1.25f);
    }

    /**
     * Helper to apply Y-rotation to a neck bone.
     * ADDS to current rotation (preserves animation) instead of replacing it.
     */
    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        // Add to current rotation (which includes animation) instead of setting from snapshot
        bone.setRotY(bone.getRotY() + rotationY);
    }

    /**
     * Softly distributes head look rotations across the neck segments so the
     * turn feels organic instead of hinging on a single joint.
     */
    private void applyNeckFollow() {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) {
            return;
        }

        GeoBone head = headOpt.get();
        var snapshot = head.getInitialSnapshot();

        float headDeltaX = head.getRotX() - snapshot.getRotX();
        float headDeltaY = head.getRotY() - snapshot.getRotY();

        // Limit how far the neck blend can push the segments (roughly 20–25 deg each axis)
        headDeltaX = Mth.clamp(headDeltaX, -0.35f, 0.35f);
        headDeltaY = Mth.clamp(headDeltaY, -0.45f, 0.45f);

        // Reset the head bone so only the neck segments provide the visible rotation
        head.setRotX(snapshot.getRotX());
        head.setRotY(snapshot.getRotY());

        // Two neck controls: base gets a lighter amount, tip gets the majority for smoother motion
        applyNeckBoneFollow("neck1", headDeltaX, headDeltaY, 0.4f);
        applyNeckBoneFollow("neck2", headDeltaX, headDeltaY, 0.6f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        var snapshot = bone.getInitialSnapshot();

        bone.setRotX(snapshot.getRotX() + headDeltaX * weight);
        bone.setRotY(snapshot.getRotY() + headDeltaY * weight);
    }
    /**
     * Applies tail drag effect based on turning speed (yaw velocity).
     * Works for both wild and ridden dragons - tail swings with turn direction.
     */
    private void applyTailDrag(Cindervane entity, float partialTick) {
        // Use yawVelocity instead of bodyRotDeviation so it works when riding
        double velocity = entity.yawVelocity.get(partialTick);

        // Clamp velocity to prevent tail from going crazy during rapid movements
        velocity = Mth.clamp(velocity, -30.0, 30.0); // Max ~30 degrees of tail swing

        // Apply additional client-side smoothing to prevent snapping during sprint transitions
        // Server-side yawVelocity smoothing (0.25f) isn't enough for visual smoothness
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity); // Per-entity smoothing

        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;

        // Tail swings with increasing intensity toward tip
        applyTailBoneRotation("tail1", velocityRad * 1.0f);
        applyTailBoneRotation("tail2", velocityRad * 2.5f);
        applyTailBoneRotation("tail3", velocityRad * 3.0f);
    }

    /**
     * Helper to apply Y-rotation to a tail bone.
     * ADDS to current rotation (preserves animation) instead of replacing it.
     */
    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        // Add to current rotation (which includes animation) instead of setting from snapshot
        bone.setRotY(bone.getRotY() + rotationY);
    }

}


