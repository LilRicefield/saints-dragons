package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.particle.VolitansBreathMotion;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class VolitansBreathCombatComponent {
    public static final double FIRING_RANGE = VolitansBreathMotion.RANGE * 0.9D;
    private static final double START_GAP = 7.0D;
    private static final double STOP_GAP = 4.5D;
    private final Volitans dragon;
    private final DragonCombatAim.ShotGrace shotGrace = new DragonCombatAim.ShotGrace();
    private int lastMode = -1;
    private int waterBursts;
    private int burstTicks;
    private boolean extended;
    private boolean running;
    private long nextDecision;
    private long recoveryUntil;
    private String decision = "idle";
    private Vec3 passDestination;
    private Vec3 passDirection;
    private boolean passInAir;
    private long passUntil;
    private int side = 1;
    private long learningTrial;
    private int firingOpportunityTicks;

    public VolitansBreathCombatComponent(Volitans dragon) {
        this.dragon = dragon;
    }

    public boolean ready(LivingEntity target) {
        return canFight(target) && !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                && !dragon.isGroundMobilityActive() && !dragon.isBurrowing()
                && !dragon.isTakeoff() && !dragon.isLanding()
                && dragon.level().getGameTime() >= recoveryUntil
                && dragon.canUseCurrentBreathMode() && dragon.getWaterBreathEnergy() >= 0.25F
                && dragon.combatManager.canStart(ModAbilities.VOLITANS_BREATH)
                && dragon.getAiCombatPacing().canUse(ModAbilities.VOLITANS_BREATH, true);
    }

    public boolean tryStart(LivingEntity target) {
        if (!ready(target) || gap(target) < START_GAP
                || dragon.getBreathOrigin().distanceToSqr(target.getBoundingBox().getCenter()) > FIRING_RANGE * FIRING_RANGE) {
            return false;
        }
        // Keep tracking between decisions so a turning target cannot permanently stall alignment.
        DragonCombatAim.Shot shot = dragon.getAiBreathShot(target);
        long now = dragon.level().getGameTime();
        if (shot != DragonCombatAim.Shot.ALIGNED || now < nextDecision) return false;
        nextDecision = now + 8;
        int mode = chooseMode(target);
        int previousMode = dragon.getBreathMode();
        dragon.setBreathMode(mode);
        if (!dragon.combatManager.tryUseAiAbility(ModAbilities.VOLITANS_BREATH, true, 16, 0, 20, 0)) {
            dragon.setBreathMode(previousMode);
            return false;
        }
        return true;
    }

    private int chooseMode(LivingEntity target) {
        MobEffectInstance poison = target.getEffect(MobEffects.POISON);
        int amplifier = dragon.getConfiguredPoisonAmplifier("poison_breath_poison_level", 1);
        int duration = Math.max(0, (int) Math.round(dragon.getConfiguredExtra("poison_breath_poison_duration_ticks", 80)));
        boolean poisonUseful = !dragon.isVenomNeutralized() && !DragonElementalImmunity.isPoisonImmune(target)
                && duration > 0 && target.canBeAffected(new MobEffectInstance(MobEffects.POISON, duration, amplifier))
                && (poison == null || poison.getDuration() <= 40 || poison.getAmplifier() < amplifier);
        if (!poisonUseful || lastMode == 1) return 0;
        if (waterBursts >= 2) return 1;
        return dragon.getRandom().nextFloat() < (lastMode == 0 ? 0.75F : 0.5F) ? 1 : 0;
    }

    public void begin() {
        running = true;
        firingOpportunityTicks = 0;
        LivingEntity target = dragon.getTarget();
        learningTrial = target != null ? dragon.getCombatLearning().beginAttack(
                DragonCombatLearning.Attack.BREATH, target, 17) : 0;
        burstTicks = 40 + dragon.getRandom().nextInt(41);
        extended = false;
        shotGrace.reset();
        passDestination = null;
        passDirection = null;
        side = dragon.getRandom().nextBoolean() ? 1 : -1;
        decision = "startup:" + modeName(dragon.getBreathMode());
    }

    public void startedBreathing() {
        dragon.getCombatLearning().releaseAttack(learningTrial);
        lastMode = dragon.getBreathMode();
        waterBursts = lastMode == 0 ? Math.min(2, waterBursts + 1) : 0;
        decision = "breathing:" + modeName(lastMode);
    }

    public boolean continueBreath(LivingEntity target, boolean active, int activeTicks) {
        if (!canFight(target) || dragon.isTakeoff() || dragon.isLanding()) return stop("combat-interrupted");
        if (gap(target) < STOP_GAP) return stop("melee-range");
        Vec3 offset = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter());
        double horizontalGap = offset.horizontalDistance() - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        if (dragon.isAerial() && !dragon.isInWaterOrBubble() && horizontalGap < 3 && offset.y < -3) {
            return stop("target-underneath");
        }
        DragonCombatAim.Shot shot = dragon.getAiBreathShot(target);
        if (!shotGrace.allows(shot, dragon.tickCount, 20, 12)) return stop("shot:" + shot.name().toLowerCase(java.util.Locale.ROOT));
        if (active && shot == DragonCombatAim.Shot.ALIGNED) firingOpportunityTicks++;
        if (active && activeTicks >= burstTicks) {
            if (!extended && shot == DragonCombatAim.Shot.ALIGNED && gap(target) >= 10
                    && dragon.getWaterBreathEnergy() >= 0.4F
                    && dragon.getCombatFlightState().targetVelocity().lengthSqr() < 0.04D
                    && dragon.getRandom().nextFloat() < 0.4F) {
                burstTicks += 20;
                extended = true;
            } else {
                return stop("burst-complete");
            }
        }
        return true;
    }

    public void end() {
        if (!running) return;
        running = false;
        DragonCombatLearning.Outcome outcome = switch (decision) {
            case "shot:blocked" -> DragonCombatLearning.Outcome.BLOCKED;
            case "melee-range", "target-underneath", "burst-complete", "shot:aligning", "shot:out_of_arc", "shot:out_of_range" ->
                    firingOpportunityTicks >= 10 ? DragonCombatLearning.Outcome.COMPLETED : DragonCombatLearning.Outcome.RESPONSE_ONLY;
            default -> DragonCombatLearning.Outcome.CANCELLED;
        };
        dragon.getCombatLearning().deferAttackResult(learningTrial, outcome, VolitansBreathMotion.LIFETIME + 2);
        learningTrial = 0;
        int recovery = 40 + dragon.getRandom().nextInt(21);
        recoveryUntil = dragon.level().getGameTime() + recovery;
        dragon.getAiCombatPacing().recordUse(ModAbilities.VOLITANS_BREATH, 12, recovery, true, 20, 0);
        if (decision.startsWith("breathing:") || decision.startsWith("startup:")) decision = "breath-ended";
        passDestination = null;
        passUntil = dragon.level().getGameTime() + 24;
    }

    public long learningTrial() { return learningTrial; }

    public boolean makingSpace() {
        return !running && passDirection != null && dragon.level().getGameTime() < passUntil;
    }

    public Vec3 movingDestination(LivingEntity target, boolean air) {
        Vec3 center = DragonTargetingHelper.movementAnchor(target).getBoundingBox().getCenter();
        Vec3 feet = center.add(0, -dragon.getBbHeight() * 0.5D, 0);
        if (passInAir != air) {
            passInAir = air;
            passDestination = null;
            passDirection = null;
        }
        if (running || makingSpace()) {
            if (passDestination == null) {
                Vec3 heading = passDirection != null ? passDirection : horizontal(center.subtract(dragon.position()));
                passDirection = heading;
                Vec3 lateral = new Vec3(-heading.z, 0, heading.x).scale(side);
                passDestination = running ? feet.add(heading.scale(14)).add(lateral.scale(8))
                        : dragon.position().add(heading.scale(18)).add(lateral.scale(6));
                if (!air) passDestination = passDestination.add(0, dragon.getBbHeight() * 0.5D, 0);
            }
            // Commit to a course through the burst; aiming can turn independently within its neck limits.
            return passDestination;
        }
        Vec3 lead = dragon.getCombatFlightState().targetVelocity().scale(4);
        if (lead.lengthSqr() > 36) lead = lead.normalize().scale(6);
        Vec3 heading = horizontal(center.subtract(dragon.getBoundingBox().getCenter()));
        Vec3 lateral = new Vec3(-heading.z, 0, heading.x).scale(side * 4);
        return (air ? feet : center).add(lead).subtract(heading.scale(18)).add(lateral);
    }

    public double groundStopDistance(LivingEntity target) {
        double radii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return ready(target) && dragon.getAiBreathShot(target) == DragonCombatAim.Shot.ALIGNED
                ? 18 + radii + dragon.getCombatLearning().expectation(target,
                DragonCombatLearning.Attack.BREATH, false).spacingBonus() * 0.5D : 3.5D + radii;
    }

    public String debugSummary() {
        return decision + ",last=" + modeName(lastMode) + ",recovery="
                + Math.max(0, recoveryUntil - dragon.level().getGameTime());
    }

    private boolean canFight(LivingEntity target) {
        double range = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        return dragon.isTargetValid(target) && target.level() == dragon.level()
                && !dragon.isVehicle() && !dragon.isPassenger() && !dragon.areRiderControlsLocked()
                && !dragon.isOrderedToSit() && !dragon.isBaby() && !dragon.isSleepLocked()
                && !dragon.isAiSpecialCombatActive() && !dragon.isAiSpecialCombatReserved()
                && dragon.distanceToSqr(target) <= range * range;
    }

    private double gap(LivingEntity target) {
        return Math.max(0, dragon.distanceTo(target) - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D);
    }

    private boolean stop(String reason) {
        decision = reason;
        return false;
    }

    private Vec3 horizontal(Vec3 direction) {
        Vec3 flat = direction.multiply(1, 0, 1);
        return flat.lengthSqr() < 1.0E-6D ? Vec3.directionFromRotation(0, dragon.getYRot()) : flat.normalize();
    }

    private static String modeName(int mode) {
        return mode < 0 ? "none" : mode == 0 ? "water" : "poison";
    }
}
