package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansClawAbility extends DragonAbility<Volitans> {
    private static final float BASE_DAMAGE = 11.0f;
    private static final double BASE_RANGE = 5.0;
    private static final double RIDDEN_RANGE_BONUS = 1.5;
    private static final double CLAW_ANGLE_DEG = 95.0;
    private static final double SWEEP_HORIZONTAL = 3.0;
    private static final double SWEEP_VERTICAL = 2.6;

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
            getUser().triggerAnim("actions", useLeftClaw ? "swipe_left" : "swipe_right");
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
        Vec3 origin = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        double range = BASE_RANGE + (dragon.getControllingPassenger() != null ? RIDDEN_RANGE_BONUS : 0.0);
        double cosLimit = Math.cos(Math.toRadians(CLAW_ANGLE_DEG));

        AABB sweep = new AABB(origin, origin.add(look.scale(range))).inflate(SWEEP_HORIZONTAL, SWEEP_VERTICAL, SWEEP_HORIZONTAL);
        return dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                        entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity))
                .stream()
                .filter(entity -> {
                    Vec3 toward = entity.getBoundingBox().getCenter().subtract(origin);
                    double len = toward.length();
                    if (len <= 1.0e-4) {
                        return false;
                    }
                    double dot = toward.scale(1.0 / len).dot(look);
                    return dot > 0.0 && (dot >= cosLimit || len < (range * 0.6));
                })
                .toList();
    }

    private void applyHit(LivingEntity target) {
        Volitans dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = BASE_DAMAGE * dragon.getHungerMeleeDamageMultiplier();
        target.hurt(source, damage);
    }
}
