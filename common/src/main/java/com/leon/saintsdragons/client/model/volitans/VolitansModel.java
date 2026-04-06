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
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_volitans.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/volitans.animation.json");
    private static final ResourceLocation BABY_ANIM = SaintsDragonsCommon.rl("animations/entity/baby_volitans.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans_female.png");
    private static final ResourceLocation BLOODSHOT_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans_bloodshot.png");
    private static final ResourceLocation BLOODSHOT_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans_bloodshot_female.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/baby_volitans.png");
    private static final ResourceLocation BABY_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/baby_volitans_female.png");

    public VolitansModel() {
        super(SaintsDragonsCommon.rl("volitans"));
    }

    @Override
    public ResourceLocation getModelResource(Volitans animatable) {
        return animatable.isBaby() ? BABY_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Volitans animatable) {
        if (animatable.isBaby()) {
            return animatable.isFemale() ? BABY_FEMALE_TEXTURE : BABY_TEXTURE;
        }
        boolean bloodshot = animatable.getTextureVariant() == Volitans.VARIANT_BLOODSHOT;
        if (animatable.isFemale()) {
            return bloodshot ? BLOODSHOT_FEMALE_TEXTURE : FEMALE_TEXTURE;
        }
        return bloodshot ? BLOODSHOT_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Volitans animatable) {
        return animatable.isBaby() ? BABY_ANIM : ANIM;
    }

    @Override
    public void setCustomAnimations(Volitans entity, long instanceId, AnimationState<Volitans> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (com.leon.saintsdragons.client.ui.DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        if (entity.isAlive()){
            if (entity.isTamingStunned()) {
                return;
            }
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, animationState.getPartialTick());
            applyBankingRoll(entity, animationState);
            applyFlightPitch(entity, animationState);
            applyNeckBankingLean(entity, animationState.getPartialTick());
            applyGroundNeckTurn(entity, animationState.getPartialTick());
            applySwimPitch(entity, animationState.getPartialTick());
            applySwimRoll(entity, animationState.getPartialTick());
            applyTailDrag(entity, animationState.getPartialTick());

        }
    }

    private void applyBodyRotationDeviation(Volitans entity, float partialTick) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.getBodyRotDeviation().get(partialTick);
        float deviationRad = (float)(deviation * net.minecraft.util.Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applyBankingRoll(Volitans entity, AnimationState<Volitans> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }
        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();
        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        float barrelRollRad = entity.getSmoothedRoll(partialTick);
        body.setRotZ(snap.getRotZ() + bankAngleRad + barrelRollRad);
    }

    private void applyFlightPitch(Volitans entity, AnimationState<Volitans> state) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        root.setRotX(snap.getRotX() - pitchRad);
    }

    private void applyNeckFollow(Volitans entity, EntityModelData modelData, float partialTick) {
        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.25f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.50f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 1.0f);
    }

    private void applyNeckBankingLean(Volitans entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.25f);
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.50f);
        applyNeckBoneRotation("headController", neckLeanRad * 1.0f);
    }

    private void applyGroundNeckTurn(Volitans entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float) (-velocity * Mth.DEG_TO_RAD);

        applyNeckBoneRotation("neck1Controller", turnRad * 0.25f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.50f);
        applyNeckBoneRotation("headController", turnRad * 1.0f);
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
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        float addX = headDeltaX * weight;
        float addY = headDeltaY * weight;

        bone.setRotX(bone.getRotX() + addX);
        bone.setRotY(bone.getRotY() + addY);
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
        float swimPitchRad = Mth.clamp(entity.getFlightPitchRadians(partialTick), -Mth.HALF_PI, Mth.HALF_PI);
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
