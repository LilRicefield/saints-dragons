package com.leon.saintsdragons.server.entity.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

final class GroundEffectSurfaceSnap {
    private static final double SURFACE_EPSILON = 0.002D;

    private GroundEffectSurfaceSnap() {
    }

    static void snap(Entity entity, double rendererPlaneOffset) {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(
                entity.getX(), entity.getY() + 0.5D, entity.getZ()).mutable();
        for (int i = 0; i < 7; i++) {
            BlockState state = entity.level().getBlockState(cursor);
            VoxelShape collision = state.getCollisionShape(entity.level(), cursor);
            if (!collision.isEmpty()) {
                double surfaceY = cursor.getY() + collision.max(Direction.Axis.Y);
                double entityY = surfaceY - rendererPlaneOffset + SURFACE_EPSILON;
                if (Math.abs(entity.getY() - entityY) > 1.0E-4D) {
                    entity.setPos(entity.getX(), entityY, entity.getZ());
                }
                return;
            }
            cursor.move(Direction.DOWN);
        }
    }
}
