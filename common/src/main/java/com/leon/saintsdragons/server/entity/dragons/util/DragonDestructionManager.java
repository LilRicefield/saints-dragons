package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared destructive helpers for dragon abilities (fire breath, etc.).
 */
public final class DragonDestructionManager {
    private DragonDestructionManager() {}

    public static void applyFireBreathImpact(ServerLevel level,
                                             DragonEntity dragon,
                                             Vec3 impactPoint,
                                             double radius,
                                             float damage,
                                             int fireSeconds) {
        if (level == null || dragon == null || impactPoint == null) {
            return;
        }
        damageEntities(level, dragon, impactPoint, radius, damage, fireSeconds);
        igniteBlocks(level, impactPoint, radius);
    }

    private static void damageEntities(ServerLevel level,
                                       DragonEntity dragon,
                                       Vec3 impactPoint,
                                       double radius,
                                       float damage,
                                       int fireSeconds) {
        AABB area = new AABB(impactPoint, impactPoint).inflate(radius);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != dragon && !dragon.isAlly(e))) {

            if (hit.add(entity)) {
                entity.hurt(level.damageSources().dragonBreath(), damage);
                entity.setSecondsOnFire(fireSeconds);
            }
        }
    }

    private static void igniteBlocks(ServerLevel level, Vec3 impactPoint, double radius) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        RandomSource random = level.random;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (random.nextFloat() > 0.35F) {
                continue;
            }
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > radius * radius) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }

            if (level.isEmptyBlock(pos)) {
                BlockState fire = Blocks.FIRE.defaultBlockState();
                if (fire.canSurvive(level, pos)) {
                    level.setBlock(pos, fire, 11);
                }
            } else if (random.nextFloat() < 0.15F) {
                BlockPos above = pos.above();
                if (level.isEmptyBlock(above) && Blocks.FIRE.defaultBlockState().canSurvive(level, above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }
}
