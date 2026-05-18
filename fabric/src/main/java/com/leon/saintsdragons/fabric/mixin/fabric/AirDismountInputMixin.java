package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LocalPlayer.class)
public abstract class AirDismountInputMixin {
    @Unique
    private static final String SAINTSDRAGONS_LAND_TO_DISMOUNT_MESSAGE = "entity.saintsdragons.all.land_to_dismount";
    @Unique
    private static final int SAINTSDRAGONS_MESSAGE_INTERVAL_TICKS = 20;
    @Unique
    private long saintsdragons$lastBlockedDismountMessageTick = -SAINTSDRAGONS_MESSAGE_INTERVAL_TICKS;

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerInputPacket;<init>(FFZZ)V"
            ),
            index = 3
    )
    private boolean saintsdragons$blockAirDismountPacket(boolean shiftKeyDown) {
        if (!shiftKeyDown) {
            return false;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof RideableFlyingDragon dragon && saintsdragons$shouldBlockDismount(player, dragon)) {
            saintsdragons$sendBlockedMessage(player);
            return false;
        }
        return true;
    }

    @Unique
    private static boolean saintsdragons$shouldBlockDismount(LocalPlayer player, RideableFlyingDragon dragon) {
        return dragon.getControllingPassenger() == player && !dragon.canRiderShiftDismount();
    }

    @Unique
    private void saintsdragons$sendBlockedMessage(LocalPlayer player) {
        long gameTime = player.level().getGameTime();
        if (gameTime - saintsdragons$lastBlockedDismountMessageTick < SAINTSDRAGONS_MESSAGE_INTERVAL_TICKS) {
            return;
        }
        saintsdragons$lastBlockedDismountMessageTick = gameTime;
        player.displayClientMessage(Component.translatable(SAINTSDRAGONS_LAND_TO_DISMOUNT_MESSAGE), true);
    }
}
