package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MessageDragonRideInput(boolean goingUp,
                                     boolean goingDown,
                                     DragonRiderAction action,
                                     String abilityName,
                                     float forward,
                                     float strafe,
                                     float yaw) {

    public boolean hasAbilityName() {
        return abilityName != null && !abilityName.isEmpty();
    }

    public static void encode(MessageDragonRideInput msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.goingUp());
        buf.writeBoolean(msg.goingDown());
        buf.writeEnum(msg.action() != null ? msg.action() : DragonRiderAction.NONE);
        if (msg.action() == DragonRiderAction.ABILITY_USE || msg.action() == DragonRiderAction.ABILITY_STOP) {
            buf.writeUtf(msg.abilityName() != null ? msg.abilityName() : "");
        }
        buf.writeFloat(msg.forward());
        buf.writeFloat(msg.strafe());
        buf.writeFloat(msg.yaw());
    }

    public static MessageDragonRideInput decode(FriendlyByteBuf buf) {
        boolean goingUp = buf.readBoolean();
        boolean goingDown = buf.readBoolean();
        DragonRiderAction action = buf.readEnum(DragonRiderAction.class);
        String abilityName = null;
        if (action == DragonRiderAction.ABILITY_USE || action == DragonRiderAction.ABILITY_STOP) {
            abilityName = buf.readUtf();
            if (abilityName.isEmpty()) {
                abilityName = null;
            }
        }
        float forward = buf.readFloat();
        float strafe = buf.readFloat();
        float yaw = buf.readFloat();
        return new MessageDragonRideInput(goingUp, goingDown, action, abilityName, forward, strafe, yaw);
    }

    public static void handle(MessageDragonRideInput msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof RideableDragonBase dragon && dragon.canBeControlledBy(player)) {
                    dragon.handleRiderNetworkInput(player, msg);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
