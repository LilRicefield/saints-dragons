package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;

/**
 * Generic hurt ability for all dragons.
 * Plays hurt animation and optionally fires a manual vocal when the animation lacks keyframed audio.
 */
public class HurtAbility<T extends DragonEntity> extends DragonAbility<T> {

    private static final int DURATION_TICKS = 11; // ~0.55s at 20 TPS
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    private static final String DEFAULT_CONTROLLER = "action";

    private final String controllerId;
    private final String animationTrigger;
    private final String manualVocalKey;

    public HurtAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                       T user) {
        super(type, user, TRACK, 10); // Small cooldown to prevent spam

        String abilityId = type.getName();
        this.controllerId = resolveControllerId(abilityId);
        this.animationTrigger = resolveAnimationTrigger(abilityId);
        this.manualVocalKey = resolveManualVocalKey(abilityId);
    }

    private static String resolveAnimationTrigger(String abilityId) {
        // Allow future dragons to supply specialized hurt clips via ability id mapping
        return switch (abilityId) {
            case "cindervane_hurt" -> "cindervane_hurt";
            default -> "hurt";
        };
    }

    private static String resolveManualVocalKey(String abilityId) {
        return switch (abilityId) {
            case "hurt", "cindervane_hurt", "primitive_drake_hurt", "raevyx_hurt" -> abilityId; // Manual audio fallback when animation lacks audio
            default -> null;                      // Other dragons rely on keyframed audio
        };
    }

    private static String resolveControllerId(String abilityId) {
        return switch (abilityId) {
            case "cindervane_hurt" -> "actions";
            default -> DEFAULT_CONTROLLER;
        };
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (animationTrigger != null) {
            getUser().triggerAnim(controllerId, animationTrigger);
        }

        if (!getLevel().isClientSide && manualVocalKey != null && getUser() instanceof SoundHandledDragon soundHandled) {
            soundHandled.getSoundHandler().playVocal(manualVocalKey);
        }
    }

    @Override
    public boolean tryAbility() {
        // Always allowed if off cooldown; damage event gates invocation
        return canUse();
    }

    @Override
    public boolean damageInterrupts() {
        // Already a damage reaction
        return false;
    }

    @Override
    public boolean isOverlayAbility() {
        return true;
    }
}


