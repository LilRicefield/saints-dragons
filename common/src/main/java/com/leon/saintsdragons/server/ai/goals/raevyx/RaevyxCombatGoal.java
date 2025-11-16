package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;

/**
 * All-in-one combat goal for Raevyx - handles movement, attack selection, and execution.
 * No state machine, no windup phases - just instant attacks when in range.
 */
public class RaevyxCombatGoal extends Goal {
    private final Raevyx wyvern;
    private final double biteRange = 3.0;
    private final double goreRange = 4.5;
    private final double beamMinRange = 32.0; // Only use beam when target is far away
    private final double chaseSpeed = 1.75D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    // Roar opener mechanic
    private boolean hasUsedRoarOpener = false;
    private int roarOpenerDelay = 0;

    // Beam cooldown mechanic (AI only - 3 minute cooldown)
    private int beamCooldown = 0;
    private static final int BEAM_COOLDOWN_TICKS = 3600; // 3 minutes (60 seconds * 20 ticks * 3)

    public RaevyxCombatGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wyvern.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        // Don't attack creative/spectator players
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = wyvern.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        // Stop attacking if player switches to creative/spectator
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
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
        wyvern.getNavigation().stop();
        // Don't modify running state - let other systems handle it
        wyvern.setAggressive(false);
        pathRecalcCooldown = 0;

        // Reset roar opener for next combat encounter
        hasUsedRoarOpener = false;
        roarOpenerDelay = 0;
    }

    @Override
    public void start() {
        // Don't set running to avoid speed boost - just use chaseSpeed multiplier
        wyvern.setAggressive(true);

        // Initialize roar opener - wait 5 ticks then roar
        hasUsedRoarOpener = false;
        roarOpenerDelay = 5;

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            wyvern.getNavigation().moveTo(target, chaseSpeed);
            rememberTargetPosition(target);
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Tick down beam cooldown
        if (beamCooldown > 0) {
            beamCooldown--;
        }

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Handle roar opener - use roar once at the start of combat
            if (!hasUsedRoarOpener) {
                if (roarOpenerDelay > 0) {
                    roarOpenerDelay--;
                    // Keep chasing during delay
                    updateChasePath(target);
                } else {
                    // Delay expired - use roar ability
                    wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_ROAR);
                    hasUsedRoarOpener = true;
                    attackCooldown = 40; // Brief cooldown after roar
                }
                return; // Don't do normal attacks during roar opener phase
            }

            // Normal combat after roar opener
            double gap = getGapToTarget(target);
            boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);

            // Only use beam when target is far away (>32 blocks) AND beam is off cooldown
            if (gap > beamMinRange && hasLineOfSight && beamCooldown <= 0) {
                // Long range and beam available - stop and use beam
                if (!isCurrentlyAttacking()) {
                    wyvern.getNavigation().stop();
                    pathRecalcCooldown = 0;
                }
                tryAttack(target);
            } else if (gap > goreRange) {
                // Medium-long range (4.5-32 blocks) OR beam on cooldown - chase to get closer
                if (!isCurrentlyAttacking()) {
                    updateChasePath(target);
                }
            } else {
                // In melee range (0-4.5 blocks) - stop moving and attack
                wyvern.getNavigation().stop();
                pathRecalcCooldown = 0;
                tryAttack(target);
            }
        }
    }

    /**
     * Check if wyvern is currently executing an attack ability
     */
    private boolean isCurrentlyAttacking() {
        return wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_BITE)
            || wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_HORN_GORE)
            || wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM)
            || wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_ROAR);
    }

    /**
     * Try to attack target based on distance. Instant execution, no windup.
     */
    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!wyvern.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        // Choose attack based on distance - fire immediately
        if (gap <= biteRange) {
            // Close range - bite attack
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_BITE);
            attackCooldown = 20;
        } else if (gap <= goreRange) {
            // Medium range - horn gore
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_HORN_GORE);
            attackCooldown = 20;
        } else if (gap > beamMinRange && beamCooldown <= 0) {
            // Long range (>32 blocks) and beam available - lightning beam (smart tracking)
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM);
            attackCooldown = 60;
            beamCooldown = BEAM_COOLDOWN_TICKS; // 3 minute cooldown after using beam
        }
        // Note: 4.5-32 block range has no attack - wyvern will chase to get closer
    }

    /**
     * Get the gap between entity edges (not centers)
     */
    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.wyvern.distanceTo(target);
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.wyvern.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = wyvern.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            wyvern.getNavigation().moveTo(target, chaseSpeed);
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
}
