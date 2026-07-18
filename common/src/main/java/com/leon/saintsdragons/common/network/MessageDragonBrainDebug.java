package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record MessageDragonBrainDebug(
        boolean active,
        int entityId,
        String dragonName,
        long gameTime,
        int hunger,
        int maxHunger,
        boolean huntFoodPursuit,
        String activeActivity,
        List<String> activeActivities,
        List<BehaviourState> behaviours,
        List<MemoryState> memories,
        List<Marker> markers
) {
    private static final int MAX_ACTIVITIES = 16;
    private static final int MAX_BEHAVIOURS = 64;
    private static final int MAX_MEMORIES = 32;
    private static final int MAX_MARKERS = 32;
    private static final int MAX_DETAILS = 16;
    private static final int MAX_STRING_LENGTH = 192;

    public MessageDragonBrainDebug {
        dragonName = safe(dragonName, "unknown");
        activeActivity = safe(activeActivity, "none");
        activeActivities = copyLimited(activeActivities, MAX_ACTIVITIES);
        behaviours = copyLimited(behaviours, MAX_BEHAVIOURS);
        memories = copyLimited(memories, MAX_MEMORIES);
        markers = copyLimited(markers, MAX_MARKERS);
    }

    public static MessageDragonBrainDebug clear() {
        return new MessageDragonBrainDebug(
                false, -1, "", 0L, 0, 0, false,
                "none", List.of(), List.of(), List.of(), List.of());
    }

    public static void encode(MessageDragonBrainDebug message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active());
        if (!message.active()) {
            return;
        }

        buffer.writeVarInt(message.entityId());
        writeString(buffer, message.dragonName());
        buffer.writeVarLong(message.gameTime());
        buffer.writeVarInt(message.hunger());
        buffer.writeVarInt(message.maxHunger());
        buffer.writeBoolean(message.huntFoodPursuit());
        writeString(buffer, message.activeActivity());
        writeList(buffer, message.activeActivities(), MessageDragonBrainDebug::writeString);
        writeList(buffer, message.behaviours(), BehaviourState::encode);
        writeList(buffer, message.memories(), MemoryState::encode);
        writeList(buffer, message.markers(), Marker::encode);
    }

    public static MessageDragonBrainDebug decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return clear();
        }

        return new MessageDragonBrainDebug(
                true,
                buffer.readVarInt(),
                readString(buffer),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                readString(buffer),
                readList(buffer, MAX_ACTIVITIES, MessageDragonBrainDebug::readString),
                readList(buffer, MAX_BEHAVIOURS, BehaviourState::decode),
                readList(buffer, MAX_MEMORIES, MemoryState::decode),
                readList(buffer, MAX_MARKERS, Marker::decode)
        );
    }

    public static void handle(MessageDragonBrainDebug message) {
        Services.PLATFORM.runOnClient(() ->
                com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDragonBrainDebug(message));
    }

    public record BehaviourState(String activity,
                                 int priority,
                                 String name,
                                 String status,
                                 boolean claimsControl,
                                 long cooldownTicks,
                                 List<String> details) {
        public BehaviourState {
            activity = safe(activity, "none");
            name = safe(name, "unknown");
            status = safe(status, "STOPPED");
            details = copyLimited(details, MAX_DETAILS);
        }

        private static void encode(FriendlyByteBuf buffer, BehaviourState state) {
            writeString(buffer, state.activity());
            buffer.writeVarInt(state.priority());
            writeString(buffer, state.name());
            writeString(buffer, state.status());
            buffer.writeBoolean(state.claimsControl());
            buffer.writeVarLong(state.cooldownTicks());
            writeList(buffer, state.details(), MessageDragonBrainDebug::writeString);
        }

        private static BehaviourState decode(FriendlyByteBuf buffer) {
            return new BehaviourState(
                    readString(buffer),
                    buffer.readVarInt(),
                    readString(buffer),
                    readString(buffer),
                    buffer.readBoolean(),
                    buffer.readVarLong(),
                    readList(buffer, MAX_DETAILS, MessageDragonBrainDebug::readString)
            );
        }
    }

    public record MemoryState(String name, String value) {
        public MemoryState {
            name = safe(name, "unknown");
            value = safe(value, "empty");
        }

        private static void encode(FriendlyByteBuf buffer, MemoryState state) {
            writeString(buffer, state.name());
            writeString(buffer, state.value());
        }

        private static MemoryState decode(FriendlyByteBuf buffer) {
            return new MemoryState(readString(buffer), readString(buffer));
        }
    }

    public record Marker(String kind,
                         @Nullable Vec3 position,
                         int entityId,
                         String label) {
        public Marker {
            kind = safe(kind, "UNKNOWN");
            label = safe(label, "");
        }

        private static void encode(FriendlyByteBuf buffer, Marker marker) {
            writeString(buffer, marker.kind());
            buffer.writeBoolean(marker.position() != null);
            if (marker.position() != null) {
                buffer.writeDouble(marker.position().x);
                buffer.writeDouble(marker.position().y);
                buffer.writeDouble(marker.position().z);
            }
            buffer.writeVarInt(marker.entityId());
            writeString(buffer, marker.label());
        }

        private static Marker decode(FriendlyByteBuf buffer) {
            String kind = readString(buffer);
            Vec3 position = buffer.readBoolean()
                    ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                    : null;
            return new Marker(kind, position, buffer.readVarInt(), readString(buffer));
        }
    }

    private static String safe(@Nullable String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.length() <= MAX_STRING_LENGTH ? value : value.substring(0, MAX_STRING_LENGTH);
    }

    private static void writeString(FriendlyByteBuf buffer, String value) {
        buffer.writeUtf(safe(value, ""), MAX_STRING_LENGTH);
    }

    private static String readString(FriendlyByteBuf buffer) {
        return buffer.readUtf(MAX_STRING_LENGTH);
    }

    private static <T> List<T> copyLimited(@Nullable List<T> source, int maximum) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source.subList(0, Math.min(maximum, source.size())));
    }

    private static <T> void writeList(FriendlyByteBuf buffer, List<T> values, Writer<T> writer) {
        buffer.writeVarInt(values.size());
        for (T value : values) {
            writer.write(buffer, value);
        }
    }

    private static <T> List<T> readList(FriendlyByteBuf buffer, int maximum, Reader<T> reader) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid dragon brain debug list size: " + size);
        }
        List<T> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(reader.read(buffer));
        }
        return values;
    }

    @FunctionalInterface
    private interface Writer<T> {
        void write(FriendlyByteBuf buffer, T value);
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(FriendlyByteBuf buffer);
    }
}
