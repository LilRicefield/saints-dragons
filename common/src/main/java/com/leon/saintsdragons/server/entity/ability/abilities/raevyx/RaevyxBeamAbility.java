package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModDamageTypes;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonRedstonePowerManager;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class RaevyxBeamAbility extends DragonAbility<Raevyx> {
    public static final int STARTUP_TICKS = 20;
    public static final float AI_BEAM_MERCY_HEALTH_FRACTION = RaevyxStatProfile.BeamAbility.AI_BEAM_MERCY_HEALTH_FRACTION;
    private static final double AI_TARGET_HIT_RADIUS = RaevyxStatProfile.BeamAbility.AI_TARGET_HIT_RADIUS;
    private static final double RIDER_BEAM_RADIUS = RaevyxStatProfile.BeamAbility.RIDER_BEAM_RADIUS;
    private static final double AI_BEAM_RADIUS = RaevyxStatProfile.BeamAbility.AI_BEAM_RADIUS;
    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 400)
    };
    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 80)
    };
    private static final float DEFAULT_BEAM_DAMAGE = RaevyxStatProfile.BeamAbility.DEFAULT_BEAM_DAMAGE;
    private static final float ENERGY_COST_PER_TICK = 0.014f;
    private boolean hasBeamFired = false;
    private boolean beamStartPlayed = false;
    private boolean beamLoopActive = false;
    private boolean aiControlled;
    private boolean groundAiBeam;
    private int aiBurstTicks;
    private final DragonCombatAim.ShotGrace shotGrace = new DragonCombatAim.ShotGrace();
    private boolean retreatUsed;
    private int retreatAimUnlockTick;
    private long learningTrial;
    private DragonCombatLearning.Outcome learningOutcome = DragonCombatLearning.Outcome.CANCELLED;
    private int learningFiringTicks;
    private final Set<BlockPos> energizedRedstoneWires = new HashSet<>();
    public RaevyxBeamAbility(DragonAbilityType<Raevyx, RaevyxBeamAbility> type, Raevyx user) {
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, 0);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            Raevyx wyvern = getUser();
            aiControlled = wyvern.getControllingPassenger() == null;
            groundAiBeam = aiControlled && !wyvern.isAerial();
            aiBurstTicks = 40 + wyvern.getRandom().nextInt(41);
            shotGrace.reset();
            retreatUsed = false;
            retreatAimUnlockTick = 0;
            learningTrial = 0;
            learningFiringTicks = 0;
            learningOutcome = DragonCombatLearning.Outcome.CANCELLED;
            if (aiControlled) wyvern.setAiBeamDecision("windup");
            if (!wyvern.canUseBeam()) {
                interrupt();
                return;
            }

            hasBeamFired = false;
            learningTrial = aiControlled && !wyvern.level().isClientSide
                    ? wyvern.getCombatLearning().beginAttack(DragonCombatLearning.Attack.BEAM,
                            wyvern.getTarget(), STARTUP_TICKS) : 0;
            beamLoopActive = false;
            beamStartPlayed = true;
            wyvern.setBeamGlowActive(true);
            wyvern.setBeaming(false);
            releaseEnergizedRedstone(wyvern);
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beam_start");
            if (!wyvern.level().isClientSide) {
                float pitch = 0.9f + wyvern.getRandom().nextFloat() * 0.2f;
                wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LIGHTNING_BEAM_START.get(), 1.8f, pitch, 40);
            }
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            Raevyx wyvern = getUser();
            if (!wyvern.level().isClientSide && aiControlled && !canContinueAiBeam(false)) {
                interrupt();
                return;
            }
            wyvern.setBeaming(true);
            if (learningTrial != 0) wyvern.getCombatLearning().releaseAttack(learningTrial);
            if (aiControlled) wyvern.setAiBeamDecision("firing");
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beaming");
            beamLoopActive = true;
            if (!hasBeamFired) {
                fireBeamOnce();
                hasBeamFired = true;
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == AbilitySectionType.ACTIVE) {
            Raevyx wyvern = getUser();
            if (aiControlled) stopAiBeam("burst-complete");
            wyvern.setBeaming(false);
            wyvern.setBeamGlowActive(false);
            wyvern.clearBeamPath();
            releaseEnergizedRedstone(wyvern);
            triggerBeamStop(wyvern);
            hasBeamFired = false;
        }
    }

    @Override
    public void interrupt() {
        Raevyx wyvern = getUser();
        wyvern.setBeaming(false);
        wyvern.setBeamGlowActive(false);
        wyvern.clearBeamPath();
        releaseEnergizedRedstone(wyvern);
        triggerBeamStop(wyvern);
        hasBeamFired = false;
        super.interrupt();
    }

    @Override
    public void end() {
        Raevyx wyvern = getUser();
        if (learningTrial != 0) {
            wyvern.getCombatLearning().finishAttack(learningTrial, learningOutcome);
            learningTrial = 0;
        }
        wyvern.getCombatAim().clear();
        if (isUsing() && aiControlled && !wyvern.level().isClientSide) {
            wyvern.finishAiBeam();
        }
        super.end();
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null) return;
        Raevyx wyvern = getUser();
        if (wyvern.level().isClientSide) return;
        boolean active = section.sectionType == AbilitySectionType.ACTIVE;
        if (aiControlled && !canContinueAiBeam(active)) {
            interrupt();
            return;
        }
        if (!active) return;
        float energyDrain = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("beam_drain_per_tick", ENERGY_COST_PER_TICK);
        energyDrain = Math.max(0.0f, energyDrain);
        if (energyDrain > 0.0f) {
            wyvern.consumeBeamEnergy(energyDrain);
        }
        if (!wyvern.hasBeamEnergy()) {
            wyvern.setBeamDepleted(true);
            if (aiControlled) wyvern.setAiBeamDecision("energy-depleted");
            interrupt();
            return;
        }

        BeamPath path = computeBeamPath(wyvern);
        if (path == null) {
            releaseEnergizedRedstone(wyvern);
            return;
        }
        learningFiringTicks++;
        damageAlongBeam(wyvern, path.origin(), path.impact());
    }

    private boolean canContinueAiBeam(boolean active) {
        Raevyx wyvern = getUser();
        LivingEntity target = wyvern.getTarget();
        if (wyvern.getControllingPassenger() != null || !isValidTarget(target)
                || !target.isAlive()) return stopAiBeam("target-lost");
        if (isAtAiBeamMercyThreshold(target)) return stopAiBeam("mercy-threshold");
        if (active && getTicksInSection() >= aiBurstTicks) return stopAiBeam("burst-complete");

        if (groundAiBeam) {
            if (wyvern.isAerial() || wyvern.isInWaterOrBubble()) return stopAiBeam("locomotion-changed");
            double gap = Math.max(0.0D, wyvern.distanceTo(target)
                    - (wyvern.getBbWidth() + target.getBbWidth()) * 0.5D);
            // Point-blank/under-body pressure wins immediately, including during the wind-up.
            double horizontalGap = Math.sqrt(wyvern.position().distanceToSqr(
                    new Vec3(target.getX(), wyvern.getY(), target.getZ())))
                    - (wyvern.getBbWidth() + target.getBbWidth()) * 0.5D;
            if (gap <= 4.5D || horizontalGap <= 1.0D) return stopAiBeam("close-melee");

            if (retreatAimUnlockTick > 0) {
                if (getTicksInSection() < retreatAimUnlockTick) return true;
                wyvern.clearAiBeamRetreat();
                wyvern.setAiBeamDecision("firing-after-retreat");
                retreatAimUnlockTick = 0;
            }
            if (active && gap <= 8.0D && getTicksInSection() >= 12) {
                if (retreatUsed) return stopAiBeam("retreat-spent");
                Vec3 aim = wyvern.getBeamAimDirection();
                if (aim == null) return stopAiBeam("aim-unavailable");
                Vec3 horizontalAim = new Vec3(aim.x, 0.0D, aim.z).normalize();
                Vec3 towardTarget = new Vec3(target.getX() - wyvern.getX(), 0.0D,
                        target.getZ() - wyvern.getZ()).normalize();
                if (horizontalAim.dot(towardTarget) < 0.5D) return stopAiBeam("flanked");
                // Fixed world-space aim during one short, grounded retreat. No added damage or i-frames.
                if (!wyvern.beginAiBeamRetreat(horizontalAim.scale(-6.0D), 10)) return stopAiBeam("retreat-blocked");
                wyvern.lockAiBeamDirection(aim);
                wyvern.setAiBeamDecision("retreat-locked-aim");
                retreatUsed = true;
                retreatAimUnlockTick = getTicksInSection() + 12;
                return true;
            }
        }

        DragonCombatAim.Shot shot = wyvern.getAiBeamShot(target, Raevyx.BEAM_RANGE);
        if (shot != DragonCombatAim.Shot.ALIGNED) {
            wyvern.setAiBeamDecision("tracking:" + shot.reason());
        }
        return shotGrace.allows(shot, wyvern.tickCount, wyvern.isAerial() ? 20 : 10, 10)
                || stopAiBeam(shot.reason());
    }

    private boolean stopAiBeam(String reason) {
        learningOutcome = switch (reason) {
            case "blocked", "retreat-blocked" -> DragonCombatLearning.Outcome.BLOCKED;
            case "close-melee", "flanked", "retreat-spent", "burst-complete", "aligning", "out_of_arc", "out_of_range"
                    -> learningFiringTicks >= 8 ? DragonCombatLearning.Outcome.COMPLETED
                    : DragonCombatLearning.Outcome.RESPONSE_ONLY;
            // Only recordHit confirms success; another attacker can also push the target below this threshold.
            case "mercy-threshold" -> DragonCombatLearning.Outcome.RESPONSE_ONLY;
            default -> DragonCombatLearning.Outcome.CANCELLED;
        };
        getUser().setAiBeamDecision(reason);
        return false;
    }

    private void triggerBeamStop(Raevyx wyvern) {
        if (beamLoopActive || beamStartPlayed) {
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beam_stop");
            if (!wyvern.level().isClientSide) {
                float pitch = 0.95f + wyvern.getRandom().nextFloat() * 0.15f;
                wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LIGHTNING_BEAM_STOP.get(), 1.6f, pitch, 50);
            }
        }
        beamLoopActive = false;
        beamStartPlayed = false;
    }

    private void fireBeamOnce() {
        Raevyx wyvern = getUser();
        BeamPath path = computeBeamPath(wyvern);
        if (path != null) {
            damageAlongBeam(wyvern, path.origin(), path.impact());
        }
    }
    
    private BeamPath computeBeamPath(Raevyx wyvern) {
        if (!wyvern.updateBeamPathFromAim()) {
            return null;
        }

        Vec3 origin = wyvern.getBeamStartPosition();
        Vec3 impact = wyvern.getBeamEndPosition();
        if (origin == null || impact == null) {
            return null;
        }
        return new BeamPath(origin, impact);
    }

    private void damageAlongBeam(Raevyx wyvern, Vec3 start, Vec3 end) {
        if (!(wyvern.level() instanceof ServerLevel server)) return;

        boolean riderControlled = wyvern.getControllingPassenger() != null;
        final float configuredBaseDamage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("lightning_beam", DEFAULT_BEAM_DAMAGE);
        final double radiusBase = riderControlled ? RIDER_BEAM_RADIUS : AI_BEAM_RADIUS;
        final double RADIUS = radiusBase;
        final float DAMAGE = configuredBaseDamage * wyvern.getDamageMultiplier();

        Set<BlockPos> currentWires = DragonUtilities.applyLightningBeamImpact(server, start, end, RADIUS);
        Set<BlockPos> releasedWires = new HashSet<>(energizedRedstoneWires);
        releasedWires.removeAll(currentWires);
        Set<BlockPos> affectedWires = new HashSet<>(currentWires);
        affectedWires.addAll(releasedWires);
        DragonRedstonePowerManager.update(server, wyvern.getUUID(), currentWires);
        DragonUtilities.refreshLightningBeamRedstone(server, affectedWires);

        energizedRedstoneWires.clear();
        energizedRedstoneWires.addAll(currentWires);

        if (!riderControlled) {
            damageAiBeamTargetOnly(wyvern, start, end, DAMAGE, RADIUS);
            return;
        }

        var beamAABB = new AABB(start, end).inflate(RADIUS);
        var potentialTargets = server.getEntitiesOfClass(LivingEntity.class, beamAABB, e -> e != wyvern
                && wyvern.isTargetValid(e)
                && e.attackable()
                && !isAllied(wyvern, e)
                && !DragonElementalImmunity.isElectricityImmune(e));
        for (var target : potentialTargets) {
            var targetAABB = target.getBoundingBox().inflate(RADIUS);
            var hit = targetAABB.clip(start, end);
            boolean pointBlankOverlap = targetAABB.contains(start) || targetAABB.contains(end);

            if (hit.isPresent() || pointBlankOverlap) {
                var hitPos = hit.orElse(start);
                if (!target.hurt(resolveBeamDamageSource(wyvern, target), DAMAGE)) {
                    continue;
                }
                var away = target.position().subtract(hitPos).normalize();
                target.push(away.x * 0.15, 0.08, away.z * 0.15);
            }
        }
    }

    private void damageAiBeamTargetOnly(Raevyx wyvern, Vec3 start, Vec3 end, float damage, double radius) {
        LivingEntity target = wyvern.getTarget();
        if (!isValidTarget(target)
                || !wyvern.isTargetValid(target)
                || isAllied(wyvern, target)) {
            return;
        }
        if (DragonElementalImmunity.isElectricityImmune(target)) {
            wyvern.getCombatLearning().recordContact(learningTrial);
            return;
        }

        var targetAABB = target.getBoundingBox().inflate(Math.min(radius, AI_TARGET_HIT_RADIUS));
        var hit = targetAABB.clip(start, end);
        boolean pointBlankOverlap = targetAABB.contains(start) || targetAABB.contains(end);
        if (hit.isEmpty() && !pointBlankOverlap) {
            return;
        }

        var hitPos = hit.orElse(start);
        float mercyFloor = target.getMaxHealth() * AI_BEAM_MERCY_HEALTH_FRACTION;
        float allowedDamage = Math.max(0.0F, target.getHealth() - mercyFloor);
        if (allowedDamage <= 0.0F) {
            stopAiBeam("mercy-threshold");
            interrupt();
            return;
        }
        if (!target.hurt(resolveBeamDamageSource(wyvern, target), Math.min(damage, allowedDamage))) {
            wyvern.getCombatLearning().recordContact(learningTrial);
            return;
        }
        wyvern.getCombatLearning().recordHit(learningTrial, target);
        if (isAtAiBeamMercyThreshold(target)) {
            stopAiBeam("mercy-threshold");
            interrupt();
        }
        var away = target.position().subtract(hitPos).normalize();
        target.push(away.x * 0.15, 0.08, away.z * 0.15);
    }

    private boolean isAllied(Raevyx wyvern, Entity other) {
        return wyvern.isAlly(other);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (!getUser().isTargetValid(target)) return false;
        if (target.isRemoved()) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }

    private void releaseEnergizedRedstone(Raevyx wyvern) {
        if (wyvern.level() instanceof ServerLevel server && !energizedRedstoneWires.isEmpty()) {
            DragonRedstonePowerManager.clear(server, wyvern.getUUID());
            DragonUtilities.refreshLightningBeamRedstone(server, energizedRedstoneWires);
            energizedRedstoneWires.clear();
        } else if (wyvern.level() instanceof ServerLevel server) {
            DragonRedstonePowerManager.clear(server, wyvern.getUUID());
        }
    }

    public static boolean isAtAiBeamMercyThreshold(LivingEntity target) {
        return target != null
                && target.getHealth() <= target.getMaxHealth() * AI_BEAM_MERCY_HEALTH_FRACTION;
    }

    private DamageSource resolveBeamDamageSource(Raevyx wyvern, LivingEntity target) {
        if (target instanceof DragonEntity) {
            return wyvern.level().damageSources().mobAttack(wyvern);
        }
        if (target.isBlocking()) {
            return wyvern.level().damageSources().mobProjectile(wyvern, wyvern);
        }
        return ModDamageTypes.raevyxLightning(wyvern);
    }

    private record BeamPath(Vec3 origin, Vec3 impact) {}
}
