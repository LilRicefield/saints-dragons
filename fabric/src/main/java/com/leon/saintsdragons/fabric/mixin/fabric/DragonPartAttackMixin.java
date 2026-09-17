package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.common.network.MessageDragonPartAttack;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class DragonPartAttackMixin {
    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void saintsdragons$attackPart(ClientPacketListener connection, Packet<?> packet, Player player, Entity target) {
        if (target instanceof DragonPartEntity part && part.getDragonParent() != null) {
            Entity dragon = part.getDragonParent();
            NetworkHandler.sendToServer(new MessageDragonPartAttack(dragon.getId(), dragon.getUUID(), part.getPartIndex()));
        } else {
            connection.send(packet);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$ignorePartInteraction(Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (target instanceof DragonPartEntity) callback.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$ignorePartInteractionAt(Player player, Entity target, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (target instanceof DragonPartEntity) callback.setReturnValue(InteractionResult.PASS);
    }
}
