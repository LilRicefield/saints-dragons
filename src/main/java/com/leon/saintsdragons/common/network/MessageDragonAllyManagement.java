package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network message for managing dragon allies.
 * Handles adding/removing allies and syncing ally lists.
 */
public class MessageDragonAllyManagement {
    private final int dragonId;
    private final Action action;
    private final String username;
    
    public MessageDragonAllyManagement(int dragonId, Action action, String username) {
        this.dragonId = dragonId;
        this.action = action;
        this.username = username;
    }
    
    public MessageDragonAllyManagement(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        this.action = buffer.readEnum(Action.class);
        this.username = buffer.readUtf(16);
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dragonId);
        buffer.writeEnum(action);
        buffer.writeUtf(username, 16);
    }
    
    public static void handle(MessageDragonAllyManagement message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Find the dragon entity
            Entity entity = player.level().getEntity(message.dragonId);
            if (!(entity instanceof DragonEntity dragon)) {
                player.sendSystemMessage(Component.translatable("saintsdragons.message.dragon_not_found"));
                return;
            }
            
            // Check if player owns the dragon
            if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
                player.sendSystemMessage(Component.translatable("saintsdragons.message.not_dragon_owner"));
                return;
            }
            
            DragonAllyManager allyManager = dragon.allyManager;
            DragonAllyManager.AllyResult result;
            
            switch (message.action) {
                case ADD:
                    result = allyManager.addAlly(message.username);
                    break;
                case REMOVE:
                    result = allyManager.removeAlly(message.username);
                    break;
                default:
                    result = DragonAllyManager.AllyResult.INVALID_USERNAME;
                    break;
            }
            
            // Send result message to player
            Component resultMessage = Component.translatable("saintsdragons.message.ally." + result.name().toLowerCase(), message.username);
            player.sendSystemMessage(resultMessage);
            
            // Send updated ally list to client
            ModNetworkHandler.sendToPlayer(player, new MessageDragonAllyList(dragon.getId(), allyManager.getAllyUsernames()));
        });
        context.setPacketHandled(true);
    }
    
    public enum Action {
        ADD,
        REMOVE
    }
}
