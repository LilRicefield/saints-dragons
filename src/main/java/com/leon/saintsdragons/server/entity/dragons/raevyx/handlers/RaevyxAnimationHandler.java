package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles all animation logic for Raevyx
 * Extracted from Raevyx to improve organization and maintainability
 */
public record RaevyxAnimationHandler(Raevyx wyvern) {
    
    // ===== ANIMATION TRIGGERS =====
    
    /**
     * Triggers the dodge animation
     */
    public void triggerDodgeAnimation() {
        wyvern.triggerAnim("action", "dodge");
    }
    
    /**
     * Triggers sleep enter animation
     */
    public void triggerSleepEnter() {
        wyvern.triggerAnim("action", "sleep_enter");
    }
    
    /**
     * Triggers sleep exit animation
     */
    public void triggerSleepExit() {
        wyvern.triggerAnim("action", "sleep_exit");
    }

    // ===== GECKOLIB SETUP =====
    /**
     * Sets up all GeckoLib animation triggers for the action controller
     */
    public void setupActionController(AnimationController<Raevyx> actionController) {
        // Register triggerable one-shots for server-side triggerAnim()
        registerVocalTriggers(actionController);
        
        // Register native keys for triggers
        actionController.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_bite"));
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.raevyx.horn_gore"));
        actionController.triggerableAnim("dodge",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge"));
        actionController.triggerableAnim("lightning_beam_start",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_start"));
        actionController.triggerableAnim("lightning_beaming",
                RawAnimation.begin().thenLoop("animation.raevyx.lightning_beaming"));
        actionController.triggerableAnim("lightning_beam_stop",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_stop"));
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.raevyx.eat"));
        
        // Summon Storm variants
        actionController.triggerableAnim("summon_storm_ground",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_ground"));
        actionController.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air"));
        
        // Sleep transitions
        actionController.triggerableAnim("sleep_enter",
                RawAnimation.begin().thenPlay("animation.raevyx.sleep_enter"));
        actionController.triggerableAnim("sleep_exit",
                RawAnimation.begin().thenPlay("animation.raevyx.sleep_exit"));
    }
    
    /**
     * Registers vocal animation triggers
     */
    private void registerVocalTriggers(AnimationController<Raevyx> action) {
        // Only register sounds that actually exist in sounds.json + required ability animations
        wyvern.getVocalEntries().forEach((key, entry) ->
                action.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId())));
    }
    
    // ===== ANIMATION PREDICATES =====
    
    /**
     * Handles banking animation based on bank direction
     */
    public PlayState bankingPredicate(AnimationState<Raevyx> state) {
        state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.banking_off"));
        return PlayState.CONTINUE;
    }
    
    /**
     * Handles pitching animation based on pitch direction
     */
    public PlayState pitchingPredicate(AnimationState<Raevyx> state) {
        double pitchDir = wyvern.getPitchDirection();
        
        if (pitchDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_down"));
        } else if (pitchDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_up"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_off"));
        }
        return PlayState.CONTINUE;
    }
    
    /**
     * Handles action animations (combat, abilities, sleep transitions)
     */
    public PlayState actionPredicate(AnimationState<Raevyx> state) {
        // Native GeckoLib: controller idles until triggerAnim is fired
        state.getController().transitionLength(5);
        
        // If dying, force the death clip to hold until completion
        if (wyvern.isDying()) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.raevyx.die"));
            return PlayState.CONTINUE;
        }
        
        // Sleep transitions and loop
        if (wyvern.sleepingEntering) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.raevyx.sleep_enter"));
            return PlayState.CONTINUE;
        }
        if (wyvern.isSleeping()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.sleep"));
            return PlayState.CONTINUE;
        }
        if (wyvern.sleepingExiting) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.raevyx.sleep_exit"));
            return PlayState.CONTINUE;
        }
        
        return PlayState.STOP;
    }

}
