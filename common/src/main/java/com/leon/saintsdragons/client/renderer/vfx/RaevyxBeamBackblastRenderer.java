package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public final class RaevyxBeamBackblastRenderer {
    private static final float ARM_LENGTH = 6.0F;
    private static final float LIGHTNING_WIDTH_SCALE = 0.75F;
    private static final float ARM_ANGLE_DEGREES = 25.0F;
    private static final double SIDE_OFFSET = 0.25D;
    private static final double BACK_OFFSET = 0.35D;
    private static final long LEFT_SEED_SALT = 0xA54FF53A5F1D36F1L;
    private static final long RIGHT_SEED_SALT = 0x510E527FADE682D1L;

    private RaevyxBeamBackblastRenderer() {
    }
    public static void render(PoseStack poses, MultiBufferSource buffers, Vec3 mouth,
                              float bodyYaw, float visibility, float ageInTicks,
                              long seed, boolean gold) {
        if (visibility <= 0.01F) {
            return;
        }

        poses.pushPose();
        poses.translate(mouth.x, mouth.y, mouth.z);
        poses.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poses.translate(0.0D, 0.0D, -BACK_OFFSET);
        for (int side = -1; side <= 1; side += 2) {
            poses.pushPose();
            poses.translate(side * SIDE_OFFSET, 0.0D, 0.0D);
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - side * ARM_ANGLE_DEGREES));
            poses.scale(LIGHTNING_WIDTH_SCALE, LIGHTNING_WIDTH_SCALE, 1.0F);
            ProceduralBeamLightningRenderer.render(poses, buffers,
                    ARM_LENGTH, visibility, ageInTicks,
                    seed ^ (side < 0 ? LEFT_SEED_SALT : RIGHT_SEED_SALT),
                    1.0F, gold ? 0.75F : 0.0F, gold ? 0.15F : 0.0F);
            poses.popPose();
        }
        poses.popPose();
    }
}
