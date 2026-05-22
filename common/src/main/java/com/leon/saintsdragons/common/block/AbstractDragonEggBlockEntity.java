package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.server.entity.base.DragonGender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;


public abstract class AbstractDragonEggBlockEntity extends BlockEntity {
    private static final double LEGACY_NORMAL_HATCH_TICKS = 18000.0D;

    private double hatchProgress;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private UUID hatchAdvancementOwnerUUID;
    @Nullable
    private DragonGender babyGender;

    protected AbstractDragonEggBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public double getHatchProgress() {
        return hatchProgress;
    }

    public void setHatchProgress(double hatchProgress) {
        this.hatchProgress = Math.max(0.0D, Math.min(1.0D, hatchProgress));
        this.setChanged();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        this.setChanged();
    }

    @Nullable
    public UUID getHatchAdvancementOwnerUUID() {
        return this.hatchAdvancementOwnerUUID != null ? this.hatchAdvancementOwnerUUID : this.ownerUUID;
    }

    public void setHatchAdvancementOwnerUUID(@Nullable UUID hatchAdvancementOwnerUUID) {
        this.hatchAdvancementOwnerUUID = hatchAdvancementOwnerUUID;
        this.setChanged();
    }

    @Nullable
    public DragonGender getBabyGender() {
        return this.babyGender;
    }

    public void setBabyGender(@Nullable DragonGender gender) {
        this.babyGender = gender;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("HatchProgress", this.hatchProgress);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.hatchAdvancementOwnerUUID != null) {
            tag.putUUID("HatchAdvancementOwnerUUID", this.hatchAdvancementOwnerUUID);
        }
        if (this.babyGender != null) {
            tag.putByte("BabyGender", this.babyGender.getId());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("HatchProgress")) {
            this.hatchProgress = Math.max(0.0D, Math.min(1.0D, tag.getDouble("HatchProgress")));
        } else {
            double legacyTicks = Math.max(0, tag.getInt("HatchProgressTicks"));
            this.hatchProgress = Math.max(0.0D, Math.min(1.0D, legacyTicks / LEGACY_NORMAL_HATCH_TICKS));
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.hasUUID("HatchAdvancementOwnerUUID")) {
            this.hatchAdvancementOwnerUUID = tag.getUUID("HatchAdvancementOwnerUUID");
        }
        if (tag.contains("BabyGender")) {
            this.babyGender = DragonGender.fromId(tag.getByte("BabyGender"));
        }
    }
}
