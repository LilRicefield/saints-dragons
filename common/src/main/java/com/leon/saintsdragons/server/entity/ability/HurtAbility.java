package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;

/**
 * Generic hurt ability for all dragons.
 * Plays hurt animation. Sound is handled by animation keyframes via dragon sound profiles.
 */
public class HurtAbility<T extends DragonEntity> extends DragonAbility<T> {

    private static final int DURATION_TICKS = 11; // ~0.55s at 20 TPS
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    private static final String DEFAULT_CONTROLLER = "action";

    private final String controllerId;
    private final String animationTrigger;

    public HurtAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                       T user) {
        super(type, user, TRACK, 10); // Small cooldown to prevent spam

        String abilityId = type.getName();
        this.controllerId = resolveControllerId(abilityId);
        this.animationTrigger = resolveAnimationTrigger(abilityId);
    }

    private static String resolveAnimationTrigger(String abilityId) {
        // Allow future dragons to supply specialized hurt clips via ability id mapping
        return switch (abilityId) {
            case "cindervane_hurt" -> "cindervane_hurt";
            case "raevyx_hurt" -> "raevyx_hurt";
            case "nulljaw_hurt" -> "nulljaw_hurt";
            case "ignivorus_hurt" -> "ignivorus_hurt";
            case "stegonaut_hurt" -> "stegonaut_hurt";
            default -> "hurt";
        };
    }

    private static String resolveControllerId(String abilityId) {
        return switch (abilityId) {
            case "raevyx_hurt", "ignivorus_hurt", "cindervane_hurt", "nulljaw_hurt", "stegonaut_hurt" -> "instant";
            default -> DEFAULT_CONTROLLER;
        };
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        // Trigger animation, sound is handled by animation keyframes
        if (animationTrigger != null) {
            getUser().triggerAnim(controllerId, animationTrigger);
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
