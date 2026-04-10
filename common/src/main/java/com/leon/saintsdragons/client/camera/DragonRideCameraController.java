package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.Entity;

public final class DragonRideCameraController {
    private static int activeVehicleId = Integer.MIN_VALUE;
    private static CameraState state = null;

    private DragonRideCameraController() {
    }

    public static boolean supports(Entity vehicle) {
        return vehicle instanceof Raevyx
                || vehicle instanceof Cindervane
                || vehicle instanceof Ignivorus
                || vehicle instanceof Varasuchus
                || vehicle instanceof Stegonaut
                || vehicle instanceof Volitans
                || vehicle instanceof Nulljaw;
    }

    public static CameraOutput update(Entity vehicle, float partialTick) {
        DragonRideCameraTuning.CameraProfile profile = DragonRideCameraTuning.getProfile(vehicle);
        if (state == null || activeVehicleId != vehicle.getId()) {
            activeVehicleId = vehicle.getId();
            state = new CameraState(
                    profile.groundedDistance(),
                    0.0,
                    profile.groundedVerticalShift(),
                    profile.groundedPitchOffset()
            );
        }

        boolean airOrWaterMode = DragonRideCameraTuning.isAirOrWaterMode(vehicle);
        float targetZoom = airOrWaterMode ? profile.airOrWaterDistance() : profile.groundedDistance();
        double targetLateralShift = airOrWaterMode ? computeTargetLateralShift(vehicle, partialTick, profile) : 0.0;
        double targetVerticalShift = airOrWaterMode ? profile.airOrWaterVerticalShift() : profile.groundedVerticalShift();
        float targetPitchOffset = airOrWaterMode ? profile.airOrWaterPitchOffset() : profile.groundedPitchOffset();

        state.zoom = approach(state.zoom, targetZoom, profile.zoomSmoothing());
        state.lateralShift = approach(state.lateralShift, targetLateralShift, profile.lateralShiftSmoothing());
        state.verticalShift = approach(state.verticalShift, targetVerticalShift, profile.verticalShiftSmoothing());
        state.pitchOffset = approach(state.pitchOffset, targetPitchOffset, profile.pitchSmoothing());

        return new CameraOutput(state.zoom, state.lateralShift, state.verticalShift, state.pitchOffset);
    }

    public static void reset() {
        activeVehicleId = Integer.MIN_VALUE;
        state = null;
    }

    private static double computeTargetLateralShift(Entity vehicle, float partialTick, DragonRideCameraTuning.CameraProfile profile) {
        if (profile.bankShiftMax() == 0.0f) {
            return 0.0;
        }

        float bankAngle = getBankAngleDegrees(vehicle, partialTick);
        if (Math.abs(bankAngle) <= 0.001f) {
            return 0.0;
        }

        double velocity = vehicle.getDeltaMovement().horizontalDistance();
        double velocityFactor = Math.min(velocity * 2.0, 1.5);
        return -(bankAngle / 45.0) * profile.bankShiftMax() * velocityFactor;
    }

    private static float getBankAngleDegrees(Entity vehicle, float partialTick) {
        if (vehicle instanceof Raevyx raevyx) {
            return raevyx.getBankAngleDegrees(partialTick);
        }
        if (vehicle instanceof Cindervane cindervane) {
            return cindervane.getBankAngleDegrees(partialTick);
        }
        if (vehicle instanceof Ignivorus ignivorus) {
            return ignivorus.getBankAngleDegrees(partialTick);
        }
        if (vehicle instanceof Volitans volitans) {
            return volitans.getBankAngleDegrees(partialTick);
        }
        if (vehicle instanceof Varasuchus varasuchus) {
            return varasuchus.getSwimRollAngleDegrees(partialTick);
        }
        return 0.0f;
    }

    private static float approach(float current, float target, float smoothing) {
        return current + (target - current) * smoothing;
    }

    private static double approach(double current, double target, double smoothing) {
        return current + (target - current) * smoothing;
    }

    private static final class CameraState {
        private float zoom;
        private double lateralShift;
        private double verticalShift;
        private float pitchOffset;

        private CameraState(float zoom, double lateralShift, double verticalShift, float pitchOffset) {
            this.zoom = zoom;
            this.lateralShift = lateralShift;
            this.verticalShift = verticalShift;
            this.pitchOffset = pitchOffset;
        }
    }

    public record CameraOutput(float zoom, double lateralShift, double verticalShift, float pitchOffset) {
    }
}
