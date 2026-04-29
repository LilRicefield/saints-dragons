package com.leon.saintsdragons.server.entity.effect.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class VolitansPoisonBallEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(VolitansPoisonBallEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUUID;
    private LivingEntity owner;
    private double impactRadius;
    private float impactDamage;
    private int poisonDurationTicks;
    private int poisonAmplifier;
    private int lifetimeTicks;
    private int livedTicks;

    public VolitansPoisonBallEntity(EntityType<? extends VolitansPoisonBallEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.noPhysics = true;
        this.refreshDimensions();
    }

    public VolitansPoisonBallEntity(Level level, Vec3 pos, @Nullable LivingEntity owner,
                                    double impactRadius, float impactDamage,
                                    int poisonDurationTicks, int poisonAmplifier, int lifetimeTicks) {
        this(ModEntities.VOLITANS_POISON_BALL.get(), level);
        this.setPos(pos);
        this.owner = owner;
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.impactRadius = impactRadius;
        this.impactDamage = impactDamage;
        this.poisonDurationTicks = poisonDurationTicks;
        this.poisonAmplifier = poisonAmplifier;
        this.lifetimeTicks = lifetimeTicks;
        this.setDeltaMovement(Vec3.ZERO);
        this.setVisualScale(1.0F);
        this.hasImpulse = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public void setVisualScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
        this.refreshDimensions();
    }

    public float getVisualScale() {
        return this.entityData.get(DATA_SCALE);
    }

    @Override
    public void tick() {
        livedTicks++;

        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();

        if (!this.isNoGravity()) {
            float gravityProgress = Math.min(1.0F, livedTicks / 20.0F);
            double gravity = -0.05D * gravityProgress;
            motion = motion.add(0.0D, gravity, 0.0D);
        }

        Vec3 nextPos = currentPos.add(motion);

        net.minecraft.world.phys.BlockHitResult blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                currentPos,
                nextPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));

        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        EntityHitResult entityHit = findEntityHit(currentPos, nextPos);
        boolean hitEntity = entityHit != null;

        if (hitEntity && hitBlock) {
            double blockDist = blockHit.getLocation().distanceToSqr(currentPos);
            double entityDist = entityHit.getLocation().distanceToSqr(currentPos);
            this.setPos(entityDist <= blockDist ? entityHit.getLocation() : blockHit.getLocation());
        } else if (hitEntity) {
            this.setPos(entityHit.getLocation());
        } else if (hitBlock) {
            this.setPos(blockHit.getLocation());
        } else {
            this.setPos(nextPos);
        }

        if (!level().isClientSide) {
            if (!this.isNoGravity()) {
                motion = motion.scale(0.99D);
            }
            this.setDeltaMovement(motion);

            if (livedTicks > lifetimeTicks || hitEntity || hitBlock) {
                explode();
            }
        } else {
            if (!this.isNoGravity()) {
                motion = motion.scale(0.99D);
            }
            this.setDeltaMovement(motion);
            spawnTrailParticles();
        }
    }

    @Nullable
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        if (level().isClientSide) {
            return null;
        }
        AABB bounds = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity resolvedOwner = getOwner();
        return ProjectileUtil.getEntityHitResult(level(), this, start, end, bounds, target -> {
            if (!(target instanceof LivingEntity living)) {
                return false;
            }
            if (!living.isAlive()) {
                return false;
            }
            if (resolvedOwner != null && (target == resolvedOwner || isAlliedTarget(resolvedOwner, living))) {
                return false;
            }
            return true;
        });
    }

    private void spawnTrailParticles() {
        float scale = getVisualScale();
        level().addParticle(ParticleTypes.ENTITY_EFFECT, getX(), getY() + 0.2D * scale, getZ(), 0.25D, 0.75D, 0.2D);
        level().addParticle(ParticleTypes.WITCH, getX(), getY() + 0.1D * scale, getZ(), 0.0D, 0.01D, 0.0D);
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        float scale = getVisualScale();

        int effectCount = Math.min(80, Math.max(8, (int) (14 * scale)));
        server.sendParticles(ParticleTypes.ENTITY_EFFECT, impact.x, impact.y + 0.35D * scale, impact.z,
                effectCount, 0.7D * scale, 0.45D * scale, 0.7D * scale, 0.0D);
        server.sendParticles(ParticleTypes.WITCH, impact.x, impact.y + 0.35D * scale, impact.z,
                effectCount / 2, 0.65D * scale, 0.4D * scale, 0.65D * scale, 0.03D);
        server.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, getSoundSource(), 1.0F + scale * 0.08F, 0.8F);

        LivingEntity ownerEntity = getOwner();
        AABB area = new AABB(
                impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius
        );

        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, area, target ->
                target.isAlive()
                        && target != ownerEntity
                        && (ownerEntity == null || !isAlliedTarget(ownerEntity, target))
        );

        for (LivingEntity target : hits) {
            if (ownerEntity != null) {
                target.hurt(server.damageSources().mobAttack(ownerEntity), impactDamage);
            } else {
                target.hurt(server.damageSources().magic(), impactDamage);
            }
            if (poisonDurationTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, poisonAmplifier));
            }

            Vec3 knockback = target.position().subtract(impact);
            if (knockback.lengthSqr() > 1.0E-6) {
                knockback = knockback.normalize().scale(0.18D * scale);
                target.push(knockback.x, 0.05D, knockback.z);
                target.hasImpulse = true;
            }
        }

        discard();
    }

    private boolean isAlliedTarget(LivingEntity ownerEntity, LivingEntity target) {
        if (ownerEntity == null || target == null) {
            return false;
        }
        if (ownerEntity.isAlliedTo(target)) {
            return true;
        }
        if (ownerEntity instanceof DragonEntity dragon) {
            return dragon.isAlly(target);
        }
        return false;
    }

    @Nullable
    private LivingEntity getOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel server) {
            Entity entity = server.getEntity(ownerUUID);
            if (entity instanceof LivingEntity living) {
                owner = living;
            }
        }
        return owner;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        livedTicks = tag.getInt("Lived");
        lifetimeTicks = tag.getInt("Lifetime");
        impactRadius = tag.getDouble("ImpactRadius");
        impactDamage = tag.getFloat("ImpactDamage");
        poisonDurationTicks = tag.getInt("PoisonDuration");
        poisonAmplifier = tag.getInt("PoisonAmplifier");
        if (tag.contains("Scale")) {
            setVisualScale(tag.getFloat("Scale"));
        }
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Lived", livedTicks);
        tag.putInt("Lifetime", lifetimeTicks);
        tag.putDouble("ImpactRadius", impactRadius);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.putInt("PoisonDuration", poisonDurationTicks);
        tag.putInt("PoisonAmplifier", poisonAmplifier);
        tag.putFloat("Scale", getVisualScale());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public float getEyeHeight(@NotNull Pose pose) {
        return 0.5F * getVisualScale();
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        float scale = getVisualScale();
        return EntityDimensions.fixed(0.98F * scale, 0.98F * scale);
    }
}