package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Supplier;

public class DragonSwimWanderGoal extends Goal {
    private static final int RANDOM_TARGET_ATTEMPTS = 6;

    private final Mob mob;
    private final Supplier<AsyncSwimController> swimController;
    private final float turnSpeed;
    private final double swimSpeed;
    private final int interval;

    private Vec3 targetPos;
    private int recalcTimer;
    private int obstructionCheckCooldown;
    private boolean cachedObstructionResult;

    public DragonSwimWanderGoal(Mob mob, Supplier<AsyncSwimController> swimController, float turnSpeedDegrees, double swimSpeed, int interval) {
        this.mob = mob;
        this.swimController = swimController;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.interval = interval;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!canUseSwimMovement() || !mob.isInWaterOrBubble() || mob.isVehicle() || mob.getTarget() != null || isAerialDragonState()) {
            return false;
        }
        if (!canWanderForCommand()) {
            return false;
        }

        if (mob.getRandom().nextInt(interval) != 0) {
            return false;
        }

        this.targetPos = findRandomSwimTarget();
        return targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUseSwimMovement() || !mob.isInWaterOrBubble() || mob.isVehicle() || mob.getTarget() != null || isAerialDragonState()) {
            return false;
        }
        if (!canWanderForCommand()) {
            return false;
        }
        return targetPos != null && mob.distanceToSqr(targetPos) > 4.0D;
    }

    @Override
    public void start() {
        this.recalcTimer = 0;
        this.obstructionCheckCooldown = 0;
        this.cachedObstructionResult = false;
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        AsyncSwimController controller = swimController.get();
        if (controller != null) {
            controller.stop();
        }
    }

    @Override
    public void tick() {
        if (targetPos == null || isAerialDragonState()) {
            stop();
            return;
        }

        mob.getNavigation().stop();

        if (++recalcTimer > 100) {
            Vec3 newTarget = findRandomSwimTarget();
            if (newTarget != null) {
                targetPos = newTarget;
            }
            recalcTimer = 0;
        }

        if (obstructionCheckCooldown <= 0) {
            cachedObstructionResult = isLineObstructed(mob.position(), targetPos);
            obstructionCheckCooldown = 5;
        } else {
            obstructionCheckCooldown--;
        }

        if (cachedObstructionResult) {
            Vec3 newTarget = findRandomSwimTarget();
            if (newTarget != null) {
                targetPos = newTarget;
            }
        }

        double speed = swimSpeed;
        if (mob instanceof SemiAquaticDragon dragon) {
            speed = dragon.getSwimSpeed() * swimSpeed;
        }
        AsyncSwimController controller = swimController.get();
        if (controller == null) {
            return;
        }
        if (!controller.trackTarget(targetPos, speed, turnSpeed)) {
            Vec3 newTarget = findRandomSwimTarget();
            if (newTarget != null) {
                targetPos = newTarget;
            }
            return;
        }
        controller.serverTick();
    }

    private Vec3 findRandomSwimTarget() {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < RANDOM_TARGET_ATTEMPTS; attempt++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 32.0D + mob.getRandom().nextDouble() * 64.0D;
            int targetBlockX = Mth.floor(mob.getX() + Math.cos(angle) * distance);
            int targetBlockZ = Mth.floor(mob.getZ() + Math.sin(angle) * distance);
            int startY = Mth.floor(mob.getY());

            if (!isBlockAreaLoaded(targetBlockX, targetBlockZ, targetBlockX, targetBlockZ)) {
                continue;
            }

            cursor.set(targetBlockX, startY, targetBlockZ);
            int surfaceY = startY;
            while (cursor.getY() < mob.level().getMaxBuildHeight()
                    && mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                surfaceY = cursor.getY();
                cursor.move(0, 1, 0);
            }

            cursor.setY(startY);
            int bottomY = startY;
            while (cursor.getY() > mob.level().getMinBuildHeight()
                    && mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                bottomY = cursor.getY();
                cursor.move(0, -1, 0);
            }

            if (surfaceY == bottomY) {
                continue;
            }

            int minY = bottomY + 3;
            int maxY = surfaceY - 1;
            if (minY >= maxY) {
                continue;
            }

            int targetY = mob.getRandom().nextFloat() < 0.25F
                    ? Math.max(minY, maxY - 2)
                    : minY + mob.getRandom().nextInt(maxY - minY + 1);

            cursor.set(targetBlockX, targetY, targetBlockZ);
            if (!mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                continue;
            }

            return new Vec3(targetBlockX + 0.5D, targetY, targetBlockZ + 0.5D);
        }
        return null;
    }

    private boolean isLineObstructed(Vec3 from, Vec3 to) {
        if (!isRayAreaLoaded(from, to)) {
            return true;
        }
        HitResult hit = mob.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        return hit.getType() != HitResult.Type.MISS;
    }

    private boolean isRayAreaLoaded(Vec3 from, Vec3 to) {
        int minX = Mth.floor(Math.min(from.x, to.x)) - 1;
        int maxX = Mth.floor(Math.max(from.x, to.x)) + 1;
        int minZ = Mth.floor(Math.min(from.z, to.z)) - 1;
        int maxZ = Mth.floor(Math.max(from.z, to.z)) + 1;
        return isBlockAreaLoaded(minX, minZ, maxX, maxZ);
    }

    private boolean isBlockAreaLoaded(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int minChunkX = SectionPos.blockToSectionCoord(Math.min(minBlockX, maxBlockX));
        int maxChunkX = SectionPos.blockToSectionCoord(Math.max(minBlockX, maxBlockX));
        int minChunkZ = SectionPos.blockToSectionCoord(Math.min(minBlockZ, maxBlockZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Math.max(minBlockZ, maxBlockZ));

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!isChunkLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isChunkLoaded(int chunkX, int chunkZ) {
        int sampleX = (chunkX << 4) + 8;
        int sampleZ = (chunkZ << 4) + 8;
        return mob.level().hasChunkAt(new BlockPos(sampleX, Mth.floor(mob.getY()), sampleZ));
    }

    private boolean isAerialDragonState() {
        return mob instanceof RideableDragonBase dragon && dragon.isAerial();
    }

    private boolean canUseSwimMovement() {
        return !(mob instanceof DragonMovementCapable dragon) || dragon.canSwim();
    }

    private boolean canWanderForCommand() {
        if (!(mob instanceof RideableDragonBase dragon) || !dragon.isTame()) {
            return true;
        }
        return dragon.getCommand() == 2;
    }
}
