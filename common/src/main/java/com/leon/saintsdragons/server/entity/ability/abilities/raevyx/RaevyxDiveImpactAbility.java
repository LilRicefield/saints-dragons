package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.particle.raevyx.SonicRingData;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import com.leon.saintsdragons.server.entity.effect.GroundCrackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RaevyxDiveImpactAbility {
    private static final double MIN_IMPACT_SPEED = 1.45D;
    private static final int MIN_DIVE_TICKS = 8;
    private static final double MIN_DIVE_DROP = 12.0D;
    private static final double FULL_POWER_SPEED = 3.25D;
    private static final float BASE_DAMAGE = 8.0F;
    private static final float DAMAGE_PER_EXCESS_SPEED = 20.0F;
    private static final float MAX_DAMAGE = 40.0F;
    private static final double MIN_RADIUS = 7.0D;
    private static final double MAX_RADIUS = 12.0D;
    private static final double MIN_KNOCKBACK = 0.8D;
    private static final double MAX_KNOCKBACK = 2.0D;
    private static final int SONIC_RING_INTERVAL_TICKS = 3;

    private final Raevyx dragon;
    private double peakDiveSpeed;
    private boolean armed;
    private boolean wasAirborne;
    private int diveTicks;
    private double diveDrop;
    private int sonicRingCooldown;
    private Vec3 lastPosition;
    private Vec3 observedMovement = Vec3.ZERO;

    public RaevyxDiveImpactAbility(Raevyx dragon) {
        this.dragon = dragon;
    }

    public void tickServer() {
        if (dragon.level().isClientSide) {
            return;
        }

        Vec3 currentPosition = dragon.position();
        observedMovement = lastPosition != null ? currentPosition.subtract(lastPosition) : Vec3.ZERO;
        lastPosition = currentPosition;

        if (!dragon.isAlive() || dragon.isInWaterOrBubble()) {
            clear();
            return;
        }

        if (dragon.onGround()) {
            if (wasAirborne && armed) {
                applyImpact(peakDiveSpeed);
            }
            clear();
            return;
        }

        Player rider = dragon.getControllingPassenger() instanceof Player player ? player : null;
        boolean ownerRiding = rider != null && dragon.isTame() && dragon.isOwnedBy(rider);
        if (!ownerRiding) {
            return;
        }

        wasAirborne = true;
        if (!dragon.isFlying() || !dragon.isRiderDiving() || observedMovement.y >= 0.0D) {
            clearDiveCharge();
            return;
        }

        double speed = observedMovement.length();
        diveTicks++;
        diveDrop += Math.max(0.0D, -observedMovement.y);
        peakDiveSpeed = Math.max(peakDiveSpeed, speed);
        armed = peakDiveSpeed >= MIN_IMPACT_SPEED
                && diveTicks >= MIN_DIVE_TICKS
                && diveDrop >= MIN_DIVE_DROP;
        if (armed) {
            tickSonicRingCue();
        }
    }

    private void tickSonicRingCue() {
        if (sonicRingCooldown > 0) {
            sonicRingCooldown--;
            return;
        }
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 direction = observedMovement.lengthSqr() > 1.0E-4D ? observedMovement.normalize() : dragon.getLookAngle();
        Vec3 origin = dragon.position()
                .add(0.0D, dragon.getBbHeight() * 0.52D, 0.0D)
                .add(direction.scale(Math.max(1.6D, dragon.getBbWidth() * 0.6D)));
        float yaw = (float) Math.atan2(direction.x, direction.z);
        float pitch = (float) Math.asin(-direction.y);
        float scale = 5.0F + dragon.getRandom().nextFloat() * 1.1F;
        server.sendParticles(
                new SonicRingData(yaw, pitch, scale, 16),
                origin.x, origin.y, origin.z,
                1,
                0.0D, 0.0D, 0.0D,
                0.0D
        );
        sonicRingCooldown = SONIC_RING_INTERVAL_TICKS;
    }

    private void applyImpact(double impactSpeed) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        double power = Mth.clamp(
                (impactSpeed - MIN_IMPACT_SPEED) / (FULL_POWER_SPEED - MIN_IMPACT_SPEED),
                0.0D,
                1.0D
        );
        float damage = Math.min(MAX_DAMAGE,
                BASE_DAMAGE + (float) ((impactSpeed - MIN_IMPACT_SPEED) * DAMAGE_PER_EXCESS_SPEED));
        double radius = Mth.lerp(power, MIN_RADIUS, MAX_RADIUS);
        double knockback = Mth.lerp(power, MIN_KNOCKBACK, MAX_KNOCKBACK);
        Vec3 origin = impactOrigin();
        Player rider = dragon.getControllingPassenger() instanceof Player player ? player : null;
        AABB area = dragon.getBoundingBox().inflate(radius, Math.max(3.0D, radius * 0.6D), radius);
        List<LivingEntity> impactTargets = new ArrayList<>();

        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity != dragon
                        && entity != rider
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && horizontalDistanceToSqr(entity.getBoundingBox(), origin) <= radius * radius)) {
            target.hurt(server.damageSources().mobAttack(dragon), damage);
            impactTargets.add(target);
            Vec3 outward = target.position().subtract(origin).multiply(1.0D, 0.0D, 1.0D);
            if (outward.lengthSqr() < 1.0E-4D) {
                outward = new Vec3(0.0D, 0.0D, 1.0D);
            }
            outward = outward.normalize().scale(knockback);
            target.push(outward.x, 0.3D + power * 0.35D, outward.z);
            target.hasImpulse = true;
        }

        RaevyxChainLightningAbility.chainFromImpact(dragon, origin, impactTargets, power);
        spawnImpactVisuals(server, origin, power);
        server.playSound(null, dragon.blockPosition(), ModSounds.RAEVYX_DIVE_IMPACT.get(),
                dragon.getSoundSource(), 2.0F, 0.9F + dragon.getRandom().nextFloat() * 0.15F);
        dragon.triggerScreenShake((float) Mth.lerp(power, 0.7D, 1.2D), 10 + Mth.floor(power * 6.0D));
    }

    private Vec3 impactOrigin() {
        AABB box = dragon.getBoundingBox();
        return new Vec3((box.minX + box.maxX) * 0.5D, box.minY, (box.minZ + box.maxZ) * 0.5D);
    }

    private static double horizontalDistanceToSqr(AABB box, Vec3 origin) {
        double dx = Math.max(Math.max(box.minX - origin.x, 0.0D), origin.x - box.maxX);
        double dz = Math.max(Math.max(box.minZ - origin.z, 0.0D), origin.z - box.maxZ);
        return dx * dx + dz * dz;
    }

    private void spawnImpactVisuals(ServerLevel server, Vec3 origin, double power) {
        float ringScale = (float) Mth.lerp(power, 0.8D, 1.35D);
        ImpactRingEntity ring = new ImpactRingEntity(server, origin.add(0.0D, 0.08D, 0.0D), ringScale);
        server.addFreshEntity(ring);

        double crackY = dragon.getBoundingBox().minY + 0.02D;
        server.addFreshEntity(new GroundCrackEntity(server,
                new Vec3(origin.x, crackY, origin.z), dragon.getYRot()));

        spawnGroundDust(server, origin, power);
    }

    private void spawnGroundDust(ServerLevel server, Vec3 origin, double power) {
        BlockPos groundPos = BlockPos.containing(origin.x, dragon.getBoundingBox().minY - 0.1D, origin.z);
        BlockState ground = server.getBlockState(groundPos);
        if (ground.isAir() || ground.liquid()) {
            groundPos = groundPos.below();
            ground = server.getBlockState(groundPos);
        }
        if (!ground.isAir() && !ground.liquid()) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    origin.x, groundPos.getY() + 1.05D, origin.z,
                    35 + Mth.floor(power * 35.0D),
                    2.0D + power * 2.0D, 0.25D, 2.0D + power * 2.0D,
                    0.22D + power * 0.15D);
        }

        int dustCount = 18 + Mth.floor(power * 18.0D);
        for (int i = 0; i < dustCount; i++) {
            double angle = (Math.PI * 2.0D * i) / dustCount;
            double speed = 0.18D + power * 0.22D;
            server.sendParticles(ModParticles.DRAGON_DUST.get(),
                    origin.x, dragon.getBoundingBox().minY + 0.08D, origin.z,
                    0, Math.cos(angle) * speed, 0.08D + power * 0.08D,
                    Math.sin(angle) * speed, 1.0D);
        }
    }

    private void clearDiveCharge() {
        peakDiveSpeed = 0.0D;
        armed = false;
        diveTicks = 0;
        diveDrop = 0.0D;
        sonicRingCooldown = 0;
    }

    private void clear() {
        clearDiveCharge();
        wasAirborne = false;
    }
}
