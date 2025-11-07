package com.leon.saintsdragons.client.model.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Ignivorus model using GeckoLib's built-in head tracking system
 */
public class IgnivorusModel extends DefaultedEntityGeoModel<Ignivorus> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/ignivorus.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/ignivorus.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus.png");

    public IgnivorusModel() {
        super(SaintsDragonsCommon.rl("ignivorus"), "head");
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
            applyNeckFollow();
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

    private void applyNeckFollow() {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();
        float headDeltaX = head.getRotX() - head.getInitialSnapshot().getRotX();
        float headDeltaY = head.getRotY() - head.getInitialSnapshot().getRotY();

        head.setRotX(head.getInitialSnapshot().getRotX());
        head.setRotY(head.getInitialSnapshot().getRotY());

        applyNeckBoneFollow("neck1LookControl", headDeltaX, headDeltaY, 0.15f);
        applyNeckBoneFollow("neck2LookControl", headDeltaX, headDeltaY, 0.20f);
        applyNeckBoneFollow("neck3LookControl", headDeltaX, headDeltaY, 0.25f);
        applyNeckBoneFollow("neck4LookControl", headDeltaX, headDeltaY, 0.30f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();
        bone.setRotX(snap.getRotX() + headDeltaX * weight);
        bone.setRotY(snap.getRotY() + headDeltaY * weight);
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
