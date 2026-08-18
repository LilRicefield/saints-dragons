package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class DragonOwnerTeleport {
    private DragonOwnerTeleport() {
    }

    public static boolean attempt(RideableDragonBase dragon, LivingEntity owner) {
        if (owner == null || dragon.level() != owner.level()) {
            return false;
        }
        BlockPos ownerPos = BlockPos.containing(DragonOwnerFollowTarget.groundTarget(dragon, owner));
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos candidate = ownerPos.offset(
                    dragon.getRandom().nextInt(7) - 3, 0,
                    dragon.getRandom().nextInt(7) - 3);
            if (isFriendly(dragon.level(), candidate)) {
                dragon.teleportTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                dragon.getAIMovement().stop();
                return true;
            }
        }
        return false;
    }

    private static boolean isFriendly(Level level, BlockPos position) {
        BlockPos below = position.below();
        BlockState floor = level.getBlockState(below);
        return floor.isSolidRender(level, below)
                && level.getBlockState(position).isAir()
                && level.getBlockState(position.above()).isAir();
    }
}
