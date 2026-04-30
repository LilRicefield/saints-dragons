package com.leon.saintsdragons.client.model.varasuchus;

import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class VarasuchusModel extends DefaultedEntityGeoModel<Varasuchus> {
    public VarasuchusModel() {
        super(SaintsDragonsCommon.rl ("varasuchus"));
    }

    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/varasuchus.geo.json");
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_varasuchus.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/varasuchus.animation.json");
    private static final ResourceLocation BABY_ANIM = SaintsDragonsCommon.rl("animations/entity/baby_varasuchus.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus_female.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/varasuchus/baby_varasuchus.png");
    private static final ResourceLocation BABY_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/varasuchus/baby_varasuchus_female.png");


    @Override
    public ResourceLocation getModelResource(Varasuchus entity) {
        return entity != null && entity.isBaby() ? BABY_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Varasuchus entity) {
        if (entity == null) {
            return MALE_TEXTURE;
        }
        if (entity.isBaby()) {
            return entity.isFemale() ? BABY_FEMALE_TEXTURE : BABY_TEXTURE;
        }
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Varasuchus entity) {
        return entity != null && entity.isBaby() ? BABY_ANIM : ANIM;
    }

    @Override
    public void setCustomAnimations(Varasuchus entity, long instanceId, AnimationState<Varasuchus> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        float partialTick = animationState.getPartialTick();
        if (entity.isAlive()){
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
            applyTailDrag(entity, partialTick);
            applySwimPitch(entity, partialTick);
            applySwimRoll(entity, partialTick);
        }
    }

    private void applyBodyRotationDeviation(Varasuchus entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) {
            return;
        }
        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.getBodyRotDeviation().get(partialTick);
        float deviationRad = (float)(deviation * net.minecraft.util.Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    private void applyGroundNeckTurn(Varasuchus entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
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

    private void applyNeckFollow(Varasuchus entity, EntityModelData modelData, float partialTick) {
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.35f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.40f);
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.45f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.46f);
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

    private void applyTailDrag(Varasuchus entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
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

    private void applySwimPitch(Varasuchus entity, float partialTick) {
        if (!entity.isInWater() && !entity.isInWaterOrBubble()) {
            return;
        }
        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }
        GeoBone body = bodyOpt.get();
        float swimPitchRad = entity.getSwimPitchRadians(partialTick);
        body.setRotX(body.getRotX() + swimPitchRad);
    }

    private void applySwimRoll(Varasuchus entity, float partialTick) {
        if (!entity.isInWater() && !entity.isInWaterOrBubble()) {
            return;
        }

        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }
        GeoBone body = bodyOpt.get();
        float swimRollDegrees = entity.getSwimRollAngleDegrees(partialTick);
        float swimRollRad = swimRollDegrees * Mth.DEG_TO_RAD;

        body.setRotZ(body.getRotZ() + swimRollRad);
    }
}