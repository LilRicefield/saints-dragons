package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.effect.LightningVisualEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class RaevyxChainLightningAbility {
    private static final float BITE_CHAIN_DAMAGE = 10.0F;
    private static final double BITE_CHAIN_RADIUS = 8.0D;
    private static final int BITE_CHAIN_JUMPS = 5;
    private static final float BITE_CHAIN_FALLOFF = 0.75F;
    private static final int CHAIN_VISUAL_LIFETIME = 5;

    private static final float IMPACT_CHAIN_DAMAGE_MIN = 7.0F;
    private static final float IMPACT_CHAIN_DAMAGE_MAX = 15.0F;
    private static final double IMPACT_CHAIN_RADIUS = 12.0D;
    private static final int IMPACT_CHAIN_JUMPS = 4;
    private static final float IMPACT_CHAIN_FALLOFF = 0.8F;

    private RaevyxChainLightningAbility() {
    }

    public static void chainFromBite(Raevyx wyvern, LivingEntity start) {
        chainFromTarget(wyvern, start, BITE_CHAIN_DAMAGE, BITE_CHAIN_RADIUS, BITE_CHAIN_JUMPS,
                BITE_CHAIN_FALLOFF, true);
    }

    public static void chainFromKatana(ServerPlayer player, LivingEntity start) {
        chainFromTarget(player, start, BITE_CHAIN_DAMAGE, BITE_CHAIN_RADIUS, BITE_CHAIN_JUMPS,
                BITE_CHAIN_FALLOFF, true);
    }

    public static void chainFromImpact(Raevyx wyvern, Vec3 origin, Collection<LivingEntity> impactTargets, double power) {
        LivingEntity start = nearestTarget(origin, impactTargets);
        if (start == null) {
            return;
        }

        float damage = (float) (IMPACT_CHAIN_DAMAGE_MIN + (IMPACT_CHAIN_DAMAGE_MAX - IMPACT_CHAIN_DAMAGE_MIN) * power);
        chainFromTarget(wyvern, start, damage, IMPACT_CHAIN_RADIUS, IMPACT_CHAIN_JUMPS,
                IMPACT_CHAIN_FALLOFF, true);
    }

    private static void chainFromTarget(LivingEntity caster, LivingEntity start, float baseDamage, double radius,
                                        int maxJumps, float falloff, boolean dischargeIfBlocked) {
        if (!(caster.level() instanceof ServerLevel)) {
            return;
        }

        Set<LivingEntity> hit = new HashSet<>();
        hit.add(start);

        LivingEntity current = start;
        float damage = baseDamage;
        boolean jumped = false;

        for (int i = 0; i < maxJumps; i++) {
            LivingEntity next = findNearestChainTarget(caster, centerOf(current), hit, radius);
            if (next == null) {
                if (!jumped && dischargeIfBlocked) {
                    dischargeInto(caster, current, damage);
                }
                return;
            }

            hurtWithLightning(caster, next, damage);
            noteDragonAggro(caster, next);
            spawnArc(caster, centerOf(current), centerOf(next));

            hit.add(next);
            current = next;
            damage *= falloff;
            jumped = true;
        }
    }

    private static LivingEntity nearestTarget(Vec3 origin, Collection<LivingEntity> targets) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity target : targets) {
            double distance = target.position().distanceToSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = target;
            }
        }
        return best;
    }

    private static LivingEntity findNearestChainTarget(LivingEntity caster, Vec3 origin, Set<LivingEntity> exclude,
                                                       double radius) {
        AABB area = new AABB(origin, origin).inflate(radius, radius, radius);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity target : caster.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> distanceToSqr(entity.getBoundingBox(), origin) <= radius * radius)) {
            if (!isValidChainTarget(caster, target, exclude)) {
                continue;
            }
            double distance = distanceToSqr(target.getBoundingBox(), origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = target;
            }
        }
        return best;
    }

    private static boolean isValidChainTarget(LivingEntity caster, LivingEntity target, Set<LivingEntity> exclude) {
        return target != null
                && target != caster
                && !exclude.contains(target)
                && target.isAlive()
                && target.attackable()
                && !isAllied(caster, target)
                && !isProtectedTamedPet(target)
                && !DragonElementalImmunity.isElectricityImmune(target);
    }

    private static void dischargeInto(LivingEntity caster, LivingEntity target, float damage) {
        Vec3 center = centerOf(target);
        if (isValidDischargeTarget(caster, target)) {
            hurtWithLightning(caster, target, damage);
            noteDragonAggro(caster, target);
        }
        spawnArc(caster, center.add(0.0D, 0.35D, 0.0D), center);
        if (!(caster instanceof ServerPlayer)) {
            spawnContainedBurst(caster, center);
        }
    }

    private static boolean isValidDischargeTarget(LivingEntity caster, LivingEntity target) {
        return target != null
                && target != caster
                && target.isAlive()
                && target.attackable()
                && !isAllied(caster, target)
                && !isProtectedTamedPet(target)
                && !DragonElementalImmunity.isElectricityImmune(target);
    }

    private static void hurtWithLightning(LivingEntity caster, LivingEntity target, float damage) {
        float scaledDamage = caster instanceof Raevyx wyvern
                ? damage * wyvern.getDamageMultiplier()
                : damage;
        DamageSource source = caster instanceof ServerPlayer player
                ? caster.level().damageSources().playerAttack(player)
                : target instanceof DragonEntity
                        ? caster.level().damageSources().mobAttack(caster)
                        : caster.level().damageSources().lightningBolt();
        target.hurt(source, scaledDamage);
    }

    private static boolean isAllied(LivingEntity caster, LivingEntity target) {
        return caster instanceof DragonEntity dragon
                ? dragon.isAlly(target)
                : caster.isAlliedTo(target);
    }

    private static void noteDragonAggro(LivingEntity caster, LivingEntity target) {
        if (caster instanceof Raevyx wyvern) {
            wyvern.noteAggroFrom(target);
        }
    }

    private static boolean isProtectedTamedPet(LivingEntity entity) {
        if (entity instanceof DragonEntity) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return true;
        }
        return entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
    }

    private static Vec3 centerOf(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static double distanceToSqr(AABB box, Vec3 point) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void spawnArc(LivingEntity caster, Vec3 from, Vec3 to) {
        if (!(caster.level() instanceof ServerLevel server)) {
            return;
        }

        LightningVisualEntity.VisualStyle style = caster instanceof ServerPlayer
                ? LightningVisualEntity.VisualStyle.BLOOD_TEMPEST
                : LightningVisualEntity.VisualStyle.RAEVYX;
        server.addFreshEntity(new LightningVisualEntity(server, from, to,
                1.0F, CHAIN_VISUAL_LIFETIME, server.random.nextLong(), style));

        Vec3 midpoint = from.lerp(to, 0.5D);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                midpoint.x, midpoint.y, midpoint.z,
                2, 0.06D, 0.06D, 0.06D, 0.0D);
    }

    private static void spawnContainedBurst(LivingEntity caster, Vec3 center) {
        if (!(caster.level() instanceof ServerLevel server)) {
            return;
        }

        server.sendParticles(new RaevyxLightningStormData(1.25F),
                center.x, center.y, center.z,
                2, 0.08D, 0.08D, 0.08D, 0.0D);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                10, 0.25D, 0.25D, 0.25D, 0.02D);
    }
}
