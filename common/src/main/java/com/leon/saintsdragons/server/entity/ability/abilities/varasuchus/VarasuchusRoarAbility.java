package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Cosmetic roar for the Rift Drake. Plays roar animation and sound.
 * Locks abilities for the full animation duration (~5s) but allows movement.
 */
public class VarasuchusRoarAbility extends DragonAbility<Varasuchus> {

    // Animation length: ~4.75s = 95 ticks. Round up to 100 ticks for 5 seconds.
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 85),
            new AbilitySectionDuration(RECOVERY, 9)
    };

    private static final int ROAR_TOTAL_TICKS = 100; // 5 seconds @ 20 TPS

    public VarasuchusRoarAbility(DragonAbilityType<Varasuchus, VarasuchusRoarAbility> type,
                                 Varasuchus user) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Varasuchus dragon = getUser();
            dragon.triggerAnim("action", "roar");
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(
                        ModSounds.VARASUCHUS_ROAR.get(),
                        1.0f,
                        1.0f,
                        140
                );
            }

            dragon.lockAbilities(ROAR_TOTAL_TICKS);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Varasuchus dragon = getUser();

        // Continuous screen shake during the entire ability
        if (!dragon.level().isClientSide) {
            // Phase 2 has stronger shake due to the aggressive swipes
            float intensity = dragon.isPhaseTwoActive() ? 1.0F : 0.8F;
            dragon.triggerScreenShake(intensity);
        }

        if (section.sectionType == ACTIVE) {
            // Roar is cosmetic only now; phase 2 damage lives in slash barrage.
        }
    }
}
