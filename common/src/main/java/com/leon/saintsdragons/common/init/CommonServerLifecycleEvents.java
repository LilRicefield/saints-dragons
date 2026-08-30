package com.leon.saintsdragons.common.init;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.item.BloodTempestArmorSetBonus;
import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.data.WikiReminderSavedData;
import com.leon.saintsdragons.server.debug.DragonPathDebugTracker;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBuffAbility;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
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
    private static final String WIKI_URL = "https://raevyx.miraheze.org/wiki/Main_Page";

    private CommonServerLifecycleEvents() {
    }

    public static void onEndServerTick(MinecraftServer server) {
        DragonPathDebugTracker.tick(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BloodTempestArmorSetBonus.tick(player);
            DragonlordArmorSetBonus.tick(player);
        }

        if (server.getTickCount() % 20 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                StegonautBuffAbility.updateAllPortableBuffs(level);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (SaintsDragonsConfig.isRaevyxSpawningEnabled()
                    && SaintsDragonsConfig.isRaevyxCustomSpawningEnabled()) {
                RaevyxStormSpawner.tick(level);
            }
            if (SaintsDragonsConfig.isStegonautSpawningEnabled()
                    && SaintsDragonsConfig.isStegonautCustomSpawningEnabled()) {
                StegonautLushCaveSpawner.tick(level);
            }
            if (SaintsDragonsConfig.isVolitansSpawningEnabled()
                    && SaintsDragonsConfig.isVolitansCustomSpawningEnabled()) {
                VolitansUnderwaterSpawner.tick(level);
            }
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        AsyncDragonPathfinder.onServerStopping(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DragonlordArmorSetBonus.saveHealthForReload(player);
            onPlayerDisconnect(player);
        }

        RaevyxStormSpawner.clearTracking();
        StegonautLushCaveSpawner.clearTracking();
        VolitansUnderwaterSpawner.clearTracking();
        DragonPathDebugTracker.clearAll();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        sendWikiReminder(player);
        DragonlordArmorSetBonus.queueHealthRestore(player);

        RideableDragonBase dragon = findMountedDragon(player);
        if (dragon == null) {
            return;
        }

        SaintsDragonsCommon.LOGGER.info("Restoring mounted dragon {} for player {}", dragon, player.getGameProfile().getName());
        dragon.restoreMountedAnimationStateAfterLogin();
    }

    private static void sendWikiReminder(ServerPlayer player) {
        if (!SaintsDragonsConfig.isWikiReminderEnabled()) {
            return;
        }

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
        DialogueSessionRegistry.suspend(player);
        DragonPathDebugTracker.clear(player);
        BloodTempestArmorSetBonus.clear(player);
        DragonlordArmorSetBonus.saveHealthForReload(player);
        DragonlordArmorSetBonus.clear(player);

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
