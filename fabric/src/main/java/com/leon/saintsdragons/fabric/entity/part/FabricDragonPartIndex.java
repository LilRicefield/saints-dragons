package com.leon.saintsdragons.fabric.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class FabricDragonPartIndex {
    public interface Access {
        FabricDragonPartIndex saintsdragons$partIndex();
    }

    private static final int MAX_COLUMNS_PER_DRAGON = 256;
    private final Level level;
    private final Map<Ignivorus, Entry> entries = new IdentityHashMap<>();
    private final Map<Long, Set<Entry>> columns = new HashMap<>();
    private final Set<Entry> oversized = new HashSet<>();

    public FabricDragonPartIndex(Level level) {
        this.level = level;
    }

    public void update(Ignivorus dragon, FabricDragonPart[] parts) {
        if (dragon.level() != level || dragon.isRemoved() || !dragon.isAlive() || dragon.isBaby() || parts.length == 0) {
            remove(dragon);
            return;
        }

        AABB bounds = null;
        for (FabricDragonPart part : parts) {
            if (part.isPickable()) {
                bounds = bounds == null ? part.getBoundingBox() : bounds.minmax(part.getBoundingBox());
            }
        }
        if (bounds == null) {
            remove(dragon);
            return;
        }

        int minX = Mth.floor(bounds.minX) >> 4;
        int maxX = Mth.floor(bounds.maxX) >> 4;
        int minZ = Mth.floor(bounds.minZ) >> 4;
        int maxZ = Mth.floor(bounds.maxZ) >> 4;
        Entry entry = entries.get(dragon);
        if (entry != null && entry.minX == minX && entry.maxX == maxX && entry.minZ == minZ && entry.maxZ == maxZ) {
            entry.bounds = bounds;
            entry.parts = parts;
            return;
        }
        if (entry != null) removeColumns(entry);
        entry = new Entry(dragon, parts, bounds, minX, maxX, minZ, maxZ);
        entries.put(dragon, entry);
        if (entry.columnCount() > MAX_COLUMNS_PER_DRAGON) {
            oversized.add(entry);
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    columns.computeIfAbsent(ChunkPos.asLong(x, z), key -> new HashSet<>()).add(entry);
                }
            }
        }
    }

    public void remove(Ignivorus dragon) {
        Entry entry = entries.remove(dragon);
        if (entry != null) removeColumns(entry);
    }

    private void removeColumns(Entry entry) {
        if (oversized.remove(entry)) return;
        for (int x = entry.minX; x <= entry.maxX; x++) {
            for (int z = entry.minZ; z <= entry.maxZ; z++) {
                long key = ChunkPos.asLong(x, z);
                Set<Entry> bucket = columns.get(key);
                if (bucket == null) continue;
                bucket.remove(entry);
                if (bucket.isEmpty()) columns.remove(key);
            }
        }
    }

    public void append(Entity except, AABB area, Predicate<? super Entity> predicate, List<Entity> result) {
        if (entries.isEmpty()) return;
        int minX = Mth.floor(area.minX) >> 4;
        int maxX = Mth.floor(area.maxX) >> 4;
        int minZ = Mth.floor(area.minZ) >> 4;
        int maxZ = Mth.floor(area.maxZ) >> 4;
        long count = ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);

        // Snapshot candidates before calling arbitrary predicates, which may change the index.
        Set<Entry> candidates = new HashSet<>(oversized);
        if (count >= columns.size()) {
            candidates.addAll(entries.values());
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Set<Entry> bucket = columns.get(ChunkPos.asLong(x, z));
                    if (bucket != null) candidates.addAll(bucket);
                }
            }
        }
        for (Entry entry : candidates) {
            Ignivorus dragon = entry.dragon;
            if (dragon == except || dragon.level() != level || dragon.isRemoved() || !dragon.isAlive()
                    || dragon.isBaby() || !entry.bounds.intersects(area)) continue;
            for (FabricDragonPart part : entry.parts) {
                if (part != except && part.isPickable() && part.getBoundingBox().intersects(area) && predicate.test(part)) {
                    result.add(part);
                }
            }
        }
    }

    private static final class Entry {
        final Ignivorus dragon;
        FabricDragonPart[] parts;
        AABB bounds;
        final int minX, maxX, minZ, maxZ;

        Entry(Ignivorus dragon, FabricDragonPart[] parts, AABB bounds, int minX, int maxX, int minZ, int maxZ) {
            this.dragon = dragon;
            this.parts = parts;
            this.bounds = bounds;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        long columnCount() {
            return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
        }
    }
}
