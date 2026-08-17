package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaRingEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFlameEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;
public class IgnivorusUltimateAbility extends DragonAbility<Ignivorus> {

    private static final int ULTIMATE_START_TICKS = 40;
    private static final int ULTIMATE_LOOP_TICKS = 108;
    private static final int ULTIMATE_END_TICKS = 25;

    // Air ultimate timings
    private static final int ULTIMATE_START_AIR_TICKS = 29;
    private static final int ULTIMATE_END_AIR_TICKS = 29;

    private static final int TOTAL_SEQUENCE_TICKS = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS;
    private static final int COOLDOWN_TICKS_RIDER = 0;
    private static final int COOLDOWN_TICKS_AI = 6000;

    private static final double EXPLOSION_RADIUS = 32.0D;
    private static final float EXPLOSION_DAMAGE = 200.0F;
    private static final int EXPLOSION_FIRE_SECONDS = 8;
    private static final int LOOP_DAMAGE_INTERVAL = 5;

    private static final int LOOP_DAMAGE_WARMUP = 30;
    private static final int NOVA_SPAWN_DELAY = 70;
    private static final int LOOP_DAMAGE_WARMUP_AIR = 13;
    private static final int NOVA_SPAWN_DELAY_AIR = 42;
    private static final int PHASE2_DAMAGE_DELAY = 10;
    private static final int PHASE2_NOVA_SPAWN_DELAY = 13;
    private static final float PENALTY_HEALTH = 50.0F;
    private static final Component PENALTY_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_penalty");
    private static final Component REQUIREMENT_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_requires_full_health");

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, TOTAL_SEQUENCE_TICKS),
            new AbilitySectionDuration(ACTIVE, 1),
            new AbilitySectionDuration(RECOVERY, 10)
    };

    private boolean lockedControls;
    private boolean startAnimPlayed;
    private boolean loopAnimPlayed;
    private boolean endAnimPlayed;
    private int lastLoopDamageTick;
    private boolean penaltyApplied;
    private boolean isPhase2GroundMode;
    private boolean isAirborneMode;
    private boolean phase2DamageApplied;
    private boolean novaSpawned;
    private boolean transitionsToPhase2;

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, user.getControllingPassenger() != null ? COOLDOWN_TICKS_RIDER : COOLDOWN_TICKS_AI);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        if (dragon.isVehicle() && dragon.getHealth() < dragon.getMaxHealth()) {
            sendRequirementMessage();
            return false;
        }

        return super.tryAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

        if (section.sectionType == STARTUP) {
            boolean isAirborne = dragon.isAerial();
            boolean wildLowHealthUltimate = dragon.shouldTriggerWildUltimateAtCurrentHealth();
            boolean wildPhase1Transition = wildLowHealthUltimate
                    && !isAirborne
                    && dragon.isGroundedForAction();
            isAirborneMode = isAirborne;
            isPhase2GroundMode = dragon.isPhase2Active() && !isAirborne;
            transitionsToPhase2 = wildPhase1Transition;
            if (wildLowHealthUltimate && isAirborne) {
                dragon.markWildLowHealthUltimateTriggered();
            }

            if (isPhase2GroundMode) {
                dragon.lockRiderControls(ULTIMATE_LOOP_TICKS);
                lockedControls = true;
                dragon.markLandedNow();
                dragon.setHovering(false);
                dragon.setLanding(false);
                dragon.setTakeoff(false);
                dragon.setDeltaMovement(Vec3.ZERO);
                dragon.setUltimateCameraZoomActive(true);
                dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "phase2_ultimate");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_AIR.get(), 1.0f, 1.0f, 127);
                }
                phase2DamageApplied = false;
                novaSpawned = false;

                applyPenaltyHealth(dragon);
            } else {
                int totalTicks = isAirborne
                    ? (ULTIMATE_START_AIR_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_AIR_TICKS)
                    : (ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS);
                dragon.lockRiderControls(totalTicks);
                lockedControls = true;

                if (!isAirborne) {
                    dragon.markLandedNow();
                    dragon.setHovering(false);
                    dragon.setLanding(false);
                    dragon.setTakeoff(false);
                    dragon.setDeltaMovement(Vec3.ZERO);
                }

                dragon.setUltimateCameraZoomActive(true);
                startAnimPlayed = false;
                loopAnimPlayed = false;
                endAnimPlayed = false;
                lastLoopDamageTick = -LOOP_DAMAGE_INTERVAL;
                penaltyApplied = false;
                novaSpawned = false;
                if (isAirborne) {
                    dragon.triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, "ultimate_start_air");
                    if (!dragon.level().isClientSide) {
                        dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_START_AIR.get(), 1.0f, 1.0f, 54);
                    }
                } else {
                    dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "ultimate_start");
                    if (!dragon.level().isClientSide) {
                        dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_START.get(), 1.0f, 1.0f, 92);
                    }
                }
                startAnimPlayed = true;
                applyPenaltyHealth(dragon);
            }
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != STARTUP) {
            return;
        }
        int ticks = getTicksInSection();
        if (isPhase2GroundMode) {
            if (!novaSpawned && ticks >= PHASE2_NOVA_SPAWN_DELAY) {
                spawnNovaEntity();
                novaSpawned = true;
            }

            if (!phase2DamageApplied && ticks >= PHASE2_DAMAGE_DELAY) {
                triggerRingExplosion(true);
                phase2DamageApplied = true;
            }
            if (ticks >= ULTIMATE_LOOP_TICKS) {
                end();
            }
            return;
        }

        Ignivorus dragon = getUser();

        int startEndTick = isAirborneMode ? ULTIMATE_START_AIR_TICKS : ULTIMATE_START_TICKS;
        int loopEndTick = startEndTick + ULTIMATE_LOOP_TICKS;
        int novaDelay = isAirborneMode ? NOVA_SPAWN_DELAY_AIR : NOVA_SPAWN_DELAY;
        int damageWarmup = isAirborneMode ? LOOP_DAMAGE_WARMUP_AIR : LOOP_DAMAGE_WARMUP;

        if (!loopAnimPlayed && ticks >= startEndTick) {
            if (isAirborneMode) {
                dragon.triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, "ultimate_air");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_AIR.get(), 1.0f, 1.0f, 112);
            } else {
                dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "ultimate");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE.get(), 1.0f, 1.0f, 127);
            }
            loopAnimPlayed = true;
        }

        if (!novaSpawned && ticks >= novaDelay) {
            spawnNovaEntity();
            novaSpawned = true;
        }

        if (loopAnimPlayed && ticks >= startEndTick && ticks < loopEndTick) {
            int loopTick = ticks - startEndTick;
            if (loopTick >= damageWarmup && loopTick - lastLoopDamageTick >= LOOP_DAMAGE_INTERVAL) {
                boolean isOpeningPulse = loopTick == damageWarmup;

                triggerRingExplosion(isOpeningPulse);
                lastLoopDamageTick = loopTick;
            }
        }

        if (!endAnimPlayed && ticks >= loopEndTick) {
            if (isAirborneMode) {
                dragon.triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, "ultimate_end_air");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_END_AIR.get(), 1.0f, 1.0f, 38);
            } else if (transitionsToPhase2) {
                dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "ultimate_end_to_phase_2");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_END.get(), 1.0f, 1.0f, 57);
            } else {
                dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "ultimate_end");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_END.get(), 1.0f, 1.0f, 57);
            }
            endAnimPlayed = true;
        }
    }

    private void applyPenaltyHealth(Ignivorus dragon) {
        if (penaltyApplied) {
            return;
        }

        if (dragon.getRidingPlayer() != null) {
            float current = dragon.getHealth();
            float penaltyHealth = resolvePenaltyHealth();
            if (current > penaltyHealth) {
                dragon.setHealth(penaltyHealth);
                sendPenaltyMessage();
            }
        }

        penaltyApplied = true;
    }

    private void sendRequirementMessage() {
        Player rider = getUser().getRidingPlayer();
        if (rider != null) {
            rider.displayClientMessage(REQUIREMENT_MESSAGE, true);
        }
    }

    private void sendPenaltyMessage() {
        Player rider = getUser().getRidingPlayer();
        if (rider != null) {
            rider.displayClientMessage(PENALTY_MESSAGE, true);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            if (transitionsToPhase2 && endAnimPlayed) {
                getUser().completeWildPhase2Transition();
                transitionsToPhase2 = false;
            }
            releaseLocks();
        }
    }

    @Override
    public void interrupt() {
        transitionsToPhase2 = false;
        releaseLocks();
        super.interrupt();
    }

    @Override
    public void end() {
        releaseLocks();
        super.end();
    }

    private void releaseLocks() {
        if (lockedControls) {
            getUser().clearRiderControlLock();
            lockedControls = false;
        }
        getUser().setUltimateCameraZoomActive(false);
    }

    private void spawnNovaEntity() {
        Ignivorus dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }

        ServerLevel server = (ServerLevel) dragon.level();
        Vec3 center = dragon.position();

        Vec3 novaPos = center.add(0, 1.0, 0);

        IgnivorusNovaEntity nova = new IgnivorusNovaEntity(
                server,
                novaPos,
                dragon,
                resolveExplosionDamage()
        );
        server.addFreshEntity(nova);

        IgnivorusNovaRingEntity ring = new IgnivorusNovaRingEntity(
                server,
                center.add(0, 0.1, 0)
        );
        server.addFreshEntity(ring);
        int flameCount = 32;
        double flameSpeed = 1.2;
        float flameScale = 2.0F;
        int flameLifetime = 30;
        float flameDamage = 0.0F;
        var random = dragon.getRandom();
        Vec3 spawnPos = center.add(0, 4.0, 0);

        for (int i = 0; i < flameCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double horizontal = Math.sqrt(random.nextDouble());
            double vx = Math.cos(angle) * horizontal;
            double vz = Math.sin(angle) * horizontal;
            double vy = 0.35 + random.nextDouble() * 0.35;

            Vec3 velocity = new Vec3(vx, vy, vz).normalize().scale(flameSpeed * (0.8 + random.nextDouble() * 0.6));

            IgnivorusFlameEntity flame = new IgnivorusFlameEntity(
                    server,
                    spawnPos,
                    velocity,
                    dragon,
                    flameDamage,
                    flameScale,
                    flameLifetime
            );
            server.addFreshEntity(flame);
        }

    }

    private void triggerRingExplosion(boolean openingPulse) {
        Ignivorus dragon = getUser();
        Vec3 center = dragon.position();
        dragon.triggerScreenShake(openingPulse ? 2.3F : 1.2F);

        if (dragon.level().isClientSide) {
            return;
        }
        ServerLevel server = (ServerLevel) dragon.level();
        applyRingDamage(server, center);
    }

    private void applyRingDamage(ServerLevel level, Vec3 center) {
        double radiusSqr = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
        DamageSource source = level.damageSources().mobAttack(getUser());
        float explosionDamage = resolveExplosionDamage();

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                getUser().getBoundingBox().inflate(EXPLOSION_RADIUS),
                target -> {
                    if (target == getUser()) return false;
                    if (target instanceof Ignivorus baby && baby.isBaby()) return false;
                    return target.isAlive()
                            && target.attackable()
                            && !getUser().isAlly(target)
                            && !DragonElementalImmunity.isFireImmune(target);
                })) {

            if (entity.position().distanceToSqr(center) > radiusSqr) {
                continue;
            }

            entity.hurt(source, explosionDamage);
            entity.setSecondsOnFire(EXPLOSION_FIRE_SECONDS);

            Vec3 knock = entity.position().subtract(center).normalize().scale(1.4D);
            entity.push(knock.x, 0.6D, knock.z);
        }
    }

    private float resolveExplosionDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("ultimate", EXPLOSION_DAMAGE);
    }

    private float resolvePenaltyHealth() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .extraDouble("ultimate_penalty_health", PENALTY_HEALTH);
    }
}
