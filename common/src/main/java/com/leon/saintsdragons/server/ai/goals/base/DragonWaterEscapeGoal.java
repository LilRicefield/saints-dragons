package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DragonWaterEscapeGoal<T extends DragonEntity> extends Goal {
    private static final int EXECUTION_CHANCE = 1;
    private static final int SEARCH_RADIUS = 24;
    private static final int VERTICAL_SEARCH = 8;
    private static final int SURFACE_SEARCH_UP = 32;
    private static final int TARGET_ATTEMPTS = 28;
    private static final int SHORE_RESCAN_TICKS = 20;
    private static final int ROAM_TARGET_REACHED_TICKS = 60;
    private static final int WATER_ROAM_MIN_DISTANCE = 12;
    private static final int WATER_ROAM_RANDOM_DISTANCE = 24;
    private static final double SHORE_ASSIST_DISTANCE_SQR = 7.0D * 7.0D;
    private static final double WATER_ROAM_ARRIVAL_DISTANCE_SQR = 4.0D * 4.0D;

    private final T dragon;
    private final float turnSpeed;
    private final double swimSpeed;
    private EscapeTarget target;
    private int cooldownTicks;
    private int shoreRescanTicks;
    private int roamTicks;

    public DragonWaterEscapeGoal(T dragon, float turnSpeedDegrees, double swimSpeed) {
        this.dragon = dragon;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }
        if (!shouldEscapeWater()) {
            return false;
        }
        if (dragon.getRandom().nextInt(EXECUTION_CHANCE) != 0) {
            return false;
        }

        this.target = findEscapeTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && shouldEscapeWater();
    }

    @Override
    public void start() {
        dragon.getNavigation().stop();
        this.shoreRescanTicks = 0;
        this.roamTicks = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        dragon.getAiSwimController().stop();
        cooldownTicks = 10;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        dragon.getNavigation().stop();
        updateShoreLock();
        preserveEscapeAir();

        AsyncSwimController controller = dragon.getAiSwimController();
        if (!controller.trackTarget(this.target.waterPosition(), this.swimSpeed, this.turnSpeed)) {
            EscapeTarget newTarget = findEscapeTarget();
            if (newTarget != null) {
                this.target = newTarget;
            }
            return;
        }
        controller.serverTick();

        if (this.target.isShoreTarget()
                && dragon.distanceToSqr(this.target.waterPosition()) <= SHORE_ASSIST_DISTANCE_SQR) {
            applyShoreAssist(this.target.landPosition());
            return;
        }

        if (!this.target.isShoreTarget()) {
            roamTicks++;
            if (roamTicks > ROAM_TARGET_REACHED_TICKS || dragon.distanceToSqr(this.target.waterPosition()) <= WATER_ROAM_ARRIVAL_DISTANCE_SQR) {
                EscapeTarget newTarget = findEscapeTarget();
                if (newTarget != null) {
                    this.target = newTarget;
                    this.roamTicks = 0;
                }
            }
        }
    }

    private boolean shouldEscapeWater() {
        if (dragon.canSwim() || dragon.isVehicle() || !dragon.isInWaterOrBubble()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (dragon instanceof RideableDragonBase rideableDragon
                && DragonAirCombatHelper.canUseAirCombat(rideableDragon, target, 64.0D)
                && DragonAirCombatHelper.isTargetHighEnoughForAiTakeoff(rideableDragon, target, 4.0D, 2.0D)) {
            return false;
        }
        return !(dragon instanceof RideableDragonBase rideableDragon) || !rideableDragon.isAerial();
    }

    private EscapeTarget findEscapeTarget() {
        BlockPos origin = dragon.blockPosition();
        EscapeTarget currentColumnTarget = findShoreTargetInColumn(origin.getX(), origin.getZ(), origin.getY());
        if (currentColumnTarget != null) {
            return currentColumnTarget;
        }

        EscapeTarget ringTarget = findNearestShoreTargetByRings(origin);
        if (ringTarget != null) {
            return ringTarget;
        }

        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = origin.getX() + dragon.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = origin.getZ() + dragon.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            EscapeTarget target = findShoreTargetInColumn(x, z, origin.getY());
            if (target != null) {
                return target;
            }
        }

        return findRandomWaterRoamTarget(origin);
    }

    private void updateShoreLock() {
        if (this.target != null && this.target.isShoreTarget()) {
            return;
        }
        if (shoreRescanTicks > 0) {
            shoreRescanTicks--;
            return;
        }
        shoreRescanTicks = SHORE_RESCAN_TICKS;

        EscapeTarget shoreTarget = findNearestShoreTargetByRings(dragon.blockPosition());
        if (shoreTarget != null) {
            this.target = shoreTarget;
            this.roamTicks = 0;
        }
    }

    private EscapeTarget findNearestShoreTargetByRings(BlockPos origin) {
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                EscapeTarget north = findShoreTargetInColumn(origin.getX() + dx, origin.getZ() - radius, origin.getY());
                if (north != null) {
                    return north;
                }
                EscapeTarget south = findShoreTargetInColumn(origin.getX() + dx, origin.getZ() + radius, origin.getY());
                if (south != null) {
                    return south;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                EscapeTarget west = findShoreTargetInColumn(origin.getX() - radius, origin.getZ() + dz, origin.getY());
                if (west != null) {
                    return west;
                }
                EscapeTarget east = findShoreTargetInColumn(origin.getX() + radius, origin.getZ() + dz, origin.getY());
                if (east != null) {
                    return east;
                }
            }
        }
        return null;
    }

    private EscapeTarget findShoreTargetInColumn(int x, int z, int originY) {
        if (!dragon.level().hasChunkAt(new BlockPos(x, originY, z))) {
            return null;
        }

        int minY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - VERTICAL_SEARCH);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, originY + VERTICAL_SEARCH);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            cursor.set(x, y, z);
            if (!isStandableLand(cursor)) {
                continue;
            }

            BlockPos landPos = cursor.immutable();
            EscapeTarget target = findAdjacentWaterTarget(landPos);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private EscapeTarget findAdjacentWaterTarget(BlockPos landPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos waterPos = landPos.relative(direction);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos candidate = waterPos.offset(0, dy, 0);
                if (dragon.level().getFluidState(candidate).is(FluidTags.WATER)) {
                    return new EscapeTarget(
                            Vec3.atCenterOf(candidate),
                            new Vec3(landPos.getX() + 0.5D, landPos.getY(), landPos.getZ() + 0.5D)
                    );
                }
            }
        }
        return null;
    }

    private EscapeTarget findRandomWaterRoamTarget(BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = WATER_ROAM_MIN_DISTANCE + dragon.getRandom().nextDouble() * WATER_ROAM_RANDOM_DISTANCE;
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);

            if (!dragon.level().hasChunkAt(new BlockPos(x, origin.getY(), z))) {
                continue;
            }

            int minY = Math.max(dragon.level().getMinBuildHeight() + 1, origin.getY() - VERTICAL_SEARCH);
            int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, origin.getY() + VERTICAL_SEARCH);
            EscapeTarget target = findSurfaceBiasedWaterRoamTargetInColumn(cursor, x, z, origin.getY(), minY, maxY);
            if (target != null) {
                return target;
            }
        }
        return findNearbyWaterRoamTarget(origin);
    }

    private EscapeTarget findNearbyWaterRoamTarget(BlockPos origin) {
        for (int radius = 4; radius <= SEARCH_RADIUS; radius += 4) {
            for (int dx = -radius; dx <= radius; dx++) {
                EscapeTarget north = findWaterRoamTargetInColumn(origin.getX() + dx, origin.getZ() - radius, origin.getY());
                if (north != null) {
                    return north;
                }
                EscapeTarget south = findWaterRoamTargetInColumn(origin.getX() + dx, origin.getZ() + radius, origin.getY());
                if (south != null) {
                    return south;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                EscapeTarget west = findWaterRoamTargetInColumn(origin.getX() - radius, origin.getZ() + dz, origin.getY());
                if (west != null) {
                    return west;
                }
                EscapeTarget east = findWaterRoamTargetInColumn(origin.getX() + radius, origin.getZ() + dz, origin.getY());
                if (east != null) {
                    return east;
                }
            }
        }
        return null;
    }

    private EscapeTarget findWaterRoamTargetInColumn(int x, int z, int originY) {
        if (!dragon.level().hasChunkAt(new BlockPos(x, originY, z))) {
            return null;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - VERTICAL_SEARCH);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, originY + VERTICAL_SEARCH);
        return findSurfaceBiasedWaterRoamTargetInColumn(cursor, x, z, originY, minY, maxY);
    }

    private EscapeTarget findSurfaceBiasedWaterRoamTargetInColumn(BlockPos.MutableBlockPos cursor,
                                                                  int x,
                                                                  int z,
                                                                  int originY,
                                                                  int minY,
                                                                  int maxY) {
        int scanTop = Math.min(dragon.level().getMaxBuildHeight() - 2, Math.max(maxY, originY + SURFACE_SEARCH_UP));
        int highestWaterY = Integer.MIN_VALUE;
        for (int y = scanTop; y >= minY; y--) {
            cursor.set(x, y, z);
            if (!isUsableWater(cursor)) {
                continue;
            }
            highestWaterY = y;
            break;
        }

        if (highestWaterY != Integer.MIN_VALUE) {
            return EscapeTarget.roam(Vec3.atCenterOf(new BlockPos(x, highestWaterY, z)));
        }

        for (int y = maxY; y >= minY; y--) {
            cursor.set(x, y, z);
            if (isUsableWater(cursor)) {
                return EscapeTarget.roam(Vec3.atCenterOf(cursor));
            }
        }
        return null;
    }

    private boolean isUsableWater(BlockPos pos) {
        if (!dragon.level().getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        return dragon.level().getBlockState(pos).getCollisionShape(dragon.level(), pos).isEmpty();
    }

    private boolean isStandableLand(BlockPos pos) {
        if (dragon.level().getFluidState(pos).is(FluidTags.WATER)
                || dragon.level().getFluidState(pos.above()).is(FluidTags.WATER)) {
            return false;
        }
        if (!dragon.level().getBlockState(pos).getCollisionShape(dragon.level(), pos).isEmpty()) {
            return false;
        }
        if (!dragon.level().getBlockState(pos.above()).getCollisionShape(dragon.level(), pos.above()).isEmpty()) {
            return false;
        }

        BlockState below = dragon.level().getBlockState(pos.below());
        return !below.getCollisionShape(dragon.level(), pos.below()).isEmpty();
    }

    private void applyShoreAssist(Vec3 landPosition) {
        Vec3 toLand = landPosition.subtract(dragon.position());
        Vec3 horizontal = new Vec3(toLand.x, 0.0D, toLand.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        Vec3 velocity = dragon.getDeltaMovement();
        double horizontalBoost = dragon.horizontalCollision ? 0.48D : 0.36D;
        double upward = dragon.horizontalCollision ? 0.58D : 0.34D;
        dragon.setDeltaMovement(
                velocity.x * 0.45D + direction.x * horizontalBoost,
                Math.max(velocity.y, upward),
                velocity.z * 0.45D + direction.z * horizontalBoost
        );
        dragon.getMoveControl().setWantedPosition(landPosition.x, landPosition.y, landPosition.z, 1.15D);
        dragon.hasImpulse = true;
    }

    private void preserveEscapeAir() {
        if (dragon.canSwim()) {
            return;
        }
        int maxAir = dragon.getMaxAirSupply();
        if (maxAir <= 0 || dragon.getAirSupply() >= maxAir) {
            return;
        }
        dragon.setAirSupply(Math.min(maxAir, dragon.getAirSupply() + 20));
    }

    private record EscapeTarget(Vec3 waterPosition, Vec3 landPosition) {
        static EscapeTarget roam(Vec3 waterPosition) {
            return new EscapeTarget(waterPosition, null);
        }

        boolean isShoreTarget() {
            return landPosition != null;
        }
    }
}
