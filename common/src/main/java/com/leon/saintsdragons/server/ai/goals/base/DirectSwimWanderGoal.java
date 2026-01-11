package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Direct wandering for swimming creatures.
 * Picks random underwater positions and swims directly toward them.
 */
public class DirectSwimWanderGoal extends Goal {

    private final Mob mob;
    private final float turnSpeed;
    private final double swimSpeed;
    private final int interval;

    private Vec3 targetPos;
    private double currentYaw;
    private double currentPitch;
    private int recalcTimer;
    private int obstructionCheckCooldown;
    private boolean cachedObstructionResult;

    public DirectSwimWanderGoal(Mob mob, float turnSpeedDegrees, double swimSpeed, int interval) {
        this.mob = mob;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.interval = interval;
        this.currentYaw = mob.getYRot();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only use when in water, not being ridden, and no target
        if (!mob.isInWaterOrBubble() || mob.isVehicle() || mob.getTarget() != null) {
            return false;
        }

        // Random chance to trigger
        if (mob.getRandom().nextInt(interval) != 0) {
            return false;
        }

        // Try to find a random position
        this.targetPos = findRandomSwimTarget();
        return targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isInWaterOrBubble() || mob.isVehicle() || mob.getTarget() != null) {
            return false;
        }

        if (targetPos == null) {
            return false;
        }

        // Stop if we reached the target
        double dist = mob.distanceToSqr(targetPos);
        return dist > 4.0; // Within 2 blocks = close enough
    }

    @Override
    public void start() {
        this.currentYaw = mob.getYRot();
        this.currentPitch = 0.0;
        this.recalcTimer = 0;
        this.obstructionCheckCooldown = 0;
        this.cachedObstructionResult = false;
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        // Gradually slow down
        Vec3 vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.8, vel.y * 0.8, vel.z * 0.8);
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        this.mob.getNavigation().stop();

        // Periodically recalculate target to add variation
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

        // Calculate direction to target
        double dx = targetPos.x - mob.getX();
        double dy = targetPos.y - (mob.getY() + mob.getEyeHeight() * 0.5);
        double dz = targetPos.z - mob.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Calculate target yaw and pitch
        double targetYaw = Math.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0;
        double targetPitch = -(Math.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -85.0, 85.0);

        // Smooth rotation
        double yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        yawDelta = Mth.clamp(yawDelta, -turnSpeed, turnSpeed);
        currentYaw = Mth.wrapDegrees(currentYaw + yawDelta);

        double pitchDelta = targetPitch - currentPitch;
        pitchDelta = Mth.clamp(pitchDelta, -turnSpeed * 0.5, turnSpeed * 0.5);
        currentPitch += pitchDelta;

        // Apply rotation
        mob.setYRot((float) currentYaw);
        mob.yBodyRot = (float) currentYaw;
        mob.yHeadRot = (float) currentYaw;
        mob.setXRot((float) currentPitch);

        // Calculate velocity from rotation
        double yawRad = currentYaw * Mth.DEG_TO_RAD;
        double pitchRad = currentPitch * Mth.DEG_TO_RAD;

        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        // Apply velocity
        double speed = swimSpeed;
        if (mob instanceof SemiAquaticDragon dragon) {
            speed = dragon.getSwimSpeed() * swimSpeed;
        }
        mob.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
    }

    private Vec3 findRandomSwimTarget() {
        // Pick random horizontal position (32-96 blocks away)
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 32.0 + mob.getRandom().nextDouble() * 64.0;
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        double targetX = mob.getX() + offsetX;
        double targetZ = mob.getZ() + offsetZ;

        // Find water column at target position
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(targetX, mob.getY(), targetZ);

        // Find surface
        int surfaceY = (int) mob.getY();
        while (mob.level().getFluidState(cursor).is(FluidTags.WATER) &&
               cursor.getY() < mob.level().getMaxBuildHeight()) {
            surfaceY = cursor.getY();
            cursor.move(0, 1, 0);
        }

        // Find bottom
        cursor.setY((int) mob.getY());
        int bottomY = (int) mob.getY();
        while (mob.level().getFluidState(cursor).is(FluidTags.WATER) &&
               cursor.getY() > mob.level().getMinBuildHeight()) {
            bottomY = cursor.getY();
            cursor.move(0, -1, 0);
        }

        // No water at target
        if (surfaceY == bottomY) {
            return null;
        }

        // Pick random depth (favor mid-depth, avoid very bottom)
        int minY = bottomY + 3;
        int maxY = surfaceY - 1;

        if (minY >= maxY) {
            return null; // Too shallow
        }

        // 25% chance to go near surface
        int targetY;
        if (mob.getRandom().nextFloat() < 0.25F) {
            targetY = Math.max(minY, maxY - 2);
        } else {
            // Random depth
            targetY = minY + mob.getRandom().nextInt(maxY - minY + 1);
        }

        // Verify it's water
        cursor.set(targetX, targetY, targetZ);
        if (!mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
            return null;
        }

        return new Vec3(targetX, targetY, targetZ);
    }

    private boolean isLineObstructed(Vec3 from, Vec3 to) {
        HitResult hit = mob.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        return hit.getType() != HitResult.Type.MISS;
    }
}
