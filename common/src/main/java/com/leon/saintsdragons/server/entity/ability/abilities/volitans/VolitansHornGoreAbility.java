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
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansHornGoreAbility extends DragonAbility<Volitans> {
    private static final int HORN_GORE_SOUND_TICKS = 30; // 1.5s
    private static final float BASE_DAMAGE = 15.0f;
    private static final double RANGE = 7.8;
    private static final double GORE_ANGLE_DEG = 90.0;
    private static final double SWEEP_HORIZONTAL = 3.0;
    private static final double SWEEP_VERTICAL = 2.5;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 8)
    };

    private final java.util.Set<Integer> hitIds = new java.util.HashSet<>();

    public VolitansHornGoreAbility(DragonAbilityType<Volitans, VolitansHornGoreAbility> type, Volitans user) {
        super(type, user, TRACK, 12);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim(VolitansAnimationHandler.ACTION_CONTROLLER, "horn_gore");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_HORN_GORE.get(),
                        1.4f,
                        1.0f,
                        HORN_GORE_SOUND_TICKS
                );
            }
            hitIds.clear();
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        for (LivingEntity target : findTargets()) {
            if (hitIds.add(target.getId())) {
                applyGore(target);
            }
        }
    }

    private List<LivingEntity> findTargets() {
        Volitans dragon = getUser();
        double range = RANGE;

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 2.0D)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                range,
                SWEEP_HORIZONTAL,
                SWEEP_VERTICAL,
                GORE_ANGLE_DEG,
                range * 0.55D,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void applyGore(LivingEntity target) {
        Volitans dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("horn_gore", BASE_DAMAGE) * dragon.getHungerMeleeDamageMultiplier();
        target.hurt(source, damage);

        Vec3 look = dragon.getLookAngle().normalize();
        target.knockback(1.4f, -look.x, -look.z);
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, Math.max(motion.y, 0.35), motion.z);
    }
}
