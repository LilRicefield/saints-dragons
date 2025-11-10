package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Powerful bite attack for Ignivorus. Deals fire damage with armor penetration.
 * Triggers on left-click (attack key) when ridden.
 */
public class IgnivorusBiteAbility extends DragonAbility<Ignivorus> {
    // 50 health base damage with 5 armor points ignored
    private static final float BASE_DAMAGE = 50.0f;
    private static final float ARMOR_PENETRATION = 5.0f; // Armor points bypassed

    private static final double BASE_RANGE = 12.0; // Massive dragon = longer reach
    private static final double RIDDEN_RANGE_BONUS = 8.0; // Extra range when ridden for precision
    private static final double AIR_RANGE_BONUS = 2.0; // Bonus range when flying
    private static final double HIT_ANGLE_COS = Math.cos(Math.toRadians(80.0)); // Wide cone for large head

    // Animation timing: startup → active hitframe → recovery
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),    // Windup
            new AbilitySectionDuration(ACTIVE, 3),     // Hit window
            new AbilitySectionDuration(RECOVERY, 8)    // Recovery
    };

    private boolean appliedHit;

    public IgnivorusBiteAbility(DragonAbilityType<Ignivorus, IgnivorusBiteAbility> type,
                                Ignivorus user) {
        super(type, user, TRACK, 20); // 1 second cooldown (20 ticks)
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();
            // Trigger bite animation via GeckoLib action controller
            dragon.triggerAnim("action", "bite");
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        // Apply damage during ACTIVE window (hit frame)
        if (section.sectionType == ACTIVE && !appliedHit) {
            Ignivorus dragon = getUser();

            List<LivingEntity> targets = selectTargets();

            // Fallback to current target if no targets in cone
            if (targets.isEmpty()) {
                LivingEntity currentTarget = dragon.getTarget();
                if (currentTarget != null && currentTarget.isAlive() && !dragon.isAlly(currentTarget)) {
                    targets = List.of(currentTarget);
                }
            }

            // Apply bite to all valid targets
            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        // Calculate base damage from attack attribute
        float damage = BASE_DAMAGE;
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double value = attackAttr.getValue();
            if (value > 0) {
                // Use attribute value if it's higher than base
                damage = Math.max(BASE_DAMAGE, (float) value);
            }
        }

        // Apply fire damage with armor penetration
        // In Minecraft, armor penetration is handled by using specific damage sources
        // We'll use inFire damage source which bypasses some armor
        DamageSource source = dragon.level().damageSources().inFire();

        // Calculate armor penetration
        // Base damage calculation: damage - (armor * armorToughness)
        // We effectively increase damage to compensate for ARMOR_PENETRATION points
        float armorPenDamage = damage + ARMOR_PENETRATION;

        target.hurt(source, armorPenDamage);

        // Set target on fire for extra burn damage (3 seconds)
        target.setSecondsOnFire(3);

        // Apply knockback based on look direction
        Vec3 push = dragon.getLookAngle().scale(dragon.isFlying() ? 0.4 : 0.25);
        target.push(push.x, dragon.isFlying() ? 0.2 : 0.08, push.z);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();

        // Calculate effective range based on state
        double range = BASE_RANGE;
        if (dragon.getControllingPassenger() != null) {
            range += RIDDEN_RANGE_BONUS;
        }
        if (dragon.isFlying()) {
            range += AIR_RANGE_BONUS;
        }

        // Create cone-shaped detection area
        Vec3 origin = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 end = origin.add(look.scale(range));
        AABB sweep = new AABB(origin, end).inflate(2.5, 2.0, 2.5); // Wide area for large dragon

        // Find all valid targets in area
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        final double effectiveRange = range;
        List<LivingEntity> results = new ArrayList<>();

        // Filter by cone angle and distance, prioritize closest targets
        candidates.stream()
                .map(entity -> {
                    Vec3 center = entity.getBoundingBox().getCenter();
                    Vec3 toward = center.subtract(origin);
                    double distanceSqr = toward.lengthSqr();

                    if (distanceSqr < 1.0e-6) {
                        // Target is on top of mouth - always hit
                        return new TargetScore(entity, 1.0, 0.0);
                    }

                    Vec3 dir = toward.normalize();
                    double dot = dir.dot(look);

                    // Check if target is within cone angle
                    if (dot < HIT_ANGLE_COS) {
                        // Allow close targets even if outside cone
                        double closeEnough = center.distanceToSqr(origin);
                        if (closeEnough > (effectiveRange * effectiveRange * 0.5)) {
                            return null; // Too far and not in cone
                        }
                    }

                    return new TargetScore(entity, dot, distanceSqr);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingDouble(TargetScore::dot).reversed()    // Prefer targets in front
                        .thenComparingDouble(TargetScore::distanceSqr))  // Then by distance
                .map(TargetScore::entity)
                .forEach(entity -> {
                    if (!results.contains(entity)) {
                        results.add(entity);
                    }
                });

        return results;
    }

    /**
     * Helper record for scoring targets by angle and distance
     */
    private record TargetScore(LivingEntity entity, double dot, double distanceSqr) {}
}
