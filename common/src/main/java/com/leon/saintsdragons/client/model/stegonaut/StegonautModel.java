package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
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
        super(SaintsDragonsCommon.rl("stegonaut"), "head");
    }
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/stegonaut.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/stegonaut.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/stegonaut.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/stegonaut/stegonaut_female.png");

    @Override
    public ResourceLocation getModelResource(Stegonaut entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Stegonaut entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Stegonaut entity) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(Stegonaut entity, long instanceId, AnimationState<Stegonaut> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Fetch EntityModelData ONCE (best practice - avoid repeated HashMap lookups)
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();
        applyBodyRotationDeviation(entity, partialTick);
        applyTailDrag(entity, partialTick);
        applyNeckFollow(entity, modelData, partialTick);
    }

    private void applyBodyRotationDeviation(Stegonaut entity, float partialTick) {
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


    private void applyTailDrag(Stegonaut entity, float partialTick) {
        double velocity = entity.yawVelocity.get(partialTick);
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
        double bodyDeviation = entity.bodyRotDeviation.get(partialTick);

        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        totalYawRad = Mth.clamp(totalYawRad, -60.0f * Mth.DEG_TO_RAD, 60.0f * Mth.DEG_TO_RAD);

        float lookPitchRad = Mth.clamp(modelData.headPitch(), -20.0f, 20.0f) * Mth.DEG_TO_RAD;

        applyNeckBoneFollow("neck1", lookPitchRad, totalYawRad, 0.15f);
        applyNeckBoneFollow("neck2", lookPitchRad, totalYawRad, 0.20f);
        applyNeckBoneFollow("head", lookPitchRad, totalYawRad, 0.25f);
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
