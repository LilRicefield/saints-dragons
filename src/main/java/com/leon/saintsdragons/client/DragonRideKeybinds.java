package com.leon.saintsdragons.client;

import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding.Activation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DragonRideKeybinds {
    // Keybind definitions
    public static final KeyMapping DRAGON_ASCEND = new KeyMapping(
            "key.saintsdragons.ascend",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_SPACE,
            "key.categories.saintsdragons"
    );

    public static final KeyMapping DRAGON_DESCEND = new KeyMapping(
            "key.saintsdragons.descend",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            "key.categories.saintsdragons"
    );

    public static final KeyMapping DRAGON_ACCELERATE = new KeyMapping(
            "key.saintsdragons.accelerate",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LCONTROL,
            "key.categories.saintsdragons"
    );

    public static final KeyMapping DRAGON_TERTIARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_tertiary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.saintsdragons"
    );

    public static final KeyMapping DRAGON_PRIMARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_primary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "key.categories.saintsdragons"
    );

    public static final KeyMapping DRAGON_SECONDARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_secondary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.saintsdragons"
    );

    private static boolean wasAscendPressed = false;
    private static boolean wasAccelerateDown = false;
    private static boolean wasTertiaryAbilityDown = false;
    private static boolean wasPrimaryAbilityDown = false;
    private static boolean wasSecondaryAbilityDown = false;
    private static boolean wasAttackDown = false;

    // Movement state tracking to avoid spamming movement packets
    private static float lastForward = 0f;
    private static float lastStrafe = 0f;
    private static float lastYaw = 0f;
    private static boolean lastAscendDown = false;
    private static boolean lastDescendDown = false;

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventHandler {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(DRAGON_ASCEND);
            event.register(DRAGON_DESCEND);
            event.register(DRAGON_ACCELERATE);
            event.register(DRAGON_TERTIARY_ABILITY);
            event.register(DRAGON_PRIMARY_ABILITY);
            event.register(DRAGON_SECONDARY_ABILITY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            resetStateTracking();
            return;
        }

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !dragon.canBeControlledBy(player)) {
            resetStateTracking();
            return;
        }

        handleGenericControls(player, dragon);
    }

    private static void handleGenericControls(LocalPlayer player, RideableDragonBase dragon) {
        boolean ascendDown = DRAGON_ASCEND.isDown();
        boolean descendDown = DRAGON_DESCEND.isDown();
        boolean accelerateDown = DRAGON_ACCELERATE.isDown();
        boolean tertiaryDown = DRAGON_TERTIARY_ABILITY.isDown();
        boolean primaryDown = DRAGON_PRIMARY_ABILITY.isDown();
        boolean secondaryDown = DRAGON_SECONDARY_ABILITY.isDown();
        Minecraft mc = Minecraft.getInstance();
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean sneakDown = mc.options.keyShift.isDown();

        float forward = player.zza;
        float strafe = player.xxa;
        float yaw = player.getYRot();

        // Only send movement input when something actually changes (prevents spamming 20 packets/sec)
        boolean movementChanged = forward != lastForward ||
                                  strafe != lastStrafe ||
                                  Math.abs(yaw - lastYaw) > 0.1f ||
                                  ascendDown != lastAscendDown ||
                                  descendDown != lastDescendDown;

        if (movementChanged) {
            sendInput(ascendDown, descendDown, DragonRiderAction.NONE, null, forward, strafe, yaw);
            lastForward = forward;
            lastStrafe = strafe;
            lastYaw = yaw;
            lastAscendDown = ascendDown;
            lastDescendDown = descendDown;
        }

        if (accelerateDown != wasAccelerateDown) {
            DragonRiderAction action = accelerateDown ? DragonRiderAction.ACCELERATE : DragonRiderAction.STOP_ACCELERATE;
            sendInput(ascendDown, descendDown, action, null, forward, strafe, yaw);
        }

        if (ascendDown && !wasAscendPressed && !dragon.isFlying()) {
            sendInput(false, false, DragonRiderAction.TAKEOFF_REQUEST, null, forward, strafe, yaw);
        }

        handleAbilityBinding(dragon.getTertiaryRiderAbility(), tertiaryDown, wasTertiaryAbilityDown, forward, strafe, yaw);
        handleAbilityBinding(dragon.getPrimaryRiderAbility(), primaryDown, wasPrimaryAbilityDown, forward, strafe, yaw);
        handleAbilityBinding(dragon.getSecondaryRiderAbility(), secondaryDown, wasSecondaryAbilityDown, forward, strafe, yaw);
        handleAbilityBinding(dragon.getAttackRiderAbility(), attackDown, wasAttackDown, forward, strafe, yaw);

        wasAscendPressed = ascendDown;
        wasAccelerateDown = accelerateDown;
        wasTertiaryAbilityDown = tertiaryDown;
        wasPrimaryAbilityDown = primaryDown;
        wasSecondaryAbilityDown = secondaryDown;
        wasAttackDown = attackDown;
    }

    private static void handleAbilityBinding(RiderAbilityBinding binding, boolean currentDown, boolean previousDown, float forward, float strafe, float yaw) {
        if (binding == null) {
            return;
        }
        String abilityId = binding.abilityId();
        if (abilityId == null || abilityId.isEmpty()) {
            return;
        }

        Activation activation = binding.activation();
        if (activation == Activation.PRESS) {
            if (currentDown && !previousDown) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE, abilityId, forward, strafe, yaw);
            }
        } else if (activation == Activation.HOLD) {
            if (currentDown && !previousDown) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE, abilityId, forward, strafe, yaw);
            } else if (!currentDown && previousDown) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP, abilityId, forward, strafe, yaw);
            }
        }
    }

    private static void sendInput(boolean goingUp, boolean goingDown, DragonRiderAction action, String abilityName, float forward, float strafe, float yaw) {
        NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                new MessageDragonRideInput(goingUp, goingDown, action, abilityName, forward, strafe, yaw));
    }

    private static void resetStateTracking() {
        wasAscendPressed = false;
        wasAccelerateDown = false;
        wasTertiaryAbilityDown = false;
        wasPrimaryAbilityDown = false;
        wasSecondaryAbilityDown = false;
        wasAttackDown = false;
        lastForward = 0f;
        lastStrafe = 0f;
        lastYaw = 0f;
        lastAscendDown = false;
        lastDescendDown = false;
    }
}
