package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RaevyxStormLightningRenderer {
    private static final String[][] PAIRS = {
            {"headController"}, {"neck1Controller"}, {"neck2Controller"}, {"neck3Controller"}, {"bodyBone"},
            {"tail1"}, {"tail2"}, {"tail3"}, {"tail4"}, {"tail5"},
            {"leftwing"}, {"leftwingarm"}, {"rightwing"}, {"rightwingarm"},
            {"leftlegpart"}, {"leftanklepart"}, {"leftfemurpart"},
            {"rightlegpart"}, {"rightanklepart"}, {"rightfemurpart"}, {"leftouterphalanges"},
            {"justleft"}, {"justleft2"}, {"justleft3"}, {"justleft4"} ,{"leftmostmiddlephalanges"}, {"leftmiddlephalanges"}, {"leftinnerphalanges"},
            {"rightouterphalanges"}, {"justright"}, {"justright2"}, {"justright3"}, {"justright4"} ,{"rightmostmiddlephalanges"}, {"rightmiddlephalanges"},
            {"rightinnerphalanges"}, {"thagomizer"}, {"spine1"}, {"spine2"}, {"spine3"}, {"spine4"}, {"spine5"}, {"spine6"}, {"spine7"}, {"spine8"}, {"spine9"}, {"spine10"}, {"spine11"},
            {"leftwingwebbing"}, {"leftwingarmwebbing"}, {"leftinnerphalangeswebbing"}, {"leftmiddlephalangeswebbing"}, {"leftmostmiddlephalangeswebbing"}, {"leftouterphalangeswebbing"},
            {"rightwingwebbing"}, {"rightwingarmwebbing"}, {"rightinnerphalangeswebbing"}, {"rightmiddlephalangeswebbing"}, {"rightmostmiddlephalangeswebbing"}, {"rightouterphalangeswebbing"}
    };
    private static final Map<Raevyx, State> STATES = new WeakHashMap<>();
    public static boolean samplesBone(String name) {
        for (String[] entry : PAIRS) {
            for (String bone : entry) if (bone.equals(name)) return true;
        }
        return false;
    }

    private static String endBone(String[] entry, BakedGeoModel model) {
        if (entry.length > 1) return entry[1];
        var bone = model.getBone(entry[0]).orElse(null);
        if (bone != null && bone.getParent() != null && samplesBone(bone.getParent().getName())) {
            return bone.getParent().getName();
        }
        return entry[0];
    }
    private record Anchor(String bone, Vec3 local) {
        Vec3 position(Map<String, Matrix4f> transforms) {
            Matrix4f matrix = transforms.get(bone);
            if (matrix == null) return null;
            var point = matrix.transformPosition(local.toVector3f());
            return new Vec3(point.x, point.y, point.z);
        }
    }
    private record Arc(Anchor start, Anchor end, Vec3 groundStart, Vec3 ground,
                       int tick, int lifetime, long seed, boolean outward) {}
    private static final class State {
        final List<Arc> arcs = new ArrayList<>();
        final RandomSource random = RandomSource.create();
        int lastTick = -1;
        int nextBody;
        int nextGround;
        int nextOutward;
    }

    public static void render(Raevyx dragon, BakedGeoModel model, Map<String, Matrix4f> transforms,
                              PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        if (!dragon.isAlive() || !dragon.isStormAuraActive()) {
            STATES.remove(dragon);
            return;
        }
        if (model == null || transforms.isEmpty()) return;
        State state = STATES.computeIfAbsent(dragon, ignored -> new State());
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()), Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        boolean grounded = dragon.onGround() && !dragon.isFlying() && !dragon.isTakeoff();
        state.arcs.removeIf(arc -> dragon.tickCount - arc.tick >= arc.lifetime || (arc.ground != null && !grounded));
        if (state.lastTick != dragon.tickCount) {
            state.lastTick = dragon.tickCount;
            if (dragon.tickCount >= state.nextBody) {
                state.nextBody = dragon.tickCount + 2;
                for (int i = 0; i < 4; i++) {
                    if (state.arcs.stream().filter(arc -> arc.ground == null && !arc.outward).count() >= 10) break;
                    String[] pair = PAIRS[state.random.nextInt(PAIRS.length)];
                    if (pair.length == 0) continue;
                    String destination = endBone(pair, model);
                    Anchor start = anchor(pair[0], model, transforms, state.random);
                    Anchor end = anchor(destination, model, transforms, state.random);
                    if (end == null) {
                        destination = pair[0];
                        end = anchor(destination, model, transforms, state.random);
                    }
                    if (start != null && end != null) {
                        Vec3 from = start.position(transforms);
                        for (int attempt = 0; attempt < 3; attempt++) {
                            Anchor candidate = anchor(destination, model, transforms, state.random);
                            if (candidate != null && candidate.position(transforms).distanceToSqr(from)
                                    < end.position(transforms).distanceToSqr(from)) end = candidate;
                        }
                    }
                    if (start != null && end != null) state.arcs.add(new Arc(start, end, null, null,
                            dragon.tickCount, 3 + state.random.nextInt(3), state.random.nextLong(), false));
                }
            }
            if (grounded && dragon.tickCount >= state.nextGround) {
                state.nextGround = dragon.tickCount + 3 + state.random.nextInt(3);
                if (state.random.nextFloat() < 0.95F) {
                    for (int i = 0, count = 3 + state.random.nextInt(3); i < count; i++) {
                        var bounds = dragon.getBoundingBox();
                        double angle = state.random.nextDouble() * Math.PI * 2;
                        double dx = Math.cos(angle), dz = Math.sin(angle);
                        double edge = Math.min(bounds.getXsize() * 0.5 / Math.max(1.0E-6, Math.abs(dx)),
                                bounds.getZsize() * 0.5 / Math.max(1.0E-6, Math.abs(dz)));
                        double radius = edge + 1.0 + state.random.nextDouble() * 2.5;
                        Vec3 from = origin.add(dx * radius,
                                bounds.getYsize() * (1.3 + state.random.nextDouble() * 0.9), dz * radius);
                        Vec3 to = from.add(dx * 0.5, -Math.max(14, bounds.getYsize() * 2.2 + 4), dz * 0.5);
                        var hit = dragon.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, dragon));
                        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getStepY() <= 0) continue;
                        Vec3 ground = hit.getLocation().add(0, 0.035, 0);
                        if (from.y - ground.y < 0.25) continue;
                        state.arcs.add(new Arc(null, null, from, ground, dragon.tickCount, 4, state.random.nextLong(), false));
                        impact(dragon, ground, state.random);
                    }
                }
            }
        }
        if (dragon.tickCount >= state.nextOutward) {
            state.nextOutward = dragon.tickCount + 2;
            Vec3 bodyCenter = dragon.getBoundingBox().getCenter().subtract(dragon.position());
            // Keep both endpoints bone-local so the whole discharge follows the current animated pose.
            for (int i = 0; i < 3; i++) {
                if (state.arcs.stream().filter(arc -> arc.outward).count() >= 6) break;
                String bone = PAIRS[state.random.nextInt(PAIRS.length)][0];
                Anchor start = anchor(bone, model, transforms, state.random);
                if (start == null) continue;
                Vec3 from = start.position(transforms);
                Vec3 direction = from.subtract(bodyCenter).normalize().add(
                        (state.random.nextDouble() - 0.5) * 0.5,
                        0.15 + state.random.nextDouble() * 0.35,
                        (state.random.nextDouble() - 0.5) * 0.5).normalize();
                Vec3 to = from.add(direction.scale(3.0 + state.random.nextDouble() * 4.0));
                var localEnd = new Matrix4f(transforms.get(bone)).invert().transformPosition(to.toVector3f());
                if (!Float.isFinite(localEnd.x) || !Float.isFinite(localEnd.y) || !Float.isFinite(localEnd.z)) continue;
                Anchor end = new Anchor(bone, new Vec3(localEnd.x, localEnd.y, localEnd.z));
                state.arcs.add(new Arc(start, end, null, null, dragon.tickCount, 4,
                        state.random.nextLong(), true));
            }
        }
        boolean gold = dragon.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        for (Arc arc : state.arcs) {
            Vec3 from = arc.groundStart != null ? arc.groundStart.subtract(origin) : arc.start.position(transforms);
            Vec3 to = arc.ground != null ? arc.ground.subtract(origin) : arc.end.position(transforms);
            if (from == null || to == null) continue;
            Vec3 delta = to.subtract(from);
            float length = (float) delta.length();
            if (length < 0.05F) continue;
            float age = dragon.tickCount - arc.tick + partialTick;
            float fade = Mth.clamp(1 - age / arc.lifetime, 0, 1);
            float alpha = Math.min(1, age / 0.2F) * fade;
            float renderedLength = arc.outward ? length * Mth.clamp(age / 0.45F, 0, 1) : length;
            if (renderedLength < 0.05F) continue;
            poses.pushPose();
            poses.translate(from.x, from.y, from.z);
            poses.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                    (float) (delta.x / length), (float) (delta.y / length), (float) (delta.z / length)));
            ProceduralBeamLightningRenderer.emitBolt(buffers.getBuffer(RenderType.lightning()), poses.last().pose(),
                    renderedLength, arc.seed, arc.outward ? 0.75F : arc.ground == null ? 0.35F : 1.2F, alpha,
                    1, gold ? 0.72F : 0.06F, gold ? 0.12F : 0.08F);
            poses.popPose();
        }
    }

    private static Anchor anchor(String bone, BakedGeoModel model, Map<String, Matrix4f> transforms, RandomSource random) {
        var faces = DragonBoneSurfaceSampler.animatedSurfaces(model, transforms, new String[][] {{bone}});
        if (faces.isEmpty()) return null;
        double total = faces.get(faces.size() - 1).cumulativeArea();
        Vec3 point = DragonBoneSurfaceSampler.sample(faces, random.nextDouble() * total, random);
        var local = new Matrix4f(transforms.get(bone)).invert().transformPosition(point.toVector3f());
        if (!Float.isFinite(local.x) || !Float.isFinite(local.y) || !Float.isFinite(local.z)) return null;
        return new Anchor(bone, new Vec3(local.x, local.y, local.z));
    }

    private static void impact(Raevyx dragon, Vec3 point, RandomSource random) {
        boolean gold = dragon.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        for (int i = 0; i < 8; i++) {
            var particle = Minecraft.getInstance().particleEngine.createParticle(i < 6
                            ? ModParticles.RAEVYX_STORM_EMITTER.get() : ModParticles.RAEVYX_STORM_STAR.get(),
                    point.x, point.y, point.z, (random.nextDouble() - 0.5) * 0.3,
                    0.05 + random.nextDouble() * 0.12, (random.nextDouble() - 0.5) * 0.3);
            if (particle != null) particle.setColor(1, gold ? 0.72F : 0.06F, gold ? 0.12F : 0.08F);
        }
    }
}
