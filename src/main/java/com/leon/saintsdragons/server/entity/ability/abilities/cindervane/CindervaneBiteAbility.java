package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Close-range bite for the Amphithere. Triggers a ground or air animation
 * based on flight state and applies reliable melee damage in front of the snout.
 */
public class CindervaneBiteAbility extends DragonAbility<Cindervane> {
    private static final float BASE_DAMAGE = 12.0f;
    private static final double BASE_RANGE = 10.0;
    private static final double RIDDEN_RANGE_BONUS = 10.0;
    private static final double AIR_RANGE_BONUS = 0.6;
    private static final double HIT_ANGLE_COS = Math.cos(Math.toRadians(75.0));

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private boolean appliedHit;

    public CindervaneBiteAbility(DragonAbilityType<Cindervane, CindervaneBiteAbility> type,
                                 Cindervane user) {
        super(type, user, TRACK, 15);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Cindervane dragon = getUser();
            // Use unified bite animation for both air and ground
            dragon.triggerAnim("actions", "bite");
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE && !appliedHit) {
            Cindervane dragon = getUser();

            List<LivingEntity> targets = selectTargets();

            if (targets.isEmpty()) {
                LivingEntity currentTarget = dragon.getTarget();
                if (currentTarget != null && currentTarget.isAlive() && !dragon.isAlly(currentTarget)) {
                    targets = List.of(currentTarget);
                }
            }

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Cindervane dragon, LivingEntity target) {

        float damage = BASE_DAMAGE;
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double value = attackAttr.getValue();
            if (value > 0) {
                damage = (float) value;
            }
        }

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.3);
        target.push(push.x, dragon.isFlying() ? 0.15 : 0.05, push.z);
    }

    private List<LivingEntity> selectTargets() {
        Cindervane dragon = getUser();
        double range = BASE_RANGE;
        if (dragon.getControllingPassenger() != null) {
            range += RIDDEN_RANGE_BONUS;
        }
        if (dragon.isFlying()) {
            range += AIR_RANGE_BONUS;
        }

        Vec3 origin = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 end = origin.add(look.scale(range));
        AABB sweep = new AABB(origin, end).inflate(1.2, 1.0, 1.2);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        final double effectiveRange = range;
        List<LivingEntity> results = new ArrayList<>();

        candidates.stream()
                .map(entity -> {
                    Vec3 center = entity.getBoundingBox().getCenter();
                    Vec3 toward = center.subtract(origin);
                    double distanceSqr = toward.lengthSqr();
                    if (distanceSqr < 1.0e-6) {
                        return new TargetScore(entity, 1.0, 0.0);
                    }
                    Vec3 dir = toward.normalize();
                    double dot = dir.dot(look);
                    if (dot < HIT_ANGLE_COS) {
                        double closeEnough = center.distanceToSqr(origin);
                        if (closeEnough > (effectiveRange * effectiveRange * 0.64)) {
                            return null;
                        }
                    }
                    return new TargetScore(entity, dot, distanceSqr);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingDouble(TargetScore::dot).reversed()
                        .thenComparingDouble(TargetScore::distanceSqr))
                .map(TargetScore::entity)
                .forEach(entity -> {
                    if (!results.contains(entity)) {
                        results.add(entity);
                    }
                });

        return results;
    }

    private record TargetScore(LivingEntity entity, double dot, double distanceSqr) {}
}
