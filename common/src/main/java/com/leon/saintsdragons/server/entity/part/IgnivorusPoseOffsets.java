package com.leon.saintsdragons.server.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;

public final class IgnivorusPoseOffsets {
    @FunctionalInterface
    public interface Editor { void rotate(String bone, int axis, float radians, boolean fromInitial); }
    private static final String[] NECK = {"neck1Controller", "neck2Controller", "neck3Controller", "neck4Controller", "headController"};
    private static final String[] TAIL = {"tail1", "tail2", "tail3", "tail4"};
    private static final float[] TAIL_WEIGHTS = {0.5F, 0.75F, 1.0F, 1.25F};
    private static final String[] WING = {"wing", "wingjoint", "innerphalanges", "phalanges", "middlephalanges", "outerphalanges"};
    private static final float[][] DIVE = {{0,-30,8.5F}, {0,77.5F,-7.5F}, {-1.833F,-60.015F,-2.5F}, {0,0,0}, {0,-12.5F,0}, {2.5F,-12.5F,0}};

    private IgnivorusPoseOffsets() {}

    public static void apply(Ignivorus dragon, float partialTick, float headPitch, float netHeadYaw, Editor editor) {
        if (!dragon.isAlive() || dragon.isDeadOrDying() || dragon.isScentAssessing()
                || dragon.getSkyfallElapsedTicks(partialTick) >= 0.0F) return;
        float deviation = (float) dragon.getBodyRotDeviation().get(partialTick);
        if (!dragon.isVehicle() && !dragon.isInWaterOrBubble() && !dragon.isStayOrSitMuted()) {
            float pitch = headPitch * Mth.DEG_TO_RAD * (dragon.isFlying() ? 0.5F : 1);
            float yaw = (Mth.wrapDegrees(netHeadYaw) + deviation * 2) * Mth.DEG_TO_RAD;
            for (int i = 0; i < NECK.length; i++) {
                editor.rotate(NECK[i], 0, pitch * (0.30F + i * 0.01F), false);
                editor.rotate(NECK[i], 1, yaw * (0.30F + i * 0.01F), false);
            }
        }
        editor.rotate("root", 1, -deviation * Mth.DEG_TO_RAD, true);
        float bank = dragon.getBankAngleDegrees(partialTick);
        editor.rotate("body", 2, Mth.clamp(-bank * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI)
                + dragon.getSmoothedRoll(partialTick), true);
        editor.rotate("root", 0, Mth.clamp(dragon.getFlightPitchRadians(partialTick), -Mth.HALF_PI, Mth.HALF_PI), true);
        if (!dragon.isInWaterOrBubble()) {
            float dive = Mth.clamp(dragon.getDivePose(partialTick), 0, 1);
            if (dive > 0.001F) for (int side = 0; side < 2; side++) {
                for (int i = 0; i < WING.length; i++) for (int axis = 0; axis < 3; axis++) {
                    float sign = axis < 2 ? -1 : 1;
                    if (side == 1 && axis != 0) sign *= -1;
                    editor.rotate((side == 0 ? "left" : "right") + WING[i], axis, DIVE[i][axis] * Mth.DEG_TO_RAD * dive * sign, false);
                }
            }
        }
        float lean = dragon.isVehicle() && dragon.isFlying() ? -(bank / 45.0F) * 32.0F * Mth.DEG_TO_RAD : 0;
        float velocity = (float) dragon.getYawVelocity().get(partialTick);
        float turn = !dragon.isFlying() ? -Mth.clamp(velocity, -25, 25) * Mth.DEG_TO_RAD : 0;
        for (int i = 0; i < NECK.length; i++) editor.rotate(NECK[i], 1, (lean + turn) * (0.40F + i * 0.01F), false);
        float tail = dragon.getTickedTailDragVelocity(partialTick) * Mth.DEG_TO_RAD;
        for (int i = 0; i < TAIL.length; i++) editor.rotate(TAIL[i], 1, tail * TAIL_WEIGHTS[i], false);
    }
}
