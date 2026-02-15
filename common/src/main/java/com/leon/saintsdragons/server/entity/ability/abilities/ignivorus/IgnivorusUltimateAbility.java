package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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

/**
 * Cinematic "ultimate" ability: plays a 3-stage animation sequence (start, loop, end) while
 * locking rider controls. Sound effects are triggered via animation keyframes.
 *
 * Animation timing (grounded):
 * - ultimate_start: 2.00s (40 ticks)
 * - ultimate: 5.42s (108 ticks)
 * - ultimate_end: 1.25s (25 ticks)
 * Total: ~8.67 seconds
 *
 * Animation timing (air):
 * - ultimate_start_air: 1.46s (29 ticks)
 * - ultimate_air: 5.42s (108 ticks)
 * - ultimate_end_air: 1.46s (29 ticks)
 * Total: ~8.34 seconds
 */
public class IgnivorusUltimateAbility extends DragonAbility<Ignivorus> {
    // Ground ultimate timings
    private static final int ULTIMATE_START_TICKS = 40;      // 2.00s animation.ignivorus.ultimate_start
    private static final int ULTIMATE_LOOP_TICKS = 108;      // 5.42s animation.ignivorus.ultimate (same for both)
    private static final int ULTIMATE_END_TICKS = 25;        // 1.25s animation.ignivorus.ultimate_end

    // Air ultimate timings
    private static final int ULTIMATE_START_AIR_TICKS = 29;  // 1.46s animation.ignivorus.ultimate_start_air
    private static final int ULTIMATE_END_AIR_TICKS = 29;    // 1.46s animation.ignivorus.ultimate_end_air

    private static final int TOTAL_SEQUENCE_TICKS = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS; // Use ground (longer)
    private static final int COOLDOWN_TICKS_RIDER = 0; // No cooldown for riders (health penalty is enough)
    private static final int COOLDOWN_TICKS_AI = 6000; // 5 minutes (300 seconds * 20 ticks) for AI

    private static final double EXPLOSION_RADIUS = 32.0D;
    private static final float EXPLOSION_DAMAGE = 200.0F;
    private static final int EXPLOSION_FIRE_SECONDS = 8;
    private static final int LOOP_DAMAGE_INTERVAL = 5;

    // Ground ultimate effects timing
    private static final int LOOP_DAMAGE_WARMUP = 30;  // 1.5s into loop (tick 70 total: 40 start + 30)
    private static final int NOVA_SPAWN_DELAY = 70;  // 3.5s total (2s start + 1.5s into loop)

    // Air ultimate effects timing (0.63s into loop)
    private static final int LOOP_DAMAGE_WARMUP_AIR = 13;  // 0.63s into loop (tick 42 total: 29 start + 13)
    private static final int NOVA_SPAWN_DELAY_AIR = 42;  // 2.09s total (1.46s start + 0.63s into loop)

    // Phase 2 ground mode timing
    private static final int PHASE2_DAMAGE_DELAY = 10;
    private static final int PHASE2_NOVA_SPAWN_DELAY = 13;  // 0.65s for Phase 2 ground mode
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
    private boolean isPhase2GroundMode; // Instant attack mode for Phase 2 ground
    private boolean isAirborneMode; // Track if this execution is airborne (air variants)
    private boolean phase2DamageApplied; // Track if Phase 2 damage has been applied
    private boolean novaSpawned; // Track if nova entity has been spawned

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        // Riders have no cooldown (health penalty is sufficient), AI gets full cooldown
        super(type, user, TRACK, user.getControllingPassenger() != null ? COOLDOWN_TICKS_RIDER : COOLDOWN_TICKS_AI);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();

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
            // Check if dragon is airborne (flying or in air)
            boolean isAirborne = dragon.isFlying() || !dragon.onGround();
            isAirborneMode = isAirborne; // Store for duration of ability

            // Check if in Phase 2 ground mode (instant attack)
            isPhase2GroundMode = dragon.isPhase2Active() && !isAirborne;

