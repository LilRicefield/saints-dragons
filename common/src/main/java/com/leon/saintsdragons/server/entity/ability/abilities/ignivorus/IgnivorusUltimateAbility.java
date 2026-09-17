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
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusSkyfallEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusSkyfallRingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;
public class IgnivorusUltimateAbility extends DragonAbility<Ignivorus> {

    private static final int SKYFALL_TICKS = 14 * 20;
    private static final int SKYFALL_AIR_SOUND_TICKS = 15 * 20;
    public static final int SKYFALL_EXPLOSION_TICK = (int) Math.round(6.23D * 20.0D);
    private static final int SKYFALL_STAR_TICK = SKYFALL_EXPLOSION_TICK - 4;
    private static final int SKYFALL_CHARGE_TICK = SKYFALL_EXPLOSION_TICK - 20;
    private static final int SKYFALL_AURA_TICK = SKYFALL_CHARGE_TICK + 2;
    private static final int SKYFALL_SHARP_TICK = SKYFALL_EXPLOSION_TICK - 8;
    private static final int SKYFALL_SWIRL_TICK = SKYFALL_EXPLOSION_TICK - 24;
    private static final int SKYFALL_ABSORB_TICK = 5 * 20;
    private static final double EXPLOSION_VISUAL_HEIGHT = 20.0D;

    private static final int COOLDOWN_TICKS_RIDER = 0;
    private static final int COOLDOWN_TICKS_AI = 6000;

    private static final float EXPLOSION_DAMAGE = 200.0F;

    private static final int FIRE_PUFF_COUNT = 96;
    private static final int SPEC_PUFF_COUNT = 64;
    private static final double FIRE_PUFF_VIEW_DISTANCE_SQR = 128.0D * 128.0D;
    private static final float PENALTY_HEALTH = 50.0F;
    private static final Component PENALTY_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_penalty");
    private static final Component REQUIREMENT_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_requires_full_health");

