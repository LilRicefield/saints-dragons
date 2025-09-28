package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
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
                if (vehicle instanceof LightningDragonEntity lightning && lightning.isTame() && lightning.isOwnedBy(player)) {
                    handleLightningInput(msg, lightning);
                } else if (vehicle instanceof AmphithereEntity amphithere && amphithere.isTame() && amphithere.isOwnedBy(player)) {
                    handleAmphithereInput(msg, amphithere);
                } else if (vehicle instanceof RiftDrakeEntity drake && drake.isTame() && drake.isOwnedBy(player)) {
                    handleRiftDrakeInput(msg, drake);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleLightningInput(MessageDragonRideInput msg, LightningDragonEntity dragon) {
        boolean locked = dragon.areRiderControlsLocked();
        if (dragon.isFlying()) {
            dragon.setGoingUp(msg.goingUp());
            dragon.setGoingDown(msg.goingDown());
        } else {
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }

        float fwd = Math.abs(msg.forward()) > 0.02f ? msg.forward() : 0f;
        float str = Math.abs(msg.strafe()) > 0.02f ? msg.strafe() : 0f;
        dragon.setLastRiderForward(fwd);
        dragon.setLastRiderStrafe(str);

        if (!dragon.isFlying()) {
            int moveState = 0;
            float mag = Math.abs(fwd) + Math.abs(str);
            if (mag > 0.05f) {
                moveState = dragon.isAccelerating() ? 2 : 1;
            }
            if (dragon.getEntityData().get(DATA_GROUND_MOVE_STATE) != moveState) {
                dragon.getEntityData().set(DATA_GROUND_MOVE_STATE, moveState);
                dragon.syncAnimState(moveState, dragon.getSyncedFlightMode());
            }
        }

        switch (msg.action()) {
            case TAKEOFF_REQUEST:
                if (!locked) {
                    dragon.requestRiderTakeoff();
                }
                break;
            case ACCELERATE:
                if (!locked) dragon.setAccelerating(true);
                break;
            case STOP_ACCELERATE:
                dragon.setAccelerating(false);
                break;
            case ABILITY_USE:
                if (msg.hasAbilityName()) {
                    dragon.useRidingAbility(msg.abilityName());
                }
                break;
            case ABILITY_STOP:
                if (msg.hasAbilityName()) {
                    var active = dragon.getActiveAbility();
                    if (active != null) {
                        dragon.forceEndActiveAbility();
                    }
                }
                break;
            case NONE:
            default:
                break;
        }
    }

    private static void handleAmphithereInput(MessageDragonRideInput msg, AmphithereEntity dragon) {
        if (dragon.isFlying()) {
            dragon.setGoingUp(msg.goingUp());
            dragon.setGoingDown(msg.goingDown());
        } else {
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }

        float fwd = Math.abs(msg.forward()) > 0.02f ? msg.forward() : 0f;
        float str = Math.abs(msg.strafe()) > 0.02f ? msg.strafe() : 0f;

        if (!dragon.isFlying()) {
            int moveState = 0;
            float mag = Math.abs(fwd) + Math.abs(str);
            if (mag > 0.05f) {
                moveState = dragon.isAccelerating() ? 2 : 1;
            }
            dragon.setGroundMoveStateFromAI(moveState);
            dragon.setRunning(moveState == 2);
        }

        switch (msg.action()) {
            case TAKEOFF_REQUEST:
                dragon.requestRiderTakeoff();
                break;
            case ACCELERATE:
                dragon.setAccelerating(true);
                break;
            case STOP_ACCELERATE:
                dragon.setAccelerating(false);
                break;
            case ABILITY_USE:
                if (msg.hasAbilityName()) {
                    dragon.useRidingAbility(msg.abilityName());
                }
                break;
            case ABILITY_STOP:
                if (msg.hasAbilityName()) {
                    dragon.forceEndActiveAbility();
                }
                break;
            default:
                break;
        }
    }


    private static void handleRiftDrakeInput(MessageDragonRideInput msg, RiftDrakeEntity drake) {
        drake.setGoingUp(false);
        drake.setGoingDown(false);

        float forward = Math.abs(msg.forward()) > 0.02f ? msg.forward() : 0f;
        float strafe = Math.abs(msg.strafe()) > 0.02f ? msg.strafe() : 0f;
        drake.setLastRiderForward(forward);
        drake.setLastRiderStrafe(strafe);

        int moveState = 0;
        float magnitude = Math.abs(forward) + Math.abs(strafe);
        if (magnitude > 0.05f) {
            moveState = drake.isAccelerating() ? 2 : 1;
        }
        drake.setGroundMoveStateFromRider(moveState);

        switch (msg.action()) {
            case ACCELERATE -> drake.setAccelerating(true);
            case STOP_ACCELERATE -> drake.setAccelerating(false);
            case TAKEOFF_REQUEST -> drake.handleJumpRequest();
            default -> {
            }
        }
    }
}
