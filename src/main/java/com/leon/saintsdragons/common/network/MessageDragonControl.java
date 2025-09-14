package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MessageDragonControl(int dragonId, byte controlState) {

    public static void encode(MessageDragonControl message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId());
        buf.writeByte(message.controlState());
    }

    public static MessageDragonControl decode(FriendlyByteBuf buf) {
        return new MessageDragonControl(
                buf.readInt(),
                buf.readByte()
        );
    }

    public static void handle(MessageDragonControl message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player != null) {
                Entity entity = player.level().getEntity(message.dragonId());
                if (entity instanceof LightningDragonEntity dragon) {
                    if (dragon.isOwnedBy(player)) {
                        dragon.setControlState(message.controlState());
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
