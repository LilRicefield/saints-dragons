package com.leon.saintsdragons.client.input;

import com.leon.saintsdragons.common.network.MessageDragonlordDoubleJump;
import com.leon.saintsdragons.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class DragonlordDoubleJumpInput {
    private static boolean wasJumpDown;
    private static boolean wasOnGround = true;

    private DragonlordDoubleJumpInput() {
    }

    public static void clientTick(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        boolean jumpDown = minecraft.options.keyJump.isDown();
        if (player == null) {
            wasJumpDown = jumpDown;
            wasOnGround = true;
            return;
        }

        boolean onGround = player.onGround();
        if (jumpDown && !wasJumpDown && !onGround && !wasOnGround && !player.isPassenger()
                && !player.getAbilities().flying) {
            NetworkHandler.sendToServer(MessageDragonlordDoubleJump.INSTANCE);
        }
        wasJumpDown = jumpDown;
        wasOnGround = onGround;
    }
}
