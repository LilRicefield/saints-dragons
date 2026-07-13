package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.BloodTempestArmorSetBonus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class MessageBloodTempestDodge {
    private final BloodTempestDodgeDirection direction;

    public MessageBloodTempestDodge(BloodTempestDodgeDirection direction) {
        this.direction = direction;
    }

    public static void encode(MessageBloodTempestDodge message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.direction.ordinal());
    }

    public static MessageBloodTempestDodge decode(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        BloodTempestDodgeDirection[] values = BloodTempestDodgeDirection.values();
        BloodTempestDodgeDirection direction = id >= 0 && id < values.length ? values[id] : null;
        return new MessageBloodTempestDodge(direction);
    }

    public static void handle(MessageBloodTempestDodge message, ServerPlayer player) {
        BloodTempestArmorSetBonus.tryDodge(player, message.direction);
    }
}
