package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class StegonautModel extends DefaultedEntityGeoModel<Stegonaut> {
    public StegonautModel() {
        super(SaintsDragons.rl( "stegonaut"), "head");
    }
    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/stegonaut/stegonaut.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/stegonaut/stegonaut_female.png");

    @Override
    public ResourceLocation getTextureResource(Stegonaut entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    @Override
    public void setCustomAnimations(Stegonaut entity, long instanceId, AnimationState<Stegonaut> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        float partialTick = animationState.getPartialTick();

        // Apply AstemirLib-style body rotation for smooth "body follows head" behavior
        applyBodyRotationDeviation(entity, partialTick);
        applyTailDrag(entity, partialTick);
        // Note: Head rotation is handled by GeckoLib's built-in head tracking
    }

    /**
     * Applies smooth body rotation using AstemirLib's deviation approach.
     * bodyRotDeviation tracks the difference between head and body rotation.
     * This creates the natural "head leads, body follows" behavior.
     */
    private void applyBodyRotationDeviation(Stegonaut entity, float partialTick) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        // Get the smoothed head-body difference
        double deviation = entity.bodyRotDeviation.get(partialTick);

        // Convert to radians
        // GeckoLib bones rotate left when positive, Minecraft rotates right when positive
        // Subtract to flip the coordinate system
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        root.setRotY(snap.getRotY() - deviationRad);
    }

    /**
     * Applies tail drag effect when turning (The Dawn Era approach).
     * Uses yawVelocity (body turning speed) NOT bodyRotDeviation (head-body difference).
     * This way tail only swings when BODY turns, not when head looks around.
     */
    private void applyTailDrag(Stegonaut entity, float partialTick) {
        // Use yawVelocity (body turn rate) instead of bodyRotDeviation (head-body difference)
        // This prevents tail from acting like a second neck when dragon looks around while walking
        double velocity = entity.yawVelocity.get(partialTick);

        // Clamp velocity to prevent tail from going crazy during rapid movements
        velocity = Mth.clamp(velocity, -30.0, 30.0); // Max ~30 degrees of tail swing

        float velocityRad = (float)(velocity * Mth.DEG_TO_RAD);

        // Apply rotation with increasing intensity toward tip
        applyTailBoneRotation("tail1", velocityRad * 1.0f);
        applyTailBoneRotation("tail2", velocityRad * 2.5f);
        applyTailBoneRotation("tail3", velocityRad * 3.0f);
    }

    /**
     * Helper to apply Y-rotation to a tail bone.
     * ADDS to current rotation (preserves animation) instead of replacing it.
     */
    private void applyTailBoneRotation(String boneName, float rotationY) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) {
            return;
        }

        GeoBone bone = boneOpt.get();
        // Add to current rotation (which includes animation) instead of setting from snapshot
        bone.setRotY(bone.getRotY() + rotationY);
    }
}
