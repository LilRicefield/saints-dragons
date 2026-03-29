package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
/**
 * Raevyx (Lightning Dragon) model using GeckoLib's built-in head tracking system.
 * Only procedural animation: banking roll for flight physics.
 */
public class RaevyxModel extends DefaultedEntityGeoModel<Raevyx> {

    public  RaevyxModel() {
        super(SaintsDragonsCommon.rl("raevyx"));
    }

    private static final ResourceLocation ADULT_MODEL = SaintsDragonsCommon.rl("geo/entity/raevyx.geo.json");
    private static final ResourceLocation FEMALE_MODEL = SaintsDragonsCommon.rl("geo/entity/raevyx.geo.json");
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_raevyx.geo.json");

    private static final ResourceLocation ADULT_ANIM = SaintsDragonsCommon.rl("animations/entity/raevyx.animation.json");
    private static final ResourceLocation BABY_ANIM = SaintsDragonsCommon.rl("animations/entity/baby_raevyx.animation.json");

    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_female.png");
    private static final ResourceLocation NIGHT_GOLD_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold.png");
    private static final ResourceLocation NIGHT_GOLD_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_female.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/baby_raevyx.png");


    @Override
    public ResourceLocation getModelResource(Raevyx entity) {
        if (entity.isBaby()) {
            return BABY_MODEL;
        }
        if (entity.isFemale()) {
            return FEMALE_MODEL;
        }
        return ADULT_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Raevyx entity) {
        if (entity.isBaby()) {
            return BABY_TEXTURE;
        }
        boolean nightGold = entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        if (nightGold) {
            return entity.isFemale() ? NIGHT_GOLD_FEMALE_TEXTURE : NIGHT_GOLD_TEXTURE;
        }
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Raevyx entity) {
        return entity.isBaby() ? BABY_ANIM : ADULT_ANIM;
    }


    @Override
    public void setCustomAnimations(Raevyx entity, long instanceId, AnimationState<Raevyx> animationState) {
        // Let GeckoLib handle head tracking automatically
        super.setCustomAnimations(entity, instanceId, animationState);

        // Skip custom animations when rendering in GUI (like the Draconic Codex)
        if (com.leon.saintsdragons.client.ui.DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        // Fetch EntityModelData ONCE (best practice - avoid repeated HashMap lookups)
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            // Skip procedural overlays while taming stunned so the raw stun animation isn't affected
            if (entity.isTamingStunned()) {
                return;
            }

            if (entity.isDeadOrDying()){
                return;
            }
            applyBodyRotationDeviation(entity, partialTick);  // Same as Varasuchus/Stegonaut
            applyBankingRoll(entity, animationState);
            applyFlightPitch(entity, animationState);
            applyNeckFollow(entity, modelData, partialTick);   // Base head tracking first (uses animation snapshot)
            applyNeckBankingLean(entity, partialTick); // Then layer procedural leans so they add on top
            applyGroundNeckTurn(entity, partialTick);  // Same for ground turning
            applyTailDrag(entity, partialTick);
        }
    }

