package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansBiteAbility extends DragonAbility<Volitans> {
    private static final int BITE_SOUND_TICKS = 30; // 1.5s
    private static final float BASE_DAMAGE = 12.0f;
    private static final double BASE_RANGE = 4.6;
    private static final double RIDDEN_RANGE_BONUS = 1.4;
    private static final double BITE_ANGLE_DEG = 80.0;
    private static final double SWEEP_HORIZONTAL = 2.5;
    private static final double SWEEP_VERTICAL = 2.2;

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
                        BITE_SOUND_TICKS
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
        Vec3 origin = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        double range = BASE_RANGE + (dragon.getControllingPassenger() != null ? RIDDEN_RANGE_BONUS : 0.0);
        double cosLimit = Math.cos(Math.toRadians(BITE_ANGLE_DEG));

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            if (isValidTarget(dragon, target) && isTargetInArc(target, origin, look, range, cosLimit)) {
                return List.of(target);
            }
            return List.of();
        }

        AABB sweep = new AABB(origin, origin.add(look.scale(range))).inflate(SWEEP_HORIZONTAL, SWEEP_VERTICAL, SWEEP_HORIZONTAL);
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        return candidates.stream()
                .filter(entity -> isTargetInArc(entity, origin, look, range, cosLimit))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(dragon)))
                .toList();
    }

    private boolean isValidTarget(Volitans dragon, LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.attackable()
                && !dragon.isAlly(target)
                && dragon.isTargetValid(target);
    }

    private boolean isTargetInArc(LivingEntity entity, Vec3 origin, Vec3 look, double range, double cosLimit) {
        Vec3 toward = entity.getBoundingBox().getCenter().subtract(origin);
        double len = toward.length();
        if (len <= 1.0e-4 || len > range + SWEEP_HORIZONTAL) {
            return false;
        }
        double dot = toward.scale(1.0 / len).dot(look);
        return dot > 0.0 && (dot >= cosLimit || len < (range * 0.5));
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
