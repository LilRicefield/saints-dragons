package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class DraconianSwarmCombatMovementGoal extends Goal {
    private final AbstractDraconianSwarmEntity swarm;
    private Phase phase;
    private Vec3 retreatPoint;
    private int phaseTicks;
    private int waypointTicks;
    private int focusedTicks;

    public DraconianSwarmCombatMovementGoal(AbstractDraconianSwarmEntity swarm) {
        this.swarm = swarm;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.swarm.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (this.swarm.hasCombatRetreatRequest()) {
            beginRetreat(this.swarm.consumeCombatRetreatDistance());
        } else if (this.swarm.getCombatStyle() == AbstractDraconianSwarmEntity.CombatStyle.PRECISE) {
            beginApproach();
        } else {
            beginOrbit();
        }
    }

    @Override
    public void stop() {
        this.swarm.setCombatAttackWindow(false);
        this.swarm.releaseCombatAttack();
        this.swarm.getSwarmFlightController().clearWaypoint();
        this.retreatPoint = null;
    }

    @Override
    public void tick() {
        LivingEntity target = this.swarm.getTarget();
        if (target == null) {
            return;
        }

        this.swarm.getLookControl().setLookAt(target, 100.0F, 100.0F);
        if (this.phase != Phase.RETREAT && DraconianSwarmCoordinator.isPlayerFocusing(this.swarm, target)) {
            if (++this.focusedTicks >= 4) {
                this.focusedTicks = 0;
                beginRetreat(this.swarm.getCombatRetreatDistance() + 3.0D);
            }
        } else {
            this.focusedTicks = 0;
        }
        if (this.swarm.hasCombatRetreatRequest()) {
            beginRetreat(this.swarm.consumeCombatRetreatDistance());
        }

        switch (this.phase) {
            case ORBIT -> tickOrbit(target);
            case APPROACH -> tickApproach(target);
            case RETREAT -> tickRetreat(target);
        }
    }

    private void tickOrbit(LivingEntity target) {
        this.swarm.setCombatAttackWindow(false);
        if (--this.phaseTicks <= 0) {
            beginApproach();
            return;
        }

        if (--this.waypointTicks <= 0 || this.swarm.getSwarmFlightController().isIdle()) {
            this.waypointTicks = 10;
            DraconianSwarmCoordinator.OrbitSlot slot =
                    DraconianSwarmCoordinator.getOrbitSlot(this.swarm, target);
            double direction = (target.getId() & 1) == 0 ? 1.0D : -1.0D;
            double coordinatedAngle = target.tickCount * 0.07D * direction + slot.angleOffset();
            double radius = this.swarm.getCombatOrbitRadius();
            double height = this.swarm.getCombatOrbitHeight();
            Vec3 anchor = target.getBoundingBox().getCenter();
            Vec3 orbitPoint = anchor.add(
                    Math.cos(coordinatedAngle) * radius,
                    height,
                    Math.sin(coordinatedAngle) * radius);
            this.swarm.getSwarmFlightController().setWaypoint(orbitPoint, this.swarm.getOrbitSpeed());
        }
    }

    private void tickApproach(LivingEntity target) {
        if (DraconianSwarmCoordinator.isAttackReservedByOther(this.swarm, target)) {
            beginOrbit();
            return;
        }
        this.swarm.setCombatAttackWindow(true);
        Vec3 velocity = target.getDeltaMovement();
        double prediction = this.swarm.getCombatStyle() == AbstractDraconianSwarmEntity.CombatStyle.PRECISE
                ? 2.0D : 3.0D;
        Vec3 targetPoint = target.position().add(
                velocity.x * prediction,
                target.getBbHeight() * 0.55D + velocity.y,
                velocity.z * prediction);
        this.swarm.getSwarmFlightController().setDirectWaypoint(targetPoint, this.swarm.getChaseSpeed());
    }

    private void tickRetreat(LivingEntity target) {
        this.swarm.setCombatAttackWindow(false);
        if (this.retreatPoint == null || --this.phaseTicks <= 0
                || this.swarm.position().distanceToSqr(this.retreatPoint) < 2.25D) {
            if (this.swarm.getCombatStyle() == AbstractDraconianSwarmEntity.CombatStyle.PRECISE) {
                beginApproach();
            } else {
                beginOrbit();
            }
            return;
        }
        this.swarm.getSwarmFlightController().setDirectWaypoint(this.retreatPoint, this.swarm.getRetreatSpeed());
        this.swarm.getLookControl().setLookAt(target, 100.0F, 100.0F);
    }

    private void beginOrbit() {
        this.phase = Phase.ORBIT;
        this.phaseTicks = this.swarm.getOrbitDurationTicks();
        this.waypointTicks = 0;
        this.retreatPoint = null;
        this.swarm.setCombatAttackWindow(false);
    }

    private void beginApproach() {
        this.phase = Phase.APPROACH;
        this.retreatPoint = null;
        this.swarm.setCombatAttackWindow(true);
    }

    private void beginRetreat(double distance) {
        LivingEntity target = this.swarm.getTarget();
        if (target == null) {
            beginOrbit();
            return;
        }
        Vec3 away = this.swarm.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter());
        if (away.lengthSqr() < 1.0E-4D) {
            away = this.swarm.getLookAngle().scale(-1.0D);
        }
        away = away.normalize();
        this.retreatPoint = this.swarm.position().add(away.scale(distance)).add(0.0D, 1.5D, 0.0D);
        this.phase = Phase.RETREAT;
        this.phaseTicks = 30;
        this.swarm.setCombatAttackWindow(false);
        this.swarm.getSwarmFlightController().setDirectWaypoint(this.retreatPoint, this.swarm.getRetreatSpeed());
    }

    private enum Phase {
        ORBIT,
        APPROACH,
        RETREAT
    }
}
