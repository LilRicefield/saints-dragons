package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class AmphithereFlightGoal extends Goal {
    private static final int MIN_AIR_TICKS = 140;
    private static final int MAX_AIR_TICKS = 360;

    private final AmphithereEntity dragon;
    private int desiredAirTime = MIN_AIR_TICKS;
    private boolean landing;
    private Vec3 landingTarget;

    public AmphithereFlightGoal(AmphithereEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!dragon.isAlive() || dragon.isBaby()) {
            return false;
        }
        if (dragon.isOrderedToSit() || dragon.isPassenger()) {
            return false;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            return true;
        }
        if (dragon.isFlying()) {
            return true;
        }
        return dragon.getGroundTicks() > 40 && dragon.canTakeoff();
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isOrderedToSit() || dragon.isPassenger()) {
            return false;
        }
        return dragon.isFlying();
    }

    @Override
    public void start() {
        landing = false;
        landingTarget = null;
        desiredAirTime = MIN_AIR_TICKS + dragon.getRandom().nextInt(MAX_AIR_TICKS - MIN_AIR_TICKS + 1);
        if (!dragon.isFlying() && dragon.canTakeoff()) {
            dragon.setFlying(true);
        }
        pickCruiseTarget();
    }

    @Override
    public void stop() {
        dragon.assignFlightTarget(null);
        if (!dragon.isLanding()) {
            dragon.setHovering(false);
        }
        landing = false;
        landingTarget = null;
    }

    @Override
    public void tick() {
        if (!dragon.isFlying()) {
            return;
        }

        if (!landing && dragon.getAirTicks() > desiredAirTime && dragon.getRandom().nextInt(60) == 0) {
            beginLanding();
        }

        if (landing) {
            if (landingTarget == null || dragon.distanceToSqr(landingTarget) < 4.0D) {
                landingTarget = chooseLandingSpot();
                dragon.assignFlightTarget(landingTarget);
            }
            if (landingTarget != null) {
                dragon.getMoveControl().setWantedPosition(landingTarget.x, landingTarget.y, landingTarget.z, dragon.getFlightSpeed() * 0.8F);
            }
        } else {
            Vec3 target = dragon.getFlightTarget();
            if (target == null || dragon.distanceToSqr(target) < 9.0D || !isTargetValid(target)) {
                pickCruiseTarget();
                target = dragon.getFlightTarget();
            }

            if (target != null) {
                dragon.getMoveControl().setWantedPosition(target.x, target.y, target.z, dragon.getFlightSpeed());
            }

            if (dragon.getRandom().nextInt(200) == 0) {
                pickCruiseTarget();
            }
        }
    }

    private void pickCruiseTarget() {
        Vec3 chosen = chooseFlightTarget();
        dragon.assignFlightTarget(chosen);
    }

    private Vec3 chooseFlightTarget() {
        BlockPos origin = dragon.blockPosition();
        double radius = 10.0D + dragon.getRandom().nextDouble() * 18.0D;
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        double targetX = origin.getX() + 0.5D + offsetX;
        double targetZ = origin.getZ() + 0.5D + offsetZ;

        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
        double preferredAltitude = dragon.getPreferredFlightAltitude();
        double targetY = groundY + preferredAltitude;
        targetY = Math.min(targetY, dragon.level().getMaxBuildHeight() - 10.0D);

        return new Vec3(targetX, targetY, targetZ);
    }

    private Vec3 chooseLandingSpot() {
        BlockPos origin = dragon.blockPosition();
        double radius = 6.0D + dragon.getRandom().nextDouble() * 8.0D;
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;

        double targetX = origin.getX() + 0.5D + Math.cos(angle) * radius;
        double targetZ = origin.getZ() + 0.5D + Math.sin(angle) * radius;
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) targetX, (int) targetZ);
        double targetY = groundY + 1.2D;

        return new Vec3(targetX, targetY, targetZ);
    }

    private boolean isTargetValid(Vec3 target) {
        HitResult hit = dragon.level().clip(new ClipContext(
                dragon.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void beginLanding() {
        landing = true;
        dragon.setLanding(true);
        dragon.setHovering(false);
        landingTarget = chooseLandingSpot();
        dragon.assignFlightTarget(landingTarget);
    }
}
