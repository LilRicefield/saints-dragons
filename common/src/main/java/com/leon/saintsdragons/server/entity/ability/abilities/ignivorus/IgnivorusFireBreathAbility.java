package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.particle.FireBreathBurstData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final int RIDER_ACTIVE_TICKS = 240;
    private static final int AI_ACTIVE_TICKS = 240;
    private static final int BLOCK_BREAK_START_TICKS = 120;
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
    private boolean openingBurstEmitted = false;
    private boolean aiControlled;
    private int aiActiveTicks;
    private int lostShotTicks;
    private static final double AI_START_GAP = 10.0D;
    private static final double AI_STOP_GAP = 6.0D;
    private static final int AI_LOST_SHOT_GRACE_TICKS = 12;

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
        if (section.sectionType == STARTUP) {
            aiControlled = dragon.getControllingPassenger() == null;
            aiActiveTicks = aiControlled ? 80 + dragon.getRandom().nextInt(81) : RIDER_ACTIVE_TICKS;
            lostShotTicks = 0;
            if (aiControlled) dragon.setAiFireBreathDecision("windup");
        }
        if (!dragon.level().isClientSide && aiControlled && !canContinueAiBreath(false)) {
            interrupt();
            return;
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
            openingBurstEmitted = false;
            if (aiControlled) dragon.setAiFireBreathDecision("breathing");
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
            if (aiControlled) dragon.setAiFireBreathDecision("burst-complete");
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

    @Override
    public void end() {
        Ignivorus dragon = getUser();
        if (isUsing() && aiControlled && !dragon.level().isClientSide) {
            // Shared across ground/air combat, and measured from completion or cancellation.
            dragon.getAiCombatPacing().recordUse(getAbilityType(), 10,
                    400 + dragon.getRandom().nextInt(201), false, 0, 0);
        }
        super.end();
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
        if (section == null) return;
        Ignivorus dragon = getUser();
        if (!dragon.level().isClientSide && aiControlled && !canContinueAiBreath(section.sectionType == ACTIVE)) {
            interrupt();
            return;
        }
        if (section.sectionType != ACTIVE) return;
        if (!dragon.level().isClientSide) {
            float drain = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_drain_per_tick", DEFAULT_FIRE_BREATH_DRAIN_PER_TICK);
            if (dragon.isFireBreathDepleted() || dragon.getFireBreathEnergy() <= 0) {
                if (aiControlled) dragon.setAiFireBreathDecision("energy-depleted");
                interrupt();
                return;
            }
            dragon.drainFireBreathEnergy(drain);
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
        dragon.emitFireBreathSection(origin, aim, getTicksInSection() >= BLOCK_BREAK_START_TICKS);
        if (!openingBurstEmitted && dragon.level() instanceof ServerLevel level) {
            openingBurstEmitted = true;
            for (ServerPlayer viewer : level.players()) {
                if (viewer.distanceToSqr(origin) <= 128.0 * 128.0) {
                    level.sendParticles(viewer, new FireBreathBurstData(dragon.getId()), true,
                            origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static boolean canStartAiBreath(Ignivorus dragon, LivingEntity target) {
        return isValidTarget(target) && dragon.isTargetValid(target)
                && dragon.canUseFireBreath() && dragon.getFireBreathEnergy() >= 0.4F
                && bodyGap(dragon, target) >= AI_START_GAP
                && dragon.hasAiFireBreathShot(target, ExpandingBreathSection.DEFAULT_RANGE * 0.85D);
    }

    private boolean canContinueAiBreath(boolean active) {
        Ignivorus dragon = getUser();
        LivingEntity target = dragon.getTarget();
        if (dragon.getControllingPassenger() != null || !isValidTarget(target)
                || !dragon.isTargetValid(target)) return stopAiBreath("target-lost");
        if (bodyGap(dragon, target) <= AI_STOP_GAP) return stopAiBreath("close-melee");
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        double horizontalGap = Math.sqrt(dx * dx + dz * dz)
                - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        if (!dragon.isAerial() && horizontalGap <= 2.0D) return stopAiBreath("target-underneath");
        if (active && getTicksInSection() >= aiActiveTicks) return stopAiBreath("burst-complete");
        boolean clearShot = dragon.hasAiFireBreathShot(target, ExpandingBreathSection.DEFAULT_RANGE);
        lostShotTicks = clearShot ? 0 : lostShotTicks + 1;
        return lostShotTicks < AI_LOST_SHOT_GRACE_TICKS || stopAiBreath("shot-lost");
    }

    private boolean stopAiBreath(String reason) {
        getUser().setAiFireBreathDecision(reason);
        return false;
    }

    private static double bodyGap(Ignivorus dragon, LivingEntity target) {
        return Math.max(0.0D, dragon.distanceTo(target) - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D);
    }

    private static boolean isValidTarget(LivingEntity target) {
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
