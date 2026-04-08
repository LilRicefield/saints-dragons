package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public final class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/nulljaw.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/nulljaw.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw_female.png");
    public NulljawModel() {
        super(SaintsDragonsCommon.rl("nulljaw"));
    }

    @Override
    public ResourceLocation getModelResource(Nulljaw animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Nulljaw animatable) {
        return animatable.isFemale() ? FEMALE_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Nulljaw animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(Nulljaw entity, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (com.leon.saintsdragons.client.ui.DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null || !entity.isAlive() || entity.isDeadOrDying()) {
            return;
        }

        float partialTick = animationState.getPartialTick();
        applyBodyRotationDeviation(entity, partialTick);
        applyFlightPitch(entity, partialTick);
        applyNeckFollow(entity, modelData, partialTick);
        applyTailDrag(entity, partialTick);
    }

    private void applyBodyRotationDeviation(Nulljaw entity, float partialTick) {
        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }

        float deviationRad = (float) (entity.getBodyRotDeviation().get(partialTick) * Mth.DEG_TO_RAD);
        GeoBone body = bodyOpt.get();
        body.setRotY(body.getRotY() - deviationRad);
    }

    private void applyFlightPitch(Nulljaw entity, float partialTick) {
        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }

        float pitchRad = Mth.clamp(-entity.getFlightPitchRadians(partialTick) * 1.75F, -0.70F, 0.70F);
        GeoBone body = bodyOpt.get();
        body.setRotX(body.getRotX() + pitchRad);
    }

    private void applyNeckFollow(Nulljaw entity, EntityModelData modelData, float partialTick) {
        if (entity.isVehicle()) {
            return;
        }

        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float) (bodyDeviation * 1.4D * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD * 0.35F;

        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.25F);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.45F);
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.65F);
        applyNeckBoneFollow("headController", lookPitchRad, totalYawRad, 0.85F);
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

    private void applyTailDrag(Nulljaw entity, float partialTick) {
        double yawVelocity = entity.getYawVelocity().get(partialTick);
        yawVelocity = Mth.clamp(yawVelocity, -30.0D, 30.0D);
        float smoothedVelocity = entity.smoothTailDragVelocity((float) yawVelocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;

        applyTailBoneRotation("tail1", velocityRad * 0.45F);
        applyTailBoneRotation("tail2", velocityRad * 0.70F);
        applyTailBoneRotation("tail3", velocityRad * 0.95F);
        applyTailBoneRotation("tail4", velocityRad * 1.20F);
        applyTailBoneRotation("tail5", velocityRad * 1.45F);
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
