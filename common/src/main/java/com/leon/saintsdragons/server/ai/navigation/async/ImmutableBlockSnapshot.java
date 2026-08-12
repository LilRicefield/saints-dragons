package com.leon.saintsdragons.server.ai.navigation.async;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A worker-safe view of a bounded set of loaded chunk sections.
 *
 * <p>The section palettes are copied on the server thread. Workers never retain or query a
 * {@link ServerLevel}, {@link ChunkAccess}, or live entity through this object. Missing chunks and
 * positions outside the captured bounds are deliberately solid so a path cannot escape into data
 * that was not captured. Context-sensitive collision shapes are also resolved during capture;
 * workers only consume those results or Minecraft's immutable per-state shape cache.</p>
 */
final class ImmutableBlockSnapshot implements BlockGetter {
    private static final BlockState OUTSIDE = Blocks.BARRIER.defaultBlockState();
    private static final List<AABB> FULL_BLOCK_LOCAL =
            List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
    private static final Map<BlockState, List<AABB>> STATIC_COLLISION_BOXES =
            new ConcurrentHashMap<>();

    private final Map<Long, PalettedContainer<BlockState>> sections;
    private final Long2ObjectOpenHashMap<List<AABB>> collisionBoxCache = new Long2ObjectOpenHashMap<>();
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int minBuildHeight;
    private final int height;

    private ImmutableBlockSnapshot(Map<Long, PalettedContainer<BlockState>> sections,
                                   Long2ObjectOpenHashMap<List<AABB>> dynamicCollisionBoxes,
                                   int minX,
                                   int minY,
                                   int minZ,
                                   int maxX,
                                   int maxY,
                                   int maxZ,
                                   int minBuildHeight,
                                   int height) {
        this.sections = Map.copyOf(sections);
        this.collisionBoxCache.putAll(dynamicCollisionBoxes);
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.minBuildHeight = minBuildHeight;
        this.height = height;
    }

