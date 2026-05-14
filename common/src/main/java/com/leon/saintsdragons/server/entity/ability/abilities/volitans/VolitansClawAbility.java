package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
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
    private static final double RANGE = 6.5;
    private static final double CLAW_ANGLE_DEG = 95.0;
    private static final double SWEEP_HORIZONTAL = 3.0;
    private static final double SWEEP_VERTICAL = 2.6;
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
            getUser().triggerAnim(VolitansAnimationHandler.ACTION_CONTROLLER, useLeftClaw ? "swipe_left" : "swipe_right");
            if (!getUser().level().isClientSide) {
                float pitch = 0.96f + getUser().getRandom().nextFloat() * 0.08f;
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_CLAWS.get(),
                        1.9f,
                        pitch,
                        CLAW_SOUND_TICKS
                );
            }
            appliedHit = false;
        }
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
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                range,
                SWEEP_HORIZONTAL,
                SWEEP_VERTICAL,
                CLAW_ANGLE_DEG,
                range * 0.6D,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void applyHit(LivingEntity target) {
        Volitans dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("claw", BASE_DAMAGE) * dragon.getHungerMeleeDamageMultiplier();
        target.hurt(source, damage);
    }
}
