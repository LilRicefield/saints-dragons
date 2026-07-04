package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AsyncSwarmFlyingPathNavigation extends FlyingPathNavigation {
    private final AsyncSwarmFlightController controller;

    public AsyncSwarmFlyingPathNavigation(Mob mob, Level level, AsyncSwarmFlightController controller) {
        super(mob, level);
        this.controller = controller;
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speedModifier) {
        this.controller.setWaypoint(new Vec3(x, y, z), speedModifier);
        return true;
    }

    @Override
    public boolean moveTo(Entity entity, double speedModifier) {
        this.controller.setWaypoint(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), speedModifier);
        return true;
    }

    @Override
    public void stop() {
        this.path = null;
        this.controller.clearWaypoint();
    }

    @Override
    public boolean isDone() {
        return this.controller.isIdle();
    }

    @Override
    public boolean isInProgress() {
        return !this.controller.isIdle();
    }

    @Override
    public void tick() {
    }
}
