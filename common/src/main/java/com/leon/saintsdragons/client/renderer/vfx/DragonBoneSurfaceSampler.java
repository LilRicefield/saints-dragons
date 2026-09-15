package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
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

final class DragonBoneSurfaceSampler {
    private static final Map<GeoBone, List<Face>> SURFACES = new WeakHashMap<>();
    private static final double SURFACE_OFFSET = 0.06;
    record Face(Vec3 origin, Vec3 edgeU, Vec3 edgeV, Vec3 normal) {}
    record WeightedFace(Face face, double cumulativeArea) {}
    static List<WeightedFace> animatedSurfaces(BakedGeoModel model, Map<String, Matrix4f> transforms, String[][] anchors) {
        List<WeightedFace> result = new ArrayList<>();
        double totalArea = 0.0;
        for (String[] names : anchors) {
            GeoBone bone = null;
            for (String name : names) {
                bone = model.getBone(name).orElse(null);
                if (bone != null) break;
            }
            if (bone == null || !isVisible(bone)) continue;
            Matrix4f transform = transforms.get(bone.getName());
            if (transform == null) continue;
            for (Face local : SURFACES.computeIfAbsent(bone, DragonBoneSurfaceSampler::cacheSurfaces)) {
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

    static Vec3 sample(List<WeightedFace> faces, double area, RandomSource random) {
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
