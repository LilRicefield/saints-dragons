package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record MessageCameraImpulse(Vec3 origin, float radius, float intensity, int durationTicks) {
    public static void encode(MessageCameraImpulse message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.origin().x);
        buffer.writeDouble(message.origin().y);
        buffer.writeDouble(message.origin().z);
        buffer.writeFloat(message.radius());
        buffer.writeFloat(message.intensity());
        buffer.writeVarInt(message.durationTicks());
    }

    public static MessageCameraImpulse decode(FriendlyByteBuf buffer) {
        return new MessageCameraImpulse(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt()
        );
    }

    public static void handle(MessageCameraImpulse message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleCameraImpulse(message));
    }
}
