package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.forge.entity.part.ForgeDragonPart;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$onHandleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        ServerLevel level = this.player.serverLevel();
        int entityId = ((ServerboundInteractPacketAccessor) packet).getEntityId();
        Entity vanillaEntity = level.getEntity(entityId);

        if (vanillaEntity instanceof ForgeDragonPart directPart) {
            packet.dispatch(new ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(InteractionHand hand) {
                }

                @Override
                public void onInteraction(InteractionHand hand, Vec3 pos) {
                }

                @Override
                public void onAttack() {
                    player.server.execute(() -> {
                        if (!player.isRemoved() && directPart.isAlive() && directPart.level() == player.level()) {
                            player.attack(directPart);
                        }
                    });
                }
            });
            ci.cancel();
            return;
        }

        if (vanillaEntity == null) {
            ForgeDragonPart hitPart = saintsdragons$findHitPartEntity(level);

            if (hitPart != null) {
                packet.dispatch(new ServerboundInteractPacket.Handler() {
                    @Override
                    public void onInteraction(InteractionHand hand) {
                    }

                    @Override
                    public void onInteraction(InteractionHand hand, Vec3 pos) {
                    }

                    @Override
                    public void onAttack() {
                        player.server.execute(() -> {
                            if (!player.isRemoved() && hitPart.isAlive() && hitPart.level() == player.level()) {
                                player.attack(hitPart);
                            }
                        });
                    }
                });

                ci.cancel();
            }
        }
    }

    @Unique
    private ForgeDragonPart saintsdragons$findHitPartEntity(ServerLevel level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachPos = eyePos.add(lookVec.scale(player.getEntityReach()));

        ForgeDragonPart closestPart = null;
        double closestDistance = Double.MAX_VALUE;

        for (PartEntity<?> part : level.getPartEntities()) {
            if (part instanceof ForgeDragonPart forgePart) {
                AABB box = forgePart.getBoundingBox();

                var clipResult = box.clip(eyePos, reachPos);
                if (clipResult.isPresent()) {
                    double distance = eyePos.distanceToSqr(clipResult.get());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestPart = forgePart;
                    }
                }
            }
        }

        return closestPart;
    }

}
