package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Phase Shift - Ultimate toggle ability
 * - Toggles between Phase 1 (quadruped, bite only) and Phase 2 (can use claws + bite)
 * - Can only be activated on ground initially
 * - No cooldown, plays transition animation when entering phase 2
 */
public class NulljawPhaseShiftAbility extends DragonAbility<Nulljaw> {
    private static final int TRANSITION_DURATION = 127; // ~6.3 seconds for phase 2 transition animation
    private static final int LOCK_DURATION = 150;
    private static final int[] SHAKE_TICKS = {43, 63, 83}; // Matches keyframed phase roar + follow-up impacts
    private static final float[] SHAKE_INTENSITIES = {0.95F, 0.85F, 0.75F};

    private final boolean enteringPhaseTwo;
    private boolean phaseToggleApplied;
    private int nextShakeIndex;

    private static final DragonAbilitySection[] TRACK_ENTER_PHASE2 = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, TRANSITION_DURATION), // Transition animation
            new AbilitySectionInstant(AbilitySectionType.ACTIVE), // Apply phase change
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 10) // Small tail
    };

    private static final DragonAbilitySection[] TRACK_EXIT_PHASE2 = new DragonAbilitySection[] {
            new AbilitySectionInstant(AbilitySectionType.ACTIVE) // Instant revert
    };

    public NulljawPhaseShiftAbility(DragonAbilityType<Nulljaw, NulljawPhaseShiftAbility> type, Nulljaw user) {
        super(type, user, user.isPhaseTwoActive() ? TRACK_EXIT_PHASE2 : TRACK_ENTER_PHASE2, 0); // No cooldown
        this.enteringPhaseTwo = !user.isPhaseTwoActive();
        this.phaseToggleApplied = false;
        this.nextShakeIndex = 0;
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
            nextShakeIndex = 0;
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
                                com.leon.saintsdragons.common.registry.ModSounds.NULLJAW_PHASE1.get(),
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

            while (nextShakeIndex < SHAKE_TICKS.length && ticks >= SHAKE_TICKS[nextShakeIndex]) {
                if (!getUser().level().isClientSide) {
                    float intensity = SHAKE_INTENSITIES[nextShakeIndex];
                    getUser().triggerScreenShake(intensity);
                }
                if (!phaseToggleApplied) {
                    getUser().setPhaseTwoActive(true, true);
                    phaseToggleApplied = true;
                }
                nextShakeIndex++;
            }
        }
    }

}
