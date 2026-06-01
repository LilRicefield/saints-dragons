package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DragonLeaveWaterGoal<T extends DragonEntity & SemiAquaticDragon> extends Goal {
    private static final int EXECUTION_CHANCE = 30;
    private static final int MAX_STUCK_TICKS = 60;
    private static final int MAX_TOTAL_TICKS = 400;
    private static final int REPATH_INTERVAL_TICKS = 40;
    private static final int LAND_SEARCH_HORIZONTAL = 23;
    private static final int LAND_SEARCH_VERTICAL = 7;
    private static final int LAND_TARGET_ATTEMPTS = 8;

    private final T dragon;
    private final double speedModifier;
    private Vec3 targetPos;
    private int stuckTicks;
    private int totalTicks;
    private Vec3 lastPos;

    public DragonLeaveWaterGoal(T dragon) {
        this(dragon, 1.0D);
    }

    public DragonLeaveWaterGoal(T dragon, double speedModifier) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.dragon.level().getFluidState(this.dragon.blockPosition()).is(FluidTags.WATER)) {
            return false;
        }
        if (!this.dragon.shouldLeaveWater()) {
            return false;
        }
        if (isFollowingOwnerInWater()) {
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
        if (this.targetPos == null) {
            return;
        }
        this.dragon.getNavigation().moveTo(this.targetPos.x, this.targetPos.y, this.targetPos.z, this.speedModifier);
        this.stuckTicks = 0;
        this.totalTicks = 0;
        this.lastPos = this.dragon.position();
    }

    @Override
    public void tick() {
        this.totalTicks++;

        Vec3 currentPos = this.dragon.position();
        if (this.lastPos != null && currentPos.distanceToSqr(this.lastPos) < 0.01D) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
        }
        this.lastPos = currentPos;

        if (this.targetPos != null && this.totalTicks % REPATH_INTERVAL_TICKS == 0) {
            this.dragon.getNavigation().moveTo(this.targetPos.x, this.targetPos.y, this.targetPos.z, this.speedModifier);
        }

        if (this.dragon.horizontalCollision && this.dragon.isInWater()) {
            float yawRad = this.dragon.getYRot() * Mth.DEG_TO_RAD;
            Vec3 currentVelocity = this.dragon.getDeltaMovement();
            double pushX = -Mth.sin(yawRad) * 0.3D;
            double pushZ = Mth.cos(yawRad) * 0.3D;
            double upward = Mth.clamp(currentVelocity.y + 0.08D, 0.24D, 0.45D);
            this.dragon.setDeltaMovement(currentVelocity.x + pushX, upward, currentVelocity.z + pushZ);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.dragon.shouldLeaveWater()) {
            this.dragon.getNavigation().stop();
            return false;
        }
        if (isFollowingOwnerInWater()) {
            this.dragon.getNavigation().stop();
            return false;
        }
        if (this.stuckTicks > MAX_STUCK_TICKS || this.totalTicks > MAX_TOTAL_TICKS) {
            this.dragon.getNavigation().stop();
            return false;
        }
        if (!this.dragon.isInWater()) {
            return false;
        }

        return this.targetPos != null
                && !this.dragon.getNavigation().isDone()
                && !this.dragon.level().getFluidState(BlockPos.containing(this.targetPos)).is(FluidTags.WATER);
    }

    private Vec3 generateTarget() {
        Vec3 target = LandRandomPos.getPos(this.dragon, LAND_SEARCH_HORIZONTAL, LAND_SEARCH_VERTICAL);
        int tries = 0;

        while (target != null && tries < LAND_TARGET_ATTEMPTS) {
            if (!hasWaterNearby(target)) {
                return target;
            }

            target = LandRandomPos.getPos(this.dragon, LAND_SEARCH_HORIZONTAL, LAND_SEARCH_VERTICAL);
            tries++;
        }

        return null;
    }

    private boolean hasWaterNearby(Vec3 target) {
        BlockPos center = BlockPos.containing(target);
        for (BlockPos blockPos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 0, 2))) {
            if (this.dragon.level().getFluidState(blockPos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFollowingOwnerInWater() {
        if (!this.dragon.isTame() || this.dragon.getCommand() != 0) {
            return false;
        }
        LivingEntity owner = this.dragon.getOwner();
        return owner != null && owner.isAlive() && owner.level() == this.dragon.level() && owner.isInWaterOrBubble();
    }
}
