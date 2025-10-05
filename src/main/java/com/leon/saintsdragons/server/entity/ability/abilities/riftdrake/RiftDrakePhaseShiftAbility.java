package com.leon.saintsdragons.server.entity.ability.abilities.riftdrake;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Phase Shift - Ultimate toggle ability
 * - Toggles between Phase 1 (quadruped, bite only) and Phase 2 (can use claws + bite)
 * - Can only be activated on ground initially
 * - No cooldown, plays transition animation when entering phase 2
 */
public class RiftDrakePhaseShiftAbility extends DragonAbility<RiftDrakeEntity> {
    private static final int TRANSITION_DURATION = 40; // 2 seconds for phase 2 transition animation

    private static final DragonAbilitySection[] TRACK_ENTER_PHASE2 = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, TRANSITION_DURATION), // Transition animation
            new AbilitySectionInstant(AbilitySectionType.ACTIVE), // Apply phase change
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 10) // Small tail
    };

    private static final DragonAbilitySection[] TRACK_EXIT_PHASE2 = new DragonAbilitySection[] {
            new AbilitySectionInstant(AbilitySectionType.ACTIVE) // Instant revert
    };

    public RiftDrakePhaseShiftAbility(DragonAbilityType<RiftDrakeEntity, RiftDrakePhaseShiftAbility> type, RiftDrakeEntity user) {
        super(type, user, user.isPhaseTwoActive() ? TRACK_EXIT_PHASE2 : TRACK_ENTER_PHASE2, 0); // No cooldown
    }

    @Override
    public boolean isOverlayAbility() {
        return true; // Never blocks other abilities during instant revert, but locks during transition
    }

    @Override
    public boolean canUse() {
        // Can only activate phase 2 for the first time on ground
        if (!getUser().isPhaseTwoActive() && getUser().isInWater()) {
            return false; // Must be on ground to activate phase 2
        }
        return super.canUse();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Entering phase 2 - play transition animation
            getUser().triggerAnim("action", "phase2");
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            // Actually toggle phase state
            boolean newPhase = !getUser().isPhaseTwoActive();
            getUser().setPhaseTwoActive(newPhase, true);

            // Play phase 1 sound only when reverting (instant case)
            if (!newPhase && !getLevel().isClientSide) {
                getLevel().playSound(null, getUser().getX(), getUser().getY(), getUser().getZ(),
                        com.leon.saintsdragons.common.registry.ModSounds.RIFTDRAKE_PHASE1.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.4f,
                        0.9f + getUser().getRandom().nextFloat() * 0.2f);
            }
        }
    }

    // Sound is now handled by GeckoLib keyframe in the animation JSON
}
