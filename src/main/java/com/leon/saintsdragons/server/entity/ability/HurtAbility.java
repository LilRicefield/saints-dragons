package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

/**
 * Simple, extendable hurt reaction ability: plays a one-shot animation and sound.
 * Future dragons can extend or mirror this pattern for their own hurt clips/sfx.
 */
public class HurtAbility extends DragonAbility<LightningDragonEntity> {

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionInstant(DragonAbilitySection.AbilitySectionType.ACTIVE)
    };

    public HurtAbility(DragonAbilityType<LightningDragonEntity, ? extends DragonAbility<LightningDragonEntity>> type,
                       LightningDragonEntity user) {
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