            if (isPhase2GroundMode) {
                // Phase 2 ground ultimate - quick attack with 5 tick delay
                dragon.lockRiderControls(ULTIMATE_LOOP_TICKS); // 5.42 seconds (108 ticks)
                lockedControls = true;

                // Lock movement
                dragon.markLandedNow();
                dragon.setHovering(false);
                dragon.setLanding(false);
                dragon.setTakeoff(false);
                dragon.setDeltaMovement(Vec3.ZERO);

                dragon.setUltimateCameraZoomActive(true);

                // Play Phase 2 ultimate animation
                dragon.triggerAnim("action", "phase2_ultimate");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_AIR.get(), 1.0f, 1.0f, 127);
                }

                // Initialize damage tracking flag
                phase2DamageApplied = false;
                novaSpawned = false;

                applyPenaltyHealth(dragon);
            } else {
                // Normal ultimate or Phase 2 air ultimate - use multi-stage animation
                int totalTicks = isAirborne
                    ? (ULTIMATE_START_AIR_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_AIR_TICKS)
                    : (ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS);
                dragon.lockRiderControls(totalTicks);
                lockedControls = true;

                if (!isAirborne) {
                    // Ground version - lock movement
                    dragon.markLandedNow();
                    dragon.setHovering(false);
                    dragon.setLanding(false);
                    dragon.setTakeoff(false);
                    dragon.setDeltaMovement(Vec3.ZERO);
                }

                dragon.setUltimateCameraZoomActive(true);

                // Reset animation tracking flags
                startAnimPlayed = false;
                loopAnimPlayed = false;
                endAnimPlayed = false;
                lastLoopDamageTick = -LOOP_DAMAGE_INTERVAL;
                penaltyApplied = false;
                novaSpawned = false;

                // Play ONLY the first animation (start) - use _air variant if airborne
                if (isAirborne) {
                    dragon.triggerAnim("action", "ultimate_start_air");
                    if (!dragon.level().isClientSide) {
                        dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_START_AIR.get(), 1.0f, 1.0f, 54);
                    }
                } else {
                    dragon.triggerAnim("action", "ultimate_start");
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

        // Handle Phase 2 ground mode damage with delay, then end early
        if (isPhase2GroundMode) {
            // Spawn nova entity after delay (0.65 seconds)
            if (!novaSpawned && ticks >= PHASE2_NOVA_SPAWN_DELAY) {
                spawnNovaEntity();
                novaSpawned = true;
            }

            if (!phase2DamageApplied && ticks >= PHASE2_DAMAGE_DELAY) {
                triggerRingExplosion(true);
                phase2DamageApplied = true;
            }
            // End ability after animation completes (108 ticks)
            if (ticks >= ULTIMATE_LOOP_TICKS) {
                end();
            }
            return;
        }

        Ignivorus dragon = getUser();

        // Calculate timing based on air vs ground mode
        int startEndTick = isAirborneMode ? ULTIMATE_START_AIR_TICKS : ULTIMATE_START_TICKS;
        int loopEndTick = startEndTick + ULTIMATE_LOOP_TICKS;
        int novaDelay = isAirborneMode ? NOVA_SPAWN_DELAY_AIR : NOVA_SPAWN_DELAY;
        int damageWarmup = isAirborneMode ? LOOP_DAMAGE_WARMUP_AIR : LOOP_DAMAGE_WARMUP;

        // Manually trigger each animation when the previous one finishes
        // This prevents gaps/flickers between animations

        if (!loopAnimPlayed && ticks >= startEndTick) {
            if (isAirborneMode) {
                dragon.triggerAnim("action", "ultimate_air");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_AIR.get(), 1.0f, 1.0f, 112);
            } else {
                dragon.triggerAnim("action", "ultimate");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE.get(), 1.0f, 1.0f, 127);
            }
            loopAnimPlayed = true;
        }

        // Spawn nova entity at the configured delay (from start of ability)
        if (!novaSpawned && ticks >= novaDelay) {
            spawnNovaEntity();
            novaSpawned = true;
        }

        // Apply continuous ring-of-fire damage only while the "ultimate" animation plays
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
                dragon.triggerAnim("action", "ultimate_end_air");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_END_AIR.get(), 1.0f, 1.0f, 38);
            } else {
                dragon.triggerAnim("action", "ultimate_end");
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_ULTIMATE_END.get(), 1.0f, 1.0f, 57);
            }
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

        // Spawn flame burst with randomized directions.
        int flameCount = 32;
        double flameSpeed = 1.2;
        float flameScale = 2.0F;
        int flameLifetime = 30;
        // Keep burst flames visual-only so ultimate damage is controlled by the configured ultimate value.
        float flameDamage = 0.0F;

        var random = dragon.getRandom();
        Vec3 spawnPos = center.add(0, 4.0, 0);

        for (int i = 0; i < flameCount; i++) {
            // Random unit vector biased slightly upward for a dramatic burst.
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

        if (openingPulse) {
            // Vanilla particles removed; custom entities handle visuals.
        }

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
                    // Don't damage self
                    if (target == getUser()) return false;

                    // Don't damage baby Ignivorus dragons (protect the young!)
                    if (target instanceof Ignivorus baby && baby.isBaby()) return false;

                    // Only damage alive, attackable entities that aren't allies
                    return target.isAlive() && target.attackable() && !getUser().isAlly(target);
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
