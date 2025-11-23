package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/nulljaw.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/nulljaw.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw_female.png");

    public NulljawModel() {
        super(SaintsDragonsCommon.rl("nulljaw"), "headController");
    }

    @Override
    public ResourceLocation getModelResource(Nulljaw entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Nulljaw entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Nulljaw entity) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(Nulljaw entity, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        float partialTick = animationState.getPartialTick();
        applyBodyRotationDeviation(entity, partialTick);
        if (entity.isAlive() && entity.isSwimming()) {
            applySwimRoll(entity, animationState);
        }
        applyGroundNeckTurn(entity, partialTick);
        applyTailDrag(entity, partialTick);
        applyNeckFollow(entity, animationState);
    }

    private void applyBodyRotationDeviation(Nulljaw entity, float partialTick) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * net.minecraft.util.Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applySwimRoll(Nulljaw entity, AnimationState<Nulljaw> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) return;

        GeoBone body = bodyOpt.get();

        float partialTick = state.getPartialTick();
        float swimRollDeg = entity.getSwimRollAngleDegrees(partialTick);
        float swimRollRad = Mth.clamp(-swimRollDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        // Add to whatever the animation already set (don't use snapshot - we want to layer on top)
        body.setRotZ(body.getRotZ() + swimRollRad);
    }

    private void applyGroundNeckTurn(Nulljaw entity, float partialTick) {
        double velocity = entity.yawVelocity.get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);
        applyNeckBoneRotation("neck1Controller", turnRad * 0.4f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.42f);
        applyNeckBoneRotation("neck3Controller", turnRad * 0.44f);
        applyNeckBoneRotation("headController", turnRad * 0.46f);
    }

    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }
        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(Nulljaw entity, AnimationState<Nulljaw> state) {
        var headOpt = getBone("headController");
        if (headOpt.isEmpty()) return;
        GeoBone head = headOpt.get();
        float partialTick = state.getPartialTick();
        float headDeltaX = head.getRotX() - head.getInitialSnapshot().getRotX();
        float headDeltaY = head.getRotY() - head.getInitialSnapshot().getRotY();
        double bodyDeviation = entity.bodyRotDeviation.get(partialTick);
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = headDeltaY + structuralYawRad;
        totalYawRad = Mth.clamp(totalYawRad, -60.0f * Mth.DEG_TO_RAD, 60.0f * Mth.DEG_TO_RAD);
        float clampedPitchRad = Mth.clamp(headDeltaX, -20.0f * Mth.DEG_TO_RAD, 20.0f * Mth.DEG_TO_RAD);
        head.setRotX(head.getInitialSnapshot().getRotX());
        head.setRotY(head.getInitialSnapshot().getRotY());
        applyNeckBoneFollow("neck1Controller", clampedPitchRad, totalYawRad, 0.35f);
        applyNeckBoneFollow("neck2Controller", clampedPitchRad, totalYawRad, 0.40f);
        applyNeckBoneFollow("neck3Controller", clampedPitchRad, totalYawRad, 0.45f);
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

    private void applyTailDrag(Nulljaw entity, float partialTick) {
        double velocity = entity.yawVelocity.get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;
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
