package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.particle.CindervaneFireTrailParticle;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.Map;
import java.util.WeakHashMap;

public final class CindervaneFireBodyParticles {
    private static final String[][] ANCHORS = {
            {"neck1Controller"}, {"neck2Controller"}, {"neck3Controller"}, {"neck4Controller"},
            {"mainbodyBone", "heightController"}, {"secondbodybone", "bone"},
            {"tail1"}, {"tail2"}, {"tail3"}, {"tail4"},
            {"leftarm"}, {"leftforearm"}, {"leftforearm2"},
            {"rightarm"}, {"rightforearm"}, {"rightforearm2"}
    };
    private static final Map<Cindervane, Integer> LAST_TICK = new WeakHashMap<>();
    private static final int FLAMES_PER_TICK = 64;
    private static final int DARK_FLAMES_PER_TICK = 16;
    private static final int SPARKS_PER_TICK = 10;
    private static final int SMOKE_PER_TICK = 4;

    private CindervaneFireBodyParticles() {}

    public static boolean samplesBone(String name) {
        for (String[] names : ANCHORS) {
            for (String candidate : names) {
                if (candidate.equals(name)) return true;
            }
        }
        return false;
    }

    public static void emit(Cindervane dragon, BakedGeoModel model,
                            Map<String, Matrix4f> transforms, float partialTick) {
        if (model == null || !dragon.isAlive() || !dragon.isBreathingFire() || dragon.isInWaterOrBubble()
                || ShaderPassCompatibility.isIrisShadowPass()) return;
        Integer previous = LAST_TICK.get(dragon);
        if (previous != null && previous == dragon.tickCount) return;
        var faces = DragonBoneSurfaceSampler.animatedSurfaces(model, transforms, ANCHORS);
        if (faces.isEmpty()) return;
        LAST_TICK.put(dragon, dragon.tickCount);
        double totalArea = faces.get(faces.size() - 1).cumulativeArea();
        Vec3 renderOrigin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        Vec3 motion = dragon.getDeltaMovement().scale(0.7);
        var random = dragon.getRandom();
        for (int i = 0; i < FLAMES_PER_TICK; i++) {
            double area = (i + random.nextDouble()) * totalArea / FLAMES_PER_TICK;
            Vec3 point = DragonBoneSurfaceSampler.sample(faces, area, random).add(renderOrigin);
            int layer = Math.floorMod(i + dragon.tickCount, 3);
            var type = switch (layer) {
                case 0 -> ModParticles.CINDERVANE_FIRE_TRAIL.get();
                case 1 -> ModParticles.CINDERVANE_SPEC_TRAIL.get();
                default -> ModParticles.CINDERVANE_MORE_SPEC_TRAIL.get();
            };
            var particle = Minecraft.getInstance().particleEngine.createParticle(type,
                    point.x, point.y, point.z, motion.x, motion.y + 0.025, motion.z);
            if (particle instanceof CindervaneFireTrailParticle flameParticle) {
                flameParticle.setBodySizeMultiplier(1.5625F);
            }
        }
        for (int i = 0; i < DARK_FLAMES_PER_TICK; i++) {
            double area = (i + random.nextDouble()) * totalArea / DARK_FLAMES_PER_TICK;
            Vec3 point = DragonBoneSurfaceSampler.sample(faces, area, random).add(renderOrigin);
            var particle = Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_DARK_FIRE_TRAIL.get(),
                    point.x, point.y, point.z, motion.x, motion.y + 0.025, motion.z);
            if (particle instanceof CindervaneFireTrailParticle flameParticle) {
                flameParticle.setBodySizeMultiplier(1.5625F);
            }
        }
        for (int i = 0; i < SPARKS_PER_TICK; i++) {
            double area = (i + random.nextDouble()) * totalArea / SPARKS_PER_TICK;
            Vec3 point = DragonBoneSurfaceSampler.sample(faces, area, random).add(renderOrigin);
            Vec3 drift = motion.add((random.nextDouble() - 0.5) * 0.12,
                    0.025 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.12);
            var type = i < 4 ? ModParticles.FIRE_BREATH_EMBER.get()
                    : i < 8 ? ModParticles.CINDERVANE_MOUTH_EMITTER.get()
                    : ModParticles.CINDERVANE_FIRE_BODY_STAR.get();
            Minecraft.getInstance().particleEngine.createParticle(type,
                    point.x, point.y, point.z, drift.x, drift.y, drift.z);
        }
        for (int i = 0; i < SMOKE_PER_TICK; i++) {
            double area = (i + random.nextDouble()) * totalArea / SMOKE_PER_TICK;
            Vec3 point = DragonBoneSurfaceSampler.sample(faces, area, random).add(renderOrigin);
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_FIRE_BODY_SMOKE.get(),
                    point.x, point.y, point.z,
                    motion.x * 0.5, motion.y * 0.5 + 0.035, motion.z * 0.5);
        }
    }

}
