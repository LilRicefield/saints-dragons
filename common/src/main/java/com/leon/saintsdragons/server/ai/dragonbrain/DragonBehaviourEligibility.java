package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class DragonBehaviourEligibility {
    private static final Blocker[] CHECK_ORDER = Blocker.values();
    public static final Policy AMBIENT = Policy.of(Blocker.RIDDEN, Blocker.SITTING, Blocker.SLEEPING,
            Blocker.BREEDING, Blocker.COMBAT, Blocker.ABILITY);
    public static final Policy INVESTIGATION = Policy.of(Blocker.RIDDEN, Blocker.SITTING, Blocker.SLEEPING);
    public static final Policy EMERGENCY = Policy.of(Blocker.RIDDEN);

    private DragonBehaviourEligibility() {
    }

    @Nullable
    public static String rejection(DragonEntity dragon, Policy policy) {
        if (!dragon.isAlive() || dragon.isDying() || dragon.isRemoved()) return "dying";
        for (Blocker blocker : CHECK_ORDER) {
            if (!policy.blockers.contains(blocker)) continue;
            boolean blocked = switch (blocker) {
                case RIDDEN -> dragon.isVehicle() || dragon.isPassenger();
                case SITTING -> dragon.isOrderedToSit() || dragon.isInSittingPose() || dragon.isInSitTransition();
                case SLEEPING -> dragon.isSleepLocked();
                case BREEDING -> dragon.isInLove();
                case COMBAT -> dragon.isAggressive() || dragon.getTarget() != null && dragon.getTarget().isAlive();
                case ABILITY -> hasCommittedAction(dragon);
                case AERIAL -> dragon.isAerial();
                case IN_WATER -> dragon.isInWaterOrBubble();
            };
            if (blocked) return blocker.reason;
        }
        return null;
    }

    public static boolean hasCommittedAction(DragonEntity dragon) {
        return dragon.getActiveAbility() != null || dragon.combatManager.hasActiveOverlay()
                || dragon.areRiderControlsLocked();
    }

    public record Policy(Set<Blocker> blockers) {
        public Policy {
            blockers = Set.copyOf(blockers);
        }

        public static Policy of(Blocker... blockers) {
            EnumSet<Blocker> selected = EnumSet.noneOf(Blocker.class);
            for (Blocker blocker : blockers) selected.add(blocker);
            return new Policy(selected);
        }

        public Policy also(Blocker... blockers) {
            EnumSet<Blocker> selected = EnumSet.noneOf(Blocker.class);
            selected.addAll(this.blockers);
            for (Blocker blocker : blockers) selected.add(blocker);
            return new Policy(selected);
        }
    }

    public enum Blocker {
        RIDDEN("ridden"), SITTING("sitting"), SLEEPING("sleep"), BREEDING("breeding"),
        COMBAT("combat"), ABILITY("ability"), AERIAL("aerial"), IN_WATER("in-water");

        private final String reason;

        Blocker(String reason) {
            this.reason = reason;
        }
    }
}
