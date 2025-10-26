package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
/**
 * Raevyx (Lightning Dragon) model using GeckoLib's built-in head tracking system.
 * The "head" bone parents all neck bones, so GeckoLib automatically rotates the entire chain.
 * Only procedural animation: banking roll for flight physics.
 */
public class RaevyxModel extends DefaultedEntityGeoModel<Raevyx> {
    private static final ResourceLocation ADULT_MODEL = ResourceLocation.fromNamespaceAndPath("saintsdragons", "geo/entity/raevyx.geo.json");
    private static final ResourceLocation FEMALE_MODEL = ResourceLocation.fromNamespaceAndPath("saintsdragons", "geo/entity/raevyx.geo.json");
    private static final ResourceLocation BABY_MODEL = ResourceLocation.fromNamespaceAndPath("saintsdragons", "geo/entity/baby_raevyx.geo.json");

    private static final ResourceLocation ADULT_ANIM = ResourceLocation.fromNamespaceAndPath("saintsdragons", "animations/entity/raevyx.animation.json");
    private static final ResourceLocation BABY_ANIM = ResourceLocation.fromNamespaceAndPath("saintsdragons", "animations/entity/baby_raevyx.animation.json");

    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/raevyx.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/raevyx_female.png");
    private static final ResourceLocation BABY_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/baby_raevyx.png");

    public RaevyxModel() {
        // Defaulted paths under entity/ and built-in head rotation for "head" bone
        super(SaintsDragons.rl("raevyx"),"head");
    }

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
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Raevyx entity) {
        return entity.isBaby() ? BABY_ANIM : ADULT_ANIM;
    }


    @Override
    public void setCustomAnimations(Raevyx entity, long instanceId, AnimationState<Raevyx> animationState) {
        // Let GeckoLib handle head tracking automatically
        super.setCustomAnimations(entity, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            applyBodyRotationDeviation(entity, partialTick);  // Same as Nulljaw/Stegonaut
            applyBankingRoll(entity, animationState);
            applyNeckFollow(entity, animationState);
            applyTailDrag(entity, partialTick);
        }
    }

    /**
     * Applies smooth body rotation using AstemirLib's deviation approach.
     * bodyRotDeviation tracks the difference between head and body rotation.
     * This creates the natural "head leads, body follows" behavior.
     */
    private void applyBodyRotationDeviation(Raevyx entity, float partialTick) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        // Get the smoothed head-body difference
        double deviation = entity.bodyRotDeviation.get(partialTick);

        // Convert to radians and apply
        // GeckoLib bones rotate left when positive, Minecraft rotates right when positive
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    /**
     * Apply smoothed banking roll straight to the body bone so we can lean at any angle.
     * FIXED: Always calculate from initialSnapshot to prevent cross-entity sync bleeding.
     */
    private void applyBankingRoll(Raevyx entity, AnimationState<Raevyx> state) {
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
     * Distributes the parent "head" bone's rotation across neck segments like a giraffe.
     * GeckoLib rotates the main "head" bone, and we distribute that rotation so each
     * neck segment contributes a portion, creating smooth natural movement.
     * Simple version like Nulljaw - no special beaming logic.
     */
    private void applyNeckFollow(Raevyx entity, AnimationState<Raevyx> state) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();

        // Get how much GeckoLib rotated the parent "head" bone
        float headDeltaX = head.getRotX() - head.getInitialSnapshot().getRotX();
        float headDeltaY = head.getRotY() - head.getInitialSnapshot().getRotY();

        // COUNTER-ROTATE the parent "head" bone so it doesn't rotate rigidly
        // We'll redistribute this rotation across the neck segments instead
        head.setRotX(head.getInitialSnapshot().getRotX());
        head.setRotY(head.getInitialSnapshot().getRotY());

        // Now distribute the rotation across neck segments (4 segments for Raevyx)
        applyNeckBoneFollow("neck1LookControl", headDeltaX, headDeltaY, 0.15f);  // Base
        applyNeckBoneFollow("neck2LookControl", headDeltaX, headDeltaY, 0.20f);  // Lower-mid
        applyNeckBoneFollow("neck3LookControl", headDeltaX, headDeltaY, 0.25f);  // Upper-mid
        applyNeckBoneFollow("neck4LookControl", headDeltaX, headDeltaY, 0.30f);  // Tip
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();

        // Apply weighted portion of the head's rotation
        float addX = headDeltaX * weight;
        float addY = headDeltaY * weight;

        // Set directly from snapshot (no lerp to avoid cross-entity sync)
        bone.setRotX(snap.getRotX() + addX);
        bone.setRotY(snap.getRotY() + addY);
    }

    private void applyTailDrag(Raevyx entity, float partialTick) {
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);
        applyTailBoneRotation("tail1Controller", deviationRad * 0.5f);
        applyTailBoneRotation("tail2Controller", deviationRad * 1.0f);
        applyTailBoneRotation("tail3Controller", deviationRad * 1.5f);
        applyTailBoneRotation("tail4Controller", deviationRad * 2.0f);
        applyTailBoneRotation("tail5Controller", deviationRad * 2.5f);
    }

    /**
     * Helper to apply Y-rotation to a tail bone
     */
    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();
        bone.setRotY(snap.getRotY() + rotationY);
    }
}
