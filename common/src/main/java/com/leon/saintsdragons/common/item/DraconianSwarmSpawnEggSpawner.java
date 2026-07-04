package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public final class DraconianSwarmSpawnEggSpawner {
    private DraconianSwarmSpawnEggSpawner() {
    }

    public static InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        BlockPos origin = clickedState.getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());

        boolean spawned = spawn(ModEntities.LATCHER.get(), level, origin.offset(-2, 0, 0))
                | spawn(ModEntities.WINGED.get(), level, origin)
                | spawn(ModEntities.WHETTLED.get(), level, origin.offset(2, 0, 0));
        if (!spawned) {
            return InteractionResult.FAIL;
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean spawn(EntityType<? extends Mob> type, ServerLevel level, BlockPos pos) {
        Entity entity = type.spawn(level, pos, MobSpawnType.SPAWN_EGG);
        return entity != null;
    }
}
