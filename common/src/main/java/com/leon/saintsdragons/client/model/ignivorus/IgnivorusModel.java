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


public class IgnivorusModel extends DefaultedEntityGeoModel<Ignivorus> {

    public IgnivorusModel() {
        super(SaintsDragonsCommon.rl("ignivorus"));
    }
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/ignivorus.geo.json");
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_ignivorus.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/ignivorus.animation.json");
    private static final ResourceLocation BABY_ANIM = SaintsDragonsCommon.rl("animations/entity/baby_ignivorus.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_female.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/baby_ignivorus.png");
    private static final ResourceLocation BABY_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/baby_ignivorus_female.png");
    private static final ResourceLocation TEXTURE_SECOND_VARIANT = SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus.png");
    private static final ResourceLocation FEMALE_TEXTURE_SECOND_VARIANT = SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus_female.png");


    @Override
    public ResourceLocation getModelResource(Ignivorus entity) {
        return entity != null && entity.isBaby() ? BABY_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Ignivorus entity) {
        if (entity == null) {
            return TEXTURE;
        }

        int variant = entity.getTextureVariant();
        boolean isFemale = entity.isFemale();

        if (entity.isBaby()) {
            return isFemale ? BABY_FEMALE_TEXTURE : BABY_TEXTURE;
        }
        if (variant == 1) {
            return isFemale ? FEMALE_TEXTURE_SECOND_VARIANT : TEXTURE_SECOND_VARIANT;
        } else {
            return isFemale ? FEMALE_TEXTURE : TEXTURE;
        }
    }

    @Override
    public ResourceLocation getAnimationResource(Ignivorus entity) {
        return entity != null && entity.isBaby() ? BABY_ANIM : ANIM;
    }

    @Override
    public void setCustomAnimations(Ignivorus entity, long instanceId, AnimationState<Ignivorus> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Fetch EntityModelData ONCE (best practice - avoid repeated HashMap lookups)
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            // Skip all procedural overlays while taming stunned so the raw stun animation isn't
            // contaminated by head-look or banking offsets.
            if (entity.isTamingStunned()) {
                return;
            }

            if (entity.isDeadOrDying()){
                return;
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyBankingRoll(entity, animationState);
            applyFlightPitch(entity, animationState);
            applyNeckFollow(entity, modelData, partialTick);
            applyNeckBankingLean(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
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

    private void applyFlightPitch(Ignivorus entity, AnimationState<Ignivorus> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) return;

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        body.setRotX(snap.getRotX() + pitchRad);
    }

    private void applyNeckBankingLean(Ignivorus entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 32.0f * Mth.DEG_TO_RAD;
        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.4f);
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.41f);
        applyNeckBoneRotation("neck3Controller", neckLeanRad * 0.42f);
        applyNeckBoneRotation("neck4Controller", neckLeanRad * 0.43f);
        applyNeckBoneRotation("headController", neckLeanRad * 0.44f);
    }

    private void applyGroundNeckTurn(Ignivorus entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }
        double velocity = entity.yawVelocity.get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);
        applyNeckBoneRotation("neck1Controller", turnRad * 0.4f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.41f);
        applyNeckBoneRotation("neck3Controller", turnRad * 0.42f);
        applyNeckBoneRotation("neck4Controller", turnRad * 0.43f);
        applyNeckBoneRotation("headController", turnRad * 0.44f);
    }

    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }
        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(Ignivorus entity, EntityModelData modelData, float partialTick) {

        // Get body deviation (how much head leads body)
        double bodyDeviation = entity.bodyRotDeviation.get(partialTick);

        // Combine look rotation + structural bend (NO CLAMPING - let body control handle it)
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        float lookPitchRad;
        if (entity.isVehicle() && entity.isFlying()) {
            // Use synced flight pitch for rider look so other clients see the same smoothed motion.
            lookPitchRad = entity.getFlightPitchRadians(partialTick);
            float maxPitchRad = 25.0f * Mth.DEG_TO_RAD;
            lookPitchRad = Mth.clamp(lookPitchRad, -maxPitchRad, maxPitchRad);
        } else {
            lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        }

        // Distribute rotation across neck segments (DragonBodyControl prevents over-rotation)
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.30f);  // Base
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.35f);  // Lower-mid
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.40f);  // Upper-mid
        applyNeckBoneFollow("neck4Controller", lookPitchRad, totalYawRad, 0.42f);  // Tip
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.45f);  // Tip
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
