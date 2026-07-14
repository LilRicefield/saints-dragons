package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class MessageDragonlordDoubleJump {
    public static final MessageDragonlordDoubleJump INSTANCE = new MessageDragonlordDoubleJump();

    private MessageDragonlordDoubleJump() {
    }

    public static void encode(MessageDragonlordDoubleJump message, FriendlyByteBuf buffer) {
    }

    public static MessageDragonlordDoubleJump decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MessageDragonlordDoubleJump message, ServerPlayer player) {
        DragonlordArmorSetBonus.handleAirborneJump(player);
    }
}
