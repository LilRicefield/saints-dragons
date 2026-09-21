package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class DragonNearbyEntities {
    private static final int MAX_SNAPSHOTS = 4;
    private static final int MAX_CACHED_ENTITIES = 128;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public <E extends LivingEntity> List<E> find(ServerLevel level, Class<E> type, AABB bounds,
                                               Predicate<? super E> filter) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Nearby queries require the server thread");
        long now = level.getGameTime();
        snapshots.removeIf(snapshot -> snapshot.tick != now || snapshot.dimension != level.dimension());
        for (Snapshot snapshot : snapshots) {
            if (snapshot.type.isAssignableFrom(type) && contains(snapshot.bounds, bounds)) {
                List<E> result = new ArrayList<>();
                for (WeakReference<LivingEntity> reference : snapshot.entities) {
                    LivingEntity entity = reference.get();
                    if (entity != null && type.isInstance(entity) && !entity.isRemoved()
                            && entity.level() == level && entity.getBoundingBox().intersects(bounds)) {
                        E candidate = type.cast(entity);
                        if (filter.test(candidate)) result.add(candidate);
                    }
                }
                return result;
            }
        }
        AABB searchBounds = bounds.inflate(2.0D);
        List<E> entities = level.getEntitiesOfClass(type, searchBounds, entity -> true);
        if (entities.size() <= MAX_CACHED_ENTITIES) {
            if (snapshots.size() == MAX_SNAPSHOTS) snapshots.remove(0);
            snapshots.add(new Snapshot(level.dimension(), now, type, searchBounds,
                    entities.stream().map(entity -> new WeakReference<LivingEntity>(entity)).toList()));
        }
        List<E> result = new ArrayList<>();
        for (E entity : entities) {
            if (!entity.isRemoved() && entity.level() == level && entity.getBoundingBox().intersects(bounds)
                    && filter.test(entity)) result.add(entity);
        }
        return result;
    }

    public void invalidate() {
        snapshots.clear();
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private record Snapshot(ResourceKey<Level> dimension, long tick, Class<? extends LivingEntity> type, AABB bounds,
                            List<WeakReference<LivingEntity>> entities) {
    }
}
