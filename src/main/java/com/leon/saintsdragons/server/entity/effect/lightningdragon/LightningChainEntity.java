package com.leon.saintsdragons.server.entity.effect.lightningdragon;

import com.leon.saintsdragons.common.particle.lightningdragon.LightningArcData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningChainData;
import com.leon.saintsdragons.common.registry.ModEntities;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Entity that manages chain lightning effects, spawning particles and dealing damage
 * over time rather than using individual particles for each segment.
 */
public class LightningChainEntity extends Entity {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LightningChainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(LightningChainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFESPAN = SynchedEntityData.defineId(LightningChainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(LightningChainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_CHAIN = SynchedEntityData.defineId(LightningChainEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    // Chain lightning specific data
    private Vec3 startPos;
    private Vec3 endPos;
    private int chainCount = 0;
    private int maxChains = 3;

    public LightningChainEntity(EntityType<? extends LightningChainEntity> entityType, Level level) {
        super(entityType, level);
    }

    public LightningChainEntity(Level level, double x, double y, double z, float damage, float size, 
                               LivingEntity caster, boolean isChain) {
        this(ModEntities.LIGHTNING_CHAIN.get(), level);
        this.setPos(x, y, z);
        this.setDamage(damage);
        this.setSize(size);
        this.setCaster(caster);
        this.setIsChain(isChain);
        this.setDelay(0);
        this.setLifespan(0);
    }

    public LightningChainEntity(Level level, Vec3 startPos, Vec3 endPos, float damage, float size, 
                               LivingEntity caster, boolean isChain) {
        this(level, startPos.x, startPos.y, startPos.z, damage, size, caster, isChain);
        this.startPos = startPos;
        this.endPos = endPos;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 0F);
        this.entityData.define(SIZE, 1.0F);
        this.entityData.define(LIFESPAN, 0);
        this.entityData.define(DELAY, 0);
        this.entityData.define(IS_CHAIN, false);
    }

    public int getLifespan() {
        return this.entityData.get(LIFESPAN);
    }

    public void setLifespan(int lifespan) {
        this.entityData.set(LIFESPAN, lifespan);
    }

    public int getDelay() {
        return this.entityData.get(DELAY);
    }

    public void setDelay(int delay) {
        this.entityData.set(DELAY, delay);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float size) {
        if (!this.level().isClientSide) {
            this.entityData.set(SIZE, Mth.clamp(size, 0.5F, 3.0F));
        }
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
    }

    public boolean getIsChain() {
        return entityData.get(IS_CHAIN);
    }

    public void setIsChain(boolean isChain) {
        entityData.set(IS_CHAIN, isChain);
    }

    public void setCaster(@Nullable LivingEntity caster) {
        this.owner = caster;
        this.ownerUUID = caster == null ? null : caster.getUUID();
    }

    @Nullable
    public LivingEntity getCaster() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = 64.0F * getViewScale();
        return distance < d0 * d0;
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@Nonnull Pose pose) {
        return EntityDimensions.scalable(this.getSize() * 0.5F, this.getSize());
    }

    @Override
    public void tick() {
        super.tick();
        
        setLifespan(this.getLifespan() + 1);
        int adjustedLifespan = this.getLifespan() - this.getDelay();

        if (this.level().isClientSide) {
            // Client-side particle effects
            if (adjustedLifespan == 1) {
                spawnLightningParticles();
            }
        } else {
            // Server-side damage and chain logic
            if (adjustedLifespan == 5) {
                dealDamage();
                if (getIsChain() && chainCount < maxChains) {
                    attemptChainLightning();
                }
            }
            
            if (adjustedLifespan > 20) {
                this.discard();
            }
        }
    }

    private void spawnLightningParticles() {
        if (startPos != null && endPos != null) {
            // Spawn animated lightning arc that traces the path
            if (getIsChain()) {
                // Animated chain lightning arc
                this.level().addAlwaysVisibleParticle(
                    new LightningChainData(getSize(), startPos, endPos),
                    startPos.x, startPos.y, startPos.z,
                    0, 0, 0
                );
            } else {
                // Static lightning storm for single strikes
                Vec3 delta = endPos.subtract(startPos);
                int segments = Math.max(3, (int)(delta.length() * 4));
                Vec3 step = delta.scale(1.0 / segments);
                
                for (int i = 0; i <= segments; i++) {
                    Vec3 pos = startPos.add(step.scale(i));
                    Vec3 dir = step.normalize();
                    
                    this.level().addAlwaysVisibleParticle(
                        new LightningStormData(getSize()),
                        pos.x, pos.y, pos.z,
                        dir.x, dir.y, dir.z
                    );
                }
            }
        } else {
            // Single point lightning
            if (getIsChain()) {
                this.level().addAlwaysVisibleParticle(
                    new LightningArcData(getSize()),
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
                );
            } else {
                this.level().addAlwaysVisibleParticle(
                    new LightningStormData(getSize()),
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
                );
            }
        }
    }

    private void dealDamage() {
        AABB damageBox = this.getBoundingBox().inflate(getSize() * 2.0);
        
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, damageBox)) {
            if (entity.isAlive() && !entity.isInvulnerable() && entity != getCaster()) {
                LivingEntity caster = getCaster();
                if (caster == null || !caster.isAlliedTo(entity)) {
                    DamageSource damageSource = caster != null ? 
                        this.damageSources().mobAttack(caster) : 
                        this.damageSources().magic();
                    
                    entity.hurt(damageSource, getDamage());
                    
                    // Spawn impact effects at the target location
                    spawnImpactEffects(entity.position().add(0, entity.getBbHeight() / 2, 0));
                }
            }
        }
    }

