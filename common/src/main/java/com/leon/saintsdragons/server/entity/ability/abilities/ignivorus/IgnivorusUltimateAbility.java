package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
    private static final int COOLDOWN_TICKS = 20 * 60; // 60s cooldown

    // Tick thresholds for animation transitions
    @SuppressWarnings("unused")
    private static final int START_END_TICK = ULTIMATE_START_TICKS;
    @SuppressWarnings("unused")
    private static final int LOOP_END_TICK = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS;

    private static final double EXPLOSION_RADIUS = 64.0D;
    private static final float EXPLOSION_DAMAGE = 200.0F;
    private static final int EXPLOSION_FIRE_SECONDS = 8;
    private static final int EXPLOSION_PARTICLE_POINTS = 32;
    private static final int LOOP_DAMAGE_INTERVAL = 5; // ticks between pulses while ultimate animation plays
    private static final int LOOP_DAMAGE_WARMUP = 20;  // delay before first pulse (ticks inside loop)

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

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean canUse() {
        Ignivorus dragon = getUser();
        if (!dragon.onGround()) {
            return false;
        }
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding()) {
            return false;
        }
        return super.canUse();
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

            // Reset animation tracking flags
            startAnimPlayed = false;
            loopAnimPlayed = false;
            endAnimPlayed = false;
            lastLoopDamageTick = -LOOP_DAMAGE_INTERVAL;

            // Play ONLY the first animation (start)
            dragon.triggerAnim("action", "ultimate_start");
            startAnimPlayed = true;
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
    }

    private void triggerRingExplosion(boolean openingPulse) {
        Ignivorus dragon = getUser();
        Vec3 center = dragon.position();
        dragon.triggerScreenShake(openingPulse ? 2.3F : 1.2F);

        if (dragon.level().isClientSide) {
            return;
        }

        ServerLevel server = (ServerLevel) dragon.level();
        spawnRingParticles(server, center);
        applyRingDamage(server, center);
    }

    private void spawnRingParticles(ServerLevel level, Vec3 center) {
        for (int i = 0; i < EXPLOSION_PARTICLE_POINTS; i++) {
            double angle = (Math.PI * 2.0D * i) / EXPLOSION_PARTICLE_POINTS;
            double x = center.x + Math.cos(angle) * EXPLOSION_RADIUS;
            double z = center.z + Math.sin(angle) * EXPLOSION_RADIUS;
            level.sendParticles(ParticleTypes.FLAME, x, center.y + 0.5D, z, 12, 0.6D, 0.4D, 0.6D, 0.02D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, center.y + 0.5D, z, 4, 0.4D, 0.2D, 0.4D, 0.01D);
        }
    }

    private void applyRingDamage(ServerLevel level, Vec3 center) {
        double radiusSqr = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
        DamageSource source = level.damageSources().mobAttack(getUser());

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                getUser().getBoundingBox().inflate(EXPLOSION_RADIUS),
                target -> target != getUser() && target.isAlive() && target.attackable() && !getUser().isAlly(target))) {

            if (entity.position().distanceToSqr(center) > radiusSqr) {
                continue;
            }

            entity.hurt(source, EXPLOSION_DAMAGE);
            entity.setSecondsOnFire(EXPLOSION_FIRE_SECONDS);

            Vec3 knock = entity.position().subtract(center).normalize().scale(1.4D);
            entity.push(knock.x, 0.6D, knock.z);
        }
    }
}
