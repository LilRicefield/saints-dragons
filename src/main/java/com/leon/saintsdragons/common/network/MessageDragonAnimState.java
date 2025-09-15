package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client animation state pulse for observers.
 * Mirrors authoritative ground/flight state so non-owners render the same loops while ridden.
 */
public record MessageDragonAnimState(int entityId, byte groundState, byte flightMode) {

    public static void encode(MessageDragonAnimState msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId());
        buf.writeByte(msg.groundState());
        buf.writeByte(msg.flightMode());
    }

    public static MessageDragonAnimState decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        byte ground = buf.readByte();
        byte flight = buf.readByte();
        return new MessageDragonAnimState(id, ground, flight);
    }

    public static void handle(MessageDragonAnimState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Entity e = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityId()) : null;
            if (e instanceof LightningDragonEntity dragon) {
                dragon.applyClientAnimState(msg.groundState(), msg.flightMode());
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
