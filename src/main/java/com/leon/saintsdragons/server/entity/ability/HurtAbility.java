package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;

/**
 * Generic hurt ability for all dragons.
 * Plays hurt animation and sound as a quick reaction to damage.
 */
public class HurtAbility<T extends DragonEntity> extends DragonAbility<T> {

    // Hurt animation duration: ~0.55s (11 ticks)
    private static final int DURATION_TICKS = 11;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    public HurtAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                       T user) {
        super(type, user, TRACK, 10); // Small cooldown to prevent spam
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        
        // Trigger hurt animation
        getUser().triggerAnim("action", "hurt");
        
        // Play hurt sound manually since keyframes are empty
        if (!getLevel().isClientSide) {
            // Cast to LightningDragonEntity to access sound handler
            if (getUser() instanceof com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity lightningDragon) {
                lightningDragon.getSoundHandler().playVocal("hurt");
            }
        }
    }

    @Override
    public boolean tryAbility() {
        // Always allowed if off cooldown; damage event gates invocation
        return canUse();
    }

    @Override
    public boolean damageInterrupts() {
        // Already a damage reaction
        return false;
    }
}
