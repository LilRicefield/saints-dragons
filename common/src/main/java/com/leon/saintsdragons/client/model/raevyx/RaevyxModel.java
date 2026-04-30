package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
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

        super.setCustomAnimations(entity, instanceId, animationState);
        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        float partialTick = animationState.getPartialTick();
        if (entity.isAlive()) {
            if (entity.isTamingStunned()) {
                return;
            }
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyFlightRotations(entity, animationState);
            applyNeckBankingLean(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
            applyTailDrag(entity, partialTick);
        }
    }

    private void applyBodyRotationDeviation(Raevyx entity, float partialTick) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.getBodyRotDeviation().get(partialTick);
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applyFlightRotations(Raevyx entity, AnimationState<Raevyx> state) {
        var rootOpt = getBone("root");
        var bodyOpt = getBone("body");
        if (rootOpt.isEmpty() || bodyOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        GeoBone body = bodyOpt.get();
        var rootSnap = root.getInitialSnapshot();
        var snap = body.getInitialSnapshot();
        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        float barrelRollRad = entity.getSmoothedRoll(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        float totalRollRad = bankAngleRad + barrelRollRad;

        root.setRotX(rootSnap.getRotX() + pitchRad);
        body.setRotY(snap.getRotY());
        body.setRotZ(snap.getRotZ() + totalRollRad);

    }

    private void applyNeckBankingLean(Raevyx entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 35.0f * Mth.DEG_TO_RAD;
        applyNeckBoneRotation("neck1Controller", neckLeanRad * 0.20f);
        applyNeckBoneRotation("neck2Controller", neckLeanRad * 0.25f);
        applyNeckBoneRotation("neck3Controller", neckLeanRad * 0.30f);
        applyNeckBoneRotation("headController", neckLeanRad * 0.35f);
    }

    private void applyGroundNeckTurn(Raevyx entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);
        applyNeckBoneRotation("neck1Controller", turnRad * 0.20f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.25f);
        applyNeckBoneRotation("neck3Controller", turnRad * 0.30f);
        applyNeckBoneRotation("headController", turnRad * 0.35f);
    }

    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(Raevyx entity, EntityModelData modelData, float partialTick) {

        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.15f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.20f);
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.25f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.30f);
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

    private void applyTailDrag(Raevyx entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity); // Per-entity smoothing
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;
        applyTailBoneRotation("tail1", velocityRad * 0.5f);
        applyTailBoneRotation("tail2", velocityRad * 0.75f);
        applyTailBoneRotation("tail3", velocityRad * 1.0f);
        applyTailBoneRotation("tail4", velocityRad * 1.25f);
        applyTailBoneRotation("tail5", velocityRad * 1.75f);
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
