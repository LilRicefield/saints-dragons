package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
    private LivingEntity owner;
    private float damage;
    private int age;
    private int maxAge;
    private Vec3 spawnPos;
    private boolean hasHitEntity = false; // Track if this flame already hit something
    private boolean hasHitBlock = false; // Track if this flame has impacted the ground
    private double igniteBlockChance = 1.0D;

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
        this.owner = owner instanceof LivingEntity livingOwner ? livingOwner : null;
        this.damage = damage;
        this.maxAge = lifetime;
        setScale(scale);
        setLifetime(lifetime);
        var config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        this.igniteBlockChance = config.extraDouble("fire_breath_ignite_block_chance", 1.0D);
        if (this.igniteBlockChance < 0.0D) {
            this.igniteBlockChance = 0.0D;
        }
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

            // Check for entity hits first (bullet-style collision)
            if (!hasHitEntity && checkEntityCollision()) {
                this.discard(); // Disappear after hitting an entity (like a bullet)
                return;
            }

            // Then check for block hits
            if (!hasHitBlock) {
                Vec3 start = position();
                Vec3 end = start.add(getDeltaMovement());
                HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    Vec3 impactPoint = blockHit.getLocation();
                    if (level() instanceof ServerLevel serverLevel) {
                        double travelDistance = spawnPos.distanceTo(impactPoint);
                        double impactRadius = Mth.clamp(1.2 + travelDistance * 0.16, 1.2, 3.2);
                        if (igniteBlockChance > 0.0D && level().random.nextDouble() <= igniteBlockChance) {
                            DragonDestructionManager.applyFlameImpact(serverLevel, impactPoint, impactRadius);
                        }
                    }
                    hasHitBlock = true;
                    setPos(impactPoint);
                    setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        // Move forward
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        // Minimal slowdown to maintain speed
        setDeltaMovement(motion.scale(0.995));
    }

    /**
     * Bullet-style collision detection - checks for entity hits along movement path.
     * Returns true if an entity was hit (flame should be destroyed).
     */
    private boolean checkEntityCollision() {
        // Raycast along the movement path to detect hits at close range
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);

        float radius = 1.5F * getScale(); // Wider detection for reliable hits
        AABB searchBox = new AABB(start, end).inflate(radius);
        List<LivingEntity> potentialTargets = level().getEntitiesOfClass(LivingEntity.class, searchBox);

        LivingEntity owner = getOwner();
        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        // Find the closest valid target along the ray
        for (LivingEntity target : potentialTargets) {
            // Skip owner and owner's passengers
            if (ownerUUID != null && target.getUUID().equals(ownerUUID)) continue;
            if (owner != null && owner.getPassengers().contains(target)) continue;

            // Don't damage baby Ignivorus dragons (protect the young!)
            if (target instanceof Ignivorus baby && baby.isBaby()) continue;

            // Don't damage allies or tamed pets
            if (owner instanceof Ignivorus ignivorus && ignivorus.isAlly(target)) continue;

            // Check if target is within hit radius
            double distance = target.distanceToSqr(this);
            if (distance <= radius * radius && distance < closestDistance) {
                closestTarget = target;
                closestDistance = distance;
            }
        }

        // If we found a target, deal damage and return true
        if (closestTarget != null) {
            hasHitEntity = true;

            // Always use mobAttack damage source - this bypasses Fire Resistance
            DamageSource damageSource = owner != null
                    ? level().damageSources().mobAttack(owner)
                    : level().damageSources().generic();

            // Deal impact damage (works through Fire Resistance)
            closestTarget.hurt(damageSource, damage);

            // Only set on fire if target doesn't have Fire Resistance
            if (!closestTarget.fireImmune()) {
                closestTarget.setSecondsOnFire(3);
            }

            return true; // Signal that we hit something
        }

        return false; // No hit
    }

    private LivingEntity getOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(ownerUUID);
            if (entity instanceof LivingEntity livingEntity) {
                owner = livingEntity;
            }
        }
        return owner;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.maxAge = tag.getInt("MaxAge");
        this.damage = tag.getFloat("Damage");
        this.hasHitEntity = tag.getBoolean("HasHitEntity");
        this.hasHitBlock = tag.getBoolean("HasHitBlock");
        this.igniteBlockChance = tag.contains("IgniteBlockChance") ? tag.getDouble("IgniteBlockChance") : 1.0D;
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
        tag.putBoolean("HasHitEntity", this.hasHitEntity);
        tag.putBoolean("HasHitBlock", this.hasHitBlock);
        tag.putDouble("IgniteBlockChance", this.igniteBlockChance);
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
        return distance < 65536.0;
    }
}
