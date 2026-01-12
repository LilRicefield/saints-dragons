package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Individual flame projectile for Ignivorus flamethrower effect.
 * Spawned continuously during fire breath to create a stream of flames.
 */
public class IgnivorusFlameEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IgnivorusFlameEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(IgnivorusFlameEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private float damage;
    private int age;
    private int maxAge;
    private Vec3 spawnPos;

    public IgnivorusFlameEntity(EntityType<? extends IgnivorusFlameEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.maxAge = 20; // Default 1 second lifetime
    }

    public IgnivorusFlameEntity(Level level, Vec3 position, Vec3 velocity, Entity owner, float damage, float scale, int lifetime) {
        this(ModEntities.IGNIVORUS_FLAME.get(), level);
        setPos(position);
        setDeltaMovement(velocity);
        this.spawnPos = position;
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.damage = damage;
        this.maxAge = lifetime;
        setScale(scale);
        setLifetime(lifetime);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_LIFETIME, 20);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
        this.maxAge = lifetime;
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public int getAge() {
        return this.age;
    }

    @Override
    public void tick() {
        super.tick();

        // Increment age on BOTH client and server for animation
        this.age++;

        if (!this.level().isClientSide) {
            if (spawnPos == null) {
                spawnPos = position();
            }

            // Remove when lifetime expires
            if (this.age >= this.maxAge) {
                this.discard();
                return;
            }

            // Ignite blocks when the flame hits solid terrain
            Vec3 start = position();
            Vec3 end = start.add(getDeltaMovement());
            HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                Vec3 impactPoint = blockHit.getLocation();
                if (level() instanceof ServerLevel serverLevel) {
                    double travelDistance = spawnPos.distanceTo(impactPoint);
                    double impactRadius = Mth.clamp(1.2 + travelDistance * 0.16, 1.2, 3.2);
                    DragonDestructionManager.applyFlameImpact(serverLevel, impactPoint, impactRadius);
                }
                this.discard();
                return;
            }

            // Damage entities in path
            damageNearbyEntities();
        }

        // Move forward
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        // Minimal slowdown to maintain speed
        setDeltaMovement(motion.scale(0.995));
    }

    private void damageNearbyEntities() {
        float radius = 0.5F * getScale();
        AABB box = getBoundingBox().inflate(radius);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, box);

        Entity owner = getOwner();

        for (LivingEntity target : entities) {
            // Skip owner and owner's passengers
            if (target.getUUID().equals(ownerUUID)) continue;
            if (owner != null && owner.getPassengers().contains(target)) continue;

            // Check distance
            if (target.distanceToSqr(this) <= radius * radius) {
                DamageSource damageSource = level().damageSources().inFire();
                if (owner != null) {
                    damageSource = level().damageSources().mobAttack((LivingEntity) owner);
                }

                target.hurt(damageSource, damage);
                target.setSecondsOnFire(3);
            }
        }
    }

    private Entity getOwner() {
        if (ownerUUID != null && level() != null) {
            for (Entity entity : level().getEntities(null, AABB.ofSize(position(), 100, 100, 100))) {
                if (entity.getUUID().equals(ownerUUID)) {
                    return entity;
                }
            }
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.maxAge = tag.getInt("MaxAge");
        this.damage = tag.getFloat("Damage");
        if (tag.contains("SpawnX")) {
            this.spawnPos = new Vec3(tag.getDouble("SpawnX"), tag.getDouble("SpawnY"), tag.getDouble("SpawnZ"));
        }
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("MaxAge", this.maxAge);
        tag.putFloat("Damage", this.damage);
        if (this.spawnPos != null) {
            tag.putDouble("SpawnX", this.spawnPos.x);
            tag.putDouble("SpawnY", this.spawnPos.y);
            tag.putDouble("SpawnZ", this.spawnPos.z);
        }
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Render from unlimited distance
        return true;
    }
}
