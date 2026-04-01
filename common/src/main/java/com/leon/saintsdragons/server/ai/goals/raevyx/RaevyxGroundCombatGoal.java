package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * All-in-one combat goal for Raevyx - handles movement, attack selection, and execution.
 * No state machine, no windup phases - just instant attacks when in range.
 */
public class RaevyxGroundCombatGoal extends Goal {
    private final Raevyx wyvern;
    private final double biteRange = 3.0;
    private final double goreRange = 4.5;
    private final double groundRendRange = 8.5;
    private final double groundRendMinRange = 3.4;
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
    private int groundRendCooldown = 0;
    private static final int GROUND_REND_COOLDOWN_TICKS = 400;
    private final RaevyxCombatDirector combatDirector = new RaevyxCombatDirector();
    private int postRoarGroundRendTicks = 0;

    public RaevyxGroundCombatGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wyvern.getTarget();

        if (!wyvern.isTargetValid(target)) {
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

        // Don't use ground combat if target is airborne (let air combat goal handle it)
        if (isTargetAirborne(target)) {
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

        if (!wyvern.isTargetValid(target)) {
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

        // Stop ground combat if target becomes airborne (switch to air combat goal)
        if (isTargetAirborne(target)) {
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
        combatDirector.reset();

        // Reset roar opener for next combat encounter
        hasUsedRoarOpener = false;
        roarOpenerDelay = 0;
    }

    @Override
    public void start() {
        // Don't set running to avoid speed boost - just use chaseSpeed multiplier
        wyvern.setAggressive(true);
        combatDirector.reset();

        // Initialize roar opener - wait for sleep transitions to finish
        hasUsedRoarOpener = false;
        roarOpenerDelay = wyvern.isSleepTransitioning() ? 30 : 8;

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
        if (groundRendCooldown > 0) {
            groundRendCooldown--;
        }
        if (postRoarGroundRendTicks > 0) {
            postRoarGroundRendTicks--;
        }

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            if (wyvern.isGroundRending()) {
                wyvern.getNavigation().stop();
                pathRecalcCooldown = 0;
                return;
            }

            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // In water, use direct swim steering instead of land path navigation to avoid sinking/stalling.
            if (wyvern.isInWaterOrBubble()) {
                handleWaterCombat(target);
                return;
            }

            // Handle roar opener - use roar once at the start of combat
            if (!hasUsedRoarOpener) {
                if (wyvern.isSleepTransitioning()) {
                    roarOpenerDelay = Math.max(roarOpenerDelay, 8);
                    updateChasePath(target);
                    return;
                }
                if (roarOpenerDelay > 0) {
                    roarOpenerDelay--;
                    // Keep chasing during delay
                    updateChasePath(target);
                } else {
                    // Delay expired - use roar ability
                    if (!canUseAiAbility(RaevyxAbilities.RAEVYX_ROAR, true)) {
                        updateChasePath(target);
                        return;
                    }
                    wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_ROAR);
                    wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_ROAR, 40, 80, true, 120, 40);
                    hasUsedRoarOpener = true;
                    attackCooldown = 40; // Brief cooldown after roar
                    postRoarGroundRendTicks = 40;
                    wyvern.tryAIGroundDodge(target); // sidestep to avoid bolt flames
                }
                return; // Don't do normal attacks during roar opener phase
            }

            // Normal combat after roar opener
            double gap = getGapToTarget(target);
            boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);
            boolean beamReady = beamCooldown <= 0;
            combatDirector.tick(wyvern, target, gap, hasLineOfSight, beamReady);

            if (tryGroundRendPressure(target, gap, hasLineOfSight)) {
                return;
            }

            if (combatDirector.shouldTryDodge(wyvern, target, gap, isCurrentlyAttacking())) {
                if (wyvern.tryAIGroundDodge(target)) {
                    attackCooldown = Math.max(attackCooldown, 8);
                    return;
                }
            }

            if (combatDirector.shouldTryDash(wyvern, gap, isCurrentlyAttacking())) {
                if (wyvern.tryAIGroundDash(target)) {
                    attackCooldown = Math.max(attackCooldown, 12);
                    return;
                }
            }

            if (tryPostRoarGroundRend(target, gap, hasLineOfSight)) {
                return;
            }

            if (tryDirectedBeam(target, hasLineOfSight, beamReady)) {
                return;
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
            || wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_GROUND_REND)
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

        if (gap <= groundRendRange && gap > groundRendMinRange && groundRendCooldown <= 0) {
            if (startGroundRend()) {
                attackCooldown = 26;
                groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
            }
            return;
        }

        // Choose attack based on distance - fire immediately
        if (gap <= biteRange) {
            // Close range - bite attack
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_BITE, false)) {
                return;
            }
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_BITE);
            wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_BITE, 20, 20, false, 0, 18);
            attackCooldown = 20;
        } else if (gap <= goreRange) {
            // Medium range - horn gore
            if (!canUseAiAbility(RaevyxAbilities.RAEVYX_HORN_GORE, false)) {
                return;
            }
            wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_HORN_GORE);
            wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_HORN_GORE, 20, 22, false, 0, 22);
            attackCooldown = 20;
        }
        // Note: 4.5-32 block range has no attack - wyvern will chase to get closer
    }

    private boolean tryDirectedBeam(LivingEntity target, boolean hasLineOfSight, boolean beamReady) {
        if (!combatDirector.shouldTryBeam(wyvern, getGapToTarget(target), hasLineOfSight, beamReady, isCurrentlyAttacking(), attackCooldown)) {
            return false;
        }
        if (!canUseAiAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, true)) {
            return false;
        }
        wyvern.getNavigation().stop();
        pathRecalcCooldown = 0;
        wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM);
        wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM, 60, BEAM_COOLDOWN_TICKS, true, 160, 80);
        attackCooldown = 60;
        beamCooldown = BEAM_COOLDOWN_TICKS;
        return true;
    }

    private boolean tryPostRoarGroundRend(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (postRoarGroundRendTicks <= 0 || attackCooldown > 0 || isCurrentlyAttacking() || groundRendCooldown > 0) {
            return false;
        }
        if (!hasLineOfSight || gap > groundRendRange || gap < groundRendMinRange) {
            return false;
        }
        wyvern.getNavigation().stop();
        pathRecalcCooldown = 0;
        if (!startGroundRend()) {
            return false;
        }
        attackCooldown = 26;
        groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
        postRoarGroundRendTicks = 0;
        return true;
    }

    private boolean tryGroundRendPressure(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (attackCooldown > 0 || isCurrentlyAttacking() || groundRendCooldown > 0) {
            return false;
        }
        if (!hasLineOfSight || gap < groundRendMinRange || gap > groundRendRange) {
            return false;
        }
        if (gap <= goreRange && combatDirector.shouldTryDodge(wyvern, target, gap, false)) {
            return false;
        }
        wyvern.getNavigation().stop();
        pathRecalcCooldown = 0;
        if (!startGroundRend()) {
            return false;
        }
        attackCooldown = 26;
        groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
        return true;
    }

    private boolean startGroundRend() {
        if (!canUseAiAbility(RaevyxAbilities.RAEVYX_GROUND_REND, true)) {
            return false;
        }
        wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_GROUND_REND);
        wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_GROUND_REND, 26, GROUND_REND_COOLDOWN_TICKS, true, 100, 34);
        return wyvern.isAbilityActive(RaevyxAbilities.RAEVYX_GROUND_REND);
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
        if (wyvern.isInWaterOrBubble()) {
            // Ground navigation is unreliable in water for this dragon.
            return;
        }
        if (--pathRecalcCooldown <= 0 || targetMovedSignificantly(target)) {
            rememberTargetPosition(target);
            double distance = wyvern.distanceTo(target);
            pathRecalcCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            wyvern.getNavigation().moveTo(target, chaseSpeed);
        }
    }

    private void handleWaterCombat(LivingEntity target) {
        wyvern.getNavigation().stop();

        // Keep roar opener behavior in water too.
        if (!hasUsedRoarOpener) {
            if (wyvern.isSleepTransitioning()) {
                roarOpenerDelay = Math.max(roarOpenerDelay, 8);
            } else if (roarOpenerDelay > 0) {
                roarOpenerDelay--;
            } else {
                if (!canUseAiAbility(RaevyxAbilities.RAEVYX_ROAR, true)) {
                    return;
                }
                wyvern.combatManager.tryUseAbility(RaevyxAbilities.RAEVYX_ROAR);
                wyvern.getAiCombatPacing().recordUse(RaevyxAbilities.RAEVYX_ROAR, 40, 80, true, 120, 40);
                hasUsedRoarOpener = true;
                attackCooldown = 40;
            }
        }

        boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);
        double gap = getGapToTarget(target);
        boolean inAttackRange = gap <= goreRange;

        // Direct steering toward target with gentle buoyancy so it doesn't sink while aggroing.
        Vec3 current = wyvern.getDeltaMovement();
        Vec3 toTarget = target.position().subtract(wyvern.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
        Vec3 desiredHorizontal = horizontal.lengthSqr() > 1.0E-4
                ? horizontal.normalize().scale(0.27D)
                : Vec3.ZERO;

        double nx = current.x + (desiredHorizontal.x - current.x) * 0.30D;
        double nz = current.z + (desiredHorizontal.z - current.z) * 0.30D;

        double targetY = target.getY() + target.getBbHeight() * 0.45D;
        double yDiff = targetY - wyvern.getY();
        double ny = current.y;
        if (yDiff > 0.9D) {
            ny = Math.max(current.y + 0.035D, 0.06D);
        } else if (yDiff < -1.3D) {
            ny = Math.min(current.y - 0.03D, -0.08D);
        } else {
            ny = current.y + 0.012D;
        }

        wyvern.setDeltaMovement(nx, ny, nz);
        wyvern.getMoveControl().setWantedPosition(target.getX(), targetY, target.getZ(), 1.0D);

        if (hasUsedRoarOpener && inAttackRange && hasLineOfSight) {
            tryAttack(target);
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

    /**
     * Check if target is airborne (flying, riding flying mount, or off ground)
     */
    private boolean isTargetAirborne(LivingEntity target) {
        // Check if target is on ground
        if (target.onGround()) {
            return false;
        }

        // Check if riding something (might be a flying dragon)
        if (target.isPassenger() && target.getVehicle() != null) {
            return true; // Assume mounted targets are valid air targets
        }

        // Check if significantly off ground (more than 8 blocks up for elytra/flight stability)
        // Increased from 3 to prevent low elytra gliding from triggering constant takeoff
        double groundY = wyvern.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        if (target.getY() - groundY > 8.0) {
            return true;
        }

        return false;
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return wyvern.combatManager.canStart(abilityType) && wyvern.getAiCombatPacing().canUse(abilityType, majorAbility);
    }
}
