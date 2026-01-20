package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * BlockEntity for Cindervane eggs to store parent information and baby gender
 */
public class CindervaneEggBlockEntity extends BlockEntity {
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private DragonGender babyGender;

    public CindervaneEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CINDERVANE_EGG.get(), pos, state);
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
    public DragonGender getBabyGender() {
        return this.babyGender;
    }

    public void setBabyGender(@Nullable DragonGender gender) {
        this.babyGender = gender;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.babyGender != null) {
            tag.putByte("BabyGender", this.babyGender.getId());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("BabyGender")) {
            this.babyGender = DragonGender.fromId(tag.getByte("BabyGender"));
        }
    }
}
