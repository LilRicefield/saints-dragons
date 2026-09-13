package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.particle.CindervaneFireTrailParticle;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;
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
    private static final Map<GeoBone, List<Face>> SURFACES = new WeakHashMap<>();
    private static final int FLAMES_PER_TICK = 64;
    private static final int DARK_FLAMES_PER_TICK = 16;
    private static final int SPARKS_PER_TICK = 10;
    private static final int SMOKE_PER_TICK = 4;
    private static final double SURFACE_OFFSET = 0.06;

    private record Face(Vec3 origin, Vec3 edgeU, Vec3 edgeV, Vec3 normal) {}
    private record WeightedFace(Face face, double cumulativeArea) {}

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
        List<WeightedFace> faces = animatedSurfaces(model, transforms);
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
            Vec3 point = sample(faces, area, random).add(renderOrigin);
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
            Vec3 point = sample(faces, area, random).add(renderOrigin);
            var particle = Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_DARK_FIRE_TRAIL.get(),
                    point.x, point.y, point.z, motion.x, motion.y + 0.025, motion.z);
            if (particle instanceof CindervaneFireTrailParticle flameParticle) {
                flameParticle.setBodySizeMultiplier(1.5625F);
            }
        }
        for (int i = 0; i < SPARKS_PER_TICK; i++) {
            double area = (i + random.nextDouble()) * totalArea / SPARKS_PER_TICK;
            Vec3 point = sample(faces, area, random).add(renderOrigin);
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
            Vec3 point = sample(faces, area, random).add(renderOrigin);
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_FIRE_BODY_SMOKE.get(),
                    point.x, point.y, point.z,
                    motion.x * 0.5, motion.y * 0.5 + 0.035, motion.z * 0.5);
        }
    }

    private static List<WeightedFace> animatedSurfaces(BakedGeoModel model, Map<String, Matrix4f> transforms) {
        List<WeightedFace> result = new ArrayList<>();
        double totalArea = 0.0;
        for (String[] names : ANCHORS) {
            GeoBone bone = null;
            for (String name : names) {
                bone = model.getBone(name).orElse(null);
                if (bone != null) break;
            }
            if (bone == null || !isVisible(bone)) continue;
            Matrix4f transform = transforms.get(bone.getName());
            if (transform == null) continue;
            for (Face local : SURFACES.computeIfAbsent(bone, CindervaneFireBodyParticles::cacheSurfaces)) {
                Vec3 origin = transformPosition(transform, local.origin());
                Vec3 edgeU = transformDirection(transform, local.edgeU());
                Vec3 edgeV = transformDirection(transform, local.edgeV());
                Vec3 cross = edgeU.cross(edgeV);
                double area = cross.length();
                if (!Double.isFinite(area) || area < 1.0E-8) continue;
                Vec3 normal = cross.scale(1.0 / area);
                if (normal.dot(transformDirection(transform, local.normal())) < 0) {
                    normal = normal.scale(-1);
                }
                totalArea += area;
                result.add(new WeightedFace(new Face(origin, edgeU, edgeV, normal), totalArea));
            }
        }
        return result;
    }

    private static boolean isVisible(GeoBone bone) {
        if (bone.isHidden()) return false;
        for (GeoBone parent = bone.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.isHidingChildren()) return false;
        }
        return true;
    }

    private static List<Face> cacheSurfaces(GeoBone bone) {
        List<Face> faces = new ArrayList<>();
        for (GeoCube cube : bone.getCubes()) {
            Vec3 size = cube.size();
            if (size.x <= 1.0E-6 || size.y <= 1.0E-6 || size.z <= 1.0E-6 || cube.quads() == null) continue;
            PoseStack poses = new PoseStack();
            RenderUtils.translateToPivotPoint(poses, cube);
            RenderUtils.rotateMatrixAroundCube(poses, cube);
            RenderUtils.translateAwayFromPivotPoint(poses, cube);
            Matrix4f transform = poses.last().pose();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null || quad.vertices().length != 4) continue;
                Vec3 origin = transformPosition(transform, vector(quad.vertices()[0].position()));
                Vec3 edgeU = transformPosition(transform, vector(quad.vertices()[1].position())).subtract(origin);
                Vec3 edgeV = transformPosition(transform, vector(quad.vertices()[3].position())).subtract(origin);
                if (edgeU.cross(edgeV).lengthSqr() < 1.0E-12) continue;
                Vec3 normal = transformDirection(transform, vector(quad.normal())).normalize();
                faces.add(new Face(origin, edgeU, edgeV, normal));
            }
        }
        return List.copyOf(faces);
    }

    private static Vec3 sample(List<WeightedFace> faces, double area, RandomSource random) {
        int low = 0, high = faces.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (area < faces.get(middle).cumulativeArea()) high = middle;
            else low = middle + 1;
        }
        Face face = faces.get(low).face();
        return face.origin().add(face.edgeU().scale(random.nextDouble()))
                .add(face.edgeV().scale(random.nextDouble())).add(face.normal().scale(SURFACE_OFFSET));
    }

    private static Vec3 transformPosition(Matrix4fc matrix, Vec3 position) {
        return vector(matrix.transformPosition(position.toVector3f()));
    }

    private static Vec3 transformDirection(Matrix4fc matrix, Vec3 direction) {
        return vector(matrix.transformDirection(direction.toVector3f()));
    }

    private static Vec3 vector(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
