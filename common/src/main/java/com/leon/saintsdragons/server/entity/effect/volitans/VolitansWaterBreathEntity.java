package com.leon.saintsdragons.server.entity.effect.volitans;

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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** Legacy sprite presentation only. VolitansBreathStream owns all gameplay collision and effects. */
public class VolitansWaterBreathEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_POISON_MODE =
            SynchedEntityData.defineId(VolitansWaterBreathEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(VolitansWaterBreathEntity.class, EntityDataSerializers.INT);
    private int age;

    public VolitansWaterBreathEntity(EntityType<? extends VolitansWaterBreathEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public VolitansWaterBreathEntity(Level level, Vec3 position, Vec3 velocity, int lifetime, boolean poison) {
        this(ModEntities.VOLITANS_WATER_BREATH.get(), level);
        setPos(position);
        setDeltaMovement(velocity);
        entityData.set(DATA_LIFETIME, Math.max(1, lifetime));
        entityData.set(DATA_POISON_MODE, poison);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_POISON_MODE, false);
        entityData.define(DATA_LIFETIME, 28);
    }

    @Override
    public void tick() {
        super.tick();
        if (++age >= getMaxAge()) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        var hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }
        setPos(end.x, end.y, end.z);
        setDeltaMovement(getDeltaMovement().scale(0.997));
    }

    public int getAge() {
        return age;
    }

    public int getMaxAge() {
        return entityData.get(DATA_LIFETIME);
    }

    public boolean isPoisonMode() {
        return entityData.get(DATA_POISON_MODE);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        age = tag.getInt("Age");
        entityData.set(DATA_LIFETIME, Math.max(1, tag.getInt("MaxAge")));
        entityData.set(DATA_POISON_MODE, tag.getBoolean("PoisonMode"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("MaxAge", getMaxAge());
        tag.putBoolean("PoisonMode", isPoisonMode());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
