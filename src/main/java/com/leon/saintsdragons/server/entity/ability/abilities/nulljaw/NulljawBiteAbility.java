package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Phase 1 bite attack for the Rift Drake. Shorter range than Amphithere due to shorter neck.
 */
public class NulljawBiteAbility extends DragonAbility<Nulljaw> {
    private static final float BASE_DAMAGE = 40.0f;
    private static final double BASE_RANGE = 7.5;
    private static final double RIDDEN_RANGE_BONUS = 1.0;
    private static final double SWIM_RANGE_BONUS = 8.0;
    private static final double BITE_ANGLE_DEG = 90.0;     // Half-angle of bite cone
    private static final double BITE_SWIPE_HORIZONTAL = 5.0;
    private static final double BITE_SWIPE_HORIZONTAL_RIDDEN = 1.5;
    private static final double BITE_SWIPE_VERTICAL = 5.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 6),
            new AbilitySectionDuration(RECOVERY, 6)
    };

    private boolean appliedHit;

    public NulljawBiteAbility(DragonAbilityType<Nulljaw, NulljawBiteAbility> type,
                              Nulljaw user) {
        super(type, user, TRACK, 15);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Nulljaw dragon = getUser();
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

        if (section.sectionType == ACTIVE && !appliedHit) {
            Nulljaw dragon = getUser();

            // Find primary target using Lightning Dragon's superior geometry
            LivingEntity primary = findPrimaryTarget();

            // Fallback 1: use current target if within generous melee range
            if (primary == null) {
                LivingEntity t = dragon.getTarget();
                if (t != null && t.isAlive()) {
                    double d = t.distanceTo(dragon);
                    if (d <= 5.2) {
                        primary = t;
                    }
                }
            }

            // Fallback 2: short raycast from mouth along look direction
            if (primary == null) {
                boolean ridden = dragon.getControllingPassenger() != null;
                double effectiveRange = getEffectiveRange();
                primary = raycastTargetAlongMouth(effectiveRange + 2.0, ridden ? 2.0 : 1.0);
            }

            if (primary != null) {
                applyHit(dragon, primary);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Nulljaw dragon, LivingEntity target) {
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
        target.push(push.x, dragon.isSwimming() ? 0.15 : 0.05, push.z);
    }

    // ===== Range calculation =====

    private double getEffectiveRange() {
        Nulljaw dragon = getUser();
        double range = BASE_RANGE;

        if (dragon.getControllingPassenger() != null) {
            range += RIDDEN_RANGE_BONUS;
        }
        if (dragon.isSwimming()) {
            range += SWIM_RANGE_BONUS;
        }

        return range;
    }

    // ===== Lightning Dragon's superior target finding =====

    private LivingEntity findPrimaryTarget() {
        Nulljaw dragon = getUser();
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = getEffectiveRange();

        // Forward sweep out from the mouth so hits originate ahead of the head
        double horizontalInflate = ridden ? BITE_SWIPE_HORIZONTAL_RIDDEN : BITE_SWIPE_HORIZONTAL;
        AABB forwardSweep = new AABB(mouth, mouth.add(look.scale(effectiveRange)))
                .inflate(horizontalInflate, BITE_SWIPE_VERTICAL, horizontalInflate);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, forwardSweep,
                e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e));

        double cosLimit = Math.cos(Math.toRadians(BITE_ANGLE_DEG));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity e : candidates) {
            // Compute closest point on target's AABB to the mouth
            double distToAabb = distancePointToAABB(mouth, e.getBoundingBox());
            if (distToAabb > effectiveRange + 0.4) continue;

            // Direction toward the closest point for angle test
            Vec3 toward = closestPointOnAABB(mouth, e.getBoundingBox()).subtract(mouth);
            double len = toward.length();
            if (len <= 0.0001) continue;
            Vec3 dir = toward.scale(1.0 / len);
            double dot = dir.dot(look);

            // Require the bite to project forward from the head
            if (dot <= 0.0) continue;

            // Be forgiving with angle when very close; otherwise enforce cone
            boolean veryClose = distToAabb < (effectiveRange * 0.35);
            boolean goodAngle = dot >= cosLimit;
            if (ridden) {
                // Slightly relax the cone while ridden but keep hits forward
                goodAngle = goodAngle || dot >= (cosLimit * 0.75);
            }
            if (!(veryClose || goodAngle)) continue;

            if (distToAabb < bestDist) {
                bestDist = distToAabb;
                best = e;
            }
        }
        return best;
    }

    // ===== Geometry helpers =====

    private static double distancePointToAABB(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 closestPointOnAABB(Vec3 p, AABB box) {
        double cx = Mth.clamp(p.x, box.minX, box.maxX);
        double cy = Mth.clamp(p.y, box.minY, box.maxY);
        double cz = Mth.clamp(p.z, box.minZ, box.maxZ);
        return new Vec3(cx, cy, cz);
    }

    private LivingEntity raycastTargetAlongMouth(double maxDistance, double inflateRadius) {
        Nulljaw dragon = getUser();
        Vec3 start = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(maxDistance));

        AABB sweep = new AABB(start, end).inflate(inflateRadius);
        var hit = ProjectileUtil.getEntityHitResult(dragon.level(), dragon, start, end, sweep,
                e -> e instanceof LivingEntity le && e != dragon && e.isAlive() && le.attackable() && !dragon.isAlly(e),
                (float)(maxDistance * maxDistance));

        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }
}
