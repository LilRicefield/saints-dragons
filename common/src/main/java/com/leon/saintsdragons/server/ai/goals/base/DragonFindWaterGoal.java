package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DragonFindWaterGoal<T extends DragonEntity & SemiAquaticDragon> extends Goal {
    private static final int EXECUTION_CHANCE = 30;
    private static final int TARGET_ATTEMPTS = 15;

    private final T dragon;
    private final double speedModifier;
    private BlockPos targetPos;

    public DragonFindWaterGoal(T dragon) {
        this(dragon, 1.0D);
    }

    public DragonFindWaterGoal(T dragon, double speedModifier) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.dragon.onGround()) {
            return false;
        }
        if (isStandingInWater()) {
            return false;
        }
        if (!this.dragon.shouldEnterWater()) {
            return false;
        }
        if (isFollowingOwnerOutOfWater()) {
            return false;
        }
        if (this.dragon.getTarget() == null && this.dragon.getRandom().nextInt(EXECUTION_CHANCE) != 0) {
            return false;
        }

        this.targetPos = generateTarget();
        return this.targetPos != null;
    }

    @Override
    public void start() {
        moveToTarget();
    }

    @Override
    public void tick() {
        moveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.dragon.shouldEnterWater()) {
            stopMovement();
            return false;
        }
        if (isFollowingOwnerOutOfWater()) {
            stopMovement();
            return false;
        }
        return this.targetPos != null
                && isMovementActive()
                && !isStandingInWater();
    }

    @Override
    public void stop() {
        stopMovement();
        this.targetPos = null;
    }

    private void moveToTarget() {
        if (this.targetPos != null) {
            if (this.dragon instanceof RideableDragonBase rideable) {
                rideable.getAIMovement().moveToGroundPosition(
                        net.minecraft.world.phys.Vec3.atBottomCenterOf(this.targetPos),
                        this.speedModifier,
                        false
                );
            } else {
                this.dragon.getNavigation().moveTo(
                        this.targetPos.getX(),
                        this.targetPos.getY(),
                        this.targetPos.getZ(),
                        this.speedModifier
                );
            }
        }
    }

    private boolean isMovementActive() {
        if (this.dragon instanceof RideableDragonBase rideable) {
            return rideable.getAIMovement().isPathing();
        }
        return !this.dragon.getNavigation().isDone();
    }

    private void stopMovement() {
        if (this.dragon instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().stop();
        } else {
            this.dragon.getNavigation().stop();
        }
    }

    private boolean isStandingInWater() {
        return this.dragon.level().getFluidState(this.dragon.blockPosition()).is(FluidTags.WATER);
    }

    private BlockPos generateTarget() {
        BlockPos foundPos = null;
        RandomSource random = this.dragon.getRandom();
        int range = this.dragon.getWaterSearchRange();
        int horizontalRange = Math.max(1, range / 2);

        for (int i = 0; i < TARGET_ATTEMPTS; i++) {
            BlockPos blockPos = this.dragon.blockPosition().offset(
                    random.nextInt(range) - horizontalRange,
                    3,
                    random.nextInt(range) - horizontalRange
            );

            while (this.dragon.level().isEmptyBlock(blockPos) && blockPos.getY() > this.dragon.level().getMinBuildHeight()) {
                blockPos = blockPos.below();
            }

            if (this.dragon.level().getFluidState(blockPos).is(FluidTags.WATER)) {
                foundPos = blockPos;
            }
        }

        return foundPos;
    }

    private boolean isFollowingOwnerOutOfWater() {
        if (!this.dragon.isTame() || this.dragon.getCommand() != 0) {
            return false;
        }
        LivingEntity owner = this.dragon.getOwner();
        return owner != null && owner.isAlive() && owner.level() == this.dragon.level() && !owner.isInWaterOrBubble();
    }
}
