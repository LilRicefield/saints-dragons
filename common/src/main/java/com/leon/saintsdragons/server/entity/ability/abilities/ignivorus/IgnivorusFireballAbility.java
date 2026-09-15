package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFireballEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class IgnivorusFireballAbility extends DragonAbility<Ignivorus> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 20;
    private static final int MAGMA_LIFETIME_TICKS = 200;
    private static final double FIREBALL_SPEED = 5.0D;
    private static final int FIRE_RELEASE_TICKS = 15;
    private static final int AI_ALIGNMENT_TICKS = 30;
    private static final DragonCombatAim.Profile AIR_AIM = new DragonCombatAim.Profile(70, 55, 3, 0);
    private static final int CHARGE_LEVEL_2_TICKS = 25;
    private static final int CHARGE_LEVEL_3_TICKS = 50;
    private static final int MAX_CHARGE_TICKS = 95;
    private static final int MIN_CHARGE_DISPLAY_TICKS = 3;
    private static final float BASE_SCALE = 4.0F;
    private static final double BASE_IMPACT_RADIUS = 8.0D;
    private static final float DEFAULT_IMPACT_DAMAGE = 70.0F;
    private static final float LEVEL_2_MULTIPLIER = 1.5F;
    private static final float LEVEL_3_MULTIPLIER = 2.0F;
    private int chargeTicks = 0;
    private int lastChargeAnimLevel = 0;
    private boolean hasFired = false;
    private boolean releaseRequested = false;
    private int releaseTicks = 0;
    private int releaseChargeTicks = 0;
    private boolean aiAirCast;
    private boolean aiControlled;
    private long learningTrial;
    private int aiReleaseTicks;
    private int aiLostSightTicks;
    private boolean aiReleasePending;
    private int aiAlignmentTicks;
    private int aiAimTick = Integer.MIN_VALUE;
    private String aiAimFailure = "no-visible-observation";
    private DragonCombatAim.Shot aiShot = DragonCombatAim.Shot.NO_TARGET;
    private @Nullable AiSolution aiSolution;
    private @Nullable LivingEntity aiTarget;

    public IgnivorusFireballAbility(DragonAbilityType<Ignivorus, IgnivorusFireballAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        boolean airborne = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
        boolean hasRider = dragon.getControllingPassenger() != null;
        boolean aiUse = !dragon.isVehicle() && dragon.getTarget() != null;
        return hasRider || ((dragon.isPhase2Active() || airborne) && aiUse);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == ACTIVE) {
            Ignivorus dragon = getUser();
            aiControlled = !dragon.isVehicle();
            learningTrial = aiControlled && !dragon.level().isClientSide
                    ? dragon.getCombatLearning().beginAttack(DragonCombatLearning.Attack.PROJECTILE,
                            dragon.getTarget(), 20) : 0;
            if (aiControlled) dragon.setAiFireballDecision("windup");
            aiAirCast = dragon.getControllingPassenger() == null && dragon.isAerial();
            aiReleaseTicks = aiAirCast ? 25 + dragon.getRandom().nextInt(21) : 0;
            aiLostSightTicks = 0;
            aiReleasePending = false;
            aiAlignmentTicks = 0;
            aiAimTick = Integer.MIN_VALUE;
            aiSolution = null;
            aiShot = DragonCombatAim.Shot.NO_TARGET;
            aiTarget = aiControlled ? dragon.getTarget() : null;
            // Start charging
            chargeTicks = 0;
            hasFired = false;
            releaseRequested = false;
            releaseTicks = 0;
            releaseChargeTicks = 0;
            lastChargeAnimLevel = 0;
            getUser().setFireballChargeLevel(0);
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Ignivorus dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved() && !dragon.isInWaterOrBubble();
    }

    @Override
    public void tickUsing() {
        if (hasFired) {
            return;
        }

        if (aiControlled && !getUser().level().isClientSide) {
            Ignivorus dragon = getUser();
            if (dragon.isVehicle() || aiTarget == null || aiTarget != dragon.getTarget()
                    || !aiTarget.isAlive() || !dragon.isTargetValid(aiTarget)) {
                cancelAiCast("target-changed");
                return;
            }
            updateAiAim();
        }

        if (aiAirCast && !getUser().level().isClientSide) {
            Ignivorus dragon = getUser();
            var target = dragon.getTarget();
            if (dragon.getControllingPassenger() != null || !dragon.isAerial()
                    || target == null || !target.isAlive() || !dragon.isTargetValid(target)) {
                cancelAiCast("flight-ended");
                return;
            }
            double gap = dragon.distanceTo(target) - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
            aiLostSightTicks = dragon.getSensing().hasLineOfSight(target) ? 0 : aiLostSightTicks + 1;
            if (gap <= 6.0D || aiLostSightTicks >= 12) {
                cancelAiCast(gap <= 6.0D ? "close-melee" : "lost-sight");
                return;
            }
            // Ground AI and riders retain their own release controls.
            if (!releaseRequested && chargeTicks >= aiReleaseTicks) requestRelease();
        }

        if (aiReleasePending && !getUser().level().isClientSide) {
            aiAlignmentTicks++;
            if (aiShot == DragonCombatAim.Shot.ALIGNED && getUser().getCombatAim().ready(2)
                    && aiSolution != null) {
                if (clearTrajectory(getUser(), getFireballOrigin(getUser()), aiSolution.direction(),
                        aiSolution.travelTicks(), BASE_SCALE * getChargeMultiplier(releaseChargeTicks))) {
                    beginRelease();
                } else {
                    cancelAiCast("trajectory-blocked");
                    return;
                }
            } else {
                String reason = aiSolution == null ? aiAimFailure : aiShot.reason();
                getUser().setAiFireballDecision("aligning:" + reason + ":" + aiAlignmentTicks);
                if (aiAlignmentTicks >= AI_ALIGNMENT_TICKS) {
                    cancelAiCast("alignment-timeout:" + reason);
                    return;
                }
            }
        }

        if (releaseRequested) {
            releaseTicks++;
            if (!getUser().level().isClientSide && getChargeLevel(releaseChargeTicks) == 3
                    && releaseTicks == FIRE_RELEASE_TICKS - 2) {
                getUser().level().broadcastEntityEvent(getUser(), Ignivorus.FIREBALL_PRE_SHOT_STAR_EVENT);
            }
            if (releaseTicks >= FIRE_RELEASE_TICKS) {
                fireFireball(releaseChargeTicks);
                hasFired = true;
                end();
            }
            return;
        }
        if (!aiReleasePending && chargeTicks < MAX_CHARGE_TICKS) {
            chargeTicks++;
        }
        int currentChargeLevel = getChargeLevel(chargeTicks);

        boolean displayCharge = chargeTicks >= MIN_CHARGE_DISPLAY_TICKS;
        getUser().setFireballChargeLevel(displayCharge ? currentChargeLevel : 0);

        if (displayCharge && currentChargeLevel > lastChargeAnimLevel) {
            triggerChargeAnimation(currentChargeLevel);
            lastChargeAnimLevel = currentChargeLevel;
        }

    }

    @Override
    public void interrupt() {
        resetChargeState();
        super.interrupt();
    }

    @Override
    public void end() {
        if (aiControlled && ("windup".equals(getUser().getAiFireballDecision())
                || getUser().getAiFireballDecision().startsWith("release:")
                || getUser().getAiFireballDecision().startsWith("aligning:"))) {
            getUser().setAiFireballDecision("cancelled");
        }
        if (learningTrial != 0) {
            getUser().getCombatLearning().finishAttack(learningTrial, DragonCombatLearning.Outcome.CANCELLED);
            learningTrial = 0;
        }
        if (aiControlled) getUser().getCombatAim().clear();
        resetChargeState();
        super.end();
    }

    public void requestRelease() {
        if (hasFired || releaseRequested || aiReleasePending) {
            return;
        }
        releaseChargeTicks = Math.max(1, chargeTicks);
        if (aiControlled && !getUser().level().isClientSide) {
            aiReleasePending = true;
            return;
        }
        beginRelease();
    }

    private void beginRelease() {
        aiReleasePending = false;
        releaseRequested = true;
        releaseTicks = 0;
        if (aiControlled) getUser().setAiFireballDecision("release:stage-" + getChargeLevel(releaseChargeTicks));
        getUser().setFireballChargeLevel(0);
        triggerShootAnimation(getChargeLevel(releaseChargeTicks));
    }

    private int getChargeLevel(int ticks) {
        if (ticks >= CHARGE_LEVEL_3_TICKS) {
            return 3;
        } else if (ticks >= CHARGE_LEVEL_2_TICKS) {
            return 2;
        } else {
            return 1;
        }
    }

    private float getChargeMultiplier(int ticks) {
        int level = getChargeLevel(ticks);
        return switch (level) {
            case 3 -> LEVEL_3_MULTIPLIER;
            case 2 -> LEVEL_2_MULTIPLIER;
            default -> 1.0F;
        };
    }

    private void fireFireball(int chargeAtRelease) {
        Ignivorus dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 spawnPos = getFireballOrigin(dragon);

        float multiplier = getChargeMultiplier(chargeAtRelease);
        float damage = resolveImpactDamage() * multiplier;
        double radius = BASE_IMPACT_RADIUS * multiplier;
        float scale = BASE_SCALE * multiplier;
        AiLaunch launch = aiControlled ? predictAiLaunch(dragon, spawnPos, scale) : null;
        if (aiControlled && launch == null) return;
        Vec3 direction = aiControlled ? launch.direction() : getAimDirection(dragon);

        IgnivorusFireballEntity fireball = new IgnivorusFireballEntity(server, spawnPos, dragon,
                radius, damage, MAGMA_LIFETIME_TICKS);
        fireball.setDeltaMovement(direction.scale(FIREBALL_SPEED));
        fireball.setVisualScale(scale);
        fireball.hasImpulse = true;
        if (server.addFreshEntity(fireball)) {
            if (aiControlled) dragon.setAiFireballDecision("fired:stage-" + getChargeLevel(chargeAtRelease));
            if (learningTrial != 0) {
                dragon.getCombatLearning().releaseAttack(learningTrial);
                // A missing impact (unload/removal) is neutral when this bounded fallback expires.
                dragon.getCombatLearning().deferAttackResult(learningTrial,
                        DragonCombatLearning.Outcome.CANCELLED, MAGMA_LIFETIME_TICKS + 2);
                fireball.trackCombatResult(learningTrial, launch.expectedDistance());
                learningTrial = 0;
            }
            server.broadcastEntityEvent(dragon, switch (getChargeLevel(chargeAtRelease)) {
                case 3 -> Ignivorus.FIREBALL_LEVEL_THREE_MOUTH_EVENT;
                case 2 -> Ignivorus.FIREBALL_LEVEL_TWO_MOUTH_EVENT;
                default -> Ignivorus.FIREBALL_MOUTH_EVENT;
            });
        } else if (aiControlled) {
            dragon.setAiFireballDecision("spawn-rejected");
        }
    }

    private void resetChargeState() {
        chargeTicks = 0;
        lastChargeAnimLevel = 0;
        hasFired = false;
        releaseRequested = false;
        releaseTicks = 0;
        releaseChargeTicks = 0;
        aiReleasePending = false;
        aiAlignmentTicks = 0;
        aiAimTick = Integer.MIN_VALUE;
        aiSolution = null;
        aiShot = DragonCombatAim.Shot.NO_TARGET;
        aiTarget = null;
        getUser().setFireballChargeLevel(0);
    }

    private Vec3 getFireballOrigin(Ignivorus dragon) {
        Vec3 origin = dragon.getFireBreathStartAnchor(1.0f);
        return origin != null ? origin : dragon.getEyePosition();
    }

    private Vec3 getAimDirection(Ignivorus dragon) {
        return DragonAimHelper.riderTargetOrLookDirection(
                dragon,
                getFireballOrigin(dragon),
                dragon.getTarget(),
                0.6D
        );
    }

    public DragonCombatAim.Shot updateAiAim() {
        Ignivorus dragon = getUser();
        if (!aiControlled || dragon.level().isClientSide || aiAimTick == dragon.tickCount) return aiShot;
        if (aiTarget == null || aiTarget != dragon.getTarget() || !dragon.isTargetValid(aiTarget)) {
            return DragonCombatAim.Shot.NO_TARGET;
        }
        aiAimTick = dragon.tickCount;
        Vec3 origin = getFireballOrigin(dragon);
        float scale = BASE_SCALE * getChargeMultiplier(aiReleasePending || releaseRequested ? releaseChargeTicks : chargeTicks);
        aiSolution = predictAiSolution(dragon, origin, scale);
        dragon.getCombatAim().trackPoint(dragon.getTarget(), origin,
                dragon.isAerial() ? AIR_AIM : DragonCombatAim.FIRE,
                aiSolution == null ? null : aiSolution.aimPoint());
        aiShot = dragon.getCombatAim().assessDirection(aiSolution == null ? null : aiSolution.direction(), 5);
        return aiShot;
    }

    public void cancelAiCast(String reason) {
        getUser().setAiFireballDecision(reason);
        if (learningTrial != 0) {
            getUser().getCombatLearning().finishAttack(learningTrial, "trajectory-blocked".equals(reason)
                    ? DragonCombatLearning.Outcome.BLOCKED : DragonCombatLearning.Outcome.CANCELLED);
            learningTrial = 0;
        }
        getUser().forceEndAbility(getAbilityType());
    }

    private @Nullable AiSolution predictAiSolution(Ignivorus dragon, Vec3 origin, float scale) {
        LivingEntity target = dragon.getTarget();
        Vec3 predicted = dragon.getCombatLearning().predictCenter(target, 0, 12);
        if (predicted == null) return unavailableAiSolution("no-visible-observation");
        Vec3 aimPoint = predicted;
        // A few bounded passes solve travel time and the same delayed gravity used by the entity.
        for (int iteration = 0; iteration < 4; iteration++) {
            double ticks = origin.distanceTo(aimPoint) / FIREBALL_SPEED;
            if (!Double.isFinite(ticks) || ticks < 1.0E-4D || ticks > 32) return unavailableAiSolution("out-of-range");
            predicted = dragon.getCombatLearning().predictCenter(target, ticks, 12);
            if (predicted == null) return unavailableAiSolution("no-visible-observation");
            Vec3 direction = aimPoint.subtract(origin).normalize();
            Vec3 displacement = simulateDisplacement(direction, ticks, scale);
            double drop = direction.y * FIREBALL_SPEED * ticks - displacement.y;
            aimPoint = predicted.add(0, drop, 0);
        }
        double ticks = origin.distanceTo(aimPoint) / FIREBALL_SPEED;
        if (!Double.isFinite(ticks) || ticks < 1.0E-4D || ticks > 32) return unavailableAiSolution("out-of-range");
        return new AiSolution(aimPoint, aimPoint.subtract(origin).normalize(), ticks, origin.distanceTo(predicted));
    }

    private @Nullable AiSolution unavailableAiSolution(String reason) {
        aiAimFailure = reason;
        return null;
    }

    private @Nullable AiLaunch predictAiLaunch(Ignivorus dragon, Vec3 origin, float scale) {
        AiSolution solution = predictAiSolution(dragon, origin, scale);
        if (solution == null) return rejectAiLaunch(dragon, aiAimFailure);
        DragonCombatAim.Shot shot = dragon.getCombatAim().assessDirection(solution.direction(), 5);
        if (shot != DragonCombatAim.Shot.ALIGNED) return rejectAiLaunch(dragon, "release-" + shot.reason());
        if (!clearTrajectory(dragon, origin, solution.direction(), solution.travelTicks(), scale)) {
            return rejectAiLaunch(dragon, "trajectory-blocked");
        }
        return new AiLaunch(solution.direction(), solution.expectedDistance());
    }

    private @Nullable AiLaunch rejectAiLaunch(Ignivorus dragon, String reason) {
        dragon.setAiFireballDecision(reason);
        if (learningTrial != 0) {
            dragon.getCombatLearning().finishAttack(learningTrial, "trajectory-blocked".equals(reason)
                    ? DragonCombatLearning.Outcome.BLOCKED : DragonCombatLearning.Outcome.RESPONSE_ONLY);
            learningTrial = 0;
        }
        return null;
    }

    private Vec3 simulateDisplacement(Vec3 direction, double ticks, float scale) {
        Vec3 motion = direction.scale(FIREBALL_SPEED);
        Vec3 displacement = Vec3.ZERO;
        double travelled = 0;
        for (int tick = 0; tick < Math.ceil(ticks); tick++) {
            motion = IgnivorusFireballEntity.motionForTick(motion, travelled, scale);
            Vec3 step = motion.scale(Math.min(1, ticks - tick));
            displacement = displacement.add(step);
            travelled += step.length();
        }
        return displacement;
    }

    private boolean clearTrajectory(Ignivorus dragon, Vec3 origin, Vec3 direction, double ticks, float scale) {
        Vec3 position = origin;
        Vec3 motion = direction.scale(FIREBALL_SPEED);
        double travelled = 0;
        for (int tick = 0; tick < Math.ceil(ticks); tick++) {
            motion = IgnivorusFireballEntity.motionForTick(motion, travelled, scale);
            Vec3 step = motion.scale(Math.min(1, ticks - tick));
            Vec3 next = position.add(step);
            if (!dragon.level().hasChunkAt(BlockPos.containing(next))
                    || dragon.level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS) return false;
            position = next;
            travelled += step.length();
        }
        return true;
    }

    private record AiLaunch(Vec3 direction, double expectedDistance) { }
    private record AiSolution(Vec3 aimPoint, Vec3 direction, double travelTicks, double expectedDistance) { }

    private float resolveImpactDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("fireball", DEFAULT_IMPACT_DAMAGE);
    }

    private String fireballAnimation(String animation) {
        Ignivorus dragon = getUser();
        boolean airborne = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
        return !dragon.isPhase2Active() && !airborne && dragon.onGround() ? animation + "_ground" : animation;
    }

    private void triggerChargeAnimation(int level) {
        Ignivorus dragon = getUser();
        switch (level) {
            case 1 -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level1_charge"));
                if (!dragon.level().isClientSide) {
                    dragon.level().broadcastEntityEvent(dragon, Ignivorus.FIREBALL_CHARGE_EVENT);
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL1_CHARGE.get(), 1.0f, 1.0f, 54);
                }
            }
            case 2 -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level2_charge"));
                if (!dragon.level().isClientSide) {
                    dragon.level().broadcastEntityEvent(dragon, Ignivorus.FIREBALL_LEVEL_TWO_CHARGE_EVENT);
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL2_CHARGE.get(), 1.0f, 1.0f, 68);
                }
            }
            case 3 -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level3_charge"));
                if (!dragon.level().isClientSide) {
                    dragon.level().broadcastEntityEvent(dragon, Ignivorus.FIREBALL_LEVEL_THREE_CHARGE_EVENT);
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL3_CHARGE.get(), 1.0f, 1.0f, 94);
                }
            }
            default -> { }
        }
    }


    private void triggerShootAnimation(int level) {
        Ignivorus dragon = getUser();
        switch (level) {
            case 2 -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level2_shoot"));
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL2_SHOOTS.get(), 1.0f, 1.0f, 76);
                }
            }
            case 3 -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level3_shoot"));
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL3_SHOOTS.get(), 1.0f, 1.0f, 45);
                }
            }
            default -> {
                dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, fireballAnimation("fireball_level1_shoot"));
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL1_SHOOTS.get(), 1.0f, 1.0f, 66);
                }
            }
        }
    }
}
