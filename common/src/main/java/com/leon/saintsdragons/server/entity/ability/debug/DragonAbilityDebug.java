package com.leon.saintsdragons.server.entity.ability.debug;

import com.leon.saintsdragons.common.network.MessageDragonAbilityDebugBox;
import com.leon.saintsdragons.common.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class DragonAbilityDebug {
    private static final double DEFAULT_SEND_RADIUS = 96.0D;

    private DragonAbilityDebug() {
    }

    public static void sendBox(Entity source, AABB box, int colorRgb, int lifetimeTicks) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MessageDragonAbilityDebugBox message = new MessageDragonAbilityDebugBox(box, colorRgb, lifetimeTicks);
        double maxDistSqr = DEFAULT_SEND_RADIUS * DEFAULT_SEND_RADIUS;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(source) <= maxDistSqr) {
                NetworkHandler.sendToPlayer(player, message);
            }
        }
    }
}
