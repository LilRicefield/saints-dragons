package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class VolitansModel extends DefaultedEntityGeoModel<Volitans> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/volitans.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/volitans.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans.png");

    public VolitansModel() {
        super(SaintsDragonsCommon.rl("volitans"));
    }

    @Override
    public ResourceLocation getModelResource(Volitans animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Volitans animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Volitans animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(Volitans entity, long instanceId, AnimationState<Volitans> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (!entity.isAlive() || entity.isDeadOrDying()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        applyBankingRoll(entity, animationState);
        applyFlightPitch(entity, animationState);
        applyNeckBankingLean(entity, animationState.getPartialTick());
        applyGroundNeckTurn(entity, animationState.getPartialTick());
        applySwimPitch(entity, animationState.getPartialTick());
        applySwimRoll(entity, animationState.getPartialTick());
        if (modelData != null) {
            applyHeadLookFollow(entity, modelData, animationState.getPartialTick());
        }
        applyTailDrag(entity, animationState.getPartialTick());
    }

    private void applyBankingRoll(Volitans entity, AnimationState<Volitans> state) {
        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        applyBoneRoll("body", bankAngleRad);
    }

    private void applyFlightPitch(Volitans entity, AnimationState<Volitans> state) {
        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);
        applyBonePitch("body", -pitchRad);
    }

    private void applyBoneRoll(String boneName, float rollRad) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }
        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();
        bone.setRotZ(snap.getRotZ() + rollRad);
    }

    private void applyBonePitch(String boneName, float pitchRad) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }
        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();
        bone.setRotX(snap.getRotX() + pitchRad);
    }

    private void applyHeadLookFollow(Volitans entity, EntityModelData modelData, float partialTick) {
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float structuralYawRad = (float) (bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.25f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.50f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.75f);
    }

    private void applyNeckBankingLean(Volitans entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.15f);
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.30f);
        applyNeckBoneRotation("headController", neckLeanRad * 0.75f);
    }

    private void applyGroundNeckTurn(Volitans entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float) (-velocity * Mth.DEG_TO_RAD);

        applyNeckBoneRotation("neck1Controller", turnRad * 0.15f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.30f);
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

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        bone.setRotX(bone.getRotX() + headDeltaX * weight);
        bone.setRotY(bone.getRotY() + headDeltaY * weight);
    }

    private void applySwimPitch(Volitans entity, float partialTick) {
        if (!entity.isInWaterOrBubble() || entity.isFlying()) {
            return;
        }

        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        float pitchDeg = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        float swimPitchRad = (float) Math.toRadians(Mth.clamp(pitchDeg, -45.0f, 45.0f));
        body.setRotX(body.getRotX() - swimPitchRad * 0.75f);
    }

    private void applySwimRoll(Volitans entity, float partialTick) {
        if (!entity.isInWaterOrBubble() || entity.isFlying()) {
            return;
        }

        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float swimRollRad = (float) velocity * Mth.DEG_TO_RAD * 0.35f;
        body.setRotZ(body.getRotZ() + swimRollRad);
    }

    private void applyTailDrag(Volitans entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float smoothedVelocity = entity.smoothTailDragVelocity((float) velocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;

        applyTailBoneRotation("tail1", velocityRad * 0.5f);
        applyTailBoneRotation("tail2", velocityRad * 0.75f);
        applyTailBoneRotation("tail3", velocityRad * 1.0f);
        applyTailBoneRotation("tail4", velocityRad * 1.25f);
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
