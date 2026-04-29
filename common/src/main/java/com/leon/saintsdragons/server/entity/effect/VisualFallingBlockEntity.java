package com.leon.saintsdragons.server.entity.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class VisualFallingBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> BLOCK_STATE =
            SynchedEntityData.defineId(VisualFallingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

    private int lifetime = 40;

    public VisualFallingBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    public VisualFallingBlockEntity(EntityType<?> entityType, Level level, double x, double y, double z, BlockState blockState, int lifetime) {
        this(entityType, level);
        this.setBlockState(blockState);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.lifetime = lifetime;
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    public BlockState getBlockState() {
        return this.entityData.get(BLOCK_STATE);
    }

    public void setBlockState(BlockState blockState) {
        this.entityData.set(BLOCK_STATE, blockState);
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround() && this.tickCount > 20) {
            this.discard();
        }
        if (this.tickCount > lifetime) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.putInt("Lifetime", this.lifetime);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState")));
        this.lifetime = tag.getInt("Lifetime");
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
    }
}