    static ImmutableBlockSnapshot capture(ServerLevel level, BlockPos requestedMin, BlockPos requestedMax) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Immutable block snapshots must be captured on the server thread");
        }

        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();
        int minX = Math.min(requestedMin.getX(), requestedMax.getX());
        int minY = Math.max(minBuildHeight, Math.min(requestedMin.getY(), requestedMax.getY()));
        int minZ = Math.min(requestedMin.getZ(), requestedMax.getZ());
        int maxX = Math.max(requestedMin.getX(), requestedMax.getX());
        int maxY = Math.min(maxBuildHeight - 1, Math.max(requestedMin.getY(), requestedMax.getY()));
        int maxZ = Math.max(requestedMin.getZ(), requestedMax.getZ());

        Map<Long, PalettedContainer<BlockState>> sections = new HashMap<>();
        Long2ObjectOpenHashMap<List<AABB>> dynamicCollisionBoxes = new Long2ObjectOpenHashMap<>();
        if (minY <= maxY) {
            int minChunkX = SectionPos.blockToSectionCoord(minX);
            int maxChunkX = SectionPos.blockToSectionCoord(maxX);
            int minChunkZ = SectionPos.blockToSectionCoord(minZ);
            int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
            int minSectionY = SectionPos.blockToSectionCoord(minY);
            int maxSectionY = SectionPos.blockToSectionCoord(maxY);

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    LevelChunkSection[] chunkSections = chunk.getSections();
                    for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                        int sectionIndex = level.getSectionIndexFromSectionY(sectionY);
                        if (sectionIndex < 0 || sectionIndex >= chunkSections.length) {
                            continue;
                        }
                        PalettedContainer<BlockState> copiedStates = chunkSections[sectionIndex].getStates().copy();
                        sections.put(SectionPos.asLong(chunkX, sectionY, chunkZ), copiedStates);
                        captureDynamicCollisionBoxes(
                                level,
                                copiedStates,
                                chunkX,
                                sectionY,
                                chunkZ,
                                minX,
                                minY,
                                minZ,
                                maxX,
                                maxY,
                                maxZ,
                                dynamicCollisionBoxes
                        );
                    }
                }
            }
        }

        return new ImmutableBlockSnapshot(
                sections,
                dynamicCollisionBoxes,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                minBuildHeight,
                maxBuildHeight - minBuildHeight
        );
    }

    private static void captureDynamicCollisionBoxes(ServerLevel level,
                                                      PalettedContainer<BlockState> states,
                                                      int sectionX,
                                                      int sectionY,
                                                      int sectionZ,
                                                      int minX,
                                                      int minY,
                                                      int minZ,
                                                      int maxX,
                                                      int maxY,
                                                      int maxZ,
                                                      Long2ObjectOpenHashMap<List<AABB>> destination) {
        if (!states.maybeHas(state -> state.getBlock().hasDynamicShape())) {
            return;
        }

        int sectionMinX = sectionX << 4;
        int sectionMinY = sectionY << 4;
        int sectionMinZ = sectionZ << 4;
        int fromX = Math.max(minX, sectionMinX);
        int fromY = Math.max(minY, sectionMinY);
        int fromZ = Math.max(minZ, sectionMinZ);
        int toX = Math.min(maxX, sectionMinX + 15);
        int toY = Math.min(maxY, sectionMinY + 15);
        int toZ = Math.min(maxZ, sectionMinZ + 15);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int y = fromY; y <= toY; y++) {
                    BlockState state = states.get(x & 15, y & 15, z & 15);
                    if (!state.getBlock().hasDynamicShape()) {
                        continue;
                    }
                    cursor.set(x, y, z);
                    destination.put(
                            cursor.asLong(),
                            moveCollisionBoxes(resolveLocalCollisionBoxes(level, state, cursor), cursor)
                    );
                }
            }
        }
    }

    boolean contains(BlockPos pos) {
        return pos.getX() >= this.minX && pos.getX() <= this.maxX
                && pos.getY() >= this.minY && pos.getY() <= this.maxY
                && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
    }

    List<AABB> collisionBoxes(BlockPos pos) {
        long key = pos.asLong();
        List<AABB> cached = this.collisionBoxCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<AABB> computed = this.computeCollisionBoxes(pos.immutable());
        this.collisionBoxCache.put(key, computed);
        return computed;
    }

    private List<AABB> computeCollisionBoxes(BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        List<AABB> localBoxes;
        if (state.getBlock().hasDynamicShape()) {
            // Dynamic states were resolved during capture; fail closed if one escaped it.
            localBoxes = FULL_BLOCK_LOCAL;
        } else {
            // Minecraft bakes non-dynamic collision shapes into immutable BlockState caches.
            localBoxes = STATIC_COLLISION_BOXES.computeIfAbsent(
                    state,
                    ignored -> resolveLocalCollisionBoxes(this, state, pos)
            );
        }
        return moveCollisionBoxes(localBoxes, pos);
    }

    private static List<AABB> resolveLocalCollisionBoxes(BlockGetter level, BlockState state, BlockPos pos) {
        try {
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                return List.of();
            }
            return List.copyOf(shape.toAabbs());
        } catch (RuntimeException exception) {
            return FULL_BLOCK_LOCAL;
        }
    }

    private static List<AABB> moveCollisionBoxes(List<AABB> localBoxes, BlockPos pos) {
        if (localBoxes.isEmpty()) {
            return List.of();
        }
        return localBoxes.stream()
                .map(box -> box.move(pos.getX(), pos.getY(), pos.getZ()))
                .toList();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (!this.contains(pos)) {
            return OUTSIDE;
        }
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        PalettedContainer<BlockState> states = this.sections.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
        if (states == null) {
            return OUTSIDE;
        }
        return states.get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getMinBuildHeight() {
        return this.minBuildHeight;
    }
}