    /**
     * Applies smooth body rotation using deviation approach.
     * bodyRotDeviation tracks the difference between head and body rotation.
     * This creates the natural "head leads, body follows" behavior.
     */
    private void applyBodyRotationDeviation(Raevyx entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        // Get the smoothed head-body difference
        double deviation = entity.getBodyRotDeviation().get(partialTick);

        // Convert to radians and apply
        // GeckoLib bones rotate left when positive, Minecraft rotates right when positive
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    /**
     * Apply smoothed banking roll straight to the body bone so we can lean at any angle.
     * FIXED: Always calculate from initialSnapshot to prevent cross-entity sync bleeding.
     */
    private void applyBankingRoll(Raevyx entity, AnimationState<Raevyx> state) {
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

    private void applyFlightPitch(Raevyx entity, AnimationState<Raevyx> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        body.setRotX(snap.getRotX() + pitchRad);
    }

    /**
     * Lean neck into the banking direction when being ridden.
     * Bank right → head turns left (opposite direction).
     * This creates a natural counter-steering effect during flight.
     */
    private void applyNeckBankingLean(Raevyx entity, float partialTick) {
        // Only apply when being ridden and banking
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }

        // Get the banking angle (-90 to +90 degrees)
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);

        // Convert to radians and scale
        // Bank right → head turns left (opposite direction)
        // Scale: 45° bank = ~35° neck lean total (Raevyx has longer neck than Cindervane)
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 35.0f * Mth.DEG_TO_RAD;

        // Apply with increasing intensity toward the head (4 neck segments)
        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.15f);  // Base - subtle
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.20f);  // Lower-mid
        applyNeckBoneRotation("neck3Controller", neckLeanRad * 0.25f);  // Upper-midNear head
        applyNeckBoneRotation("headController", neckLeanRad * 0.30f);   // Head - most pronounced
    }

    /**
     * Turn neck in the direction of ground turning based on yaw velocity.
     * Head leans INTO the turn direction when walking/running on ground.
     */
    private void applyGroundNeckTurn(Raevyx entity, float partialTick) {
        // Only apply when on ground (not flying)
        if (entity.isFlying()) {
            return;
        }

        // Use yaw velocity to determine turn direction and magnitude
        double velocity = entity.getYawVelocity().get(partialTick);

        // Clamp to prevent excessive rotation
        velocity = Mth.clamp(velocity, -25.0, 25.0);

        // Convert to radians - head turns IN the direction of the turn (opposite of tail drag)
        // So we NEGATE the velocity
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);

        // Apply with same values as banking lean (4 neck segments)
        applyNeckBoneRotation("neck1Controller", turnRad * 0.3f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.5f);
        applyNeckBoneRotation("neck3Controller", turnRad * 0.7f);
        applyNeckBoneRotation("headController", turnRad * 0.35f);
    }

    /**
     * Helper to apply Y-rotation to a neck bone for banking lean.
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

    private void applyNeckFollow(Raevyx entity, EntityModelData modelData, float partialTick) {

        // Get body deviation (how much head leads body)
        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);

        // Combine look rotation + structural bend (NO CLAMPING - let body control handle it)
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        // Simple pitch conversion (NO CLAMPING - let body control handle it)
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }


        // Distribute rotation across neck segments (DragonBodyControl prevents over-rotation)
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.15f);  // Base
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.20f);  // Lower-mid
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.25f);  // Upper-mid
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.30f);  // Tip
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        // Apply weighted portion of the head's rotation on top of the animated pose
        float addX = headDeltaX * weight;
        float addY = headDeltaY * weight;

        bone.setRotX(bone.getRotX() + addX);
        bone.setRotY(bone.getRotY() + addY);
    }

    /**
     * Applies tail drag effect based on turning speed (yaw velocity).
     * Works for both wild and ridden dragons - tail swings with turn direction.
     */
    private void applyTailDrag(Raevyx entity, float partialTick) {
        // Use yawVelocity instead of bodyRotDeviation so it works when riding
        double velocity = entity.getYawVelocity().get(partialTick);

        // Clamp velocity to prevent tail from going crazy during rapid movements (takeoff, dodging, etc.)
        velocity = Mth.clamp(velocity, -30.0, 30.0); // Max ~30 degrees of tail swing

        // Apply additional client-side smoothing to prevent snapping during sprint transitions
        // Server-side yawVelocity smoothing (0.25f) isn't enough for visual smoothness
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity); // Per-entity smoothing

        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;

        // Tail swings with increasing intensity toward tip
        applyTailBoneRotation("tail1", velocityRad * 0.5f);
        applyTailBoneRotation("tail2", velocityRad * 0.75f);
        applyTailBoneRotation("tail3", velocityRad * 1.0f);
        applyTailBoneRotation("tail4", velocityRad * 1.25f);
        applyTailBoneRotation("tail5", velocityRad * 1.75f);
    }

    /**
     * Helper to apply Y-rotation to a tail bone.
     * ADDS to current rotation (preserves animation keyframes from GeckoLib).
     */
    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        // Add to current rotation (which includes animation keyframes)
        bone.setRotY(bone.getRotY() + rotationY);
    }
}
