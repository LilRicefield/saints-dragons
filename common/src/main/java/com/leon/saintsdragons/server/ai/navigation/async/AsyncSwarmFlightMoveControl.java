package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class AsyncSwarmFlightMoveControl extends MoveControl {
    private final AsyncSwarmFlightController controller;

    public AsyncSwarmFlightMoveControl(Mob mob, AsyncSwarmFlightController controller) {
        super(mob);
        this.controller = controller;
    }

    @Override
    public void setWantedPosition(double x, double y, double z, double speedModifier) {
        super.setWantedPosition(x, y, z, speedModifier);
        this.controller.setWaypoint(new Vec3(x, y, z), speedModifier);
    }

    @Override
    public void tick() {
    }
}
