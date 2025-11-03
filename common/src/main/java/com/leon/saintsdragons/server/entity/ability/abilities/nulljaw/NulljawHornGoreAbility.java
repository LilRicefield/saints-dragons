package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Rift Drake horn gore melee: high damage + very strong knockback.
 * Stronger knockback than Lightning Dragon version.
 * Works in both phase 1 and phase 2.
 */
public class NulljawHornGoreAbility extends DragonAbility<Nulljaw> {
    private static final float GORE_DAMAGE = 16.0f;
    private static final double GORE_RANGE = 7.0;
    private static final double GORE_RANGE_RIDDEN = 8.5;
    private static final double GORE_ANGLE_DEG = 90.0; // half-angle

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 5),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 6),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 6)
    };

    // Track entities already hit during the ACTIVE window so we don't double hit in multi-tick ACTIVE
    private final java.util.Set<Integer> hitIdsThisUse = new java.util.HashSet<>();

    public NulljawHornGoreAbility(DragonAbilityType<Nulljaw, NulljawHornGoreAbility> type, Nulljaw user) {
        super(type, user, TRACK, 3);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Trigger gore animation via normal GeckoLib action trigger
            getUser().triggerAnim("action", "horn_gore");
            hitIdsThisUse.clear();
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            // TODO: Add horn gore sound when created
            // Sound will be triggered via animation keyframes
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) return;
        if (section.sectionType != AbilitySectionType.ACTIVE) return;

        // Multi-target horn gore: hit all valid entities in cone that haven't been hit yet
        java.util.List<LivingEntity> candidates = findTargets();
        java.util.List<LivingEntity> newHits = new java.util.ArrayList<>();
        for (LivingEntity le : candidates) {
            if (hitIdsThisUse.add(le.getId())) {
                newHits.add(le);
            }
        }
        if (!newHits.isEmpty()) {
            for (LivingEntity le : newHits) {
                applyGore(le);
            }
        }
    }

    private java.util.List<LivingEntity> findTargets() {
        Nulljaw dragon = getUser();
        Vec3 head = dragon.getHeadPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        boolean ridden = dragon.getControllingPassenger() != null;
        double range = ridden ? GORE_RANGE_RIDDEN : GORE_RANGE;

        // Use drake body bounding box inflated by range as broadphase
        AABB broad = dragon.getBoundingBox().inflate(range, range, range);
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, broad,
                e -> e != dragon && e.isAlive() && e.attackable() && !isAllied(dragon, e));

        double cosLimit = Math.cos(Math.toRadians(GORE_ANGLE_DEG));
        java.util.List<LivingEntity> hits = new java.util.ArrayList<>();

        for (LivingEntity e : candidates) {
            // Distance from head to target AABB
            double dist = distancePointToAABB(head, e.getBoundingBox());
            if (dist > range + 0.4) continue;

            // Angle test from head toward target center
            Vec3 toward = e.getBoundingBox().getCenter().subtract(head);
            double len = toward.length();
            if (len < 1.0e-4) continue;
            double dot = toward.normalize().dot(look);

            boolean close = dist < (range * 0.6);
            boolean angleOk = dot >= cosLimit;
            if (ridden) {
                // More lenient while ridden or accept within body range
                double bodyDist = distancePointToAABB(e.position(), dragon.getBoundingBox());
                angleOk = angleOk || dot >= (cosLimit * 0.7) || bodyDist <= range;
            }
            if (!(close || angleOk)) continue;
            hits.add(e);
        }
        return hits;
    }

    private void applyGore(LivingEntity target) {
        Nulljaw dragon = getUser();
        DamageSource src = dragon.level().damageSources().mobAttack(dragon);

        // Phase 2 deals more damage
        float damageMultiplier = dragon.isPhaseTwoActive() ? 1.3f : 1.0f;

        // Armor penetration: ignore 3 armor points when calculating effective damage
        // Phase 2 ignores 5 armor points (stronger penetration)
        float armorPenetration = dragon.isPhaseTwoActive() ? 5.0f : 3.0f;
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float desiredPostArmor = damageAfterArmor(GORE_DAMAGE * damageMultiplier, Math.max(0f, armor - armorPenetration), toughness);

        // Find a raw damage value which, after the target's ACTUAL armor/toughness, equals desiredPostArmor
        float rawToDeal = solveRawDamageForPostArmor(desiredPostArmor, armor, toughness);
        target.hurt(src, rawToDeal);

        // Very strong directional knockback away from drake head
        // Stronger than Lightning Dragon (2.0 base vs 1.4)
        Vec3 look = dragon.getLookAngle().normalize();
        double strength = dragon.isPhaseTwoActive() ? 3.5 : 2.0; // Even stronger in phase 2
        // knockback(strength, x, z): applies horizontal knockback opposite to (x,z)
        target.knockback((float) strength, -look.x, -look.z);

        // Higher vertical lift - significantly stronger than Lightning Dragon
        Vec3 dv = target.getDeltaMovement();
        float verticalLift = dragon.isPhaseTwoActive() ? 0.9f : 0.5f; // Phase 2 launches enemies higher
        target.setDeltaMovement(dv.x, Math.max(dv.y, verticalLift), dv.z);

    }

    // ===== helpers =====
    private static float damageAfterArmor(float damage, float armor, float toughness) {
        // Mirrors vanilla CombatRules.getDamageAfterAbsorb
        float f = 2.0F + toughness / 4.0F;
        float reduction = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - reduction / 25.0F);
    }

    private static float solveRawDamageForPostArmor(float desiredPostArmor, float armor, float toughness) {
        // Binary search a raw damage amount such that after-armor equals desiredPostArmor
        // Monotonic increasing, so safe to search
        float lo = 0.0f;
        float hi = Math.max(desiredPostArmor + 16.0f, 16.0f); // reasonable starting upper bound
        // Ensure upper bound is sufficient
        for (int i = 0; i < 8 && damageAfterArmor(hi, armor, toughness) < desiredPostArmor; i++) {
            hi *= 2.0f;
        }
        // Binary search
        for (int it = 0; it < 20; it++) { // ~1e-6 relative precision typically
            float mid = (lo + hi) * 0.5f;
            float val = damageAfterArmor(mid, armor, toughness);
            if (val < desiredPostArmor) lo = mid; else hi = mid;
        }
        return (lo + hi) * 0.5f;
    }

    private static double distancePointToAABB(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isAllied(Nulljaw dragon, Entity other) {
        // Use the comprehensive ally system from DragonEntity
        return dragon.isAlly(other);
    }
}
