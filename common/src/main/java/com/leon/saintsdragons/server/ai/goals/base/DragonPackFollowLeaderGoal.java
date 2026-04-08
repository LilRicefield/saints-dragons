package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Generic pack-follow behavior for dragon species that support alpha/leader logic.
 */
public class DragonPackFollowLeaderGoal<T extends DragonEntity & PackMember<T>> extends Goal {
    private static final double AIR_MOVE_TARGET_EPSILON_SQR = 9.0D;
    private static final double AIR_MOVE_SPEED_EPSILON = 0.15D;

    private final T member;
    private final Class<T> memberClass;
    private final double followSpeed;
    private final double startFollowDistSq;
    private final double stopFollowDistSq;
    private final int maxRepathCooldown;
    private final int minRepathCooldown;

    @Nullable
    private T leader;
    private int leaderRefreshCooldown = 0;
    private int pathRecalcCooldown = 0;
    private int airMoveRefreshCooldown = 0;
    private double lastLeaderX = Double.NaN;
    private double lastLeaderY = Double.NaN;
    private double lastLeaderZ = Double.NaN;
    private Vec3 lastAirMoveTarget = null;
    private double lastAirMoveSpeed = Double.NaN;

    public DragonPackFollowLeaderGoal(T member,
                                      Class<T> memberClass,
                                      double followSpeed,
                                      double startFollowDistance,
                                      double stopFollowDistance) {
        this.member = member;
        this.memberClass = memberClass;
        this.followSpeed = followSpeed;
        this.startFollowDistSq = startFollowDistance * startFollowDistance;
        this.stopFollowDistSq = stopFollowDistance * stopFollowDistance;
        this.minRepathCooldown = 5;
        this.maxRepathCooldown = 22;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!canPackFollow()) {
            return false;
        }
        leader = resolveLeader(false);
        if (leader == null) {
            return false;
        }
        return member.distanceToSqr(leader) > startFollowDistSq;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canPackFollow()) {
            if (member instanceof RideableDragonBase rideableMember
                    && member instanceof DragonFlightCapable flightMember
                    && !flightMember.isLanding()
                    && (flightMember.isFlying() || flightMember.isTakeoff() || flightMember.isHovering())) {
                DragonAggroLandingHelper.beginAggroLanding(rideableMember, member, getAirFollowSpeed(flightMember));
                return true;
            }
            return false;
        }
        if (member instanceof DragonFlightCapable flightMember && flightMember.isLanding()) {
            return !member.onGround();
        }
        if (!isLeaderUsable(leader)) {
            leader = resolveLeader(true);
            if (leader == null) {
                return false;
            }
        }
        return member.distanceToSqr(leader) > stopFollowDistSq;
    }

    @Override
    public void start() {
        pathRecalcCooldown = 0;
        resetLeaderTracking();
    }

    @Override
    public void stop() {
        member.getNavigation().stop();
        if (member instanceof DragonFlightCapable flightMember && member instanceof RideableDragonBase) {
            flightMember.setHovering(false);
        }
        leader = null;
        pathRecalcCooldown = 0;
        resetLeaderTracking();
    }

    @Override
    public void tick() {
        if (leaderRefreshCooldown-- <= 0) {
            leaderRefreshCooldown = withJitter(member.getPackLeaderRefreshIntervalTicks(), 10);
            T refreshed = resolveLeader(true);
            if (refreshed == null) {
                stop();
                return;
            }
            leader = refreshed;
        }

        if (!isLeaderUsable(leader)) {
            return;
        }

        member.getLookControl().setLookAt(leader, 20.0F, 20.0F);

        if (handleAirPackFollowing(leader)) {
            return;
        }

        double distance = member.distanceTo(leader);
        if (distance * distance <= stopFollowDistSq) {
            member.getNavigation().stop();
            pathRecalcCooldown = 0;
            return;
        }

        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean moved = leaderMovedSignificantly(leader);
        boolean navIdle = member.getNavigation().isDone() || !member.getNavigation().isInProgress();
        if (navIdle || moved || pathRecalcCooldown <= 0) {
            member.getNavigation().moveTo(leader, followSpeed);
            rememberLeaderPosition(leader);
            pathRecalcCooldown = computePathRecalcCooldown(distance);
        }
    }

    private boolean canPackFollow() {
        if (!member.canParticipateInPack()) {
            member.setPackLeaderUuid(null);
            return false;
        }
        if (!member.isBaby() && member.hasNearbyAssignedBabies(memberClass)) {
            return false;
        }
        if (member.isOrderedToSit() || member.isVehicle() || member.isPassenger()) {
            return false;
        }
        LivingEntity target = member.getTarget();
        if (target != null && member.isTargetValid(target)) {
            return false;
        }
        return true;
    }

    @Nullable
    private T resolveLeader(boolean keepExistingIfValid) {
        if (!(member.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        UUID stored = member.getPackLeaderUuid();
        if (stored != null) {
            T storedLeader = getPackMemberByUuid(serverLevel, stored);
            if (isLeaderUsable(storedLeader) && hasCapacityForMember(storedLeader, serverLevel)) {
                // Stability first: if current leader is still valid, keep it.
                // This prevents pack-wide alpha thrashing while contenders are close in score.
                return storedLeader;
            }
        }

        T best = findBestLeader(serverLevel);
        if (best == null || best == member) {
            member.setPackLeaderUuid(member.getUUID());
            return null;
        }

        member.setPackLeaderUuid(best.getUUID());
        return best;
    }

    @Nullable
    private T findBestLeader(ServerLevel serverLevel) {
        double radius = Math.max(8.0D, member.getPackSearchRadius());
        AABB box = member.getBoundingBox().inflate(radius);
        List<T> nearby = serverLevel.getEntitiesOfClass(memberClass, box, this::isCandidateLeader);

        T best = member.canLeadPack() ? member : null;
        for (T candidate : nearby) {
            if (best == null || isBetterLeader(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean isCandidateLeader(T candidate) {
        if (!isLeaderUsable(candidate)) {
            return false;
        }
        return isPackCompatible(candidate);
    }

    private boolean isPackCompatible(T other) {
        if (other == null || other == member) {
            return false;
        }
        if (member.isTame() != other.isTame()) {
            return false;
        }
        if (member.isTame()) {
            var owner = member.getOwner();
            return owner != null && other.isOwnedBy(owner);
        }
        return true;
    }

    private boolean isBetterLeader(T candidate, T currentBest) {
        int candidatePriority = candidate.getPackLeadershipPriority();
        int currentPriority = currentBest.getPackLeadershipPriority();
        if (candidatePriority != currentPriority) {
            return candidatePriority > currentPriority;
        }

        double candidateHealthRatio = candidate.getHealth() / Math.max(1.0F, candidate.getMaxHealth());
        double currentHealthRatio = currentBest.getHealth() / Math.max(1.0F, currentBest.getMaxHealth());
        if (Math.abs(candidateHealthRatio - currentHealthRatio) > 0.0001D) {
            return candidateHealthRatio > currentHealthRatio;
        }

        return compareUuid(candidate.getUUID(), currentBest.getUUID()) < 0;
    }

    private boolean hasCapacityForMember(T candidateLeader, ServerLevel serverLevel) {
        int maxPack = Math.max(1, candidateLeader.getMaxPackSize());
        UUID currentLeader = member.getPackLeaderUuid();
        if (currentLeader != null && currentLeader.equals(candidateLeader.getUUID())) {
            return true;
        }
        if (maxPack <= 1) {
            return false;
        }

        double radius = Math.max(8.0D, candidateLeader.getPackSearchRadius());
        AABB box = candidateLeader.getBoundingBox().inflate(radius);
        List<T> nearby = serverLevel.getEntitiesOfClass(memberClass, box, this::isPackCompatibleOrLeader);

        int followers = 0;
        UUID leaderId = candidateLeader.getUUID();
        for (T mate : nearby) {
            if (mate == candidateLeader) {
                continue;
            }
            UUID mateLeader = mate.getPackLeaderUuid();
            if (leaderId.equals(mateLeader)) {
                followers++;
            }
        }
        return followers < maxPack - 1;
    }

    private boolean isPackCompatibleOrLeader(T other) {
        if (other == candidateSafeSelf()) {
            return true;
        }
        return other != null && other.canParticipateInPack() && (other == member || isPackCompatible(other));
    }

    @Nullable
    private T candidateSafeSelf() {
        return member;
    }

    private boolean isLeaderUsable(@Nullable T candidate) {
        if (candidate == null || candidate == member) {
            return false;
        }
        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }
        return candidate.canLeadPack() && candidate.canParticipateInPack() && isPackCompatible(candidate);
    }

    @Nullable
    private T getPackMemberByUuid(ServerLevel serverLevel, UUID uuid) {
        Entity entity = serverLevel.getEntity(uuid);
        if (entity == null || !memberClass.isInstance(entity)) {
            return null;
        }
        return memberClass.cast(entity);
    }

    private int compareUuid(UUID a, UUID b) {
        int msb = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        if (msb != 0) {
            return msb;
        }
        return Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    private int withJitter(int base, int jitter) {
        int clampedBase = Math.max(20, base);
        return clampedBase + member.getRandom().nextInt(Math.max(1, jitter + 1));
    }

    private int computePathRecalcCooldown(double distance) {
        return Mth.clamp((int) Math.ceil(distance * 0.40D), minRepathCooldown, maxRepathCooldown);
    }

    private boolean leaderMovedSignificantly(T currentLeader) {
        if (Double.isNaN(lastLeaderX)) {
            return true;
        }
        double dx = currentLeader.getX() - this.lastLeaderX;
        double dy = currentLeader.getY() - this.lastLeaderY;
        double dz = currentLeader.getZ() - this.lastLeaderZ;
        return dx * dx + dy * dy + dz * dz > 1.0D;
    }

    private void rememberLeaderPosition(T currentLeader) {
        this.lastLeaderX = currentLeader.getX();
        this.lastLeaderY = currentLeader.getY();
        this.lastLeaderZ = currentLeader.getZ();
    }

    private void resetLeaderTracking() {
        this.lastLeaderX = Double.NaN;
        this.lastLeaderY = Double.NaN;
        this.lastLeaderZ = Double.NaN;
        this.airMoveRefreshCooldown = 0;
        this.lastAirMoveTarget = null;
        this.lastAirMoveSpeed = Double.NaN;
    }

    private boolean handleAirPackFollowing(T currentLeader) {
        if (!(member instanceof RideableDragonBase rideableMember) || !(member instanceof DragonFlightCapable flightMember)) {
            return false;
        }
        if (!(currentLeader instanceof DragonFlightCapable flightLeader)) {
            return false;
        }

        if (airMoveRefreshCooldown > 0) {
            airMoveRefreshCooldown--;
        }

        boolean leaderAirborne = isDragonAirborne(flightLeader, currentLeader);
        boolean memberAirborne = isDragonAirborne(flightMember, rideableMember);
        if (member.isBaby()) {
            if (flightMember.isFlying() || flightMember.isTakeoff() || flightMember.isHovering() || flightMember.isLanding()) {
                flightMember.setFlying(false);
                flightMember.setTakeoff(false);
                flightMember.setHovering(false);
                flightMember.setLanding(false);
            }
            return false;
        }
        if (!leaderAirborne && !memberAirborne) {
            return false;
        }

        if (flightMember.isLanding()) {
            if (!rideableMember.getNavigation().isInProgress()) {
                DragonAggroLandingHelper.beginAggroLanding(rideableMember, currentLeader, getAirFollowSpeed(flightMember));
            }
            return true;
        }

        if (leaderAirborne && !flightMember.isFlying() && !flightMember.isTakeoff() && flightMember.canTakeoff()) {
            flightMember.setFlying(true);
            flightMember.setTakeoff(true);
            flightMember.setLanding(false);
            flightMember.setHovering(false);
            resetLeaderTracking();
        }

        Vec3 target = getAirFollowTarget(currentLeader);
        double distanceToTargetSq = rideableMember.distanceToSqr(target.x, target.y, target.z);
        if (!leaderAirborne && distanceToTargetSq <= stopFollowDistSq) {
            if (flightMember.isFlying() || flightMember.isHovering()) {
                DragonAggroLandingHelper.beginAggroLanding(rideableMember, currentLeader, getAirFollowSpeed(flightMember));
            }
            pathRecalcCooldown = 0;
            return true;
        }

        if (distanceToTargetSq > 1.0D) {
            requestAirMove(rideableMember, target, getAirFollowSpeed(flightMember));
        } else {
            rideableMember.getNavigation().stop();
        }
        rememberLeaderPosition(currentLeader);
        return true;
    }

    private boolean isDragonAirborne(DragonFlightCapable dragon, Entity entity) {
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering() || dragon.isLanding()) {
            return true;
        }
        if (entity.onGround()) {
            return false;
        }
        BlockPos pos = entity.blockPosition();
        int groundY = entity.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY();
        return entity.getY() - groundY > 4.0D;
    }

    private Vec3 getAirFollowTarget(T currentLeader) {
        Vec3 leaderLook = currentLeader.getLookAngle();
        Vec3 lateral = new Vec3(-leaderLook.z, 0.0D, leaderLook.x);
        if (lateral.lengthSqr() < 1.0E-4D) {
            lateral = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            lateral = lateral.normalize();
        }

        int slot = Math.floorMod(member.getUUID().hashCode(), 3);
        double lateralOffset = switch (slot) {
            case 0 -> -3.0D;
            case 1 -> 3.0D;
            default -> 0.0D;
        };
        double trailingOffset = slot == 2 ? 5.0D : 3.5D;
        double hoverOffset = slot == 2 ? 1.5D : 2.0D;

        return currentLeader.position()
                .subtract(leaderLook.scale(trailingOffset))
                .add(lateral.scale(lateralOffset))
                .add(0.0D, currentLeader.getBbHeight() + hoverOffset, 0.0D);
    }

    private double getAirFollowSpeed(DragonFlightCapable flightMember) {
        return Math.max(1.0D, flightMember.getFlightSpeed() * 1.05D);
    }

    private void requestAirMove(RideableDragonBase rideableMember, Vec3 target, double speed) {
        if (member.handleDirectAirPackFollow(target, speed)) {
            lastAirMoveTarget = target;
            lastAirMoveSpeed = speed;
            airMoveRefreshCooldown = airMoveRefreshInterval(speed);
            return;
        }
        if (shouldRefreshAirMoveTarget(target, speed)) {
            rideableMember.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
            lastAirMoveTarget = target;
            lastAirMoveSpeed = speed;
            airMoveRefreshCooldown = airMoveRefreshInterval(speed);
        }
    }

    private boolean shouldRefreshAirMoveTarget(Vec3 target, double speed) {
        if (lastAirMoveTarget == null || airMoveRefreshCooldown <= 0) {
            return true;
        }
        if (target.distanceToSqr(lastAirMoveTarget) > AIR_MOVE_TARGET_EPSILON_SQR) {
            return true;
        }
        return Math.abs(speed - lastAirMoveSpeed) > AIR_MOVE_SPEED_EPSILON;
    }

    private int airMoveRefreshInterval(double speed) {
        if (speed >= 1.4D) {
            return 3;
        }
        if (speed >= 1.0D) {
            return 5;
        }
        return 7;
    }
}
