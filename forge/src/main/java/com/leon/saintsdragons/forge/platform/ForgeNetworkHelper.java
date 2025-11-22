package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.NetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;

public final class ForgeNetworkHelper implements NetworkHelper {
    private static final String PROTOCOL_VERSION = "1";
    private SimpleChannel channel;
    private final AtomicInteger nextId = new AtomicInteger();

    public ForgeNetworkHelper() {
        // Lazy initialization - defer channel creation until first use
        // to avoid ServiceConfigurationError during early class loading
    }

    private SimpleChannel getChannel() {
        if (channel == null) {
            channel = NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(SaintsDragonsCommon.MOD_ID, "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );
        }
        return channel;
    }

    @Override
    public <T> void registerServerbound(Class<T> type,
                                        ResourceLocation id,
                                        PacketEncoder<T> encoder,
                                        PacketDecoder<T> decoder,
                                        ServerboundHandler<T> handler) {
        getChannel().messageBuilder(type, nextId.getAndIncrement())
                .encoder(encoder::encode)
                .decoder(decoder::decode)
                .consumerMainThread((message, contextSupplier) -> {
                    ServerPlayer sender = contextSupplier.get().getSender();
                    if (sender != null) {
                        handler.handle(message, sender);
                    }
                })
                .add();
    }

    @Override
    public <T> void registerClientbound(Class<T> type,
                                        ResourceLocation id,
                                        PacketEncoder<T> encoder,
                                        PacketDecoder<T> decoder,
                                        ClientboundHandler<T> handler) {
        getChannel().messageBuilder(type, nextId.getAndIncrement())
                .encoder(encoder::encode)
                .decoder(decoder::decode)
                .consumerMainThread((message, contextSupplier) -> handler.handle(message))
                .add();
    }

    @Override
    public void sendToServer(Object message) {
        getChannel().sendToServer(message);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        getChannel().send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToTracking(Entity entity, Object message) {
        getChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    @Override
    public void sendToDimension(Level level, Object message) {
        getChannel().send(PacketDistributor.DIMENSION.with(level::dimension), message);
    }
}
