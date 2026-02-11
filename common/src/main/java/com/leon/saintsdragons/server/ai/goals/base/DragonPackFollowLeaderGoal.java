package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Generic pack-follow behavior for dragon species that support alpha/leader logic.
 */
public class DragonPackFollowLeaderGoal<T extends DragonEntity & PackMember<T>> extends Goal {
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
    private double lastLeaderX = Double.NaN;
    private double lastLeaderY = Double.NaN;
    private double lastLeaderZ = Double.NaN;

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
            return false;
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
    }
}
