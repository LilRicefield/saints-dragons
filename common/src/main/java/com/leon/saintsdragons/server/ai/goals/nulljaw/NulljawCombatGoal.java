package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class NulljawCombatGoal extends Goal {
    private static final double EXTRA_BITE_REACH = 1.5D;
    private static final double WAYPOINT_CHANGE_DISTANCE_SQR = 16.0D;

    private final Nulljaw dragon;
    private int movementRefreshCooldown;
    private Vec3 lastWaypoint;
    private NulljawPackCombatCoordinator.Phase lastPhase;
    private LivingEntity coordinatedTarget;

    public NulljawCombatGoal(Nulljaw dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return canChase(this.dragon.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.movementRefreshCooldown = 0;
        this.lastWaypoint = null;
        this.lastPhase = null;
        this.coordinatedTarget = this.dragon.getTarget();
        this.dragon.beginAiFlight();
    }

    @Override
    public void tick() {
        LivingEntity target = this.dragon.getTarget();
        if (target == null) {
            return;
        }
        if (target != this.coordinatedTarget) {
            NulljawPackCombatCoordinator.leave(this.dragon, this.coordinatedTarget);
            this.coordinatedTarget = target;
            this.movementRefreshCooldown = 0;
            this.lastWaypoint = null;
            this.lastPhase = null;
        }

        this.dragon.getLookControl().setLookAt(target, 100.0F, 100.0F);
        NulljawPackCombatCoordinator.Directive directive =
                NulljawPackCombatCoordinator.getDirective(this.dragon, target);
        if (directive.mayBite()
                && isInBiteRange(target)
                && this.dragon.getSensing().hasLineOfSight(target)
                && this.dragon.combatManager.tryUseAbility(ModAbilities.NULLJAW_BITE)) {
            NulljawPackCombatCoordinator.markBiteStarted(this.dragon, target);
        }

        if (this.movementRefreshCooldown > 0) {
            this.movementRefreshCooldown--;
        }

        Vec3 waypoint = directive.waypoint();
        boolean phaseChanged = directive.phase() != this.lastPhase;
        boolean waypointChanged = this.lastWaypoint == null
                || this.lastWaypoint.distanceToSqr(waypoint) >= WAYPOINT_CHANGE_DISTANCE_SQR;
        if (phaseChanged
                || this.movementRefreshCooldown == 0
                || !this.dragon.getAIMovement().isPathing()
                || waypointChanged) {
            this.dragon.beginAiFlight();
            this.dragon.getAIMovement().setWaypoint(waypoint, speedFor(directive.phase()));
            this.lastWaypoint = waypoint;
            this.lastPhase = directive.phase();
            this.movementRefreshCooldown = refreshTicksFor(directive.phase());
        }
    }

    @Override
    public void stop() {
        LivingEntity target = this.coordinatedTarget;
        NulljawPackCombatCoordinator.leave(this.dragon, target);
        this.dragon.getAIMovement().stop();
        this.movementRefreshCooldown = 0;
        this.lastWaypoint = null;
        this.lastPhase = null;
        this.coordinatedTarget = null;
        if (!canChase(this.dragon.getTarget())) {
            this.dragon.setTarget(null);
            this.dragon.setCombatFormationLeaderUuid(null);
        }
    }

    private static double speedFor(NulljawPackCombatCoordinator.Phase phase) {
        return switch (phase) {
            case ORBIT -> 0.9D;
            case STAGE -> 1.1D;
            case DIVE -> 1.4D;
            case EGRESS -> 1.3D;
        };
    }

    private static int refreshTicksFor(NulljawPackCombatCoordinator.Phase phase) {
        return switch (phase) {
            case ORBIT -> 8;
            case STAGE, EGRESS -> 5;
            case DIVE -> 2;
        };
    }

    private boolean canChase(LivingEntity target) {
        return !this.dragon.isBaby()
                && !this.dragon.isVehicle()
                && !this.dragon.isPassenger()
                && !this.dragon.isOrderedToSit()
                && isValidTarget(target)
                && this.dragon.distanceToSqr(target) <= getFollowRangeSqr();
    }

    private boolean isValidTarget(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == this.dragon.level()
                && target.attackable()
                && !this.dragon.isAlly(target)
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private boolean isInBiteRange(LivingEntity target) {
        double reach = this.dragon.getBbWidth() * 0.5D
                + target.getBbWidth() * 0.5D
                + EXTRA_BITE_REACH;
        return this.dragon.distanceToSqr(target) <= reach * reach;
    }

    private double getFollowRangeSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        return followRange * followRange;
    }
}
