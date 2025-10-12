package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity.VocalEntry;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;

/**
 * Generic death ability for all dragons.
 * Plays death animation and sound, then finalizes death after animation completes.
 */
public class DieAbility<T extends DragonEntity> extends DragonAbility<T> {

    public DieAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                      T user) {
        super(type, user, buildTrack(user), 0);
    }

    private static <E extends DragonEntity> DragonAbilitySection[] buildTrack(E dragon) {
        int duration = Math.max(1, dragon.getDeathAnimationDurationTicks());
        return new DragonAbilitySection[] {
                new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, duration)
        };
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        // Allow dragons to update custom death state
        T dragon = getUser();
        dragon.onDeathAbilityStarted();

        // Use ability ID to look up vocal entry (matches the pattern)
        String abilityId = this.getAbilityType().getName();

        // Trigger death animation using wyvern-specific metadata when available
        String controllerId = "action";
        VocalEntry deathEntry = dragon.getVocalEntries().get(abilityId);
        if (deathEntry != null && deathEntry.controllerId() != null) {
            controllerId = deathEntry.controllerId();
        }
        dragon.triggerAnim(controllerId, "die");

        // Play death sound manually using ability ID as vocal key
        if (!getLevel().isClientSide && dragon instanceof SoundHandledDragon soundDragon) {
            soundDragon.getSoundHandler().playVocal(abilityId);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // After death animation completes, actually kill the wyvern
        if (!getLevel().isClientSide) {
            // Remove invulnerability and kill the wyvern
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
