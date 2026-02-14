package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFlameEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Continuous fire-breath ability for Ignivorus.
 * Holds while the rider presses the tertiary key (default: G) and
 * applies block ignition + entity damage along the rider's look direction.
 *
 * Animation flow (similar to Raevyx beam):
 * - STARTUP (4 ticks/75ms): Plays fire_breath_starts animation, NO fire cone yet
 * - ACTIVE (400 ticks): Loops fire_breathing animation, fire cone renders
 */
public class IgnivorusFireBreathAbility extends DragonAbility<Ignivorus> {

    // Animation timing: 75ms startup matches fire_breath_starts animation duration
    private static final int STARTUP_TICKS = 9;
    private static final int RIDER_ACTIVE_TICKS = 160;  // ~8 seconds for riders
    private static final int AI_ACTIVE_TICKS = 80;      // 4 seconds for AI
    private static final int COOLDOWN_TICKS = 40;
    private static final float DEFAULT_FIRE_BREATH_DRAIN_PER_TICK = 1.0f / RIDER_ACTIVE_TICKS;

    private static final double MAX_RANGE = 64.0D;  // Must match layer's MAX_VISUAL_DISTANCE!
    private static final double IMPACT_RADIUS = 1.25D;
    private static final float DEFAULT_DAMAGE_PER_SECOND = 80.0F;  // Config value = damage per second
    private static final int FIRE_DURATION_SECONDS = 3;

    // Flame spawn offset tuning (relative to fireBone position)
    // Adjust these to fine-tune where flames appear relative to the mouth
    private static final double FLAME_SPAWN_OFFSET_FORWARD = 0.0;  // Blocks forward (+ = forward, - = backward)
    private static final double FLAME_SPAWN_OFFSET_UP = 0.0;       // Blocks up (+ = up, - = down)
    private static final double FLAME_SPAWN_OFFSET_RIGHT = 0.0;    // Blocks right (+ = right, - = left)

