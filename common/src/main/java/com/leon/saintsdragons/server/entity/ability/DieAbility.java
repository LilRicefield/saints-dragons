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

        // Trigger death animation using specific metadata when available
        String controllerId = "action";
        VocalEntry deathEntry = dragon.getVocalEntries().get(abilityId);
        if (deathEntry != null && deathEntry.controllerId() != null) {
            controllerId = deathEntry.controllerId();
        }

        // Use baby die animation if this is a baby dragon
        String animationTrigger = "die";
        if (abilityId.startsWith("baby_")) {
            animationTrigger = "baby_die";
        }
        dragon.triggerAnim(controllerId, animationTrigger);
    }

    @Override
    public void tickUsing() {
        super.tickUsing();

        // Manually trigger death sound at tick 1 (like Mowzie's Mobs)
        // Must play directly via playSound() - playVocal() is blocked by isDeadOrDying() check!
        if (getTicksInSection() == 1 && !getLevel().isClientSide) {
            T dragon = getUser();
            String abilityId = this.getAbilityType().getName();

            // Look up the vocal entry to get the sound event
            VocalEntry deathEntry = dragon.getVocalEntries().get(abilityId);
            if (deathEntry != null && deathEntry.soundSupplier() != null) {
                net.minecraft.sounds.SoundEvent sound = deathEntry.soundSupplier().get();
                float volume = deathEntry.volume();
                float pitch = deathEntry.basePitch() + (dragon.getRandom().nextFloat() - 0.5f) * deathEntry.pitchVariance() * 2f;

                // Play sound directly (bypasses vocal system's isDeadOrDying check)
                dragon.playSound(sound, volume, pitch);
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // Animation complete - tickDeath() will handle entity removal and loot drops
        complete();
    }

    @Override
    public boolean damageInterrupts() {
        return false;
    }
}
