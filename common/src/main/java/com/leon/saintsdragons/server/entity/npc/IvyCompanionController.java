package com.leon.saintsdragons.server.entity.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
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

    private final IvyTheDragonMerchant ivy;
    private int ownerLastHurtByTimestamp = -1;
    private int ownerLastHurtMobTimestamp = -1;
    private boolean ownerCombatTimestampsSynced = false;

    IvyCompanionController(IvyTheDragonMerchant ivy) {
        this.ivy = ivy;
    }

    void tick() {
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
