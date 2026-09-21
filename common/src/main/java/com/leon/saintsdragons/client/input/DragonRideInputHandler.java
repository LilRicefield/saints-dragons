package com.leon.saintsdragons.client.input;

import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import com.leon.saintsdragons.common.network.MessageBloodTempestKatanaAbility;
import com.leon.saintsdragons.common.network.MessageDragonlordSwordAbility;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderDualAbilityBinding;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding.Activation;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonChestCarrier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import java.util.function.Consumer;

public final class DragonRideInputHandler {
    private static final String KEY_CATEGORY = "key.categories.saintsdragons";
    public static final KeyMapping DRAGON_ASCEND = new KeyMapping(
            "key.saintsdragons.ascend",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_DESCEND = new KeyMapping(
            "key.saintsdragons.descend",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_ACCELERATE = new KeyMapping(
            "key.saintsdragons.accelerate",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_TERTIARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_tertiary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_PRIMARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_primary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_SECONDARY_ABILITY = new KeyMapping(
            "key.saintsdragons.ability_secondary",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_TOGGLE_MELEE = new KeyMapping(
            "key.saintsdragons.toggle_melee",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            KEY_CATEGORY
    );

    public static final KeyMapping DRAGON_FLEX = new ControlChordKeyMapping(
            "key.saintsdragons.flex",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_4,
            KEY_CATEGORY
    );

    private static final KeyMapping[] ALL_KEYS = {
            DRAGON_ASCEND,
            DRAGON_DESCEND,
            DRAGON_ACCELERATE,
            DRAGON_TERTIARY_ABILITY,
            DRAGON_PRIMARY_ABILITY,
            DRAGON_SECONDARY_ABILITY,
            DRAGON_TOGGLE_MELEE,
            DRAGON_FLEX
    };

    private static boolean wasAscendPressed = false;
    private static boolean wasAccelerateDown = false;
    private static boolean wasTertiaryAbilityDown = false;
    private static boolean wasPrimaryAbilityDown = false;
    private static boolean wasSecondaryAbilityDown = false;
    private static boolean wasAttackDown = false;
    private static boolean wasToggleMeleeDown = false;
    private static boolean wasPitchLockDown = false;
    private static boolean wasFlexDown = false;
    private static boolean wasHeldAbilityDown = false;
    private static final DualAbilityState primaryDualState = new DualAbilityState();
    private static final DualAbilityState secondaryDualState = new DualAbilityState();
    private static final DualAbilityState tertiaryDualState = new DualAbilityState();
    private static RideableDragonBase lastControlledDragon;
    private static float lastForward = 0f;
    private static float lastStrafe = 0f;
    private static boolean lastAscendDown = false;
    private static boolean lastDescendDown = false;
    private static long lastLeftTapTime = 0;
    private static long lastRightTapTime = 0;
    private static boolean wasLeftKeyDown = false;
    private static boolean wasRightKeyDown = false;
    private static long lastForwardTapTime = 0;
    private static boolean wasForwardKeyDown = false;
    private static long lastBackwardTapTime = 0;
    private static boolean wasBackwardKeyDown = false;
    private static final long DOUBLE_TAP_WINDOW_MS = 300;

    private DragonRideInputHandler() {
    }

    public static void registerKeys(Consumer<KeyMapping> registrar, boolean rebuildMappings) {
        for (KeyMapping mapping : ALL_KEYS) {
            registrar.accept(mapping);
        }
        if (rebuildMappings) {
            KeyMapping.resetMapping();
        }
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            lastControlledDragon = null;
            wasHeldAbilityDown = false;
            resetStateTracking();
            return;
        }

        Entity vehicle = player.getVehicle();
        boolean heldAbilityDown = DRAGON_PRIMARY_ABILITY.isDown();
        if (!(vehicle instanceof RideableDragonBase dragon) || !dragon.canBeControlledBy(player)) {
            lastControlledDragon = null;
            if (mc.screen == null
                    && heldAbilityDown
                    && !wasHeldAbilityDown) {
                if (player.getMainHandItem().is(ModItems.DRAGONLORD_SWORD.get())) {
                    NetworkHandler.sendToServer(MessageDragonlordSwordAbility.INSTANCE);
                } else if (player.getMainHandItem().is(ModItems.BLOOD_TEMPEST_KATANA.get())) {
                    NetworkHandler.sendToServer(MessageBloodTempestKatanaAbility.INSTANCE);
                }
            }
            wasHeldAbilityDown = heldAbilityDown;
            resetStateTracking();
            return;
        }

        if (lastControlledDragon != dragon) {
            resetStateTracking();
            lastControlledDragon = dragon;
        }
        wasHeldAbilityDown = heldAbilityDown;
        handleControls(mc, player, dragon);
    }

    private static void handleControls(Minecraft mc, LocalPlayer player, RideableDragonBase dragon) {
        if (tryOpenDragonInventory(mc, dragon, player.zza, player.xxa)) {
            return;
        }
        if (dragon.areRiderControlsLocked()) {
            handleLockedInputs(mc, dragon);
            return;
        }
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean groundDragon = dragon instanceof RideableGroundDragon;
        boolean groundDragonSwimming = groundDragon && dragon.isInWaterOrBubble();
        boolean ascendDown = DRAGON_ASCEND.isDown() || ((!groundDragon || groundDragonSwimming) && jumpDown);
        boolean descendDown = DRAGON_DESCEND.isDown() || mc.options.keyShift.isDown();
        boolean accelerateDown = DRAGON_ACCELERATE.isDown() || mc.options.keySprint.isDown();
        boolean tertiaryDown = DRAGON_TERTIARY_ABILITY.isDown();
        boolean primaryDown = DRAGON_PRIMARY_ABILITY.isDown();
        boolean secondaryDown = DRAGON_SECONDARY_ABILITY.isDown();
        boolean toggleMeleeDown = DRAGON_TOGGLE_MELEE.isDown();
        boolean pitchLockDown = mc.options.keyUse.isDown();
        boolean flexDown = isFlexDown();
        boolean attackDown = mc.options.keyAttack.isDown();
        float forward = player.zza;
        float strafe = player.xxa;
        boolean movementChanged = forward != lastForward
                || strafe != lastStrafe
                || ascendDown != lastAscendDown
                || descendDown != lastDescendDown;

        if (movementChanged) {
            sendInput(ascendDown, descendDown, DragonRiderAction.NONE, null, forward, strafe);
            lastForward = forward;
            lastStrafe = strafe;
            lastAscendDown = ascendDown;
            lastDescendDown = descendDown;
        }

        if (accelerateDown != wasAccelerateDown) {
            DragonRiderAction action = accelerateDown
                    ? DragonRiderAction.ACCELERATE
                    : DragonRiderAction.STOP_ACCELERATE;
            sendInput(ascendDown, descendDown, action, null, forward, strafe);
        }

        if (ascendDown && !wasAscendPressed) {
            boolean canTakeoffNow = dragon.canTakeoff();
            boolean alreadyFlying = dragon.isFlying();
            boolean breachWaterBypass =
                    dragon.supportsRiderWaterBreach()
                    && dragon.isInWaterOrBubble()
                    && !dragon.isUnderWater()
                    && !alreadyFlying;
            boolean fallRecoveryBypass =
                    dragon instanceof RideableFlyingDragon
                    && !alreadyFlying
                    && !dragon.onGround()
                    && !dragon.isInWaterOrBubble()
                    && !dragon.isInLava()
                    && (dragon.fallDistance >= 1.0F || dragon.getDeltaMovement().y <= -0.02D);
            if ((!alreadyFlying && canTakeoffNow) || breachWaterBypass || fallRecoveryBypass) {
                sendInput(ascendDown, descendDown, DragonRiderAction.TAKEOFF_REQUEST, null, forward, strafe);
            }
        }

        if (toggleMeleeDown && !wasToggleMeleeDown) {
            if (dragon.isRiderMeleeToggleHandledByAbility(primaryDualState.activeAbilityId())
                    || dragon.isRiderMeleeToggleHandledByAbility(secondaryDualState.activeAbilityId())
                    || dragon.isRiderMeleeToggleHandledByAbility(tertiaryDualState.activeAbilityId())) {
                sendInput(false, false, DragonRiderAction.TOGGLE_MELEE, null, forward, strafe);
            } else
            if (dragon.hasSecondaryMelee()) {
                sendInput(false, false, DragonRiderAction.TOGGLE_MELEE, null, forward, strafe);
                DragonUIRegistry.getMeleeModeNotification()
                        .showNotification((dragon.getMeleeMode() + 1) % 2);
            } else {
                player.displayClientMessage(
                        Component.translatable("saintsdragons.message.no_secondary_melee"),
                        true
                );
            }
        }
        if (dragon.isRiderInputEnabled(DragonRiderAction.START_PITCH_MODE) && pitchLockDown != wasPitchLockDown) {
            DragonRiderAction action = pitchLockDown
                    ? DragonRiderAction.START_PITCH_MODE
                    : DragonRiderAction.STOP_PITCH_MODE;
            sendInput(false, false, action, null, forward, strafe);
        }
        if (flexDown && !wasFlexDown && dragon.isRiderInputEnabled(DragonRiderAction.FLEX)) {
            sendInput(false, false, DragonRiderAction.FLEX, null, forward, strafe);
        }

        if (dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_A)
                || dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_D)) {
            boolean leftDown = mc.options.keyLeft.isDown();
            boolean rightDown = mc.options.keyRight.isDown();
            long currentTime = System.currentTimeMillis();
            if (leftDown && !wasLeftKeyDown) {
                if (currentTime - lastLeftTapTime < DOUBLE_TAP_WINDOW_MS
                        && dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_A)) {
                    dragon.onClientRiderAction(DragonRiderAction.DOUBLE_TAP_A);
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_A, null, forward, strafe);
                }
                lastLeftTapTime = currentTime;
            }
            if (rightDown && !wasRightKeyDown) {
                if (currentTime - lastRightTapTime < DOUBLE_TAP_WINDOW_MS
                        && dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_D)) {
                    dragon.onClientRiderAction(DragonRiderAction.DOUBLE_TAP_D);
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_D, null, forward, strafe);
                }
                lastRightTapTime = currentTime;
            }

            wasLeftKeyDown = leftDown;
            wasRightKeyDown = rightDown;
        } else {
            wasLeftKeyDown = false;
            wasRightKeyDown = false;
            lastLeftTapTime = 0L;
            lastRightTapTime = 0L;
        }

        if (dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_W)) {
            boolean forwardDown = mc.options.keyUp.isDown();
            long currentTime = System.currentTimeMillis();
            if (forwardDown && !wasForwardKeyDown) {
                if (currentTime - lastForwardTapTime < DOUBLE_TAP_WINDOW_MS) {
                    dragon.onClientRiderAction(DragonRiderAction.DOUBLE_TAP_W);
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_W, null, forward, strafe);
                }
                lastForwardTapTime = currentTime;
            }

            wasForwardKeyDown = forwardDown;
        } else {
            wasForwardKeyDown = false;
            lastForwardTapTime = 0L;
        }
        if (dragon.isRiderInputEnabled(DragonRiderAction.DOUBLE_TAP_S)) {
            boolean backwardDown = mc.options.keyDown.isDown();
            long currentTime = System.currentTimeMillis();
            if (backwardDown && !wasBackwardKeyDown) {
                if (currentTime - lastBackwardTapTime < DOUBLE_TAP_WINDOW_MS) {
                    dragon.onClientRiderAction(DragonRiderAction.DOUBLE_TAP_S);
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_S, null, forward, strafe);
                }
                lastBackwardTapTime = currentTime;
            }

            wasBackwardKeyDown = backwardDown;
        } else {
            wasBackwardKeyDown = false;
            lastBackwardTapTime = 0L;
        }

        handleAbilityInput(dragon.getTertiaryRiderAbility(), dragon.getTertiaryRiderDualAbility(),
                tertiaryDualState, tertiaryDown, wasTertiaryAbilityDown, forward, strafe);
        handleAbilityInput(dragon.getPrimaryRiderAbility(), dragon.getPrimaryRiderDualAbility(),
                primaryDualState, primaryDown, wasPrimaryAbilityDown, forward, strafe);
        handleAbilityInput(dragon.getSecondaryRiderAbility(), dragon.getSecondaryRiderDualAbility(),
                secondaryDualState, secondaryDown, wasSecondaryAbilityDown, forward, strafe);
        handleAbilityBinding(dragon.getAttackRiderAbility(), attackDown, wasAttackDown, forward, strafe);
        wasAscendPressed = ascendDown;
        wasAccelerateDown = accelerateDown;
        wasTertiaryAbilityDown = tertiaryDown;
        wasPrimaryAbilityDown = primaryDown;
        wasSecondaryAbilityDown = secondaryDown;
        wasAttackDown = attackDown;
        wasToggleMeleeDown = toggleMeleeDown;
        wasPitchLockDown = pitchLockDown;
        wasFlexDown = flexDown;
    }

    private static void handleAbilityBinding(RiderAbilityBinding binding,
                                             boolean currentDown,
                                             boolean previousDown,
                                             float forward,
                                             float strafe) {
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
                sendInput(false, false, DragonRiderAction.ABILITY_USE, abilityId, forward, strafe);
            }
        } else if (activation == Activation.HOLD) {
            if (currentDown && !previousDown) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE, abilityId, forward, strafe);
            } else if (!currentDown && previousDown) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP, abilityId, forward, strafe);
            }
        }
    }

    private static boolean tryOpenDragonInventory(Minecraft mc, RideableDragonBase dragon, float forward, float strafe) {
        if (!(dragon instanceof DragonChestCarrier)) {
            return false;
        }
        if (mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen) {
            mc.setScreen(null);
            sendInput(false, false, DragonRiderAction.OPEN_INVENTORY, null, forward, strafe);
            return true;
        }
        if (mc.screen == null && mc.options.keyInventory.consumeClick()) {
            sendInput(false, false, DragonRiderAction.OPEN_INVENTORY, null, forward, strafe);
            return true;
        }
        return false;
    }

    private static void handleAbilityInput(RiderAbilityBinding binding, RiderDualAbilityBinding dualBinding,
                                           DualAbilityState state, boolean currentDown, boolean previousDown,
                                           float forward, float strafe) {
        if (dualBinding != null || state.binding != null) {
            state.tick(dualBinding, currentDown, previousDown, forward, strafe);
        } else {
            handleAbilityBinding(binding, currentDown, previousDown, forward, strafe);
        }
    }

    private static final class DualAbilityState {
        private RiderDualAbilityBinding binding;
        private int heldTicks;
        private long pressStartedAt;
        private boolean triggered;

        private void tick(RiderDualAbilityBinding nextBinding, boolean currentDown, boolean previousDown,
                          float forward, float strafe) {
            long now = System.currentTimeMillis();
            if (currentDown && !previousDown) {
                reset();
                binding = nextBinding;
                pressStartedAt = now;
            }
            if (binding == null) {
                return;
            }
            if (currentDown) {
                heldTicks++;
                if (!triggered && reachedThreshold(now)) {
                    sendInput(false, false, DragonRiderAction.ABILITY_USE,
                            binding.holdAbility().abilityId(), forward, strafe);
                    triggered = true;
                }
                return;
            }
            if (previousDown) {
                if (!triggered && reachedThreshold(now)) {
                    sendInput(false, false, DragonRiderAction.ABILITY_USE,
                            binding.holdAbility().abilityId(), forward, strafe);
                    triggered = true;
                }
                if (triggered) {
                    stopHeldAbility(forward, strafe);
                } else {
                    sendInput(false, false, DragonRiderAction.ABILITY_USE, binding.tapAbilityId(), forward, strafe);
                }
            }
            reset();
        }

        private boolean reachedThreshold(long now) {
            return binding.holdTicks() > 0 ? heldTicks >= binding.holdTicks()
                    : now - pressStartedAt >= binding.holdMillis();
        }

        private String activeAbilityId() {
            return binding != null && triggered ? binding.holdAbility().abilityId() : null;
        }

        private void stopHeldAbility(float forward, float strafe) {
            if (binding != null && triggered && binding.holdAbility().activation() == Activation.HOLD) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP,
                        binding.holdAbility().abilityId(), forward, strafe);
            }
        }

        private void cancel() {
            stopHeldAbility(0f, 0f);
            reset();
        }

        private void reset() {
            binding = null;
            heldTicks = 0;
            pressStartedAt = 0L;
            triggered = false;
        }
    }

    private static void handleLockedInputs(Minecraft mc, RideableDragonBase dragon) {
        boolean tertiaryDown = DRAGON_TERTIARY_ABILITY.isDown();
        boolean primaryDown = DRAGON_PRIMARY_ABILITY.isDown();
        boolean secondaryDown = DRAGON_SECONDARY_ABILITY.isDown();
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean toggleMeleeDown = DRAGON_TOGGLE_MELEE.isDown();
        boolean pitchLockDown = mc.options.keyUse.isDown();
        boolean flexDown = isFlexDown();

        primaryDualState.cancel();
        secondaryDualState.cancel();
        tertiaryDualState.cancel();
        handleLockedAbilityRelease(dragon.getTertiaryRiderAbility(), tertiaryDown, wasTertiaryAbilityDown);
        handleLockedAbilityRelease(dragon.getPrimaryRiderAbility(), primaryDown, wasPrimaryAbilityDown);
        handleLockedAbilityRelease(dragon.getSecondaryRiderAbility(), secondaryDown, wasSecondaryAbilityDown);
        handleLockedAbilityRelease(dragon.getAttackRiderAbility(), attackDown, wasAttackDown);
        if (wasPitchLockDown && !pitchLockDown) {
            sendInput(false, false, DragonRiderAction.STOP_PITCH_MODE, null, 0f, 0f);
        }
        resetStateTracking();
        wasTertiaryAbilityDown = tertiaryDown;
        wasPrimaryAbilityDown = primaryDown;
        wasSecondaryAbilityDown = secondaryDown;
        wasAttackDown = attackDown;
        wasToggleMeleeDown = toggleMeleeDown;
        wasPitchLockDown = pitchLockDown;
        wasFlexDown = flexDown;
    }

    private static void handleLockedAbilityRelease(RiderAbilityBinding binding,
                                                   boolean currentDown,
                                                   boolean previousDown) {
        if (binding == null || binding.activation() != Activation.HOLD) {
            return;
        }
        String abilityId = binding.abilityId();
        if (abilityId == null || abilityId.isEmpty()) {
            return;
        }
        if (!currentDown && previousDown) {
            sendInput(false, false, DragonRiderAction.ABILITY_STOP, abilityId, 0f, 0f);
        }
    }

    private static void sendInput(boolean goingUp,
                                  boolean goingDown,
                                  DragonRiderAction action,
                                  String abilityName,
                                  float forward,
                                  float strafe) {
        NetworkHandler.sendToServer(new MessageDragonRideInput(
                goingUp,
                goingDown,
                action,
                abilityName,
                forward,
                strafe
        ));
    }

    private static void resetStateTracking() {
        wasAscendPressed = false;
        wasAccelerateDown = false;
        wasTertiaryAbilityDown = false;
        wasPrimaryAbilityDown = false;
        wasSecondaryAbilityDown = false;
        wasAttackDown = false;
        wasToggleMeleeDown = false;
        wasPitchLockDown = false;
        wasFlexDown = false;
        primaryDualState.reset();
        secondaryDualState.reset();
        tertiaryDualState.reset();
        lastForward = 0f;
        lastStrafe = 0f;
        lastAscendDown = false;
        lastDescendDown = false;
        lastLeftTapTime = 0;
        lastRightTapTime = 0;
        wasLeftKeyDown = false;
        wasRightKeyDown = false;
        lastForwardTapTime = 0;
        wasForwardKeyDown = false;
        lastBackwardTapTime = 0;
        wasBackwardKeyDown = false;
    }

    private static boolean isFlexDown() {
        return DRAGON_FLEX.isDown();
    }
}
