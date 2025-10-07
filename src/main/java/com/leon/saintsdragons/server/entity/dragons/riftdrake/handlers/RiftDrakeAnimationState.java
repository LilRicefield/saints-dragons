package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.common.animation.AnimationEasing;
import com.leon.saintsdragons.common.animation.AnimationInterpolation;
import com.leon.saintsdragons.common.animation.SmoothFloat;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Maintains client-friendly smoothed animation parameters for the Rift Drake.
 * The entity updates this each tick and the model samples it during rendering.
 */
public class RiftDrakeAnimationState {

    private final SmoothFloat groundBlend = new SmoothFloat(AnimationInterpolation.CATMULLROM, AnimationEasing.EASE_IN_OUT, 0.0f);
    private final SmoothFloat swimYaw = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f);
    private final SmoothFloat swimPitch = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f);
    private final SmoothFloat headYaw = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f, true);
    private final SmoothFloat headPitch = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f);
    private final SmoothFloat travelSpeed = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f);
    private final SmoothFloat yawRate = new SmoothFloat(AnimationInterpolation.LINEAR, AnimationEasing.EASE_OUT, 0.0f);
    private final SmoothFloat actionBlend = new SmoothFloat(AnimationInterpolation.CATMULLROM, AnimationEasing.EASE_IN_OUT, 0.0f);

    private float prevBodyYaw;
    private String prevActiveAbilityKey;
    private boolean wasPhaseTwo;
    private final SmoothFloat phaseBlend = new SmoothFloat(AnimationInterpolation.CATMULLROM, AnimationEasing.EASE_IN_OUT, 0.0f);

    public void tick(RiftDrakeEntity drake) {
        float targetGround = drake.getEffectiveGroundState();
        if (drake.isSwimming()) {
            // Treat swimming as idle for movement loops; swim controller handles motion.
            targetGround = 0.0f;
        }
        groundBlend.setTo(targetGround);
        groundBlend.update(0.35f);

        float targetSwimYaw = drake.isSwimming() ? drake.getSwimTurnDirection() : 0.0f;
        swimYaw.setTo(targetSwimYaw);
        swimYaw.update(0.4f);

        float targetSwimPitch;
        if (!drake.isSwimming()) {
            targetSwimPitch = 0.0f;
        } else if (drake.isSwimmingUp()) {
            targetSwimPitch = -1.0f;
        } else if (drake.isSwimmingDown()) {
            targetSwimPitch = 1.0f;
        } else {
            targetSwimPitch = 0.0f;
        }
        swimPitch.setTo(targetSwimPitch);
        swimPitch.update(0.4f);

        float yawDelta = Mth.wrapDegrees(drake.getYHeadRot() - drake.yBodyRot);
        headYaw.setTo(yawDelta);
        headYaw.update(0.45f);

        headPitch.setTo(drake.getXRot());
        headPitch.update(0.4f);

        float targetSpeed;
        if (drake.getNavigation().isInProgress()) {
            targetSpeed = 0.4f;
        } else {
            double motionSq = drake.getDeltaMovement().horizontalDistanceSqr();
            targetSpeed = (float) Math.sqrt(motionSq);
        }
        travelSpeed.setTo(targetSpeed);
        travelSpeed.update(0.35f);

        float yawDeltaDeg = Mth.wrapDegrees(drake.yBodyRot - prevBodyYaw);
        prevBodyYaw = drake.yBodyRot;
        yawRate.setTo(yawDeltaDeg * 0.1f);
        yawRate.update(0.4f);

        String activeAbility = getActiveAbilityKey(drake);
        float targetAction = activeAbility != null ? 1.0f : 0.0f;
        actionBlend.setTo(targetAction);
        actionBlend.update(activeAbility != null ? 0.25f : 0.12f);
        prevActiveAbilityKey = activeAbility;

        boolean phaseTwoNow = drake.isPhaseTwoActive();
        if (phaseTwoNow != wasPhaseTwo) {
            phaseBlend.force(phaseTwoNow ? 1.0f : 0.0f);
        }
        phaseBlend.setTo(phaseTwoNow ? 1.0f : 0.0f);
        phaseBlend.update(0.18f);
        wasPhaseTwo = phaseTwoNow;
    }

    public float getGroundBlend(float partialTick) {
        return groundBlend.get(partialTick);
    }

    public float getSwimYaw(float partialTick) {
        return swimYaw.get(partialTick);
    }

    public float getSwimPitch(float partialTick) {
        return swimPitch.get(partialTick);
    }

    public float getHeadYawDegrees(float partialTick) {
        return headYaw.get(partialTick);
    }

    public float getHeadPitchDegrees(float partialTick) {
        return headPitch.get(partialTick);
    }

    public float getHeadYawRadians(float partialTick) {
        return getHeadYawDegrees(partialTick) * Mth.DEG_TO_RAD;
    }

    public float getHeadPitchRadians(float partialTick) {
        return getHeadPitchDegrees(partialTick) * Mth.DEG_TO_RAD;
    }

    public float getTravelSpeed(float partialTick) {
        return travelSpeed.get(partialTick);
    }

    public float getYawRate(float partialTick) {
        return yawRate.get(partialTick);
    }

    public float getActionBlend(float partialTick) {
        return actionBlend.get(partialTick);
    }

    public float getPhaseBlend(float partialTick) {
        return phaseBlend.get(partialTick);
    }

    public void resetImmediate(RiftDrakeEntity drake) {
        float yawDelta = Mth.wrapDegrees(drake.getYHeadRot() - drake.yBodyRot);
        groundBlend.force(drake.getEffectiveGroundState());
        swimYaw.force(drake.isSwimming() ? drake.getSwimTurnDirection() : 0.0f);
        if (!drake.isSwimming()) {
            swimPitch.force(0.0f);
        } else if (drake.isSwimmingUp()) {
            swimPitch.force(-1.0f);
        } else if (drake.isSwimmingDown()) {
            swimPitch.force(1.0f);
        } else {
            swimPitch.force(0.0f);
        }
        headYaw.force(yawDelta);
        headPitch.force(drake.getXRot());
        if (drake.getNavigation().isInProgress()) {
            travelSpeed.force(0.4f);
        } else {
            double motionSq = drake.getDeltaMovement().horizontalDistanceSqr();
            travelSpeed.force((float) Math.sqrt(motionSq));
        }
        prevBodyYaw = drake.yBodyRot;
        yawRate.force(0.0f);
        actionBlend.force(0.0f);
        prevActiveAbilityKey = getActiveAbilityKey(drake);
        wasPhaseTwo = drake.isPhaseTwoActive();
        phaseBlend.force(wasPhaseTwo ? 1.0f : 0.0f);
    }

    private static String getActiveAbilityKey(RiftDrakeEntity drake) {
        var ability = drake.getActiveAbility();
        if (ability == null) return null;
        return com.leon.saintsdragons.common.registry.AbilityRegistry.getName(ability.getAbilityType());
    }
}


