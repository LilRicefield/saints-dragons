package com.leon.saintsdragons.server.entity.effect;

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

public class GroundCrackEntity extends Entity {
    private static final int DURATION = 34;
    private int age;

    public GroundCrackEntity(EntityType<? extends GroundCrackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public GroundCrackEntity(Level level, Vec3 position, float yaw) {
        this(ModEntities.STEGONAUT_GROUND_CRACK.get(), level);
        setPos(position);
        setYRot(yaw);
        this.yRotO = yaw;
    }

    @Override
    protected void defineSynchedData() {
    }

    public float getScale(float partialTicks) {
        float progress = Math.min((age + partialTicks) / 5.0F, 1.0F);
        return 4.0F + progress * 3.0F;
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
        setYRot(tag.getFloat("Yaw"));
        this.yRotO = getYRot();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("Yaw", getYRot());
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
