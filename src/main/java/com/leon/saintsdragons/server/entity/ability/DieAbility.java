package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity.VocalEntry;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;

/**
 * Generic death ability for all dragons.
 * Plays death animation and sound, then finalizes death after animation completes.
 */
public class DieAbility<T extends DragonEntity> extends DragonAbility<T> {

    // Death animation duration: ~3.071s (62 ticks)
    private static final int DURATION_TICKS = 62;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    public DieAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                      T user) {
        super(type, user, TRACK, 0);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        
        // Allow dragons to update custom death state
        T dragon = getUser();
        dragon.onDeathAbilityStarted();

        // Trigger death animation using dragon-specific metadata when available
        String controllerId = "action";
        VocalEntry deathEntry = dragon.getVocalEntries().get("die");
        if (deathEntry != null && deathEntry.controllerId() != null) {
            controllerId = deathEntry.controllerId();
        }
        dragon.triggerAnim(controllerId, "die");

        // Play death sound manually since keyframes are empty
        if (!getLevel().isClientSide && dragon instanceof SoundHandledDragon soundDragon) {
            soundDragon.getSoundHandler().playVocal("die");
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // After death animation completes, actually kill the dragon
        if (!getLevel().isClientSide) {
            // Remove invulnerability and kill the dragon
            getUser().setInvulnerable(false);
            getUser().hurt(getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        }
        complete();
    }

    @Override
    public boolean damageInterrupts() {
        return false;
    }
}
