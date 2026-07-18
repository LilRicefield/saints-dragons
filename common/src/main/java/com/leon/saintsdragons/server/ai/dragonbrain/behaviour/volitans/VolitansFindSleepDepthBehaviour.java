package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class VolitansFindSleepDepthBehaviour extends DragonBehaviour<Volitans> {
    private static final int TARGET_ATTEMPTS = 24;
    private static final int HORIZONTAL_RADIUS = 14;
    private static final int DOWN_SCAN_BLOCKS = 24;
    private static final int COOLDOWN_TICKS = 80;
    private static final double ARRIVAL_DISTANCE_SQR = 9.0D;
    private static final int FLOOR_CLEARANCE_BLOCKS = 2;

    private final float turnSpeed;
    private final double swimSpeed;
    @Nullable
    private Vec3 target;
    private int cooldown;

    public VolitansFindSleepDepthBehaviour(float turnSpeed, double swimSpeed) {
        this.turnSpeed = turnSpeed;
        this.swimSpeed = swimSpeed;
    }

    @Override
    protected boolean canStart(DragonBrainContext<Volitans> context) {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        Volitans dragon = context.dragon();
        if (dragon.isInLove() || !dragon.shouldSeekUnderwaterSleepDepth()) {
            return false;
        }
        target = findSleepDepthTarget(dragon);
        if (target == null) {
            cooldown = COOLDOWN_TICKS;
            return false;
        }
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Volitans> context) {
        Volitans dragon = context.dragon();
        return target != null
                && !dragon.isInLove()
                && dragon.shouldSeekUnderwaterSleepDepth()
                && dragon.distanceToSqr(target) > ARRIVAL_DISTANCE_SQR;
    }

    @Override
    protected void start(DragonBrainContext<Volitans> context) {
        context.dragon().getNavigation().stop();
    }

    @Override
    protected void tick(DragonBrainContext<Volitans> context) {
        Volitans dragon = context.dragon();
        if (target == null) {
            return;
        }
        dragon.getNavigation().stop();
        AsyncSwimController controller = dragon.getAiSwimController();
        if (!controller.trackTarget(target, dragon.getSwimSpeed() * swimSpeed, turnSpeed)) {
            Vec3 replacement = findSleepDepthTarget(dragon);
            if (replacement != null) {
                target = replacement;
            }
            return;
        }
        controller.serverTick();
    }

    @Override
    protected void stop(DragonBrainContext<Volitans> context) {
        target = null;
        context.dragon().getAiSwimController().stop();
        cooldown = 20;
    }

    private Vec3 findSleepDepthTarget(Volitans dragon) {
        BlockPos origin = dragon.blockPosition();
        Vec3 sameColumn = findSleepDepthTargetInColumn(dragon, origin.getX(), origin.getZ(), origin.getY());
        if (sameColumn != null) {
            return sameColumn;
        }
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = origin.getX() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int z = origin.getZ() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            Vec3 candidate = findSleepDepthTargetInColumn(dragon, x, z, origin.getY());
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private Vec3 findSleepDepthTargetInColumn(Volitans dragon, int x, int z, int originY) {
        BlockPos column = new BlockPos(x, originY, z);
        if (!dragon.level().hasChunkAt(column)) {
            return null;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, originY, z);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 1, originY + 8);
        int surfaceY = findSurfaceY(dragon, cursor, maxY);
        if (surfaceY == Integer.MIN_VALUE) {
            return null;
        }
        int minScanY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - DOWN_SCAN_BLOCKS);
        cursor.set(x, originY, z);
        int bottomY = findBottomY(dragon, cursor, minScanY);
        int minTargetY = bottomY + FLOOR_CLEARANCE_BLOCKS;
        int maxTargetY = Math.min(originY - 1, surfaceY - Mth.ceil(dragon.getBbHeight()) - 10);
        for (int y = maxTargetY; y >= minTargetY; y--) {
            Vec3 candidate = new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
            if (hasBodyWaterClearance(dragon, x, y, z)
                    && dragon.isDeepEnoughForUnderwaterSleepAt(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private int findSurfaceY(Volitans dragon, BlockPos.MutableBlockPos cursor, int maxY) {
        boolean foundWater = false;
        for (int y = cursor.getY(); y <= maxY; y++) {
            cursor.setY(y);
            if (dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                foundWater = true;
            } else {
                return foundWater ? y : Integer.MIN_VALUE;
            }
        }
        return foundWater ? maxY + 1 : Integer.MIN_VALUE;
    }

    private int findBottomY(Volitans dragon, BlockPos.MutableBlockPos cursor, int minY) {
        int bottomY = cursor.getY();
        for (int y = cursor.getY(); y >= minY; y--) {
            cursor.setY(y);
            if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)
                    || !dragon.level().getBlockState(cursor)
                    .getCollisionShape(dragon.level(), cursor).isEmpty()) {
                return y + 1;
            }
            bottomY = y;
        }
        return bottomY;
    }

    private boolean hasBodyWaterClearance(Volitans dragon, int x, int y, int z) {
        int radius = Math.max(0, Mth.ceil(dragon.getBbWidth() * 0.5F - 0.25F));
        int height = Math.max(1, Mth.ceil(dragon.getBbHeight()));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(x + dx, y + dy, z + dz);
                    if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)
                            || !dragon.level().getBlockState(cursor)
                            .getCollisionShape(dragon.level(), cursor).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "target", target == null ? "none" : target.toString(),
                "cooldown", Integer.toString(cooldown)
        );
    }
}
