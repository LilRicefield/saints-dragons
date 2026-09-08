package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class FireBreathParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 24;
    private static final float FRAME_TICKS = 0.35F;
    private static final float GROWTH_TICKS = 5.0F;
    private static final int IMPACT_FADE_TICKS = 3;
    private static final float EMBER_EMITTER_CHANCE = 0.15F;
    private static final int MAX_EMBERS = 2;
    private static final float SMOKE_SCALE = 1.25F;
    private static final float CORE_SCALE = 0.65F;
    private static final double SMOKE_DEPTH_OFFSET = 0.04;
    private static final double EMBER_SPREAD_X_DEGREES = -40.0;
    private static final double EMBER_SPREAD_Y_DEGREES = 40.0;

    private final SpriteSet sprites;
    private final Vec3 origin;
    private final Vec3 forward;
    private final Vec3 spread;
    private final double speed;
    private final double range;
    private final float fullSize;
    private final float spin;
    private double distance;
    private int impactAge = -1;
    private final boolean emitsEmber;
    private final int emberEmissionAge;
    private int embersEmitted;
    private final int smokeEmissionAge;
    private boolean emittedSmoke;

    private FireBreathParticle(ClientLevel level, double x, double y, double z, Vec3 velocity,
                              FireBreathParticleData data, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.emitsEmber = random.nextFloat() < EMBER_EMITTER_CHANCE;
        this.emberEmissionAge = 2 + random.nextInt(5);
        this.origin = new Vec3(x, y, z);
        this.forward = velocity.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : velocity.normalize();
        this.speed = Mth.clamp(velocity.length(), 0.5, 12) * (0.85 + random.nextDouble() * 0.15);
        this.range = data.range();
        this.fullSize = 1.3F + random.nextFloat() * 0.4F;
        this.spin = (random.nextFloat() - 0.5F) * 0.04F;

        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = Math.sqrt(random.nextDouble());
        this.spread = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));

        this.lifetime = Math.min(ExpandingBreathSection.MAX_TICKS, (int) Math.ceil(range / speed)) + 1;
        this.smokeEmissionAge = random.nextFloat() < 0.15F
                ? 1 + random.nextInt(Math.max(1, lifetime - 1)) : -1;
        this.hasPhysics = false;
        this.roll = this.oRoll = random.nextFloat() * (float) (Math.PI * 2);
        this.quadSize = fullSize * 0.45F;
        this.setColor(1.0F, 0.42F, 0.035F);
        this.setSprite(sprites.get(0, FRAME_COUNT - 1));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        this.roll += spin;
        if (impactAge >= 0) {
            emitEmbers();
            return;
        }

        Vec3 start = new Vec3(x, y, z);
        distance = Math.min(range, distance + speed);
        double spreadWidth = 0.15 + 0.8 * Math.max(0, ExpandingBreathSection.halfWidth(distance) - fullSize);
        double endSpread = Mth.clamp((distance / range - 0.5) * 2.0, 0.0, 1.0);
        spreadWidth *= 1.0 + endSpread * endSpread * (3.0 - 2.0 * endSpread);
        Vec3 end = origin.add(forward.scale(distance)).add(spread.scale(spreadWidth));
        if (!level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) {
            remove();
            return;
        }

        // Fabric's vanilla ClipContext requires an entity even for cosmetic raycasts.
        var contextEntity = Minecraft.getInstance().getCameraEntity();
        if (contextEntity == null) {
            remove();
            return;
        }
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, contextEntity));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 impact = hit.getLocation().subtract(forward.scale(0.03));
            setPos(impact.x, impact.y, impact.z);
            impactAge = age - 1;
            lifetime = Math.min(lifetime, age + IMPACT_FADE_TICKS);
        } else {
            setPos(end.x, end.y, end.z);
        }
        setBoundingBox(new AABB(new Vec3(x, y, z), new Vec3(x, y, z))
                .inflate(fullSize * SMOKE_SCALE + SMOKE_DEPTH_OFFSET));
        emitEmbers();
    }

    private void emitEmbers() {
        if (!emittedSmoke && smokeEmissionAge >= 0 && (age >= smokeEmissionAge || impactAge >= 0)) {
            emittedSmoke = true;
            Vec3 position = new Vec3(xo, yo, zo).lerp(new Vec3(x, y, z), random.nextDouble());
            Vec3 drift = impactAge >= 0 ? Vec3.ZERO : forward.scale(speed * 0.2);
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.FIRE_BREATH_SMOKE.get(),
                    position.x, position.y, position.z,
                    drift.x, drift.y, drift.z);
        }
        if (!emitsEmber || embersEmitted >= MAX_EMBERS || age < emberEmissionAge + embersEmitted * 2) return;
        embersEmitted++;

        Vec3 movement = new Vec3(x - xo, y - yo, z - zo);
        Vec3 direction = movement.lengthSqr() > 1.0E-8 ? movement.normalize() : forward;
        Vec3 reference = Math.abs(direction.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        double yaw = Math.toRadians((random.nextDouble() - 0.5) * EMBER_SPREAD_X_DEGREES);
        double pitch = Math.toRadians((random.nextDouble() - 0.5) * EMBER_SPREAD_Y_DEGREES);
        Vec3 launch = direction.scale(Math.cos(yaw) * Math.cos(pitch))
                .add(right.scale(Math.sin(yaw) * Math.cos(pitch))).add(up.scale(Math.sin(pitch)));
        Vec3 velocity = launch.scale((1 + random.nextDouble() * 3) / 20.0);
        Vec3 position = new Vec3(xo, yo, zo).lerp(new Vec3(x, y, z), random.nextDouble());
        Minecraft.getInstance().particleEngine.createParticle(ModParticles.FIRE_BREATH_EMBER.get(),
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        Particle glow = Minecraft.getInstance().particleEngine.createParticle(ModParticles.GLOWING_EMITTER.get(),
                position.x, position.y, position.z,
                random.nextGaussian() * 0.16,
                random.nextGaussian() * 0.16,
                random.nextGaussian() * 0.16);
        if (glow != null) {
            glow.setColor(1.0F, 0.55F, 0.12F);
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float renderAge = Math.max(0, age - 1 + partialTicks);
        int frame = (int) (renderAge / FRAME_TICKS) % FRAME_COUNT;
        setSprite(sprites.get(frame, FRAME_COUNT - 1));
        float growth = Mth.clamp(renderAge / GROWTH_TICKS, 0, 1);
        this.quadSize = fullSize * Mth.lerp(growth, 0.45F, 1.0F);
        float fade = Mth.clamp((lifetime - 1 - renderAge) / 3.0F, 0, 1);
        if (impactAge >= 0) {
            fade = Math.min(fade, Mth.clamp(1 - (renderAge - impactAge) / IMPACT_FADE_TICKS, 0, 1));
        }
        float flameSize = quadSize;
        Vec3 position = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y),
                Mth.lerp(partialTicks, zo, z));
        Vec3 behind = position.subtract(camera.getPosition()).normalize().scale(SMOKE_DEPTH_OFFSET);
        double oldX = x, oldY = y, oldZ = z, oldXo = xo, oldYo = yo, oldZo = zo;
        x += behind.x;
        y += behind.y;
        z += behind.z;
        xo += behind.x;
        yo += behind.y;
        zo += behind.z;
        this.quadSize = flameSize * SMOKE_SCALE;
        this.setColor(0.8667F, 0.3137F, 0.0F);
        this.alpha = fade;
        try {
            super.render(buffer, camera, partialTicks);
        } finally {
            x = oldX;
            y = oldY;
            z = oldZ;
            xo = oldXo;
            yo = oldYo;
            zo = oldZo;
            this.quadSize = flameSize;
            this.setColor(1.0F, 0.42F, 0.035F);
            this.alpha = fade;
        }
        super.render(buffer, camera, partialTicks);
        x -= behind.x;
        y -= behind.y;
        z -= behind.z;
        xo -= behind.x;
        yo -= behind.y;
        zo -= behind.z;
        this.quadSize = flameSize * CORE_SCALE;
        this.setColor(1.0F, 0.70F, 0.18F);
        this.alpha = fade;
        try {
            super.render(buffer, camera, partialTicks);
        } finally {
            x = oldX;
            y = oldY;
            z = oldZ;
            xo = oldXo;
            yo = oldYo;
            zo = oldZo;
            this.quadSize = flameSize;
            this.setColor(1.0F, 0.6431F, 0.4157F);
            this.alpha =  fade;
        }
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<FireBreathParticleData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull FireBreathParticleData data, @NotNull ClientLevel level,
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            float amount = (3 + level.random.nextInt(3)) * data.density();
            int count = (int) amount;
            if (level.random.nextFloat() < amount - count) count++;
            if (count == 0) return null;
            Vec3 velocity = new Vec3(xSpeed, ySpeed, zSpeed);
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.FIRE_BREATH_FLICKER.get(),
                    x, y, z, xSpeed, ySpeed, zSpeed);
            emitForwardGlows(level, new Vec3(x, y, z), velocity);
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.FIRE_BREATH_OUTER_FLAME.get(),
                    x, y, z, xSpeed, ySpeed, zSpeed);
            for (int i = 1; i < count; i++) {
                Minecraft.getInstance().particleEngine.add(
                        new FireBreathParticle(level, x, y, z, velocity, data, sprites));
            }
            return new FireBreathParticle(level, x, y, z, velocity, data, sprites);
        }

        private void emitForwardGlows(ClientLevel level, Vec3 origin, Vec3 velocity) {
            Vec3 forward = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : new Vec3(0, 0, 1);
            Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = forward.cross(reference).normalize();
            Vec3 up = right.cross(forward).normalize();
            for (int i = 0; i < 2; i++) {
                double around = level.random.nextDouble() * Math.PI * 2;
                double spreadAngle = Math.toRadians(10 + level.random.nextDouble() * 15);
                Vec3 outward = right.scale(Math.cos(around)).add(up.scale(Math.sin(around)));
                double launchSpeed = Mth.clamp(velocity.length(), 0.5, 12)
                        * (0.85 + level.random.nextDouble() * 0.15);
                Vec3 launch = forward.scale(Math.cos(spreadAngle))
                        .add(outward.scale(Math.sin(spreadAngle))).scale(launchSpeed);
                Particle glow = Minecraft.getInstance().particleEngine.createParticle(
                        ModParticles.GLOWING_EMITTER.get(), origin.x, origin.y, origin.z,
                        launch.x, launch.y, launch.z);
                if (glow != null) {
                    glow.setColor(1.0F, 0.55F, 0.12F);
                    if (glow instanceof GlowingEmitterParticle spark) spark.enableTerrainCollision();
                }
            }
        }
    }
}
