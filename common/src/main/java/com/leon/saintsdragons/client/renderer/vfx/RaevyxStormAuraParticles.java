package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import java.util.Map;
import java.util.WeakHashMap;

public final class RaevyxStormAuraParticles {
    private static final String[][] ANCHORS = {
            {"neck1Controller"}, {"neck2Controller"}, {"neck3Controller"}, {"bodyBone"},
            {"tail1"}, {"tail2"}, {"tail3"}, {"tail4"}, {"tail5"},
            {"leftwing"}, {"leftwingarm"}, {"rightwing"}, {"rightwingarm"},
            {"leftlegpart"}, {"leftanklepart"}, {"leftfemurpart"},
            {"rightlegpart"}, {"rightanklepart"}, {"rightfemurpart"}, {"leftouterphalanges"},
            {"justleft"} ,{"leftmostmiddlephalanges"}, {"leftmiddlephalanges"}, {"leftinnerphalanges"},
            {"rightouterphalanges"}, {"justright"} ,{"rightmostmiddlephalanges"}, {"rightmiddlephalanges"},
            {"rightinnerphalanges"}
    };
    private static final Map<Raevyx, Integer> LAST_TICK = new WeakHashMap<>();

    public static boolean samplesBone(String name) {
        for (String[] anchor : ANCHORS) if (anchor[0].equals(name)) return true;
        return false;
    }

    public static void emit(Raevyx dragon, BakedGeoModel model, Map<String, Matrix4f> transforms, float partialTick) {
        if (model == null || !dragon.isAlive() || !dragon.isStormAuraActive()
                || ShaderPassCompatibility.isIrisShadowPass()) return;
        Integer previous = LAST_TICK.get(dragon);
        if (previous != null && previous == dragon.tickCount) return;
        var faces = DragonBoneSurfaceSampler.animatedSurfaces(model, transforms, ANCHORS);
        if (faces.isEmpty()) return;
        LAST_TICK.put(dragon, dragon.tickCount);
        double totalArea = faces.get(faces.size() - 1).cumulativeArea();
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        var random = dragon.getRandom();
        boolean gold = dragon.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        for (int layer = 0; layer < 5; layer++) {
            int count = layer < 2 ? 3 : layer == 2 ? 10 : layer == 3 ? 12 : 4;
            var type = switch (layer) {
                case 0 -> ModParticles.RAEVYX_STORM_AURA.get();
                case 1 -> ModParticles.RAEVYX_STORM_ZAP.get();
                case 2, 4 -> ModParticles.RAEVYX_STORM_EMITTER.get();
                default -> ModParticles.RAEVYX_STORM_STAR.get();
            };
            for (int i = 0; i < count; i++) {
                Vec3 point = DragonBoneSurfaceSampler.sample(faces,
                        (i + random.nextDouble()) * totalArea / count, random).add(origin);
                Vec3 drift = dragon.getDeltaMovement().scale(0.7).add(
                        (random.nextDouble() - 0.5) * 0.16, random.nextDouble() * 0.08,
                        (random.nextDouble() - 0.5) * 0.16);
                var particle = Minecraft.getInstance().particleEngine.createParticle(type,
                        point.x, point.y, point.z, drift.x, drift.y, drift.z);
                if (particle != null) {
                    if (layer == 4) particle.setColor(0.0F, 0.0F, 0.0F);
                    else particle.setColor(1.0F, gold ? 0.72F : 0.06F, gold ? 0.12F : 0.08F);
                }
            }
        }
    }
}
