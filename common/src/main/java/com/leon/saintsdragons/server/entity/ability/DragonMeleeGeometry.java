package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class DragonMeleeGeometry {
    private DragonMeleeGeometry() {
    }

    public record ForwardAttack(Vec3 origin, Vec3 forward) {
        public ForwardAttack offset(double forwardOffset) {
            return new ForwardAttack(origin.add(forward.scale(forwardOffset)), forward);
        }

        public AABB sweep(double range, double horizontalInflate, double verticalInflate) {
            return new AABB(origin, origin.add(forward.scale(range)))
                    .inflate(horizontalInflate, verticalInflate, horizontalInflate);
        }
    }

    public static ForwardAttack forwardAttack(DragonEntity dragon) {
        Entity rider = dragon.getControllingPassenger();
        Vec3 forward = rider instanceof LivingEntity living ? living.getLookAngle() : dragon.getLookAngle();
        return forwardAttack(dragon, forward);
    }

    public static ForwardAttack bodyForwardAttack(DragonEntity dragon) {
        return forwardAttack(dragon, Vec3.directionFromRotation(0.0F, dragon.yBodyRot));
    }

    private static ForwardAttack forwardAttack(DragonEntity dragon, Vec3 forward) {
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = dragon.getLookAngle();
        }
        forward = forward.normalize();

        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        } else {
            horizontal = horizontal.normalize();
        }

        Vec3 origin = dragon.position()
                .add(0.0D, dragon.getBbHeight() * 0.52D, 0.0D)
                .add(horizontal.scale(dragon.getBbWidth() * 0.35D));
        return new ForwardAttack(origin, forward);
    }

    public static List<LivingEntity> findForwardTargets(DragonEntity dragon,
                                                        double range,
                                                        double angleDeg,
                                                        Predicate<LivingEntity> filter) {
        return findForwardTargets(
                dragon,
                range,
                range,
                range,
                angleDeg,
                range * 0.35D,
                filter
        );
    }

    public static List<LivingEntity> findForwardTargets(DragonEntity dragon,
                                                        double range,
                                                        double horizontalInflate,
                                                        double verticalInflate,
                                                        double angleDeg,
                                                        double closeHitRange,
                                                        Predicate<LivingEntity> filter) {
        return findForwardTargets(
                forwardAttack(dragon),
                dragon,
                range,
                horizontalInflate,
                verticalInflate,
                angleDeg,
                closeHitRange,
                filter
        );
    }

    public static List<LivingEntity> findForwardTargets(DragonEntity dragon,
                                                        double range,
                                                        double horizontalInflate,
                                                        double verticalInflate,
                                                        double angleDeg,
                                                        double closeHitRange,
                                                        double forwardOffset,
                                                        Predicate<LivingEntity> filter) {
        return findForwardTargets(
                forwardAttack(dragon).offset(forwardOffset),
                dragon,
                range,
                horizontalInflate,
                verticalInflate,
                angleDeg,
                closeHitRange,
                filter
        );
    }

    public static List<LivingEntity> findSweepTargets(DragonEntity dragon,
                                                      double range,
                                                      double horizontalInflate,
                                                      double verticalInflate,
                                                      double forwardOffset,
                                                      Predicate<LivingEntity> filter) {
        return findSweepTargets(
                forwardAttack(dragon).offset(forwardOffset),
                dragon,
                range,
                horizontalInflate,
                verticalInflate,
                filter
        );
    }

    public static List<LivingEntity> findBodySweepTargets(DragonEntity dragon,
                                                          double range,
                                                          double horizontalInflate,
                                                          double verticalInflate,
                                                          double forwardOffset,
                                                          Predicate<LivingEntity> filter) {
        return findSweepTargets(
                bodyForwardAttack(dragon).offset(forwardOffset),
                dragon,
                range,
                horizontalInflate,
                verticalInflate,
                filter
        );
    }

    private static List<LivingEntity> findSweepTargets(ForwardAttack attack,
                                                       DragonEntity dragon,
                                                       double range,
                                                       double horizontalInflate,
                                                       double verticalInflate,
                                                       Predicate<LivingEntity> filter) {
        return dragon.level().getEntitiesOfClass(LivingEntity.class,
                        attack.sweep(range, horizontalInflate, verticalInflate),
                        entity -> entity != dragon && entity.isAlive() && entity.attackable() && filter.test(entity))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> distancePointToAABB(attack.origin(), entity.getBoundingBox())))
                .toList();
    }

    private static List<LivingEntity> findForwardTargets(ForwardAttack attack,
                                                        DragonEntity dragon,
                                                        double range,
                                                        double horizontalInflate,
                                                        double verticalInflate,
                                                        double angleDeg,
                                                        double closeHitRange,
                                                        Predicate<LivingEntity> filter) {
        double cosLimit = Math.cos(Math.toRadians(angleDeg));
        return dragon.level().getEntitiesOfClass(LivingEntity.class,
                        attack.sweep(range, horizontalInflate, verticalInflate),
                        entity -> entity != dragon && entity.isAlive() && entity.attackable() && filter.test(entity))
                .stream()
                .filter(entity -> isForwardTarget(attack, entity, range, horizontalInflate, cosLimit, closeHitRange))
                .sorted(Comparator.comparingDouble(entity -> distancePointToAABB(attack.origin(), entity.getBoundingBox())))
                .toList();
    }

    public static boolean isDirectAiTargetValid(DragonEntity dragon, LivingEntity target, double extraReach) {
        if (target == null
                || !target.isAlive()
                || !target.attackable()
                || dragon.isAlly(target)
                || !dragon.isTargetValid(target)) {
            return false;
        }
        AABB dragonBox = dragon.getBoundingBox();
        AABB targetBox = target.getBoundingBox();
        double horizontalGap = horizontalAabbGap(dragonBox, targetBox);
        double verticalGap = verticalAabbGap(dragonBox, targetBox);
        double horizontalReach = Math.max(0.0D, extraReach) + Math.max(1.0D, dragon.getBbWidth() * 0.5D);
        double verticalReach = Math.max(2.0D, dragon.getBbHeight() * 0.75D + target.getBbHeight() * 0.5D);
        return horizontalGap <= horizontalReach && verticalGap <= verticalReach;
    }

    public static boolean isForwardTarget(ForwardAttack attack,
                                          LivingEntity target,
                                          double range,
                                          double horizontalInflate,
                                          double cosLimit,
                                          double closeHitRange) {
        double dist = distancePointToAABB(attack.origin(), target.getBoundingBox());
        if (dist > range + horizontalInflate) {
            return false;
        }
        if (dist <= closeHitRange) {
            return true;
        }

        Vec3 toward = closestPointOnAABB(attack.origin(), target.getBoundingBox()).subtract(attack.origin());
        double len = toward.length();
        if (len <= 1.0E-4D) {
            return true;
        }

        double dot = toward.scale(1.0D / len).dot(attack.forward());
        return dot > 0.0D && (dot >= cosLimit || dist < range * 0.35D);
    }

    public static double distancePointToAABB(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Vec3 closestPointOnAABB(Vec3 point, AABB box) {
        return new Vec3(
                clamp(point.x, box.minX, box.maxX),
                clamp(point.y, box.minY, box.maxY),
                clamp(point.z, box.minZ, box.maxZ)
        );
    }

    private static double horizontalAabbGap(AABB first, AABB second) {
        double dx = Math.max(Math.max(first.minX - second.maxX, second.minX - first.maxX), 0.0D);
        double dz = Math.max(Math.max(first.minZ - second.maxZ, second.minZ - first.maxZ), 0.0D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double verticalAabbGap(AABB first, AABB second) {
        return Math.max(Math.max(first.minY - second.maxY, second.minY - first.maxY), 0.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
