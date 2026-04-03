package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Adapter that preserves existing navigation.moveTo(...) call sites
 * while routing path requests into the async flight controller.
 */
public class AsyncFlyingPathNavigation extends FlyingPathNavigation {
    private final AsyncFlightController controller;

    public AsyncFlyingPathNavigation(Mob mob, Level level, AsyncFlightController controller) {
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
        Vec3 target = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        this.controller.setWaypoint(target, speedModifier);
        return true;
    }

    @Override
    public void stop() {
        this.path = null;
        this.controller.clearAllWaypoints();
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
        // The async controller owns movement and path progression.
        // Vanilla FlyingPathNavigation.tick() assumes this.path is populated
        // and will crash when followThePath() dereferences it.
    }
}
