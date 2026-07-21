package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.PassiveTreeDestroyer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class DragonDestructionManager {
    private DragonDestructionManager() {}
    private static final Map<BlockPos, Integer> blockMeltProgress = new HashMap<>();
    private static final Map<BlockPos, Long> lastExposureTick = new HashMap<>();
    private static final int FIRE_BREATH_FURNACE_BOOST_TICKS = 10;
    private static final int FIRE_BREATH_SMOKER_BOOST_TICKS = 20;
    private static final int FIRE_BREATH_BLAST_FURNACE_BOOST_TICKS = 20;
    private static final int FIRE_BREATH_FURNACE_LIT_TICKS = 40;
    private static final int FIRE_BODY_FURNACE_BOOST_TICKS = 4;
    private static final int FIRE_BODY_SMOKER_BOOST_TICKS = 6;
    private static final int FIRE_BODY_BLAST_FURNACE_BOOST_TICKS = 6;
    private static final int FIRE_BODY_FURNACE_LIT_TICKS = 20;
    private static final int PASSIVE_TREE_CHECK_INTERVAL = 3;
    private static final int MAX_TREE_LOGS = 192;
    private static final int MAX_TREE_LEAVES = 768;
    private static final int MAX_LEAF_DISTANCE = 6;

    public static boolean canApplyPassiveTreeDestruction(ServerLevel level, DragonEntity dragon) {
        return level != null
                && dragon instanceof PassiveTreeDestroyer
                && !dragon.isBaby()
                && dragon.isAlive()
                && DragonGriefingRules.canDestroyBlocks(level);
    }

    public static boolean isPassivelyBreakableTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || isNaturalTreeLeaf(state, 0);
    }

    public static void applyPassiveTreeDestruction(ServerLevel level, DragonEntity dragon) {
        if (!canApplyPassiveTreeDestruction(level, dragon)
                || (dragon.tickCount + dragon.getId()) % PASSIVE_TREE_CHECK_INTERVAL != 0) {
            return;
        }

        Vec3 planarMotion = new Vec3(dragon.getDeltaMovement().x, 0.0D, dragon.getDeltaMovement().z);
        double horizontalReach = Math.max(0.75D, dragon.getBbWidth() * 0.25D);
        AABB contactBounds = dragon.getBoundingBox().inflate(horizontalReach, 0.25D, horizontalReach);
        if (planarMotion.lengthSqr() > 1.0E-4D) {
            Vec3 probe = planarMotion.normalize().scale(Math.min(1.5D, 0.5D + planarMotion.length() * 2.0D));
            contactBounds = contactBounds.expandTowards(probe.x, 0.0D, probe.z);
        }

        BlockPos contactedLog = findContactedTreeLog(level, contactBounds);
        if (contactedLog == null) {
            return;
        }

        Set<BlockPos> logs = gatherConnectedLogs(level, contactedLog);
        if (logs.size() < 2) {
            return;
        }
        Set<BlockPos> leaves = gatherTreeLeaves(level, logs);

        destroyTreeBlocks(level, dragon, leaves);
        destroyTreeBlocks(level, dragon, logs);
    }

    private static int destroyTreeBlocks(ServerLevel level, DragonEntity dragon, Set<BlockPos> blocks) {
        int destroyed = 0;
        for (BlockPos pos : blocks) {
            if (!DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, pos)
                    && level.destroyBlock(pos, false, dragon)) {
                destroyed++;
            }
        }
        return destroyed;
    }

    private static BlockPos findContactedTreeLog(ServerLevel level, AABB bounds) {
        BlockPos contactedLeaf = null;
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(bounds.minX), Mth.floor(bounds.minY), Mth.floor(bounds.minZ),
                Mth.floor(bounds.maxX), Mth.floor(bounds.maxY), Mth.floor(bounds.maxZ))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                if (!DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, pos)) {
                    return pos.immutable();
                }
                continue;
            }
            if (contactedLeaf == null
                    && isNaturalTreeLeaf(state, 0)
                    && !DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, pos)) {
                contactedLeaf = pos.immutable();
            }
        }
        return contactedLeaf == null ? null : findLogConnectedToLeaf(level, contactedLeaf);
    }

    private static BlockPos findLogConnectedToLeaf(ServerLevel level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<LeafSearchNode> open = new ArrayDeque<>();
        open.add(new LeafSearchNode(origin, 0));

        while (!open.isEmpty() && visited.size() < MAX_TREE_LEAVES) {
            LeafSearchNode search = open.removeFirst();
            if (search.distance() > MAX_LEAF_DISTANCE || !visited.add(search.pos())
                    || !level.isLoaded(search.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(search.pos());
            if (state.is(BlockTags.LOGS)) {
                if (!DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, search.pos())) {
                    return search.pos();
                }
                continue;
            }
            if (!state.is(BlockTags.LEAVES)) {
                continue;
            }
            if (DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, search.pos())) {
                continue;
            }
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                open.addLast(new LeafSearchNode(
                        search.pos().relative(direction).immutable(), search.distance() + 1));
            }
        }
        return null;
    }

    private static Set<BlockPos> gatherConnectedLogs(ServerLevel level, BlockPos origin) {
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> discovered = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        discovered.add(start);
        open.add(start);

        while (!open.isEmpty() && logs.size() < MAX_TREE_LOGS) {
            BlockPos current = open.removeFirst();
            if (!level.isLoaded(current)) {
                continue;
            }
            BlockState state = level.getBlockState(current);
            if (!state.is(BlockTags.LOGS)
                    || DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, current)) {
                continue;
            }
            logs.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos neighbor = current.offset(dx, dy, dz).immutable();
                        if (discovered.add(neighbor)
                                && level.isLoaded(neighbor)
                                && level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            open.addLast(neighbor);
                        }
                    }
                }
            }
        }
        return logs;
    }

    private static Set<BlockPos> gatherTreeLeaves(ServerLevel level, Set<BlockPos> logs) {
        Set<BlockPos> leaves = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<LeafSearchNode> open = new ArrayDeque<>();
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                open.addLast(new LeafSearchNode(log.relative(direction).immutable(), 1));
            }
        }

        while (!open.isEmpty() && leaves.size() < MAX_TREE_LEAVES) {
            LeafSearchNode search = open.removeFirst();
            if (search.distance() > MAX_LEAF_DISTANCE || !visited.add(search.pos())
                    || !level.isLoaded(search.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(search.pos());
            if (!isNaturalTreeLeaf(state, search.distance())) {
                continue;
            }
            if (DragonGriefingRules.isProtectedFromPassiveTreeDestruction(level, search.pos())) {
                continue;
            }
            leaves.add(search.pos());
            for (Direction direction : Direction.values()) {
                open.addLast(new LeafSearchNode(search.pos().relative(direction).immutable(), search.distance() + 1));
            }
        }
        return leaves;
    }

    private static boolean isNaturalTreeLeaf(BlockState state, int distance) {
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.PERSISTENT)
                && state.getValue(BlockStateProperties.PERSISTENT)) {
            return false;
        }
        return distance == 0
                || !state.hasProperty(BlockStateProperties.DISTANCE)
                || state.getValue(BlockStateProperties.DISTANCE) == distance;
    }

    private record LeafSearchNode(BlockPos pos, int distance) {
    }
    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds,
                                             int meltTicksRequired,
                                             boolean canMeltBlocks) {
        applyFireBreathImpact(level, dragon, impactPoint, radius, damage, fireSeconds, meltTicksRequired, canMeltBlocks, true);
    }

    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds,
                                             int meltTicksRequired,
                                             boolean canMeltBlocks,
                                             boolean igniteBlocks) {
        if (level == null || dragon == null || impactPoint == null) {
            return;
        }
        damageEntities(level, dragon, impactPoint, radius, damage, fireSeconds);
        if (igniteBlocks) {
            igniteBlocks(level, impactPoint, radius);
        }
        DragonUtilities.accelerateCooking(level, dragon, impactPoint, radius,
                FIRE_BREATH_FURNACE_BOOST_TICKS,
                FIRE_BREATH_SMOKER_BOOST_TICKS,
                FIRE_BREATH_BLAST_FURNACE_BOOST_TICKS,
                FIRE_BREATH_FURNACE_LIT_TICKS);

        if (canMeltBlocks && meltTicksRequired > 0) {
            double destructionRadius = radius * 0.6;
            meltBlocks(level, impactPoint, destructionRadius, meltTicksRequired);
        }
    }

    public static void applyFlameImpact(ServerLevel level, Vec3 impactPoint, double radius) {
        applyFlameImpact(level, null, impactPoint, radius);
    }

    public static void applyFlameImpact(ServerLevel level, DragonEntity dragon, Vec3 impactPoint, double radius) {
        if (level == null || impactPoint == null) {
            return;
        }
        igniteBlocks(level, impactPoint, radius);
        DragonUtilities.accelerateCooking(level, dragon, impactPoint, radius,
                FIRE_BREATH_FURNACE_BOOST_TICKS,
                FIRE_BREATH_SMOKER_BOOST_TICKS,
                FIRE_BREATH_BLAST_FURNACE_BOOST_TICKS,
                FIRE_BREATH_FURNACE_LIT_TICKS);
    }

    private static void damageEntities(ServerLevel level,
                                       DragonEntity dragon,
                                       Vec3 impactPoint,
                                       double radius,
                                       float damage,
                                       int fireSeconds) {
        AABB area = new AABB(impactPoint, impactPoint).inflate(radius);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != dragon && !dragon.isAlly(e))) {

            if (hit.add(entity)) {
                entity.hurt(level.damageSources().dragonBreath(), damage);
                entity.setSecondsOnFire(fireSeconds);
            }
        }
    }

    private static void igniteBlocks(ServerLevel level, Vec3 impactPoint, double radius) {
        if (!DragonGriefingRules.canSetBlocksOnFire(level)) {
            return;
        }
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        RandomSource random = level.random;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (random.nextFloat() > 0.6F) {
                continue;
            }
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > radius * radius) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }

            if (level.isEmptyBlock(pos)) {
                BlockState fire = Blocks.FIRE.defaultBlockState();
                if (fire.canSurvive(level, pos)) {
                    level.setBlock(pos, fire, 11);
                }
            } else if (random.nextFloat() < 0.35F) {
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) && Blocks.FIRE.defaultBlockState().canSurvive(level, above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }

    public static void applyFireBodyCookingAura(ServerLevel level, Vec3 impactPoint, double radius) {
        applyFireBodyCookingAura(level, null, impactPoint, radius);
    }

    public static void applyFireBodyCookingAura(ServerLevel level, DragonEntity dragon, Vec3 impactPoint, double radius) {
        DragonUtilities.accelerateCooking(level, dragon, impactPoint, radius,
                FIRE_BODY_FURNACE_BOOST_TICKS,
                FIRE_BODY_SMOKER_BOOST_TICKS,
                FIRE_BODY_BLAST_FURNACE_BOOST_TICKS,
                FIRE_BODY_FURNACE_LIT_TICKS);
    }

    private static void meltBlocks(ServerLevel level, Vec3 impactPoint, double radius, int requiredTicks) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        long currentTick = level.getGameTime();
        Set<BlockPos> exposedThisTick = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > radius * radius || !level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !canMeltBlock(level, pos, state)) {
                continue;
            }

            BlockPos immutablePos = pos.immutable();
            exposedThisTick.add(immutablePos);
            int progress = blockMeltProgress.getOrDefault(immutablePos, 0);
            progress++;
            blockMeltProgress.put(immutablePos, progress);
            lastExposureTick.put(immutablePos, currentTick);

            // Break block once it's been exposed long enough
            if (progress >= requiredTicks) {
                level.destroyBlock(immutablePos, true);
                blockMeltProgress.remove(immutablePos);
                lastExposureTick.remove(immutablePos);
            }
        }
        cleanupStaleProgress(currentTick);
    }
    private static void cleanupStaleProgress(long currentTick) {
        lastExposureTick.entrySet().removeIf(entry -> {
            long lastTick = entry.getValue();
            boolean isStale = (currentTick - lastTick) > 20;
            if (isStale) {
                blockMeltProgress.remove(entry.getKey());
            }
            return isStale;
        });
    }

    private static boolean canMeltBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return state.getBlock() != Blocks.BEDROCK
                && state.getBlock() != Blocks.BARRIER
                && state.getBlock() != Blocks.COMMAND_BLOCK
                && state.getBlock() != Blocks.CHAIN_COMMAND_BLOCK
                && state.getBlock() != Blocks.REPEATING_COMMAND_BLOCK
                && state.getBlock() != Blocks.STRUCTURE_BLOCK
                && state.getBlock() != Blocks.JIGSAW
                && state.getBlock() != Blocks.END_PORTAL
                && state.getBlock() != Blocks.END_PORTAL_FRAME
                && state.getBlock() != Blocks.END_GATEWAY;
    }
}
