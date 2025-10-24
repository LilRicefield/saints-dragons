package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Cosmetic roar for the Amphithere. Plays the authored roar animation
 * and routes a synced sound at the mouth locator; no gameplay impact.
 */
public class CindervaneRoarAbility extends DragonAbility<Cindervane> {

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 30),
            new AbilitySectionDuration(RECOVERY, 10)
    };

    private static final int SOUND_DELAY_TICKS = 3;

    private boolean soundQueued = false;

    public CindervaneRoarAbility(DragonAbilityType<Cindervane, CindervaneRoarAbility> type,
                                 Cindervane user) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            // Fire the authored action clip; controller name is "actions" for Amphithere
            Cindervane dragon = getUser();
            String trigger = dragon.isFlying() ? "roar_air" : "roar_ground";
            dragon.triggerAnim("actions", trigger);
            soundQueued = true;
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

        // Continuously trigger screen shake during the entire ability
        Cindervane dragon = getUser();
        if (!dragon.level().isClientSide) {
            dragon.triggerScreenShake(0.6F);
        }

        if (soundQueued && section.sectionType == STARTUP && getTicksInSection() >= SOUND_DELAY_TICKS) {
            if (!dragon.level().isClientSide) {
                Vec3 mouth = dragon.getMouthPosition();
                boolean flying = dragon.isFlying();
                float basePitch = flying ? 1.05f : 0.9f;
                float pitch = basePitch + dragon.getRandom().nextFloat() * 0.05f;
                dragon.level().playSound(null,
                        mouth.x, mouth.y, mouth.z,
                        ModSounds.CINDERVANE_ROAR.get(),
                        SoundSource.NEUTRAL,
                        1.5f,
                        pitch);
            }
            soundQueued = false;
        }
    }
}

