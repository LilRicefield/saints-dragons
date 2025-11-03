package com.leon.saintsdragons.fabric.server;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Fabric server-side event hooks that keep rideable dragons stable across player disconnects
 * without forcing an unsafe dismount mid-flight.
 */
public final class FabricServerEvents {

    private FabricServerEvents() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> handlePlayerJoin(handler.player)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> handlePlayerDisconnect(handler.player)));

        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::handleServerStopping);
    }

    private static void handleServerStopping(MinecraftServer server) {
        // Ensure pending disconnect callbacks have already run; we only need to mark any remaining mounts.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            handlePlayerDisconnect(player);
        }
    }

    private static void handlePlayerJoin(ServerPlayer player) {
        RideableDragonBase dragon = findMountedDragon(player);
        if (dragon == null) {
            return;
        }

        SaintsDragonsCommon.LOGGER.info("Restoring mounted dragon {} for player {}", dragon, player.getGameProfile().getName());

        // Re-sync critical animation state so the dragon is responsive immediately after login.
        dragon.setPersistenceRequired();
        dragon.initializeAnimationState();
        dragon.resetAnimationState();
        dragon.tickAnimationStates();
        dragon.syncAnimState(dragon.getGroundMoveState(), dragon.getSyncedFlightMode());
        dragon.setAccelerating(false);
        dragon.setLastRiderForward(0f);
        dragon.setLastRiderStrafe(0f);
    }

    private static void handlePlayerDisconnect(ServerPlayer player) {
        RideableDragonBase dragon = findMountedDragon(player);
        if (dragon == null) {
            return;
        }

        SaintsDragonsCommon.LOGGER.info("Preserving mounted dragon {} for player {} on disconnect", dragon, player.getGameProfile().getName());

        // Keep the dragon marked as persistent so it survives chunk unloads while the owner is offline.
        dragon.setPersistenceRequired();
        dragon.getNavigation().stop();
        dragon.setAccelerating(false);
    }

    private static RideableDragonBase findMountedDragon(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof RideableDragonBase rideable) {
            return rideable.isRemoved() ? null : rideable;
        }

        Entity root = player.getRootVehicle();
        if (root instanceof RideableDragonBase rideable) {
            return rideable.isRemoved() ? null : rideable;
        }

        return null;
    }
}