    private static final DragonAbilitySection[] SKYFALL_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, SKYFALL_TICKS)
    };
    private static final int PHASE2_SKYFALL_OFFSET = 16;
    private static final DragonAbilitySection[] PHASE2_SKYFALL_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, SKYFALL_TICKS - PHASE2_SKYFALL_OFFSET)
    };

    private boolean lockedControls;
    private boolean penaltyApplied;
    private boolean novaSpawned;
    private boolean explosionStarSpawned;
    private boolean skyfallChargeSpawned;
    private boolean skyfallAuraSpawned;
    private boolean skyfallSharpSpawned;
    private boolean skyfallSwirlSpawned;
    private boolean skyfallCircleSpawned;
    private boolean skyfallAbsorbSpawned;
    private boolean transitionsToPhase2;
    private boolean transitionsAfterLanding;
    private boolean groundSkyfallMode;
    private boolean airSkyfallMode;
    private boolean phase2SkyfallMode;

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, SKYFALL_TRACK, user.getControllingPassenger() != null ? COOLDOWN_TICKS_RIDER : COOLDOWN_TICKS_AI);
    }

    @Override
    public void start() {
        groundSkyfallMode = !getUser().isAerial();
        airSkyfallMode = !groundSkyfallMode;
        phase2SkyfallMode = groundSkyfallMode && getUser().isPhase2Active();
        super.start();
    }

    @Override
    public DragonAbilitySection[] getSectionTrack() {
        return phase2SkyfallMode ? PHASE2_SKYFALL_TRACK : SKYFALL_TRACK;
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
        if (section == null || section.sectionType != STARTUP) return;
        Ignivorus dragon = getUser();
        boolean lowHealthUltimate = dragon.shouldTriggerWildUltimateAtCurrentHealth();
        transitionsToPhase2 = lowHealthUltimate && groundSkyfallMode && dragon.isGroundedForAction();
        transitionsAfterLanding = lowHealthUltimate && airSkyfallMode;
        if (lowHealthUltimate && airSkyfallMode) dragon.markWildLowHealthUltimateTriggered();
        beginSkyfall(dragon);
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != STARTUP) {
            return;
        }
        int ticks = getTicksInSection();
        if (groundSkyfallMode || airSkyfallMode) {
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
    }

    private void beginSkyfall(Ignivorus dragon) {
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
        dragon.getCombatAim().clear();
        dragon.lockRiderControls(SKYFALL_TICKS - offset);
        if (!dragon.level().isClientSide) dragon.getAIMovement().stopAndClearAllMovement();
        lockedControls = true;
        if (!airSkyfallMode) {
            dragon.markLandedNow();
            dragon.setHovering(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
        }
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setUltimateCameraZoomActive(false);
        dragon.triggerHitboxAnimation(airSkyfallMode ? AnimationHelper.FLIGHT_CONTROLLER : IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, airSkyfallMode ? "skyfall_air" : phase2SkyfallMode ? "skyfall_phase2" : "skyfall");
        if (!dragon.level().isClientSide) {
            dragon.getSoundHandler().playMovingEntitySound(
                    airSkyfallMode ? ModSounds.IGNIVORUS_SKYFALL_AIR.get()
                            : phase2SkyfallMode ? ModSounds.IGNIVORUS_SKYFALL_PHASE2.get() : ModSounds.IGNIVORUS_SKYFALL.get(),
                    1.0F, 1.0F, airSkyfallMode ? SKYFALL_AIR_SOUND_TICKS : SKYFALL_TICKS - offset);
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
            releaseLocks();
            if (transitionsToPhase2 && groundSkyfallMode && getUser().isAlive()
                    && !getUser().isTamingStunned()) {
                getUser().queueWildPhase2Transition();
                transitionsToPhase2 = false;
            }
            if (transitionsAfterLanding && getUser().isAlive() && !getUser().isTamingStunned()) {
                getUser().queueWildPhase2AfterLanding();
                transitionsAfterLanding = false;
            }
        }
    }

    @Override
    public void interrupt() {
        transitionsToPhase2 = false;
        transitionsAfterLanding = false;
        if (groundSkyfallMode || airSkyfallMode) {
            getUser().stopHitboxAnimation(airSkyfallMode ? AnimationHelper.FLIGHT_CONTROLLER : IgnivorusAnimationHandler.MOVEMENT_CONTROLLER,
                    airSkyfallMode ? "skyfall_air" : phase2SkyfallMode ? "skyfall_phase2" : "skyfall");
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

        IgnivorusSkyfallEntity nova = new IgnivorusSkyfallEntity(
                server,
                novaPos,
                dragon,
                resolveExplosionDamage()
        );
        server.addFreshEntity(nova);

        if (!airSkyfallMode) {
        IgnivorusSkyfallRingEntity ring = new IgnivorusSkyfallRingEntity(
                server,
                center.add(0, 0.1, 0)
        );
        server.addFreshEntity(ring);
        }

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
        for (int side = 0; !airSkyfallMode && side < 4; side++) {
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
            if (!airSkyfallMode) {
            server.sendParticles(viewer, ModParticles.IGNIVORUS_GROUND_IMPACT.get(), true,
                    center.x, center.y + 0.24D, center.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            server.sendParticles(viewer, airSkyfallMode ? ModParticles.IGNIVORUS_AIR_AFTERMATH.get() : ModParticles.IGNIVORUS_AFTERMATH.get(), true,
                    center.x, center.y, center.z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < FIRE_PUFF_COUNT + SPEC_PUFF_COUNT; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos(1.0D - random.nextDouble() * 1.4D);
            double sinPhi = Math.sin(phi);
            Vec3 dir = new Vec3(Math.cos(theta) * sinPhi, Math.cos(phi), Math.sin(theta) * sinPhi);
            Vec3 pos = base.add(dir.scale(8.0D + random.nextDouble() * 8.0D));
            Vec3 vel = dir.scale(1.2D + random.nextDouble() * 1.0D)
                    .add(0.0D, 0.05D + random.nextDouble() * 0.25D, 0.0D);
            for (var viewer : viewers) {
                server.sendParticles(viewer, i < FIRE_PUFF_COUNT ? ModParticles.IGNIVORUS_EXPLOSION_FIRE.get()
                        : ModParticles.IGNIVORUS_EXPLOSION_SPEC.get(), true,
                        pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1.0D);
            }
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
