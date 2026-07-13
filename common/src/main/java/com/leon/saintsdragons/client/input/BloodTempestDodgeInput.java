package com.leon.saintsdragons.client.input;

import com.leon.saintsdragons.common.item.BloodTempestArmorSetBonus;
import com.leon.saintsdragons.common.network.BloodTempestDodgeDirection;
import com.leon.saintsdragons.common.network.MessageBloodTempestDodge;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class BloodTempestDodgeInput {
    private static final long DOUBLE_TAP_WINDOW_MS = 200L;

    private static long lastForwardTap;
    private static long lastLeftTap;
    private static long lastBackwardTap;
    private static long lastRightTap;
    private static boolean wasForwardDown;
    private static boolean wasLeftDown;
    private static boolean wasBackwardDown;
    private static boolean wasRightDown;

    private BloodTempestDodgeInput() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || player.getVehicle() instanceof RideableDragonBase
                || !BloodTempestArmorSetBonus.isWearingFullSet(player)) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        boolean forwardDown = minecraft.options.keyUp.isDown();
        boolean leftDown = minecraft.options.keyLeft.isDown();
        boolean backwardDown = minecraft.options.keyDown.isDown();
        boolean rightDown = minecraft.options.keyRight.isDown();

        lastForwardTap = handleTap(forwardDown, wasForwardDown, lastForwardTap, now, BloodTempestDodgeDirection.FORWARD);
        lastLeftTap = handleTap(leftDown, wasLeftDown, lastLeftTap, now, BloodTempestDodgeDirection.LEFT);
        lastBackwardTap = handleTap(backwardDown, wasBackwardDown, lastBackwardTap, now, BloodTempestDodgeDirection.BACKWARD);
        lastRightTap = handleTap(rightDown, wasRightDown, lastRightTap, now, BloodTempestDodgeDirection.RIGHT);

        wasForwardDown = forwardDown;
        wasLeftDown = leftDown;
        wasBackwardDown = backwardDown;
        wasRightDown = rightDown;
    }

    private static long handleTap(boolean down,
                                  boolean wasDown,
                                  long lastTap,
                                  long now,
                                  BloodTempestDodgeDirection direction) {
        if (!down || wasDown) {
            return lastTap;
        }
        if (lastTap > 0L && now - lastTap <= DOUBLE_TAP_WINDOW_MS) {
            NetworkHandler.sendToServer(new MessageBloodTempestDodge(direction));
            return 0L;
        }
        return now;
    }

    private static void reset() {
        lastForwardTap = 0L;
        lastLeftTap = 0L;
        lastBackwardTap = 0L;
        lastRightTap = 0L;
        wasForwardDown = false;
        wasLeftDown = false;
        wasBackwardDown = false;
        wasRightDown = false;
    }
}
