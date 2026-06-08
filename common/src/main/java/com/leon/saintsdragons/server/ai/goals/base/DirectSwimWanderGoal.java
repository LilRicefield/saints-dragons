package com.leon.saintsdragons.server.ai.goals.base;

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

public class DirectSwimWanderGoal extends Goal {
    private static final int RANDOM_TARGET_ATTEMPTS = 6;

    private final Mob mob;
    private final float turnSpeed;
    private final double swimSpeed;
    private final int interval;
    private final boolean preferShore;

    private Vec3 targetPos;
    private double currentYaw;
    private double currentPitch;
    private int recalcTimer;
    private int obstructionCheckCooldown;
    private boolean cachedObstructionResult;

    public DirectSwimWanderGoal(Mob mob, float turnSpeedDegrees, double swimSpeed, int interval) {
        this(mob, turnSpeedDegrees, swimSpeed, interval, false);
    }

    public DirectSwimWanderGoal(Mob mob, float turnSpeedDegrees, double swimSpeed, int interval, boolean preferShore) {
        this.mob = mob;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.interval = interval;
        this.preferShore = preferShore;
        this.currentYaw = mob.getYRot();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!canUseSwimMovement() || !mob.isInWaterOrBubble() || mob.isVehicle() || mob.getTarget() != null || isAerialDragonState()) {
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
        if (!isAerialDragonState()) {
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.targetPos = null;
        if (isAerialDragonState()) {
            return;
        }
        Vec3 vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.8, vel.y * 0.8, vel.z * 0.8);
    }

    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }

        if (isAerialDragonState()) {
            stop();
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
        double vx = dirX * speed;
        double vy = dirY * speed;
        double vz = dirZ * speed;

        // Shore-preferring land dragons need extra "hop" to climb out at the lip.
        if (preferShore && mob.isInWaterOrBubble()) {
            // Ignivorus needs a bit more vertical help to reliably clear shoreline lips.
            double collisionHop = 0.32D;
            double upwardAssist = 0.18D;
            double upwardAssistThreshold = 0.5D;
            double yDiff = targetPos != null ? (targetPos.y - mob.getY()) : 0.0D;
            if (mob.horizontalCollision) {
                vy = Math.max(vy, collisionHop);
            } else if (targetPos != null) {
                if (yDiff > upwardAssistThreshold) {
                    vy = Math.max(vy, upwardAssist);
                }
            }
        }

        mob.setDeltaMovement(vx, vy, vz);
    }

    private Vec3 findRandomSwimTarget() {
        if (preferShore) {
            Vec3 shore = findNearbyShoreTarget();
            if (shore != null) {
                return shore;
            }
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < RANDOM_TARGET_ATTEMPTS; attempt++) {
            // Pick random horizontal position (32-96 blocks away)
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 32.0 + mob.getRandom().nextDouble() * 64.0;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            int targetBlockX = Mth.floor(mob.getX() + offsetX);
            int targetBlockZ = Mth.floor(mob.getZ() + offsetZ);
            int startY = Mth.floor(mob.getY());
            cursor.set(targetBlockX, startY, targetBlockZ);

            // Never trigger synchronous chunk loading from AI goals on server thread.
            if (!isBlockAreaLoaded(targetBlockX, targetBlockZ, targetBlockX, targetBlockZ)) {
                continue;
            }

            // Find surface
            int surfaceY = startY;
            while (cursor.getY() < mob.level().getMaxBuildHeight()
                    && mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                surfaceY = cursor.getY();
                cursor.move(0, 1, 0);
            }

            // Find bottom
            cursor.setY(startY);
            int bottomY = startY;
            while (cursor.getY() > mob.level().getMinBuildHeight()
                    && mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                bottomY = cursor.getY();
                cursor.move(0, -1, 0);
            }

            // No water at target
            if (surfaceY == bottomY) {
                continue;
            }

            // Pick random depth (favor mid-depth, avoid very bottom)
            int minY = bottomY + 3;
            int maxY = surfaceY - 1;
            if (minY >= maxY) {
                continue; // Too shallow
            }

            // 25% chance to go near surface
            int targetY;
            if (mob.getRandom().nextFloat() < 0.25F) {
                targetY = Math.max(minY, maxY - 2);
            } else {
                targetY = minY + mob.getRandom().nextInt(maxY - minY + 1);
            }

            // Verify it's still water
            cursor.set(targetBlockX, targetY, targetBlockZ);
            if (!mob.level().getFluidState(cursor).is(FluidTags.WATER)) {
                continue;
            }

            return new Vec3(targetBlockX + 0.5D, targetY, targetBlockZ + 0.5D);
        }
        return null;
    }

    private Vec3 findNearbyShoreTarget() {
        BlockPos start = mob.blockPosition();
        int maxRadius = 28;

        for (int radius = 4; radius <= maxRadius; radius += 4) {
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                int x = start.getX() + (int) (Math.cos(rad) * radius);
                int z = start.getZ() + (int) (Math.sin(rad) * radius);
                if (!isBlockAreaLoaded(x - 1, z - 1, x + 1, z + 1)) {
                    continue;
                }

                int groundY = mob.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                BlockPos ground = new BlockPos(x, groundY, z);

                if (!mob.level().getFluidState(ground).isEmpty()) {
                    continue;
                }
                if (!(mob.level().getBlockState(ground).isSolid() || mob.level().getBlockState(ground.below()).isSolid())) {
                    continue;
                }

                // If any adjacent block has water, this is a shoreline candidate.
                boolean shoreline = false;
                for (int dx = -1; dx <= 1 && !shoreline; dx++) {
                    for (int dz = -1; dz <= 1 && !shoreline; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        BlockPos neighbor = ground.offset(dx, 0, dz);
                        if (mob.level().getFluidState(neighbor).is(FluidTags.WATER)
                                || mob.level().getFluidState(neighbor.below()).is(FluidTags.WATER)) {
                            shoreline = true;
                        }
                    }
                }

                if (shoreline) {
                    // Aim just above shoreline lip; too-high targets cause bobbing against banks.
                    return new Vec3(x + 0.5D, groundY + 0.15D, z + 0.5D);
                }
            }
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
        return preferShore || !(mob instanceof DragonMovementCapable dragon) || dragon.canSwim();
    }
}
