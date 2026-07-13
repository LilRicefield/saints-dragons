package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record MessageBloodTempestAfterimage(int entityId, BloodTempestAfterimageProfile profile,
                                            Vec3 origin, Vec3 destination) {
    public MessageBloodTempestAfterimage(int entityId, BloodTempestAfterimageProfile profile) {
        this(entityId, profile, null, null);
    }

    public static void encode(MessageBloodTempestAfterimage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
        buffer.writeEnum(message.profile());
        boolean hasFixedPath = message.origin() != null && message.destination() != null;
        buffer.writeBoolean(hasFixedPath);
        if (hasFixedPath) {
            writeVec3(buffer, message.origin());
            writeVec3(buffer, message.destination());
        }
    }

    public static MessageBloodTempestAfterimage decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        BloodTempestAfterimageProfile profile = buffer.readEnum(BloodTempestAfterimageProfile.class);
        if (!buffer.readBoolean()) {
            return new MessageBloodTempestAfterimage(entityId, profile);
        }
        return new MessageBloodTempestAfterimage(entityId, profile, readVec3(buffer), readVec3(buffer));
    }

    public static void handle(MessageBloodTempestAfterimage message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleBloodTempestAfterimage(message));
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 position) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
