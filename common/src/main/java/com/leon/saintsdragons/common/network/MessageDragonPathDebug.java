package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record MessageDragonPathDebug(
        boolean active,
        int entityId,
        String locomotionMode,
        String movementMode,
        List<Vec3> navigationNodes,
        int navigationFirstIndex,
        int navigationNextIndex,
        int navigationNodeCount,
        List<Vec3> swimNodes,
        int swimFirstIndex,
        int swimNextIndex,
        int swimNodeCount,
        String searchType,
        long searchId,
        List<Vec3> searchClosedNodes,
        int searchClosedNodeCount,
        List<Vec3> searchOpenNodes,
        int searchOpenNodeCount,
        List<Vec3> searchCandidateNodes,
        int searchCandidateNodeCount,
        @Nullable Vec3 searchStart,
        @Nullable Vec3 searchTarget,
        boolean searchReached,
        long searchDurationMicros,
        @Nullable Vec3 movementTarget,
        @Nullable Vec3 swimTarget,
        @Nullable Vec3 swimEndpoint,
        @Nullable Vec3 rejectedTarget,
        @Nullable Vec3 combatTarget,
        boolean swimCalculating,
        boolean swimMoving,
        int swimStuckTicks,
        int swimRetries
) {
    private static final int MAX_NODE_COUNT = 512;

    public MessageDragonPathDebug {
        locomotionMode = locomotionMode == null ? "NONE" : locomotionMode;
        movementMode = movementMode == null ? "NONE" : movementMode;
        searchType = searchType == null ? "NONE" : searchType;
        navigationNodes = navigationNodes == null ? List.of() : List.copyOf(navigationNodes);
        swimNodes = swimNodes == null ? List.of() : List.copyOf(swimNodes);
        searchClosedNodes = searchClosedNodes == null ? List.of() : List.copyOf(searchClosedNodes);
        searchOpenNodes = searchOpenNodes == null ? List.of() : List.copyOf(searchOpenNodes);
        searchCandidateNodes = searchCandidateNodes == null ? List.of() : List.copyOf(searchCandidateNodes);
    }

    public static MessageDragonPathDebug clear() {
        return new MessageDragonPathDebug(
                false,
                -1,
                "NONE",
                "NONE",
                List.of(),
                0,
                0,
                0,
                List.of(),
                0,
                0,
                0,
                "NONE",
                0L,
                List.of(),
                0,
                List.of(),
                0,
                List.of(),
                0,
                null,
                null,
                false,
                0L,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                0,
                0
        );
    }

    public static void encode(MessageDragonPathDebug message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active());
        if (!message.active()) {
            return;
        }

        buffer.writeVarInt(message.entityId());
        buffer.writeUtf(message.locomotionMode());
        buffer.writeUtf(message.movementMode());
        writeVecList(buffer, message.navigationNodes());
        buffer.writeVarInt(message.navigationFirstIndex());
        buffer.writeVarInt(message.navigationNextIndex());
        buffer.writeVarInt(message.navigationNodeCount());
        writeVecList(buffer, message.swimNodes());
        buffer.writeVarInt(message.swimFirstIndex());
        buffer.writeVarInt(message.swimNextIndex());
        buffer.writeVarInt(message.swimNodeCount());
        buffer.writeUtf(message.searchType());
        buffer.writeVarLong(message.searchId());
        writeVecList(buffer, message.searchClosedNodes());
        buffer.writeVarInt(message.searchClosedNodeCount());
        writeVecList(buffer, message.searchOpenNodes());
        buffer.writeVarInt(message.searchOpenNodeCount());
        writeVecList(buffer, message.searchCandidateNodes());
        buffer.writeVarInt(message.searchCandidateNodeCount());
        writeNullableVec(buffer, message.searchStart());
        writeNullableVec(buffer, message.searchTarget());
        buffer.writeBoolean(message.searchReached());
        buffer.writeVarLong(message.searchDurationMicros());
        writeNullableVec(buffer, message.movementTarget());
        writeNullableVec(buffer, message.swimTarget());
        writeNullableVec(buffer, message.swimEndpoint());
        writeNullableVec(buffer, message.rejectedTarget());
        writeNullableVec(buffer, message.combatTarget());
        buffer.writeBoolean(message.swimCalculating());
        buffer.writeBoolean(message.swimMoving());
        buffer.writeVarInt(message.swimStuckTicks());
        buffer.writeVarInt(message.swimRetries());
    }

    public static MessageDragonPathDebug decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return clear();
        }

        return new MessageDragonPathDebug(
                true,
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                readVecList(buffer),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                readVecList(buffer),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readVarLong(),
                readVecList(buffer),
                buffer.readVarInt(),
                readVecList(buffer),
                buffer.readVarInt(),
                readVecList(buffer),
                buffer.readVarInt(),
                readNullableVec(buffer),
                readNullableVec(buffer),
                buffer.readBoolean(),
                buffer.readVarLong(),
                readNullableVec(buffer),
                readNullableVec(buffer),
                readNullableVec(buffer),
                readNullableVec(buffer),
                readNullableVec(buffer),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(MessageDragonPathDebug message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDragonPathDebug(message));
    }

    private static void writeVecList(FriendlyByteBuf buffer, List<Vec3> positions) {
        buffer.writeVarInt(positions.size());
        for (Vec3 position : positions) {
            writeVec(buffer, position);
        }
    }

    private static List<Vec3> readVecList(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_NODE_COUNT) {
            throw new IllegalArgumentException("Invalid dragon path debug node count: " + size);
        }
        List<Vec3> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(readVec(buffer));
        }
        return positions;
    }

    private static void writeNullableVec(FriendlyByteBuf buffer, @Nullable Vec3 position) {
        buffer.writeBoolean(position != null);
        if (position != null) {
            writeVec(buffer, position);
        }
    }

    private static @Nullable Vec3 readNullableVec(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? readVec(buffer) : null;
    }

    private static void writeVec(FriendlyByteBuf buffer, Vec3 position) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
