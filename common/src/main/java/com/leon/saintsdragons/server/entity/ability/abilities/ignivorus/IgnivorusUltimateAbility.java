package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.particles.ParticleTypes;
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

/**
 * Cinematic "ultimate" ability: plays a 3-stage animation sequence (start, loop, end) while
 * locking rider controls. Sound effects are triggered via animation keyframes.
 *
 * Animation timing:
 * - ultimate_start: 1.38s (28 ticks)
 * - ultimate: 5.42s (108 ticks)
 * - ultimate_end: 1.38s (28 ticks)
 * Total: ~8.2 seconds
 */
public class IgnivorusUltimateAbility extends DragonAbility<Ignivorus> {
    private static final int ULTIMATE_START_TICKS = 28;      // 1.38s animation.ignivorus.ultimate_start
    private static final int ULTIMATE_LOOP_TICKS = 108;      // 5.42s animation.ignivorus.ultimate
    private static final int ULTIMATE_END_TICKS = 28;        // 1.38s animation.ignivorus.ultimate_end
    private static final int TOTAL_SEQUENCE_TICKS = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS;
    private static final int COOLDOWN_TICKS = 0;

    // Tick thresholds for animation transitions
    @SuppressWarnings("unused")
    private static final int START_END_TICK = ULTIMATE_START_TICKS;
    @SuppressWarnings("unused")
    private static final int LOOP_END_TICK = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS;

    private static final double EXPLOSION_RADIUS = 32.0D; // Reduced from 64 for better visibility
    private static final float EXPLOSION_DAMAGE = 200.0F;
    private static final int EXPLOSION_FIRE_SECONDS = 8;
    private static final int EXPLOSION_PARTICLE_POINTS = 64; // Doubled for denser particle ring
    private static final int LOOP_DAMAGE_INTERVAL = 5; // ticks between pulses while ultimate animation plays
    private static final int LOOP_DAMAGE_WARMUP = 20;  // delay before first pulse (ticks inside loop)
    private static final float PENALTY_HEALTH = 50.0F;
    private static final Component PENALTY_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_penalty");
    private static final Component REQUIREMENT_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_requires_full_health");
    private static final Component GROUND_MESSAGE =
            Component.translatable("saintsdragons.message.ignivorus.ultimate_ground_only");

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

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        if (!dragon.onGround()) {
            sendGroundMessage();
            return false;
        }
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding()) {
            sendGroundMessage();
            return false;
        }

        // Only enforce full health requirement for ridden dragons (player-controlled)
        // AI-controlled wild dragons can use it regardless of health
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
            // Lock controls for the full sequence duration
            dragon.lockRiderControls(TOTAL_SEQUENCE_TICKS);
            lockedControls = true;
            dragon.markLandedNow();
            dragon.setHovering(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
            dragon.setDeltaMovement(Vec3.ZERO);
            dragon.setUltimateCameraZoomActive(true);

            // Reset animation tracking flags
            startAnimPlayed = false;
            loopAnimPlayed = false;
            endAnimPlayed = false;
            lastLoopDamageTick = -LOOP_DAMAGE_INTERVAL;
            penaltyApplied = false;

            // Play ONLY the first animation (start)
            dragon.triggerAnim("action", "ultimate_start");
            startAnimPlayed = true;
            applyPenaltyHealth(dragon);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        Ignivorus dragon = getUser();
        int ticks = getTicksInSection();

        // Manually trigger each animation when the previous one finishes
        // This prevents gaps/flickers between animations

        if (!loopAnimPlayed && ticks >= START_END_TICK) {
            dragon.triggerAnim("action", "ultimate");
            loopAnimPlayed = true;
        }

        // Apply continuous ring-of-fire damage only while the "ultimate" animation plays
        if (loopAnimPlayed && ticks >= START_END_TICK && ticks < LOOP_END_TICK) {
            int loopTick = ticks - START_END_TICK;
            if (loopTick >= LOOP_DAMAGE_WARMUP && loopTick - lastLoopDamageTick >= LOOP_DAMAGE_INTERVAL) {
                triggerRingExplosion(loopTick == LOOP_DAMAGE_WARMUP);
                lastLoopDamageTick = loopTick;
            }
        }

        if (!endAnimPlayed && ticks >= LOOP_END_TICK) {
            dragon.triggerAnim("action", "ultimate_end");
            endAnimPlayed = true;
        }
    }

    private void applyPenaltyHealth(Ignivorus dragon) {
        if (penaltyApplied) {
            return;
        }

        // ONLY apply health penalty to tamed dragons when owner triggers it (being ridden)
        // Wild dragons and AI-controlled tamed dragons use it for free
        if (dragon.isTame() && dragon.isVehicle()) {
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

    private void sendGroundMessage() {
        Player rider = getUser().getRidingPlayer();
        if (rider != null) {
            rider.displayClientMessage(GROUND_MESSAGE, true);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            releaseLocks();
        }
    }

    @Override
    public void interrupt() {
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

    private void triggerRingExplosion(boolean openingPulse) {
        Ignivorus dragon = getUser();
        Vec3 center = dragon.position();
        dragon.triggerScreenShake(openingPulse ? 2.3F : 1.2F);

        if (dragon.level().isClientSide) {
            return;
        }

        ServerLevel server = (ServerLevel) dragon.level();

        // Spawn central explosion particles for dramatic effect
        if (openingPulse) {
            // Big initial burst at dragon's position
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 3, 1.0D, 0.5D, 1.0D, 0.0D);
            server.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0D, center.z, 100, 2.0D, 1.0D, 2.0D, 0.2D);
            server.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.5D, center.z, 50, 1.5D, 0.5D, 1.5D, 0.1D);
        }

        spawnRingParticles(server, center);
        applyRingDamage(server, center);
    }

    private void spawnRingParticles(ServerLevel level, Vec3 center) {
        for (int i = 0; i < EXPLOSION_PARTICLE_POINTS; i++) {
            double angle = (Math.PI * 2.0D * i) / EXPLOSION_PARTICLE_POINTS;
            double x = center.x + Math.cos(angle) * EXPLOSION_RADIUS;
            double z = center.z + Math.sin(angle) * EXPLOSION_RADIUS;

            // Spawn particles at ground level and slightly above for better visibility
            double y = center.y + 0.1D; // Just above ground

            // Main flame particles - more count, more spread
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.8D, 0.6D, 0.8D, 0.05D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5D, z, 8, 0.6D, 0.4D, 0.6D, 0.02D);

            // Add extra fire particles shooting upward for dramatic effect
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.3D, 1.0D, 0.3D, 0.1D);

            // Add some lava particles for extra pizzazz
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 3, 0.5D, 0.2D, 0.5D, 0.0D);
        }
    }

    private void applyRingDamage(ServerLevel level, Vec3 center) {
        double radiusSqr = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
        DamageSource source = level.damageSources().mobAttack(getUser());
        float explosionDamage = resolveExplosionDamage();

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                getUser().getBoundingBox().inflate(EXPLOSION_RADIUS),
                target -> target != getUser() && target.isAlive() && target.attackable() && !getUser().isAlly(target))) {

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
