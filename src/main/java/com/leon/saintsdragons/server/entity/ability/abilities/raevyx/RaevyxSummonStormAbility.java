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
    private static final int ANIMATION_LENGTH_TICKS = 145; // 7.25 seconds
    private static final int SCREEN_SHAKE_TRIGGER_TICK = 35; // 1.76 seconds (35.2 ticks)

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, ANIMATION_LENGTH_TICKS), // 7.25s animation
            new AbilitySectionInstant(AbilitySectionType.ACTIVE),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 20) // small tail to keep action controller busy
    };

    private boolean isGroundCast;

    public RaevyxSummonStormAbility(DragonAbilityType<Raevyx, RaevyxSummonStormAbility> type, Raevyx user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public void tickUsing() {
        // Continuously trigger screen shake from when the roar starts (tick 35) until the end
        if (getTicksInUse() >= SCREEN_SHAKE_TRIGGER_TICK) {
            if (!getUser().level().isClientSide) {
                getUser().triggerScreenShake(1.5F); // Continuous intensity for dramatic effect
            }
        }
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Determine if this is a ground or air cast
            isGroundCast = !getUser().isFlying();

            // Grant invulnerability for the full animation duration
            getUser().startTemporaryInvuln(ANIMATION_LENGTH_TICKS);

            // Lock controls ONLY for ground cast - air cast allows free movement
            if (isGroundCast) {
                getUser().lockRiderControls(ANIMATION_LENGTH_TICKS); // 7.25 seconds
                getUser().lockTakeoff(ANIMATION_LENGTH_TICKS);
            } else {
                // Air cast: only lock takeoff to prevent awkward transitions, but allow movement
                getUser().lockTakeoff(ANIMATION_LENGTH_TICKS);
            }

            // Play appropriate animation variant (sound is handled by keyframe at 1.76s)
            String trigger = isGroundCast ? "summon_storm_ground" : "summon_storm_air";
            getUser().triggerAnim("action", trigger);
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
}
