package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.util.animation.AnimationHelper;
import com.leon.saintsdragons.server.data.RaevyxStormSavedData;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.sounds.SoundEvents;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

public class RaevyxSummonStormAbility extends DragonAbility<Raevyx> {
    private static final int DEFAULT_SUPERCHARGE_TICKS = 20 * 60;
    private static final int DEFAULT_COOLDOWN_TICKS = 20 * 240;
    private static final int MIN_SUPERCHARGE_TICKS = 20;
    private static final int MIN_COOLDOWN_TICKS = 20;
    public record CastTiming(int castTicks, float introTicks, int burstTicks, int soundTicks,
                             int shakeStartTick, int extraShakeTick) {
        public float chargeTick() { return introTicks + burstTicks; }
        public float starFlashTick() { return introTicks - 7.0F; }
    }

    public static final CastTiming GROUND_TIMING = new CastTiming(125, 50, 10, 140, 49, 38);
    public static final CastTiming AIR_TIMING = new CastTiming(110, 38.4F, 8, 115, 35, -1);

    public static CastTiming timingFor(boolean airCast) {
        return airCast ? AIR_TIMING : GROUND_TIMING;
    }

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(AbilitySectionType.STARTUP),
            new AbilitySectionInstant(AbilitySectionType.ACTIVE),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 20)
    };

    private boolean isGroundCast;
    private boolean groundExtraShakeTriggered;
    private boolean superchargeStarted;
    private CastTiming timing = GROUND_TIMING;

    public RaevyxSummonStormAbility(DragonAbilityType<Raevyx, RaevyxSummonStormAbility> type, Raevyx user) {
        super(type, user, TRACK, 0);
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.STARTUP) {
            return;
        }

        if (getLevel().isClientSide) return;
        float ticks = getUser().getStormCastAge(0);
        if (ticks >= timing.chargeTick()) commitSupercharge();
        if (timing.extraShakeTick() >= 0 && !groundExtraShakeTriggered
                && ticks >= timing.extraShakeTick()) {
            groundExtraShakeTriggered = true;
            getUser().triggerScreenShake(1.8F);
        }
        if (ticks >= timing.shakeStartTick() && ticks < timing.castTicks()) {
            getUser().triggerScreenShake(1.5F);
        }
        if (ticks >= timing.castTicks()) {
            nextSection();
        }
    }

    private void commitSupercharge() {
        if (superchargeStarted) return;
        // Interruptions before this point grant nothing. Afterward the earned charge remains.
        getUser().startSupercharge(getConfiguredSuperchargeTicks());
        superchargeStarted = true;
    }

    @Override
    protected boolean canContinueUsing() {
        return getUser().isAlive();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            superchargeStarted = false;
            isGroundCast = !getUser().isFlying();
            timing = timingFor(!isGroundCast);
            getUser().setStormVisuals(true, !isGroundCast);
            getUser().startTemporaryInvuln(timing.castTicks());
            getUser().lockRiderControls(timing.castTicks());
            getUser().lockTakeoff(timing.castTicks());
            groundExtraShakeTriggered = false;

            if (isGroundCast) {
                getUser().triggerAnim(RaevyxAnimationHandler.ACTION_CONTROLLER, "summon_storm");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_SUMMON_STORM.get(), 1.6f, 1.0f, timing.soundTicks());
                }
            } else {
                getUser().triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, "summon_storm_air");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_SUMMON_STORM_AIR.get(), 1.6f, 1.0f, timing.soundTicks());
                }
            }
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (!getLevel().isClientSide) {
                int stormDurationTicks = getConfiguredStormDurationTicks();

                if (getLevel() instanceof ServerLevel server) {
                    var ld = server.getLevelData();
                    if (ld instanceof ServerLevelData data) {
                        RaevyxStormSavedData.beforeSummoning(server);
                        int rainTicks = data.isRaining() ? Math.max(data.getRainTime(), stormDurationTicks) : stormDurationTicks;
                        int thunderTicks = data.isThundering() ? Math.max(data.getThunderTime(), stormDurationTicks) : stormDurationTicks;
                        server.setWeatherParameters(0, stormDurationTicks, true, true);
                        data.setRainTime(rainTicks);
                        data.setThunderTime(thunderTicks);
                    }

                    server.playSound(null, getUser().blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.WEATHER, 6.0f, 0.9f);
                }
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == AbilitySectionType.STARTUP) {
            releaseLocks();
        }
    }

    @Override
    public void end() {
        releaseLocks();
        getUser().clearTemporaryInvuln();
        super.end();
    }

    private void releaseLocks() {
        getUser().setStormVisuals(false, false);
        getUser().clearTakeoffLock();
        getUser().clearRiderControlLock();
        groundExtraShakeTriggered = false;
    }

    @Override
    public int getMaxCooldown() {
        return getConfiguredCooldownTicks();
    }

    private int getConfiguredSuperchargeTicks() {
        int ticks = (int) Math.round(DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("summon_storm_supercharge_ticks", DEFAULT_SUPERCHARGE_TICKS));
        return Math.max(MIN_SUPERCHARGE_TICKS, ticks);
    }

    private int getConfiguredCooldownTicks() {
        int ticks = (int) Math.round(DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("summon_storm_cooldown_ticks", DEFAULT_COOLDOWN_TICKS));
        return Math.max(MIN_COOLDOWN_TICKS, ticks);
    }

    private int getConfiguredStormDurationTicks() {
        int ticks = (int) Math.round(DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("summon_storm_duration_ticks", DEFAULT_SUPERCHARGE_TICKS));
        return Math.max(MIN_SUPERCHARGE_TICKS, ticks);
    }
}
