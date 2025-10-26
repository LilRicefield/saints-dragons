package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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
            applyHeadClamp(entity);
            applyNeckFollow(entity, animationState);
        }
    }

    /**
     * Manually applies smoothed head rotation using vanilla's interpolation.
     * This replaces GeckoLib's automatic head tracking with explicit smoothed values.
     */
    private void applySmoothedHeadRotation(Raevyx entity, float partialTick) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();
        var snap = head.getInitialSnapshot();

        // Use vanilla's interpolated rotation values for smoothness
        float entityYaw = entity.yBodyRot;
        float headYaw = Mth.lerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Convert to relative rotation (head offset from body)
        float relativeYaw = Mth.degreesDifference(entityYaw, headYaw) * Mth.DEG_TO_RAD;
        float relativePitch = -headPitch * Mth.DEG_TO_RAD;

        // Apply to head bone
        head.setRotY(snap.getRotY() + relativeYaw);
        head.setRotX(snap.getRotX() + relativePitch);
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
     * Clamps the main "head" bone rotation to prevent extreme angles.
     * This bone is controlled by GeckoLib but we limit how far it can turn.
     */
    private void applyHeadClamp(Raevyx entity) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();
        var snap = head.getInitialSnapshot();

        float deltaY = head.getRotY() - snap.getRotY();
        float deltaX = head.getRotX() - snap.getRotX();

        // Clamp to reasonable limits
        float yawClamp = entity.isFlying() ? 0.9f : 0.6f;   // ~51° / ~34°
        float pitchClamp = entity.isFlying() ? 1.0f : 0.5f; // ~57° / ~29°

        deltaY = Mth.clamp(deltaY, -yawClamp, yawClamp);
        deltaX = Mth.clamp(deltaX, -pitchClamp, pitchClamp);

        // Set directly from snapshot (no lerp to avoid cross-entity sync)
        head.setRotY(snap.getRotY() + deltaY);
        head.setRotX(snap.getRotX() + deltaX);
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
        applyNeckBoneFollow("neck1LookControl", headDeltaX, headDeltaY, 0.25f);  // Base
        applyNeckBoneFollow("neck2LookControl", headDeltaX, headDeltaY, 0.35f);  // Lower-mid
        applyNeckBoneFollow("neck3LookControl", headDeltaX, headDeltaY, 0.45f);  // Upper-mid
        applyNeckBoneFollow("neck4LookControl", headDeltaX, headDeltaY, 0.50f);  // Tip
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
}
