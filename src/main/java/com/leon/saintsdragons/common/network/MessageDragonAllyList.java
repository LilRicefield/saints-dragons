package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.client.screen.DragonAllyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Network message for syncing wyvern ally lists to clients.
 * Updates the GUI with the current ally list.
 */
public class MessageDragonAllyList {
    private final int dragonId;
    private final List<String> allyList;
    
    public MessageDragonAllyList(int dragonId, List<String> allyList) {
        this.dragonId = dragonId;
        this.allyList = new ArrayList<>(allyList);
    }
    
    public MessageDragonAllyList(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        int size = buffer.readInt();
        this.allyList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allyList.add(buffer.readUtf(16));
        }
    }
    
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dragonId);
        buffer.writeInt(allyList.size());
        for (String ally : allyList) {
            buffer.writeUtf(ally, 16);
        }
    }
    
    public static void handle(MessageDragonAllyList message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Only handle on client side
            if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen instanceof DragonAllyScreen allyScreen) {
                    // Update the ally list in the GUI
                    allyScreen.updateAllyList(message.allyList);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
