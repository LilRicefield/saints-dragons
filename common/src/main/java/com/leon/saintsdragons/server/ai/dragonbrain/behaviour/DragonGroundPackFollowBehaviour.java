package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.navigation.DragonGroundRequest;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DragonGroundPackFollowBehaviour<T extends RideableDragonBase & PackMember<T>>
        extends DragonBehaviour<T> {
    private final Class<T> memberClass;
    private final double walkSpeed;
    private final double runSpeed;
    private final double runDistanceSq;
    private final double startDistanceSq;
    private final double stopDistanceSq;
    @Nullable
    private T leader;
    private int leaderRefreshCooldown;
    private boolean running;
    private boolean movementModeInitialized;

    public DragonGroundPackFollowBehaviour(Class<T> memberClass,
                                           double startDistance,
                                           double stopDistance) {
        this.memberClass = memberClass;
        this.walkSpeed = DragonAdultOwnerFollowTuning.WALK_SPEED;
        this.runSpeed = DragonAdultOwnerFollowTuning.RUN_SPEED;
        this.runDistanceSq = DragonAdultOwnerFollowTuning.RUN_DISTANCE
                * DragonAdultOwnerFollowTuning.RUN_DISTANCE;
        this.startDistanceSq = startDistance * startDistance;
        this.stopDistanceSq = stopDistance * stopDistance;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T member = context.dragon();
        if (!canFollow(member)) return false;
        leader = resolveLeader(context.level(), member);
        return leader != null && member.distanceToSqr(leader) > startDistanceSq;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T member = context.dragon();
        if (!canFollow(member)) return false;
        if (!usableLeader(member, leader)) {
            leader = resolveLeader(context.level(), member);
        }
        return leader != null && member.distanceToSqr(leader) > stopDistanceSq;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetTracking();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T member = context.dragon();
        if (leaderRefreshCooldown-- <= 0) {
            leaderRefreshCooldown = Math.max(20, member.getPackLeaderRefreshIntervalTicks())
                    + member.getRandom().nextInt(11);
            leader = resolveLeader(context.level(), member);
        }
        if (!usableLeader(member, leader)) return;

        member.getLookControl().setLookAt(leader, 20.0F, 20.0F);
        double distance = member.distanceTo(leader);
        if (distance * distance <= stopDistanceSq) {
            member.setAccelerating(false);
            member.getAIMovement().stop();
            movementModeInitialized = false;
            return;
        }
        boolean shouldRun = distance * distance > runDistanceSq;
        member.setAccelerating(shouldRun);
        member.getAIMovement().requestGroundMovement(DragonGroundRequest.travel(
                leader.position(), shouldRun ? runSpeed : walkSpeed, shouldRun,
                DragonGroundRequest.Arrival.within(Math.sqrt(stopDistanceSq))));
        running = shouldRun;
        movementModeInitialized = true;
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAIMovement().stop();
        context.dragon().setAccelerating(false);
        leader = null;
        resetTracking();
    }

    private boolean canFollow(T member) {
        if (!member.canParticipateInPack()) {
            member.setPackLeaderUuid(null);
            return false;
        }
        if (!member.isBaby() && member.hasNearbyAssignedBabies(memberClass)) return false;
        if (member.isOrderedToSit() || member.isVehicle() || member.isPassenger() || member.isInLove()) return false;
        if (!member.canSwim() && member.isInWaterOrBubble()) return false;
        LivingEntity target = member.getTarget();
        return target == null || !member.isTargetValid(target);
    }

    @Nullable
    private T resolveLeader(ServerLevel level, T member) {
        UUID stored = member.getPackLeaderUuid();
        if (stored != null) {
            Entity entity = level.getEntity(stored);
            if (memberClass.isInstance(entity)) {
                T storedLeader = memberClass.cast(entity);
                if (usableLeader(member, storedLeader) && hasCapacity(level, member, storedLeader)) {
                    return storedLeader;
                }
            }
        }

        double radius = Math.max(8.0D, member.getPackSearchRadius());
        List<T> nearby = member.getBrainUtilities().nearby().find(level, memberClass, member.getBoundingBox().inflate(radius),
                candidate -> candidate == member || usableLeader(member, candidate));
        T best = member.canLeadPack() ? member : null;
        for (T candidate : nearby) {
            if (candidate != member && hasCapacity(level, member, candidate)
                    && (best == null || betterLeader(candidate, best))) {
                best = candidate;
            }
        }
        if (best == null || best == member) {
            member.setPackLeaderUuid(member.getUUID());
            return null;
        }
        member.setPackLeaderUuid(best.getUUID());
        return best;
    }

    private boolean usableLeader(T member, @Nullable T candidate) {
        return candidate != null && candidate != member && candidate.isAlive() && !candidate.isRemoved()
                && candidate.canLeadPack() && candidate.canParticipateInPack()
                && member.isTame() == candidate.isTame();
    }

    private boolean betterLeader(T candidate, T current) {
        int priority = Integer.compare(candidate.getPackLeadershipPriority(), current.getPackLeadershipPriority());
        if (priority != 0) return priority > 0;
        double health = candidate.getHealth() / Math.max(1.0F, candidate.getMaxHealth());
        double currentHealth = current.getHealth() / Math.max(1.0F, current.getMaxHealth());
        if (Math.abs(health - currentHealth) > 0.0001D) return health > currentHealth;
        return compareUuid(candidate.getUUID(), current.getUUID()) < 0;
    }

    private boolean hasCapacity(ServerLevel level, T member, T candidate) {
        int max = Math.max(1, candidate.getMaxPackSize());
        if (max <= 1) return false;
        AABB box = candidate.getBoundingBox().inflate(Math.max(8.0D, candidate.getPackSearchRadius()));
        UUID leaderId = candidate.getUUID();
        long followers = member.getBrainUtilities().nearby().find(level, memberClass, box,
                        other -> other != candidate && leaderId.equals(other.getPackLeaderUuid()))
                .stream().count();
        return leaderId.equals(member.getPackLeaderUuid()) ? followers <= max - 1 : followers < max - 1;
    }

    private int compareUuid(UUID first, UUID second) {
        int most = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return most != 0 ? most : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    private void resetTracking() {
        running = false;
        movementModeInitialized = false;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "leader", leader == null ? "none" : leader.getName().getString(),
                "pace", movementModeInitialized ? running ? "run" : "walk" : "idle"
        );
    }
}
