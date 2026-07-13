package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.item.tools.BloodTempestKatanaAbility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class MessageBloodTempestKatanaAbility {
    public static final MessageBloodTempestKatanaAbility INSTANCE = new MessageBloodTempestKatanaAbility();

    private MessageBloodTempestKatanaAbility() {
    }

    public static void encode(MessageBloodTempestKatanaAbility message, FriendlyByteBuf buffer) {
    }

    public static MessageBloodTempestKatanaAbility decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MessageBloodTempestKatanaAbility message, ServerPlayer player) {
        BloodTempestKatanaAbility.tryUse(player);
    }
}
