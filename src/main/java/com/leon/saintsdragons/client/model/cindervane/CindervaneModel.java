package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CindervaneModel extends DefaultedEntityGeoModel<Cindervane> {
    public CindervaneModel() {
        super(SaintsDragons.rl("cindervane"), "head");
    }

    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/cindervane/cindervane.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/cindervane/cindervane_female.png");


    @Override
    public void setCustomAnimations(Cindervane entity, long instanceId, AnimationState<Cindervane> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (entity.isAlive()) {
            applyBankingRoll(entity, animationState);
            applyNeckFollow();
        }
    }

    @Override
    public ResourceLocation getTextureResource(Cindervane entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    /**
     * Apply smoothed banking roll straight to the body bone based on mouse drag.
     * Only applies when being ridden - wild Cindervanes don't bank.
     * FIXED: Always calculate from initialSnapshot to prevent cross-entity sync bleeding.
     */
    private void applyBankingRoll(Cindervane entity, AnimationState<Cindervane> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        // Banking right rotates negative around Z, hence the inversion.
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        // Set directly from snapshot + bank angle (no lerp with previous frame's bone rotation)
        // This prevents sync bleeding between multiple dragons rendering in the same frame
        body.setRotZ(snap.getRotZ() + bankAngleRad);
    }

    /**
     * Softly distributes head look rotations across the neck segments so the
     * turn feels organic instead of hinging on a single joint.
     */
    private void applyNeckFollow() {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) {
            return;
        }

        GeoBone head = headOpt.get();
        var snapshot = head.getInitialSnapshot();

        float headDeltaX = head.getRotX() - snapshot.getRotX();
        float headDeltaY = head.getRotY() - snapshot.getRotY();

        // Limit how far the neck blend can push the segments (roughly 20–25 deg each axis)
        headDeltaX = Mth.clamp(headDeltaX, -0.35f, 0.35f);
        headDeltaY = Mth.clamp(headDeltaY, -0.45f, 0.45f);

        // Reset the head bone so only the neck segments provide the visible rotation
        head.setRotX(snapshot.getRotX());
        head.setRotY(snapshot.getRotY());

        // Two neck controls: base gets a lighter amount, tip gets the majority for smoother motion
        applyNeckBoneFollow("neck1LookControl", headDeltaX, headDeltaY, 0.4f);
        applyNeckBoneFollow("neck2LookControl", headDeltaX, headDeltaY, 0.6f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        var snapshot = bone.getInitialSnapshot();

        bone.setRotX(snapshot.getRotX() + headDeltaX * weight);
        bone.setRotY(snapshot.getRotY() + headDeltaY * weight);
    }
}

