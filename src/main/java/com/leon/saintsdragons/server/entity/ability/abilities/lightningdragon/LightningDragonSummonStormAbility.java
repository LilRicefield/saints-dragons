package com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.sounds.SoundEvents;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Ultimate: Summon Storm
 * - Roars into the sky, triggers a thunderstorm in the current dimension
 * - Supercharges the dragon for 2 minutes: x2 damage on abilities
 * - Cooldown: 4 minutes
 */
public class LightningDragonSummonStormAbility extends DragonAbility<LightningDragonEntity> {
    private static final int SUPERCHARGE_TICKS = 20 * 120; // 120s
    private static final int COOLDOWN_TICKS = 20 * 240; // 240s

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 120), // 6s windup (i-frames)
            new AbilitySectionInstant(AbilitySectionType.ACTIVE),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 20) // small tail to keep action controller busy
    };

    public LightningDragonSummonStormAbility(DragonAbilityType<LightningDragonEntity, LightningDragonSummonStormAbility> type, LightningDragonEntity user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Lock inputs and grant invulnerability for 6 seconds while animating
            getUser().lockRiderControls(20 * 6);
            getUser().lockTakeoff(20 * 6);
            getUser().startTemporaryInvuln(20 * 6);
            // Play summon_storm animation variant + sound
            boolean flying = getUser().isFlying();
            String trigger = flying ? "summon_storm_air" : "summon_storm_ground";
            getUser().triggerAnim("action", trigger);
            if (!getLevel().isClientSide) {
                getLevel().playSound(null, getUser().getX(), getUser().getY(), getUser().getZ(),
                        com.leon.saintsdragons.common.registry.ModSounds.DRAGON_SUMMON_STORM.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.6f,
                        0.85f + getUser().getRandom().nextFloat() * 0.1f);
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
}
