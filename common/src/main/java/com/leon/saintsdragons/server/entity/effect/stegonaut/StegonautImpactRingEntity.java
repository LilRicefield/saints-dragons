package com.leon.saintsdragons.server.entity.effect.stegonaut;

import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class StegonautImpactRingEntity extends Entity {
    private static final int DURATION = 16;
    private int age;

    public StegonautImpactRingEntity(EntityType<? extends StegonautImpactRingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public StegonautImpactRingEntity(Level level, Vec3 position) {
        this(ModEntities.STEGONAUT_IMPACT_RING.get(), level);
        setPos(position);
    }

    @Override
    protected void defineSynchedData() {
    }

    public int getAge() {
        return age;
    }

    public int getDuration() {
        return DURATION;
    }

    public float getScale(float partialTicks) {
        float ageFrac = (age + partialTicks) / (float) DURATION;
        return 0.8F + ageFrac * 3.8F;
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
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
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