    // Block destruction settings
    private static final int ABILITY_ACTIVE_BEFORE_MELTING = 80;  // Ability must be active for 4 seconds before melting starts
    private static final int BLOCK_MELT_TICKS = 40;  // Each block takes 2 seconds of continuous exposure to melt
    private static final int FLAME_SPAWN_MIN = 3;
    private static final int FLAME_SPAWN_MAX = 5;
    private static final double DEFAULT_FLAME_SPAWN_MULTIPLIER = 1.0D;
    private static final double DEFAULT_FLAME_SPEED_MULTIPLIER = 1.0D;
    private static final double DEFAULT_FLAME_LIFETIME_MULTIPLIER = 1.0D;

    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, RIDER_ACTIVE_TICKS)
    };

    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, AI_ACTIVE_TICKS)
    };

    // Animation state tracking (follows Raevyx pattern)
    private boolean breathStartPlayed = false;
    private boolean breathLoopActive = false;

    // Track total active ticks for ability-wide block destruction
    private int totalActiveTicks = 0;

    public IgnivorusFireBreathAbility(DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> type,
                                      Ignivorus user) {
        // Choose track based on whether user has a controlling passenger
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

        // Check if target is still valid for AI
        if (!dragon.isTame() && dragon.getControllingPassenger() == null) {
            if (!isValidTarget(dragon.getTarget())) {
                interrupt();
                return;
            }
        }

        if (section.sectionType == STARTUP) {
            if (!dragon.canUseFireBreath()) {
                interrupt();
                return;
            }
            // Play startup animation but don't show fire cone yet
            breathStartPlayed = true;
            breathLoopActive = false;
            totalActiveTicks = 0;  // Reset total active time
            dragon.setBreathingFire(false);  // NO fire cone during startup
            dragon.setFireBreathProgress(0);  // Reset progress
            dragon.clearFireBreathPath();
            dragon.triggerAnim("action", "fire_breath_start");  // Play 75ms start animation
            if (!dragon.level().isClientSide) {
                float pitch = 0.92f + dragon.getRandom().nextFloat() * 0.15f;
                dragon.playSound(ModSounds.IGNIVORUS_FIRE_BREATH_START.get(), 2.0f, pitch);
            }

        } else if (section.sectionType == ACTIVE) {
            // Start showing fire cone and loop breathing animation
            dragon.setBreathingFire(true);  // NOW spawn fire cone
            dragon.triggerAnim("action", "fire_breathing");  // Loop breathing animation
            breathLoopActive = true;
        }
    }

    @Override
    protected void endSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE) {
            Ignivorus dragon = getUser();
            dragon.setBreathingFire(false);
            dragon.clearFireBreathPath();
            triggerBreathStop(dragon);
        }
    }

    @Override
    public void interrupt() {
        Ignivorus dragon = getUser();
        dragon.setBreathingFire(false);
        dragon.setFireBreathProgress(0);  // Reset progress on interrupt
        dragon.clearFireBreathPath();
        totalActiveTicks = 0;  // Reset active time
        triggerBreathStop(dragon);
        super.interrupt();
    }

    /**
     * Triggers the fire breath stop animation to cleanly exit the breathing loop.
     * Only plays if breath was actually active (start or loop played).
     */
    private void triggerBreathStop(Ignivorus dragon) {
        if (breathLoopActive || breathStartPlayed) {
            dragon.triggerAnim("action", "fire_breath_stop");
            if (!dragon.level().isClientSide) {
                float pitch = 0.92f + dragon.getRandom().nextFloat() * 0.15f;
                dragon.playSound(ModSounds.IGNIVORUS_FIRE_BREATH_END.get(), 2.0f, pitch);
            }
        }
        breathStartPlayed = false;
        breathLoopActive = false;
    }

    @Override
    protected boolean canContinueUsing() {
        Ignivorus dragon = getUser();
        if (!dragon.isAlive() || dragon.isRemoved()) {
            return false;
        }
        if (dragon.isInWaterOrBubble()) {
            return false;
        }
        return true;
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        Ignivorus dragon = getUser();

        // Check if target is still valid for AI - interrupt if not
        if (!dragon.isTame() && dragon.getControllingPassenger() == null) {
            if (!isValidTarget(dragon.getTarget())) {
                interrupt();
                return;
            }
        }

        // Increment total active time - this determines when blocks start breaking
        totalActiveTicks++;
        if (!dragon.level().isClientSide) {
            float drain = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_drain_per_tick", DEFAULT_FIRE_BREATH_DRAIN_PER_TICK);
            drain = Math.max(0.0f, drain);
            if (drain > 0.0f) {
                dragon.setFireBreathEnergy(Math.max(0.0f, dragon.getFireBreathEnergy() - drain));
            }
            if (!dragon.hasFireBreathEnergy()) {
                dragon.setFireBreathDepleted(true);
                interrupt();
                return;
            }
        }

        // Increment stream progress for extending animation (0-40, like Ice & Fire)
        int currentProgress = dragon.getFireBreathProgress();
        if (currentProgress < 40) {
            dragon.setFireBreathProgress(currentProgress + 1);
        }

        Vec3 origin = dragon.getFireBreathStartAnchor(1.0f);
        if (origin == null) {
            dragon.clearFireBreathPath();
            return;
        }

        Vec3 aim = dragon.refreshFireAimDirection(origin, false);
        if (aim == null || aim.lengthSqr() < 1.0E-6) {
            dragon.clearFireBreathPath();
            return;
        }

        Vec3 impact = traceImpact(dragon, origin, aim);

        dragon.syncFireBreathPath(origin, impact);

        // Spawn flame projectile stream
        int spawnedFlames = 0;
        if (dragon.level() instanceof ServerLevel serverLevel) {
            spawnedFlames = spawnFlameProjectiles(serverLevel, dragon, origin, aim);
        }

        // Only apply destruction when stream has extended (progress > 10)
        // This prevents instant block burning at full range
        if (currentProgress > 10 && dragon.level() instanceof ServerLevel serverLevel) {
            double sizeScale = Math.max(0.8D, dragon.getBbWidth());
            double baseRadius = IMPACT_RADIUS * sizeScale;

            // Calculate current impact point based on progress (0-40 → 0.0-1.0)
            double progressRatio = Math.min(1.0, currentProgress / 40.0);
            Vec3 currentImpact = origin.add(impact.subtract(origin).scale(progressRatio));

            // Apply block effects only at the impact point to avoid excessive destruction
            boolean canMeltBlocks = totalActiveTicks >= ABILITY_ACTIVE_BEFORE_MELTING;
            // Projectile-only damage model: flame entities are the sole source of entity damage.
            // Keep impact damage at 0 to avoid look/area damage before flames visually arrive.
            float fallbackImpactDamage = 0.0f;
            DragonDestructionManager.applyFireBreathImpact(
                serverLevel,
                dragon,
                currentImpact,
                baseRadius,
                fallbackImpactDamage,
                FIRE_DURATION_SECONDS,
                BLOCK_MELT_TICKS,
                canMeltBlocks,
                false
            );
        }
    }


    /**
     * Computes damage per tick from the config value (which represents damage per second).
     * Config value is divided by 20 since this is called every tick (20 ticks = 1 second).
     * This ensures config value directly represents DPS for user-friendly configuration.
     */
    private static float computeDamage(Ignivorus dragon, double sizeScale) {
        float configDamagePerSecond = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("fire_breath", DEFAULT_DAMAGE_PER_SECOND);

        // Convert damage per second to damage per tick (20 ticks = 1 second)
        return configDamagePerSecond / 20.0F;
    }

    private Vec3 traceImpact(Ignivorus dragon, Vec3 origin, Vec3 direction) {
        Vec3 reach = origin.add(direction.scale(MAX_RANGE));
        ClipContext ctx = new ClipContext(origin, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon);
        HitResult hit = dragon.level().clip(ctx);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return reach;
        }
        return hit.getLocation();
    }

    /**
     * Spawns flame projectiles in a continuous stream to create flamethrower effect.
     */
    private int spawnFlameProjectiles(ServerLevel level, Ignivorus dragon, Vec3 origin, Vec3 direction) {
        RandomSource random = dragon.getRandom();
        double sizeScale = Math.max(0.8D, dragon.getBbWidth());
        float damagePerProjectile = computeDamage(dragon, sizeScale) * 4.0F;
        var config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double spawnMultiplier = config.extraDouble("fire_breath_flame_spawn_multiplier",
                DEFAULT_FLAME_SPAWN_MULTIPLIER);
        double speedMultiplier = config.extraDouble("fire_breath_flame_speed_multiplier",
                DEFAULT_FLAME_SPEED_MULTIPLIER);
        double lifetimeMultiplier = config.extraDouble("fire_breath_flame_lifetime_multiplier",
                DEFAULT_FLAME_LIFETIME_MULTIPLIER);
        if (spawnMultiplier <= 0.0D) {
            return 0;
        }

        int minCount = Math.max(1, (int) Math.round(FLAME_SPAWN_MIN * spawnMultiplier));
        int maxCount = Math.max(minCount, (int) Math.round(FLAME_SPAWN_MAX * spawnMultiplier));
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        int spawnedCount = 0;

        for (int i = 0; i < count; i++) {
            // Increased spread for wider cone effect
            double spreadAmount = 0.28 + random.nextDouble() * 0.22;
            Vec3 spread = new Vec3(
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount
            );

            // Faster velocity for a more forceful flamethrower
            double baseSpeed = 4.6 + random.nextDouble() * 1.8;
            double speed = Math.max(0.1D, baseSpeed * speedMultiplier);
            Vec3 velocity = direction.normalize().scale(speed).add(spread);

            // Start small, will grow to 2x size as it travels
            float scale = 1.5F + random.nextFloat() * 0.5F;
            int baseLifetime = 24 + random.nextInt(12);
            int projectileLifetime = Math.max(1, (int) Math.round(baseLifetime * lifetimeMultiplier));

            // Bias spawns forward so more flames appear near the end of the stream
            double forwardBias = 0.6 + random.nextDouble() * 0.8; // 0.6 - 1.4 blocks ahead
            Vec3 spawnPos = origin.add(direction.normalize().scale(forwardBias));
            // Apply spawn offset in dragon's local coordinate space
            spawnPos = applyLocalOffset(spawnPos, direction, FLAME_SPAWN_OFFSET_FORWARD,
                                        FLAME_SPAWN_OFFSET_UP, FLAME_SPAWN_OFFSET_RIGHT);

            IgnivorusFlameEntity flame = new IgnivorusFlameEntity(
                    level, spawnPos, velocity, dragon, damagePerProjectile, scale, projectileLifetime
            );

            if (level.addFreshEntity(flame)) {
                spawnedCount++;
            }
        }
        if (count > 0 && spawnedCount == 0 && dragon.tickCount % 20 == 0) {
            System.out.println("[IGNIVORUS_DEBUG][FIRE_BREATH] Flame spawn rejected this tick"
                    + " count=" + count
                    + " spawnMultiplier=" + spawnMultiplier
                    + " speedMultiplier=" + speedMultiplier
                    + " lifetimeMultiplier=" + lifetimeMultiplier
                    + " dragon=" + dragon.getName().getString()
                    + " pos=" + dragon.position());
        }
        return spawnedCount;
    }

    /**
     * Applies local offset (forward/up/right) to a world position based on look direction.
     * Transforms the offset from dragon's local space to world space.
     */
    private Vec3 applyLocalOffset(Vec3 origin, Vec3 lookDirection, double forward, double up, double right) {
        // If no offset, return origin directly
        if (forward == 0.0 && up == 0.0 && right == 0.0) {
            return origin;
        }

        // Normalize look direction
        Vec3 forwardVec = lookDirection.normalize();

        // Calculate right vector (perpendicular to forward, in horizontal plane)
        Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x).normalize();

        // Calculate up vector (perpendicular to both forward and right)
        Vec3 upVec = rightVec.cross(forwardVec).normalize();

        // Apply offsets in local space
        Vec3 offset = forwardVec.scale(forward)
                .add(upVec.scale(up))
                .add(rightVec.scale(right));

        return origin.add(offset);
    }

    /**
     * Checks if the target is valid for continued breathing.
     * Breath stops if target is null, dead, removed, or in creative mode.
     */
    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        if (target.isRemoved()) return false;

        // Stop breathing if target switches to creative mode
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }
}
