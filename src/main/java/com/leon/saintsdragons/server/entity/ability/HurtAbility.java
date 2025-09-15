package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;

/**
 * Generic hurt reaction ability: plays a one-shot animation and sound.
 * Works with any dragon type that extends DragonEntity.
 */
public class HurtAbility extends DragonAbility<DragonEntity> {

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionInstant(DragonAbilitySection.AbilitySectionType.ACTIVE)
    };

    public HurtAbility(DragonAbilityType<DragonEntity, ? extends DragonAbility<DragonEntity>> type,
                       DragonEntity user) {
        super(type, user, TRACK, 10); // tiny cooldown to avoid spam
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        // Play the hurt animation and sound once, then complete
        if (!getLevel().isClientSide) {
            // Trigger action clip (registered via registerVocalTriggers)
            getUser().triggerAnim("action", "hurt");
            // Route through sound handler for consistent volume/pitch & future routing
            getUser().getSoundHandler().playVocal("hurt");
        }
        complete();
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