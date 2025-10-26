package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;

/**
 * Simple LookControl wrapper for dragons.
 * Just delegates to vanilla - smoothing is handled client-side via The Dawn Era approach.
 */
public class DragonLookControl<T extends DragonEntity> extends LookControl {

    protected final T dragon;

    public DragonLookControl(T dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        super.tick();
    }
}
