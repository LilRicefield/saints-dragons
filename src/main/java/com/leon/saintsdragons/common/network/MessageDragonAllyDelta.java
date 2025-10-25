package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.client.screen.DragonAllyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Optimized network message for syncing individual ally changes (delta sync).
 * Sends only add/remove operations instead of the entire ally list.
 */
public class MessageDragonAllyDelta {
    private final int dragonId;
    private final String username;
    private final boolean isAdd;

    public MessageDragonAllyDelta(int dragonId, String username, boolean isAdd) {
        this.dragonId = dragonId;
        this.username = username;
        this.isAdd = isAdd;
    }

    public MessageDragonAllyDelta(FriendlyByteBuf buffer) {
        this.dragonId = buffer.readInt();
        this.username = buffer.readUtf(16);
        this.isAdd = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(dragonId);
        buffer.writeUtf(username, 16);
        buffer.writeBoolean(isAdd);
    }

    public static void handle(MessageDragonAllyDelta message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Only handle on client side
            if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen instanceof DragonAllyScreen allyScreen) {
                    // Apply delta update to GUI
                    if (message.isAdd) {
                        allyScreen.addAlly(message.username);
                    } else {
                        allyScreen.removeAlly(message.username);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
