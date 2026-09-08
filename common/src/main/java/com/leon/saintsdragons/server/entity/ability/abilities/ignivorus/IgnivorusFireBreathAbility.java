package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;


public class IgnivorusFireBreathAbility extends DragonAbility<Ignivorus> {

    private static final int STARTUP_TICKS = 9;
    private static final int RIDER_ACTIVE_TICKS = 160;
    private static final int AI_ACTIVE_TICKS = 80;
    private static final int COOLDOWN_TICKS = 40;
    private static final float DEFAULT_FIRE_BREATH_DRAIN_PER_TICK = 1.0f / RIDER_ACTIVE_TICKS;
    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, RIDER_ACTIVE_TICKS)
    };

    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, AI_ACTIVE_TICKS)
    };

    private boolean breathStartPlayed = false;
    private boolean breathLoopActive = false;

    public IgnivorusFireBreathAbility(DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> type,
                                      Ignivorus user) {
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

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
            breathStartPlayed = true;
            breathLoopActive = false;
            dragon.setBreathingFire(false);
            dragon.setFireBreathProgress(0);
            dragon.clearFireBreathPath();
            dragon.triggerAnim(IgnivorusAnimationHandler.ACTION_CONTROLLER, "fire_breath_start");
            if (!dragon.level().isClientSide) {
                float pitch = 0.92f + dragon.getRandom().nextFloat() * 0.15f;
                dragon.playSound(ModSounds.IGNIVORUS_FIRE_BREATH_START.get(), 2.0f, pitch);
            }

        } else if (section.sectionType == ACTIVE) {
            dragon.setBreathingFire(true);
            dragon.triggerAnim(IgnivorusAnimationHandler.ACTION_CONTROLLER, "fire_breathing");
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
        dragon.setFireBreathProgress(0);
        dragon.clearFireBreathPath();
        triggerBreathStop(dragon);
        super.interrupt();
    }

    private void triggerBreathStop(Ignivorus dragon) {
        if (breathLoopActive || breathStartPlayed) {
            dragon.triggerAnim(IgnivorusAnimationHandler.ACTION_CONTROLLER, "fire_breath_stop");
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
        if (!dragon.isTame() && dragon.getControllingPassenger() == null) {
            if (!isValidTarget(dragon.getTarget())) {
                interrupt();
                return;
            }
        }
        if (!dragon.level().isClientSide) {
            float drain = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_drain_per_tick", DEFAULT_FIRE_BREATH_DRAIN_PER_TICK);
            if (!dragon.drainFireBreathEnergy(drain)) {
                interrupt();
                return;
            }
        }
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
        dragon.syncFireBreathPath(origin, origin.add(aim.normalize().scale(ExpandingBreathSection.DEFAULT_RANGE)));
        dragon.emitFireBreathSection(origin, aim);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        if (target.isRemoved()) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }
}
