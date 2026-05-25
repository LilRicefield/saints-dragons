package com.leon.saintsdragons.server.entity.effect;

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

public class ImpactRingEntity extends Entity {
    private static final int DURATION = 16;
    private static final EntityDataAccessor<Float> DATA_VISUAL_SCALE =
            SynchedEntityData.defineId(ImpactRingEntity.class, EntityDataSerializers.FLOAT);
    private int age;

    public ImpactRingEntity(EntityType<? extends ImpactRingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ImpactRingEntity(Level level, Vec3 position) {
        this(ModEntities.STEGONAUT_IMPACT_RING.get(), level);
        setPos(position);
    }

    public ImpactRingEntity(Level level, Vec3 position, float visualScale) {
        this(level, position);
        setVisualScale(visualScale);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_VISUAL_SCALE, 1.0F);
    }

    public void setVisualScale(float visualScale) {
        entityData.set(DATA_VISUAL_SCALE, Math.max(0.05F, visualScale));
    }

    public int getAge() {
        return age;
    }

    public int getDuration() {
        return DURATION;
    }

    public float getScale(float partialTicks) {
        float ageFrac = (age + partialTicks) / (float) DURATION;
        return (0.8F + ageFrac * 3.8F) * entityData.get(DATA_VISUAL_SCALE);
    }

    public float getOpacity(float partialTicks) {
        float ageFrac = (age + partialTicks) / (float) DURATION;
        return Math.max(1.0F - ageFrac * ageFrac, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (!level().isClientSide && age >= DURATION) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        age = tag.getInt("Age");
        setVisualScale(tag.contains("VisualScale") ? tag.getFloat("VisualScale") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("VisualScale", entityData.get(DATA_VISUAL_SCALE));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }
}
