package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CindervaneFireballMouthRenderer {
    private static final java.util.Map<Cindervane, Integer> LAST_SPARK_BURST = new java.util.WeakHashMap<>();
    private static final ResourceLocation[] SMOKE = frames("smoke_bomb_orange", 10);
    private static final ResourceLocation[] CIRCLE = frames("circle_thinning", 12);
    private static final float CIRCLE_FRAME_TICKS = 0.5F;
    private static final float SMOKE_FRAME_TICKS = 1.0F;
    private static final AttachedPlaneFlipbookRenderer.Style CIRCLE_STYLE =
            new AttachedPlaneFlipbookRenderer.Style(2.7F, 0.9F, CIRCLE_FRAME_TICKS, 1.0F, 0.0F);

    private CindervaneFireballMouthRenderer() {}

    public static void render(Cindervane dragon, Vec3 head, PoseStack poses,
                              MultiBufferSource buffers, float partialTick) {
        float age = dragon.getFireballMouthAge(partialTick);
        float smokeDuration = SMOKE.length * SMOKE_FRAME_TICKS;
        float circleDuration = CIRCLE.length * CIRCLE_FRAME_TICKS;
        if (!dragon.isAlive() || head == null || age < 0 || age >= Math.max(smokeDuration, circleDuration)) return;
        Vec3 forward = Vec3.directionFromRotation(
                Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()),
                Mth.rotLerp(partialTick, dragon.yHeadRotO, dragon.yHeadRot));
        Vec3 anchor = head.subtract(dragon.position()).add(forward.scale(1.2D));
        emitMouthSparks(dragon, anchor, forward, age, partialTick);
        if (age < smokeDuration) {
            float fade = 1.0F - Mth.clamp((age / smokeDuration - 0.6F) / 0.4F, 0.0F, 1.0F);
            ResourceLocation texture = SMOKE[(int) (age / SMOKE_FRAME_TICKS)];
            MultiBufferSource smokeBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
            BillboardFlashRenderer.renderQuad(poses, smokeBuffers, texture,
                    (float) anchor.x, (float) anchor.y, (float) anchor.z,
                    2.25F, 0.0F, 1.0F, 1.0F, 1.0F, fade);
        }
        if (age >= circleDuration) return;
        ResourceLocation circleTexture = CIRCLE[(int) (age / CIRCLE_FRAME_TICKS)];
        MultiBufferSource circleBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(circleTexture));
        poses.pushPose();
        poses.translate(anchor.x, anchor.y, anchor.z);
        float pitch = (float) Math.acos(Mth.clamp(forward.y, -1.0D, 1.0D));
        float yaw = (float) Math.atan2(forward.z, forward.x);
        poses.mulPose(Axis.YP.rotation(Mth.PI / 2.0F - yaw));
        poses.mulPose(Axis.XP.rotation(-Mth.PI / 2.0F + pitch));
        AttachedPlaneFlipbookRenderer.renderOnce(poses, circleBuffers, CIRCLE, CIRCLE_STYLE,
                age, 0.0F, 1.0F, 0.55F, 0.12F);
        poses.popPose();
    }

    private static void emitMouthSparks(Cindervane dragon, Vec3 anchor, Vec3 forward, float age, float partialTick) {
        if (com.leon.saintsdragons.client.renderer.ShaderPassCompatibility.isIrisShadowPass()) return;
        int shotTick = Math.round(dragon.tickCount + partialTick - age);
        Integer previous = LAST_SPARK_BURST.put(dragon, shotTick);
        if (previous != null && previous == shotTick) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ())).add(anchor);
        var random = dragon.getRandom();
        for (int i = 0; i < 18; i++) {
            Vec3 spread = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5,
                    random.nextDouble() - 0.5);
            Vec3 velocity = forward.scale(0.15 + random.nextDouble() * 0.15)
                    .add(spread.scale(0.35)).add(dragon.getDeltaMovement().scale(0.65));
            Vec3 point = origin.add(spread.scale(0.3));
            dragon.level().addParticle(i < 8
                            ? com.leon.saintsdragons.common.registry.ModParticles.FIRE_BREATH_EMBER.get()
                            : com.leon.saintsdragons.common.registry.ModParticles.CINDERVANE_MOUTH_EMITTER.get(), true,
                    point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] result = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            result[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return result;
    }
}
