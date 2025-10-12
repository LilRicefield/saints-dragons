package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.network.ModNetworkHandler;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network message for requesting wyvern ally list from server.
 * Sent by client when opening the ally management GUI.
 */
public class MessageDragonAllyRequest {
    private final int dragonId;
    
    public MessageDragonAllyRequest(int dragonId) {
        this.dragonId = dragonId;
    }
    
    public MessageDragonAllyRequest(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dragonId);
    }
    
    public static void handle(MessageDragonAllyRequest message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Find the wyvern entity
            Entity entity = player.level().getEntity(message.dragonId);
            if (!(entity instanceof DragonEntity dragon)) {
                return; // Dragon not found, ignore
            }
            
            // Check if player owns the wyvern
            if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
                return; // Player doesn't own wyvern, ignore
            }
            
            // Send current ally list to client
            ModNetworkHandler.sendToPlayer(player, new MessageDragonAllyList(dragon.getId(), dragon.allyManager.getAllyUsernames()));
        });
        context.setPacketHandled(true);
    }
}
