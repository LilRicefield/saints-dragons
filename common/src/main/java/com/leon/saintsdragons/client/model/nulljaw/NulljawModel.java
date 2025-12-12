package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.ObjectUtils;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {
    public NulljawModel() {
        super(SaintsDragonsCommon.rl ("nulljaw"));
    }

    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/nulljaw.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/nulljaw.animation.json");
    private static final ResourceLocation MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw_female.png");


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

        // Fetch EntityModelData ONCE (best practice - avoid repeated HashMap lookups)
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();


        if (entity.isAlive()){
            if (entity.isDeadOrDying()){
                return;
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
            applyTailDrag(entity, partialTick);
            applyNeckFollow(entity, modelData, partialTick);
        }
    }

    private void applyBodyRotationDeviation(Nulljaw entity, float partialTick) {
        var rootOpt = getBone("body");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * net.minecraft.util.Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
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
        applyAdditiveRotation(bone, 0.0f, rotationY, 0.0f);
    }

    private void applyNeckFollow(Nulljaw entity, EntityModelData modelData, float partialTick) {
        var headOpt = getBone("headController");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();

        // Combine look rotation + structural bend (NO CLAMPING - let body control handle it)
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;

        double bodyDeviation = entity.bodyRotDeviation.get(partialTick);
        float structuralYawRad = (float)(bodyDeviation * 2.0 * Mth.DEG_TO_RAD);
        float totalYawRad = lookYawRad + structuralYawRad;

        // Reset head controller to snapshot (let neck bones handle the tracking)
        head.setRotX(head.getInitialSnapshot().getRotX());
        head.setRotY(head.getInitialSnapshot().getRotY());

        // Distribute rotation across neck segments (DragonBodyControl prevents over-rotation)
        applyNeckBoneFollow("neck1Controller", lookPitchRad, totalYawRad, 0.35f);
        applyNeckBoneFollow("neck2Controller", lookPitchRad, totalYawRad, 0.40f);
        applyNeckBoneFollow("neck3Controller", lookPitchRad, totalYawRad, 0.45f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;
        GeoBone bone = boneOpt.get();
        float addX = headDeltaX * weight;
        float addY = headDeltaY * weight;
        applyAdditiveRotation(bone, addX, addY, 0.0f);
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
        applyAdditiveRotation(bone, 0.0f, rotationY, 0.0f);
    }

    private void applyAdditiveRotation(GeoBone bone, float addX, float addY, float addZ) {
        var snap = bone.getInitialSnapshot();
        // Preserve animated pose (current - snapshot) and layer procedural rotation on top
        float animX = bone.getRotX() - snap.getRotX();
        float animY = bone.getRotY() - snap.getRotY();
        float animZ = bone.getRotZ() - snap.getRotZ();
        bone.setRotX(snap.getRotX() + animX + addX);
        bone.setRotY(snap.getRotY() + animY + addY);
        bone.setRotZ(snap.getRotZ() + animZ + addZ);
    }
}
