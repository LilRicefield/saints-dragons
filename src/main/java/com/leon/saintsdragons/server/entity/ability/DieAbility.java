package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.damagesource.DamageTypes;

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
        
        // Trigger death animation
        getUser().triggerAnim("action", "die");
        
        // Play death sound manually since keyframes are empty
        if (!getLevel().isClientSide) {
            // Cast to LightningDragonEntity to access sound handler
            if (getUser() instanceof com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity lightningDragon) {
                lightningDragon.getSoundHandler().playVocal("die");
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // Let the existing death system handle the rest
        complete();
    }

    @Override
    public boolean damageInterrupts() {
        return false;
    }
}
