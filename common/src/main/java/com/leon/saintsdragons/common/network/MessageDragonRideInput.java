package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record MessageDragonRideInput(
        boolean goingUp,
        boolean goingDown,
        DragonRiderAction action,
        String abilityName,
        float forward,
        float strafe,
        float yaw
) {
    private static final int MAX_ABILITY_NAME_LENGTH = 64;

    public boolean hasAbilityName() {
        return abilityName != null && !abilityName.isEmpty();
    }

    public static void encode(MessageDragonRideInput msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.goingUp());
        buf.writeBoolean(msg.goingDown());
        buf.writeEnum(msg.action() != null ? msg.action() : DragonRiderAction.NONE);
        if (actionCarriesString(msg.action())) {
            buf.writeUtf(msg.abilityName() != null ? msg.abilityName() : "", MAX_ABILITY_NAME_LENGTH);
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
        if (actionCarriesString(action)) {
            abilityName = buf.readUtf(MAX_ABILITY_NAME_LENGTH);
            if (abilityName.isEmpty()) {
                abilityName = null;
            }
        }
        float forward = buf.readFloat();
        float strafe = buf.readFloat();
        float yaw = buf.readFloat();
        return new MessageDragonRideInput(goingUp, goingDown, action, abilityName, forward, strafe, yaw);
    }

    public static void handle(MessageDragonRideInput msg, ServerPlayer player) {
        if (player == null) {
            return;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof RideableDragonBase dragon && dragon.canBeControlledBy(player)) {
            dragon.handleRiderNetworkInput(player, msg);
        }
    }

    private static boolean actionCarriesString(DragonRiderAction action) {
        return action == DragonRiderAction.ABILITY_USE
                || action == DragonRiderAction.ABILITY_STOP
                || action == DragonRiderAction.GROUND_JUMP;
    }
}