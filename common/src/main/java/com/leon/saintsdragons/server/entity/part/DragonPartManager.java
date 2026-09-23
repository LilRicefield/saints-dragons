package com.leon.saintsdragons.server.entity.part;

import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class DragonPartManager<P extends Entity & DragonPartEntity> {
    private final Ignivorus dragon;
    private final IntFunction<P> factory;
    private final List<P> parts = new ArrayList<>();

    public DragonPartManager(Ignivorus dragon, IntFunction<P> factory) {
        this.dragon = dragon;
        this.factory = factory;
    }

    public List<P> parts() { return parts; }

    // allocate the stable part list without sampling a pose or querying the world
    public void initializeParts() {
        if (!parts.isEmpty() || dragon.isRemoved()) return;
        // structure templates can construct dragons on a worldgen worker
        // pose selection checks terrain so defer it until update()
        AABB initialBounds = new AABB(dragon.position(), dragon.position());
        for (int i = 0; i < IgnivorusHitboxes.REGIONS.size(); i++) {
            P part = factory.apply(i);
            part.updateBounds(initialBounds);
            parts.add(part);
        }
    }

    public void update() {
        if (dragon.isRemoved()) {
            remove();
            return;
        }
        initializeParts();
        // Abilities may have sampled a pose before this tick's movement and flight updates finished.
        dragon.getCollisionState().invalidate();
        for (int i = 0; i < parts.size(); i++) parts.get(i).updateBounds(dragon.isBaby()
                ? new AABB(dragon.position(), dragon.position())
                : dragon.getCollisionState().bounds(i));
    }

    public void remove() {
        for (P part : parts) part.remove(Entity.RemovalReason.DISCARDED);
        parts.clear();
    }
}
