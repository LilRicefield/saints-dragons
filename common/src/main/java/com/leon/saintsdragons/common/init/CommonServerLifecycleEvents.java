package com.leon.saintsdragons.common.init;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.data.WikiReminderSavedData;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBuffAbility;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.world.RaevyxStormSpawner;
import com.leon.saintsdragons.server.world.StegonautLushCaveSpawner;
import com.leon.saintsdragons.server.world.VolitansUnderwaterSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class CommonServerLifecycleEvents {
    private static final String WIKI_URL = "https://raevyx.miraheze.org/";

    private CommonServerLifecycleEvents() {
    }

    public static void onEndServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                StegonautBuffAbility.updateAllPortableBuffs(level);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            RaevyxStormSpawner.tick(level);
            StegonautLushCaveSpawner.tick(level);
            VolitansUnderwaterSpawner.tick(level);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onPlayerDisconnect(player);
        }

        RaevyxStormSpawner.clearTracking();
        StegonautLushCaveSpawner.clearTracking();
        VolitansUnderwaterSpawner.clearTracking();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        sendWikiReminder(player);

        RideableDragonBase dragon = findMountedDragon(player);
        if (dragon == null) {
            return;
        }

        SaintsDragonsCommon.LOGGER.info("Restoring mounted dragon {} for player {}", dragon, player.getGameProfile().getName());
        dragon.restoreMountedAnimationStateAfterLogin();
    }

    private static void sendWikiReminder(ServerPlayer player) {
        if (player == null || !WikiReminderSavedData.get(player.serverLevel()).markShownIfFirst(player.getUUID())) {
            return;
        }

        Component url = Component.literal(WIKI_URL).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WIKI_URL)));
        player.displayClientMessage(Component.translatable("saintsdragons.message.wiki_prompt").append(" ").append(url), false);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        RideableDragonBase dragon = findMountedDragon(player);
        if (dragon == null) {
            return;
        }

        SaintsDragonsCommon.LOGGER.info("Preserving mounted dragon {} for player {} on disconnect", dragon, player.getGameProfile().getName());
        dragon.setPersistenceRequired();
        dragon.getNavigation().stop();
        dragon.setAccelerating(false);

        if (dragon.isOrderedToSit()) {
            dragon.forceSitProgress(dragon.maxSitTicks());
        }
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