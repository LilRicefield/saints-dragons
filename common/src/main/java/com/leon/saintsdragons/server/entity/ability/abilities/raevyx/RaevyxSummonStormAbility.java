package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.sounds.SoundEvents;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Ultimate: Summon Storm
 * - Roars into the sky, triggers a thunderstorm in the current dimension
 * - Supercharges the wyvern for 1 minute: x2 damage on abilities
 * - Cooldown: 4 minutes
 */
public class RaevyxSummonStormAbility extends DragonAbility<Raevyx> {
    private static final int DEFAULT_SUPERCHARGE_TICKS = 20 * 60;
    private static final int DEFAULT_COOLDOWN_TICKS = 20 * 240;
    private static final int MIN_SUPERCHARGE_TICKS = 20;
    private static final int MIN_COOLDOWN_TICKS = 20;
    private static final int GROUND_ONE_SHOT_TICKS = 125;
    private static final int GROUND_SOUND_TICKS = 140;
    private static final int GROUND_SHAKE_START_TICKS = 53;
    private static final int AIR_SHAKE_START_TICKS = 35;
    private static final int AIR_ONE_SHOT_TICKS = 110;
    private static final int AIR_SOUND_TICKS = 115;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, AIR_ONE_SHOT_TICKS),
            new AbilitySectionInstant(AbilitySectionType.ACTIVE),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 20)
    };

    private boolean isGroundCast;
    private boolean screenShakeActive;
    private int activeStartupDuration = AIR_ONE_SHOT_TICKS;

    public RaevyxSummonStormAbility(DragonAbilityType<Raevyx, RaevyxSummonStormAbility> type, Raevyx user) {
        super(type, user, TRACK, 0);
    }

    @Override
    public void tickUsing() {
        // Only shake while the loop animation is running
        if (screenShakeActive && !getUser().level().isClientSide) {
            getUser().triggerScreenShake(1.5F);
        }

        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.STARTUP) {
            return;
        }

        int ticks = getTicksInSection();
        if (isGroundCast) {
            if (!screenShakeActive && ticks >= GROUND_SHAKE_START_TICKS) {
                screenShakeActive = true;
            }
            if (ticks >= activeStartupDuration) {
                nextSection();
            }
            return;
        }

        if (!screenShakeActive && ticks >= AIR_SHAKE_START_TICKS) {
            screenShakeActive = true;
        }

        if (ticks >= activeStartupDuration) {
            screenShakeActive = false;
            nextSection();
        }
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Determine if this is a ground or air cast
            isGroundCast = !getUser().isFlying();
            activeStartupDuration = isGroundCast ? GROUND_ONE_SHOT_TICKS : AIR_ONE_SHOT_TICKS;

            // Grant invulnerability for the full animation duration
            getUser().startTemporaryInvuln(activeStartupDuration);

            // Lock controls to mirror cinematic handling regardless of flight state
            getUser().lockRiderControls(activeStartupDuration);
            getUser().lockTakeoff(activeStartupDuration);

            if (isGroundCast) {
                getUser().triggerAnim("action", "summon_storm");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_SUMMON_STORM.get(), 1.6f, 1.0f, GROUND_SOUND_TICKS);
                }
                screenShakeActive = false;
            } else {
                getUser().triggerAnim("action", "summon_storm_air");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_SUMMON_STORM_AIR.get(), 1.6f, 1.0f, AIR_SOUND_TICKS);
                }
                screenShakeActive = false;
            }
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (!getLevel().isClientSide) {
                int superchargeTicks = getConfiguredSuperchargeTicks();
                int stormDurationTicks = getConfiguredStormDurationTicks();
                // Apply supercharge
                getUser().startSupercharge(superchargeTicks);

                // Force thunderstorm in this dimension for at least 1 minute
                if (getLevel() instanceof ServerLevel server) {
                    var ld = server.getLevelData();
                    if (ld instanceof ServerLevelData data) {
                        // Always ensure rain and thunder are active, extending duration if needed
                        if (!data.isRaining()) {
                            data.setRaining(true);
                        }
                        data.setRainTime(Math.max(data.getRainTime(), stormDurationTicks));

                        // Force thundering state - always set to true to ensure it triggers
                        if (!data.isThundering()) {
                            data.setThundering(true);
                        }
                        data.setThunderTime(Math.max(data.getThunderTime(), stormDurationTicks));

                        // Sync weather to all clients
                        server.setWeatherParameters(0, stormDurationTicks, true, true);
                    }

                    // Dramatic thunder sound cue
                    server.playSound(null, getUser().blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            net.minecraft.sounds.SoundSource.WEATHER, 6.0f, 0.9f);
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
        // Clear temporary invulnerability to prevent it from persisting if ability is interrupted
        getUser().clearTemporaryInvuln();
        super.end();
    }

    private void releaseLocks() {
        getUser().clearTakeoffLock();
        getUser().clearRiderControlLock();
        screenShakeActive = false;
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
