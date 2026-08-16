package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class DraconianPellucidaBlock extends Block {
    public DraconianPellucidaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof AbstractDraconianSwarmEntity) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }
}
