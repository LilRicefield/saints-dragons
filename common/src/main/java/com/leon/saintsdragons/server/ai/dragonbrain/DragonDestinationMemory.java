package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class DragonDestinationMemory {
    private static final int MAX_ENTRIES = 48;
    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    private long lastPrunedAt = Long.MIN_VALUE;

    public void remember(ResourceLocation purpose, ServerLevel level, BlockPos position, Outcome outcome, int ttlTicks) {
        prune(level.getGameTime());
        Key key = new Key(purpose, GlobalPos.of(level.dimension(), position.immutable()));
        entries.remove(key);
        if (ttlTicks <= 0) return;
        if (entries.size() >= MAX_ENTRIES) entries.remove(entries.keySet().iterator().next());
        entries.put(key, new Entry(outcome, level.getGameTime() + ttlTicks));
    }

    public boolean rejected(ResourceLocation purpose, ServerLevel level, BlockPos position) {
        prune(level.getGameTime());
        Entry entry = entries.get(new Key(purpose, GlobalPos.of(level.dimension(), position)));
        return entry != null && entry.outcome == Outcome.FAILED;
    }

    public List<BlockPos> known(ResourceLocation purpose, ServerLevel level, BlockPos origin, double range,
                                Predicate<BlockPos> validator) {
        prune(level.getGameTime());
        List<BlockPos> result = new ArrayList<>();
        List<Key> invalid = new ArrayList<>();
        for (Map.Entry<Key, Entry> remembered : entries.entrySet()) {
            Key key = remembered.getKey();
            if (!key.purpose.equals(purpose) || key.position.dimension() != level.dimension()
                    || remembered.getValue().outcome != Outcome.SUCCESS) continue;
            BlockPos position = key.position.pos();
            if (origin.distSqr(position) > range * range || !loaded(level, position)) continue;
            if (validator.test(position)) result.add(position);
            else invalid.add(key);
        }
        invalid.forEach(entries::remove);
        result.sort(Comparator.comparingDouble(origin::distSqr));
        return List.copyOf(result);
    }

    public void forget(ResourceLocation purpose, ServerLevel level, BlockPos position) {
        entries.remove(new Key(purpose, GlobalPos.of(level.dimension(), position)));
    }

    public static boolean loaded(ServerLevel level, BlockPos position) {
        return !level.isOutsideBuildHeight(position) && level.getWorldBorder().isWithinBounds(position)
                && level.hasChunkAt(position);
    }

    public void clear() {
        entries.clear();
        lastPrunedAt = Long.MIN_VALUE;
    }

    private void prune(long now) {
        if (now == lastPrunedAt) return;
        lastPrunedAt = now;
        entries.values().removeIf(entry -> now >= entry.expiresAt);
    }

    public enum Outcome { SUCCESS, FAILED }

    private record Key(ResourceLocation purpose, GlobalPos position) {
    }

    private record Entry(Outcome outcome, long expiresAt) {
    }
}
