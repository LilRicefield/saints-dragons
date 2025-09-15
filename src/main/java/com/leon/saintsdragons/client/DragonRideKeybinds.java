package com.leon.saintsdragons.client;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonControl;
import com.leon.saintsdragons.common.network.NetworkHandler;
// no client-driven beam/rider anchor sync
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

    // Hold-to-fire Lightning Beam (G)
    public static final KeyMapping DRAGON_BEAM = new KeyMapping(
            "key.saintsdragons.ability3",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.saintsdragons"
    );

    // One-shot Roar (R)
    public static final KeyMapping DRAGON_ROAR = new KeyMapping(
            "key.saintsdragons.roar",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "key.categories.saintsdragons"
    );

    // One-shot Summon Storm (H)
    public static final KeyMapping DRAGON_SUMMON = new KeyMapping(
            "key.saintsdragons.summon_storm",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.saintsdragons"
    );

    
    // State tracking
    private static boolean wasAscendPressed = false;
    private static boolean wasBeamDown = false;
    private static boolean wasRoarDown = false;
    private static boolean wasSummonDown = false;
    // (no debug anchor toggle)
    
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventHandler {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(DRAGON_ASCEND);
            event.register(DRAGON_DESCEND);
            event.register(DRAGON_ACCELERATE);
            event.register(DRAGON_BEAM);
            event.register(DRAGON_ROAR);
            event.register(DRAGON_SUMMON);
            // no debug key
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null || player.getVehicle() == null) return;
        
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof LightningDragonEntity dragon)) return;
        
        if (!dragon.isTame() || !dragon.isOwnedBy(player)) return;

        // Seat syncing disabled; use server-deterministic seat placement
        
        // Handle control state system (Ice & Fire style)
        handleDragonControlState(dragon);
        
        // Handle ascend key both for flying and takeoff from ground
        boolean currentAscend = DRAGON_ASCEND.isDown();
        boolean currentDescend = DRAGON_DESCEND.isDown();
        boolean currentAccelerate = DRAGON_ACCELERATE.isDown();
        boolean beamDown = DRAGON_BEAM.isDown();
        boolean roarDown = DRAGON_ROAR.isDown();
        boolean summonDown = DRAGON_SUMMON.isDown();
        
        // Sample rider movement inputs for server-side ground animation sync
        float fwd = player.zza;
        float str = player.xxa;
        float yaw = player.getYRot();

        if (dragon.isFlying()) {
            // Flying controls - send every tick for responsive flight
            DragonRiderAction action = currentAccelerate ? DragonRiderAction.ACCELERATE : DragonRiderAction.STOP_ACCELERATE;
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(currentAscend, currentDescend, action, null, fwd, str, yaw));
        } else {
            // Ground takeoff - only trigger once per key press
            if (currentAscend && !wasAscendPressed) {
                // Request takeoff from ground
                NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                        new MessageDragonRideInput(false, false, DragonRiderAction.TAKEOFF_REQUEST, null, fwd, str, yaw));
            }
            // Ground acceleration - send every tick to handle both press and release
            DragonRiderAction groundAction = currentAccelerate ? DragonRiderAction.ACCELERATE : DragonRiderAction.STOP_ACCELERATE;
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, groundAction, null, fwd, str, yaw));
            // Also send a lightweight NONE action frame to ensure server always receives current inputs
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, DragonRiderAction.NONE, null, fwd, str, yaw));
        }

        // Handle hold-to-fire beam start/stop (send transitions only)
        // Start on press
        if (beamDown && !wasBeamDown) {
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, DragonRiderAction.ABILITY_USE, "lightning_beam", fwd, str, yaw));
        }
        // Stop on release
        if (!beamDown && wasBeamDown) {
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, DragonRiderAction.ABILITY_STOP, "lightning_beam", fwd, str, yaw));
        }

        // Handle roar as a one-shot on key press
        if (roarDown && !wasRoarDown) {
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, DragonRiderAction.ABILITY_USE, "roar", fwd, str, yaw));
        }

        // Handle summon storm as a one-shot on key press
        if (summonDown && !wasSummonDown) {
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonRideInput(false, false, DragonRiderAction.ABILITY_USE, "summon_storm", fwd, str, yaw));
        }

        
        wasAscendPressed = currentAscend;
        wasBeamDown = beamDown;
        wasRoarDown = roarDown;
        wasSummonDown = summonDown;
    }
    
    /**
     * Handle control state system like Ice & Fire dragons
     */
    private static void handleDragonControlState(LightningDragonEntity dragon) {
        byte controlState = buildControlState();
        byte previousState = dragon.getControlState();
        if (controlState != previousState) {
            NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                    new MessageDragonControl(dragon.getId(), controlState));
            dragon.setControlState(controlState);
        }
    }

    private static byte buildControlState() {
        byte controlState = 0;
        if (DRAGON_ASCEND.isDown()) controlState |= 1;   // Bit 0: Up/ascend
        if (DRAGON_DESCEND.isDown()) controlState |= 2;  // Bit 1: Down/descend

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyAttack.isDown()) controlState |= 4; // Bit 2: Attack
        if (mc.options.keyShift.isDown()) controlState |= 32; // Bit 5: Dismount
        return controlState;
    }
}
