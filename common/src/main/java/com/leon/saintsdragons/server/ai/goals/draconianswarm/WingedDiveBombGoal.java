package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.Winged;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class WingedDiveBombGoal extends Goal {
    private static final double MAX_DISTANCE_SQ = 256.0D;
    private static final int TRACK_TICKS = 8;
    private static final int DIVE_CURVE_TICKS = 21;

    private final Winged winged;
    private Vec3 horizontalDirection = Vec3.ZERO;
    private Vec3 attackDirection = Vec3.ZERO;
    private int sequenceTicks;
    private double horizontalCorrection;
    private double verticalCorrection;
    private boolean committed;
    private boolean hitTarget;

    public WingedDiveBombGoal(Winged winged) {
        this.winged = winged;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.winged.canStartCombatAttack()) {
            return false;
        }
        LivingEntity target = this.winged.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distanceSq = this.winged.distanceToSqr(target);
        return distanceSq <= MAX_DISTANCE_SQ
                && this.winged.isDiveBombReady()
                && this.winged.tryClaimCombatAttack();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.winged.getTarget();
        return this.winged.isSwooping()
                && target != null
                && target.isAlive()
                && !this.hitTarget
                && this.sequenceTicks < TRACK_TICKS + DIVE_CURVE_TICKS;
    }

    @Override
    public void start() {
        this.sequenceTicks = 0;
        this.horizontalDirection = Vec3.ZERO;
        this.attackDirection = Vec3.ZERO;
        this.horizontalCorrection = 1.0D;
        this.verticalCorrection = 1.0D;
        this.committed = false;
        this.hitTarget = false;
        this.winged.setSwooping(true);
        this.winged.consumeDiveBomb();
    }

    @Override
    public void tick() {
        LivingEntity target = this.winged.getTarget();
        if (target == null) {
            return;
        }
        this.sequenceTicks++;
        this.winged.getLookControl().setLookAt(target, 100.0F, 100.0F);

        if (this.sequenceTicks <= TRACK_TICKS) {
            tickTrackingSetup(target);
            return;
        }
        if (!this.committed) {
            commitDive(target);
        }

        tickDiveCurve(target);
        tryHitTarget(target);
    }

    private void tickTrackingSetup(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 away = this.winged.position().subtract(targetCenter).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) {
            away = this.winged.getLookAngle().scale(-1.0D).multiply(1.0D, 0.0D, 1.0D);
        }
        Vec3 setupPoint = targetCenter.add(away.normalize().scale(3.0D)).add(0.0D, 6.0D, 0.0D);
        this.winged.getSwarmFlightController().setDirectWaypoint(setupPoint, 0.85D);
    }

    private void commitDive(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 difference = targetCenter.subtract(this.winged.getBoundingBox().getCenter());
        Vec3 horizontal = new Vec3(difference.x, 0.0D, difference.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = this.winged.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        this.horizontalDirection = horizontal.normalize();
        this.horizontalCorrection = Math.min(1.8D, 0.1D * horizontal.length());
        this.verticalCorrection = Math.min(2.0D, 0.09D * Math.abs(difference.y));
        this.committed = true;
        this.winged.getSwarmFlightController().clearWaypoint();
        this.winged.performSwoopAnimation();
    }

    private void tickDiveCurve(LivingEntity target) {
        int curveTick = this.sequenceTicks - TRACK_TICKS - 1;
        double frame = Mth.clamp(curveTick / (double) DIVE_CURVE_TICKS, 0.0D, 1.0D);

        Vec3 toTargetHorizontal = target.getBoundingBox().getCenter()
                .subtract(this.winged.getBoundingBox().getCenter())
                .multiply(1.0D, 0.0D, 1.0D);
        if (frame < 0.55D && toTargetHorizontal.lengthSqr() > 1.0E-4D) {
            Vec3 correctedDirection = toTargetHorizontal.normalize();
            this.horizontalDirection = this.horizontalDirection.scale(0.82D)
                    .add(correctedDirection.scale(0.18D)).normalize();
            double liveCorrection = Math.min(1.8D, 0.1D * toTargetHorizontal.length());
            this.horizontalCorrection = Mth.lerp(0.18D, this.horizontalCorrection, liveCorrection);
        }

        double forward = this.horizontalCorrection * 1.4D
                * (1.0D - Math.exp(2.0D * (frame - 1.0D)));
        forward = Mth.clamp(forward, 0.08D, 1.85D);
        double arcFrame = Math.min(1.0D, frame / 0.8D);
        double vertical = this.verticalCorrection * 1.35D * -Math.cos(arcFrame * Math.PI);
        vertical = Mth.clamp(vertical, -1.45D, 0.95D);

        double targetFloor = target.getY() + 0.45D;
        if (this.winged.getY() <= targetFloor) {
            vertical = Math.max(vertical, 0.35D);
        } else if (vertical < 0.0D) {
            Vec3 downwardProbe = new Vec3(0.0D, vertical * 1.5D, 0.0D);
            if (!this.winged.level().noCollision(
                    this.winged, this.winged.getBoundingBox().move(downwardProbe))) {
                vertical = 0.45D;
            }
        }

        Vec3 desiredVelocity = this.horizontalDirection.scale(forward).add(0.0D, vertical, 0.0D);
        if (desiredVelocity.lengthSqr() < 1.0E-4D) {
            return;
        }
        this.attackDirection = desiredVelocity.normalize();
        Vec3 segmentTarget = this.winged.position().add(this.attackDirection.scale(8.0D));
        this.winged.getSwarmFlightController().setDirectWaypoint(
                segmentTarget, desiredVelocity.length());
    }

    private void tryHitTarget(LivingEntity target) {
        AABB hitbox = this.winged.getBoundingBox().inflate(0.85D);
        List<LivingEntity> hits = this.winged.level().getEntitiesOfClass(
                LivingEntity.class, hitbox,
                this.winged::canHitWithSwarmAttack);
        if (hits.isEmpty()) {
            return;
        }
        LivingEntity victim = hits.contains(target) ? target : hits.get(0);
        float damage = (float) this.winged.getDiveBombDamage();
        if (victim.hurt(this.winged.damageSources().mobAttack(this.winged), damage)) {
            victim.setDeltaMovement(victim.getDeltaMovement().scale(0.25D)
                    .add(this.attackDirection.scale(0.9D)).add(0.0D, 0.12D, 0.0D));
            victim.hurtMarked = true;
            this.hitTarget = true;
        }
    }

    @Override
    public void stop() {
        this.winged.setSwooping(false);
        if (this.hitTarget) {
            this.winged.requestCombatRetreat();
        }
        this.winged.releaseCombatAttack();
        this.horizontalDirection = Vec3.ZERO;
        this.attackDirection = Vec3.ZERO;
    }
}
