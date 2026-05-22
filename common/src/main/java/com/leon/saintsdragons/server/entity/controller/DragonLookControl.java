package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;

public class DragonLookControl<T extends DragonEntity> extends LookControl {

    protected final T dragon;
    protected float maxYRotSpeed = 10.0f;
    protected float maxXRotSpeed = 10.0f;

    public DragonLookControl(T dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    public DragonLookControl(T dragon, float maxYRotSpeed, float maxXRotSpeed) {
        super(dragon);
        this.dragon = dragon;
        this.maxYRotSpeed = maxYRotSpeed;
        this.maxXRotSpeed = maxXRotSpeed;
    }

    @Override
    public void tick() {
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return;
        }
        if (!dragon.isVehicle() && (dragon.isOrderedToSit() || dragon.getSitProgress() > 0.0f)) {
            return;
        }
        if (dragon.isVehicle()) {
            super.tick();
            return;
        }
        float oldYaw = dragon.yHeadRot;
        float oldPitch = dragon.getXRot();
        super.tick();
        float newYaw = dragon.yHeadRot;
        float newPitch = dragon.getXRot();
        float yawChange = Mth.wrapDegrees(newYaw - oldYaw);
        float pitchChange = newPitch - oldPitch;
        yawChange = Mth.clamp(yawChange, -maxYRotSpeed, maxYRotSpeed);
        pitchChange = Mth.clamp(pitchChange, -maxXRotSpeed, maxXRotSpeed);
        dragon.setYHeadRot(oldYaw + yawChange);
        dragon.setXRot(oldPitch + pitchChange);
    }
}
