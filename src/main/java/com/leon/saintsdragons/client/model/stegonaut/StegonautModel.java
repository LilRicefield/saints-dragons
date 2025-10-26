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
     * Tail swings OPPOSITE to turn direction, creating natural drag/whip effect.
     * Base segments swing more, tip segments swing less.
     */
    private void applyTailDrag(Stegonaut entity, float partialTick) {
        // Get the body rotation deviation (yaw change delta)
        double deviation = entity.bodyRotDeviation.get(partialTick);
        float deviationRad = (float)(deviation * Mth.DEG_TO_RAD);

        // Apply negative rotation (opposite to turn) with decreasing intensity
        // Base tail segment swings most (2x), tip swings least (1x)
        applyTailBoneRotation("tail1", deviationRad * 1.0f);
        applyTailBoneRotation("tail2", deviationRad * 2.5f);
        applyTailBoneRotation("tail3", deviationRad * 3.0f);
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