    private void spawnImpactEffects(Vec3 impactPos) {
        if (!(this.level() instanceof ServerLevel server)) return;
        
        // Spawn layered lightning arc impact effects for dramatic visual
        float size = getSize() * 0.6f; // Reduced from 1.2f
        
        // Spawn multiple layered particles for impact effect
        for (int layer = 0; layer < 4; layer++) {
            float layerSize = size * (1.0f + layer * 0.25f);
            float layerOffset = layer * 0.1f; // Reduced from 0.15f
            
            // Spawn particles in a small radius around the impact point
            for (int i = 0; i < 6; i++) {
                double angle = (i * Math.PI * 2) / 6.0;
                double offsetX = Math.cos(angle) * layerOffset;
                double offsetZ = Math.sin(angle) * layerOffset;
                
                server.sendParticles(new LightningArcData(layerSize),
                        impactPos.x + offsetX, impactPos.y + layerOffset, impactPos.z + offsetZ,
                        1, 0, 0, 0, 0.0);
            }
        }
    }

    private void spawnChainImpactEffects(Vec3 chainPos) {
        if (!(this.level() instanceof ServerLevel server)) return;
        
        // Spawn chain-specific impact effects (smaller, more focused)
        float size = getSize() * 0.4f; // Reduced from 0.8f
        
        // Spawn fewer layers for chain impacts (more focused effect)
        for (int layer = 0; layer < 3; layer++) {
            float layerSize = size * (1.0f + layer * 0.2f);
            float layerOffset = layer * 0.05f; // Reduced from 0.1f
            
            // Spawn particles in a tighter radius for chain effects
            for (int i = 0; i < 4; i++) {
                double angle = (i * Math.PI * 2) / 4.0;
                double offsetX = Math.cos(angle) * layerOffset;
                double offsetZ = Math.sin(angle) * layerOffset;
                
                server.sendParticles(new LightningArcData(layerSize),
                        chainPos.x + offsetX, chainPos.y + layerOffset, chainPos.z + offsetZ,
                        1, 0, 0, 0, 0.0);
            }
        }
    }

    private void attemptChainLightning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        AABB searchBox = this.getBoundingBox().inflate(getSize() * 4.0);
        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (entity.isAlive() && !entity.isInvulnerable() && entity != getCaster()) {
                LivingEntity caster = getCaster();
                if (caster == null || !caster.isAlliedTo(entity)) {
                    double distance = this.distanceTo(entity);
                    if (distance < nearestDistance) {
                        nearestTarget = entity;
                        nearestDistance = distance;
                    }
                }
            }
        }
        
        if (nearestTarget != null) {
            // Create chain lightning to the nearest target
            Vec3 chainStart = this.position();
            Vec3 chainEnd = nearestTarget.position().add(0, nearestTarget.getBbHeight() / 2, 0);
            
            // Spawn impact effect at the current position (where chain originates)
            spawnChainImpactEffects(chainStart);
            
            LightningChainEntity chainEntity = new LightningChainEntity(
                serverLevel, chainStart, chainEnd, 
                getDamage() * 0.8f, getSize() * 0.9f, 
                getCaster(), true
            );
            chainEntity.chainCount = this.chainCount + 1;
            chainEntity.maxChains = this.maxChains;
            
            serverLevel.addFreshEntity(chainEntity);
        }
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag compound) {
        compound.putInt("lifespan", this.getLifespan());
        compound.putInt("delay", this.getDelay());
        compound.putFloat("damage", this.getDamage());
        compound.putFloat("size", this.getSize());
        compound.putBoolean("isChain", this.getIsChain());
        compound.putInt("chainCount", this.chainCount);
        compound.putInt("maxChains", this.maxChains);
        
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }
        
        if (startPos != null) {
            compound.putDouble("startX", startPos.x);
            compound.putDouble("startY", startPos.y);
            compound.putDouble("startZ", startPos.z);
        }
        
        if (endPos != null) {
            compound.putDouble("endX", endPos.x);
            compound.putDouble("endY", endPos.y);
            compound.putDouble("endZ", endPos.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag compound) {
        this.setLifespan(compound.getInt("lifespan"));
        this.setDelay(compound.getInt("delay"));
        this.setDamage(compound.getFloat("damage"));
        this.setSize(compound.getFloat("size"));
        this.setIsChain(compound.getBoolean("isChain"));
        this.chainCount = compound.getInt("chainCount");
        this.maxChains = compound.getInt("maxChains");
        
        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
        }
        
        if (compound.contains("startX")) {
            this.startPos = new Vec3(
                compound.getDouble("startX"),
                compound.getDouble("startY"),
                compound.getDouble("startZ")
            );
        }
        
        if (compound.contains("endX")) {
            this.endPos = new Vec3(
                compound.getDouble("endX"),
                compound.getDouble("endY"),
                compound.getDouble("endZ")
            );
        }
    }
}
