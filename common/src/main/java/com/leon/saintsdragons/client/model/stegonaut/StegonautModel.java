package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class StegonautModel extends DefaultedEntityGeoModel<Stegonaut> {

    public StegonautModel() {
        super(SaintsDragonsCommon.rl("stegonaut"));
    }

    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/stegonaut.geo.json");
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_stegonaut.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/stegonaut.animation.json");
    private static final ResourceLocation BABY_ANIM = SaintsDragonsCommon.rl("animations/entity/baby_stegonaut.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/stegonaut.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/stegonaut_female.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/baby_stegonaut.png");
    private static final ResourceLocation BABY_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/baby_stegonaut_female.png");

    @Override
    public ResourceLocation getModelResource(Stegonaut entity) {
        return entity != null && entity.isBaby() ? BABY_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Stegonaut entity) {
        if (entity == null) {
            return MALE_TEXTURE;
        }
        if (entity.isBaby()) {
            return entity.isFemale() ? BABY_FEMALE_TEXTURE : BABY_TEXTURE;
        }
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Stegonaut entity) {
        return entity != null && entity.isBaby() ? BABY_ANIM : ANIM;
    }

    @Override
    public void setCustomAnimations(Stegonaut entity, long instanceId, AnimationState<Stegonaut> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (com.leon.saintsdragons.client.ui.DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();

        if(entity.isAlive()) {
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyTailDrag(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
        }
    }

    private void applyNeckBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyGroundNeckTurn(Stegonaut entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -25.0, 25.0);
        float turnRad = (float)(-velocity * Mth.DEG_TO_RAD);
        applyNeckBoneRotation("neck1Controller", turnRad * 0.3f);
        applyNeckBoneRotation("neck2Controller", turnRad * 0.5f);
        applyNeckBoneRotation("headController", turnRad * 0.35f);
    }

    private void applyBodyRotationDeviation(Stegonaut entity, float partialTick) {
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


    private void applyTailDrag(Stegonaut entity, float partialTick) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float targetVelocity = (float) velocity;
        float smoothedVelocity = entity.smoothTailDragVelocity(targetVelocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;
        applyTailBoneRotation("tail1", velocityRad * 0.5f);
        applyTailBoneRotation("tail2", velocityRad * 1.0f);
        applyTailBoneRotation("tail3", velocityRad * 1.5f);
    }

    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        bone.setRotY(bone.getRotY() + rotationY);
    }

    private void applyNeckFollow(Stegonaut entity, EntityModelData modelData, float partialTick) {
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.35f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.40f);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.45f);
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
}