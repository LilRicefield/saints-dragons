package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class DragonWaterEscapeBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int SEARCH_RADIUS = 24;
    private static final int VERTICAL_SEARCH = 8;
    private static final int SURFACE_SEARCH_UP = 32;
    private static final int TARGET_ATTEMPTS = 28;
    private static final int SHORE_RESCAN_TICKS = 20;
    private static final int ROAM_TARGET_REACHED_TICKS = 60;
    private static final int WATER_ROAM_MIN_DISTANCE = 12;
    private static final int WATER_ROAM_RANDOM_DISTANCE = 24;
    private static final double WATER_ROAM_ARRIVAL_DISTANCE_SQR = 4.0D * 4.0D;

    private final float turnSpeed;
    private final double swimSpeed;
    private final Predicate<T> startCondition;
    private final Predicate<T> continueCondition;
    @Nullable
    private EscapeTarget target;
    private int shoreRescanTicks;
    private int roamTicks;
    private boolean shoreTransitioning;

    public DragonWaterEscapeBehaviour(float turnSpeed, double swimSpeed) {
        this(turnSpeed, swimSpeed,
                dragon -> !dragon.canSwim(),
                dragon -> !dragon.canSwim());
    }

    public DragonWaterEscapeBehaviour(float turnSpeed,
                                      double swimSpeed,
                                      Predicate<T> startCondition,
                                      Predicate<T> continueCondition) {
        this.turnSpeed = turnSpeed;
        this.swimSpeed = swimSpeed;
        this.startCondition = Objects.requireNonNull(startCondition);
        this.continueCondition = Objects.requireNonNull(continueCondition);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!shouldEscapeWater(dragon)) {
            return false;
        }
        target = findEscapeTarget(dragon);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (shoreTransitioning) {
            return target != null
                    && continueCondition.test(dragon)
                    && !dragon.isVehicle()
                    && !dragon.isAerial()
                    && dragon.isInWaterOrBubble();
        }
        return target != null
                && continueCondition.test(dragon)
                && !dragon.isVehicle()
                && !dragon.isAerial()
                && (dragon.isInWaterOrBubble() || !dragon.onGround());
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        context.dragon().getNavigation().stop();
        shoreRescanTicks = 0;
        roamTicks = 0;
        shoreTransitioning = false;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (target == null) {
            return;
        }

        dragon.getNavigation().stop();
        if (shoreTransitioning) {
            if (dragon.isInWaterOrBubble() && target.landPosition() != null) {
                applyShoreAssist(dragon, target.landPosition());
            }
            return;
        }
        updateShoreLock(dragon);
        preserveEscapeAir(dragon);

        AsyncSwimController controller = dragon.getAiSwimController();
        if (!controller.trackTarget(target.waterPosition(), swimSpeed, turnSpeed)) {
            EscapeTarget replacement = findEscapeTarget(dragon);
            if (replacement != null) {
                target = replacement;
            }
            return;
        }
        controller.serverTick();

        if (target.isShoreTarget() && shouldBeginShoreTransition(dragon, target)) {
            controller.stop();
            shoreTransitioning = true;
            applyShoreAssist(dragon, target.landPosition());
            return;
        }

        if (!target.isShoreTarget()) {
            roamTicks++;
            if (roamTicks > ROAM_TARGET_REACHED_TICKS
                    || dragon.distanceToSqr(target.waterPosition()) <= WATER_ROAM_ARRIVAL_DISTANCE_SQR) {
                EscapeTarget replacement = findEscapeTarget(dragon);
                if (replacement != null) {
                    target = replacement;
                    roamTicks = 0;
                }
            }
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        target = null;
        shoreTransitioning = false;
        context.dragon().getAiSwimController().stop();
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return 10;
    }

    private boolean shouldEscapeWater(T dragon) {
        return startCondition.test(dragon)
                && !dragon.isVehicle()
                && !dragon.isAerial()
                && dragon.isInWaterOrBubble();
    }

    @Nullable
    private EscapeTarget findEscapeTarget(T dragon) {
        BlockPos origin = dragon.blockPosition();
        EscapeTarget targetInColumn = findShoreTargetInColumn(dragon, origin.getX(), origin.getZ(), origin.getY());
        if (targetInColumn != null) {
            return targetInColumn;
        }

        EscapeTarget shore = findNearestShoreTargetByRings(dragon, origin);
        if (shore != null) {
            return shore;
        }

        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = origin.getX() + dragon.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = origin.getZ() + dragon.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            shore = findShoreTargetInColumn(dragon, x, z, origin.getY());
            if (shore != null) {
                return shore;
            }
        }
        return findRandomWaterRoamTarget(dragon, origin);
    }

    private void updateShoreLock(T dragon) {
        if (target != null && target.isShoreTarget()) {
            return;
        }
        if (shoreRescanTicks > 0) {
            shoreRescanTicks--;
            return;
        }
        shoreRescanTicks = SHORE_RESCAN_TICKS;
        EscapeTarget shore = findNearestShoreTargetByRings(dragon, dragon.blockPosition());
        if (shore != null) {
            target = shore;
            roamTicks = 0;
        }
    }

    @Nullable
    private EscapeTarget findNearestShoreTargetByRings(T dragon, BlockPos origin) {
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int offset = -radius; offset <= radius; offset++) {
                EscapeTarget target = findShoreTargetInColumn(
                        dragon, origin.getX() + offset, origin.getZ() - radius, origin.getY());
                if (target != null) return target;
                target = findShoreTargetInColumn(
                        dragon, origin.getX() + offset, origin.getZ() + radius, origin.getY());
                if (target != null) return target;
            }
            for (int offset = -radius + 1; offset <= radius - 1; offset++) {
                EscapeTarget target = findShoreTargetInColumn(
                        dragon, origin.getX() - radius, origin.getZ() + offset, origin.getY());
                if (target != null) return target;
                target = findShoreTargetInColumn(
                        dragon, origin.getX() + radius, origin.getZ() + offset, origin.getY());
                if (target != null) return target;
            }
        }
        return null;
    }

    @Nullable
    private EscapeTarget findShoreTargetInColumn(T dragon, int x, int z, int originY) {
        BlockPos column = new BlockPos(x, originY, z);
        if (!dragon.level().hasChunkAt(column)) {
            return null;
        }
        int minY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - VERTICAL_SEARCH);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, originY + VERTICAL_SEARCH);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; y--) {
            cursor.set(x, y, z);
            if (isStandableLand(dragon, cursor)) {
                EscapeTarget target = findAdjacentWaterTarget(dragon, cursor.immutable());
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    @Nullable
    private EscapeTarget findAdjacentWaterTarget(T dragon, BlockPos landPosition) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos waterPosition = landPosition.relative(direction);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos candidate = waterPosition.offset(0, dy, 0);
                if (dragon.level().getFluidState(candidate).is(FluidTags.WATER)) {
                    return new EscapeTarget(
                            Vec3.atCenterOf(candidate),
                            Vec3.atBottomCenterOf(landPosition)
                    );
                }
            }
        }
        return null;
    }

    private EscapeTarget findRandomWaterRoamTarget(T dragon, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = WATER_ROAM_MIN_DISTANCE
                    + dragon.getRandom().nextDouble() * WATER_ROAM_RANDOM_DISTANCE;
            int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, origin.getY(), z);
            if (!dragon.level().hasChunkAt(column)) {
                continue;
            }
            int minY = Math.max(dragon.level().getMinBuildHeight() + 1, origin.getY() - VERTICAL_SEARCH);
            int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, origin.getY() + VERTICAL_SEARCH);
            EscapeTarget target = findSurfaceBiasedWaterRoamTargetInColumn(
                    dragon, cursor, x, z, origin.getY(), minY, maxY);
            if (target != null) {
                return target;
            }
        }
        return findNearbyWaterRoamTarget(dragon, origin);
    }

    @Nullable
    private EscapeTarget findNearbyWaterRoamTarget(T dragon, BlockPos origin) {
        for (int radius = 4; radius <= SEARCH_RADIUS; radius += 4) {
            for (int offset = -radius; offset <= radius; offset++) {
                EscapeTarget target = findWaterRoamTargetInColumn(
                        dragon, origin.getX() + offset, origin.getZ() - radius, origin.getY());
                if (target != null) return target;
                target = findWaterRoamTargetInColumn(
                        dragon, origin.getX() + offset, origin.getZ() + radius, origin.getY());
                if (target != null) return target;
            }
            for (int offset = -radius + 1; offset <= radius - 1; offset++) {
                EscapeTarget target = findWaterRoamTargetInColumn(
                        dragon, origin.getX() - radius, origin.getZ() + offset, origin.getY());
                if (target != null) return target;
                target = findWaterRoamTargetInColumn(
                        dragon, origin.getX() + radius, origin.getZ() + offset, origin.getY());
                if (target != null) return target;
            }
        }
        return null;
    }

    @Nullable
    private EscapeTarget findWaterRoamTargetInColumn(T dragon, int x, int z, int originY) {
        BlockPos column = new BlockPos(x, originY, z);
        if (!dragon.level().hasChunkAt(column)) {
            return null;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(dragon.level().getMinBuildHeight() + 1, originY - VERTICAL_SEARCH);
        int maxY = Math.min(dragon.level().getMaxBuildHeight() - 2, originY + VERTICAL_SEARCH);
        return findSurfaceBiasedWaterRoamTargetInColumn(
                dragon, cursor, x, z, originY, minY, maxY);
    }

    @Nullable
    private EscapeTarget findSurfaceBiasedWaterRoamTargetInColumn(T dragon,
                                                                  BlockPos.MutableBlockPos cursor,
                                                                  int x,
                                                                  int z,
                                                                  int originY,
                                                                  int minY,
                                                                  int maxY) {
        int scanTop = Math.min(dragon.level().getMaxBuildHeight() - 2,
                Math.max(maxY, originY + SURFACE_SEARCH_UP));
        for (int y = scanTop; y >= minY; y--) {
            cursor.set(x, y, z);
            if (isUsableWater(dragon, cursor)) {
                return EscapeTarget.roam(Vec3.atCenterOf(cursor));
            }
        }
        for (int y = maxY; y >= minY; y--) {
            cursor.set(x, y, z);
            if (isUsableWater(dragon, cursor)) {
                return EscapeTarget.roam(Vec3.atCenterOf(cursor));
            }
        }
        return null;
    }

    private boolean isUsableWater(T dragon, BlockPos position) {
        return dragon.level().getFluidState(position).is(FluidTags.WATER)
                && dragon.level().getBlockState(position)
                .getCollisionShape(dragon.level(), position).isEmpty();
    }

    private boolean isStandableLand(T dragon, BlockPos position) {
        if (dragon.level().getFluidState(position).is(FluidTags.WATER)
                || dragon.level().getFluidState(position.above()).is(FluidTags.WATER)) {
            return false;
        }
        if (!dragon.level().getBlockState(position)
                .getCollisionShape(dragon.level(), position).isEmpty()) {
            return false;
        }
        if (!dragon.level().getBlockState(position.above())
                .getCollisionShape(dragon.level(), position.above()).isEmpty()) {
            return false;
        }
        BlockState floor = dragon.level().getBlockState(position.below());
        return !floor.getCollisionShape(dragon.level(), position.below()).isEmpty();
    }

    private boolean shouldBeginShoreTransition(T dragon, EscapeTarget escapeTarget) {
        double assistRadius = Math.max(2.5D, dragon.getBbWidth() * 0.75D + 1.0D);
        return dragon.position().distanceToSqr(escapeTarget.waterPosition())
                <= assistRadius * assistRadius;
    }

    private void applyShoreAssist(T dragon, Vec3 landPosition) {
        Vec3 horizontal = new Vec3(
                landPosition.x - dragon.getX(), 0.0D, landPosition.z - dragon.getZ());
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
        dragon.getMoveControl().setWantedPosition(
                landPosition.x, landPosition.y, landPosition.z, 1.15D);
        dragon.hasImpulse = true;
    }

    private void preserveEscapeAir(T dragon) {
        int maxAir = dragon.getMaxAirSupply();
        if (maxAir > 0 && dragon.getAirSupply() < maxAir) {
            dragon.setAirSupply(Math.min(maxAir, dragon.getAirSupply() + 20));
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "target_type", target == null ? "none" : target.isShoreTarget() ? "shore" : "surface_roam",
                "phase", shoreTransitioning ? "shore_transition" : "swim_route",
                "water_target", target == null ? "none" : target.waterPosition().toString(),
                "land_target", target == null || target.landPosition() == null
                        ? "none" : target.landPosition().toString()
        );
    }

    private record EscapeTarget(Vec3 waterPosition, @Nullable Vec3 landPosition) {
        private static EscapeTarget roam(Vec3 waterPosition) {
            return new EscapeTarget(waterPosition, null);
        }

        private boolean isShoreTarget() {
            return landPosition != null;
        }
    }
}
