package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;

/**
 * Custom LookControl for dragons that limits rotation speed for smoother head movement.
 * Vanilla LookControl can rotate very fast - we slow it down for natural behavior.
 */
public class DragonLookControl<T extends DragonEntity> extends LookControl {

    protected final T dragon;
    protected float maxYRotSpeed = 10.0f;  // Max degrees per tick for yaw (default: ~40)
    protected float maxXRotSpeed = 10.0f;  // Max degrees per tick for pitch (default: ~40)

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
        // Skip look control when being ridden
        if (dragon.isVehicle()) {
            return;
        }

        // Vanilla LookControl can rotate at ~40 deg/tick which looks snappy
        // We clamp rotation speed for smoother, more natural head movement
        float oldYaw = dragon.yHeadRot;
        float oldPitch = dragon.getXRot();

        super.tick(); // Let vanilla calculate target rotations

        float newYaw = dragon.yHeadRot;
        float newPitch = dragon.getXRot();

        // Calculate how much vanilla rotated
        float yawChange = Mth.wrapDegrees(newYaw - oldYaw);
        float pitchChange = newPitch - oldPitch;

        // Clamp to our speed limits
        yawChange = Mth.clamp(yawChange, -maxYRotSpeed, maxYRotSpeed);
        pitchChange = Mth.clamp(pitchChange, -maxXRotSpeed, maxXRotSpeed);

        // Apply clamped rotation
        dragon.setYHeadRot(oldYaw + yawChange);
        dragon.setXRot(oldPitch + pitchChange);
    }
}
