package com.leon.saintsdragons.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DustParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected DustParticle(ClientLevel level, double x, double y, double z,
                           double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = -0.01F;
        this.friction = 0.86F;
        this.lifetime = 18 + this.random.nextInt(9);
        this.quadSize = 1.35F + this.random.nextFloat() * 0.55F;
        this.roll = (float) (Math.PI * 2.0D * this.random.nextDouble());
        this.oRoll = this.roll;
        applyBlockColor(level, x, y, z);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.setSpriteFromAge(this.sprites);
        float progress = this.age / (float) this.lifetime;
        this.alpha = progress < 0.22F ? progress / 0.22F : 1.0F - ((progress - 0.22F) / 0.78F);
        this.alpha = Math.max(0.0F, this.alpha);
        this.roll += 0.025F;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd = this.yd * this.friction + this.gravity;
        this.zd *= this.friction;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void applyBlockColor(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y - 0.1D, z).mutable();
        BlockState state = level.getBlockState(pos);
        for (int i = 0; i < 4 && (state.isAir() || state.is(Blocks.WATER)); i++) {
            pos.move(0, -1, 0);
            state = level.getBlockState(pos);
        }

        int color = DustParticle.getBlockColor(level, pos, state);
        this.rCol = ((color >> 16) & 255) / 255.0F;
        this.gCol = ((color >> 8) & 255) / 255.0F;
        this.bCol = (color & 255) / 255.0F;
    }

    private static int getBlockColor(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return 0xD6D1C8;
        }

        try {
            int color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            if (color != -1) {
                return color;
            }
        } catch (Exception ignored) {
        }

        return state.getMapColor(level, pos).col;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new DustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
