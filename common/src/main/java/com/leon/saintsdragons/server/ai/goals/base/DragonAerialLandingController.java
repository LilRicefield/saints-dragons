package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class DragonAerialLandingController<T extends Mob & DragonFlightCapable> {
    private static final int LANDING_FORCE_DROP_TICKS = 80;
    private static final int LANDING_RETARGET_TICKS = 120;
    private static final double LANDING_STATE_ALTITUDE = 1.5D;

    private final T dragon;
    private final double landingBlendAltitude;
    private final Runnable onLandingComplete;

    private Vec3 landingPosition;
    private boolean landingApproach;
    private int landingApproachTicks;
    private boolean landingForceDrop;

    public DragonAerialLandingController(T dragon, double landingBlendAltitude, Runnable onLandingComplete) {
        this.dragon = dragon;
        this.landingBlendAltitude = landingBlendAltitude;
        this.onLandingComplete = onLandingComplete;
    }

    public boolean isLandingApproachActive() {
        return landingApproach;
    }

    public void beginLandingApproach() {
        if (landingApproach) {
            return;
        }

        landingPosition = findLandingTarget(16, 24);
        if (landingPosition == null) {
            return;
        }

        landingApproach = true;
        landingApproachTicks = 0;
        landingForceDrop = false;
        dragon.setHovering(false);
        dragon.setTakeoff(false);
    }

    public void tickLandingApproach() {
        if (!landingApproach) {
            return;
        }

        // Keep hover disabled while AI is committing to a landing run.
        dragon.setHovering(false);

        if (dragon.onGround()) {
            finishLanding();
            return;
        }

        if (dragon.isInWaterOrBubble()) {
            abortInWater();
            return;
        }

        landingApproachTicks++;

        if (landingApproachTicks > LANDING_RETARGET_TICKS) {
            if (!tryRetargetLanding(true) && !tryRetargetLanding(false)) {
                abortLandingApproach();
                return;
            }
            landingApproachTicks = 0;
        }

        if (!landingForceDrop && landingApproachTicks > LANDING_FORCE_DROP_TICKS) {
            landingForceDrop = true;
            if (!tryRetargetLanding(true) && !tryRetargetLanding(false)) {
                abortLandingApproach();
                return;
            }
        }

        if (landingPosition == null) {
            if (!tryRetargetLanding(landingForceDrop)) {
                abortLandingApproach();
            }
            return;
        }

        BlockPos landingGround = BlockPos.containing(landingPosition.x, landingPosition.y - 1.0, landingPosition.z);
        if (!landingForceDrop && !isWideLandingSurface(landingGround)) {
            if (!tryRetargetLanding(false)) {
                abortLandingApproach();
            }
            return;
        }

        double altitude = dragon.getY() - landingPosition.y;

        if (!dragon.onGround()) {
            Vec3 motion = dragon.getDeltaMovement();
            double descentRate = altitude > landingBlendAltitude ? 0.12D : 0.06D;
            double maxFallSpeed = altitude > landingBlendAltitude ? -1.2D : -0.75D;
            double newY = Math.max(motion.y - descentRate, maxFallSpeed);
            dragon.setDeltaMovement(motion.x, newY, motion.z);
        }

        double moveSpeed = altitude > landingBlendAltitude ? 1.35D : (altitude > 2.0D ? 1.05D : 0.95D);
        dragon.getMoveControl().setWantedPosition(landingPosition.x, landingPosition.y, landingPosition.z, moveSpeed);

        double landingStartAltitude = Math.max(2.0D, Math.min(landingBlendAltitude, 5.0D));
        if (!dragon.isLanding() && altitude >= -0.5D && altitude <= landingStartAltitude) {
            double dx = dragon.getX() - landingPosition.x;
            double dz = dragon.getZ() - landingPosition.z;
            double horizontalDistSq = dx * dx + dz * dz;
            double landingDistSq = altitude <= LANDING_STATE_ALTITUDE ? 4.0D : 9.0D;
            if (horizontalDistSq <= landingDistSq) {
                dragon.setLanding(true);
            }
        }

        if (landingPosition != null) {
            double dx = dragon.getX() - landingPosition.x;
            double dz = dragon.getZ() - landingPosition.z;
            double horizontalDistSq = dx * dx + dz * dz;
            boolean nearTouchdown = altitude <= 0.7D && altitude >= -1.0D && horizontalDistSq <= 6.25D;
            BlockPos supportPos = BlockPos.containing(landingPosition.x, landingPosition.y - 1.0D, landingPosition.z);
            boolean hasSupport = dragon.level().hasChunkAt(supportPos)
                    && !dragon.level().getBlockState(supportPos).isAir()
                    && dragon.level().getBlockState(supportPos).getFluidState().isEmpty();
            if (nearTouchdown && hasSupport) {
                finishLanding();
            }
        }
    }

    public void finishLanding() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        landingPosition = null;
        onLandingComplete.run();
        dragon.setHovering(false);
        dragon.setFlying(false);
    }

    public void reset() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        landingPosition = null;
    }

    private void abortLandingApproach() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        landingPosition = null;
        dragon.setLanding(false);
    }

    private void abortInWater() {
        landingApproach = false;
        landingApproachTicks = 0;
        landingForceDrop = false;
        landingPosition = null;
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setTakeoff(false);
        dragon.setFlying(false);
    }

    private boolean tryRetargetLanding(boolean dropFallback) {
        Vec3 nextTarget = dropFallback ? findValidDropTarget() : findLandingTarget(20, 28);
        if (nextTarget == null && dropFallback) {
            nextTarget = findLandingTarget(24, 36);
        }
        if (nextTarget == null) {
            return false;
        }

        landingPosition = nextTarget;
        return true;
    }

    private Vec3 findLandingTarget(int radius, int attempts) {
        BlockPos origin = dragon.blockPosition();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int dx = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = dragon.getRandom().nextInt(radius * 2 + 1) - radius;
            BlockPos column = origin.offset(dx, 0, dz);
            if (!dragon.level().hasChunkAt(column)) {
                continue;
            }

            int surfaceY = dragon.level().getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
            BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
            if (isWideLandingSurface(ground)) {
                return new Vec3(column.getX() + 0.5, ground.getY() + 1.0, column.getZ() + 0.5);
            }
        }

        return null;
    }

    private Vec3 findValidDropTarget() {
        BlockPos origin = dragon.blockPosition();

        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                BlockPos checkPos = origin.offset(dx, 0, dz);

                if (!dragon.level().hasChunkAt(checkPos)) {
                    continue;
                }

                int surfaceY = dragon.level().getHeight(Heightmap.Types.WORLD_SURFACE, checkPos.getX(), checkPos.getZ());
                BlockPos groundPos = new BlockPos(checkPos.getX(), surfaceY - 1, checkPos.getZ());
                var state = dragon.level().getBlockState(groundPos);

                if (!state.isAir() && state.getFluidState().isEmpty() && state.isFaceSturdy(dragon.level(), groundPos, Direction.UP)) {
                    return new Vec3(checkPos.getX() + 0.5, groundPos.getY() + 1.0, checkPos.getZ() + 0.5);
                }
            }
        }

        return null;
    }

    private boolean isWideLandingSurface(BlockPos ground) {
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (!state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }
        return isLandingSpaceClear(ground);
    }

    private boolean isLandingSpaceClear(BlockPos ground) {
        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        if (!aboveState.getCollisionShape(dragon.level(), above).isEmpty() || !aboveState.getFluidState().isEmpty()) {
            return false;
        }
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty() && aboveTwoState.getFluidState().isEmpty();
    }
}
