package com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles all animation logic for the Lightning Dragon
 * Extracted from LightningDragonEntity to improve organization and maintainability
 */
public record LightningDragonAnimationHandler(LightningDragonEntity dragon) {
    
    // ===== ANIMATION TRIGGERS =====
    
    /**
     * Triggers the dodge animation
     */
    public void triggerDodgeAnimation() {
        dragon.triggerAnim("action", "dodge");
    }
    
    /**
     * Triggers sleep enter animation
     */
    public void triggerSleepEnter() {
        dragon.triggerAnim("action", "sleep_enter");
    }
    
    /**
     * Triggers sleep exit animation
     */
    public void triggerSleepExit() {
        dragon.triggerAnim("action", "sleep_exit");
    }

    // ===== GECKOLIB SETUP =====
    /**
     * Sets up all GeckoLib animation triggers for the action controller
     */
    public void setupActionController(AnimationController<LightningDragonEntity> actionController) {
        // Register triggerable one-shots for server-side triggerAnim()
        registerVocalTriggers(actionController);
        
        // Register native keys for triggers
        actionController.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.lightning_bite"));
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.horn_gore"));
        actionController.triggerableAnim("dodge",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.dodge"));
        actionController.triggerableAnim("lightning_beam",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.lightning_beam"));
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.eat"));
        
        // Summon Storm variants
        actionController.triggerableAnim("summon_storm_ground",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.summon_storm_ground"));
        actionController.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.summon_storm_air"));
        
        // Sleep transitions
        actionController.triggerableAnim("sleep_enter",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_enter"));
        actionController.triggerableAnim("sleep_exit",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_exit"));
    }
    
    /**
     * Registers vocal animation triggers
     */
    private void registerVocalTriggers(AnimationController<LightningDragonEntity> action) {
        // Only register sounds that actually exist in sounds.json + required ability animations
        String[] keys = {
            "grumble1", "grumble2", "grumble3", "purr", "snort", "chuff", "content", "annoyed",
            "excited", "roar", "roar_ground", "roar_air", "growl_warning", "lightning_bite", "horn_gore", "hurt", "die"
        };
        
        for (String key : keys) {
            action.triggerableAnim(key, RawAnimation.begin().thenPlay("animation.lightning_dragon." + key));
        }
    }
    
    // ===== ANIMATION PREDICATES =====
    
    /**
     * Handles banking animation based on bank direction
     */
    public PlayState bankingPredicate(AnimationState<LightningDragonEntity> state) {
        double bankDir = dragon.getBankDirection();
        
        if (bankDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_right"));
        } else if (bankDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_left"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_off"));
        }
        return PlayState.CONTINUE;
    }
    
    /**
     * Handles pitching animation based on pitch direction
     */
    public PlayState pitchingPredicate(AnimationState<LightningDragonEntity> state) {
        double pitchDir = dragon.getPitchDirection();
        
        if (pitchDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_down"));
        } else if (pitchDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_up"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_off"));
        }
        return PlayState.CONTINUE;
    }
    
    /**
     * Handles action animations (combat, abilities, sleep transitions)
     */
    public PlayState actionPredicate(AnimationState<LightningDragonEntity> state) {
        // Native GeckoLib: controller idles until triggerAnim is fired
        state.getController().transitionLength(5);
        
        // If summoning (controls locked), force the summon clip variant to prevent bleed
        if (dragon.isSummoning()) {
            String clip = dragon.isFlying() ?
                    "animation.lightning_dragon.summon_storm_air" :
                    "animation.lightning_dragon.summon_storm_ground";
            state.setAndContinue(RawAnimation.begin().thenPlay(clip));
            return PlayState.CONTINUE;
        }
        
        // If dying, force the death clip to hold until completion
        if (dragon.isDying()) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.die"));
            return PlayState.CONTINUE;
        }
        
        // Sleep transitions and loop
        if (dragon.sleepingEntering) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_enter"));
            return PlayState.CONTINUE;
        }
        if (dragon.isSleeping()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.sleep"));
            return PlayState.CONTINUE;
        }
        if (dragon.sleepingExiting) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_exit"));
            return PlayState.CONTINUE;
        }
        
        return PlayState.STOP;
    }

}