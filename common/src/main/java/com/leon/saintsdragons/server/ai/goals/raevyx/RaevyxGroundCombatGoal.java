package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class RaevyxGroundCombatGoal extends Goal {
    private final Raevyx wyvern;
    private final double biteRange = 3;
    private final double biteOnlyPreyRange = 1.35;
    private final double goreRange = 4.5;
    private final double groundRendRange = 8.5;
    private final double groundRendMinRange = 3.4;
    private final double chaseSpeed = DragonAIMovementController.GROUND_SPRINT_SPEED;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private int pathFailureBackoff = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;
    private int beamCooldown = 0;
    private static final int BEAM_COOLDOWN_TICKS = 3600;
    private int groundRendCooldown = 0;
    private static final int GROUND_REND_COOLDOWN_TICKS = 400;
    private int postRoarGroundRendTicks = 0;
    private static final int MODE_REEVALUATE_TICKS = 6;
    private static final int DAMAGE_MEMORY_TICKS = 30;
    private static final double DASH_MIN_RANGE = 8.0D;
    private static final double DASH_MAX_RANGE = 26.0D;
    private static final float BUDGET_REGEN_PER_TICK = 0.025f;
    private static final float DASH_COST = 0.62f;
    private static final float BEAM_COST = 0.25f;
    private static final int MOBILITY_LOCK_TICKS = 24;
    private static final int BEAM_LOCK_TICKS = 30;
    private Mode combatMode = Mode.PRESSURE;
    private int modeReevaluateCooldown = 0;
    private int recentDamageTicks = 0;
    private long lastDamageGameTime = Long.MIN_VALUE;
    private int mobilityLockTicks = 0;
    private int beamLockTicks = 0;
    private float mobilityBudget = 1.0f;

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
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.isFlying() || wyvern.isHovering() || wyvern.isTakeoff() || wyvern.isLanding()) {
            return false;
        }

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

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (wyvern.isVehicle() || wyvern.isOrderedToSit()) {
            return false;
        }

        if (wyvern.isFlying() || wyvern.isHovering() || wyvern.isTakeoff() || wyvern.isLanding()) {
            return false;
        }

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
        wyvern.getAIMovement().stop();
        wyvern.setAggressive(false);
        pathRecalcCooldown = 0;
        pathFailureBackoff = 0;
        resetCombatPacing();
    }

    @Override
    public void start() {
        wyvern.setAggressive(true);
        resetCombatPacing();

        LivingEntity target = wyvern.getTarget();
        if (target != null) {
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (!wyvern.getAIMovement().moveToGroundTarget(target, chaseSpeed, true)) {
                wyvern.getAIMovement().setGroundIdle();
                pathFailureBackoff = 20;
            }
            rememberTargetPosition(target);
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
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
                wyvern.getAIMovement().stop();
                pathRecalcCooldown = 0;
                return;
            }

            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (wyvern.isInWaterOrBubble()) {
                handleWaterCombat(target);
                return;
            }
            boolean biteOnlyPrey = DragonTargetingHelper.isBiteOnlyPreyTarget(target);
            double gap = getGapToTarget(target);
            boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);
            boolean beamReady = beamCooldown <= 0;
            if (!biteOnlyPrey) {
                tickCombatPacing(target, gap, hasLineOfSight, beamReady);
            }
            if (!biteOnlyPrey && tryGroundRendPressure(target, gap, hasLineOfSight)) {
                return;
            }

            if (!biteOnlyPrey && tryRoar(gap, hasLineOfSight)) {
                return;
            }

            if (!biteOnlyPrey && shouldTryDash(gap, isCurrentlyAttacking())) {
                if (tryAIGroundDash(target)) {
                    attackCooldown = Math.max(attackCooldown, 12);
                    return;
                }
            }

            if (!biteOnlyPrey && tryPostRoarGroundRend(target, gap, hasLineOfSight)) {
                return;
            }

            if (!biteOnlyPrey && tryDirectedBeam(target, hasLineOfSight, beamReady)) {
            } else if (gap > getMeleeStopRange(target)) {
                if (!isCurrentlyAttacking()) {
                    updateChasePath(target);
                }
            } else {
                if (attackCooldown > 0 || wyvern.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
                    updateChasePath(target);
                    return;
                }
                wyvern.getAIMovement().stop();
                pathRecalcCooldown = 0;
                tryAttack(target);
            }
        }
    }

    private boolean isCurrentlyAttacking() {
        return wyvern.isAbilityActive(ModAbilities.RAEVYX_BITE)
            || wyvern.isAbilityActive(ModAbilities.RAEVYX_HORN_GORE)
            || wyvern.isAbilityActive(ModAbilities.RAEVYX_GROUND_REND)
            || wyvern.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)
            || wyvern.isAbilityActive(ModAbilities.RAEVYX_ROAR);
    }

    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!wyvern.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
            if (gap <= biteOnlyPreyRange && canUseAiAbility(ModAbilities.RAEVYX_BITE, false)) {
                if (startAiAbility(ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
                    attackCooldown = 20;
                }
            }
            return;
        }

        if (gap <= groundRendRange && gap > groundRendMinRange && groundRendCooldown <= 0) {
            if (startGroundRend()) {
                attackCooldown = 26;
                groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
            }
            return;
        }

        if (gap <= biteRange) {
            if (!canUseAiAbility(ModAbilities.RAEVYX_BITE, false)) {
                return;
            }
            if (startAiAbility(ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
                attackCooldown = 20;
            }
        } else if (gap <= goreRange) {
            if (!canUseAiAbility(ModAbilities.RAEVYX_HORN_GORE, false)) {
                return;
            }
            if (startAiAbility(ModAbilities.RAEVYX_HORN_GORE, false, 20, 22, 0, 22)) {
                attackCooldown = 20;
            }
        }
    }

    private boolean tryDirectedBeam(LivingEntity target, boolean hasLineOfSight, boolean beamReady) {
        if (RaevyxBeamAbility.isAtAiBeamMercyThreshold(target)) {
            return false;
        }
        if (!shouldTryBeam(getGapToTarget(target), hasLineOfSight, beamReady, isCurrentlyAttacking(), attackCooldown)) {
            return false;
        }
        if (!canUseAiAbility(ModAbilities.RAEVYX_LIGHTNING_BEAM, true)) {
            return false;
        }
        wyvern.getAIMovement().stop();
        pathRecalcCooldown = 0;
        if (!startAiAbility(ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, BEAM_COOLDOWN_TICKS, 160, 80)) {
            return false;
        }
        attackCooldown = 60;
        beamCooldown = BEAM_COOLDOWN_TICKS;
        return true;
    }

    private boolean tryRoar(double gap, boolean hasLineOfSight) {
        if (attackCooldown > 0 || isCurrentlyAttacking() || !hasLineOfSight || gap > 18.0D) {
            return false;
        }
        if (wyvern.getRandom().nextFloat() >= 0.045F || !canUseAiAbility(ModAbilities.RAEVYX_ROAR, true)) {
            return false;
        }
        wyvern.getAIMovement().stop();
        pathRecalcCooldown = 0;
        if (!startAiAbility(ModAbilities.RAEVYX_ROAR, true, 24, 70, 80, 32)) {
            return false;
        }
        attackCooldown = 24;
        postRoarGroundRendTicks = 40;
        return true;
    }

    private boolean tryAIGroundDash(LivingEntity target) {
        if (wyvern.isFlying() || wyvern.isTakeoff() || wyvern.isLanding() || wyvern.isHovering() || wyvern.isInWaterOrBubble()) {
            return false;
        }
        if (wyvern.isDashing() || wyvern.isDodging()) {
            return false;
        }

        double dx = target.getX() - wyvern.getX();
        double dz = target.getZ() - wyvern.getZ();
        if (dx * dx + dz * dz > 0.0001D) {
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            wyvern.setYRot(targetYaw);
            wyvern.yBodyRot = targetYaw;
        }

        final int dashDuration = 27;
        final int dashCooldown = 50;
        final double dashDistance = 30.0D;
        if (!wyvern.beginForwardDashMotion(dashDuration, dashCooldown, dashDistance)) {
            return false;
        }
        wyvern.triggerDashFeedback();
        return true;
    }

    private boolean tryPostRoarGroundRend(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (postRoarGroundRendTicks <= 0 || attackCooldown > 0 || isCurrentlyAttacking() || groundRendCooldown > 0) {
            return false;
        }
        if (!hasLineOfSight || gap > groundRendRange || gap < groundRendMinRange) {
            return false;
        }
        wyvern.getAIMovement().stop();
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
        wyvern.getAIMovement().stop();
        pathRecalcCooldown = 0;
        if (!startGroundRend()) {
            return false;
        }
        attackCooldown = 26;
        groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
        return true;
    }

    private boolean startGroundRend() {
        if (!canUseAiAbility(ModAbilities.RAEVYX_GROUND_REND, true)) {
            return false;
        }
        return startAiAbility(ModAbilities.RAEVYX_GROUND_REND, true, 26, GROUND_REND_COOLDOWN_TICKS, 100, 34)
                && wyvern.isAbilityActive(ModAbilities.RAEVYX_GROUND_REND);
    }

    private void resetCombatPacing() {
        combatMode = Mode.PRESSURE;
        modeReevaluateCooldown = 0;
        recentDamageTicks = 0;
        lastDamageGameTime = Long.MIN_VALUE;
        mobilityLockTicks = 0;
        beamLockTicks = 0;
        mobilityBudget = 1.0f;
    }

    private void tickCombatPacing(LivingEntity target, double gap, boolean hasLineOfSight, boolean beamReady) {
        if (mobilityLockTicks > 0) {
            mobilityLockTicks--;
        }
        if (beamLockTicks > 0) {
            beamLockTicks--;
        }
        if (recentDamageTicks > 0) {
            recentDamageTicks--;
        }
        mobilityBudget = Math.min(1.0f, mobilityBudget + BUDGET_REGEN_PER_TICK);

        long gameTime = wyvern.level().getGameTime();
        if (wyvern.hurtTime > 0 && gameTime != lastDamageGameTime) {
            lastDamageGameTime = gameTime;
            recentDamageTicks = DAMAGE_MEMORY_TICKS;
        }

        if (modeReevaluateCooldown > 0) {
            modeReevaluateCooldown--;
            return;
        }
        modeReevaluateCooldown = MODE_REEVALUATE_TICKS;
        combatMode = decideMode(target, gap, hasLineOfSight, beamReady);
    }

    private boolean shouldTryDash(double gap, boolean currentlyAttacking) {
        if (currentlyAttacking || wyvern.isBeaming() || wyvern.isDodging() || wyvern.isDashing()) {
            return false;
        }
        if (mobilityLockTicks > 0 || gap < DASH_MIN_RANGE || gap > DASH_MAX_RANGE || mobilityBudget < DASH_COST) {
            return false;
        }

        float chance = switch (combatMode) {
            case SPACE -> 0.20f;
            case PRESSURE -> 0.14f;
            default -> 0.06f;
        };
        if (recentDamageTicks > 0) {
            chance *= 0.75f;
        }

        if (wyvern.getRandom().nextFloat() >= chance) {
            return false;
        }
        mobilityBudget = Math.max(0.0f, mobilityBudget - DASH_COST);
        mobilityLockTicks = MOBILITY_LOCK_TICKS;
        beamLockTicks = Math.max(beamLockTicks, 12);
        return true;
    }

    private boolean shouldTryBeam(double gap, boolean hasLineOfSight, boolean beamReady, boolean currentlyAttacking, int cooldownTicks) {
        if (currentlyAttacking || cooldownTicks > 0) {
            return false;
        }
        if (!beamReady || !hasLineOfSight || beamLockTicks > 0 || mobilityBudget < BEAM_COST) {
            return false;
        }

        float chance = switch (combatMode) {
            case PRESSURE -> 0.20f;
            case SPACE -> 0.10f;
            default -> 0.04f;
        };
        if (gap < 8.0D || gap > 32.0D) {
            chance *= 0.45f;
        }
        if (recentDamageTicks > 0) {
            chance *= 0.8f;
        }

        if (wyvern.getRandom().nextFloat() >= chance) {
            return false;
        }
        mobilityBudget = Math.max(0.0f, mobilityBudget - BEAM_COST);
        beamLockTicks = BEAM_LOCK_TICKS;
        return true;
    }

    private Mode decideMode(LivingEntity target, double gap, boolean hasLineOfSight, boolean beamReady) {
        if (recentDamageTicks > 0 && gap <= 14.0D) {
            return Mode.EVADE;
        }
        if (gap <= 5.0D) {
            return Mode.REPOSITION;
        }
        if (gap > 12.0D) {
            return Mode.SPACE;
        }
        if (hasLineOfSight && beamReady && gap >= 8.0D && gap <= 28.0D) {
            return Mode.PRESSURE;
        }
        if (isLikelyMeleeThreat(target, gap)) {
            return Mode.EVADE;
        }
        return Mode.PRESSURE;
    }

    private boolean isLikelyMeleeThreat(LivingEntity target, double gap) {
        if (gap > 6.0D) {
            return false;
        }
        if (target instanceof Player player) {
            return player.getAttackStrengthScale(0.0f) > 0.85f;
        }
        return target.swingTime > 0;
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.wyvern.distanceTo(target);
        double combinedRadii = (this.wyvern.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMeleeStopRange(LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(target) ? biteOnlyPreyRange : goreRange;
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
            return;
        }

        if (pathFailureBackoff > 0) {
            pathFailureBackoff--;
            updateGroundMoveState();
            return;
        }
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean targetMoved = targetMovedSignificantly(target);
        boolean navigationIdle = wyvern.getAIMovement().hasArrived()
                || !wyvern.getAIMovement().isPathing();
        boolean shouldRepath = wyvern.getAIMovement().hasFailed()
                || pathRecalcCooldown <= 0
                || (targetMoved && navigationIdle);

        if (shouldRepath) {
            rememberTargetPosition(target);
            double distance = wyvern.distanceTo(target);
            int baseCooldown = Mth.clamp((int) (distance * 0.6D), 5, 20);
            boolean started = wyvern.getAIMovement().moveToGroundTarget(target, chaseSpeed, true);
            if (started) {
                pathRecalcCooldown = baseCooldown;
            } else {
                wyvern.getAIMovement().setGroundIdle();
                pathRecalcCooldown = Math.max(baseCooldown, 20);
                pathFailureBackoff = 20 + wyvern.getRandom().nextInt(21);
            }
        }
        updateGroundMoveState();
    }

    private void updateGroundMoveState() {
        if (!wyvern.isFlying() && wyvern.onGround() && wyvern.getAIMovement().isPathing()) {
            wyvern.getAIMovement().setGroundRun();
        } else {
            wyvern.getAIMovement().setGroundIdle();
        }
    }

    private void handleWaterCombat(LivingEntity target) {
        wyvern.getAIMovement().stop();

        boolean hasLineOfSight = wyvern.getSensing().hasLineOfSight(target);
        double gap = getGapToTarget(target);
        boolean inAttackRange = gap <= getMeleeStopRange(target);
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

        if (inAttackRange && hasLineOfSight) {
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

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonTargetingHelper.isTargetAirborne(target, 8.0D);
    }

    private boolean canUseAiAbility(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return wyvern.combatManager.canStart(abilityType) && wyvern.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        return wyvern.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
    }

    private enum Mode {
        PRESSURE,
        SPACE,
        EVADE,
        REPOSITION
    }
}
