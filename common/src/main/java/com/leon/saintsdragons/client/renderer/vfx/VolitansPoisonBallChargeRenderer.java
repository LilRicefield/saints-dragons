package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class VolitansPoisonBallChargeRenderer {
    private static final ResourceLocation[] CHARGE = frames("volitans/poison/poison_ball_charge/poison_ball_charge", 8);
    private static final ResourceLocation[] SHOT = frames("volitans/poison/second_poison_explosion/second_poison_explosion", 12);
    private static final float CHARGE_FRAME_TICKS = 0.75F;
    private static final float SHOT_FRAME_TICKS = 0.75F;
    private static final double FORWARD_OFFSET = 1.2D;
    private static final AttachedPlaneFlipbookRenderer.Style SHOT_STYLE =
            new AttachedPlaneFlipbookRenderer.Style(4.0F, 1.0F, SHOT_FRAME_TICKS, 1.0F, 0.0F);

    private VolitansPoisonBallChargeRenderer() {}

    public static void render(Volitans dragon, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (!dragon.isAlive()) return;
        float chargeAge = dragon.getPoisonBallChargeAge(partialTick);
        float fireAge = dragon.getPoisonBallFireAge(partialTick);
        boolean firing = fireAge >= 0 && fireAge < SHOT.length * SHOT_FRAME_TICKS;
        if (chargeAge < 0 && !firing) return;
        Vec3 mouth = dragon.getClientLocatorPosition("breathBoneOrigin");
        if (mouth == null) return;

        var rider = dragon.getControllingPassenger();
        Vec3 forward = rider != null ? rider.getViewVector(partialTick) : dragon.getViewVector(partialTick);
        var target = dragon.getTarget();
        if (rider == null && target != null && target.isAlive()) {
            Vec3 aim = DragonAimHelper.directionTo(mouth,
                    target.getEyePosition(partialTick).add(target.getDeltaMovement().scale(0.6D)));
            if (aim != null) forward = aim;
        }
        forward = forward.normalize();
        Vec3 renderOrigin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        Vec3 anchor = mouth.add(forward.scale(FORWARD_OFFSET)).subtract(renderOrigin);
        if (chargeAge >= 0) {
            int frame = Mth.floor(chargeAge / CHARGE_FRAME_TICKS) % CHARGE.length;
            float halfSize = Mth.lerp(Mth.clamp(chargeAge / 13.0F, 0, 1), 1.5F, 3.0F);
            BillboardFlashRenderer.renderQuad(poses, buffers, CHARGE[frame],
                    (float) anchor.x, (float) anchor.y, (float) anchor.z,
                    halfSize, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (firing) {
            poses.pushPose();
            try {
                poses.translate(anchor.x, anchor.y, anchor.z);
                float pitch = (float) Math.acos(Mth.clamp(forward.y, -1.0D, 1.0D));
                float yaw = (float) Math.atan2(forward.z, forward.x);
                poses.mulPose(Axis.YP.rotation(Mth.PI / 2.0F - yaw));
                poses.mulPose(Axis.XP.rotation(-Mth.PI / 2.0F + pitch));
                AttachedPlaneFlipbookRenderer.renderOnce(poses, buffers, SHOT, SHOT_STYLE,
                        fireAge, 0.0F, 1.0F, 1.0F, 1.0F);
            } finally {
                poses.popPose();
            }
        }
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return textures;
    }
}
