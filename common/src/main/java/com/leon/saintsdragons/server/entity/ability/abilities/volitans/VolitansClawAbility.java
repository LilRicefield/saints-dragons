package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.ability.debug.DragonAbilityDebug;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansClawAbility extends DragonAbility<Volitans> {
    private static final float BASE_DAMAGE = 11.0f;
    private static final double RANGE = 3.5;
    private static final double HITBOX_FORWARD_OFFSET = 2.0;
    private static final int DEBUG_COLOR = 0x66FFAA;
    private static final int DEBUG_TICKS = 20;
    private static final int CLAW_SOUND_TICKS = 26; // 1.3s

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 3),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private final boolean useLeftClaw;
    private boolean appliedHit;

    public VolitansClawAbility(DragonAbilityType<Volitans, VolitansClawAbility> type, Volitans user) {
        super(type, user, TRACK, 8);
        // Alternate swipes to make spam look less repetitive.
        this.useLeftClaw = user.getRandom().nextBoolean();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Volitans dragon = getUser();
            String controller = isAerial(dragon)
                    ? VolitansAnimationHandler.AIR_ACTION_CONTROLLER
                    : VolitansAnimationHandler.ACTION_CONTROLLER;
            dragon.triggerAnim(controller, useLeftClaw ? "swipe_left" : "swipe_right");
            if (!dragon.level().isClientSide) {
                float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
                dragon.getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_CLAWS.get(),
                        1.9f,
                        pitch,
                        CLAW_SOUND_TICKS
                );
            }
            appliedHit = false;
        }
    }

    private boolean isAerial(Volitans dragon) {
        return dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE || appliedHit) {
            return;
        }

        for (LivingEntity target : findTargets()) {
            applyHit(target);
        }
        appliedHit = true;
    }

    private List<LivingEntity> findTargets() {
        Volitans dragon = getUser();
        double range = RANGE;

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            sendDebugBox(dragon, range);
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return List.of(target);
            }
            return List.of();
        }

        sendDebugBox(dragon, range);
        return DragonMeleeGeometry.findBodySweepTargets(
                dragon,
                range,
                range,
                range,
                HITBOX_FORWARD_OFFSET,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void sendDebugBox(Volitans dragon, double range) {
        if (dragon.level().isClientSide) {
            return;
        }
        DragonMeleeGeometry.ForwardAttack attack = DragonMeleeGeometry.bodyForwardAttack(dragon).offset(HITBOX_FORWARD_OFFSET);
        DragonAbilityDebug.sendBox(dragon, attack.sweep(range, range, range), DEBUG_COLOR, DEBUG_TICKS);
    }

    private void applyHit(LivingEntity target) {
        Volitans dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("claw", BASE_DAMAGE) * dragon.getHungerMeleeDamageMultiplier();
        target.hurt(source, damage);
    }
}
