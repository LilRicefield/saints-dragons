package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DragonAggroLandingHelper {
    private static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;
    private static final double SHARED_LANDING_SPEED = 1.0D;

    private DragonAggroLandingHelper() {
    }

    public static void beginAggroLanding(
            RideableDragonBase dragon,
            @Nullable LivingEntity target,
            double landingSpeed
    ) {
        tryBeginAggroLanding(dragon, target, landingSpeed);
    }

    public static boolean tryBeginAggroLanding(
            RideableDragonBase dragon,
            @Nullable LivingEntity target,
            double landingSpeed
    ) {
        if (!(dragon instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        Vec3 landingTarget = findLandingTarget(dragon, target);
        if (landingTarget == null) {
            return false;
        }
        flightCapable.beginAiLanding();
        dragon.getNavigation().moveTo(landingTarget.x, landingTarget.y, landingTarget.z, SHARED_LANDING_SPEED);
        return true;
    }

    public static @Nullable Vec3 findLandingTarget(RideableDragonBase dragon, @Nullable LivingEntity target) {
        BlockPos origin = dragon.blockPosition();
        double currentAltitude = Math.max(0.0D, dragon.getY()
                - dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragon.getBlockX(), dragon.getBlockZ()));
        double minHorizontalDistanceSqr = currentAltitude > 6.0D
                ? MIN_AIRBORNE_LANDING_HORIZONTAL * MIN_AIRBORNE_LANDING_HORIZONTAL
                : 0.0D;

        for (int radius = 0; radius <= 32; radius += 8) {
            for (int attempt = 0; attempt < 14; attempt++) {
                int dx = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                int dz = radius == 0 ? 0 : dragon.getRandom().nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz < minHorizontalDistanceSqr) {
                    continue;
                }

                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }

                int surfaceY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
                if (isValidLandingSurface(dragon, ground)) {
                    return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
                }
            }
        }
        return null;
    }

    private static boolean isValidLandingSurface(Mob dragon, BlockPos ground) {
        if (!dragon.level().hasChunkAt(ground)) {
            return false;
        }

        var state = dragon.level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isFaceSturdy(dragon.level(), ground, Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        var aboveState = dragon.level().getBlockState(above);
        var aboveTwoState = dragon.level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(dragon.level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(dragon.level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }
}
