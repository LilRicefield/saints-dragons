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
    private static final ResourceLocation[] ABSORB_FIRE = frames("better_fire", 8);
    private static final ResourceLocation ABSORB_EMITTER = SaintsDragonsCommon.rl("textures/particle/glowing_emitter.png");
    private static final ResourceLocation[] LEVEL_TWO_SHARP = reversedFrames("sharp_impact", 7, 0);
    private static final ResourceLocation[] LEVEL_TWO_GROUND = reversedFrames("fire_ground_impact", 5, 0);
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
    private static final ResourceLocation[] SHOOT_SHARP = frames("sharp_impact", 8);
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
        boolean levelTwo = dragon.isLevelTwoFireballMouth();
        float burstScale = levelTwo ? 1.5F : 1.0F;
        if (levelTwo) {
            layer(poses, buffers, SHOOT_SHARP, age, 1.0F, anchor, 6.0F, 1.0F, 0.55F, 0.12F);
        }
        layer(poses, buffers, STEAM, age, 0.5F, anchor, 2.5F * burstScale, 1.0F, 0.48F, 0.08F);
        layer(poses, buffers, SPLATTER, age, 1.0F, anchor, 3.0F * burstScale, 1.0F, 1.0F, 1.0F);
        layer(poses, buffers, FIRE, age, 1.0F, anchor, 4.5F * burstScale, 1.0F, 1.0F, 1.0F);
        int shotTick = Math.round(dragon.tickCount + partialTick - age);
        Integer previous = LAST_BURST.put(dragon, shotTick);
        if (previous != null && previous == shotTick) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ())).add(anchor);
        var random = dragon.getRandom();
        for (int i = 0; i < (levelTwo ? 48 : 32); i++) {
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
        boolean levelTwo = dragon.getFireballChargeVfxLevel() == 2;
        if (levelTwo) {
            renderAbsorption(dragon, poses, buffers, anchor, age);
            layer(poses, buffers, LEVEL_TWO_SHARP, age, 1.0F, anchor, 4.5F, 1.0F, 0.55F, 0.12F);
            layer(poses, buffers, LEVEL_TWO_GROUND, age, 1.0F, anchor, 7.5F, 1.0F, 1.0F, 1.0F);
        } else {
            layer(poses, buffers, CHARGE, age, 1.0F, anchor, 3.75F, 1.0F, 1.0F, 1.0F);
        }
        Integer previous = LAST_CHARGE_EMISSION.put(dragon, dragon.tickCount);
        if (previous != null && previous == dragon.tickCount) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ())).add(anchor);
        var particles = Minecraft.getInstance().particleEngine;
        var random = dragon.getRandom();
        for (int i = 0; i < (levelTwo ? 8 : 6); i++) {
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
        if (age >= (levelTwo ? 15.0F : 14.0F)) return;
        Vec3 bodyForward = Vec3.directionFromRotation(0.0F,
                Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot));
        Vec3 right = bodyForward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 start = origin.subtract(forward.scale(0.8D)).subtract(bodyForward.scale(0.35D));
        for (int side : new int[] {-1, 1}) {
            Vec3 point = start.add(right.scale(side * 0.75D));
            Vec3 launch = bodyForward.scale(-Math.cos(Math.toRadians(30)))
                    .add(right.scale(side * Math.sin(Math.toRadians(30))));
            if (levelTwo) launch = launch.scale(1.25D);
            var particle = particles.createParticle(ModParticles.FIRE_BREATH_BACKBLAST.get(),
                    point.x, point.y, point.z, launch.x, launch.y, launch.z);
            if (particle instanceof FireBreathBackblastParticle backblast) {
                if (levelTwo) backblast.configureLevelTwoChargeBurst();
                else backblast.configureChargeBurst();
            }
        }
    }

    private static void renderAbsorption(Ignivorus dragon, PoseStack poses, MultiBufferSource buffers,
                                         Vec3 anchor, float age) {
        for (int i = 0; i < 40; i++) {
            float time = age - i * 0.2F;
            if (time < 0.0F) continue;
            int cycle = (int) (time / 8.0F);
            float progress = (time % 8.0F) / 8.0F;
            var random = net.minecraft.util.RandomSource.create(dragon.getUUID().getLeastSignificantBits()
                    ^ (i * 73428767L) ^ (cycle * 912931L));
            double azimuth = random.nextDouble() * Math.PI * 2.0D;
            double vertical = random.nextDouble() * 2.0D - 1.0D;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
            double radius = (2.0D + random.nextDouble() * 2.0D) * (1.0D - progress * progress);
            Vec3 point = anchor.add(Math.cos(azimuth) * horizontal * radius,
                    vertical * radius, Math.sin(azimuth) * horizontal * radius);
            boolean fire = i < 16;
            ResourceLocation texture = fire ? ABSORB_FIRE[Math.min(7, (int) (progress * 8))] : ABSORB_EMITTER;
            float fadeIn = Mth.clamp(progress / 0.15F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - progress) / 0.25F, 0.0F, 1.0F);
            float alpha = fadeIn * fadeOut * 0.9F;
            float size = (fire ? 0.3F : 0.055F) * (1.0F - progress * 0.55F);
            MultiBufferSource compatible = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
            BillboardFlashRenderer.renderQuad(poses, compatible, texture,
                    (float) point.x, (float) point.y, (float) point.z,
                    size, 0.0F, 1.0F, fire ? 0.55F : 0.75F, fire ? 0.10F : 0.20F, alpha);
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

    private static ResourceLocation[] reversedFrames(String prefix, int first, int last) {
        ResourceLocation[] frames = new ResourceLocation[first - last + 1];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + (first - i) + ".png");
        }
        return frames;
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return frames;
    }
}
