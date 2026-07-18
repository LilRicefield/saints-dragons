package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class NulljawPackCombatCoordinator {
    private static final double ORBIT_RADIUS = 8.5D;
    private static final double STAGE_RADIUS = 10.0D;
    private static final double EGRESS_DISTANCE = 11.0D;
    private static final double ORBIT_ANGULAR_SPEED = 0.035D;
    private static final double STAGE_ARRIVAL_DISTANCE_SQR = 9.0D;
    private static final double EGRESS_ARRIVAL_DISTANCE_SQR = 12.25D;
    private static final double TARGET_PREDICTION_TICKS = 2.0D;
    private static final int MEMBERSHIP_REFRESH_TICKS = 10;
    private static final int FORMATION_WARMUP_TICKS = 20;
    private static final int PINCER_STAGGER_TICKS = 6;
    private static final int BITE_COMMIT_TICKS = 7;
    private static final int STAGE_TIMEOUT_TICKS = 55;
    private static final int DIVE_TIMEOUT_TICKS = 35;
    private static final int EGRESS_TIMEOUT_TICKS = 55;
    private static final int GROUP_EXPIRY_TICKS = 200;

    private static final Map<ServerLevel, Map<GroupKey, GroupState>> GROUPS = new WeakHashMap<>();

    private NulljawPackCombatCoordinator() {
    }

    public enum Phase {
        ORBIT,
        STAGE,
        DIVE,
        EGRESS
    }

    public record Directive(Phase phase,
                            Vec3 waypoint,
                            boolean mayBite,
                            boolean attackReserved,
                            int formationSlot,
                            int formationSize,
                            UUID combatLeaderUuid) {
    }

    public static Directive getDirective(Nulljaw dragon, LivingEntity target) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return soloDirective(dragon, target);
        }

        UUID leaderUuid = dragon.getCombatFormationLeaderUuid();
        if (leaderUuid == null) {
            leaderUuid = dragon.getUUID();
            dragon.setCombatFormationLeaderUuid(leaderUuid);
        }

        long gameTime = serverLevel.getGameTime();
        Map<GroupKey, GroupState> levelGroups = GROUPS.computeIfAbsent(serverLevel, ignored -> new HashMap<>());
        pruneExpiredGroups(levelGroups, gameTime);

        GroupKey key = new GroupKey(leaderUuid, target.getUUID());
        GroupState group = levelGroups.computeIfAbsent(key, ignored -> new GroupState(gameTime));
        group.lastTouchedAt = gameTime;
        group.refreshMembersIfNeeded(serverLevel, dragon, target, leaderUuid, gameTime);
        group.scheduleWaveIfReady(target, gameTime);

        Directive directive = group.directiveFor(dragon, target, leaderUuid, gameTime);
        dragon.setCombatFormationDebug(
                directive.phase().name(),
                directive.formationSlot(),
                directive.formationSize(),
                directive.attackReserved()
        );
        return directive;
    }

    public static void markBiteStarted(Nulljaw dragon, LivingEntity target) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID leaderUuid = dragon.getCombatFormationLeaderUuid();
        if (leaderUuid == null) {
            return;
        }
        Map<GroupKey, GroupState> levelGroups = GROUPS.get(serverLevel);
        if (levelGroups == null) {
            return;
        }
        GroupState group = levelGroups.get(new GroupKey(leaderUuid, target.getUUID()));
        if (group != null) {
            group.commitBite(dragon.getUUID(), serverLevel.getGameTime());
        }
    }

    public static void leave(Nulljaw dragon, @Nullable LivingEntity target) {
        dragon.clearCombatFormationDebug();
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Map<GroupKey, GroupState> levelGroups = GROUPS.get(serverLevel);
        if (levelGroups == null) {
            return;
        }

        UUID memberUuid = dragon.getUUID();
        UUID leaderUuid = dragon.getCombatFormationLeaderUuid();
        if (target != null && leaderUuid != null) {
            GroupKey key = new GroupKey(leaderUuid, target.getUUID());
            GroupState group = levelGroups.get(key);
            if (group != null && group.removeMember(memberUuid, serverLevel.getGameTime())) {
                levelGroups.remove(key);
            }
            return;
        }

        levelGroups.entrySet().removeIf(entry -> entry.getValue().removeMember(memberUuid, serverLevel.getGameTime()));
    }

    private static Directive soloDirective(Nulljaw dragon, LivingEntity target) {
        Vec3 targetPoint = predictedTargetCenter(target);
        return new Directive(
                Phase.DIVE,
                targetPoint,
                true,
                true,
                0,
                1,
                dragon.getUUID()
        );
    }

    private static void pruneExpiredGroups(Map<GroupKey, GroupState> groups, long gameTime) {
        if (gameTime % 100L != 0L) {
            return;
        }
        groups.entrySet().removeIf(entry -> gameTime - entry.getValue().lastTouchedAt > GROUP_EXPIRY_TICKS);
    }

    private static Vec3 predictedTargetCenter(LivingEntity target) {
        return target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(TARGET_PREDICTION_TICKS));
    }

    private static Vec3 horizontalDirection(double angle) {
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private static int compareUuid(UUID first, UUID second) {
        int most = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return most != 0
                ? most
                : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    private record GroupKey(UUID leaderUuid, UUID targetUuid) {
    }

    private static final class GroupState {
        private final List<UUID> members = new ArrayList<>();
        private final Map<UUID, AttackRun> attackRuns = new HashMap<>();

        private long lastTouchedAt;
        private long nextMembershipRefreshAt;
        private long nextAttackAt;
        private int nextAttackerIndex;
        private int waveIndex;

        private GroupState(long gameTime) {
            this.lastTouchedAt = gameTime;
            this.nextAttackAt = gameTime + FORMATION_WARMUP_TICKS;
        }

        private void refreshMembersIfNeeded(ServerLevel level,
                                            Nulljaw requestingDragon,
                                            LivingEntity target,
                                            UUID leaderUuid,
                                            long gameTime) {
            if (gameTime < this.nextMembershipRefreshAt && this.members.contains(requestingDragon.getUUID())) {
                return;
            }
            this.nextMembershipRefreshAt = gameTime + MEMBERSHIP_REFRESH_TICKS;

            double searchRadius = Math.max(
                    requestingDragon.getPackSearchRadius(),
                    requestingDragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
            );
            AABB searchBounds = target.getBoundingBox().inflate(searchRadius);
            List<Nulljaw> candidates = level.getEntitiesOfClass(
                    Nulljaw.class,
                    searchBounds,
                    member -> member.isAlive()
                            && !member.isBaby()
                            && !member.isVehicle()
                            && member.getTarget() == target
                            && leaderUuid.equals(member.getCombatFormationLeaderUuid())
            );
            if (!candidates.contains(requestingDragon)) {
                candidates.add(requestingDragon);
            }
            candidates.sort((first, second) -> compareUuid(first.getUUID(), second.getUUID()));

            int maxMembers = Math.max(1, requestingDragon.getMaxPackSize());
            boolean wasEmpty = this.members.isEmpty();
            this.members.clear();
            for (int index = 0; index < Math.min(maxMembers, candidates.size()); index++) {
                this.members.add(candidates.get(index).getUUID());
            }

            this.attackRuns.keySet().removeIf(memberUuid -> !this.members.contains(memberUuid));
            if (this.members.size() == 1 && wasEmpty) {
                this.nextAttackAt = gameTime;
            }
        }

        private void scheduleWaveIfReady(LivingEntity target, long gameTime) {
            if (this.members.isEmpty() || !this.attackRuns.isEmpty() || gameTime < this.nextAttackAt) {
                return;
            }

            int memberCount = this.members.size();
            int primarySlot = Math.floorMod(this.nextAttackerIndex++, memberCount);
            UUID primary = this.members.get(primarySlot);
            double baseOrbitAngle = orbitAngle(gameTime, target.getUUID());
            this.attackRuns.put(primary, new AttackRun(
                    gameTime,
                    horizontalDirection(slotAngle(baseOrbitAngle, primarySlot, memberCount)),
                    gameTime
            ));

            boolean usePincer = memberCount >= 3 && this.waveIndex % 2 == 0;
            if (usePincer) {
                int oppositeSlot = Math.floorMod(primarySlot + Math.max(1, memberCount / 2), memberCount);
                UUID opposite = this.members.get(oppositeSlot);
                if (!opposite.equals(primary)) {
                    this.attackRuns.put(opposite, new AttackRun(
                            gameTime + PINCER_STAGGER_TICKS,
                            horizontalDirection(slotAngle(baseOrbitAngle, oppositeSlot, memberCount)),
                            gameTime + PINCER_STAGGER_TICKS
                    ));
                }
            }
            this.waveIndex++;
        }

        private Directive directiveFor(Nulljaw dragon,
                                       LivingEntity target,
                                       UUID leaderUuid,
                                       long gameTime) {
            int formationSize = Math.max(1, this.members.size());
            int slot = this.members.indexOf(dragon.getUUID());
            if (slot < 0) {
                slot = 0;
                formationSize = 1;
            }

            AttackRun run = this.attackRuns.get(dragon.getUUID());
            if (run == null || gameTime < run.launchAt) {
                return orbitDirective(target, leaderUuid, slot, formationSize, run != null, gameTime);
            }

            if (run.phase == Phase.STAGE) {
                Vec3 stage = stagingPoint(target, run.approachDirection, slot);
                if (dragon.position().distanceToSqr(stage) <= STAGE_ARRIVAL_DISTANCE_SQR
                        || gameTime - run.phaseStartedAt >= STAGE_TIMEOUT_TICKS) {
                    run.phase = Phase.DIVE;
                    run.phaseStartedAt = gameTime;
                } else {
                    return new Directive(Phase.STAGE, stage, false, true, slot, formationSize, leaderUuid);
                }
            }

            if (run.phase == Phase.DIVE) {
                if (run.biteStartedAt >= 0L
                        && gameTime - run.biteStartedAt >= BITE_COMMIT_TICKS) {
                    beginEgress(dragon.getUUID(), target, gameTime);
                } else if (run.biteStartedAt < 0L
                        && gameTime - run.phaseStartedAt >= DIVE_TIMEOUT_TICKS) {
                    beginEgress(dragon.getUUID(), target, gameTime);
                } else {
                    return new Directive(
                            Phase.DIVE,
                            predictedTargetCenter(target),
                            run.biteStartedAt < 0L,
                            true,
                            slot,
                            formationSize,
                            leaderUuid
                    );
                }
            }

            run = this.attackRuns.get(dragon.getUUID());
            if (run != null && run.phase == Phase.EGRESS) {
                Vec3 exit = run.exitWaypoint != null
                        ? run.exitWaypoint
                        : egressPoint(target, run.approachDirection);
                if (dragon.position().distanceToSqr(exit) <= EGRESS_ARRIVAL_DISTANCE_SQR
                        || gameTime - run.phaseStartedAt >= EGRESS_TIMEOUT_TICKS) {
                    finishRun(dragon.getUUID(), gameTime);
                    return orbitDirective(target, leaderUuid, slot, formationSize, false, gameTime);
                }
                return new Directive(Phase.EGRESS, exit, false, true, slot, formationSize, leaderUuid);
            }

            return orbitDirective(target, leaderUuid, slot, formationSize, false, gameTime);
        }

        private Directive orbitDirective(LivingEntity target,
                                         UUID leaderUuid,
                                         int slot,
                                         int formationSize,
                                         boolean reserved,
                                         long gameTime) {
            double angle = slotAngle(orbitAngle(gameTime, target.getUUID()), slot, formationSize);
            Vec3 radial = horizontalDirection(angle);
            double radius = ORBIT_RADIUS + (slot % 2) * 1.25D;
            double height = switch (slot % 4) {
                case 0 -> 2.5D;
                case 1 -> 5.0D;
                case 2 -> 3.75D;
                default -> 6.0D;
            };
            double bob = Math.sin(gameTime * 0.08D + slot * 1.7D) * 0.45D;
            Vec3 waypoint = predictedTargetCenter(target)
                    .add(radial.scale(radius))
                    .add(0.0D, height + bob, 0.0D);
            return new Directive(Phase.ORBIT, waypoint, false, reserved, slot, formationSize, leaderUuid);
        }

        private void beginEgress(UUID memberUuid, LivingEntity target, long gameTime) {
            AttackRun run = this.attackRuns.get(memberUuid);
            if (run == null || run.phase == Phase.EGRESS) {
                return;
            }
            run.phase = Phase.EGRESS;
            run.phaseStartedAt = gameTime;
            run.exitWaypoint = egressPoint(target, run.approachDirection);
        }

        private void commitBite(UUID memberUuid, long gameTime) {
            AttackRun run = this.attackRuns.get(memberUuid);
            if (run != null && run.phase == Phase.DIVE && run.biteStartedAt < 0L) {
                run.biteStartedAt = gameTime;
            }
        }

        private void finishRun(UUID memberUuid, long gameTime) {
            this.attackRuns.remove(memberUuid);
            if (this.attackRuns.isEmpty()) {
                this.nextAttackAt = gameTime + waveCooldownTicks(this.members.size());
            }
        }

        private boolean removeMember(UUID memberUuid, long gameTime) {
            this.members.remove(memberUuid);
            if (this.attackRuns.containsKey(memberUuid)) {
                finishRun(memberUuid, gameTime);
            }
            return this.members.isEmpty();
        }

        private static Vec3 stagingPoint(LivingEntity target, Vec3 approachDirection, int slot) {
            double height = 3.5D + (slot % 2) * 2.0D;
            return predictedTargetCenter(target)
                    .add(approachDirection.scale(STAGE_RADIUS))
                    .add(0.0D, height, 0.0D);
        }

        private static Vec3 egressPoint(LivingEntity target, Vec3 approachDirection) {
            return predictedTargetCenter(target)
                    .subtract(approachDirection.scale(EGRESS_DISTANCE))
                    .add(0.0D, 3.0D, 0.0D);
        }

        private static double orbitAngle(long gameTime, UUID targetUuid) {
            double direction = (targetUuid.getLeastSignificantBits() & 1L) == 0L ? 1.0D : -1.0D;
            return gameTime * ORBIT_ANGULAR_SPEED * direction;
        }

        private static double slotAngle(double baseAngle, int slot, int formationSize) {
            return baseAngle + slot * (Math.PI * 2.0D / Math.max(1, formationSize));
        }

        private static int waveCooldownTicks(int memberCount) {
            return 32 + Math.max(1, memberCount) * 5;
        }
    }

    private static final class AttackRun {
        private final long launchAt;
        private final Vec3 approachDirection;
        private Phase phase = Phase.STAGE;
        private long phaseStartedAt;
        private long biteStartedAt = -1L;
        private @Nullable Vec3 exitWaypoint;

        private AttackRun(long launchAt, Vec3 approachDirection, long phaseStartedAt) {
            this.launchAt = launchAt;
            this.approachDirection = approachDirection;
            this.phaseStartedAt = phaseStartedAt;
        }
    }
}
