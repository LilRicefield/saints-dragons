package com.leon.saintsdragons.server.entity.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

final class IvyCompanionController {
    private static final double FOLLOW_START_DISTANCE_SQR = 6.25D;
    private static final double FOLLOW_STOP_DISTANCE_SQR = 2.25D;
    private static final double FOLLOW_RUN_DISTANCE_SQR = 20.25D;
    private static final double FOLLOW_TELEPORT_DISTANCE_SQR = 4096.0D;
    private static final double FOLLOW_WALK_SPEED = 1.0D;
    private static final double FOLLOW_RUN_SPEED = 1.3D;
    private static final double DIRECT_FOLLOW_WALK_SPEED = 0.18D;
    private static final double DIRECT_FOLLOW_RUN_SPEED = 0.28D;
    private static final double DIRECT_FOLLOW_DROP_Y = 1.5D;
    private static final double DIRECT_FOLLOW_FAILED_PATH_DISTANCE_SQR = 9.0D;
    private static final double OWNER_DEFENSE_RANGE_SQR = 32.0D * 32.0D;
    private static final int LADDER_SEARCH_RADIUS = 12;
    private static final int LADDER_VERTICAL_MARGIN = 4;
    private static final double LADDER_MIN_OWNER_Y_DELTA = 3.0D;
    private static final double LADDER_MIN_CLIMBING_OWNER_Y_DELTA = 1.75D;
    private static final double LADDER_MAX_START_Y_DELTA = 1.25D;
    private static final double LADDER_MAX_EXIT_OWNER_Y_DELTA = 1.35D;
    private static final int LADDER_MIN_CLIMB_BLOCKS = 2;
    private static final int LADDER_RETRY_COOLDOWN = 40;
    private static final int LADDER_CHAIN_RETRY_COOLDOWN = 0;
    private static final double LADDER_APPROACH_SPEED = 1.15D;
    private static final double LADDER_CLIMB_SPEED = 0.18D;
    private static final double LADDER_EXIT_SPEED = 0.18D;
    private static final int LADDER_EXIT_TICKS = 18;

    private final IvyTheDragonMerchant ivy;
    private int ownerLastHurtByTimestamp = -1;
    private int ownerLastHurtMobTimestamp = -1;
    private boolean ownerCombatTimestampsSynced = false;
    private int ladderRetryCooldown;

    IvyCompanionController(IvyTheDragonMerchant ivy) {
        this.ivy = ivy;
    }

    void tick() {
        if (ladderRetryCooldown > 0) {
            ladderRetryCooldown--;
        }
        if (ivy.getTarget() != null || ivy.getCompanionCommand() != IvyTheDragonMerchant.CompanionCommand.FOLLOW) {
            ivy.setRunning(false);
        }
    }

    Goal createStayGoal() {
        return new StayGoal();
    }

    Goal createFollowOwnerGoal() {
        return new FollowOwnerGoal();
    }

    Goal createLadderClimbGoal() {
        return new LadderClimbGoal();
    }

    Goal createOwnerDefenseGoal() {
        return new OwnerDefenseGoal();
    }

