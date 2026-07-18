package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class DragonIdleLookBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private final double playerRange;
    @Nullable
    private Player player;
    private float randomYaw;
    private int lookTicks;

    public DragonIdleLookBehaviour(double playerRange) {
        super(false);
        this.playerRange = playerRange;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (dragon.getTarget() != null || dragon.isVehicle() || dragon.isOrderedToSit()) {
            return;
        }
        if (lookTicks-- <= 0) {
            player = context.level().getNearestPlayer(dragon, playerRange);
            lookTicks = 40 + dragon.getRandom().nextInt(40);
            randomYaw = dragon.getYRot() + dragon.getRandom().nextFloat() * 180.0F - 90.0F;
        }
        if (player != null && player.isAlive() && dragon.distanceToSqr(player) <= playerRange * playerRange) {
            dragon.getLookControl().setLookAt(player, 10.0F, dragon.getMaxHeadXRot());
        } else {
            double radians = Math.toRadians(randomYaw);
            dragon.getLookControl().setLookAt(dragon.getX() - Math.sin(radians) * 4.0D,
                    dragon.getEyeY(), dragon.getZ() + Math.cos(radians) * 4.0D);
        }
    }
}
