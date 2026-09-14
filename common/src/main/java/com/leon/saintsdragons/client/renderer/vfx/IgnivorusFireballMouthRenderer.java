package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.particle.CindervaneFireTrailParticle;
import com.leon.saintsdragons.client.particle.FireBreathBackblastParticle;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class IgnivorusFireballMouthRenderer {
    private static final ResourceLocation[] CHARGE = {
            SaintsDragonsCommon.rl("textures/particle/smaller_fire_explosion4.png"),
            SaintsDragonsCommon.rl("textures/particle/smaller_fire_explosion3.png"),
            SaintsDragonsCommon.rl("textures/particle/smaller_fire_explosion2.png"),
            SaintsDragonsCommon.rl("textures/particle/smaller_fire_explosion1.png"),
            SaintsDragonsCommon.rl("textures/particle/smaller_fire_explosion0.png")
    };
    private static final java.util.Map<Ignivorus, Integer> LAST_CHARGE_EMISSION = new java.util.WeakHashMap<>();
    private static final ResourceLocation[] FIRE = frames("fire_explosion", 7);
    private static final ResourceLocation[] SPLATTER = frames("splatter_orange", 10);
    private static final ResourceLocation[] STEAM = frames("steamy_explosion", 16);
    private static final java.util.Map<Ignivorus, Integer> LAST_BURST = new java.util.WeakHashMap<>();

    private IgnivorusFireballMouthRenderer() {}

    public static void render(Ignivorus dragon, Vec3 mouth, PoseStack poses,
                              MultiBufferSource buffers, float partialTick) {
        renderCharge(dragon, mouth, poses, buffers, partialTick);
        float age = dragon.getFireballMouthAge(partialTick);
        if (!dragon.isAlive() || mouth == null || age < 0.0F || age >= 10.0F
                || ShaderPassCompatibility.isIrisShadowPass()) return;
        Vec3 forward = Vec3.directionFromRotation(
                Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()),
                Mth.rotLerp(partialTick, dragon.yHeadRotO, dragon.yHeadRot));
        Vec3 anchor = mouth.subtract(dragon.position()).add(forward.scale(0.8D));
        layer(poses, buffers, STEAM, age, 0.5F, anchor, 2.5F, 1.0F, 0.48F, 0.08F);
        layer(poses, buffers, SPLATTER, age, 1.0F, anchor, 3.0F, 1.0F, 1.0F, 1.0F);
        layer(poses, buffers, FIRE, age, 1.0F, anchor, 4.5F, 1.0F, 1.0F, 1.0F);
        int shotTick = Math.round(dragon.tickCount + partialTick - age);
        Integer previous = LAST_BURST.put(dragon, shotTick);
        if (previous != null && previous == shotTick) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ())).add(anchor);
        var random = dragon.getRandom();
        for (int i = 0; i < 32; i++) {
            Vec3 spread = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,
                    random.nextDouble() - 0.5D);
            Vec3 velocity = forward.scale(0.3D + random.nextDouble() * 0.4D)
                    .add(spread.scale(0.8D)).add(dragon.getDeltaMovement().scale(0.65D));
            Vec3 point = origin.add(spread.scale(0.5D));
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_MOUTH_EMITTER.get(),
                    point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void renderCharge(Ignivorus dragon, Vec3 mouth, PoseStack poses,
                                     MultiBufferSource buffers, float partialTick) {
        float age = dragon.getFireballChargeVfxAge(partialTick);
        if (!dragon.isAlive() || mouth == null || age < 0.0F
                || ShaderPassCompatibility.isIrisShadowPass()) return;
        Vec3 forward = Vec3.directionFromRotation(
                Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()),
                Mth.rotLerp(partialTick, dragon.yHeadRotO, dragon.yHeadRot));
        Vec3 anchor = mouth.subtract(dragon.position()).add(forward.scale(0.8D));
        layer(poses, buffers, CHARGE, age, 1.0F, anchor, 3.75F, 1.0F, 1.0F, 1.0F);
        Integer previous = LAST_CHARGE_EMISSION.put(dragon, dragon.tickCount);
        if (previous != null && previous == dragon.tickCount) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ())).add(anchor);
        var particles = Minecraft.getInstance().particleEngine;
        var random = dragon.getRandom();
        for (int i = 0; i < 6; i++) {
            Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,
                    random.nextDouble() - 0.5D).normalize().scale(1.3D + random.nextDouble() * 1.2D);
            Vec3 point = origin.add(offset);
            Vec3 drift = dragon.getDeltaMovement().scale(0.65D).subtract(offset.scale(0.06D));
            var spec = particles.createParticle(ModParticles.CINDERVANE_MORE_SPEC_TRAIL.get(),
                    point.x, point.y, point.z, drift.x, drift.y, drift.z);
            if (spec instanceof CindervaneFireTrailParticle fireSpec) {
                fireSpec.setAnimationSpeed(2.0F);
            }
        }
        if (age >= 14.0F) return;
        Vec3 bodyForward = Vec3.directionFromRotation(0.0F,
                Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot));
        Vec3 right = bodyForward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 start = origin.subtract(forward.scale(0.8D)).subtract(bodyForward.scale(0.35D));
        for (int side : new int[] {-1, 1}) {
            Vec3 point = start.add(right.scale(side * 0.75D));
            Vec3 launch = bodyForward.scale(-Math.cos(Math.toRadians(30)))
                    .add(right.scale(side * Math.sin(Math.toRadians(30))));
            var particle = particles.createParticle(ModParticles.FIRE_BREATH_BACKBLAST.get(),
                    point.x, point.y, point.z, launch.x, launch.y, launch.z);
            if (particle instanceof FireBreathBackblastParticle backblast) {
                backblast.configureChargeBurst();
            }
        }
    }

    private static void layer(PoseStack poses, MultiBufferSource buffers, ResourceLocation[] frames,
                              float age, float frameTicks, Vec3 anchor, float size,
                              float red, float green, float blue) {
        float duration = frames.length * frameTicks;
        if (age >= duration) return;
        float progress = age / duration;
        float fade = Mth.clamp((progress - 0.5F) * 2.0F, 0.0F, 1.0F);
        float alpha = 1.0F - fade * fade * (3.0F - 2.0F * fade);
        ResourceLocation texture = frames[Math.min(frames.length - 1, (int) (age / frameTicks))];
        MultiBufferSource compatible = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
        BillboardFlashRenderer.renderQuad(poses, compatible, texture,
                (float) anchor.x, (float) anchor.y, (float) anchor.z,
                size * (0.8F + progress * 0.4F), 0.0F, red, green, blue, alpha);
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return frames;
    }
}