    private boolean canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand command) {
        return ivy.isTame()
                && ivy.getCompanionCommand() == command
                && ivy.getTarget() == null
                && !ivy.isCompanionAiBlocked();
    }

    private boolean canDefendOwner() {
        return ivy.isTame()
                && ivy.getOwner() instanceof Player
                && !ivy.isCompanionAiBlocked()
                && ivy.isAlive();
    }

    @Nullable
    private LivingEntity getValidDefenseTarget() {
        LivingEntity owner = ivy.getOwner();
        if (!(owner instanceof Player) || !owner.isAlive() || owner.level().dimension() != ivy.level().dimension()) {
            return null;
        }

        LivingEntity hurtBy = owner.getLastHurtByMob();
        int hurtByTimestamp = owner.getLastHurtByMobTimestamp();
        if (hurtByTimestamp != ownerLastHurtByTimestamp && canTargetForOwner(hurtBy, owner)) {
            return hurtBy;
        }

        LivingEntity hurtMob = owner.getLastHurtMob();
        int hurtMobTimestamp = owner.getLastHurtMobTimestamp();
        if (hurtMobTimestamp != ownerLastHurtMobTimestamp && canTargetForOwner(hurtMob, owner)) {
            return hurtMob;
        }

        return null;
    }

    private boolean canTargetForOwner(@Nullable LivingEntity target, LivingEntity owner) {
        return target != null
                && target.isAlive()
                && target != ivy
                && target != owner
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()))
                && !ivy.isOwnedBy(target)
                && target.level().dimension() == ivy.level().dimension()
                && ivy.distanceToSqr(target) <= OWNER_DEFENSE_RANGE_SQR;
    }

    private void rememberOwnerCombatTimestamps() {
        LivingEntity owner = ivy.getOwner();
        if (owner == null) {
            return;
        }
        ownerLastHurtByTimestamp = owner.getLastHurtByMobTimestamp();
        ownerLastHurtMobTimestamp = owner.getLastHurtMobTimestamp();
        ownerCombatTimestampsSynced = true;
    }

    private class StayGoal extends Goal {
        StayGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand.STAY);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            stopMovement();
        }

        @Override
        public void tick() {
            stopMovement();
        }

        @Override
        public void stop() {
            ivy.setRunning(false);
        }

        private void stopMovement() {
            ivy.getNavigation().stop();
            ivy.setRunning(false);
            ivy.setDeltaMovement(0.0D, ivy.getDeltaMovement().y, 0.0D);
        }
    }

    private class FollowOwnerGoal extends Goal {
        private LivingEntity owner;

        FollowOwnerGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand.FOLLOW)) {
                return false;
            }
            LivingEntity resolvedOwner = ivy.getOwner();
            if (resolvedOwner == null
                    || !resolvedOwner.isAlive()
                    || resolvedOwner.level().dimension() != ivy.level().dimension()
                    || ivy.distanceToSqr(resolvedOwner) < FOLLOW_START_DISTANCE_SQR) {
                return false;
            }
            owner = resolvedOwner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null
                    && owner.isAlive()
                    && owner.level().dimension() == ivy.level().dimension()
                    && canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand.FOLLOW)
                    && ivy.distanceToSqr(owner) > FOLLOW_STOP_DISTANCE_SQR;
        }

        @Override
        public void stop() {
            owner = null;
            ivy.setRunning(false);
            ivy.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) {
                return;
            }
            ivy.getLookControl().setLookAt(owner, 30.0F, 30.0F);
            double distance = ivy.distanceToSqr(owner);
            if (distance > FOLLOW_TELEPORT_DISTANCE_SQR && tryTeleportNearOwner(owner)) {
                ivy.setRunning(false);
                ivy.getNavigation().stop();
                return;
            }
            boolean shouldRun = distance > FOLLOW_RUN_DISTANCE_SQR;
            ivy.setRunning(shouldRun);
            boolean pathing = ivy.getNavigation().moveTo(owner, shouldRun ? FOLLOW_RUN_SPEED : FOLLOW_WALK_SPEED);
            if (shouldDirectFollow(owner, distance, pathing)) {
                directFollowOwner(owner, shouldRun);
            }
        }

        private boolean shouldDirectFollow(LivingEntity owner, double distanceSqr, boolean pathing) {
            return owner.getY() < ivy.getY() - DIRECT_FOLLOW_DROP_Y
                    || owner.getDeltaMovement().y < -0.35D
                    || (!pathing && distanceSqr > DIRECT_FOLLOW_FAILED_PATH_DISTANCE_SQR)
                    || ivy.getNavigation().isDone() && distanceSqr > DIRECT_FOLLOW_FAILED_PATH_DISTANCE_SQR;
        }

        private void directFollowOwner(LivingEntity owner, boolean shouldRun) {
            Vec3 toOwner = owner.position().subtract(ivy.position());
            Vec3 horizontal = new Vec3(toOwner.x, 0.0D, toOwner.z);
            if (horizontal.lengthSqr() < 1.0E-4D) {
                return;
            }
            ivy.getNavigation().stop();
            Vec3 direction = horizontal.normalize();
            double speed = shouldRun ? DIRECT_FOLLOW_RUN_SPEED : DIRECT_FOLLOW_WALK_SPEED;
            Vec3 current = ivy.getDeltaMovement();
            ivy.setDeltaMovement(
                    current.x * 0.35D + direction.x * speed,
                    current.y,
                    current.z * 0.35D + direction.z * speed
            );
            ivy.setYRot((float) (Math.atan2(direction.z, direction.x) * (180.0F / Math.PI)) - 90.0F);
            ivy.setRunning(true);
        }

        private boolean tryTeleportNearOwner(LivingEntity owner) {
            if (!(ivy.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            for (int attempt = 0; attempt < 12; attempt++) {
                int xOffset = ivy.getRandom().nextIntBetweenInclusive(-3, 3);
                int zOffset = ivy.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(xOffset) < 2 && Math.abs(zOffset) < 2) {
                    continue;
                }
                int x = Mth.floor(owner.getX()) + xOffset;
                int y = Mth.floor(owner.getY());
                int z = Mth.floor(owner.getZ()) + zOffset;
                if (!serverLevel.noCollision(ivy, ivy.getBoundingBox().move(x - ivy.getX(), y - ivy.getY(), z - ivy.getZ()))) {
                    continue;
                }
                ivy.moveTo(x + 0.5D, y, z + 0.5D, ivy.getYRot(), ivy.getXRot());
                return true;
            }
            return false;
        }
    }

    private class LadderClimbGoal extends Goal {
        private LivingEntity owner;
        private LadderPlan plan;
        private Phase phase = Phase.APPROACH;
        private int exitTicks;

        LadderClimbGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand.FOLLOW)) {
                return false;
            }
            LivingEntity resolvedOwner = ivy.getOwner();
            double ownerYDelta = resolvedOwner == null ? 0.0D : Math.abs(resolvedOwner.getY() - ivy.getY());
            boolean ownerUsingLadder = resolvedOwner != null && isNearLadder(resolvedOwner.blockPosition());
            double requiredYDelta = ownerUsingLadder ? LADDER_MIN_CLIMBING_OWNER_Y_DELTA : LADDER_MIN_OWNER_Y_DELTA;
            if (resolvedOwner == null
                    || !resolvedOwner.isAlive()
                    || resolvedOwner.level().dimension() != ivy.level().dimension()
                    || ladderRetryCooldown > 0
                    || ownerYDelta < requiredYDelta) {
                return false;
            }
            LadderPlan resolvedPlan = findLadderPlan(resolvedOwner);
            if (resolvedPlan == null) {
                ladderRetryCooldown = LADDER_RETRY_COOLDOWN / 2;
                return false;
            }
            owner = resolvedOwner;
            plan = resolvedPlan;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null
                    && owner.isAlive()
                    && owner.level().dimension() == ivy.level().dimension()
                    && plan != null
                    && canUseCompanionMovement(IvyTheDragonMerchant.CompanionCommand.FOLLOW)
                    && phase != Phase.DONE;
        }

        @Override
        public void start() {
            phase = Phase.APPROACH;
            exitTicks = 0;
            ivy.setRunning(false);
        }

        @Override
        public void stop() {
            boolean chainNextLadder = phase == Phase.DONE
                    && owner != null
                    && Math.abs(owner.getY() - ivy.getY()) >= LADDER_MIN_CLIMBING_OWNER_Y_DELTA;
            int nextCooldown = chainNextLadder ? LADDER_CHAIN_RETRY_COOLDOWN : LADDER_RETRY_COOLDOWN;
            ivy.setNoGravity(false);
            ivy.setShiftKeyDown(false);
            ivy.setRunning(false);
            ivy.fallDistance = 0.0F;
            owner = null;
            plan = null;
            phase = Phase.APPROACH;
            ladderRetryCooldown = nextCooldown;
        }

        @Override
        public void tick() {
            if (owner == null || plan == null) {
                phase = Phase.DONE;
                return;
            }
            ivy.getLookControl().setLookAt(owner, 30.0F, 30.0F);
            if (phase == Phase.CLIMB && !isLadder(plan.ladderAt(ivy.blockPosition().getY()))) {
                LadderPlan updatedPlan = findLadderPlan(owner);
                if (updatedPlan != null) {
                    plan = updatedPlan;
                }
            }

            switch (phase) {
                case APPROACH -> tickApproach();
                case CLIMB -> tickClimb();
                case EXIT -> tickExit();
                case DONE -> {
                }
            }
        }

        private void tickApproach() {
            BlockPos start = plan.startLadder();
            ivy.setNoGravity(false);
            ivy.setShiftKeyDown(false);
            ivy.getNavigation().moveTo(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, LADDER_APPROACH_SPEED);
            if (isAtLadderApproach(start)) {
                ivy.getNavigation().stop();
                centerOnLadder(start);
                ivy.moveTo(ivy.getX(), start.getY(), ivy.getZ(), ivy.getYRot(), ivy.getXRot());
                ivy.setDeltaMovement(0.0D, 0.0D, 0.0D);
                ivy.fallDistance = 0.0F;
                phase = Phase.CLIMB;
            }
        }

        private boolean isAtLadderApproach(BlockPos start) {
            boolean centered = Math.abs(ivy.getX() - (start.getX() + 0.5D)) <= 0.55D
                    && Math.abs(ivy.getZ() - (start.getZ() + 0.5D)) <= 0.55D;
            boolean adjacent = ivy.blockPosition().getY() == start.getY()
                    && Math.abs(ivy.blockPosition().getX() - start.getX()) <= 1
                    && Math.abs(ivy.blockPosition().getZ() - start.getZ()) <= 1
                    && isNearLadder(ivy.blockPosition());
            return Math.abs(ivy.getY() - start.getY()) <= 1.25D && (centered || adjacent);
        }

        private void tickClimb() {
            BlockPos currentLadder = plan.ladderAt(Mth.floor(ivy.getY()));
            if (!isLadder(currentLadder)) {
                currentLadder = plan.startLadder();
                if (Math.abs(ivy.getY() - currentLadder.getY()) > 0.35D) {
                    ivy.moveTo(ivy.getX(), currentLadder.getY(), ivy.getZ(), ivy.getYRot(), ivy.getXRot());
                }
            }
            centerOnLadder(currentLadder);
            ivy.setNoGravity(true);
            ivy.fallDistance = 0.0F;
            ivy.setShiftKeyDown(!plan.climbUp());
            faceLadder(currentLadder);

            double targetY = plan.exitLadder().getY();
            double deltaY = targetY - ivy.getY();
            if (Math.abs(deltaY) <= 0.18D) {
                if (!plan.exitStandable()) {
                    LadderPlan updatedPlan = findLadderPlan(owner);
                    if (updatedPlan != null && updatedPlan != plan) {
                        plan = updatedPlan;
                    }
                    if (!plan.exitStandable()) {
                        LadderPlan emergencyExit = findStandableExitNear(plan.exitLadder(), owner);
                        if (emergencyExit != null) {
                            plan = emergencyExit;
                        }
                    }
                    if (!plan.exitStandable()) {
                        centerOnLadder(plan.exitLadder());
                        faceLadder(plan.exitLadder());
                        ivy.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        ivy.hasImpulse = true;
                        return;
                    }
                    targetY = plan.exitLadder().getY();
                    deltaY = targetY - ivy.getY();
                    if (Math.abs(deltaY) > 0.18D) {
                        return;
                    }
                }
                ivy.setDeltaMovement(0.0D, 0.0D, 0.0D);
                ivy.moveTo(ivy.getX(), targetY, ivy.getZ(), ivy.getYRot(), ivy.getXRot());
                ivy.setNoGravity(false);
                ivy.setShiftKeyDown(false);
                phase = Phase.EXIT;
                exitTicks = LADDER_EXIT_TICKS;
                return;
            }

            double climb = Math.copySign(LADDER_CLIMB_SPEED, deltaY);
            ivy.setDeltaMovement(0.0D, climb, 0.0D);
            ivy.hasImpulse = true;
        }

        private void tickExit() {
            Vec3 exitCenter = Vec3.atBottomCenterOf(plan.exitFeet());
            Vec3 toExit = exitCenter.subtract(ivy.position());
            ivy.fallDistance = 0.0F;
            if (toExit.horizontalDistanceSqr() > 0.09D && exitTicks > 0) {
                Vec3 horizontal = new Vec3(toExit.x, 0.0D, toExit.z).normalize().scale(LADDER_EXIT_SPEED);
                double vertical = Mth.clamp(toExit.y * 0.25D, -0.12D, 0.18D);
                ivy.setDeltaMovement(horizontal.x, vertical, horizontal.z);
                ivy.hasImpulse = true;
                exitTicks--;
            } else {
                ivy.getNavigation().stop();
                ivy.moveTo(exitCenter.x, exitCenter.y, exitCenter.z, ivy.getYRot(), ivy.getXRot());
                ivy.setDeltaMovement(0.0D, Math.min(ivy.getDeltaMovement().y, 0.0D), 0.0D);
                phase = Phase.DONE;
            }
        }

        private void centerOnLadder(BlockPos ladderPos) {
            ivy.moveTo(ladderPos.getX() + 0.5D, ivy.getY(), ladderPos.getZ() + 0.5D, ivy.getYRot(), ivy.getXRot());
        }

        private void faceLadder(BlockPos ladderPos) {
            BlockState state = ivy.level().getBlockState(ladderPos);
            if (state.hasProperty(LadderBlock.FACING)) {
                Direction facing = state.getValue(LadderBlock.FACING);
                int yRot = facing.getOpposite().get2DDataValue() * 90;
                ivy.setYRot(yRot);
                ivy.setYHeadRot(yRot);
            }
        }

        @Nullable
        private LadderPlan findLadderPlan(LivingEntity owner) {
            Level level = ivy.level();
            int minY = Math.max(level.getMinBuildHeight(), Math.min(Mth.floor(ivy.getY()), Mth.floor(owner.getY())) - LADDER_VERTICAL_MARGIN);
            int maxY = Math.min(level.getMaxBuildHeight() - 1, Math.max(Mth.floor(ivy.getY()), Mth.floor(owner.getY())) + LADDER_VERTICAL_MARGIN);
            BlockPos ownerPos = owner.blockPosition();
            BlockPos ivyPos = ivy.blockPosition();
            LadderPlan best = null;
            double bestScore = Double.MAX_VALUE;
            double ownerYDelta = Math.abs(owner.getY() - ivy.getY());
            boolean allowTrackingExit = ownerYDelta >= LADDER_MIN_OWNER_Y_DELTA || isNearLadder(owner.blockPosition());

            int minX = Math.min(ownerPos.getX(), ivyPos.getX()) - LADDER_SEARCH_RADIUS;
            int maxX = Math.max(ownerPos.getX(), ivyPos.getX()) + LADDER_SEARCH_RADIUS;
            int minZ = Math.min(ownerPos.getZ(), ivyPos.getZ()) - LADDER_SEARCH_RADIUS;
            int maxZ = Math.max(ownerPos.getZ(), ivyPos.getZ()) + LADDER_SEARCH_RADIUS;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    LadderPlan candidate = findPlanInColumn(owner, x, z, minY, maxY, allowTrackingExit);
                    if (candidate == null) {
                        continue;
                    }
                    double score = candidate.score(owner);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            return best;
        }

        @Nullable
        private LadderPlan findPlanInColumn(LivingEntity owner, int x, int z, int minY, int maxY, boolean allowTrackingExit) {
            LadderPlan best = null;
            double bestScore = Double.MAX_VALUE;
            for (int startY = minY; startY <= maxY; startY++) {
                BlockPos start = new BlockPos(x, startY, z);
                if (!isLadder(start)) {
                    continue;
                }
                if (Math.abs(startY - ivy.getY()) > LADDER_MAX_START_Y_DELTA) {
                    continue;
                }
                if (!hasReachableLadderBase(start)) {
                    continue;
                }
                for (int exitY = minY; exitY <= maxY; exitY++) {
                    BlockPos exitLadder = new BlockPos(x, exitY, z);
                    if (!isLadder(exitLadder)) {
                        continue;
                    }
                    if (Math.abs(exitY - startY) < LADDER_MIN_CLIMB_BLOCKS) {
                        continue;
                    }
                    if (Math.abs(exitY - owner.getY()) > LADDER_MAX_EXIT_OWNER_Y_DELTA) {
                        continue;
                    }
                    if (!isTowardOwnerY(startY, exitY, owner)) {
                        continue;
                    }
                    if (!isContinuousLadder(x, z, startY, exitY)) {
                        continue;
                    }
                    Direction exitDirection = bestExitDirection(exitLadder, owner);
                    boolean exitStandable = exitDirection != null;
                    if (exitDirection == null) {
                        if (!allowTrackingExit) {
                            continue;
                        }
                        exitDirection = trackingExitDirection(exitLadder, owner);
                    }
                    LadderPlan candidate = new LadderPlan(start, exitLadder, exitDirection, exitStandable, null);
                    double score = candidate.score(owner);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            return best;
        }

        private boolean isTowardOwnerY(int startY, int exitY, LivingEntity owner) {
            return owner.getY() > ivy.getY()
                    ? exitY > startY
                    : exitY < startY;
        }

        private boolean isContinuousLadder(int x, int z, int startY, int exitY) {
            int min = Math.min(startY, exitY);
            int max = Math.max(startY, exitY);
            for (int y = min; y <= max; y++) {
                if (!isLadder(new BlockPos(x, y, z))) {
                    return false;
                }
            }
            return true;
        }

        @Nullable
        private Direction bestExitDirection(BlockPos ladderPos, LivingEntity owner) {
            BlockState state = ivy.level().getBlockState(ladderPos);
            Direction preferred = state.hasProperty(LadderBlock.FACING)
                    ? state.getValue(LadderBlock.FACING).getOpposite()
                    : null;
            Direction best = null;
            double bestScore = Double.MAX_VALUE;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (preferred != null && direction != preferred) {
                    continue;
                }
                BlockPos exitFeet = ladderPos.relative(direction);
                if (!canStandAt(exitFeet)) {
                    continue;
                }
                double score = exitFeet.distToCenterSqr(owner.position());
                if (score < bestScore) {
                    bestScore = score;
                    best = direction;
                }
            }
            if (best != null || preferred == null) {
                return best;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos exitFeet = ladderPos.relative(direction);
                if (!canStandAt(exitFeet)) {
                    continue;
                }
                double score = exitFeet.distToCenterSqr(owner.position()) + 4.0D;
                if (score < bestScore) {
                    bestScore = score;
                    best = direction;
                }
            }
            return best;
        }

        private boolean canStandAt(BlockPos feet) {
            Level level = ivy.level();
            BlockPos floor = feet.below();
            return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
        }

        @Nullable
        private LadderPlan findStandableExitNear(BlockPos ladderPos, LivingEntity owner) {
            LadderPlan best = null;
            double bestScore = Double.MAX_VALUE;
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                BlockPos anchor = ladderPos.offset(0, yOffset, 0);
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos feet = anchor.relative(direction);
                    if (!canStandAt(feet)) {
                        continue;
                    }
                    double score = feet.distToCenterSqr(owner.position()) + Math.abs(feet.getY() - owner.getY()) * 4.0D;
                    if (score < bestScore) {
                        bestScore = score;
                        best = new LadderPlan(ladderPos, ladderPos, direction, true, feet);
                    }
                }
            }
            return best;
        }

        private Direction trackingExitDirection(BlockPos ladderPos, LivingEntity owner) {
            BlockState state = ivy.level().getBlockState(ladderPos);
            if (state.hasProperty(LadderBlock.FACING)) {
                return state.getValue(LadderBlock.FACING).getOpposite();
            }
            Direction best = Direction.NORTH;
            double bestScore = Double.MAX_VALUE;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                double score = ladderPos.relative(direction).distToCenterSqr(owner.position());
                if (score < bestScore) {
                    bestScore = score;
                    best = direction;
                }
            }
            return best;
        }

        private boolean hasReachableLadderBase(BlockPos ladderPos) {
            if (Math.abs(ladderPos.getY() - ivy.getY()) <= 0.25D) {
                return true;
            }
            BlockPos below = ladderPos.below();
            return ladderPos.getY() > ivy.getY()
                    && levelHasSolidFloor(below)
                    && ivy.level().getBlockState(below).getCollisionShape(ivy.level(), below).isEmpty();
        }

        private boolean levelHasSolidFloor(BlockPos feet) {
            BlockPos floor = feet.below();
            return ivy.level().getBlockState(floor).isFaceSturdy(ivy.level(), floor, Direction.UP);
        }

        private boolean isLadder(BlockPos pos) {
            BlockState state = ivy.level().getBlockState(pos);
            return state.is(Blocks.LADDER);
        }

        private boolean isNearLadder(BlockPos pos) {
            if (isLadder(pos)) {
                return true;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (isLadder(pos.relative(direction))) {
                    return true;
                }
            }
            return false;
        }

        private enum Phase {
            APPROACH,
            CLIMB,
            EXIT,
            DONE
        }
    }

    private record LadderPlan(BlockPos startLadder, BlockPos exitLadder, Direction exitDirection, boolean exitStandable,
                              @Nullable BlockPos explicitExitFeet) {
        boolean climbUp() {
            return exitLadder.getY() >= startLadder.getY();
        }

        BlockPos ladderAt(int y) {
            return new BlockPos(startLadder.getX(), y, startLadder.getZ());
        }

        BlockPos exitFeet() {
            return explicitExitFeet != null ? explicitExitFeet : exitLadder.relative(exitDirection);
        }

        double score(LivingEntity owner) {
            return Math.abs(exitFeet().getY() - owner.getY()) * 8.0D
                    + exitFeet().distToCenterSqr(owner.position())
                    + startLadder.distToCenterSqr(owner.position()) * 0.02D
                    + (exitStandable ? 0.0D : 24.0D);
        }
    }

    private class OwnerDefenseGoal extends Goal {
        private LivingEntity target;

        OwnerDefenseGoal() {
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!canDefendOwner()) {
                rememberOwnerCombatTimestamps();
                return false;
            }
            if (!ownerCombatTimestampsSynced) {
                rememberOwnerCombatTimestamps();
                return false;
            }
            target = getValidDefenseTarget();
            return target != null;
        }

        @Override
        public void start() {
            if (target != null) {
                ivy.setTarget(target);
                rememberOwnerCombatTimestamps();
            }
        }

        @Override
        public void stop() {
            target = null;
        }
    }
}
