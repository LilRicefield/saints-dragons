package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Centralises packet registration and dispatch using the platform abstraction layer.
 */
public final class NetworkHandler {
    private static final NetworkHelper NETWORK = Services.PLATFORM.getNetworkHelper();
    private static boolean registered = false;

    private NetworkHandler() {
    }

    private static ResourceLocation id(String path) {
        return SaintsDragonsCommon.rl(path);
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        NETWORK.registerServerbound(
                MessageDragonRideInput.class,
                id("dragon_ride_input"),
                MessageDragonRideInput::encode,
                MessageDragonRideInput::decode,
                MessageDragonRideInput::handle
        );

        NETWORK.registerServerbound(
                MessageDragonAllyManagement.class,
                id("dragon_ally_management"),
                MessageDragonAllyManagement::encode,
                MessageDragonAllyManagement::decode,
                MessageDragonAllyManagement::handle
        );

        NETWORK.registerServerbound(
                MessageDragonAllyRequest.class,
                id("dragon_ally_request"),
                MessageDragonAllyRequest::encode,
                MessageDragonAllyRequest::decode,
                MessageDragonAllyRequest::handle
        );

        NETWORK.registerClientbound(
                MessageDragonAllyList.class,
                id("dragon_ally_list"),
                MessageDragonAllyList::encode,
                MessageDragonAllyList::decode,
                MessageDragonAllyList::handle
        );

        NETWORK.registerClientbound(
                MessageDragonAllyDelta.class,
                id("dragon_ally_delta"),
                MessageDragonAllyDelta::encode,
                MessageDragonAllyDelta::decode,
                MessageDragonAllyDelta::handle
        );
    }

    public static void sendToServer(Object message) {
        NETWORK.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        NETWORK.sendToPlayer(player, message);
    }

    public static void sendToTracking(Entity entity, Object message) {
        NETWORK.sendToTracking(entity, message);
    }

    public static void sendToDimension(Level level, Object message) {
        NETWORK.sendToDimension(level, message);
    }
}
