package com.leon.saintsdragons.client.model;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.component.DragonBreathPose;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Optional;

public final class DragonModelPoseHelper {
    private DragonModelPoseHelper() {
    }

    public record WeightedBoneChain(String[] boneNames, float[] weights) {
        public WeightedBoneChain {
            if (boneNames.length != weights.length) {
                throw new IllegalArgumentException("Bone chain names and weights must have the same length");
            }
        }

        public static WeightedBoneChain of(String[] boneNames, float... weights) {
            return new WeightedBoneChain(boneNames, weights);
        }
    }

    public static Optional<GeoBone> bone(GeoModel<?> model, String boneName) {
        return model.getBone(boneName);
    }

    public static DragonBreathPose.Editor poseEditor(GeoModel<?> model) {
        return new DragonBreathPose.Editor() {
            @Override
            public String parent(String name) {
                return model.getBone(name).map(GeoBone::getParent).map(GeoBone::getName).orElse(null);
            }

            @Override
            public float rotation(String name, int axis) {
                return model.getBone(name).map(bone -> axis == 0 ? bone.getRotX()
                        : axis == 1 ? bone.getRotY() : bone.getRotZ()).orElse(0.0F);
            }

            @Override
            public void rotate(String name, int axis, float rotation, boolean fromInitial) {
                model.getBone(name).ifPresent(bone -> {
                    var initial = bone.getInitialSnapshot();
                    if (axis == 0) bone.setRotX((fromInitial ? initial.getRotX() : bone.getRotX()) + rotation);
                    else if (axis == 1) bone.setRotY((fromInitial ? initial.getRotY() : bone.getRotY()) + rotation);
                    else bone.setRotZ((fromInitial ? initial.getRotZ() : bone.getRotZ()) + rotation);
                });
            }
        };
    }

    public static void addRotationX(GeoModel<?> model, String boneName, float rotation) {
        bone(model, boneName).ifPresent(bone -> bone.setRotX(bone.getRotX() + rotation));
    }

    public static void addRotationY(GeoModel<?> model, String boneName, float rotation) {
        bone(model, boneName).ifPresent(bone -> bone.setRotY(bone.getRotY() + rotation));
    }

    public static void addRotationZ(GeoModel<?> model, String boneName, float rotation) {
        bone(model, boneName).ifPresent(bone -> bone.setRotZ(bone.getRotZ() + rotation));
    }

    public static void applyWeightedRotationY(GeoModel<?> model, WeightedBoneChain chain, float baseRotation) {
        for (int i = 0; i < chain.boneNames().length; i++) {
            addRotationY(model, chain.boneNames()[i], baseRotation * chain.weights()[i]);
        }
    }

    public static void applyWeightedNeckFollow(GeoModel<?> model, DragonEntity entity, WeightedBoneChain chain,
                                               float pitchRad, float yawRad) {
        if (entity.isStayOrSitMuted()) {
            return;
        }

        for (int i = 0; i < chain.boneNames().length; i++) {
            String boneName = chain.boneNames()[i];
            float weight = chain.weights()[i];
            addRotationX(model, boneName, pitchRad * weight);
            addRotationY(model, boneName, yawRad * weight);
        }
    }

    public static float lookYawWithBodyDeviation(DragonEntity entity, EntityModelData modelData,
                                                 float partialTick, double bodyDeviationScale) {
        double bodyDeviation = entity.getBodyRotDeviation().get(partialTick);
        float lookYawRad = modelData.netHeadYaw() * Mth.DEG_TO_RAD;
        float structuralYawRad = (float) (bodyDeviation * bodyDeviationScale * Mth.DEG_TO_RAD);
        return lookYawRad + structuralYawRad;
    }

    public static void applyGroundNeckTurn(GeoModel<?> model, DragonEntity entity, float partialTick,
                                           WeightedBoneChain chain, double clampDegrees) {
        applyGroundNeckTurn(model, entity, partialTick, chain, clampDegrees, 1.0F);
    }

    public static void applyGroundNeckTurn(GeoModel<?> model, DragonEntity entity, float partialTick,
                                           WeightedBoneChain chain, double clampDegrees, float weight) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -clampDegrees, clampDegrees);
        float turnRad = (float) (-velocity * Mth.DEG_TO_RAD);
        applyWeightedRotationY(model, chain, turnRad * weight);
    }

    public static void applyTailDrag(GeoModel<?> model, DragonEntity entity, float partialTick,
                                     WeightedBoneChain chain, double clampDegrees) {
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -clampDegrees, clampDegrees);
        float smoothedVelocity = entity.smoothTailDragVelocity((float) velocity);
        float velocityRad = smoothedVelocity * Mth.DEG_TO_RAD;
        applyWeightedRotationY(model, chain, velocityRad);
    }

    public static void applyBodyYawDeviation(GeoModel<?> model, DragonEntity entity, String boneName,
                                             float partialTick, float multiplier, boolean fromInitialSnapshot) {
        bone(model, boneName).ifPresent(bone -> {
            float deviationRad = (float) (entity.getBodyRotDeviation().get(partialTick) * Mth.DEG_TO_RAD * multiplier);
            float baseRotY = fromInitialSnapshot ? bone.getInitialSnapshot().getRotY() : bone.getRotY();
            bone.setRotY(baseRotY + deviationRad);
        });
    }
}
