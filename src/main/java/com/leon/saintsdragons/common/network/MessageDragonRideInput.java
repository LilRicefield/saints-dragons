package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MessageDragonRideInput(boolean goingUp,
                                     boolean goingDown,
                                     DragonRiderAction action,
                                     String abilityName,
                                     float forward,
                                     float strafe,
                                     float yaw) {

    public boolean hasAbilityName() {
        return abilityName != null && !abilityName.isEmpty();
    }

    public static void encode(MessageDragonRideInput msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.goingUp());
        buf.writeBoolean(msg.goingDown());
        buf.writeEnum(msg.action() != null ? msg.action() : DragonRiderAction.NONE);
        // Only send ability name if it's an ability use/stop action
        if (msg.action() == DragonRiderAction.ABILITY_USE || msg.action() == DragonRiderAction.ABILITY_STOP) {
            buf.writeUtf(msg.abilityName() != null ? msg.abilityName() : "");
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
        // Only read ability name if it's an ability use/stop action
        if (action == DragonRiderAction.ABILITY_USE || action == DragonRiderAction.ABILITY_STOP) {
            abilityName = buf.readUtf();
            if (abilityName.isEmpty()) abilityName = null;
        }
        float forward = buf.readFloat();
        float strafe = buf.readFloat();
        float yaw = buf.readFloat();
        return new MessageDragonRideInput(goingUp, goingDown, action, abilityName, forward, strafe, yaw);
    }
    
    public static void handle(MessageDragonRideInput msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof LightningDragonEntity dragon && dragon.isTame() && dragon.isOwnedBy(player)) {
                    // Always handle vertical movement commands when flying
                    boolean locked = dragon.areRiderControlsLocked();
                    if (dragon.isFlying()) {
                        dragon.setGoingUp(msg.goingUp());
                        dragon.setGoingDown(msg.goingDown());
                    } else {
                        // Clear vertical movement when not flying
                        dragon.setGoingUp(false);
                        dragon.setGoingDown(false);
                    }

                    // Update last ground movement inputs on the server for animation syncing
                    // Store small values as zero to avoid drift
                    float fwd = Math.abs(msg.forward()) > 0.02f ? msg.forward() : 0f;
                    float str = Math.abs(msg.strafe()) > 0.02f ? msg.strafe() : 0f;
                    dragon.setLastRiderForward(fwd);
                    dragon.setLastRiderStrafe(str);

                    // Immediate ground movement state update for robust observer sync
                    if (!dragon.isFlying()) {
                        int moveState = 0;
                        float mag = Math.abs(fwd) + Math.abs(str);
                        if (mag > 0.05f) {
                            moveState = dragon.isAccelerating() ? 2 : 1;
                        }
                        if (dragon.getEntityData().get(DATA_GROUND_MOVE_STATE) != moveState) {
                            dragon.getEntityData().set(DATA_GROUND_MOVE_STATE, moveState);
                            // Also nudge observers immediately
                            com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> dragon),
                                    new com.leon.saintsdragons.common.network.MessageDragonAnimState(dragon.getId(), (byte) moveState, (byte) dragon.getSyncedFlightMode())
                            );
                        }
                    }

                    // Handle actions using type-safe enum
                    switch (msg.action()) {
                        case TAKEOFF_REQUEST:
                            // Block takeoff while locked (e.g., ground roar)
                            if (!locked) {
                                dragon.requestRiderTakeoff();
                            }
                            break;
                        case ACCELERATE:
                            // Handle acceleration input - L-Ctrl pressed
                            if (!locked) dragon.setAccelerating(true);
                            break;
                        case STOP_ACCELERATE:
                            // Handle acceleration stop - L-Ctrl released
                            dragon.setAccelerating(false);
                            break;
                        case ABILITY_USE:
                            if (msg.hasAbilityName()) {
                                dragon.useRidingAbility(msg.abilityName());
                            }
                            break;
                        case ABILITY_STOP:
                            // Stop an active hold-to-use ability
                            if (msg.hasAbilityName()) {
                                var active = dragon.getActiveAbility();
                                if (active != null) {
                                    // Stop regardless of type when names match, to avoid tight coupling
                                    // (Optional) could check registry name here if needed
                                    dragon.forceEndActiveAbility();
                                }
                            }
                            break;
                        case NONE:
                        default:
                            // No special action to handle
                            break;
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
