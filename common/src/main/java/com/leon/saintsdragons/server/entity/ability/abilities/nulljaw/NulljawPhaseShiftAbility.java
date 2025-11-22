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
 * - No cooldown, plays chained transition animations when entering phase 2 on ground
 */
public class NulljawPhaseShiftAbility extends DragonAbility<Nulljaw> {
    // Ground phase 2 transition - chaining animations (start + main + end)
    private static final int GROUND_START_TICKS = 38;  // 1.875s animation.nulljaw.phase2_start
    private static final int GROUND_MAIN_TICKS = 67;   // 3.3333s animation.nulljaw.phase2
    private static final int GROUND_END_TICKS = 17;    // 0.8333s animation.nulljaw.phase2_end
    private static final int GROUND_TOTAL_SEQUENCE_TICKS = GROUND_START_TICKS + GROUND_MAIN_TICKS + GROUND_END_TICKS; // 122 ticks
    // Lock slightly longer than animations to ensure client EntityData sync completes before movement controller resumes
    // Network delay can cause client to see old phase value for ~5-10 ticks after server toggles it
    private static final int LOCK_DURATION = 120;

    private final boolean enteringPhaseTwo;
    private final boolean isGroundTransition;
    private boolean phaseToggleApplied;
    private boolean mainAnimPlayed;
    private boolean endAnimPlayed;
    private boolean screenShakeActive;

    private static final DragonAbilitySection[] TRACK_ENTER_PHASE2_GROUND = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, GROUND_TOTAL_SEQUENCE_TICKS), // Full ground sequence
            new AbilitySectionInstant(AbilitySectionType.ACTIVE), // Apply phase change
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 10) // Small tail
    };

    private static final DragonAbilitySection[] TRACK_EXIT_PHASE2 = new DragonAbilitySection[] {
            new AbilitySectionInstant(AbilitySectionType.ACTIVE) // Instant revert
    };

    public NulljawPhaseShiftAbility(DragonAbilityType<Nulljaw, NulljawPhaseShiftAbility> type, Nulljaw user) {
        super(type, user, user.isPhaseTwoActive() ? TRACK_EXIT_PHASE2 : TRACK_ENTER_PHASE2_GROUND, 0); // No cooldown
        this.enteringPhaseTwo = !user.isPhaseTwoActive();
        this.isGroundTransition = !user.isInWaterOrBubble(); // Ground uses chaining anims, underwater uses single anim
        this.phaseToggleApplied = false;
        this.mainAnimPlayed = false;
        this.endAnimPlayed = false;
        this.screenShakeActive = false;
    }


    @Override
    public boolean canUse() {
        Nulljaw user = getUser();
        boolean underwater = user.isInWaterOrBubble();
        if (!underwater && !user.onGround()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            if (enteringPhaseTwo) {
                // Lock controls, abilities, and stop all movement for the full transition
                getUser().lockRiderControls(LOCK_DURATION);
                getUser().lockAbilities(LOCK_DURATION);

                // Stop navigation and movement controllers to prevent flickering
                getUser().getNavigation().stop();
                getUser().getMoveControl().setWantedPosition(getUser().getX(), getUser().getY(), getUser().getZ(), 0);

                // CRITICAL: Toggle phase IMMEDIATELY at the start of the transition
                // This ensures the movement controller uses the correct idle/walk/run animations
                // when the lock expires and animations finish
                getUser().setPhaseTwoActive(true, true);
                phaseToggleApplied = true;
            }

            mainAnimPlayed = false;
            endAnimPlayed = false;
            screenShakeActive = false;

            // Play first animation (ground: phase2_start, underwater: phase2_underwater)
            String startTrigger = resolvePhaseAnimation();
            getUser().triggerAnim("action", startTrigger);
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (enteringPhaseTwo) {
                if (!phaseToggleApplied) {
                    getUser().setPhaseTwoActive(true, true);
                    phaseToggleApplied = true;
                }
            } else {
                // Instant revert to phase 1
                boolean newPhase = !getUser().isPhaseTwoActive();
                getUser().setPhaseTwoActive(newPhase, true);

                if (!newPhase) {
                    // Lock controls and abilities to prevent ambient sounds/animations during transition
                    getUser().lockRiderControls(45);
                    getUser().lockAbilities(45);

                    // Trigger phase1 animation (sound is handled by animation keyframe, don't play manually)
                    getUser().triggerAnim("action", "phase1");
                }
            }
        }
    }

    @Override
    public void tickUsing() {
        // Screen shake only during main animation
        if (screenShakeActive && !getUser().level().isClientSide) {
            getUser().triggerScreenShake(1.5F);
        }

        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.STARTUP) {
            return;
        }

        // Only chain animations for ground transitions entering phase 2
        if (!enteringPhaseTwo || !isGroundTransition) {
            return;
        }

        int ticks = getTicksInSection();

        // Chain main animation (phase2) after start animation
        if (!mainAnimPlayed && ticks >= GROUND_START_TICKS) {
            getUser().triggerAnim("action", "phase2");
            mainAnimPlayed = true;
            screenShakeActive = true; // Start screen shake during main animation
            // Phase already toggled in beginSection(STARTUP)
        }

        // Chain end animation (phase2_end) after main animation
        if (!endAnimPlayed && ticks >= (GROUND_START_TICKS + GROUND_MAIN_TICKS)) {
            getUser().triggerAnim("action", "phase2_end");
            endAnimPlayed = true;
            screenShakeActive = false; // Stop screen shake when entering end animation
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == AbilitySectionType.STARTUP) {
            screenShakeActive = false;
        }
    }

    @Override
    public void end() {
        screenShakeActive = false;
        super.end();
    }

    private String resolvePhaseAnimation() {
        boolean underwater = getUser().isInWaterOrBubble();
        if (underwater) {
            // Underwater uses single animation (no chaining)
            return "phase2_underwater";
        }
        // Ground uses chaining: start -> main -> end
        // This method returns the FIRST animation in the chain
        return "phase2_start";
    }
}
