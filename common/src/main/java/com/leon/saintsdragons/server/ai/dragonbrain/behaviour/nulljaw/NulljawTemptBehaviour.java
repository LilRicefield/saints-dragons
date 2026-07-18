package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public final class NulljawTemptBehaviour extends DragonBehaviour<Nulljaw> {
    private static final double START_RANGE = 10.0D;
    private static final double CONTINUE_RANGE = 12.0D;
    private static final double STOP_DISTANCE_SQR = 3.5D * 3.5D;
    private static final double SPEED = 1.0D;

    @Nullable
    private Player player;
    private int refreshCooldown;

    @Override
    protected boolean canStart(DragonBrainContext<Nulljaw> context) {
        if (!canBeTempted(context.dragon())) {
            return false;
        }
        player = findTemptingPlayer(context.dragon(), START_RANGE);
        return player != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Nulljaw> context) {
        return canBeTempted(context.dragon())
                && isTempting(context.dragon(), player)
                && context.dragon().distanceToSqr(player) <= CONTINUE_RANGE * CONTINUE_RANGE;
    }

    @Override
    protected void start(DragonBrainContext<Nulljaw> context) {
        refreshCooldown = 0;
        context.dragon().beginAiFlight();
    }

    @Override
    protected void tick(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        if (player == null) {
            return;
        }
        dragon.getLookControl().setLookAt(player, 10.0F, dragon.getMaxHeadXRot());
        if (dragon.distanceToSqr(player) <= STOP_DISTANCE_SQR) {
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            dragon.getAIMovement().stop();
            return;
        }
        if (refreshCooldown-- <= 0) {
            Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(target, SPEED)
            );
            refreshCooldown = 4;
        }
    }

    @Override
    protected void stop(DragonBrainContext<Nulljaw> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.dragon().getAIMovement().stop();
        player = null;
        refreshCooldown = 0;
    }

    public static boolean hasTemptingPlayer(Nulljaw dragon) {
        return canBeTempted(dragon) && findTemptingPlayer(dragon, START_RANGE) != null;
    }

    @Nullable
    private static Player findTemptingPlayer(Nulljaw dragon, double range) {
        return dragon.level().getEntitiesOfClass(
                        Player.class,
                        dragon.getBoundingBox().inflate(range),
                        player -> isTempting(dragon, player)
                ).stream()
                .min(Comparator.comparingDouble(dragon::distanceToSqr))
                .orElse(null);
    }

    private static boolean isTempting(Nulljaw dragon, @Nullable Player player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && (dragon.isFood(player.getMainHandItem()) || dragon.isFood(player.getOffhandItem()));
    }

    private static boolean canBeTempted(Nulljaw dragon) {
        return dragon.isAlive()
                && !dragon.isOrderedToSit()
                && !dragon.isInLove()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive());
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("player", player == null ? "none" : player.getGameProfile().getName());
    }
}
