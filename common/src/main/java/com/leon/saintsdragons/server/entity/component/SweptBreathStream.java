package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class SweptBreathStream<T> {
    private final List<Emission<T>> emissions = new ArrayList<>();
    private final Map<UUID, Long> nextHits = new HashMap<>();
    private final int hitInterval;
    private final int capacity;

    public SweptBreathStream(int hitInterval, int capacity) {
        this.hitInterval = Math.max(1, hitInterval);
        this.capacity = Math.max(1, capacity);
    }

    public void emit(ExpandingBreathSection section, T payload) {
        if (emissions.size() >= capacity) emissions.remove(0);
        emissions.add(new Emission<>(section, payload));
    }

    public void forEachPayload(Consumer<T> action) {
        emissions.forEach(emission -> action.accept(emission.payload()));
    }

    public void tick(ServerLevel level, BiPredicate<T, LivingEntity> canHit,
                     HitHandler<T> hit, BiConsumer<T, ExpandingBreathSection.Sweep> trace) {
        long now = level.getGameTime();
        nextHits.values().removeIf(expiry -> expiry <= now);
        Set<UUID> attempted = new HashSet<>();
        for (Emission<T> emission : emissions) {
            ExpandingBreathSection section = emission.section();
            AABB bounds = section.nextBounds();
            List<ExpandingBreathSection.Sweep> sweeps = section.advance(level);
            if (sweeps.isEmpty()) continue;
            T payload = emission.payload();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                    target -> canHit.test(payload, target))) {
                UUID id = target.getUUID();
                if (attempted.contains(id) || nextHits.getOrDefault(id, Long.MIN_VALUE) > now) continue;
                for (ExpandingBreathSection.Sweep sweep : sweeps) {
                    if (!sweep.hits(target.getBoundingBox())) continue;
                    attempted.add(id);
                    if (hit.apply(payload, target, sweep)) nextHits.put(id, now + hitInterval);
                    break;
                }
            }
            for (ExpandingBreathSection.Sweep sweep : sweeps) trace.accept(payload, sweep);
        }
        emissions.removeIf(emission -> emission.section().finished());
    }

    public void clear() {
        emissions.clear();
        nextHits.clear();
    }

    @FunctionalInterface
    public interface HitHandler<T> {
        boolean apply(T payload, LivingEntity target, ExpandingBreathSection.Sweep sweep);
    }

    private record Emission<T>(ExpandingBreathSection section, T payload) {}
}
