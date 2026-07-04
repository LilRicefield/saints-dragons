package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import com.leon.saintsdragons.server.entity.draconianswarm.SwoopingSwarmEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DraconianSwarmSwoopAttackGoal<T extends AbstractDraconianSwarmEntity & SwoopingSwarmEntity>
        extends Goal {
    private static final double MIN_START_DISTANCE_SQ = 16.0D;
    private static final double MAX_START_DISTANCE_SQ = 225.0D;
    private static final double PASS_THROUGH_DISTANCE = 5.0D;
    private static final double ANIMATION_START_FORWARD_SPEED = 0.22D;
    private static final int MAX_SWOOP_TICKS = 24;
    private static final int COOLDOWN_TICKS = 70;

    private final T swarm;
    private final double swoopSpeed;
    private Vec3 destination;
    private Vec3 attackDirection = Vec3.ZERO;
    private int swoopTicks;
    private int cooldown = 30;
    private boolean animationTriggered;
    private boolean hitTarget;

    public DraconianSwarmSwoopAttackGoal(T swarm, double swoopSpeed) {
        this.swarm = swarm;
        this.swoopSpeed = swoopSpeed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.swarm.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distanceSq = this.swarm.distanceToSqr(target);
        return distanceSq >= MIN_START_DISTANCE_SQ
                && distanceSq <= MAX_START_DISTANCE_SQ
                && this.swarm.getRandom().nextInt(12) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.swarm.getTarget();
        return this.swarm.isSwooping()
                && target != null
                && target.isAlive()
                && !this.hitTarget
                && !hasPassedDestination()
                && this.swoopTicks < MAX_SWOOP_TICKS
                && this.destination != null;
    }

    @Override
    public void start() {
        LivingEntity target = this.swarm.getTarget();
        if (target == null) {
            return;
        }
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 approach = targetCenter.subtract(this.swarm.getBoundingBox().getCenter());
        if (approach.lengthSqr() < 1.0E-4D) {
            approach = this.swarm.getLookAngle();
        }
        this.attackDirection = approach.normalize();
        this.destination = targetCenter.add(this.attackDirection.scale(PASS_THROUGH_DISTANCE));
        this.swoopTicks = 0;
        this.animationTriggered = false;
        this.hitTarget = false;
        this.swarm.setSwooping(true);
        this.swarm.getSwarmFlightController().setWaypoint(this.destination, this.swoopSpeed);
    }

    @Override
    public void tick() {
        this.swoopTicks++;
        LivingEntity target = this.swarm.getTarget();
        if (target == null) {
            return;
        }
        this.swarm.getLookControl().setLookAt(target, 100.0F, 100.0F);
        this.swarm.getSwarmFlightController().setWaypoint(this.destination, this.swoopSpeed);

        double forwardSpeed = this.swarm.getDeltaMovement().dot(this.attackDirection);
        if (!this.animationTriggered && this.swoopTicks >= 2
                && forwardSpeed >= ANIMATION_START_FORWARD_SPEED) {
            this.animationTriggered = true;
            this.swarm.performSwoopAnimation();
        }

        if (!this.hitTarget) {
            AABB hitbox = this.swarm.getBoundingBox().inflate(0.85D);
            List<LivingEntity> hits = this.swarm.level().getEntitiesOfClass(
                    LivingEntity.class,
                    hitbox,
                    entity -> entity.isAlive()
                            && entity != this.swarm
                            && !(entity instanceof AbstractDraconianSwarmEntity));
            if (!hits.isEmpty()) {
                LivingEntity victim = hits.contains(target) ? target : hits.get(0);
                this.swarm.doHurtTarget(victim);
                victim.setDeltaMovement(victim.getDeltaMovement().scale(0.25D)
                        .add(this.attackDirection.scale(0.9D))
                        .add(0.0D, 0.12D, 0.0D));
                victim.hurtMarked = true;
                this.hitTarget = true;
                if (!this.animationTriggered) {
                    this.animationTriggered = true;
                    this.swarm.performSwoopAnimation();
                }
            }
        }
    }

    @Override
    public void stop() {
        this.swarm.setSwooping(false);
        LivingEntity target = this.swarm.getTarget();
        if (target != null && target.isAlive()) {
            Vec3 velocity = target.getDeltaMovement();
            Vec3 chaseTarget = target.position().add(
                    velocity.x * 3.0D,
                    target.getBbHeight() * 0.55D + velocity.y,
                    velocity.z * 3.0D);
            this.swarm.getSwarmFlightController().setDirectWaypoint(chaseTarget, this.swarm.getChaseSpeed());
        } else {
            this.swarm.getSwarmFlightController().clearWaypoint();
        }
        this.destination = null;
        this.attackDirection = Vec3.ZERO;
        this.cooldown = COOLDOWN_TICKS;
    }

    private boolean hasPassedDestination() {
        if (this.destination == null || this.swoopTicks < 3) {
            return false;
        }
        return this.destination.subtract(this.swarm.getBoundingBox().getCenter())
                .dot(this.attackDirection) <= 0.0D;
    }
}
