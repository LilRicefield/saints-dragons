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
 * Powerful AoE bite attack for Ignivorus. Deals fire damage with armor penetration.
 * Triggers on left-click (attack key) when ridden.
 *
 * Hits ALL entities in a sphere around the mouth (mouth_origin locator from .geo file).
 * Range is calculated based on the mouth_origin bone position in the animated model.
 */
public class IgnivorusBiteAbility extends DragonAbility<Ignivorus> {
    // 50 health base damage with 5 armor points ignored
    private static final float BASE_DAMAGE = 50.0f;
    private static final float ARMOR_PENETRATION = 5.0f; // Armor points bypassed

    // AoE radius around the mouth position
    private static final double BASE_AOE_RADIUS = 5.0; // Base sphere radius
    private static final double RIDDEN_RADIUS_BONUS = 2.0; // Extra radius when ridden
    private static final double AIR_RADIUS_BONUS = 1.0; // Bonus radius when flying

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

        // Apply damage as a direct melee hit so even fire-immune bosses (e.g. Wardens) take it,
        // then layer the burning effect separately.
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);

        // Approximate armor penetration by boosting the raw hit damage.
        float armorPenDamage = damage + ARMOR_PENETRATION;

        target.hurt(physicalSource, armorPenDamage);

        // Set target on fire for extra burn damage (3 seconds)
        target.setSecondsOnFire(3);

        // Apply knockback based on look direction
        Vec3 push = dragon.getLookAngle().scale(dragon.isFlying() ? 0.4 : 0.25);
        target.push(push.x, dragon.isFlying() ? 0.2 : 0.08, push.z);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();

        // Calculate AoE radius dynamically based on state
        double radius = BASE_AOE_RADIUS;
        if (dragon.getControllingPassenger() != null) {
            radius += RIDDEN_RADIUS_BONUS;
        }
        if (dragon.isFlying()) {
            radius += AIR_RADIUS_BONUS;
        }

        // Get mouth position from mouth_origin locator in .geo file (with fallback)
        Vec3 mouthPos = dragon.getMouthPosition();

        // Create spherical detection area around the mouth
        // Inflate equally in all directions to create a sphere
        AABB detectionBox = new AABB(mouthPos, mouthPos).inflate(radius);

        // Find ALL valid targets in the sphere - no angle restriction!
        double finalRadius = radius;
        double finalRadius1 = radius;
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, detectionBox,
                entity -> {
                    if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                        return false;
                    }

                    // Additional sphere check - make sure they're within the actual radius
                    // This catches entities whose AABB intersects the box but center is outside
                    Vec3 entityCenter = entity.getBoundingBox().getCenter();
                    double distSqr = entityCenter.distanceToSqr(mouthPos);
                    return distSqr <= (finalRadius * finalRadius1);
                });

        // Sort by distance (closest first) for consistent behavior
        candidates.sort(Comparator.comparingDouble(e ->
            e.getBoundingBox().getCenter().distanceToSqr(mouthPos)
        ));

        return candidates;
    }
}
