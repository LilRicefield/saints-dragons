package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class VolitansFindSleepDepthGoal extends Goal {
    private static final int TARGET_ATTEMPTS = 24;
    private static final int HORIZONTAL_RADIUS = 14;
    private static final int DOWN_SCAN_BLOCKS = 24;
    private static final int COOLDOWN_TICKS = 80;
    private static final double ARRIVAL_DISTANCE_SQR = 3.0D * 3.0D;
    private static final int FLOOR_CLEARANCE_BLOCKS = 2;

    private final Volitans dragon;
    private final Supplier<AsyncSwimController> swimController;
    private final float turnSpeed;
    private final double swimSpeed;
    private Vec3 targetPos;
    private int cooldown;

    public VolitansFindSleepDepthGoal(Volitans dragon, Supplier<AsyncSwimController> swimController, float turnSpeedDegrees, double swimSpeed) {
        this.dragon = dragon;
        this.swimController = swimController;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!dragon.shouldSeekUnderwaterSleepDepth()) {
            return false;
        }

        this.targetPos = findSleepDepthTarget();
        if (targetPos == null) {
            cooldown = COOLDOWN_TICKS;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPos != null
                && dragon.shouldSeekUnderwaterSleepDepth()
                && dragon.distanceToSqr(targetPos) > ARRIVAL_DISTANCE_SQR;
    }

    @Override
    public void start() {
        dragon.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        AsyncSwimController controller = swimController.get();
        if (controller != null) {
            controller.stop();
        }
        cooldown = 20;
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        dragon.getNavigation().stop();
        AsyncSwimController controller = swimController.get();
        if (controller == null) {
            return;
        }
        if (!controller.trackTarget(targetPos, dragon.getSwimSpeed() * swimSpeed, turnSpeed)) {
            Vec3 newTarget = findSleepDepthTarget();
            if (newTarget != null) {
                targetPos = newTarget;
            }
            return;
        }
        controller.serverTick();
    }

    private Vec3 findSleepDepthTarget() {
        BlockPos origin = dragon.blockPosition();
        Vec3 currentColumnTarget = findSleepDepthTargetInColumn(origin.getX(), origin.getZ(), origin.getY());
        if (currentColumnTarget != null) {
            return currentColumnTarget;
        }

        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = origin.getX() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int z = origin.getZ() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            Vec3 target = findSleepDepthTargetInColumn(x, z, origin.getY());
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private Vec3 findSleepDepthTargetInColumn(int x, int z, int originY) {
        if (!dragon.level().hasChunkAt(new BlockPos(x, originY, z))) {
            return null;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, originY, z);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 1, originY + 8);
        int surfaceY = findSurfaceY(cursor, maxY);
        if (surfaceY == Integer.MIN_VALUE) {
            return null;
        }

        int minScanY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - DOWN_SCAN_BLOCKS);
        cursor.set(x, originY, z);
        int bottomY = findBottomY(cursor, minScanY);
        int minTargetY = bottomY + FLOOR_CLEARANCE_BLOCKS;
        int maxTargetY = Math.min(originY - 1, surfaceY - Mth.ceil(dragon.getBbHeight()) - 10);
        if (maxTargetY < minTargetY) {
            return null;
        }

        for (int y = maxTargetY; y >= minTargetY; y--) {
            Vec3 target = new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
            if (hasBodyWaterClearance(x, y, z) && dragon.isDeepEnoughForUnderwaterSleepAt(target)) {
                return target;
            }
        }
        return null;
    }

    private int findSurfaceY(BlockPos.MutableBlockPos cursor, int maxY) {
        boolean foundWater = false;
        for (int y = cursor.getY(); y <= maxY; y++) {
            cursor.setY(y);
            if (dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                foundWater = true;
                continue;
            }
            return foundWater ? y : Integer.MIN_VALUE;
        }
        return foundWater ? maxY + 1 : Integer.MIN_VALUE;
    }

    private int findBottomY(BlockPos.MutableBlockPos cursor, int minY) {
        int bottomY = cursor.getY();
        for (int y = cursor.getY(); y >= minY; y--) {
            cursor.setY(y);
            if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)
                    || !dragon.level().getBlockState(cursor).getCollisionShape(dragon.level(), cursor).isEmpty()) {
                return y + 1;
            }
            bottomY = y;
        }
        return bottomY;
    }

    private boolean hasBodyWaterClearance(int x, int y, int z) {
        int clearanceRadius = Math.max(0, Mth.ceil(dragon.getBbWidth() * 0.5F - 0.25F));
        int height = Math.max(1, Mth.ceil(dragon.getBbHeight()));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -clearanceRadius; dx <= clearanceRadius; dx++) {
                for (int dz = -clearanceRadius; dz <= clearanceRadius; dz++) {
                    if (dx * dx + dz * dz > clearanceRadius * clearanceRadius) {
                        continue;
                    }
                    cursor.set(x + dx, y + dy, z + dz);
                    if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)
                            || !dragon.level().getBlockState(cursor).getCollisionShape(dragon.level(), cursor).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
