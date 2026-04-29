package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansBiteAbility extends DragonAbility<Volitans> {
    private static final int SOUND_TICKS = 30;
    private static final float BASE_DAMAGE = 12.0f;
    private static final double RANGE = 6.0;
    private static final double ANGLE_DEGREES = 80.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 7)
    };

    private boolean appliedHit;

    public VolitansBiteAbility(DragonAbilityType<Volitans, VolitansBiteAbility> type, Volitans user) {
        super(type, user, TRACK, 10);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim("instant", "bite");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_BITE.get(),
                        1.4f,
                        1.0f,
                        SOUND_TICKS
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

        List<LivingEntity> targets = findTargets();
        for (LivingEntity target : targets) {
            applyHit(target);
        }
        appliedHit = true;
    }

    private List<LivingEntity> findTargets() {
        Volitans dragon = getUser();

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                RANGE,
                ANGLE_DEGREES,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void applyHit(LivingEntity target) {
        Volitans dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);

        float damage = dragon.getConfiguredAbilityDamage("bite", BASE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();

        target.hurt(source, damage);
        Vec3 push = dragon.getLookAngle().scale(0.30);
        target.push(push.x, 0.1, push.z);
    }
}
