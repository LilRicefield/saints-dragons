package com.leon.saintsdragons.server.ai.goals.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

public final class SuspendedFloatWanderGoal extends Goal {
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 8;

    private final PathfinderMob mob;
    private final Movement movement;
    private final BooleanSupplier canWander;
    private final double speed;
    private final int wanderChance;

    @Nullable
    private Vec3 target;

    public SuspendedFloatWanderGoal(PathfinderMob mob,
                                    Movement movement,
                                    BooleanSupplier canWander,
                                    double speed,
                                    int wanderChance) {
        this.mob = mob;
        this.movement = movement;
        this.canWander = canWander;
        this.speed = speed;
        this.wanderChance = Math.max(1, wanderChance);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        this.target = null;
        if (!this.canWander.getAsBoolean()
                || !this.movement.isIdle()
                || this.mob.getRandom().nextInt(this.wanderChance) != 0) {
            return false;
        }

        this.target = findTarget();
        return this.target != null;
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.movement.moveTo(this.target, this.speed);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.canWander.getAsBoolean()
                && !this.movement.isIdle();
    }

    @Override
    public void stop() {
        // A controller that reached its target owns its natural idle deceleration.
        // Only cancel movement when another behavior interrupted this goal mid-route.
        if (!this.movement.isIdle()) {
            this.movement.stop();
        }
        this.target = null;
    }

    private @Nullable Vec3 findTarget() {
        Vec3 view = this.mob.getViewVector(0.0F);
        Vec3 candidate = HoverRandomPos.getPos(
                this.mob,
                HORIZONTAL_RANGE,
                VERTICAL_RANGE,
                view.x,
                view.z,
                (float) Math.PI / 2.0F,
                6,
                4
        );
        if (candidate == null) {
            Vec3 forward = this.mob.position().add(view);
            candidate = AirAndWaterRandomPos.getPos(
                    this.mob,
                    HORIZONTAL_RANGE,
                    VERTICAL_RANGE - 2,
                    -4,
                    forward.x,
                    forward.z,
                    (float) Math.PI / 2.0F
            );
        }

        if (candidate == null) {
            return null;
        }
        BlockPos pos = BlockPos.containing(candidate);
        if (!this.mob.level().hasChunkAt(pos) || !this.mob.level().getFluidState(pos).isEmpty()) {
            return null;
        }
        Vec3 offset = candidate.subtract(this.mob.position());
        return this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(offset)) ? candidate : null;
    }

    public interface Movement {
        boolean isIdle();

        void moveTo(Vec3 target, double speed);

        void stop();
    }
}
