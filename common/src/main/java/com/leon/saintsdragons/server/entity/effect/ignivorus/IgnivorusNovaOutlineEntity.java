package com.leon.saintsdragons.server.entity.effect.ignivorus;

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

public class IgnivorusNovaOutlineEntity extends Entity {

    private static final int DURATION = 20;

    private int age;

    public IgnivorusNovaOutlineEntity(EntityType<? extends IgnivorusNovaOutlineEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public IgnivorusNovaOutlineEntity(Level level, Vec3 position) {
        this(ModEntities.IGNIVORUS_NOVA_OUTLINE.get(), level);
        setPos(position);
    }

    @Override
    protected void defineSynchedData() {
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
        return (float) Math.max((1.0 - ageFrac * ageFrac) * 1.0, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (!this.level().isClientSide) {
            if (this.age >= DURATION) {
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", this.age);
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
