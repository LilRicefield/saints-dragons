package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import net.minecraft.server.level.ServerLevel;
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
    private static final int STARTUP_TICKS = 4;  // ~75ms (was 18, now synced to animation)
    private static final int RIDER_ACTIVE_TICKS = 400;  // ~20 seconds for riders
    private static final int AI_ACTIVE_TICKS = 80;      // 4 seconds for AI
    private static final int COOLDOWN_TICKS = 40;

    private static final double MAX_RANGE = 64.0D;  // Must match layer's MAX_VISUAL_DISTANCE!
    private static final double IMPACT_RADIUS = 1.25D;
    private static final float DEFAULT_DAMAGE_PER_SECOND = 80.0F;  // Config value = damage per second
    private static final int FIRE_DURATION_SECONDS = 3;

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
            // Play startup animation but don't show fire cone yet
            breathStartPlayed = true;
            breathLoopActive = false;
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

        // Only apply destruction when stream has extended (progress > 10)
        // This prevents instant block burning at full range
        if (currentProgress > 10 && dragon.level() instanceof ServerLevel serverLevel) {
            double sizeScale = Math.max(0.8D, dragon.getBbWidth());
            float damage = computeDamage(dragon, sizeScale);
            double radius = IMPACT_RADIUS * sizeScale;

            // Calculate current impact point based on progress (0-40 → 0.0-1.0)
            double progressRatio = Math.min(1.0, currentProgress / 40.0);
            Vec3 currentImpact = origin.add(impact.subtract(origin).scale(progressRatio));

            DragonDestructionManager.applyFireBreathImpact(serverLevel, dragon, currentImpact, radius, damage, FIRE_DURATION_SECONDS);
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
