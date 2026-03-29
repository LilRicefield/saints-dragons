package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

public record MessageDragonAbilityDebugBox(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        int colorRgb,
        int lifetimeTicks
) {
    public MessageDragonAbilityDebugBox(AABB box, int colorRgb, int lifetimeTicks) {
        this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorRgb, lifetimeTicks);
    }

    public AABB box() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void encode(MessageDragonAbilityDebugBox message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.minX());
        buffer.writeDouble(message.minY());
        buffer.writeDouble(message.minZ());
        buffer.writeDouble(message.maxX());
        buffer.writeDouble(message.maxY());
        buffer.writeDouble(message.maxZ());
        buffer.writeInt(message.colorRgb());
        buffer.writeVarInt(message.lifetimeTicks());
    }

    public static MessageDragonAbilityDebugBox decode(FriendlyByteBuf buffer) {
        return new MessageDragonAbilityDebugBox(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(MessageDragonAbilityDebugBox message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleAbilityDebugBox(message));
    }
}
