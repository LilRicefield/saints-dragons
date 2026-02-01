package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.util.math.SmoothValue;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;

public final class DragonAnimationSyncComponent {
    private final DragonEntity dragon;
    private final EntityDataAccessor<Float> bodyDeviationData;
    private final EntityDataAccessor<Float> pitchDeviationData;
    private final EntityDataAccessor<Float> yawVelocityData;

    private final SmoothValue bodyRotDeviation = SmoothValue.rotation(0.0);
    private final SmoothValue xRotDeviation = SmoothValue.rotation(0.0);
    private final SmoothValue yawVelocity = SmoothValue.rotation(0.0);

    private float clientTailDragVelocity = 0f;

    public DragonAnimationSyncComponent(DragonEntity dragon,
                                        EntityDataAccessor<Float> bodyDeviationData,
                                        EntityDataAccessor<Float> pitchDeviationData,
                                        EntityDataAccessor<Float> yawVelocityData) {
        this.dragon = dragon;
        this.bodyDeviationData = bodyDeviationData;
        this.pitchDeviationData = pitchDeviationData;
        this.yawVelocityData = yawVelocityData;
    }

    public SmoothValue getBodyRotDeviation() {
        return bodyRotDeviation;
    }

    public SmoothValue getPitchDeviation() {
        return xRotDeviation;
    }

    public SmoothValue getYawVelocity() {
        return yawVelocity;
    }

    public float smoothTailDragVelocity(float targetDegrees) {
        clientTailDragVelocity = Mth.lerp(0.15f, clientTailDragVelocity, targetDegrees);
        return clientTailDragVelocity;
    }

    public void resetTailDragVelocity() {
        clientTailDragVelocity = 0f;
    }

    public void tickClientRotationDeviations() {
        double headToBody = dragon.getEntityData().get(bodyDeviationData);
        double pitchDelta = dragon.getEntityData().get(pitchDeviationData);
        double bodyYawDelta = dragon.getEntityData().get(yawVelocityData);

        bodyRotDeviation.setTo(headToBody);
        bodyRotDeviation.update(0.25f);

        xRotDeviation.setTo(pitchDelta);
        xRotDeviation.update(0.25f);

        yawVelocity.setTo(bodyYawDelta);
        yawVelocity.update(0.25f);
    }

    public void tickServerRotationTargets() {
        float bodyYawDelta = (float) (Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO) * 2.0);
        dragon.getEntityData().set(yawVelocityData, bodyYawDelta);

        if (dragon.isVehicle()) {
            dragon.getEntityData().set(bodyDeviationData, 0.0f);
            dragon.getEntityData().set(pitchDeviationData, 0.0f);
            return;
        }

        float headToBody = (float) (Mth.wrapDegrees(dragon.yHeadRot - dragon.yBodyRot) * 0.25);
        float pitchDelta = (dragon.getXRot() - dragon.xRotO) * 0.5f;

        dragon.getEntityData().set(bodyDeviationData, headToBody);
        dragon.getEntityData().set(pitchDeviationData, pitchDelta);
    }
}
