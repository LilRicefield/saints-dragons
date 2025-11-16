package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * All-in-one combat goal for Ignivorus - handles movement, attack selection, and execution.
 * Fire dragon combat: AGGRESSIVE melee-focused with randomized attacks.
 * Prioritizes unpredictable bite/body slam combos, only uses fire breath at long range.
 */
public class IgnivorusCombatGoal extends Goal {
    private final Ignivorus dragon;

    // Combat ranges
    private final double meleeEngageRange = 6.0;      // Chase until this close, then stop and melee
    private final double fireBreathMinRange = 32.0;   // Only use fire breath when target is this far

    private final double chaseSpeed = 1.75D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    // Fire breath cooldown mechanic (AI only - 3 minute cooldown)
    private int breathCooldown = 0;
    private static final int BREATH_COOLDOWN_TICKS = 3600; // 3 minutes (60 seconds * 20 ticks * 3)

    public IgnivorusCombatGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        // Don't attack creative/spectator players
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = dragon.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        // Stop attacking if player switches to creative/spectator
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        // Don't modify running state - let other systems handle it
        dragon.setAggressive(false);
        dragon.setGroundMoveStateFromAI(0);
        cancelFireBreathIfActive();
        pathRecalcCooldown = 0;
    }

    @Override
    public void start() {
        // Don't set running to avoid speed boost - just use chaseSpeed multiplier
        dragon.setAggressive(true);
        dragon.setGroundMoveStateFromAI(2);

        LivingEntity target = dragon.getTarget();
        if (target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dragon.getNavigation().moveTo(target, chaseSpeed);
            rememberTargetPosition(target);

            // Try attacking immediately if in range
            tryAttack(target);
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Tick down breath cooldown
        if (breathCooldown > 0) {
            breathCooldown--;
        }

        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) {
            // Target died or disappeared - immediately cancel fire breath
            cancelFireBreathIfActive();
            updateGroundMoveState();
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        // AGGRESSIVE CHASE: Keep chasing until in melee range
        // This ensures dragon gets close for bite/body slam instead of staying at fire breath range
        if (gap > meleeEngageRange || !hasLineOfSight) {
            if (!isCurrentlyAttacking()) {
                updateChasePath(target);
            }
        } else {
            // In melee range - stop moving and unleash melee attacks
            dragon.getNavigation().stop();
            pathRecalcCooldown = 0;
        }

        // Always try to attack when target is visible (attack selection handles range logic)
        if (hasLineOfSight) {
            tryAttack(target);
        }

        updateGroundMoveState();
    }

    /**
     * Check if dragon is currently executing an attack ability
     */
    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BODY_SLAM)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
    }

    /**
     * Immediately cancels fire breath if active (e.g., when target dies or switches to creative)
     */
    private void cancelFireBreathIfActive() {
        // Don't interfere if being ridden (let rider control abilities)
        if (dragon.isVehicle()) {
            return;
        }

        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
            dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
        }
    }

    /**
     * Attack selection: MELEE-FOCUSED with unpredictable randomization.
     * Fire breath ONLY at long range (AI can't aim well).
     * Bite and body slam randomized for unpredictability - NOT range-based.
     */
    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        // Fire breath at long range (>32 blocks) when available
        if (gap > fireBreathMinRange && breathCooldown <= 0) {
            dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            attackCooldown = 60; // Long cooldown after breath
            breathCooldown = BREATH_COOLDOWN_TICKS; // 3 minute cooldown for AI breath
        } else if (gap <= meleeEngageRange) {
            // Melee attacks ONLY in melee range (<6 blocks)
            // Randomly choose between bite and body slam for unpredictability
            if (dragon.getRandom().nextBoolean()) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BODY_SLAM);
                attackCooldown = 25; // Moderate cooldown for body slam
            } else {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BITE);
                attackCooldown = 20; // Slightly faster cooldown for bite
            }
        }
        // No attack in 6-32 block range (or when breath on cooldown at >32 blocks) - just chase
    }

    /**
     * Get the gap between entity edges (not centers)
     */
    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.dragon.distanceTo(target);
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = dragon.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            dragon.getNavigation().moveTo(target, chaseSpeed);
        }
    }

    private void rememberTargetPosition(LivingEntity target) {
        this.lastTargetX = target.getX();
        this.lastTargetY = target.getY();
        this.lastTargetZ = target.getZ();
    }

    private boolean targetMovedSignificantly(LivingEntity target) {
        double dx = target.getX() - this.lastTargetX;
        double dy = target.getY() - this.lastTargetY;
        double dz = target.getZ() - this.lastTargetZ;
        return dx * dx + dy * dy + dz * dz > 4.0D;
    }

    private void updateGroundMoveState() {
        if (!dragon.isFlying() && dragon.onGround() && dragon.getNavigation().isInProgress()) {
            dragon.setGroundMoveStateFromAI(2);
        } else {
            dragon.setGroundMoveStateFromAI(0);
        }
    }
}
