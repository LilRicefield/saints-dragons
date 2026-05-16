package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IgnivorusNovaEntity extends Entity {

    private static final int DURATION = 20;
    private static final double MAX_RADIUS = 48.0;

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(IgnivorusNovaEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUUID;
    private Entity owner;
    private int age;
    private final Set<UUID> damagedEntities = new HashSet<>();

    public IgnivorusNovaEntity(EntityType<? extends IgnivorusNovaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public IgnivorusNovaEntity(Level level, Vec3 position, Entity owner, float damage) {
        this(ModEntities.IGNIVORUS_NOVA.get(), level);
        setPos(position);
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.owner = owner;
        setDamage(damage);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DAMAGE, 100.0F);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public int getAge() {
        return this.age;
    }

    public int getDuration() {
        return DURATION;
    }

    public float getScale(float partialTicks) {
        float ageFrac = (this.age + partialTicks) / (float) DURATION;
        return ageFrac * 5.0F;
    }

    public float getOpacity(float partialTicks) {
        float ageFrac = (this.age + partialTicks) / (float) DURATION;
        return (float) Math.max((1.0 - ageFrac * ageFrac) * 0.8, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (!this.level().isClientSide) {
            if (this.age >= DURATION) {
                this.discard();
                return;
            }

            applyDamage();
        }
    }

    private void applyDamage() {
        float ageFrac = this.age / (float) DURATION;
        double currentRadius = ageFrac * MAX_RADIUS;

        AABB searchBox = this.getBoundingBox().inflate(currentRadius);
        List<LivingEntity> targets = level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive() && entity.attackable()
        );

        Entity owner = getOwner();
        DamageSource damageSource;
        if (owner instanceof LivingEntity livingOwner) {
            damageSource = level().damageSources().mobAttack(livingOwner);
        } else {
            damageSource = level().damageSources().generic();
        }

        for (LivingEntity target : targets) {
            if (target.getUUID().equals(ownerUUID)) continue;
            if (owner != null && owner.getPassengers().contains(target)) continue;
            if (damagedEntities.contains(target.getUUID())) continue;
            if (target instanceof Ignivorus baby && baby.isBaby()) continue;
            if (owner instanceof Ignivorus ignivorus && ignivorus.isAlly(target)) continue;
            if (DragonElementalImmunity.isFireImmune(target)) continue;

            double distanceSqr = target.position().distanceToSqr(position());
            double radiusSqr = currentRadius * currentRadius;

            if (distanceSqr <= radiusSqr) {
                target.hurt(damageSource, getDamage());
                target.setSecondsOnFire(8);

                Vec3 knockback = target.position().subtract(position()).normalize().scale(1.2);
                target.push(knockback.x, 0.4, knockback.z);

                damagedEntities.add(target.getUUID());
            }
        }
    }

    private Entity getOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity resolved = serverLevel.getEntity(ownerUUID);
            if (resolved != null) {
                owner = resolved;
            }
        }
        return owner;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", this.age);
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
        return distance < 16384.0;
    }
}
