package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.particle.VolitansBreathMotion;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public final class VolitansWaterRingRenderer {
    private static final ResourceLocation[] FRAMES = new ResourceLocation[8];
    private static final float FRAME_TICKS = 1.0F;
    private static final float EMISSION_INTERVAL = 14.0F;
    private static final float LIFETIME = 28.0F;
    private static final double FORWARD_OFFSET = 1.2D;
    private static final double SPEED = (VolitansBreathMotion.RANGE * 0.85D - FORWARD_OFFSET) / LIFETIME;
    private static final float START_HALF_SIZE = 1.2F;
    private static final float END_HALF_SIZE = 4.0F;

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = SaintsDragonsCommon.rl("textures/particle/shared/rings/sword_ring/sword_ring" + i + ".png");
        }
    }

    private VolitansWaterRingRenderer() {}

    public static void render(Volitans dragon, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (!dragon.isAlive() || !dragon.isBreathing() || dragon.isPoisonBreathMode()) return;
        float time = dragon.getBreathFireAge(partialTick);
        Vec3 mouth = dragon.getClientLocatorPosition("breathBoneOrigin");
        if (time < 0 || mouth == null) return;

        var rider = dragon.getControllingPassenger();
        Vec3 forward = rider != null ? rider.getViewVector(partialTick) : dragon.getViewVector(partialTick);
        var target = dragon.getTarget();
        if (rider == null && dragon.isTargetValid(target)) {
            Vec3 aim = DragonAimHelper.directionTo(mouth,
                    target.getEyePosition(partialTick).add(target.getDeltaMovement().scale(0.35D)));
            if (aim != null) forward = aim;
        }
        forward = forward.normalize();
        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 renderOrigin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        Vec3 end = mouth.add(forward.scale(FORWARD_OFFSET + SPEED * LIFETIME));
        var hit = dragon.level().clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon));
        double clearDistance = mouth.distanceTo(hit.getLocation());
        int newest = Mth.floor(time / EMISSION_INTERVAL);
        for (int slot = Math.max(0, newest - 1); slot <= newest; slot++) {
            float age = time - slot * EMISSION_INTERVAL;
            if (age < 0 || age >= LIFETIME) continue;
            double distance = FORWARD_OFFSET + age * SPEED;
            if (distance >= clearDistance) continue;
            float progress = age / LIFETIME;
            float size = Mth.lerp(smooth(progress), START_HALF_SIZE, END_HALF_SIZE);
            float alpha = 0.75F * smooth(Mth.clamp(age / 4.0F, 0, 1))
                    * (1.0F - smooth(Mth.clamp((progress - 0.55F) / 0.45F, 0, 1)));
            Vec3 center = mouth.add(forward.scale(distance)).subtract(renderOrigin);
            Vec3 across = right.scale(size);
            Vec3 vertical = up.scale(size);
            var buffer = buffers.getBuffer(BeamRenderTypes.translucent(FRAMES[Mth.floor(age / FRAME_TICKS) % FRAMES.length]));
            var pose = poses.last();
            vertex(buffer, pose, center.subtract(across).subtract(vertical), forward, 0, 1, alpha);
            vertex(buffer, pose, center.add(across).subtract(vertical), forward, 1, 1, alpha);
            vertex(buffer, pose, center.add(across).add(vertical), forward, 1, 0, alpha);
            vertex(buffer, pose, center.subtract(across).add(vertical), forward, 0, 0, alpha);
        }
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 point, Vec3 normal,
                               float u, float v, float alpha) {
        buffer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(0.55F, 0.90F, 1.0F, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
