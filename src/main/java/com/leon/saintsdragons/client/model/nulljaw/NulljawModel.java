package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Nulljaw model using GeckoLib's built-in head tracking system.
 * The "head" bone parents all neck bones, so we distribute rotation across the neck chain.
 */
public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {
    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/nulljaw/nulljaw.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/nulljaw/nulljaw_female.png");

    public NulljawModel() {
        // Defaulted paths under entity/ and built-in head rotation for "head" bone
        super(SaintsDragons.rl("nulljaw"), "head");
    }

    @Override
    public ResourceLocation getTextureResource(Nulljaw entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public void setCustomAnimations(Nulljaw entity, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        // Apply body rotation deviation for smooth "body follows head" behavior
        applyBodyRotationDeviation(entity, partialTick);

        // Apply swim roll for dynamic underwater banking (like Raevyx's flight banking)
        if (entity.isAlive() && entity.isSwimming()) {
            applySwimRoll(entity, animationState);
        }

        // Distribute head rotation across neck segments
        applyNeckFollow(entity, animationState);
    }

    /**
     * Applies smooth body rotation using AstemirLib's deviation approach.
     * bodyRotDeviation tracks the difference between head and body rotation.
     * This creates the natural "head leads, body follows" behavior.
     */
    private void applyBodyRotationDeviation(Nulljaw entity, float partialTick) {
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
        float deviationRad = (float)(deviation * net.minecraft.util.Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    /**
     * Applies dynamic swim roll to the body bone for smooth underwater banking.
     * Similar to Raevyx's flight banking but adapted for swimming mechanics.
     * Adds to whatever the animation (swimming_left/swimming_right) already set.
     */
    private void applySwimRoll(Nulljaw entity, AnimationState<Nulljaw> state) {
        var bodyOpt = getBone("root");
        if (bodyOpt.isEmpty()) return;

        GeoBone body = bodyOpt.get();

        float partialTick = state.getPartialTick();
        float swimRollDeg = entity.getSwimRollAngleDegrees(partialTick);
        float swimRollRad = Mth.clamp(-swimRollDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        // Add to whatever the animation already set (don't use snapshot - we want to layer on top)
        body.setRotZ(body.getRotZ() + swimRollRad);
    }

    /**
     * Distributes the parent "head" bone's rotation across neck segments like a giraffe.
     * GeckoLib rotates the main "head" bone, and we distribute that rotation so each
     * neck segment contributes a portion, creating smooth natural movement.
     */
    private void applyNeckFollow(Nulljaw entity, AnimationState<Nulljaw> state) {
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

        // Now distribute the rotation across neck segments (adjust bone names and weights as needed)
        applyNeckBoneFollow("neck1LookControl", headDeltaX, headDeltaY, 0.15f);  // Base
        applyNeckBoneFollow("neck2LookControl", headDeltaX, headDeltaY, 0.25f);    // Middle
        applyNeckBoneFollow("neck3LookControl", headDeltaX, headDeltaY, 0.35f);    // Tip - most rotation
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
