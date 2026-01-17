package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Visual-only slash sprite for Ignivorus ultimate effects.
 */
public class IgnivorusFireSlashEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IgnivorusFireSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(IgnivorusFireSlashEntity.class, EntityDataSerializers.INT);

    private int age;
    private int maxAge;

    public IgnivorusFireSlashEntity(EntityType<? extends IgnivorusFireSlashEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.maxAge = 20;
    }

    public IgnivorusFireSlashEntity(Level level, Vec3 position, Vec3 velocity, float scale, int lifetime) {
        this(ModEntities.IGNIVORUS_FIRE_SLASH.get(), level);
        setPos(position);
        setDeltaMovement(velocity);
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
        this.age++;

        if (!this.level().isClientSide && this.age >= this.maxAge) {
            this.discard();
            return;
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.scale(0.96));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.maxAge = tag.getInt("MaxAge");
        if (tag.contains("Scale")) {
            setScale(tag.getFloat("Scale"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("MaxAge", this.maxAge);
        tag.putFloat("Scale", getScale());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
