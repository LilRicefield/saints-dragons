package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Nulljaw model using GeckoLib's built-in head tracking system.
 * The "head" bone parents all neck bones, so we distribute rotation across the neck chain.
 */
public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {

    public NulljawModel() {
        // Defaulted paths under entity/ and built-in head rotation for "head" bone
        super(SaintsDragons.rl("nulljaw"), "head");
    }

    @Override
    public void setCustomAnimations(Nulljaw entity, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Distribute head rotation across neck segments for smooth natural movement
        // ONLY when not in phase 2 - phase 2 animation controls the neck curve itself
        if (entity.isAlive() && !entity.isPhaseTwoActive()) {
            applyNeckFollow();
            applyHeadClamp(entity);
        }
    }

    /**
     * Clamps the main "head" bone rotation to prevent extreme angles.
     * This bone is controlled by GeckoLib but we limit how far it can turn.
     */
    private void applyHeadClamp(Nulljaw entity) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();
        var snap = head.getInitialSnapshot();

        float deltaY = head.getRotY() - snap.getRotY();
        float deltaX = head.getRotX() - snap.getRotX();

        // Clamp to reasonable limits
        float yawClamp = 0.8f;   // ~46°
        float pitchClamp = 0.7f; // ~40°

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
     */
    private void applyNeckFollow() {
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
        applyNeckBoneFollow("neck1", headDeltaX, headDeltaY, 0.15f);  // Base
        applyNeckBoneFollow("neck2", headDeltaX, headDeltaY, 0.25f);    // Middle
        applyNeckBoneFollow("neck3", headDeltaX, headDeltaY, 0.35f);    // Tip - most rotation
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
