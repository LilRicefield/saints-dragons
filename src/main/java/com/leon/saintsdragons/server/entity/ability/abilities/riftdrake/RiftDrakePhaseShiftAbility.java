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
    private static final int TRANSITION_DURATION = 127; // ~6.3 seconds for phase 2 transition animation
    private static final int LOCK_DURATION = 150;
    private static final int ROAR_SHAKE_TICK = 55; // ~2.75 seconds into the animation
    private static final int FINAL_SHAKE_TICK = 110; // Late surge to cover the tail of the animation
    private static final float ROAR_SHAKE_INTENSITY = 0.95F;
    private static final float FINAL_SHAKE_INTENSITY = 0.75F;

    private final boolean enteringPhaseTwo;
    private boolean phaseToggleApplied;
    private boolean roarShakeApplied;
    private boolean finalShakeApplied;

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
        this.enteringPhaseTwo = !user.isPhaseTwoActive();
        this.phaseToggleApplied = false;
        this.roarShakeApplied = false;
        this.finalShakeApplied = false;
    }

    @Override
    public boolean isOverlayAbility() {
        return !enteringPhaseTwo;
    }

    @Override
    public boolean canUse() {
        if (!getUser().onGround() || getUser().isInWater()) {
            return false; // Phase shift requires firm footing
        }
        return super.canUse();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            if (enteringPhaseTwo) {
                // Fully lock controls and main abilities during transition
                getUser().lockRiderControls(LOCK_DURATION);
                getUser().lockAbilities(LOCK_DURATION);
            }
            phaseToggleApplied = false;
            roarShakeApplied = false;
            finalShakeApplied = false;
            getUser().triggerAnim("action", "phase2");
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (enteringPhaseTwo) {
                if (!phaseToggleApplied) {
                    getUser().setPhaseTwoActive(true, true);
                    phaseToggleApplied = true;
                }
            } else {
                boolean newPhase = !getUser().isPhaseTwoActive();
                getUser().setPhaseTwoActive(newPhase, true);

                // Play phase 1 sound only when reverting (instant case)
                if (!newPhase) {
                    getUser().triggerAnim("action", "phase1");
                    getUser().lockRiderControls(60);
                    if (!getLevel().isClientSide) {
                        getLevel().playSound(null, getUser().getX(), getUser().getY(), getUser().getZ(),
                                com.leon.saintsdragons.common.registry.ModSounds.RIFTDRAKE_PHASE1.get(),
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.4f,
                                0.9f + getUser().getRandom().nextFloat() * 0.2f);
                    }
                }
            }
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (enteringPhaseTwo && section.sectionType == AbilitySectionType.STARTUP) {
            int ticks = getTicksInSection();

            if (!roarShakeApplied && ticks >= ROAR_SHAKE_TICK) {
                if (!getUser().level().isClientSide) {
                    getUser().triggerScreenShake(ROAR_SHAKE_INTENSITY);
                }
                getUser().setPhaseTwoActive(true, true);
                phaseToggleApplied = true;
                roarShakeApplied = true;
            }

            if (!finalShakeApplied && ticks >= FINAL_SHAKE_TICK) {
                if (!getUser().level().isClientSide) {
                    getUser().triggerScreenShake(FINAL_SHAKE_INTENSITY);
                }
                finalShakeApplied = true;
            }
        }
    }

}
