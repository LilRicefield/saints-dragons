package com.leon.saintsdragons.fabric.server;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.init.CommonServerLifecycleEvents;
import com.leon.saintsdragons.common.item.BloodTempestArmorSetBonus;
import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class FabricServerEvents {

    private FabricServerEvents() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> CommonServerLifecycleEvents.onPlayerJoin(handler.player)));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> CommonServerLifecycleEvents.onPlayerDisconnect(handler.player)));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, sourceLevel, destinationLevel) ->
                        IvyTheDragonMerchant.followOwnerAcrossDimension(player, sourceLevel));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            var sourceLevel = oldPlayer.serverLevel();
            if (sourceLevel.dimension() != newPlayer.serverLevel().dimension()) {
                newPlayer.server.execute(() ->
                        IvyTheDragonMerchant.followOwnerAcrossDimension(newPlayer, sourceLevel));
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(CommonServerLifecycleEvents::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(CommonServerLifecycleEvents::onEndServerTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player)
                        || (!BloodTempestArmorSetBonus.blocksDamage(player, source)
                        && !DragonlordArmorSetBonus.blocksDamage(player, source)));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof DragonEntity dragon) || !dragon.isBaby()) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragonsCommon.rl("why"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "hit_baby");
            }
            return InteractionResult.PASS;
        });
    }
}
