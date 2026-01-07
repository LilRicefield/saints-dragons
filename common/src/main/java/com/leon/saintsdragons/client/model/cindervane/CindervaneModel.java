package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CindervaneModel extends DefaultedEntityGeoModel<Cindervane> {

public CindervaneModel() {
    super(SaintsDragonsCommon.rl("cindervane"));
}

    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/cindervane.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/cindervane.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_female.png");



    @Override
    public void setCustomAnimations(Cindervane entity, long instanceId, AnimationState<Cindervane> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Fetch EntityModelData ONCE (best practice - avoid repeated HashMap lookups)
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {

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
    private void applyBodyRotationDeviation(Cindervane entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applyBankingRoll(Cindervane entity, AnimationState<Cindervane> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        body.setRotZ(snap.getRotZ() + bankAngleRad);
    }

    private void applyFlightPitch(Cindervane entity, AnimationState<Cindervane> state) {
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

    private void applyNeckBankingLean(Cindervane entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.15f);
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.30f);
        applyNeckBoneRotation("neck3Controller", neckLeanRad * 0.45f);
        applyNeckBoneRotation("neck4Controller", neckLeanRad * 0.60f);
        applyNeckBoneRotation("headController", neckLeanRad * 0.75f);
    }

    private void applyGroundNeckTurn(Cindervane entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        double velocity = entity.yawVelocity.get(partialTick);

        velocity = Mth.clamp(velocity, -25.0, 25.0);

        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);

        applyNeckBoneRotation("neck1Controller", turnRad * 0.15f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.30f);
        applyNeckBoneRotation("neck3Controller", turnRad * 0.45f);
        applyNeckBoneRotation("neck4Controller", turnRad * 0.60f);
        applyNeckBoneRotation("headController", turnRad * 0.75f);
    }

    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(Cindervane entity, EntityModelData modelData, float partialTick) {

        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        double bodyDeviation = entity.bodyRotDeviation.get(partialTick);
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        // Distribute rotation across neck segments (DragonBodyControl prevents over-rotation)
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.15f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.30f);
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.45f);
        applyNeckBoneFollow("neck4Controller", lookPitchRad, totalYawRad, 0.60f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.75f);
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

    private void applyTailDrag(Cindervane entity, float partialTick) {
        double velocity = entity.yawVelocity.get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;
        applyTailBoneRotation("bone", velocityRad * 0.75f);
        applyTailBoneRotation("tail1", velocityRad * 1.0f);
        applyTailBoneRotation("tail2", velocityRad * 1.5f);
        applyTailBoneRotation("tail3", velocityRad * 2.0f);
        applyTailBoneRotation("tail4", velocityRad * 2.5f);
    }

    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }
        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }
}
