package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.fabric.entity.part.FabricDragonPart;
import com.leon.saintsdragons.fabric.entity.part.IgnivorusPartProvider;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fix PartEntity attack handling on the server.
 *
 * Problem: Client and server create PartEntities independently, so they have different IDs.
 * When a client attacks a PartEntity, it sends its local entity ID which doesn't exist on the server.
 *
 * Solution: When we receive an unknown entity ID, check if the player's look direction
 * intersects with any PartEntity on the server side, and dispatch the attack to that part.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MultipartInteractionBridgeMixin {

    @Unique
    private static final double ATTACK_REACH = 6.0; // Player attack reach

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$onHandleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        ServerLevel level = this.player.serverLevel();

        // Get the entity ID from the packet using our accessor
        int entityId = ((ServerboundInteractPacketIdAccessor) packet).getEntityId();

        // First check if vanilla can find it (regular entity)
        Entity vanillaEntity = level.getEntity(entityId);

        if (vanillaEntity instanceof FabricDragonPart directPart) {
            packet.dispatch(new ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(InteractionHand hand) {
                    directPart.interact(player, hand);
                }

                @Override
                public void onInteraction(InteractionHand hand, Vec3 pos) {
                    directPart.interactAt(player, pos, hand);
                }

                @Override
                public void onAttack() {
                    player.attack(directPart);
                }
            });
            ci.cancel();
            return;
        }

        if (vanillaEntity == null) {
            // Vanilla couldn't find it - the client might be targeting a PartEntity
            // Since client/server have different part IDs, we need to raycast to find the hit part
            FabricDragonPart hitPart = saintsdragons$findHitPartEntity(level);

            if (hitPart != null) {
                // Dispatch the interaction to the PartEntity
                packet.dispatch(new ServerboundInteractPacket.Handler() {
                    @Override
                    public void onInteraction(InteractionHand hand) {
                        hitPart.interact(player, hand);
                    }

                    @Override
                    public void onInteraction(InteractionHand hand, Vec3 pos) {
                        hitPart.interactAt(player, pos, hand);
                    }

                    @Override
                    public void onAttack() {
                        player.attack(hitPart);
                    }
                });

                // Cancel the original handling since we handled it
                ci.cancel();
            }
        }
        // If vanillaEntity != null, let vanilla handle it normally
    }

    /**
     * Raycast from the player's eye position to find the nearest PartEntity they're looking at.
     */
    @Unique
    private FabricDragonPart saintsdragons$findHitPartEntity(ServerLevel level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachPos = eyePos.add(lookVec.scale(ATTACK_REACH));

        FabricDragonPart closestPart = null;
        double closestDistance = Double.MAX_VALUE;

        // Check all Ignivorus part providers for hitbox parts
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof IgnivorusPartProvider provider) {
                FabricDragonPart[] providerParts = provider.saintsdragons$getParts();
                for (FabricDragonPart part : providerParts) {
                    if (part == null) {
                        continue;
                    }
                    AABB box = part.getBoundingBox();
                    var clipResult = box.clip(eyePos, reachPos);
                    if (clipResult.isPresent()) {
                        double distance = eyePos.distanceToSqr(clipResult.get());
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestPart = part;
                        }
                    }
                }
            }
        }

        return closestPart;
    }
}
