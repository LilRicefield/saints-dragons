package com.leon.saintsdragons.client.model.riftdrake;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeAnimationState;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class RiftDrakeModel extends DefaultedEntityGeoModel<RiftDrakeEntity> {

    public RiftDrakeModel() {
        super(SaintsDragons.rl("rift_drake"), "head");
    }

    @Override
    public void setCustomAnimations(RiftDrakeEntity animatable, long instanceId, AnimationState<RiftDrakeEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        RiftDrakeAnimationState smooth = animatable.getAnimationState();
        float partialTick = animationState.getPartialTick();

        applyHeadSmoothing(smooth, partialTick);
        applyNeckFollow(smooth, partialTick);
        applyTailInertia(smooth, partialTick);
    }

    private void applyHeadSmoothing(RiftDrakeAnimationState smooth, float partialTick) {
        getBone("head").ifPresent(head -> {
            var snapshot = head.getInitialSnapshot();
            float yaw = Mth.clamp(smooth.getHeadYawRadians(partialTick), -0.65f, 0.65f);
            float pitch = Mth.clamp(smooth.getHeadPitchRadians(partialTick), -0.5f, 0.4f);
            float targetYawRad = snapshot.getRotY() - yaw;
            float targetPitchRad = snapshot.getRotX() + pitch;
            head.setRotY(targetYawRad);
            head.setRotX(targetPitchRad);
        });
    }

    private void applyNeckFollow(RiftDrakeAnimationState smooth, float partialTick) {
        float headYaw = Mth.clamp(smooth.getHeadYawRadians(partialTick), -0.65f, 0.65f);
        float headPitch = Mth.clamp(smooth.getHeadPitchRadians(partialTick), -0.5f, 0.4f);

        applyNeckBone("neck1Controller", -headYaw, headPitch, 0.15f);
        applyNeckBone("neck2Controller", -headYaw, headPitch, 0.28f);
        applyNeckBone("neck3Controller", -headYaw, headPitch, 0.38f);
        applyNeckBone("neck4", -headYaw, headPitch, 0.48f);
    }

    private void applyNeckBone(String name, float headYaw, float headPitch, float weight) {
        getBone(name).ifPresent(bone -> {
            var snapshot = bone.getInitialSnapshot();
            float targetYaw = snapshot.getRotY() + headYaw * weight;
            float targetPitch = snapshot.getRotX() + headPitch * weight * 0.65f;
            bone.setRotY(lerp(bone.getRotY(), targetYaw, 0.28f));
            bone.setRotX(lerp(bone.getRotX(), targetPitch, 0.28f));
        });
    }

    private void applyTailInertia(RiftDrakeAnimationState smooth, float partialTick) {
        float speed = smooth.getTravelSpeed(partialTick);
        float yawRate = Mth.clamp(smooth.getYawRate(partialTick), -1.0f, 1.0f);
        float phaseBlend = smooth.getPhaseBlend(partialTick);
        float actionBlend = smooth.getActionBlend(partialTick);
        float swayYaw = speed * (0.25f + 0.15f * (1.0f - phaseBlend)) - yawRate * 0.3f;
        swayYaw *= (1.0f - actionBlend * 0.6f);

        applyTailBone("tail1", swayYaw * 0.4f);
        applyTailBone("tail2", swayYaw * 0.6f);
        applyTailBone("tail3", swayYaw * 0.8f);
        applyTailBone("tail4", swayYaw);
    }

    private void applyTailBone(String name, float amount) {
        getBone(name).ifPresent(bone -> {
            var snapshot = bone.getInitialSnapshot();
            float targetYaw = snapshot.getRotY() + amount;
            bone.setRotY(lerp(bone.getRotY(), targetYaw, 0.2f));
        });
    }

    private static float lerp(float current, float target, float alpha) {
        return current + (target - current) * alpha;
    }
}
