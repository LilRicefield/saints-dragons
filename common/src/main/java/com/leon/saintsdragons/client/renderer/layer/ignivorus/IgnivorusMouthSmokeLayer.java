package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Simple mouth smoke layer: spawns vanilla smoke particles at the fire bone
 * so there is a visible puff even when the long-distance cone is culled.
 */
public class IgnivorusMouthSmokeLayer extends GeoRenderLayer<Ignivorus> {

    public IgnivorusMouthSmokeLayer() {
        super(null);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, Ignivorus animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isBreathingFire()) {
            return;
        }
        if (!(animatable.level() instanceof ClientLevel clientLevel)) {
            return;
        }

        Vec3 start = animatable.getFireBreathStartAnchor(partialTick);
        if (start == null) {
            return;
        }

        Vec3 look = Vec3.directionFromRotation(animatable.getXRot(), animatable.yHeadRot).normalize();
        Vec3 spawnCenter = start.add(look.scale(0.35D));
        RandomSource random = animatable.getRandom();

        for (int i = 0; i < 4; i++) {
            double jitterX = (random.nextDouble() - 0.5D) * 0.2D;
            double jitterY = random.nextDouble() * 0.15D;
            double jitterZ = (random.nextDouble() - 0.5D) * 0.2D;
            double px = spawnCenter.x + jitterX;
            double py = spawnCenter.y + jitterY;
            double pz = spawnCenter.z + jitterZ;

            clientLevel.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0D, 0.01D, 0.0D);
            if (random.nextFloat() < 0.35F) {
                clientLevel.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 0.0D, 0.02D, 0.0D);
            }
        }
    }
}
