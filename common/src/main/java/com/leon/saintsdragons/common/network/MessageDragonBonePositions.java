package com.leon.saintsdragons.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncs dragon bone positions from client (where GeckoLib renders) to server (where hitboxes are checked).
 * This allows hitboxes to follow the animated model precisely.
 */
public record MessageDragonBonePositions(
        int entityId,
        Map<String, Vec3> bonePositions
) {
    // Bone names that we sync for hitbox positioning
    public static final String[] SYNCED_BONES = {
            "headController",
            "neck3Controller",
            "leftwing",
            "rightwing",
            "tail1",
            "tail2",
            "tail3",
            "tail4",
            "leftfrontleg",
            "rightfrontleg",
            "leftbackleg",
            "rightbackleg"
    };

    public static void encode(MessageDragonBonePositions msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId());
        buf.writeVarInt(msg.bonePositions().size());
        for (Map.Entry<String, Vec3> entry : msg.bonePositions().entrySet()) {
            buf.writeUtf(entry.getKey());
            Vec3 pos = entry.getValue();
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
        }
    }

    public static MessageDragonBonePositions decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<String, Vec3> positions = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String boneName = buf.readUtf();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            positions.put(boneName, new Vec3(x, y, z));
        }
        return new MessageDragonBonePositions(entityId, positions);
    }

    public static void handle(MessageDragonBonePositions msg, ServerPlayer player) {
        if (player == null) {
            return;
        }

        // Find the entity in the server world
        Entity entity = player.serverLevel().getEntity(msg.entityId());
        if (entity == null) {
            return;
        }

        // Update the entity's bone position cache
        // Currently only Ignivorus supports this
        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus ignivorus) {
            for (Map.Entry<String, Vec3> entry : msg.bonePositions().entrySet()) {
                ignivorus.setServerBonePosition(entry.getKey(), entry.getValue());
            }
        }
    }
}
