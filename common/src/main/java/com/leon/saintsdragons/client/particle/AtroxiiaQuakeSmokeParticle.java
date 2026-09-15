package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public final class AtroxiiaQuakeSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final double originX, originY, originZ;
    private final double startAngle, startRadius, expansion, spin;
    private final float size;

    private AtroxiiaQuakeSmokeParticle(ClientLevel level, double x, double y, double z,
                                       double dx, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        originX = x; originY = y; originZ = z;
        startAngle = Math.atan2(dz, dx);
        startRadius = 2 + random.nextDouble() * 1.5;
        expansion = 12 + random.nextDouble() * 4;
        spin = 0.7 + random.nextDouble() * 0.4;
        size = 1.0F + random.nextFloat() * 0.6F;
        lifetime = 21;
        hasPhysics = false;
        alpha = 0;
        setColor(0.8F, 0.9F, 1.0F);
        setSprite(sprites.get(0, 13));
        positionAt(0);
        xo = this.x; yo = this.y; zo = this.z;
    }

    private void positionAt(float progress) {
        float spreadProgress = Mth.clamp(progress * 1.35F, 0, 1);
        double radius = startRadius + expansion * (1 - (1 - spreadProgress) * (1 - spreadProgress));
        double angle = startAngle + spin * progress;
        x = originX + Math.cos(angle) * radius;
        y = originY + progress * 0.8;
        z = originZ + Math.sin(angle) * radius;
        setBoundingBox(new AABB(x - 6, y - 6, z - 6, x + 6, y + 6, z + 6));
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        positionAt(age / (float) lifetime);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float elapsed = age + partialTick;
        float progress = Mth.clamp(elapsed / lifetime, 0, 1);
        setSprite(sprites.get(Math.min(13, (int) (elapsed / 1.5F)), 13));
        float fade = Mth.clamp((1 - progress) / 0.65F, 0, 1);
        alpha = 0.4F * Math.min(1, elapsed / 2) * fade * fade * (3 - 2 * fade);
        quadSize = size * (1 + progress * 1.8F);
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new AtroxiiaQuakeSmokeParticle(level, x, y, z, vx, vz, sprites);
        }
    }
}
