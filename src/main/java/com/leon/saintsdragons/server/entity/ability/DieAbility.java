package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

/**
 * Death ability: plays a short death animation and sound, then finalizes death.
 * Duration: ~3.071s (62 ticks) before removal.
 */
public class DieAbility extends DragonAbility<LightningDragonEntity> {

    // 3.071s ~= 61.42 ticks; round up to 62
    private static final int DURATION_TICKS = 62;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    public DieAbility(DragonAbilityType<LightningDragonEntity, ? extends DragonAbility<LightningDragonEntity>> type,
                      LightningDragonEntity user) {
        super(type, user, TRACK, 0);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (!getLevel().isClientSide) {
            // Flag entity as dying to block inputs/movement and further damage interception
            getUser().setDying(true);
            // Trigger vanilla red hurt flash once
            getUser().level().broadcastEntityEvent(getUser(), (byte)2);

            // Trigger death animation key (if present) and play death sound
            getUser().triggerAnim("action", "die");
            getLevel().playSound(null,
                    getUser().getX(), getUser().getY(), getUser().getZ(),
                    com.leon.saintsdragons.common.registry.ModSounds.DRAGON_DIE.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    1.2f,
                    0.95f + getUser().getRandom().nextFloat() * 0.1f);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (!getLevel().isClientSide) {
            // Allow final death to proceed normally
            getUser().setInvulnerable(false);
            // Kill via generic kill source to trigger standard death cleanup
            try {
                getUser().hurt(getUser().level().damageSources().genericKill(), Float.MAX_VALUE);
            } catch (Throwable t) {
                // Fallback: hard discard if damage routing fails for some reason
                getUser().discard();
            }
        }
        complete();
    }

    @Override
    public boolean damageInterrupts() {
        return false;
    }
}
