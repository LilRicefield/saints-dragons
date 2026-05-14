package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;

public class HurtAbility<T extends DragonEntity> extends DragonAbility<T> {

    private static final int DURATION_TICKS = 11;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new DragonAbilitySection.AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, DURATION_TICKS)
    };

    private static final String DEFAULT_CONTROLLER = DragonInteractionAnimationHelper.CONTROLLER;

    private final String controllerId;
    private final String animationTrigger;

    public HurtAbility(DragonAbilityType<T, ? extends DragonAbility<T>> type,
                       T user) {
        super(type, user, TRACK, 10);

        String abilityId = type.getName();
        this.controllerId = resolveControllerId(abilityId);
        this.animationTrigger = resolveAnimationTrigger(abilityId);
    }

    private static String resolveAnimationTrigger(String abilityId) {
        return switch (abilityId) {
            case "cindervane_hurt" -> "cindervane_hurt";
            case "raevyx_hurt" -> "raevyx_hurt";
            case "varasuchus_hurt" -> "varasuchus_hurt";
            case "ignivorus_hurt" -> "ignivorus_hurt";
            case "stegonaut_hurt" -> "stegonaut_hurt";
            case "volitans_hurt" -> "volitans_hurt";
            case "nulljaw_hurt" -> "nulljaw_hurt";
            default -> "hurt";
        };
    }

    private static String resolveControllerId(String abilityId) {
        return switch (abilityId) {
            case "raevyx_hurt", "ignivorus_hurt", "cindervane_hurt", "varasuchus_hurt", "stegonaut_hurt", "volitans_hurt", "nulljaw_hurt" -> DragonInteractionAnimationHelper.CONTROLLER;
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

        if ("raevyx_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Raevyx raevyx) {
            float pitch = 0.95f + raevyx.getRandom().nextFloat() * 0.1f;
            raevyx.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_HURT.get(), 1.2f, pitch, 40);
        }
        if ("ignivorus_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Ignivorus ignivorus) {
            float pitch = 0.95f + ignivorus.getRandom().nextFloat() * 0.1f;
            ignivorus.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_HURT.get(), 1.2f, pitch, 40);
        }
        if ("cindervane_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Cindervane cindervane) {
            float pitch = 0.95f + cindervane.getRandom().nextFloat() * 0.1f;
            cindervane.getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_HURT.get(), 1.2f, pitch, 52);
        }
        if ("varasuchus_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Varasuchus varasuchus) {
            float pitch = 0.95f + varasuchus.getRandom().nextFloat() * 0.1f;
            varasuchus.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_HURT.get(), 1.2f, pitch, 34);
        }
        if ("stegonaut_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Stegonaut stegonaut) {
            float pitch = 0.95f + stegonaut.getRandom().nextFloat() * 0.1f;
            stegonaut.getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_HURT.get(), 1.2f, pitch, 30);
        }
        if ("volitans_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Volitans volitans) {
            float pitch = 0.95f + volitans.getRandom().nextFloat() * 0.1f;
            volitans.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_HURT.get(), 1.3f, pitch, 30);
        }
        if ("nulljaw_hurt".equals(animationTrigger) && !getUser().level().isClientSide
                && getUser() instanceof Nulljaw nulljaw) {
            float pitch = 0.95f + nulljaw.getRandom().nextFloat() * 0.1f;
            nulljaw.getSoundHandler().playMovingEntitySound(ModSounds.NULLJAW_HURT.get(), 1.2f, pitch, 44);
        }
    }

    @Override
    public boolean tryAbility() {
        return canUse();
    }

    @Override
    public boolean damageInterrupts() {
        return false;
    }

    @Override
    public boolean isOverlayAbility() {
        return true;
    }
}
