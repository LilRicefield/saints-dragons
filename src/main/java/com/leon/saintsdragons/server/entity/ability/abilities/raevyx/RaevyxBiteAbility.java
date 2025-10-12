package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxLightningChainEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.conductivity.ElectricalConductivityState;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.*;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Bite + chain lightning ability for LightningDragon.
 */
public class RaevyxBiteAbility extends DragonAbility<Raevyx> {
    // Tuning knobs
    private static final float BITE_DAMAGE = 25.0f;
    // Slightly larger reach to match large model scale and AI stop distance
    private static final double BITE_RANGE = 4.6;
    // Extra range when ridden to be more forgiving for small mobs
    private static final double BITE_RANGE_RIDDEN = 8.0; // match horn gore forgiveness while ridden
    // More forgiving cone; widen to reduce whiffs while steering
    private static final double BITE_ANGLE_DEG = 85.0; // half-angle of cone
    // Forward bite sweep extents
    private static final double BITE_SWIPE_HORIZONTAL = 2.5;
    private static final double BITE_SWIPE_HORIZONTAL_RIDDEN = 2.5;
    private static final double BITE_SWIPE_VERTICAL = 2.5;


    private static final float CHAIN_DAMAGE_BASE = 10.0f;
    private static final double CHAIN_RADIUS = 7.0;
    private static final int CHAIN_JUMPS = 5;
    private static final float CHAIN_FALLOFF = 0.75f;

