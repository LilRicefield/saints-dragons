package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DragonPackFollowBehaviour<T extends RideableFlyingDragon & PackMember<T>>
        extends DragonBehaviour<T> {
    private static final double AIR_TARGET_EPSILON_SQR = 9.0D;
    private static final double AIR_SPEED_EPSILON = 0.15D;
    private static final double AIR_CATCH_UP_DISTANCE = 18.0D;
    private static final double AIR_CATCH_UP_MULTIPLIER = 1.35D;

    private final Class<T> memberClass;
    private final double followSpeed;
    private final double startDistanceSq;
    private final double stopDistanceSq;
    @Nullable
    private T leader;
    private int leaderRefreshCooldown;
    private int groundRepathCooldown;
    private int airRefreshCooldown;
    private double lastLeaderX = Double.NaN;
    private double lastLeaderY = Double.NaN;
    private double lastLeaderZ = Double.NaN;
    @Nullable
    private Vec3 lastAirTarget;
    private double lastAirSpeed = Double.NaN;
    private String mode = "idle";

    public DragonPackFollowBehaviour(Class<T> memberClass,
                                     double followSpeed,
                                     double startDistance,
                                     double stopDistance) {
        super(Map.of(DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED));
        this.memberClass = memberClass;
        this.followSpeed = followSpeed;
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
        if (!canFollow(member)) {
            if (shouldLandWhenFollowEnds(member)
                    && member.canFly()
                    && member.isAerial()
                    && !member.isLanding()) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.landing(member, airSpeed(member, false))
                );
                return true;
            }
            return false;
        }
        if (member.isLanding()) return !member.onGround();
        if (!usableLeader(member, leader)) {
            leader = resolveLeader(context.level(), member);
        }
        return leader != null && member.distanceToSqr(leader) > stopDistanceSq;
    }

    protected boolean shouldLandWhenFollowEnds(T member) {
        return true;
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
        if (followInAir(context, member, leader)) return;

        mode = "ground";
        double distance = member.distanceTo(leader);
        if (distance * distance <= stopDistanceSq) {
            member.getAIMovement().stop();
            groundRepathCooldown = 0;
            return;
        }
        if (groundRepathCooldown > 0) groundRepathCooldown--;
        boolean idle = member.getAIMovement().hasArrived() || !member.getAIMovement().isPathing();
        if (idle || leaderMoved(leader) || groundRepathCooldown <= 0) {
            member.getAIMovement().setGroundWaypoint(leader, followSpeed);
            remember(leader);
            groundRepathCooldown = Mth.clamp((int)Math.ceil(distance * 0.4D), 5, 22);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        T member = context.dragon();
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        member.getAIMovement().stop();
        member.setHovering(false);
        member.setAccelerating(false);
        leader = null;
        mode = "idle";
        resetTracking();
    }

    private boolean canFollow(T member) {
        if (!member.canParticipateInPack()) {
            member.setPackLeaderUuid(null);
            return false;
        }
        if (!member.isBaby() && member.hasNearbyAssignedBabies(memberClass)) return false;
        if (member.isOrderedToSit() || member.isVehicle() || member.isPassenger() || member.isInLove()) return false;
        LivingEntity target = member.getTarget();
        return target == null || !member.isTargetValid(target);
    }

    private boolean followInAir(DragonBrainContext<T> context, T member, T currentLeader) {
        if (airRefreshCooldown > 0) airRefreshCooldown--;
        boolean leaderAirborne = isAirborne(currentLeader);
        boolean memberAirborne = isAirborne(member);
        if (!leaderAirborne && !memberAirborne) return false;

        if (member.isLanding()) {
            mode = "landing";
            if (!member.getAIMovement().isPathing()) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.landing(currentLeader, airSpeed(member, false))
                );
            }
            return true;
        }

        if (leaderAirborne && !member.isFlying() && !member.isTakeoff() && member.canTakeoff()) {
            member.setFlying(true);
            member.setTakeoff(true);
            member.setLanding(false);
            member.setHovering(false);
            resetTracking();
        }

        Vec3 target = airTarget(member, currentLeader);
        double distanceSq = member.distanceToSqr(target);
        if (!leaderAirborne && distanceSq <= stopDistanceSq) {
            mode = "landing";
            if (member.isFlying() || member.isHovering()) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.landing(currentLeader, airSpeed(member, false))
                );
            }
            groundRepathCooldown = 0;
            return true;
        }

        mode = "air";
        if (distanceSq > 1.0D) {
            boolean catchUp = distanceSq > AIR_CATCH_UP_DISTANCE * AIR_CATCH_UP_DISTANCE;
            double speed = airSpeed(member, catchUp);
            member.setAccelerating(catchUp);
            if (member.handleDirectAirPackFollow(target, speed)) {
                rememberAirTarget(target, speed);
            } else if (shouldRefreshAirTarget(target, speed)) {
                context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.auto(target, speed));
                rememberAirTarget(target, speed);
            }
        } else {
            member.setAccelerating(false);
            member.getAIMovement().stop();
        }
        remember(currentLeader);
        return true;
    }

    private boolean isAirborne(T dragon) {
        if (dragon.isAerial()) return true;
        if (dragon.onGround()) return false;
        BlockPos pos = dragon.blockPosition();
        int groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY();
        return dragon.getY() - groundY > 4.0D;
    }

    private Vec3 airTarget(T member, T currentLeader) {
        Vec3 leaderLook = currentLeader.getLookAngle();
        Vec3 lateral = new Vec3(-leaderLook.z, 0.0D, leaderLook.x);
        lateral = lateral.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : lateral.normalize();
        int slot = Math.floorMod(member.getUUID().hashCode(), 3);
        double lateralOffset = slot == 0 ? -3.0D : slot == 1 ? 3.0D : 0.0D;
        double trailingOffset = slot == 2 ? 5.0D : 3.5D;
        double hoverOffset = slot == 2 ? 1.5D : 2.0D;
        return currentLeader.position()
                .subtract(leaderLook.scale(trailingOffset))
                .add(lateral.scale(lateralOffset))
                .add(0.0D, currentLeader.getBbHeight() + hoverOffset, 0.0D);
    }

    private double airSpeed(T member, boolean catchUp) {
        double speed = Math.max(1.0D, member.getFlightSpeed() * 1.05D);
        return catchUp ? speed * AIR_CATCH_UP_MULTIPLIER : speed;
    }

    private boolean shouldRefreshAirTarget(Vec3 target, double speed) {
        return lastAirTarget == null
                || airRefreshCooldown <= 0
                || target.distanceToSqr(lastAirTarget) > AIR_TARGET_EPSILON_SQR
                || Math.abs(speed - lastAirSpeed) > AIR_SPEED_EPSILON;
    }

    private void rememberAirTarget(Vec3 target, double speed) {
        lastAirTarget = target;
        lastAirSpeed = speed;
        airRefreshCooldown = speed >= 1.4D ? 3 : speed >= 1.0D ? 5 : 7;
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
        List<T> nearby = level.getEntitiesOfClass(memberClass, member.getBoundingBox().inflate(radius),
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
                && member.isTame() == candidate.isTame()
                && (!member.isTame() || member.getOwner() != null && candidate.isOwnedBy(member.getOwner()));
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
        List<T> nearby = level.getEntitiesOfClass(memberClass, box,
                other -> other == candidate || compatibleMember(member, other));
        long followers = nearby.stream()
                .filter(other -> other != candidate && leaderId.equals(other.getPackLeaderUuid()))
                .count();
        if (!leaderId.equals(member.getPackLeaderUuid())) return followers < max - 1;
        if (followers <= max - 1) return true;
        long rank = nearby.stream()
                .filter(other -> other != candidate && other != member)
                .filter(other -> leaderId.equals(other.getPackLeaderUuid()))
                .filter(other -> compareUuid(other.getUUID(), member.getUUID()) < 0)
                .count();
        return rank < max - 1;
    }

    private boolean compatibleMember(T member, T other) {
        return other != null
                && other.canParticipateInPack()
                && member.isTame() == other.isTame()
                && (!member.isTame() || member.getOwner() != null && other.isOwnedBy(member.getOwner()));
    }

    private int compareUuid(UUID first, UUID second) {
        int most = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return most != 0 ? most : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    private boolean leaderMoved(T current) {
        if (Double.isNaN(lastLeaderX)) return true;
        double dx = current.getX() - lastLeaderX;
        double dy = current.getY() - lastLeaderY;
        double dz = current.getZ() - lastLeaderZ;
        return dx * dx + dy * dy + dz * dz > 1.0D;
    }

    private void remember(T current) {
        lastLeaderX = current.getX();
        lastLeaderY = current.getY();
        lastLeaderZ = current.getZ();
    }

    private void resetTracking() {
        groundRepathCooldown = 0;
        airRefreshCooldown = 0;
        lastLeaderX = lastLeaderY = lastLeaderZ = Double.NaN;
        lastAirTarget = null;
        lastAirSpeed = Double.NaN;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "leader", leader == null ? "none" : leader.getName().getString(),
                "mode", mode,
                "ground_repath", Integer.toString(groundRepathCooldown),
                "air_refresh", Integer.toString(airRefreshCooldown)
        );
    }
}
