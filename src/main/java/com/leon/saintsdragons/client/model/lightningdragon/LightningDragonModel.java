package com.leon.saintsdragons.client.model.lightningdragon;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
/**
 * Lightning Dragon model with enhanced bone system and procedural animations
 * Now uses DefaultedEntityGeoModel for entity-focused conveniences (default asset paths, hooks)
 */
public class LightningDragonModel extends DefaultedEntityGeoModel<LightningDragonEntity> {
    public LightningDragonModel() {
        // Defaulted paths under entity/ and built-in head rotation for "head" bone
        super(SaintsDragons.rl("lightning_dragon"), "head");
    }

    /**
     * This is where head tracking happen
     */
    @Override
    public void applyMolangQueries(LightningDragonEntity animatable, double animTime) {
        // Do not hard-reset head/neck each frame; smoothing handles stability
        super.applyMolangQueries(animatable, animTime);
    }

    @Override
    public void setCustomAnimations(LightningDragonEntity entity, long instanceId, AnimationState<LightningDragonEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        // Apply procedural animations when alive
        if (entity.isAlive()) {
            // Clamp built-in head rotation to sane limits first
            applyHeadClamp(entity);

            // Light neck follow based on clamped head rotation
            applyNeckFollow();

            // When beaming, bias the neck chain to aim along the beam direction
            applyNeckAimAlongBeam(entity, animationState);
        }
    }

    /**
     * Clamp head yaw/pitch deltas so the dragon cannot look fully backwards.
     * Uses deltas relative to the initial snapshot to avoid drift.
     */
    private void applyHeadClamp(LightningDragonEntity entity) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();
        var snap = head.getInitialSnapshot();

        float deltaY = head.getRotY() - snap.getRotY();
        float deltaX = head.getRotX() - snap.getRotX();

        float yawClamp = entity.isFlying() ? 0.9f : 0.6f;   // ~51° / ~34°
        float pitchClamp = entity.isFlying() ? 1.0f : 0.5f; // ~57° / ~29°

        deltaY = Mth.clamp(deltaY, -yawClamp, yawClamp);
        deltaX = Mth.clamp(deltaX, -pitchClamp, pitchClamp);

        head.setRotY(snap.getRotY() + deltaY);
        head.setRotX(snap.getRotX() + deltaX);
    }

    /**
     * Light neck follow: gently propagates head rotation down neck1 to neck4.
     * Uses small weights and smoothing to avoid over-rotation.
     */
    private void applyNeckFollow() {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();

        float headDeltaX = head.getRotX() - head.getInitialSnapshot().getRotX();
        float headDeltaY = head.getRotY() - head.getInitialSnapshot().getRotY();

        // Gentle distribution from head down the neck (tip follows more than base)
        applyNeckBoneFollow("neck4Controller", headDeltaX, headDeltaY, 0.16f);
        applyNeckBoneFollow("neck3Controller", headDeltaX, headDeltaY, 0.12f);
        applyNeckBoneFollow("neck2Controller", headDeltaX, headDeltaY, 0.08f);
        applyNeckBoneFollow("neck1Controller", headDeltaX, headDeltaY, 0.05f);
        applyNeckBoneFollow("neckstartController", headDeltaX, headDeltaY, 0.02f);
    }

    private void applyNeckBoneFollow(String boneName, float headDeltaX, float headDeltaY, float weight) {
        var boneOpt = getBone(boneName);
        if (boneOpt.isEmpty()) return;

        GeoBone bone = boneOpt.get();
        var snap = bone.getInitialSnapshot();

        // Cap how much any single neck bone can add from the head
        float maxNeckPitch = 0.35f; // ~20°
        float maxNeckYaw   = 0.35f; // ~20°
        float addX = Mth.clamp(headDeltaX * weight, -maxNeckPitch, maxNeckPitch);
        float addY = Mth.clamp(headDeltaY * weight, -maxNeckYaw,   maxNeckYaw);

        float targetX = snap.getRotX() + addX;
        float targetY = snap.getRotY() + addY;

        // Smooth towards target to keep motion stable
        float lerpA = 0.18f;
        bone.setRotX(Mth.lerp(lerpA, bone.getRotX(), targetX));
        bone.setRotY(Mth.lerp(lerpA, bone.getRotY(), targetY));
    }

    /**
     * While the dragon is beaming, gently steer the neck chain toward the beam direction
     * so the segments follow the aim instead of staying fixed.
     */
    private void applyNeckAimAlongBeam(LightningDragonEntity entity, AnimationState<LightningDragonEntity> state) {
        if (!entity.isBeaming()) return;

        float pt = state.getPartialTick();
        net.minecraft.world.phys.Vec3 start = entity.computeHeadMouthOrigin(pt);
        net.minecraft.world.phys.Vec3 end = entity.getClientBeamEndPosition(pt);
        if (end == null) return;
        net.minecraft.world.phys.Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0e-6) return;
        dir = dir.normalize();

        // Desired yaw/pitch in Minecraft's convention
        // Yaw: 0 = +Z (south), +90 = -X (west), uses atan2(-x, z)
        float desiredYawDeg = (float)(Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        // Pitch: MC positive looks down; GeoBone X often reads opposite of world pitch.
        // Use non-inverted pitch here; we will apply a negative sign when adding to bones if needed.
        float desiredPitchDeg = (float)(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI));

        // Compute deltas from body orientation so the base of the neck can participate
        // Anchor against head yaw for tighter beam alignment when the player looks around
        float bodyYaw = entity.yHeadRot;
        float yawErrDeg = Mth.degreesDifference(bodyYaw, desiredYawDeg);
        float yawErr = yawErrDeg * Mth.DEG_TO_RAD;
        float pitchErr = desiredPitchDeg * Mth.DEG_TO_RAD;

        // Distribute along neck segments with increasing weight toward the head
        String[] bones = {"neck1Controller", "neck2Controller", "neck3Controller", "neck4Controller"};
        float[] weights = {0.18f, 0.22f, 0.26f, 0.30f};
        float maxYaw = 0.70f;   // ~40° per bone
        float maxPitch = 0.90f; // ~52° per bone (allows near-90° combined)
        float lerp = 0.25f;     // more responsive while beaming

        for (int i = 0; i < bones.length; i++) {
            var boneOpt = getBone(bones[i]);
            if (boneOpt.isEmpty()) continue;
            GeoBone bone = boneOpt.get();

            // Invert yaw contribution to match GeoBone Y rotation orientation
            float addY = Mth.clamp(-yawErr * weights[i], -maxYaw, maxYaw);
            // Use positive pitch contribution so looking up bends neck upward
            float addX = Mth.clamp(pitchErr * weights[i], -maxPitch, maxPitch);

            // Bias around the initial snapshot to avoid cumulative drift and sign inconsistencies
            var snap = bone.getInitialSnapshot();
            float targetY = snap.getRotY() + addY;
            float targetX = snap.getRotX() + addX;

            bone.setRotY(Mth.lerp(lerp, bone.getRotY(), targetY));
            bone.setRotX(Mth.lerp(lerp, bone.getRotX(), targetX));
        }
    }

}
