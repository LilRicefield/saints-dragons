package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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
    public RaevyxModel() {
        // Defaulted paths under entity/ and built-in head rotation for "head" bone
        super(SaintsDragons.rl("raevyx"), "head");
    }


    @Override
    public void setCustomAnimations(Raevyx entity, long instanceId, AnimationState<Raevyx> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Banking is the only procedural animation - GeckoLib's built-in head tracking handles everything else!
        // The "head" bone now parents all neck bones, so GeckoLib rotates the entire chain automatically
        if (entity.isAlive()) {
            applyBankingRoll(entity, animationState);
            applyNeckFollow();
            applyHeadClamp(entity);
        }
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

        // Now distribute the rotation across neck segments (more at tip, less at base)
        // This creates a smooth curve like a giraffe neck
        applyNeckBoneFollow("neck1Controller", headDeltaX, headDeltaY, 0.10f);  // Base - least rotation
        applyNeckBoneFollow("neck2Controller", headDeltaX, headDeltaY, 0.15f);
        applyNeckBoneFollow("neck3Controller", headDeltaX, headDeltaY, 0.20f);
        applyNeckBoneFollow("neck4Controller", headDeltaX, headDeltaY, 0.25f);  // Tip - most rotation
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
