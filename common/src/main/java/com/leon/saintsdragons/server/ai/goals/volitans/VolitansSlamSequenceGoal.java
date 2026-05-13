package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class VolitansSlamSequenceGoal extends Goal {
    private static final int SEQUENCE_COOLDOWN_TICKS = 320;
    private static final int TRACK_TICKS = 100; // 5 seconds
    private static final int TAKEOFF_MAX_TICKS = Volitans.TAKEOFF_ANIMATION_TICKS + 24;
    private static final int LAND_MAX_TICKS = 60;
    private static final double MIN_START_RANGE = 10.0D;
    private static final double MAX_START_RANGE = 34.0D;
    private static final double TRACK_ALTITUDE = 9.0D;
    private static final double TRACK_SPEED = 1.55D;
    private static final double TAKEOFF_SPEED = 1.15D;
    private static final double SLAM_TRIGGER_RANGE = 40.0D;
    private static final double ABORT_FOLLOW_SPEED = 0.9D;
    private static final double FLIGHT_ACCEL = 0.14D;
    private static final double FLIGHT_DRAG = 0.94D;
    private static final double TRACK_PREDICTION_TICKS = 6.0D;
    private static final int SLAM_FORCE_RETRY_INTERVAL_TICKS = 5;
    private static final double TRACK_VERTICAL_CATCHUP_LIMIT = 0.6D;

    private final Volitans dragon;
    private int sequenceCooldownTicks = 0;
    private Phase phase = Phase.IDLE;
    private int phaseTicks = 0;

    private enum Phase {
        IDLE,
        TRACK,
        SLAM,
        LAND
    }

    public VolitansSlamSequenceGoal(Volitans dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (sequenceCooldownTicks > 0) {
            dragon.setAiSpecialCombatReserved(false);
            sequenceCooldownTicks--;
            return false;
        }
        if (dragon.isBaby() || dragon.isVehicle() || dragon.getControllingPassenger() != null || dragon.isOrderedToSit()) {
            dragon.setAiSpecialCombatReserved(false);
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.areRiderControlsLocked() || dragon.isInWaterOrBubble()) {
            dragon.setAiSpecialCombatReserved(false);
            return false;
        }
        if (dragon.getActiveAbility() != null || dragon.isBurrowing()) {
            dragon.setAiSpecialCombatReserved(false);
            return false;
        }
        if ((!dragon.isFlying() && !dragon.isHovering()) || dragon.isTakeoff() || dragon.isLanding() || dragon.onGround()) {
            dragon.setAiSpecialCombatReserved(false);
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (!isValidStartTarget(target)) {
            dragon.setAiSpecialCombatReserved(false);
            return false;
        }

        double gap = getGapToTarget(target);
        boolean canStart = gap >= MIN_START_RANGE && gap <= MAX_START_RANGE;
        dragon.setAiSpecialCombatReserved(canStart);
        return canStart;
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == Phase.IDLE) {
            return false;
        }
        if (dragon.isDeadOrDying() || dragon.isRemoved() || dragon.isInWaterOrBubble()) {
            return false;
        }
        if (phase == Phase.SLAM) {
            return dragon.isAbilityActive(ModAbilities.VOLITANS_ULTIMATE) || dragon.isUltimateSlamActive();
        }
        if (phase == Phase.LAND) {
            return !dragon.onGround() && phaseTicks <= LAND_MAX_TICKS;
        }

        LivingEntity target = dragon.getTarget();
        return isValidSequenceTarget(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        dragon.setAiSpecialCombatReserved(false);
        cancelPreSequenceAbilities();
        dragon.setAiSpecialCombatActive(true);
        dragon.setAggressive(true);
        dragon.getNavigation().stop();
        dragon.beginAiFlight();
        transitionTo(Phase.TRACK);
    }

    @Override
    public void stop() {
        dragon.setAiSpecialCombatReserved(false);
        if (phase != Phase.SLAM) {
            if (!dragon.onGround()) {
                dragon.beginAiLanding();
            } else {
                dragon.markLandedNow();
            }
        }
        dragon.setAiSpecialCombatActive(false);
        dragon.setAggressive(false);
        phase = Phase.IDLE;
        phaseTicks = 0;
        sequenceCooldownTicks = SEQUENCE_COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        phaseTicks++;

        if (target != null) {
            dragon.getLookControl().setLookAt(target, 35.0F, 35.0F);
        }

        switch (phase) {
            case TRACK -> tickTrack(target);
            case SLAM -> tickSlam();
            case LAND -> tickLand(target);
            default -> {
            }
        }
    }

    private void tickTrack(LivingEntity target) {
        if (!isValidSequenceTarget(target)) {
            transitionTo(Phase.LAND);
            return;
        }

        dragon.beginAiFlight();

        double targetY = target.getY() + TRACK_ALTITUDE;
        double desiredY = dragon.getY() + Mth.clamp(targetY - dragon.getY(),
                -TRACK_VERTICAL_CATCHUP_LIMIT,
                TRACK_VERTICAL_CATCHUP_LIMIT);
        double targetX = target.getX() + target.getDeltaMovement().x * TRACK_PREDICTION_TICKS;
        double targetZ = target.getZ() + target.getDeltaMovement().z * TRACK_PREDICTION_TICKS;
        flyToward(new Vec3(targetX, desiredY, targetZ), TRACK_SPEED, false);

        if (dragon.distanceToSqr(target) > SLAM_TRIGGER_RANGE * SLAM_TRIGGER_RANGE) {
            transitionTo(Phase.LAND);
            return;
        }

        if (phaseTicks >= TRACK_TICKS && ((phaseTicks - TRACK_TICKS) % SLAM_FORCE_RETRY_INTERVAL_TICKS == 0)) {
            dragon.combatManager.forceUseAbility(ModAbilities.VOLITANS_ULTIMATE);
            if (dragon.isAbilityActive(ModAbilities.VOLITANS_ULTIMATE) || dragon.isUltimateSlamActive()) {
                transitionTo(Phase.SLAM);
            } else {
                if (dragon.onGround()) {
                    transitionTo(Phase.LAND);
                } else {
                    flyToward(new Vec3(targetX, desiredY, targetZ), TRACK_SPEED, false);
                }
            }
        }
    }

    private void tickSlam() {
        if (!dragon.isAbilityActive(ModAbilities.VOLITANS_ULTIMATE) && !dragon.isUltimateSlamActive()) {
            transitionTo(Phase.LAND);
        }
    }

    private void tickLand(LivingEntity target) {
        dragon.beginAiLanding();
        if (target != null && dragon.distanceToSqr(target) < SLAM_TRIGGER_RANGE * SLAM_TRIGGER_RANGE) {
            dragon.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0D, target.getZ(), ABORT_FOLLOW_SPEED);
        }
        if (dragon.onGround() || phaseTicks >= LAND_MAX_TICKS) {
            dragon.markLandedNow();
            phase = Phase.IDLE;
        }
    }

    private boolean isValidStartTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target) || target == null) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }
        return target.onGround() || Math.abs(target.getDeltaMovement().y) < 0.08D;
    }

    private boolean isValidSequenceTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target) || target == null) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 48.0D;
        }
        return followRange * followRange;
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = dragon.distanceTo(target);
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, centerDistance - combinedRadii);
    }

    private void transitionTo(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void cancelPreSequenceAbilities() {
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_ULTIMATE) || dragon.isUltimateSlamActive()) {
            return;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BURROW)) {
            dragon.requestBurrowExit(false);
            dragon.forceEndActiveAbility();
            return;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_ROAR)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_BITE)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_CLAW)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_HORN_GORE)) {
            dragon.forceEndActiveAbility();
        }
    }

    private void flyToward(Vec3 destination, double speedScale, boolean forceClimb) {
        Vec3 toDest = destination.subtract(dragon.position());
        if (toDest.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 targetDir = toDest.normalize();
        Vec3 current = dragon.getDeltaMovement();
        double flightSpeed = Math.max(0.20D, dragon.getFlightSpeed() * speedScale);
        Vec3 targetVel = targetDir.scale(flightSpeed);
        Vec3 blended = new Vec3(
                current.x + (targetVel.x - current.x) * FLIGHT_ACCEL,
                current.y + (targetVel.y - current.y) * FLIGHT_ACCEL,
                current.z + (targetVel.z - current.z) * FLIGHT_ACCEL
        ).scale(FLIGHT_DRAG);

        if (forceClimb) {
            blended = new Vec3(blended.x, Math.max(blended.y, 0.18D), blended.z);
        }

        dragon.setSpeed((float) flightSpeed);
        dragon.setDeltaMovement(blended);
        dragon.move(MoverType.SELF, blended);
        dragon.hasImpulse = true;

        double horizontal = Math.sqrt(blended.x * blended.x + blended.z * blended.z);
        if (horizontal > 1.0E-4D) {
            float targetYaw = (float) (Math.atan2(blended.z, blended.x) * (180.0D / Math.PI)) - 90.0F;
            dragon.setYRot(targetYaw);
            dragon.yBodyRot = targetYaw;
            dragon.yHeadRot = targetYaw;
        }
        if (blended.lengthSqr() > 1.0E-4D) {
            float targetPitch = (float) (-(Math.atan2(blended.y, horizontal) * (180.0D / Math.PI)));
            dragon.setXRot(targetPitch);
        }
    }
}
