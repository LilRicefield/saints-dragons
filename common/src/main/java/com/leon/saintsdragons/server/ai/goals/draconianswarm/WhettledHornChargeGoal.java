package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Whettled;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class WhettledHornChargeGoal extends Goal {
    private static final double MIN_DISTANCE_SQ = 12.25D;
    private static final double MAX_DISTANCE_SQ = 225.0D;
    private static final double CHARGE_SPEED = 1.50D;
    private static final double PASS_THROUGH_DISTANCE = 7.0D;
    private static final int WARNING_TICKS = 12;
    private static final int MAX_CHARGE_TICKS = 24;
    private static final int COOLDOWN_TICKS = 55;

    private final Whettled whettled;
    private Vec3 destination;
    private Vec3 attackDirection = Vec3.ZERO;
    private int warningTicks;
    private int chargeTicks;
    private int cooldown;
    private boolean animationTriggered;
    private boolean hitTarget;

    public WhettledHornChargeGoal(Whettled whettled) {
        this.whettled = whettled;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.whettled.canStartCombatAttack()) {
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.whettled.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distanceSq = this.whettled.distanceToSqr(target);
        return distanceSq >= MIN_DISTANCE_SQ
                && distanceSq <= MAX_DISTANCE_SQ
                && this.whettled.tryClaimCombatAttack();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.whettled.getTarget();
        return this.whettled.isSwooping()
                && target != null
                && target.isAlive()
                && !this.hitTarget
                && (this.warningTicks > 0 || (this.destination != null
                && this.chargeTicks < MAX_CHARGE_TICKS && !hasPassedDestination()));
    }

    @Override
    public void start() {
        this.warningTicks = WARNING_TICKS;
        this.chargeTicks = 0;
        this.animationTriggered = false;
        this.hitTarget = false;
        this.destination = null;
        this.attackDirection = Vec3.ZERO;
        this.whettled.setSwooping(true);
        this.whettled.setHornCharging(false);
        holdStill();
    }

    @Override
    public void tick() {
        LivingEntity target = this.whettled.getTarget();
        if (target == null) {
            return;
        }
        if (this.warningTicks > 0) {
            this.warningTicks--;
            this.whettled.getLookControl().setLookAt(target, 100.0F, 100.0F);
            holdStill();
            if (this.warningTicks == 0) {
                beginCommittedCharge(target);
            }
            return;
        }

        this.chargeTicks++;
        this.whettled.getSwarmFlightController().setDirectWaypoint(this.destination, CHARGE_SPEED);
        if (!this.animationTriggered
                && this.whettled.getDeltaMovement().dot(this.attackDirection) >= 0.22D) {
            this.animationTriggered = true;
            this.whettled.performSwoopAnimation();
        }
        tryHitTarget(target);
    }

    private void beginCommittedCharge(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 approach = targetCenter.subtract(this.whettled.getBoundingBox().getCenter());
        if (approach.lengthSqr() < 1.0E-4D) {
            approach = this.whettled.getLookAngle();
        }
        approach = approach.normalize();
        this.attackDirection = approach;
        this.destination = targetCenter.add(approach.scale(PASS_THROUGH_DISTANCE));
        this.whettled.lockHornChargeDirection(this.attackDirection);
        this.whettled.setHornCharging(true);
        this.whettled.getSwarmFlightController().setDirectWaypoint(this.destination, CHARGE_SPEED);
    }

    private void holdStill() {
        this.whettled.getSwarmFlightController().clearWaypoint();
        this.whettled.setDeltaMovement(Vec3.ZERO);
        this.whettled.hurtMarked = true;
    }

    private void tryHitTarget(LivingEntity target) {
        AABB hitbox = this.whettled.getBoundingBox().inflate(0.85D);
        List<LivingEntity> hits = this.whettled.level().getEntitiesOfClass(
                LivingEntity.class, hitbox,
                this.whettled::canHitWithSwarmAttack);
        if (hits.isEmpty()) {
            return;
        }
        LivingEntity victim = hits.contains(target) ? target : hits.get(0);
        float damage = (float) this.whettled.getLungeDamage();
        if (victim.hurt(this.whettled.damageSources().mobAttack(this.whettled), damage)) {
            if (!this.animationTriggered) {
                this.animationTriggered = true;
                this.whettled.performSwoopAnimation();
            }
            victim.setDeltaMovement(victim.getDeltaMovement().scale(0.2D)
                    .add(this.attackDirection.scale(1.25D)).add(0.0D, 0.16D, 0.0D));
            victim.hurtMarked = true;
            this.hitTarget = true;
        }
    }

    @Override
    public void stop() {
        this.whettled.setSwooping(false);
        this.whettled.setHornCharging(false);
        if (this.hitTarget) {
            this.whettled.requestCombatRetreat();
        }
        this.whettled.releaseCombatAttack();
        this.destination = null;
        this.attackDirection = Vec3.ZERO;
        this.cooldown = COOLDOWN_TICKS;
    }

    private boolean hasPassedDestination() {
        return this.destination.subtract(this.whettled.getBoundingBox().getCenter())
                .dot(this.attackDirection) <= 0.0D;
    }
}
