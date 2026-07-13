package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.tools.DragonlordSwordAbility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class MessageDragonlordSwordAbility {
    public static final MessageDragonlordSwordAbility INSTANCE = new MessageDragonlordSwordAbility();

    private MessageDragonlordSwordAbility() {
    }

    public static void encode(MessageDragonlordSwordAbility message, FriendlyByteBuf buffer) {
    }

    public static MessageDragonlordSwordAbility decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MessageDragonlordSwordAbility message, ServerPlayer player) {
        DragonlordSwordAbility.tryUse(player);
    }
}
