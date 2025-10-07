package com.leon.saintsdragons.server.entity.ability.abilities.riftdrake;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Cosmetic roar for the Rift Drake. Plays roar animation and sound.
 * Locks abilities for the full animation duration (~5s) but allows movement.
 */
public class RiftDrakeRoarAbility extends DragonAbility<RiftDrakeEntity> {

    // Animation length: ~4.75s = 95 ticks. Round up to 100 ticks for 5 seconds.
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 85),
            new AbilitySectionDuration(RECOVERY, 9)
    };

    private static final int SOUND_DELAY_TICKS = 3;
    private static final int ROAR_TOTAL_TICKS = 100; // 5 seconds @ 20 TPS

    private boolean soundQueued = false;

    public RiftDrakeRoarAbility(DragonAbilityType<RiftDrakeEntity, RiftDrakeRoarAbility> type,
                                RiftDrakeEntity user) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            RiftDrakeEntity dragon = getUser();
            // Use different roar animation based on phase
            String trigger = dragon.isPhaseTwoActive() ? "roar2" : "roar";
            dragon.triggerAnim("action", trigger);
            soundQueued = true;

            // Lock abilities for full animation duration but allow walking/running
            dragon.lockAbilities(ROAR_TOTAL_TICKS);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            soundQueued = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (soundQueued && section.sectionType == STARTUP && getTicksInSection() >= SOUND_DELAY_TICKS) {
            RiftDrakeEntity dragon = getUser();
            if (!dragon.level().isClientSide) {
                Vec3 mouth = dragon.getMouthPosition();
                boolean phaseTwo = dragon.isPhaseTwoActive();
                // Phase 2 roar is deeper and louder
                float basePitch = phaseTwo ? 0.8f : 1.0f;
                float volume = phaseTwo ? 1.8f : 1.4f;
                float pitch = basePitch + dragon.getRandom().nextFloat() * 0.1f;

                dragon.level().playSound(null,
                        mouth.x, mouth.y, mouth.z,
                        ModSounds.RIFTDRAKE_ROAR.get(),
                        SoundSource.NEUTRAL,
                        volume,
                        pitch);
            }
            soundQueued = false;
        }
    }
}
