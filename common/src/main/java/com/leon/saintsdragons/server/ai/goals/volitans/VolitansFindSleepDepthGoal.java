package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class VolitansFindSleepDepthGoal extends Goal {
    private static final int TARGET_ATTEMPTS = 12;
    private static final int HORIZONTAL_RADIUS = 10;
    private static final int DOWN_SCAN_BLOCKS = 14;
    private static final int COOLDOWN_TICKS = 80;
    private static final double ARRIVAL_DISTANCE_SQR = 3.0D * 3.0D;

    private final Volitans dragon;
    private final float turnSpeed;
    private final double swimSpeed;
    private Vec3 targetPos;
    private double currentYaw;
    private double currentPitch;
    private int cooldown;

    public VolitansFindSleepDepthGoal(Volitans dragon, float turnSpeedDegrees, double swimSpeed) {
        this.dragon = dragon;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.currentYaw = dragon.getYRot();
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
        this.currentYaw = dragon.getYRot();
        this.currentPitch = dragon.getXRot();
        dragon.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        Vec3 velocity = dragon.getDeltaMovement();
        dragon.setDeltaMovement(velocity.x * 0.6D, velocity.y * 0.6D, velocity.z * 0.6D);
        cooldown = 20;
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        dragon.getNavigation().stop();

        double dx = targetPos.x - dragon.getX();
        double dy = targetPos.y - (dragon.getY() + dragon.getEyeHeight() * 0.5D);
        double dz = targetPos.z - dragon.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double targetYaw = Math.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0D;
        double targetPitch = -(Math.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -85.0D, 85.0D);

        double yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        yawDelta = Mth.clamp(yawDelta, -turnSpeed, turnSpeed);
        currentYaw = Mth.wrapDegrees(currentYaw + yawDelta);

        double pitchDelta = targetPitch - currentPitch;
        pitchDelta = Mth.clamp(pitchDelta, -turnSpeed * 0.5D, turnSpeed * 0.5D);
        currentPitch += pitchDelta;

        dragon.setYRot((float) currentYaw);
        dragon.yBodyRot = (float) currentYaw;
        dragon.yHeadRot = (float) currentYaw;
        dragon.setXRot((float) currentPitch);

        double yawRad = currentYaw * Mth.DEG_TO_RAD;
        double pitchRad = currentPitch * Mth.DEG_TO_RAD;
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        double speed = dragon.getSwimSpeed() * swimSpeed;

        dragon.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
    }

    private Vec3 findSleepDepthTarget() {
        BlockPos origin = dragon.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = origin.getX() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int z = origin.getZ() + dragon.getRandom().nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int minY = Math.max(dragon.level().getMinBuildHeight() + 1, origin.getY() - DOWN_SCAN_BLOCKS);

            for (int y = origin.getY() - 1; y >= minY; y--) {
                cursor.set(x, y, z);
                if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                    continue;
                }
                if (!dragon.level().getBlockState(cursor).getCollisionShape(dragon.level(), cursor).isEmpty()) {
                    continue;
                }

                Vec3 target = new Vec3(x + 0.5D, y + 0.1D, z + 0.5D);
                if (dragon.isDeepEnoughForUnderwaterSleepAt(target)) {
                    return target;
                }
            }
        }
        return null;
    }
}
