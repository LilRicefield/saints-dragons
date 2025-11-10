package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 */
public class IgnivorusFireBreathAbility extends DragonAbility<Ignivorus> {

    private static final int STARTUP_TICKS = 18;
    private static final int ACTIVE_TICKS = 400;
    private static final int COOLDOWN_TICKS = 40;

    private static final double MAX_RANGE = 64.0D;  // Must match layer's MAX_VISUAL_DISTANCE!
    private static final double IMPACT_RADIUS = 1.25D;
    private static final float BASE_DAMAGE = 4.0F;
    private static final int FIRE_DURATION_SECONDS = 3;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS)
    };

    public IgnivorusFireBreathAbility(DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> type,
                                      Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().setBreathingFire(false);
            getUser().setFireBreathProgress(0);  // Reset progress on startup
            getUser().clearFireBreathPath();
        } else if (section.sectionType == ACTIVE) {
            getUser().setBreathingFire(true);
        }
    }

    @Override
    protected void endSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == ACTIVE) {
            getUser().setBreathingFire(false);
            getUser().clearFireBreathPath();
        }
    }

    @Override
    public void interrupt() {
        getUser().setBreathingFire(false);
        getUser().setFireBreathProgress(0);  // Reset progress on interrupt
        getUser().clearFireBreathPath();
        super.interrupt();
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

    private static float computeDamage(Ignivorus dragon, double sizeScale) {
        double attackValue = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float scaled = (float) (BASE_DAMAGE + attackValue * 0.25F);
        return scaled * (float) (0.65D + (sizeScale * 0.2D));
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
}
