package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaRingEntity;
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

    private static final int SKYFALL_TICKS = 14 * 20;
    public static final int SKYFALL_EXPLOSION_TICK = (int) Math.round(6.23D * 20.0D);
    private static final int SKYFALL_STAR_TICK = SKYFALL_EXPLOSION_TICK - 4;
    private static final int SKYFALL_CHARGE_TICK = SKYFALL_EXPLOSION_TICK - 20;
    private static final int SKYFALL_AURA_TICK = SKYFALL_CHARGE_TICK + 2;
    private static final int SKYFALL_SHARP_TICK = SKYFALL_EXPLOSION_TICK - 8;
    private static final int SKYFALL_SWIRL_TICK = SKYFALL_EXPLOSION_TICK - 24;
    private static final int SKYFALL_ABSORB_TICK = 5 * 20;
    private static final double EXPLOSION_VISUAL_HEIGHT = 20.0D;
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
    private static final int FIRE_PUFF_COUNT = 48;
    private static final double FIRE_PUFF_VIEW_DISTANCE_SQR = 128.0D * 128.0D;
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
    private static final DragonAbilitySection[] SKYFALL_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, SKYFALL_TICKS)
    };
    private static final int PHASE2_SKYFALL_OFFSET = 16;
    private static final DragonAbilitySection[] PHASE2_SKYFALL_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, SKYFALL_TICKS - PHASE2_SKYFALL_OFFSET)
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
    private boolean explosionStarSpawned;
    private boolean skyfallChargeSpawned;
    private boolean skyfallAuraSpawned;
    private boolean skyfallSharpSpawned;
    private boolean skyfallSwirlSpawned;
    private boolean skyfallCircleSpawned;
    private boolean skyfallAbsorbSpawned;
    private boolean transitionsToPhase2;
    private boolean groundSkyfallMode;
    private boolean phase2SkyfallMode;

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, user.getControllingPassenger() != null ? COOLDOWN_TICKS_RIDER : COOLDOWN_TICKS_AI);
    }

    @Override
    public void start() {
        groundSkyfallMode = !getUser().isAerial();
        phase2SkyfallMode = groundSkyfallMode && getUser().isPhase2Active();
        super.start();
    }

    @Override
    public DragonAbilitySection[] getSectionTrack() {
        return groundSkyfallMode ? (phase2SkyfallMode ? PHASE2_SKYFALL_TRACK : SKYFALL_TRACK) : TRACK;
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
                    && !dragon.isPhase2Active()
                    && !isAirborne
                    && dragon.isGroundedForAction();
            isAirborneMode = isAirborne;
            isPhase2GroundMode = dragon.isPhase2Active() && !isAirborne;
            transitionsToPhase2 = wildPhase1Transition;
            if (groundSkyfallMode) {
                beginGroundSkyfall(dragon);
                return;
            }
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
        if (groundSkyfallMode) {
            ticks += phase2SkyfallMode ? PHASE2_SKYFALL_OFFSET : 0;
            if (!skyfallAbsorbSpawned && ticks >= SKYFALL_ABSORB_TICK) {
                skyfallAbsorbSpawned = true;
                spawnSkyfallAbsorb();
            }
            if (!skyfallChargeSpawned && ticks >= SKYFALL_CHARGE_TICK) {
                skyfallChargeSpawned = true;
                spawnSkyfallCharge();
            }
            if (!explosionStarSpawned && ticks >= SKYFALL_STAR_TICK) {
                explosionStarSpawned = true;
                spawnExplosionStar();
            }
            if (!skyfallSharpSpawned && ticks >= SKYFALL_SHARP_TICK) {
                skyfallSharpSpawned = true;
                spawnSkyfallSharp();
            }
            if (!skyfallSwirlSpawned && ticks >= SKYFALL_SWIRL_TICK) {
                skyfallSwirlSpawned = true;
                spawnSkyfallSwirl();
            }
            if (!skyfallCircleSpawned && ticks >= SKYFALL_EXPLOSION_TICK - 6) {
                skyfallCircleSpawned = true;
                spawnSkyfallCircle();
            }
            if (!skyfallAuraSpawned && ticks >= SKYFALL_AURA_TICK) {
                skyfallAuraSpawned = true;
                spawnSkyfallAura();
            }
            if (!novaSpawned && ticks >= SKYFALL_EXPLOSION_TICK) {
                novaSpawned = true;
                spawnNovaEntity();
                getUser().triggerScreenShake(3.5F);
            }
            return;
        }
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

    private void beginGroundSkyfall(Ignivorus dragon) {
        int offset = phase2SkyfallMode ? PHASE2_SKYFALL_OFFSET : 0;
        dragon.setSkyfallChargeActive(true, offset);
        novaSpawned = false;
        explosionStarSpawned = false;
        skyfallChargeSpawned = false;
        skyfallAuraSpawned = false;
        skyfallSharpSpawned = false;
        skyfallSwirlSpawned = false;
        skyfallCircleSpawned = false;
        skyfallAbsorbSpawned = false;
        penaltyApplied = false;
        endAnimPlayed = false;
        dragon.getCombatAim().clear();
        dragon.lockRiderControls(SKYFALL_TICKS - offset);
        lockedControls = true;
        dragon.markLandedNow();
        dragon.setHovering(false);
        dragon.setLanding(false);
        dragon.setTakeoff(false);
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setUltimateCameraZoomActive(false);
        dragon.triggerAnim(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, phase2SkyfallMode ? "skyfall_phase2" : "skyfall");
        if (!dragon.level().isClientSide) {
            dragon.getSoundHandler().playMovingEntitySound(
                    phase2SkyfallMode ? ModSounds.IGNIVORUS_SKYFALL_PHASE2.get() : ModSounds.IGNIVORUS_SKYFALL.get(),
                    1.0F, 1.0F, SKYFALL_TICKS - offset);
        }
        applyPenaltyHealth(dragon);
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
            if (transitionsToPhase2 && (groundSkyfallMode || endAnimPlayed)) {
                getUser().completeWildPhase2Transition();
                transitionsToPhase2 = false;
            }
            releaseLocks();
        }
    }

    @Override
    public void interrupt() {
        transitionsToPhase2 = false;
        if (groundSkyfallMode) {
            getUser().stopTriggeredAnimation(IgnivorusAnimationHandler.MOVEMENT_CONTROLLER,
                    phase2SkyfallMode ? "skyfall_phase2" : "skyfall");
        }
        releaseLocks();
        super.interrupt();
    }

    @Override
    public void end() {
        releaseLocks();
        super.end();
    }

    private void releaseLocks() {
        getUser().setSkyfallChargeActive(false);
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

        spawnExplosionFire(server, center);
    }

    private void spawnSkyfallAbsorb() {
        if (!(getUser().level() instanceof ServerLevel server)) return;
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_ABSORB.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnSkyfallCircle() {
        if (!(getUser().level() instanceof ServerLevel server)) return;
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_CIRCLE.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnSkyfallSwirl() {
        if (!(getUser().level() instanceof ServerLevel server)) return;
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_SWIRL.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnSkyfallSharp() {
        if (!(getUser().level() instanceof ServerLevel server)) return;
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_SHARP.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnSkyfallAura() {
        if (!(getUser().level() instanceof ServerLevel server)) return;
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_AURA.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnSkyfallCharge() {
        if (!(getUser().level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_SKYFALL_CHARGE.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnExplosionStar() {
        if (!(getUser().level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 base = getUser().position().add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        for (var viewer : server.players()) {
            if (viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_EXPLOSION_STAR.get(), true,
                        base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnExplosionFire(ServerLevel server, Vec3 center) {
        var random = getUser().getRandom();
        Vec3 base = center.add(0.0D, EXPLOSION_VISUAL_HEIGHT, 0.0D);
        var viewers = server.players().stream()
                .filter(viewer -> viewer.distanceToSqr(base) <= FIRE_PUFF_VIEW_DISTANCE_SQR)
                .toList();
        var crossFire = new FireBreathParticleData(64.0F, 2.0F, 3.0F);
        Vec3 crossOrigin = center.add(0.0D, 1.0D, 0.0D);
        for (int side = 0; side < 4; side++) {
            Vec3 direction = Vec3.directionFromRotation(0.0F, getUser().yBodyRot + side * 90.0F);
            Vec3 outlet = crossOrigin.add(direction.scale(2.0D));
            Vec3 velocity = direction.scale(ExpandingBreathSection.DEFAULT_SPEED);
            for (var viewer : viewers) {
                server.sendParticles(viewer, crossFire, true,
                        outlet.x, outlet.y, outlet.z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
            }
        }
        for (var viewer : viewers) {
            server.sendParticles(viewer, ModParticles.IGNIVORUS_EXPLOSION_LAYER.get(), true,
                    base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(viewer, ModParticles.IGNIVORUS_FIRE_SPEC.get(), true,
                    base.x, base.y, base.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(viewer, ModParticles.IGNIVORUS_GROUND_IMPACT.get(), true,
                    center.x, center.y + 0.24D, center.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(viewer, ModParticles.IGNIVORUS_AFTERMATH.get(), true,
                    center.x, center.y, center.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < FIRE_PUFF_COUNT; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos(1.0D - random.nextDouble() * 1.4D);
            double sinPhi = Math.sin(phi);
            Vec3 dir = new Vec3(Math.cos(theta) * sinPhi, Math.cos(phi), Math.sin(theta) * sinPhi);
            Vec3 pos = base.add(dir.scale(8.0D + random.nextDouble() * 8.0D));
            Vec3 vel = dir.scale(1.2D + random.nextDouble() * 1.0D)
                    .add(0.0D, 0.05D + random.nextDouble() * 0.25D, 0.0D);
            for (var viewer : viewers) {
                server.sendParticles(viewer, ModParticles.IGNIVORUS_EXPLOSION_FIRE.get(), true,
                        pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1.0D);
            }
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
