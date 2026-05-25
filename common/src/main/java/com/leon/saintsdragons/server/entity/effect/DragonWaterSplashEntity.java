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

public class DragonWaterSplashEntity extends Entity {
    private static final int FRAME_TIME = 3;
    private static final int FRAME_COUNT = 4;
    private static final int DURATION = FRAME_TIME * FRAME_COUNT;
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(DragonWaterSplashEntity.class, EntityDataSerializers.FLOAT);
    private int age;

    public DragonWaterSplashEntity(EntityType<? extends DragonWaterSplashEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DragonWaterSplashEntity(Level level, Vec3 position, float yaw, float scale) {
        this(ModEntities.DRAGON_WATER_SPLASH.get(), level);
        setPos(position);
        setYRot(yaw);
        this.yRotO = yaw;
        setScale(scale);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, Math.max(0.35F, scale));
    }

    public float getScale(float partialTicks) {
        float progress = Math.min((age + partialTicks) / 5.0F, 1.0F);
        return this.entityData.get(DATA_SCALE) * (0.65F + progress * 0.35F);
    }

    public float getOpacity(float partialTicks) {
        float ageFrac = (age + partialTicks) / (float) DURATION;
        return Math.max(0.75F * (1.0F - ageFrac * ageFrac), 0.0F);
    }

    public int getAnimationFrame(float partialTicks) {
        return Math.min((int) ((age + partialTicks) / FRAME_TIME), FRAME_COUNT - 1);
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
        setYRot(tag.getFloat("Yaw"));
        this.yRotO = getYRot();
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("Yaw", getYRot());
        tag.putFloat("Scale", this.entityData.get(DATA_SCALE));
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
