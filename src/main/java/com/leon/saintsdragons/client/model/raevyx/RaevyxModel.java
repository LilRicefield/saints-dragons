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
        super(SaintsDragons.rl("raevyx"), "head");
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
        super.setCustomAnimations(entity, instanceId, animationState);

        // Banking is the only procedural animation - GeckoLib's built-in head tracking handles everything else!
        // The "head" bone now parents all neck bones, so GeckoLib rotates the entire chain automatically
        if (entity.isAlive()) {
            applyBankingRoll(entity, animationState);
            applyNeckFollow(entity, animationState);
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
    private void applyNeckFollow(Raevyx entity, AnimationState<Raevyx> state) {
        var headOpt = getBone("head");
        if (headOpt.isEmpty()) return;

        GeoBone head = headOpt.get();

        var headSnap = head.getInitialSnapshot();
        float basePitch = head.getRotX() - headSnap.getRotX();
        float baseYaw = head.getRotY() - headSnap.getRotY();

        float targetPitch = basePitch;
        float targetYaw = baseYaw;

        if (entity.isBeaming()) {
            targetPitch -= entity.getBeamPitchOffsetRad();
            targetYaw += entity.getBeamYawOffsetRad();

            float maxPitch = Raevyx.MAX_BEAM_PITCH_DEG * net.minecraft.util.Mth.DEG_TO_RAD;
            float maxYaw = Raevyx.MAX_BEAM_YAW_DEG * net.minecraft.util.Mth.DEG_TO_RAD;
            targetPitch = net.minecraft.util.Mth.clamp(targetPitch, -maxPitch, maxPitch);
            targetYaw = net.minecraft.util.Mth.clamp(targetYaw, -maxYaw, maxYaw);
        }

        head.setRotX(headSnap.getRotX());
        head.setRotY(headSnap.getRotY());

        float[] weights = entity.isBeaming()
                ? com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx.BEAM_NECK_WEIGHTS
                : com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx.IDLE_NECK_WEIGHTS;

        applyNeckBoneFollow("neck1LookControl", targetPitch, targetYaw, weights[0]);
        applyNeckBoneFollow("neck2LookControl", targetPitch, targetYaw, weights[1]);
        applyNeckBoneFollow("neck3LookControl", targetPitch, targetYaw, weights[2]);
        applyNeckBoneFollow("neck4LookControl", targetPitch, targetYaw, weights[3]);

        head.setRotX(headSnap.getRotX() + targetPitch);
        head.setRotY(headSnap.getRotY() + targetYaw);
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
