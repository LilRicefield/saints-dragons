package com.leon.saintsdragons.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            "hip",
            "fireBoneOrigin",
            "breathBoneOrigin",
            "mouth_origin",
            "automountBoneRight",
            "leftwing",
            "rightwing",
            "leftwingjoint",
            "rightwingjoint",
            "tail1",
            "tail2",
            "tail3",
            "tail4",
            "leftfrontleg",
            "rightfrontleg",
            "leftbackleg",
            "rightbackleg"
    };
    private static final int MAX_BONES_PER_PACKET = SYNCED_BONES.length;
    private static final int MAX_BONE_NAME_LENGTH = 32;
    private static final Set<String> ALLOWED_BONES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(SYNCED_BONES))
    );

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
        if (count < 0 || count > MAX_BONES_PER_PACKET) {
            throw new IllegalArgumentException("Invalid bone position count: " + count);
        }
        Map<String, Vec3> positions = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String boneName = buf.readUtf(MAX_BONE_NAME_LENGTH);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            if (ALLOWED_BONES.contains(boneName)) {
                positions.put(boneName, new Vec3(x, y, z));
            }
        }
        return new MessageDragonBonePositions(entityId, positions);
    }

    public static void handle(MessageDragonBonePositions msg, ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (msg.bonePositions().isEmpty()) {
            return;
        }

        // Find the entity in the server world
        Entity entity = player.serverLevel().getEntity(msg.entityId());
        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus ignivorus) {
            // Allow nearby clients tracking the dragon to provide locator updates so multipart
            // hitboxes stay accurate even when the dragon is not being ridden.
            if (player.distanceToSqr(ignivorus) > 128.0D * 128.0D) {
                return;
            }
            for (Map.Entry<String, Vec3> entry : msg.bonePositions().entrySet()) {
                ignivorus.setServerBonePosition(entry.getKey(), entry.getValue());
            }
            return;
        }
        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane cindervane) {
            if (player.getVehicle() != cindervane || !cindervane.canBeControlledBy(player)) {
                return;
            }
            for (Map.Entry<String, Vec3> entry : msg.bonePositions().entrySet()) {
                cindervane.setServerBonePosition(entry.getKey(), entry.getValue());
            }
            return;
        }
        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans volitans) {
            // Allow nearby clients tracking the dragon to provide breath locator updates
            // so wild/unridden Volitans still spawn breath from the animated mouth.
            if (player.distanceToSqr(volitans) > 128.0D * 128.0D) {
                return;
            }
            for (Map.Entry<String, Vec3> entry : msg.bonePositions().entrySet()) {
                volitans.setServerBonePosition(entry.getKey(), entry.getValue());
            }
        }
    }
}
