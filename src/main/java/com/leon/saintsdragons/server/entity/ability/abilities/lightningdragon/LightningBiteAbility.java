package com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon;

import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
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
public class LightningBiteAbility extends DragonAbility<LightningDragonEntity> {
    // Tuning knobs
    private static final float BITE_DAMAGE = 25.0f;
    // Slightly larger reach to match large model scale and AI stop distance
    private static final double BITE_RANGE = 4.6;
    // Extra range when ridden to be more forgiving for small mobs
    private static final double BITE_RANGE_RIDDEN = 8.0; // match horn gore forgiveness while ridden
    // More forgiving cone; widen to reduce whiffs while steering
    private static final double BITE_ANGLE_DEG = 120.0; // half-angle of cone

    private static final float CHAIN_DAMAGE_BASE = 4.0f;
    private static final double CHAIN_RADIUS = 7.0;
    private static final int CHAIN_JUMPS = 4;
    private static final float CHAIN_FALLOFF = 0.75f;

    // Sections: startup (windup), active (hit frame), recovery
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 6),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 2),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 6)
    };

    private boolean didHitThisActive = false;

    public LightningBiteAbility(DragonAbilityType<LightningDragonEntity, LightningBiteAbility> type, LightningDragonEntity user) {
        super(type, user, TRACK, 24); // ~1.2s cooldown
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
                        com.leon.saintsdragons.common.registry.ModSounds.DRAGON_BITE.get(),
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
            // Fallback 3: if ridden, pick nearest eligible in body range ignoring angle
            if (primary == null && getUser().getControllingPassenger() != null) {
                primary = findNearestInBodyRangeIgnoringAngle();
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
        LightningDragonEntity dragon = getUser();
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        // Use a larger effective range when ridden to accommodate height/offset while mounted
        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = ridden ? BITE_RANGE_RIDDEN : BITE_RANGE;

        // Broadphase: sphere/box around the mouth, not the entity center
        AABB broadphase = dragon.getBoundingBox().inflate(effectiveRange, effectiveRange, effectiveRange);
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, broadphase,
                e -> e != dragon && e.isAlive() && e.attackable() && !isAllied(dragon, e));

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

            // Be forgiving with angle when very close; otherwise enforce cone
            boolean veryClose = distToAabb < (effectiveRange * 0.6);
            boolean goodAngle = dot >= cosLimit;
            // Accept within body range while ridden regardless of angle, to make clicks lenient
            double distToDragon = distancePointToAABB(e.position(), dragon.getBoundingBox());
            boolean inBodyRange = distToDragon <= effectiveRange;
            if (ridden) {
                // Relax angle further when ridden or allow body-range hits
                goodAngle = goodAngle || dot >= (cosLimit * 0.6) || inBodyRange;
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
        LightningDragonEntity dragon = getUser();
        DamageSource src = dragon.level().damageSources().mobAttack(dragon);
        float mult = dragon.getDamageMultiplier();
        primary.hurt(src, BITE_DAMAGE * mult);
        dragon.noteAggroFrom(primary);
    }

    private void chainFrom(LivingEntity start) {
        LightningDragonEntity dragon = getUser();
        Set<LivingEntity> hit = new HashSet<>();
        hit.add(start);

        LivingEntity current = start;
        float damage = CHAIN_DAMAGE_BASE;

        for (int i = 0; i < CHAIN_JUMPS; i++) {
            LivingEntity next = findNearestChainTarget(current, hit);
            if (next == null) break;

            // Damage and VFX
            float mult = dragon.getDamageMultiplier();
            next.hurt(dragon.level().damageSources().lightningBolt(), damage * mult);
            dragon.noteAggroFrom(next);
            spawnArc(current.position().add(0, current.getBbHeight() * 0.5, 0),
                     next.position().add(0, next.getBbHeight() * 0.5, 0));

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
        LightningDragonEntity dragon = getUser();
        Vec3 start = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(maxDistance));

        AABB sweep = new AABB(start, end).inflate(inflateRadius);
        var hit = ProjectileUtil.getEntityHitResult(dragon.level(), dragon, start, end, sweep,
                e -> e instanceof LivingEntity le && e != dragon && e.isAlive() && le.attackable() && !isAllied(dragon, e),
                (float)(maxDistance * maxDistance));
        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    // Final fallback: when ridden, grab nearest valid entity within body range ignoring angle
    private LivingEntity findNearestInBodyRangeIgnoringAngle() {
        LightningDragonEntity dragon = getUser();
        double range = BITE_RANGE_RIDDEN;
        AABB broad = dragon.getBoundingBox().inflate(range, range, range);
        List<LivingEntity> nearby = dragon.level().getEntitiesOfClass(LivingEntity.class, broad,
                e -> e != dragon && e.isAlive() && e.attackable() && !isAllied(dragon, e));

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearby) {
            double d = distancePointToAABB(e.position(), dragon.getBoundingBox());
            if (d <= range + 0.6) {
                if (!dragon.getSensing().hasLineOfSight(e)) continue;
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }

    private LivingEntity findNearestChainTarget(LivingEntity origin, Set<LivingEntity> exclude) {
        LightningDragonEntity dragon = getUser();
        List<LivingEntity> nearby = DragonMathUtil.getEntitiesNearby(origin, LivingEntity.class, CHAIN_RADIUS);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearby) {
            if (e == dragon || exclude.contains(e) || !e.isAlive() || !e.attackable() || isAllied(dragon, e)) continue;
            double d = e.distanceToSqr(origin);
            if (d < bestDist) {
                // Optional LOS check for coherence
                if (!dragon.getSensing().hasLineOfSight(e)) continue;
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isAllied(LightningDragonEntity dragon, Entity other) {
        if (other instanceof LightningDragonEntity od) {
            return dragon.isTame() && od.isTame() && dragon.getOwner() != null && dragon.getOwner().equals(od.getOwner());
        }
        if (other instanceof LivingEntity le) {
            if (dragon.isTame() && le.equals(dragon.getOwner())) return true;
            return dragon.isAlliedTo(le);
        }
        return false;
    }

    private void spawnArc(Vec3 from, Vec3 to) {
        if (!(getLevel() instanceof ServerLevel server)) return;
        // Spawn a simple electric spark trail along the segment
        Vec3 delta = to.subtract(from);
        int steps = Math.max(3, (int) (delta.length() * 6));
        Vec3 step = delta.scale(1.0 / steps);
        Vec3 pos = from;
        Vec3 dir = step.normalize();
        float size = 1.0f;
        for (int i = 0; i <= steps; i++) {
            // Use custom animated lightning sprite for richer arc visuals
            server.sendParticles(new LightningStormData(size),
                    pos.x, pos.y, pos.z,
                    1, dir.x, dir.y, dir.z, 0.0);
            pos = pos.add(step);
        }
    }
}
