package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class IvyBoxingCombatController {
    private static final RawAnimation ORTHODOX_IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_idle");
    private static final RawAnimation ORTHODOX_WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_walk");
    private static final RawAnimation ORTHODOX_WALK_BACKWARDS = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_walk_backwards");
    private static final RawAnimation ORTHODOX_FAST_WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_fast_walk");
    private static final RawAnimation ORTHODOX_FAST_WALK_BACKWARDS = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_fast_walk_backwards");

    private static final int STANCE_TRANSITION_TICKS = 10;
    private static final int EXIT_STANCE_TICKS = 10;
    private static final int TAUNT_ACTION_TICKS = 35;
    private static final int JAB_ACTION_TICKS = 13;
    private static final int HOOK_ACTION_TICKS = 15;
    private static final int DODGE_ACTION_TICKS = 10;
    private static final int LIVER_COUNTER_ACTION_TICKS = 15;
    private static final int COMBO_ACTION_TICKS = 20;
    private static final int JAB_JAB_HOOK_ACTION_TICKS = 25;
    private static final int RIGHT_HOOK_UPPERCUT_ACTION_TICKS = 24;
    private static final int JAB_IMPACT_TICKS = 4;
    private static final int HOOK_IMPACT_TICKS = 5;
    private static final int LIVER_COUNTER_IMPACT_TICKS = 9;
    private static final int COMBO_FIRST_IMPACT_TICKS = 5;
    private static final int COMBO_SECOND_IMPACT_TICKS = 13;
    private static final int COMBO_RETREAT_TICKS = 15;
    private static final int JAB_JAB_HOOK_FIRST_IMPACT_TICKS = 5;
    private static final int JAB_JAB_HOOK_SECOND_IMPACT_TICKS = 11;
    private static final int JAB_JAB_HOOK_THIRD_IMPACT_TICKS = 19;
    private static final int RIGHT_HOOK_UPPERCUT_FIRST_IMPACT_TICKS = 8;
    private static final int RIGHT_HOOK_UPPERCUT_SECOND_IMPACT_TICKS = 16;
    private static final int JAB_COOLDOWN_TICKS = 4;
    private static final int HOOK_COOLDOWN_TICKS = 15;
    private static final int COMBO_COOLDOWN_TICKS = 20;
    private static final int DODGE_COOLDOWN_TICKS = 20;
    private static final float REACTIVE_DODGE_CHANCE = 0.65F;
    private static final float REACTIVE_CRIT_DODGE_CHANCE = 0.92F;
    private static final double ATTACK_RANGE = 2.45D;
    private static final double HOOK_RANGE = 2.15D;
    private static final double COMBO_MIN_RANGE = 1.65D;
    private static final int HOOK_UPPERCUT_CLOSE_CHANCE = 40;
    private static final double KEEP_DISTANCE = 2.15D;
    private static final double APPROACH_DISTANCE = 3.8D;
    private static final double TARGET_STILL_EPSILON_SQ = 0.0025D;
    private static final int TARGET_STILL_PRESSURE_TICKS = 14;
    private static final float LOCK_LOOK_YAW_SPEED = 45.0F;
    private static final float LOCK_LOOK_PITCH_SPEED = 35.0F;
    private static final float LOCK_BODY_YAW_SPEED = 0.45F;
    private static final float LOCK_BODY_YAW_MAX_DELTA = 28.0F;

    private final IvyTheDragonMerchant ivy;
    private int attackCooldown;
    private int dodgeCooldown;
    private int hookCooldown;
    private int comboCooldown;
    private int exitTicks;
    private boolean exitAfterTaunt;
    private int impactTicks;
    private int secondImpactTicks;
    private int thirdImpactTicks;
    private int comboRetreatTicks;
    private int impactTargetId = -1;
    private AttackType pendingAttack = AttackType.LEFT_JAB;
    private CounterType pendingCounter = null;
    @Nullable
    private LivingEntity lastCombatTarget;
    private CombatState state = CombatState.RECOVERING;
    private int stateTicks;
    private int lastTargetId = -1;
    private double lastTargetX;
    private double lastTargetZ;
    private int targetStillTicks;

    public IvyBoxingCombatController(IvyTheDragonMerchant ivy) {
        this.ivy = ivy;
    }

    public Goal createGoal() {
        return new BoxingGoal();
    }

    public void setupMovementController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("to_orthodox",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.to_orthodox"));
        controller.triggerableAnim("exit_orthodox",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.exit_orthodox"));
        controller.triggerableAnim("orthodox_taunt",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_taunt"));
        controller.triggerableAnim("orthodox_left_jab",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_left_jab"));
        controller.triggerableAnim("orthodox_right_hook",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_right_hook"));
        controller.triggerableAnim("orthodox_left_jab_right_cross",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_left_jab_right_cross"));
        controller.triggerableAnim("orthodox_jab_jab_hook",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_jab_jab_hook"));
        controller.triggerableAnim("orthodox_right_hook_uppercut",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_right_hook_uppercut"));
        controller.triggerableAnim("dodge_backwards",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_backwards"));
        controller.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_left"));
        controller.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_right"));
        controller.triggerableAnim("dodge_left_liver_shot",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_left_liver_shot"));
        controller.triggerableAnim("dodge_right_liver_shot",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_right_liver_shot"));
    }

    public <T extends GeoEntity> boolean applyMovementAnimation(AnimationState<T> state) {
        if (!isActive()) {
            return false;
        }
        if (ivy.isBoxingExiting() || ivy.isBoxingTaunting()) {
            return true;
        }
        if (ivy.getBoxingActionTicks() > 0) {
            return true;
        }
        if (ivy.isBoxingBackingUp()) {
            AnimationHelper.setAndContinue(state, ivy.isBoxingFast() ? ORTHODOX_FAST_WALK_BACKWARDS : ORTHODOX_WALK_BACKWARDS);
        } else if (state.isMoving()) {
            AnimationHelper.setAndContinue(state, ivy.isBoxingFast() ? ORTHODOX_FAST_WALK : ORTHODOX_WALK);
        } else {
            AnimationHelper.setAndContinue(state, ORTHODOX_IDLE);
        }
        return true;
    }

    public boolean isActive() {
        return ivy.isBoxingStance() || ivy.isBoxingExiting() || ivy.isBoxingTaunting();
    }

    public boolean shouldHoldGroundAgainstKnockback() {
        return isActive()
                && ivy.getBoxingActionTicks() > 0
                && (state == CombatState.ATTACKING || state == CombatState.DODGING);
    }

    public void onHurt(@NotNull DamageSource source, boolean wasHurt) {
        if (!wasHurt || ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy) {
            return;
        }
        ivy.setTarget(attacker);
        if (ivy.isBoxingTaunting()) {
            interruptTaunt(attacker);
            return;
        }
        beginStance();
    }

    public boolean tryDodgeOnHit(@NotNull DamageSource source, float amount) {
        if (amount <= 0.0F || ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy) {
            return false;
        }
        if (!attacker.isAlive()) {
            return false;
        }
        if (ivy.isBoxingTaunting()) {
            ivy.setTarget(attacker);
            interruptTaunt(attacker);
            return false;
        }

        if (isLikelyPlayerCritical(attacker)) {
            boolean dodged = ivy.getRandom().nextFloat() < REACTIVE_CRIT_DODGE_CHANCE;
            if (!dodged) {
                return false;
            }
            ivy.setTarget(attacker);
            beginStance();
            lockSight(attacker);
            if (state != CombatState.DODGING) {
                startCounterDodge(attacker);
            }
            return true;
        }

        if (dodgeCooldown > 0 || ivy.getBoxingActionTicks() > 0 || ivy.getRandom().nextFloat() >= REACTIVE_DODGE_CHANCE) {
            return false;
        }
        ivy.setTarget(attacker);
        beginStance();
        lockSight(attacker);
        if (ivy.getRandom().nextInt(100) < 45) {
            startCounterDodge(attacker);
        } else {
            startDodge(attacker);
        }
        return true;
    }

    private boolean isLikelyPlayerCritical(LivingEntity attacker) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }

    public void dodgeBlockedHit(@NotNull DamageSource source) {
        if (ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy || !attacker.isAlive()) {
            return;
        }
        ivy.setTarget(attacker);
        if (ivy.isBoxingTaunting()) {
            interruptTaunt(attacker);
            return;
        }
        beginStance();
        lockSight(attacker);
        if (ivy.getBoxingActionTicks() <= 0 || state != CombatState.DODGING) {
            if (ivy.getRandom().nextInt(100) < 55) {
                startCounterDodge(attacker);
            } else {
                startDodge(attacker);
            }
        }
    }

    public void tick() {
        if (!isActive()) {
            return;
        }
        if (exitTicks > 0) {
            exitTicks--;
            if (exitTicks <= 0) {
                clear();
            }
            return;
        }
        int actionTicks = ivy.getBoxingActionTicks();
        if (actionTicks > 0) {
            ivy.setBoxingActionTicks(actionTicks - 1);
            if (actionTicks - 1 <= 0 && ivy.isBoxingTaunting()) {
                ivy.setBoxingTaunting(false);
                if (exitAfterTaunt) {
                    exitAfterTaunt = false;
                    startExitStance();
                }
            }
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (hookCooldown > 0) {
            hookCooldown--;
        }
        if (comboCooldown > 0) {
            comboCooldown--;
        }
        if (dodgeCooldown > 0) {
            dodgeCooldown--;
        }
        if (impactTicks > 0 && --impactTicks <= 0) {
            applyImpact(pendingCounter != null ? pendingCounter.hit : pendingAttack.firstHit);
            pendingCounter = null;
        }
        if (secondImpactTicks > 0 && --secondImpactTicks <= 0) {
            applyImpact(pendingAttack.secondHit);
        }
        if (thirdImpactTicks > 0 && --thirdImpactTicks <= 0) {
            applyImpact(pendingAttack.thirdHit);
        }
        if (comboRetreatTicks > 0 && --comboRetreatTicks <= 0) {
            applyComboRetreat();
        }
        if (stateTicks > 0) {
            stateTicks--;
        }
    }

    public void tickRotationLock() {
        if (!isActive()) {
            return;
        }
        LivingEntity target = ivy.getTarget();
        if (target != null && target.isAlive()) {
            lockSight(target);
        }
    }

    private void beginStance() {
        if (exitTicks > 0) {
            exitTicks = 0;
            ivy.setBoxingExiting(false);
        } else if (isActive()) {
            return;
        }
        exitAfterTaunt = false;
        ivy.setBoxingTaunting(false);
        ivy.cancelPassiveAnimationsForCombat();
        ivy.setBoxingStance(true);
        ivy.setBoxingActionTicks(STANCE_TRANSITION_TICKS);
        setState(CombatState.RECOVERING, STANCE_TRANSITION_TICKS);
        ivy.triggerAnim("movement", "to_orthodox");
    }

    private void clear() {
        attackCooldown = 0;
        dodgeCooldown = 0;
        hookCooldown = 0;
        comboCooldown = 0;
        exitTicks = 0;
        exitAfterTaunt = false;
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        comboRetreatTicks = 0;
        impactTargetId = -1;
        pendingCounter = null;
        lastCombatTarget = null;
        lastTargetId = -1;
        targetStillTicks = 0;
        setState(CombatState.RECOVERING, 0);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(0);
        ivy.setBoxingTaunting(false);
        ivy.setBoxingExiting(false);
        ivy.setBoxingStance(false);
    }

    private void startExitStance() {
        if (exitTicks > 0) {
            return;
        }
        ivy.setBoxingTaunting(false);
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(0);
        clearAttackTimers();
        setState(CombatState.EXITING, EXIT_STANCE_TICKS);
        exitTicks = EXIT_STANCE_TICKS;
        ivy.setBoxingStance(false);
        ivy.setBoxingExiting(true);
        ivy.triggerAnim("movement", "exit_orthodox");
    }

    private void startTaunt(LivingEntity target, boolean exitAfterTaunt) {
        ivy.getNavigation().stop();
        lockSight(target);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(TAUNT_ACTION_TICKS);
        this.exitAfterTaunt = exitAfterTaunt;
        clearAttackTimers();
        setState(CombatState.TAUNTING, TAUNT_ACTION_TICKS);
        ivy.setBoxingTaunting(true);
        ivy.setBoxingExiting(false);
        ivy.triggerAnim("movement", "orthodox_taunt");
    }

    private void clearAttackTimers() {
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        comboRetreatTicks = 0;
        impactTargetId = -1;
        pendingCounter = null;
    }

    private void interruptTaunt(LivingEntity attacker) {
        exitAfterTaunt = false;
        exitTicks = 0;
        ivy.setBoxingTaunting(false);
        ivy.setBoxingExiting(false);
        ivy.setBoxingStance(true);
        ivy.setBoxingActionTicks(0);
        ivy.setBoxingMovement(false, false);
        clearAttackTimers();
        setState(CombatState.RECOVERING, 0);
        ivy.getNavigation().stop();
        lockSight(attacker);
    }

    private void startAttack(LivingEntity target, AttackType attack) {
        setState(CombatState.ATTACKING, attack.actionTicks);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(attack.actionTicks);
        attackCooldown = attack.cooldownTicks;
        if (attack == AttackType.RIGHT_HOOK || attack == AttackType.RIGHT_HOOK_UPPERCUT) {
            hookCooldown = HOOK_COOLDOWN_TICKS;
        }
        if (attack != AttackType.LEFT_JAB && attack != AttackType.RIGHT_HOOK) {
            comboCooldown = COMBO_COOLDOWN_TICKS;
        }
        impactTicks = attack.firstImpactTicks;
        secondImpactTicks = attack.secondImpactTicks;
        thirdImpactTicks = attack.thirdImpactTicks;
        comboRetreatTicks = attack.retreatTicks;
        impactTargetId = target.getId();
        pendingAttack = attack;
        pendingCounter = null;
        ivy.triggerAnim("movement", attack.trigger);
    }

    private void startDodge(LivingEntity target) {
        int dodge = ivy.getRandom().nextInt(3);
        setState(CombatState.DODGING, DODGE_ACTION_TICKS);
        ivy.setBoxingActionTicks(DODGE_ACTION_TICKS);
        dodgeCooldown = DODGE_COOLDOWN_TICKS;
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        comboRetreatTicks = 0;
        pendingCounter = null;
        ivy.triggerAnim("movement", switch (dodge) {
            case 0 -> "dodge_left";
            case 1 -> "dodge_right";
            default -> "dodge_backwards";
        });

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).normalize();
        Vec3 impulse = dodge == 2 ? away.normalize().scale(0.48D) : side.scale(dodge == 0 ? 0.42D : -0.42D);
        ivy.setDeltaMovement(ivy.getDeltaMovement().add(impulse.x, 0.08D, impulse.z));
        ivy.hasImpulse = true;
    }

    private void startCounterDodge(LivingEntity target) {
        CounterType counter = ivy.getRandom().nextBoolean() ? CounterType.LEFT_LIVER_SHOT : CounterType.RIGHT_LIVER_SHOT;
        setState(CombatState.DODGING, LIVER_COUNTER_ACTION_TICKS);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(LIVER_COUNTER_ACTION_TICKS);
        dodgeCooldown = DODGE_COOLDOWN_TICKS + 6;
        impactTicks = LIVER_COUNTER_IMPACT_TICKS;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        comboRetreatTicks = 0;
        impactTargetId = target.getId();
        pendingCounter = counter;
        ivy.triggerAnim("movement", counter.trigger);
        applyDiagonalCounterStep(target, counter);
    }

    private void applyDiagonalCounterStep(LivingEntity target, CounterType counter) {
        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }

        Vec3 radial = away.normalize();
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        if (counter == CounterType.RIGHT_LIVER_SHOT) {
            tangent = tangent.scale(-1.0D);
        }

        Vec3 step = tangent.scale(0.44D).add(radial.scale(-0.18D)).normalize().scale(0.54D);
        ivy.setDeltaMovement(step.x, ivy.getDeltaMovement().y + 0.08D, step.z);
        ivy.hasImpulse = true;
    }

    private void applyImpact(AttackHit hit) {
        if (hit == null) {
            return;
        }
        if (impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target)) {
            impactTargetId = -1;
            return;
        }
        if (!target.isAlive() || !isInHitRange(target, hit)) {
            return;
        }
        if (hit.forwardNudge > 0.0D) {
            applyForwardNudge(target, hit.forwardNudge);
        }
        if (hit.resetInvulnerability) {
            target.invulnerableTime = 0;
        }
        target.hurt(ivy.damageSources().mobAttack(ivy), hit.damage);
        Vec3 knockback = target.position().subtract(ivy.position());
        if (knockback.horizontalDistanceSqr() > 1.0E-4D) {
            target.push(knockback.x * hit.knockback, hit.lift, knockback.z * hit.knockback);
        }
    }

    private boolean isInHitRange(LivingEntity target, AttackHit hit) {
        return ivy.getBoundingBox().inflate(hit.range, 1.15D, hit.range).intersects(target.getBoundingBox());
    }

    private void applyForwardNudge(LivingEntity target, double strength) {
        Vec3 toward = target.position().subtract(ivy.position());
        if (toward.horizontalDistanceSqr() < 1.0E-4D) {
            toward = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 step = toward.normalize().scale(strength);
        setComboHorizontalImpulse(step, 0.02D);
    }

    private void applyComboRetreat() {
        if (impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 step = away.normalize().scale(1.25);
        setComboHorizontalImpulse(step, 0.04D);
    }

    private void setComboHorizontalImpulse(Vec3 step, double lift) {
        Vec3 current = ivy.getDeltaMovement();
        ivy.setDeltaMovement(step.x, current.y + lift, step.z);
        ivy.hasImpulse = true;
    }

    private AttackType chooseAttack(double distanceSqr, boolean targetStill) {
        if (distanceSqr < KEEP_DISTANCE * KEEP_DISTANCE && comboCooldown <= 0) {
            return chooseDistanceKeepingAttack();
        }

        if (comboCooldown <= 0
                && hookCooldown <= 0
                && distanceSqr <= ATTACK_RANGE * ATTACK_RANGE
                && ivy.getRandom().nextInt(100) < HOOK_UPPERCUT_CLOSE_CHANCE) {
            return AttackType.RIGHT_HOOK_UPPERCUT;
        }

        int jabWeight = targetStill ? 25 : 50;
        int hookWeight = hookCooldown <= 0 && distanceSqr <= HOOK_RANGE * HOOK_RANGE ? (targetStill ? 30 : 35) : 0;
        int jabCrossWeight = comboCooldown <= 0 && distanceSqr >= COMBO_MIN_RANGE * COMBO_MIN_RANGE
                ? (targetStill ? 45 : 25) : 0;
        int jabJabHookWeight = comboCooldown <= 0 && distanceSqr >= COMBO_MIN_RANGE * COMBO_MIN_RANGE
                ? (targetStill ? 30 : 18) : 0;

        int totalWeight = jabWeight + hookWeight + jabCrossWeight + jabJabHookWeight;
        int roll = ivy.getRandom().nextInt(totalWeight);
        if (roll < jabJabHookWeight) {
            return AttackType.JAB_JAB_HOOK;
        }
        roll -= jabJabHookWeight;
        if (roll < jabCrossWeight) {
            return AttackType.LEFT_JAB_RIGHT_CROSS;
        }
        roll -= jabCrossWeight;
        if (roll < hookWeight) {
            return AttackType.RIGHT_HOOK;
        }
        return AttackType.LEFT_JAB;
    }

    private AttackType chooseDistanceKeepingAttack() {
        return ivy.getRandom().nextBoolean() ? AttackType.LEFT_JAB_RIGHT_CROSS : AttackType.JAB_JAB_HOOK;
    }

    private void setState(CombatState state, int ticks) {
        this.state = state;
        this.stateTicks = Math.max(0, ticks);
    }

    private void lockSight(LivingEntity target) {
        double dx = target.getX() - ivy.getX();
        double dz = target.getZ() - ivy.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float lockedYaw = approachYaw(targetYaw, ivy.getYRot());
            ivy.setYRot(lockedYaw);
            ivy.lockBoxingBodyToYaw(lockedYaw, LOCK_BODY_YAW_SPEED);
        }
        ivy.getLookControl().setLookAt(target, LOCK_LOOK_YAW_SPEED, LOCK_LOOK_PITCH_SPEED);
        ivy.lookAt(target, LOCK_LOOK_YAW_SPEED, LOCK_LOOK_PITCH_SPEED);
    }

    private static float approachYaw(float target, float current) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -IvyBoxingCombatController.LOCK_BODY_YAW_MAX_DELTA, IvyBoxingCombatController.LOCK_BODY_YAW_MAX_DELTA);
        return current + delta * IvyBoxingCombatController.LOCK_BODY_YAW_SPEED;
    }

    private void closeDistance(LivingEntity target) {
        setState(CombatState.CLOSING_DISTANCE, 8);
        ivy.setBoxingMovement(false, true);
        ivy.getNavigation().moveTo(target, 1.0D);
    }

    private boolean updateTargetStillness(LivingEntity target) {
        if (lastTargetId != target.getId()) {
            lastTargetId = target.getId();
            lastTargetX = target.getX();
            lastTargetZ = target.getZ();
            targetStillTicks = 0;
            return false;
        }

        double dx = target.getX() - lastTargetX;
        double dz = target.getZ() - lastTargetZ;
        if (dx * dx + dz * dz <= TARGET_STILL_EPSILON_SQ) {
            targetStillTicks++;
        } else {
            targetStillTicks = 0;
            lastTargetX = target.getX();
            lastTargetZ = target.getZ();
        }
        return targetStillTicks >= TARGET_STILL_PRESSURE_TICKS;
    }

    private void keepDistance(LivingEntity target) {
        setState(CombatState.KEEPING_DISTANCE, 6);
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(true, false);
        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() > 1.0E-4D) {
            Vec3 step = away.normalize().scale(0.17D);
            ivy.setDeltaMovement(ivy.getDeltaMovement().add(step.x, 0.0D, step.z));
            ivy.hasImpulse = true;
        }
    }

    private void beginCircle() {
        setState(ivy.getRandom().nextBoolean() ? CombatState.CIRCLING_LEFT : CombatState.CIRCLING_RIGHT,
                18 + ivy.getRandom().nextInt(18));
    }

    private void circle(LivingEntity target, double distanceSqr) {
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(false, false);

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }

        Vec3 radial = away.normalize();
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        if (state == CombatState.CIRCLING_RIGHT) {
            tangent = tangent.scale(-1.0D);
        }

        double preferred = 2.9D;
        double distance = Math.sqrt(distanceSqr);
        double radialCorrection = Mth.clamp(distance - preferred, -0.75D, 0.75D) * -0.035D;
        Vec3 step = tangent.scale(0.13D).add(radial.scale(radialCorrection));
        ivy.setDeltaMovement(ivy.getDeltaMovement().add(step.x, 0.0D, step.z));
        ivy.hasImpulse = true;
    }

    private class BoxingGoal extends Goal {
        BoxingGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canBox(ivy.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canBox(ivy.getTarget());
        }

        @Override
        public void start() {
            beginStance();
        }

        @Override
        public void stop() {
            ivy.getNavigation().stop();
            if (ivy.isAlive() && ivy.isBoxingStance()) {
                LivingEntity target = ivy.getTarget() != null ? ivy.getTarget() : lastCombatTarget;
                if (shouldTauntDefeatedEnemy(target)) {
                    startTaunt(target, true);
                } else {
                    startExitStance();
                }
            } else {
                clear();
            }
        }

        @Override
        public void tick() {
            LivingEntity target = ivy.getTarget();
            if (target == null) {
                return;
            }
            lastCombatTarget = target;

            lockSight(target);

            if (ivy.getBoxingActionTicks() > 0) {
                ivy.getNavigation().stop();
                ivy.setBoxingMovement(false, false);
                return;
            }

            double distanceSqr = ivy.distanceToSqr(target);
            boolean targetStill = updateTargetStillness(target);
            if (dodgeCooldown <= 0 && ivy.getRandom().nextInt(80) == 0 && distanceSqr < 16.0D) {
                ivy.getNavigation().stop();
                startDodge(target);
                return;
            }

            if (distanceSqr <= ATTACK_RANGE * ATTACK_RANGE) {
                ivy.getNavigation().stop();
                ivy.setBoxingMovement(false, false);
                if (attackCooldown <= 0) {
                    startAttack(target, chooseAttack(distanceSqr, targetStill));
                } else if (distanceSqr < KEEP_DISTANCE * KEEP_DISTANCE) {
                    keepDistance(target);
                }
                return;
            }

            if (targetStill && distanceSqr > ATTACK_RANGE * ATTACK_RANGE) {
                closeDistance(target);
                return;
            }

            if (distanceSqr > APPROACH_DISTANCE * APPROACH_DISTANCE) {
                closeDistance(target);
                return;
            }

            if ((state != CombatState.CIRCLING_LEFT && state != CombatState.CIRCLING_RIGHT) || stateTicks <= 0) {
                beginCircle();
            }
            circle(target, distanceSqr);
        }

        private boolean canBox(@Nullable LivingEntity target) {
            if (target == null || !target.isAlive() || !ivy.isAlive() || ivy.isTrading() || !ivy.isReadyForCombatAnimation()) {
                return false;
            }
            double distanceSqr = ivy.distanceToSqr(target);
            return distanceSqr <= 256.0D && (distanceSqr <= 36.0D || ivy.hasLineOfSight(target));
        }
    }

    private boolean shouldTauntDefeatedEnemy(@Nullable LivingEntity target) {
        return target != null && !target.isAlive() && !(target instanceof Player);
    }

    private enum CombatState {
        CLOSING_DISTANCE,
        KEEPING_DISTANCE,
        CIRCLING_LEFT,
        CIRCLING_RIGHT,
        ATTACKING,
        DODGING,
        TAUNTING,
        EXITING,
        RECOVERING
    }

    private enum AttackType {
        LEFT_JAB("orthodox_left_jab", JAB_ACTION_TICKS, JAB_IMPACT_TICKS, 0, 0, 0, JAB_COOLDOWN_TICKS,
                new AttackHit(2.5F, 0.015D, 0.0D, 2.35D, false, 0.0D), null, null),
        RIGHT_HOOK("orthodox_right_hook", HOOK_ACTION_TICKS, HOOK_IMPACT_TICKS, 0, 0, 0, HOOK_COOLDOWN_TICKS,
                new AttackHit(6.0F, 0.28D, 0.08D, 2.45D, false, 0.0D), null, null),
        LEFT_JAB_RIGHT_CROSS("orthodox_left_jab_right_cross", COMBO_ACTION_TICKS, COMBO_FIRST_IMPACT_TICKS,
                COMBO_SECOND_IMPACT_TICKS, 0, COMBO_RETREAT_TICKS, JAB_COOLDOWN_TICKS,
                new AttackHit(2.0F, 0.02D, 0.0D, 2.55D, true, 0.0D),
                new AttackHit(4.0F, 0.16D, 0.04D, 2.95D, true, 1), null),
        JAB_JAB_HOOK("orthodox_jab_jab_hook", JAB_JAB_HOOK_ACTION_TICKS,
                JAB_JAB_HOOK_FIRST_IMPACT_TICKS, JAB_JAB_HOOK_SECOND_IMPACT_TICKS,
                JAB_JAB_HOOK_THIRD_IMPACT_TICKS, 0, JAB_COOLDOWN_TICKS,
                new AttackHit(1.8F, 0.0D, 0.0D, 2.45D, true, 0.0D),
                new AttackHit(1.8F, 0.0D, 0.0D, 2.45D, true, 0.0D),
                new AttackHit(5.5F, 0.24D, 0.06D, 3.05D, true, 0.55D)),
        RIGHT_HOOK_UPPERCUT("orthodox_right_hook_uppercut", RIGHT_HOOK_UPPERCUT_ACTION_TICKS,
                RIGHT_HOOK_UPPERCUT_FIRST_IMPACT_TICKS, RIGHT_HOOK_UPPERCUT_SECOND_IMPACT_TICKS,
                0, 0, HOOK_COOLDOWN_TICKS,
                new AttackHit(4.0F, 0.015D, 0.0D, 2.55D, true, 0.0D),
                new AttackHit(6.0F, 0.12D, 0.24D, 3.05D, true, 0.55D),
                null);

        private final String trigger;
        private final int actionTicks;
        private final int firstImpactTicks;
        private final int secondImpactTicks;
        private final int thirdImpactTicks;
        private final int retreatTicks;
        private final int cooldownTicks;
        private final AttackHit firstHit;
        private final AttackHit secondHit;
        private final AttackHit thirdHit;

        AttackType(String trigger, int actionTicks, int firstImpactTicks, int secondImpactTicks, int thirdImpactTicks,
                   int retreatTicks, int cooldownTicks, AttackHit firstHit, @Nullable AttackHit secondHit,
                   @Nullable AttackHit thirdHit) {
            this.trigger = trigger;
            this.actionTicks = actionTicks;
            this.firstImpactTicks = firstImpactTicks;
            this.secondImpactTicks = secondImpactTicks;
            this.thirdImpactTicks = thirdImpactTicks;
            this.retreatTicks = retreatTicks;
            this.cooldownTicks = cooldownTicks;
            this.firstHit = firstHit;
            this.secondHit = secondHit;
            this.thirdHit = thirdHit;
        }
    }

    private enum CounterType {
        LEFT_LIVER_SHOT("dodge_left_liver_shot", new AttackHit(5.0F, 0.09D, 0.0D, 2.55D, true, 0.0D)),
        RIGHT_LIVER_SHOT("dodge_right_liver_shot", new AttackHit(5.0F, 0.09D, 0.0D, 2.55D, true, 0.0D));

        private final String trigger;
        private final AttackHit hit;

        CounterType(String trigger, AttackHit hit) {
            this.trigger = trigger;
            this.hit = hit;
        }
    }

    private record AttackHit(float damage, double knockback, double lift, double range, boolean resetInvulnerability,
                             double forwardNudge) {
    }
}