    // Sections: startup (windup), active (hit frame), recovery
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 3),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 2),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 3)
    };

    private boolean didHitThisActive = false;

    public RaevyxBiteAbility(DragonAbilityType<Raevyx, RaevyxBiteAbility> type, Raevyx user) {
        super(type, user, TRACK, 3);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Trigger bite animation via GeckoLib action controller (one-shot)
            getUser().triggerAnim("action", "lightning_bite");
            didHitThisActive = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) return;


        if (section.sectionType == AbilitySectionType.ACTIVE && !didHitThisActive) {
            // Play bite sound at mouth position when the bite "lands"
            if (!getLevel().isClientSide) {
                net.minecraft.world.level.Level lvl = getLevel();
                net.minecraft.world.phys.Vec3 mouth = getUser().getMouthPosition();
                lvl.playSound(
                        null,
                        mouth.x, mouth.y, mouth.z,
                        com.leon.saintsdragons.common.registry.ModSounds.RAEVYX_BITE.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL,
                        1.0f,
                        0.95f + getUser().getRandom().nextFloat() * 0.1f
                );
            }
            // Apply bite and chain once at the start of ACTIVE
            LivingEntity primary = findPrimaryTarget();
            // Fallback: use current target if within generous melee range
            if (primary == null) {
                LivingEntity t = getUser().getTarget();
                if (t != null && t.isAlive()) {
                    double d = t.distanceTo(getUser());
                    if (d <= 5.2) {
                        primary = t;
                    }
                }
            }
            // Fallback 2: short raycast from mouth along look to help during riding fly-bys
            if (primary == null) {
                boolean ridden = getUser().getControllingPassenger() != null;
                primary = raycastTargetAlongMouth(ridden ? 7.5 : 5.5, ridden ? 2.0 : 1.0);
            }
            if (primary != null) {
                bitePrimary(primary);
                chainFrom(primary);
            }
            didHitThisActive = true;
        }
    }

    // ===== Core mechanics =====

    private LivingEntity findPrimaryTarget() {
        Raevyx wyvern = getUser();
        Vec3 mouth = wyvern.getMouthPosition();
        Vec3 look = wyvern.getLookAngle().normalize();

        // Use a larger effective range when ridden to accommodate height/offset while mounted
        boolean ridden = wyvern.getControllingPassenger() != null;
        double effectiveRange = ridden ? BITE_RANGE_RIDDEN : BITE_RANGE;

        // Forward sweep out from the mouth so hits originate ahead of the head
        double horizontalInflate = ridden ? BITE_SWIPE_HORIZONTAL_RIDDEN : BITE_SWIPE_HORIZONTAL;
        AABB forwardSweep = new AABB(mouth, mouth.add(look.scale(effectiveRange)))
                .inflate(horizontalInflate, BITE_SWIPE_VERTICAL, horizontalInflate);
        List<LivingEntity> candidates = wyvern.level().getEntitiesOfClass(LivingEntity.class, forwardSweep,
                e -> e != wyvern && e.isAlive() && e.attackable() && !isAllied(wyvern, e));

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

    private void bitePrimary(LivingEntity primary) {
        Raevyx wyvern = getUser();
        DamageSource src = wyvern.level().damageSources().mobAttack(wyvern);
        float mult = wyvern.getDamageMultiplier();
        primary.hurt(src, BITE_DAMAGE * mult);
        wyvern.noteAggroFrom(primary);
    }

    private void chainFrom(LivingEntity start) {
        Raevyx wyvern = getUser();
        ElectricalConductivityState conductivity = wyvern.getConductivityState();
        Set<LivingEntity> hit = new HashSet<>();
        hit.add(start);

        LivingEntity current = start;
        float damage = CHAIN_DAMAGE_BASE;

        for (int i = 0; i < CHAIN_JUMPS; i++) {
            LivingEntity next = findNearestChainTarget(current, hit, conductivity);
            if (next == null) break;

            // Damage and VFX
            float mult = wyvern.getDamageMultiplier();
            next.hurt(wyvern.level().damageSources().lightningBolt(), damage * mult * conductivity.damageMultiplier());
            wyvern.noteAggroFrom(next);
            spawnArc(current.position().add(0, current.getBbHeight() * 0.5, 0),
                    next.position().add(0, next.getBbHeight() * 0.5, 0), conductivity);

            hit.add(next);
            current = next;
            damage *= CHAIN_FALLOFF;
        }
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
        Raevyx wyvern = getUser();
        Vec3 start = wyvern.getMouthPosition();
        Vec3 look = wyvern.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(maxDistance));

        AABB sweep = new AABB(start, end).inflate(inflateRadius);
        var hit = ProjectileUtil.getEntityHitResult(wyvern.level(), wyvern, start, end, sweep,
                e -> e instanceof LivingEntity le && e != wyvern && e.isAlive() && le.attackable() && !isAllied(wyvern, e),
                (float)(maxDistance * maxDistance));
        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    private LivingEntity findNearestChainTarget(LivingEntity origin, Set<LivingEntity> exclude, ElectricalConductivityState conductivity) {
        Raevyx wyvern = getUser();
        double rangeMult = conductivity.rangeMultiplier();
        List<LivingEntity> nearby = DragonMathUtil.getEntitiesNearby(origin, LivingEntity.class, CHAIN_RADIUS * rangeMult);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearby) {
            if (e == wyvern || exclude.contains(e) || !e.isAlive() || !e.attackable() || isAllied(wyvern, e)) continue;
            double d = e.distanceToSqr(origin);
            if (d < bestDist) {
                // Optional LOS check for coherence
                if (!wyvern.getSensing().hasLineOfSight(e)) continue;
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isAllied(Raevyx wyvern, Entity other) {
        // Use the comprehensive ally system from DragonEntity
        return wyvern.isAlly(other);
    }

    private void spawnArc(Vec3 from, Vec3 to, ElectricalConductivityState conductivity) {
        if (!(getLevel() instanceof ServerLevel server)) return;
        
        // Create a single lightning chain entity instead of multiple particles
        Raevyx wyvern = getUser();
        float damage = CHAIN_DAMAGE_BASE * wyvern.getDamageMultiplier() * conductivity.damageMultiplier();
        
        RaevyxLightningChainEntity lightningEntity = new RaevyxLightningChainEntity(
            server, from, to, 
            damage, 1.2f,
                wyvern, false // Not a chain lightning, just a single strike
        );
        
        server.addFreshEntity(lightningEntity);
    }
}