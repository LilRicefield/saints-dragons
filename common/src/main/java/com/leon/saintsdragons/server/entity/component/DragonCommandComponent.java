package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

public final class DragonCommandComponent {
    private final DragonEntity dragon;
    private final EntityDataAccessor<Integer> dataAccessor;

    public DragonCommandComponent(DragonEntity dragon, EntityDataAccessor<Integer> dataAccessor) {
        this.dragon = dragon;
        this.dataAccessor = dataAccessor;
    }

    public int getCommand() {
        return dragon.getEntityData().get(dataAccessor);
    }

    public void setCommand(int command) {
        dragon.getEntityData().set(dataAccessor, command);
        // Only sit via command if tamed; untamed dragons ignore owner commands
        if (dragon.isTame()) {
            dragon.applyCommandState(command);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Command", getCommand());
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("Command")) {
            setCommand(tag.getInt("Command"));
        }
    }
}
