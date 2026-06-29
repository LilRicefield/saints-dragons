package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.VarasuchusAnimationHandler;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInstant;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;


public class VarasuchusPhaseShiftAbility extends DragonAbility<Varasuchus> {
    private static final int GROUND_TRANSITION_TICKS = 21;      // 1.0417s
    private static final int UNDERWATER_TRANSITION_TICKS = 33;  // 1.6667s
    private static final int PHASE_TWO_SOUND_TICKS = 60;        // 3.0s
    private static final int PHASE_TWO_UNDERWATER_SOUND_TICKS = 60;
    private static final int PHASE_ONE_UNDERWATER_SOUND_TICKS = 20;
    private static final DragonAbilitySection[] TRACK_GROUND = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, GROUND_TRANSITION_TICKS),
            new AbilitySectionInstant(ACTIVE)
    };

    private static final DragonAbilitySection[] TRACK_UNDERWATER = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, UNDERWATER_TRANSITION_TICKS),
            new AbilitySectionInstant(ACTIVE)
    };
    private final boolean enteringPhaseTwo;
    private final boolean underwaterTransition;
    private boolean phaseToggleApplied;

    public VarasuchusPhaseShiftAbility(DragonAbilityType<Varasuchus, VarasuchusPhaseShiftAbility> type, Varasuchus user) {
        super(type, user, user.isInWaterOrBubble() ? TRACK_UNDERWATER : TRACK_GROUND, 0);
        this.enteringPhaseTwo = !user.isPhaseTwoActive();
        this.underwaterTransition = user.isInWaterOrBubble();
        this.phaseToggleApplied = false;
    }

    @Override
    public boolean canUse() {
        Varasuchus user = getUser();
        if (!underwaterTransition && !user.onGround()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        int transitionTicks = underwaterTransition ? UNDERWATER_TRANSITION_TICKS : GROUND_TRANSITION_TICKS;
        Varasuchus dragon = getUser();

        dragon.lockRiderControls(transitionTicks);
        dragon.lockAbilities(transitionTicks);
        dragon.getNavigation().stop();
        dragon.getMoveControl().setWantedPosition(dragon.getX(), dragon.getY(), dragon.getZ(), 0.0D);

        boolean newPhase = enteringPhaseTwo;
        dragon.setPhaseTwoActive(newPhase, true);
        phaseToggleApplied = true;

        String trigger = resolveAnimationTrigger();
        dragon.triggerAnim(VarasuchusAnimationHandler.MOVEMENT_CONTROLLER, trigger);

        if (enteringPhaseTwo && !dragon.level().isClientSide) {
            dragon.startPhaseShiftScreenShake(transitionTicks, 1.5F);
        }

        if (!dragon.level().isClientSide) {
            if (newPhase) {
                dragon.getSoundHandler().playMovingEntitySound(
                        underwaterTransition ? ModSounds.VARASUCHUS_PHASE2_UNDERWATER.get() : ModSounds.VARASUCHUS_PHASE2.get(),
                        1.0f,
                        1.0f,
                        underwaterTransition ? PHASE_TWO_UNDERWATER_SOUND_TICKS : PHASE_TWO_SOUND_TICKS
                );
            } else {
                dragon.getSoundHandler().playMovingEntitySound(
                        underwaterTransition ? ModSounds.VARASUCHUS_PHASE1_UNDERWATER.get() : ModSounds.VARASUCHUS_PHASE1.get(),
                        1.0f,
                        1.0f,
                        underwaterTransition ? PHASE_ONE_UNDERWATER_SOUND_TICKS : transitionTicks
                );
            }
        }
    }

    @Override
    public void tickUsing() {
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP && !phaseToggleApplied) {
            getUser().setPhaseTwoActive(enteringPhaseTwo, true);
            phaseToggleApplied = true;
        }
    }

    @Override
    public void end() {
        getUser().stopPhaseShiftScreenShake();
        super.end();
    }

    private String resolveAnimationTrigger() {
        if (enteringPhaseTwo) {
            return underwaterTransition ? "phase2_underwater" : "phase2";
        }
        return underwaterTransition ? "phase1_underwater" : "phase1";
    }
}
