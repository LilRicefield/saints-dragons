package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.platform.NetworkHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricNetworkHelper implements NetworkHelper {
    private enum Direction {
        SERVERBOUND,
        CLIENTBOUND
    }

    private static final class Binding<T> {
        final ResourceLocation id;
        final PacketEncoder<T> encoder;
        final Direction direction;

        Binding(ResourceLocation id, PacketEncoder<T> encoder, Direction direction) {
            this.id = id;
            this.encoder = encoder;
            this.direction = direction;
        }
    }

    private final Map<Class<?>, Binding<?>> bindings = new ConcurrentHashMap<>();

    @Override
    public <T> void registerServerbound(Class<T> type,
                                        ResourceLocation id,
                                        PacketEncoder<T> encoder,
                                        PacketDecoder<T> decoder,
                                        ServerboundHandler<T> handler) {
        bindings.put(type, new Binding<>(id, encoder, Direction.SERVERBOUND));
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handlerAccessor, buf, responseSender) -> {
            T message = decoder.decode(buf);
            server.execute(() -> handler.handle(message, player));
        });
    }

    @Override
    public <T> void registerClientbound(Class<T> type,
                                        ResourceLocation id,
                                        PacketEncoder<T> encoder,
                                        PacketDecoder<T> decoder,
                                        ClientboundHandler<T> handler) {
        bindings.put(type, new Binding<>(id, encoder, Direction.CLIENTBOUND));
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientAccess.register(id, decoder, handler);
        }
    }

    @Override
    public void sendToServer(Object message) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new IllegalStateException("Client-only networking method invoked on a non-client environment");
        }
        Binding<Object> binding = bindingFor(message);
        if (binding.direction != Direction.SERVERBOUND) {
            throw new IllegalStateException("Attempted to send clientbound packet to server: " + message.getClass());
        }
        FriendlyByteBuf buffer = createBuffer(binding, message);
        ClientAccess.send(binding.id, buffer);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        Binding<Object> binding = bindingFor(message);
        if (binding.direction != Direction.CLIENTBOUND) {
            throw new IllegalStateException("Attempted to send serverbound packet to player: " + message.getClass());
        }
        FriendlyByteBuf buffer = createBuffer(binding, message);
        ServerPlayNetworking.send(player, binding.id, buffer);
    }

    @Override
    public void sendToTracking(Entity entity, Object message) {
        Binding<Object> binding = bindingFor(message);
        if (binding.direction != Direction.CLIENTBOUND) {
            throw new IllegalStateException("Attempted to send serverbound packet to tracking players: " + message.getClass());
        }
        for (ServerPlayer tracking : PlayerLookup.tracking(entity)) {
            FriendlyByteBuf buffer = createBuffer(binding, message);
            ServerPlayNetworking.send(tracking, binding.id, buffer);
        }
    }

    @Override
    public void sendToDimension(Level level, Object message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Binding<Object> binding = bindingFor(message);
        if (binding.direction != Direction.CLIENTBOUND) {
            throw new IllegalStateException("Attempted to send serverbound packet to dimension: " + message.getClass());
        }
        for (ServerPlayer player : PlayerLookup.world(serverLevel)) {
            FriendlyByteBuf buffer = createBuffer(binding, message);
            ServerPlayNetworking.send(player, binding.id, buffer);
        }
    }

    private Binding<Object> bindingFor(Object message) {
        Class<?> messageClass = message.getClass();
        @SuppressWarnings("unchecked")
        Binding<Object> binding = (Binding<Object>) bindings.get(messageClass);
        if (binding == null) {
            throw new IllegalStateException("No network binding registered for " + messageClass.getName());
        }
        return binding;
    }

    private FriendlyByteBuf createBuffer(Binding<Object> binding, Object message) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        @SuppressWarnings("unchecked")
        PacketEncoder<Object> encoder = (PacketEncoder<Object>) binding.encoder;
        encoder.encode(message, buffer);
        return buffer;
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientAccess {
        private ClientAccess() {}

        private static <T> void register(ResourceLocation id,
                                         PacketDecoder<T> decoder,
                                         ClientboundHandler<T> handler) {
            ClientPlayNetworking.registerGlobalReceiver(id, (client, handlerAccessor, buf, responseSender) -> {
                T message = decoder.decode(buf);
                client.execute(() -> handler.handle(message));
            });
        }

        private static void send(ResourceLocation id, FriendlyByteBuf buffer) {
            ClientPlayNetworking.send(id, buffer);
        }
    }
}
