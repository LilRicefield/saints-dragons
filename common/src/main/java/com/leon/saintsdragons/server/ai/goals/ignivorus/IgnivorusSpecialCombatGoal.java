package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Scripted untamed combat sequence:
 * 1) Takeoff
 * 2) Ascend to a hover anchor
 * 3) Hover-cast (fire breath or charged fireball)
 * 4) Land back down
 */
public class IgnivorusSpecialCombatGoal extends Goal {
    private static final double MIN_START_DISTANCE = 12.0D;
    private static final double MAX_START_DISTANCE = 72.0D;
    private static final double HOVER_HEIGHT_MIN = 12.0D;
    private static final double HOVER_HEIGHT_MAX = 18.0D;
    private static final double HOVER_LATERAL = 12.0D;
    private static final double HOVER_HOLD_RADIUS_SQR = 11.0D * 11.0D;
    private static final double HOVER_REPOSITION_RADIUS_SQR = 22.0D * 22.0D;
    private static final double TARGET_DRIFT_REANCHOR_SQR = 18.0D * 18.0D;

    private static final int TAKEOFF_MAX_TICKS = Ignivorus.TAKEOFF_ANIMATION_TICKS + 30;
    private static final int ASCEND_MAX_TICKS = 120;
    private static final int HOVER_MAX_TICKS = 130;
    private static final int LAND_MAX_TICKS = 120;
    private static final int FIREBALL_RELEASE_TICKS = 58;
    private static final int HOVER_DAMAGE_WINDOW_RESET_TICKS = 40;
    private static final int HOVER_DAMAGE_FORCE_LAND_HITS = 3;

    private int specialCooldownTicks = 0;
    private Phase phase = Phase.IDLE;
    private int phaseTicks = 0;
    private long lastHoverDamageTick = -1L;
    private int hoverDamageHits = 0;

    private Vec3 hoverAnchor = null;
    private boolean castWithFireball = false;
    private boolean castStarted = false;
    private boolean fireballReleaseRequested = false;
    private boolean fireBreathStarted = false;

    private final Ignivorus dragon;

    private enum Phase {
        IDLE,
        TAKEOFF,
        ASCEND,
        HOVER_CAST,
        LAND
    }

    public IgnivorusSpecialCombatGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (specialCooldownTicks > 0) {
            specialCooldownTicks--;
            return false;
        }
        if (dragon.isBaby() || dragon.isTame()) {
            return false;
        }
        if (dragon.isVehicle() || dragon.getControllingPassenger() != null || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.areRiderControlsLocked()) {
            return false;
        }
        if (dragon.isLeaping()) {
            return false;
        }
        if (dragon.isInWaterOrBubble() || dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.getActiveAbility() != null || dragon.isBreathingFire()) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target) || target == null) {
            return false;
        }

        if (target instanceof net.minecraft.world.entity.player.Player player
                && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        // Special sequence is for punishing grounded targets from above.
        if (!isTargetGrounded(target)) {
            return false;
        }

        double distSq = dragon.distanceToSqr(target);
        if (distSq < MIN_START_DISTANCE * MIN_START_DISTANCE || distSq > MAX_START_DISTANCE * MAX_START_DISTANCE) {
            return false;
        }

        // Keep usage sparse for tick budget and combat readability.
        return dragon.getRandom().nextFloat() < 0.06f;
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == Phase.IDLE) {
            return false;
        }
        if (dragon.isDeadOrDying() || dragon.isRemoved()) {
            return false;
        }
        if (dragon.isInWaterOrBubble()) {
            return false;
        }
        if (dragon.isLeaping()) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        if (phase != Phase.LAND && !dragon.isTargetValid(target)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        dragon.setAiSpecialCombatActive(true);
        dragon.setAggressive(true);
        dragon.getNavigation().stop();

        castWithFireball = dragon.getRandom().nextFloat() < 0.35f;
        castStarted = false;
        fireballReleaseRequested = false;
        fireBreathStarted = false;
        lastHoverDamageTick = -1L;
        hoverDamageHits = 0;
        hoverAnchor = null;
        transitionTo(Phase.TAKEOFF);
    }

    @Override
    public void stop() {
        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
            dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
        }
        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)) {
            dragon.requestFireballReleaseForAI();
            dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL);
        }

        // If we stop mid-air, push a clean landing handoff.
        if (!dragon.onGround()) {
            dragon.setTakeoff(false);
            dragon.setHovering(false);
            dragon.setLanding(true);
            dragon.setFlying(false);
        }

        dragon.setAiSpecialCombatActive(false);
        hoverAnchor = null;
        phase = Phase.IDLE;
        phaseTicks = 0;
        specialCooldownTicks = 180 + dragon.getRandom().nextInt(81); // 9-13s
    }

    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        phaseTicks++;

        if (target != null) {
            dragon.getLookControl().setLookAt(target, 35.0f, 35.0f);
        }

        switch (phase) {
            case TAKEOFF -> tickTakeoff(target);
            case ASCEND -> tickAscend(target);
            case HOVER_CAST -> tickHoverCast(target);
            case LAND -> tickLanding(target);
            default -> {
            }
        }
    }

    private void tickTakeoff(LivingEntity target) {
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setFlying(true);
        if (dragon.onGround() && !dragon.isTakeoff()) {
            dragon.setTakeoff(true);
        }

        if (hoverAnchor == null && target != null) {
            hoverAnchor = chooseHoverAnchor(target);
        }

        double targetY = hoverAnchor != null ? Math.max(dragon.getY() + 6.0D, hoverAnchor.y - 3.0D) : dragon.getY() + 8.0D;
        dragon.getMoveControl().setWantedPosition(dragon.getX(), targetY, dragon.getZ(), 1.3D);

        if (!dragon.onGround() && phaseTicks > Ignivorus.TAKEOFF_ANIMATION_TICKS + 4) {
            dragon.setTakeoff(false);
            transitionTo(Phase.ASCEND);
            return;
        }

        if (phaseTicks > TAKEOFF_MAX_TICKS) {
            dragon.setTakeoff(false);
            transitionTo(Phase.ASCEND);
        }
    }

    private void tickAscend(LivingEntity target) {
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setFlying(true);

        if (target != null && hoverAnchor == null) {
            hoverAnchor = chooseHoverAnchor(target);
        }

        if (hoverAnchor == null) {
            transitionTo(Phase.LAND);
            return;
        }

        if (target != null && target.position().distanceToSqr(hoverAnchor.x, target.getY(), hoverAnchor.z) > TARGET_DRIFT_REANCHOR_SQR) {
            hoverAnchor = chooseHoverAnchor(target);
        }

        dragon.getMoveControl().setWantedPosition(hoverAnchor.x, hoverAnchor.y, hoverAnchor.z, 1.4D);
        double distSq = dragon.distanceToSqr(hoverAnchor.x, hoverAnchor.y, hoverAnchor.z);
        if (distSq <= 16.0D || phaseTicks > ASCEND_MAX_TICKS) {
            transitionTo(Phase.HOVER_CAST);
        }
    }

    private void tickHoverCast(LivingEntity target) {
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setFlying(true);

        if (target != null && hoverAnchor == null) {
            hoverAnchor = chooseHoverAnchor(target);
        }
        if (hoverAnchor != null) {
            double anchorDistSq = dragon.distanceToSqr(hoverAnchor.x, hoverAnchor.y, hoverAnchor.z);
            if (anchorDistSq <= HOVER_HOLD_RADIUS_SQR) {
                // Wide hold band: stop re-aligning and commit to casting.
                dragon.getMoveControl().setWantedPosition(dragon.getX(), dragon.getY(), dragon.getZ(), 0.25D);
            } else if (anchorDistSq <= HOVER_REPOSITION_RADIUS_SQR) {
                // Gentle correction only when drifted.
                dragon.getMoveControl().setWantedPosition(hoverAnchor.x, hoverAnchor.y, hoverAnchor.z, 0.6D);
            } else {
                // Far from anchor (e.g. knockback): recover anchor quickly.
                dragon.getMoveControl().setWantedPosition(hoverAnchor.x, hoverAnchor.y, hoverAnchor.z, 0.95D);
            }
        } else {
            dragon.getMoveControl().setWantedPosition(dragon.getX(), dragon.getY(), dragon.getZ(), 0.5D);
        }

        // While casting, sustained ranged pressure can force a disengage.
        if (tickHoverDamagePressure()) {
            transitionTo(Phase.LAND);
            return;
        }

        if (!castStarted) {
            castStarted = true;
            if (castWithFireball) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL);
            } else {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
                fireBreathStarted = dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            }
        }

        if (castWithFireball) {
            if (!fireballReleaseRequested
                    && phaseTicks >= FIREBALL_RELEASE_TICKS
                    && dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)) {
                dragon.requestFireballReleaseForAI(); // requests release at level 3 hold window
                fireballReleaseRequested = true;
            }
            if (fireballReleaseRequested && !dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)) {
                transitionTo(Phase.LAND);
                return;
            }
        } else {
            // Retry breath start once if it failed early due to temporary state.
            if (!fireBreathStarted && phaseTicks == 20) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
                fireBreathStarted = dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            }
            if (phaseTicks >= 80) {
                dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
            }
        }

        if (phaseTicks > HOVER_MAX_TICKS) {
            transitionTo(Phase.LAND);
        }
    }

    private void tickLanding(LivingEntity target) {
        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
            dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
        }

        dragon.setTakeoff(false);
        dragon.setHovering(false);
        dragon.setLanding(true);
        dragon.setFlying(false);

        if (target != null) {
            BlockPos ground = dragon.level().getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    target.blockPosition()
            );
            dragon.getMoveControl().setWantedPosition(target.getX(), ground.getY() + 1.0D, target.getZ(), 0.8D);
        }

        if (dragon.onGround() || phaseTicks > LAND_MAX_TICKS) {
            dragon.setLanding(false);
            phase = Phase.IDLE;
        }
    }

    private Vec3 chooseHoverAnchor(LivingEntity target) {
        double height = HOVER_HEIGHT_MIN + dragon.getRandom().nextDouble() * (HOVER_HEIGHT_MAX - HOVER_HEIGHT_MIN);
        double lateralX = (dragon.getRandom().nextDouble() * 2.0D - 1.0D) * HOVER_LATERAL;
        double lateralZ = (dragon.getRandom().nextDouble() * 2.0D - 1.0D) * HOVER_LATERAL;

        double x = target.getX() + lateralX;
        double z = target.getZ() + lateralZ;
        double y = Math.max(target.getY() + height, dragon.getY() + 7.0D);
        y = Mth.clamp(y, dragon.level().getMinBuildHeight() + 8.0D, dragon.level().getMaxBuildHeight() - 6.0D);
        return new Vec3(x, y, z);
    }

    private boolean isTargetGrounded(LivingEntity target) {
        if (target.onGround()) {
            return true;
        }
        double groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        return target.getY() - groundY < 3.0D;
    }

    private void transitionTo(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private boolean tickHoverDamagePressure() {
        long now = dragon.level().getGameTime();
        if (lastHoverDamageTick > 0 && now - lastHoverDamageTick > HOVER_DAMAGE_WINDOW_RESET_TICKS) {
            hoverDamageHits = 0;
        }

        if (dragon.hurtTime > 0 && now != lastHoverDamageTick) {
            lastHoverDamageTick = now;
            hoverDamageHits++;
            if (hoverDamageHits >= HOVER_DAMAGE_FORCE_LAND_HITS) {
                // Do not hard-cancel immediately on single arrow; require sustained hits.
                if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
                    dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
                }
                if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)) {
                    dragon.requestFireballReleaseForAI();
                }
                return true;
            }
        }
        return false;
    }
}
