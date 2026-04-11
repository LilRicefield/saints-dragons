package com.leon.saintsdragons.client.particle.raevyx;

import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Animated lightning chain particle that traces a path between two points.
 * Creates the visual effect of lightning jumping from target to target.
 */
public class RaevyxLightningChainParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final Vec3 startPos;
    private final Vec3 endPos;
    private final long renderSeed;

    protected RaevyxLightningChainParticle(ClientLevel level, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed,
                                           float size, SpriteSet spriteSet, Vec3 startPos, Vec3 endPos) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.startPos = startPos;
        this.endPos = endPos;
        this.renderSeed = BlockPos.containing(startPos).asLong() ^ BlockPos.containing(endPos).asLong();
        this.quadSize = size;
        this.lifetime = 6;
        this.setSize(size * 2.4F, size * 2.4F);
        this.setSpriteFromAge(spriteSet);

        Vec3 midpoint = startPos.lerp(endPos, 0.5D);
        this.setPos(midpoint.x, midpoint.y, midpoint.z);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            updateSprite();
            float fadeProgress = (this.age + 1.0F) / (float) this.lifetime;
            this.alpha = fadeProgress > 0.65F ? 1.0F - ((fadeProgress - 0.65F) / 0.35F) : 1.0F;
        }
    }

    private void updateSprite() {
        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public void render(@Nonnull VertexConsumer buffer, @Nonnull Camera camera, float partialTicks) {
        Vec3 delta = this.endPos.subtract(this.startPos);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }

        Vec3 camPos = camera.getPosition();
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 up = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = direction.cross(up);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vec3 normal = side.cross(direction).normalize();

        float progress = (this.age + partialTicks) / (float) Math.max(1, this.lifetime);
        long seed = this.renderSeed + (long)(progress * 9.0F);
        renderBoltLayer(buffer, camPos, side, normal, seed, 1.0F, 0.14F);
        renderBoltLayer(buffer, camPos, side, normal, seed + 31L, 0.7F, 0.08F);
    }

    private void renderBoltLayer(VertexConsumer buffer, Vec3 camPos, Vec3 side, Vec3 normal,
                                 long seed, float alphaScale, float jitterScale) {
        float[] lateralA = new float[8];
        float[] lateralB = new float[8];
        RandomSource random = RandomSource.create(seed);
        float offsetA = 0.0F;
        float offsetB = 0.0F;

        for (int i = 7; i >= 0; --i) {
            lateralA[i] = offsetA;
            lateralB[i] = offsetB;
            offsetA += random.nextInt(11) - 5;
            offsetB += random.nextInt(11) - 5;
        }

        for (int i = 0; i < 7; ++i) {
            float t0 = i / 7.0F;
            float t1 = (i + 1) / 7.0F;
            Vec3 p0 = this.startPos.lerp(this.endPos, t0)
                    .add(side.scale(lateralA[i] * jitterScale * this.quadSize))
                    .add(normal.scale(lateralB[i] * jitterScale * this.quadSize));
            Vec3 p1 = this.startPos.lerp(this.endPos, t1)
                    .add(side.scale(lateralA[i + 1] * jitterScale * this.quadSize))
                    .add(normal.scale(lateralB[i + 1] * jitterScale * this.quadSize));
            addSegment(buffer, camPos, p0, p1, alphaScale * this.alpha);
        }
    }

    private void addSegment(VertexConsumer buffer, Vec3 camPos, Vec3 start, Vec3 end, float alpha) {
        Vec3 segment = end.subtract(start);
        double segmentLength = segment.length();
        if (segmentLength < 1.0E-4D) {
            return;
        }

        Vec3 midpoint = start.add(end).scale(0.5D);
        Vec3 toCamera = camPos.subtract(midpoint);
        if (toCamera.lengthSqr() < 1.0E-6D) {
            toCamera = new Vec3(0.0D, 1.0D, 0.0D);
        }

        Vec3 widthAxis = segment.normalize().cross(toCamera.normalize());
        if (widthAxis.lengthSqr() < 1.0E-6D) {
            widthAxis = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            widthAxis = widthAxis.normalize();
        }

        double width = Math.max(0.02D, this.quadSize * 0.18D);
        Vec3 offset = widthAxis.scale(width);
        Vec3 v0Pos = start.add(offset).subtract(camPos);
        Vec3 v1Pos = start.subtract(offset).subtract(camPos);
        Vec3 v2Pos = end.subtract(offset).subtract(camPos);
        Vec3 v3Pos = end.add(offset).subtract(camPos);

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(0.0F);

        buffer.vertex((float) v0Pos.x, (float) v0Pos.y, (float) v0Pos.z).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, alpha).uv2(light).endVertex();
        buffer.vertex((float) v1Pos.x, (float) v1Pos.y, (float) v1Pos.z).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, alpha).uv2(light).endVertex();
        buffer.vertex((float) v2Pos.x, (float) v2Pos.y, (float) v2Pos.z).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, alpha).uv2(light).endVertex();
        buffer.vertex((float) v3Pos.x, (float) v3Pos.y, (float) v3Pos.z).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, alpha).uv2(light).endVertex();
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 240; // Fullbright
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<RaevyxLightningChainData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        
        @Override
        public Particle createParticle(@Nonnull RaevyxLightningChainData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            try {
                return new RaevyxLightningChainParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet, data.startPos(), data.endPos());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return null;
            }
        }
    }
}
