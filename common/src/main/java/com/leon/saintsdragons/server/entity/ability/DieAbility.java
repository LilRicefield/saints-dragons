package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity.VocalEntry;
import com.leon.saintsdragons.util.animation.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

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
        T dragon = getUser();
        dragon.onDeathAbilityStarted();
        String abilityId = this.getAbilityType().getName();
        String controllerId = DragonInteractionAnimationHelper.CONTROLLER;
        String animationTrigger = "die";
        if (abilityId.startsWith("baby_")) {
            animationTrigger = "baby_die";
        } else if ("varasuchus_die".equals(abilityId)) {
            animationTrigger = "varasuchus_die";
        } else if ("volitans_die".equals(abilityId)) {
            animationTrigger = "volitans_die";
        }
        dragon.triggerAnim(controllerId, animationTrigger);
    }

    @Override
    public void tickUsing() {
        super.tickUsing();

        if (getTicksInSection() == 1 && !getLevel().isClientSide) {
            T dragon = getUser();
            String abilityId = this.getAbilityType().getName();
            if ("volitans_die".equals(abilityId)
                    && dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans volitans) {
                float pitch = 0.95f + volitans.getRandom().nextFloat() * 0.1f;
                volitans.playSound(ModSounds.VOLITANS_DIE.get(), 1.6f, pitch);
                return;
            }

            VocalEntry deathEntry = dragon.getVocalEntries().get(abilityId);
            if (deathEntry != null && deathEntry.soundSupplier() != null) {
                net.minecraft.sounds.SoundEvent sound = deathEntry.soundSupplier().get();
                float volume = deathEntry.volume();
                if (dragon instanceof Raevyx && dragon.isBaby()) {
                    return;
                }
                float pitch = deathEntry.basePitch() + (dragon.getRandom().nextFloat() - 0.5f) * deathEntry.pitchVariance() * 2f;

                dragon.playSound(sound, volume, pitch);
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        complete();
    }

}
