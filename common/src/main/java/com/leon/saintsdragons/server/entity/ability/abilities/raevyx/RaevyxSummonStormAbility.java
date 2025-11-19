package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.sounds.SoundEvents;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Ultimate: Summon Storm
 * - Roars into the sky, triggers a thunderstorm in the current dimension
 * - Supercharges the wyvern for 2 minutes: x2 damage on abilities
 * - Cooldown: 4 minutes
 */
public class RaevyxSummonStormAbility extends DragonAbility<Raevyx> {
    private static final int SUPERCHARGE_TICKS = 20 * 120; // 120s
    private static final int COOLDOWN_TICKS = 20 * 240; // 240s
    private static final int GROUND_START_TICKS = 38; // 1.88s animation.raevyx.summon_storm_ground_start
    private static final int GROUND_LOOP_TICKS = 114; // 5.71s animation.raevyx.summon_storm_ground
    private static final int GROUND_END_TICKS = 38; // 1.88s animation.raevyx.summon_storm_ground_end
    private static final int GROUND_TOTAL_SEQUENCE_TICKS = GROUND_START_TICKS + GROUND_LOOP_TICKS + GROUND_END_TICKS;
    private static final int AIR_ANIMATION_TICKS = 145; // 7.25 seconds animation.raevyx.summon_storm_air
    private static final int SCREEN_SHAKE_TRIGGER_TICK = 35; // 1.76 seconds (35.2 ticks)

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, GROUND_TOTAL_SEQUENCE_TICKS), // covers full ground sequence
            new AbilitySectionInstant(AbilitySectionType.ACTIVE),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 20) // small tail to keep action controller busy
    };

    private boolean isGroundCast;
    private boolean startAnimPlayed;
    private boolean loopAnimPlayed;
    private boolean endAnimPlayed;
    private boolean screenShakeActive;
    private int activeStartupDuration = GROUND_TOTAL_SEQUENCE_TICKS;

    public RaevyxSummonStormAbility(DragonAbilityType<Raevyx, RaevyxSummonStormAbility> type, Raevyx user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public void tickUsing() {
        // Only shake while the ground loop animation is running
        if (screenShakeActive && !getUser().level().isClientSide) {
            getUser().triggerScreenShake(1.5F);
        }

        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.STARTUP) {
            return;
        }

        int ticks = getTicksInSection();
        if (isGroundCast) {
            // Trigger loop/end animations when each phase completes
            if (!startAnimPlayed) {
                getUser().triggerAnim("action", "summon_storm_ground_start");
                startAnimPlayed = true;
            }
            if (!loopAnimPlayed && ticks >= GROUND_START_TICKS) {
                getUser().triggerAnim("action", "summon_storm_ground");
                loopAnimPlayed = true;
                screenShakeActive = true;
            }
            if (!endAnimPlayed && ticks >= GROUND_START_TICKS + GROUND_LOOP_TICKS) {
                getUser().triggerAnim("action", "summon_storm_ground_end");
                endAnimPlayed = true;
                screenShakeActive = false;
            }
        }

        if (ticks >= activeStartupDuration) {
            nextSection();
        }
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Determine if this is a ground or air cast
            isGroundCast = !getUser().isFlying();
            activeStartupDuration = isGroundCast ? GROUND_TOTAL_SEQUENCE_TICKS : AIR_ANIMATION_TICKS;

            // Grant invulnerability for the full animation duration
            getUser().startTemporaryInvuln(activeStartupDuration);

            // Lock controls ONLY for ground cast - air cast allows free movement
            if (isGroundCast) {
                getUser().lockRiderControls(activeStartupDuration);
            }
            getUser().lockTakeoff(activeStartupDuration);

            // Play appropriate animation variant (sound is handled by keyframe at 1.76s)
            if (isGroundCast) {
                getUser().triggerAnim("action", "summon_storm_ground_start");
                startAnimPlayed = true;
                loopAnimPlayed = false;
                endAnimPlayed = false;
                screenShakeActive = false;
            } else {
                getUser().triggerAnim("action", "summon_storm_air");
                startAnimPlayed = false;
                loopAnimPlayed = false;
                endAnimPlayed = false;
                screenShakeActive = false;
            }
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (!getLevel().isClientSide) {
                // Apply supercharge
                getUser().startSupercharge(SUPERCHARGE_TICKS);

                // Force thunderstorm in this dimension for ~2 minutes
                if (getLevel() instanceof ServerLevel server) {
                    var ld = server.getLevelData();
                    if (ld instanceof ServerLevelData data) {
                        data.setRaining(true);
                        data.setRainTime(SUPERCHARGE_TICKS);
                        data.setThundering(true);
                        data.setThunderTime(SUPERCHARGE_TICKS);
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
        super.end();
    }

    private void releaseLocks() {
        getUser().clearTakeoffLock();
        if (isGroundCast) {
            getUser().clearRiderControlLock();
        }
        screenShakeActive = false;
    }
}
