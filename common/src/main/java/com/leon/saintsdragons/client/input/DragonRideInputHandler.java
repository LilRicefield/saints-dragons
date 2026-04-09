package com.leon.saintsdragons.client.input;

import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase.RiderAbilityBinding.Activation;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

/**
 * Shared dragon riding input handling used by both Forge and Fabric integrations.
 * Platform layers hook up registration/ticking and delegate here so gameplay stays identical.
 */
public final class DragonRideInputHandler {
    private static final String KEY_CATEGORY = "key.categories.saintsdragons";

    // NOTE: Ascend/Accelerate default to UNKNOWN to avoid conflicts with vanilla Jump/Sprint on Fabric.
    // The handler checks vanilla keybinds (Jump/Shift/Sprint) as fallbacks, so dragon
    // controls work out-of-box. Players can bind these to different keys if desired.
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
    public static final KeyMapping DRAGON_TOGGLE_PITCH_MODE = new KeyMapping(
            "key.saintsdragons.toggle_pitch_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y,
            KEY_CATEGORY
    );
    public static final KeyMapping DRAGON_TAUNT = new KeyMapping(
            "key.saintsdragons.taunt",
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
            DRAGON_TOGGLE_PITCH_MODE,
            DRAGON_TAUNT
    };

    private static boolean wasAscendPressed = false;
    private static boolean wasAccelerateDown = false;
    private static boolean wasTertiaryAbilityDown = false;
    private static boolean wasPrimaryAbilityDown = false;
    private static boolean wasSecondaryAbilityDown = false;
    private static boolean wasAttackDown = false;
    private static boolean wasToggleMeleeDown = false;
    private static boolean wasTogglePitchModeDown = false;
    private static boolean wasTauntDown = false;
    private static int volitansTertiaryHoldTicks = 0;
    private static boolean volitansBreathActive = false;
    private static final int VOLITANS_TERTIARY_HOLD_TICKS = 5;
    private static long volitansPrimaryPressStartedAtMs = 0L;
    private static boolean volitansPoisonBallActive = false;
    private static final long VOLITANS_PRIMARY_HOLD_MS = 220L;
    private static int raevyxSecondaryHoldTicks = 0;
    private static boolean raevyxGroundRendTriggered = false;
    private static final int RAEVYX_SECONDARY_HOLD_TICKS = 6;

    private static float lastForward = 0f;
    private static float lastStrafe = 0f;
    private static float lastYaw = 0f;
    private static boolean lastAscendDown = false;
    private static boolean lastDescendDown = false;

    // Double-tap dodge detection (Raevyx)
    private static long lastLeftTapTime = 0;
    private static long lastRightTapTime = 0;
    private static boolean wasLeftKeyDown = false;
    private static boolean wasRightKeyDown = false;

    // Double-tap bulldoze detection (Ignivorus)
    private static long lastForwardTapTime = 0;
    private static boolean wasForwardKeyDown = false;

    // Double-tap Phase 2 detection (Ignivorus)
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
            // Ensure Minecraft rebuilds its key->binding lookup so our keys respond immediately on Fabric.
            KeyMapping.resetMapping();
        }
    }

    public static void clientTick() {
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

        handleControls(mc, player, dragon);
    }

    private static void handleControls(Minecraft mc, LocalPlayer player, RideableDragonBase dragon) {
        // CLIENT-SIDE LOCK CHECK: Block all input if controls are locked
        // This prevents client-side prediction from moving the dragon before server can block it
        if (dragon.areRiderControlsLocked()) {
            handleLockedInputs(mc, dragon);
            return;
        }

        boolean ascendDown = DRAGON_ASCEND.isDown() || mc.options.keyJump.isDown();
        boolean descendDown = DRAGON_DESCEND.isDown() || mc.options.keyShift.isDown();
        boolean accelerateDown = DRAGON_ACCELERATE.isDown() || mc.options.keySprint.isDown();
        boolean tertiaryDown = DRAGON_TERTIARY_ABILITY.isDown();
        boolean primaryDown = DRAGON_PRIMARY_ABILITY.isDown();
        boolean secondaryDown = DRAGON_SECONDARY_ABILITY.isDown();
        boolean toggleMeleeDown = DRAGON_TOGGLE_MELEE.isDown();
        boolean togglePitchModeDown = DRAGON_TOGGLE_PITCH_MODE.isDown();
        boolean tauntDown = DRAGON_TAUNT.isDown() && isCtrlDown(mc);
        boolean attackDown = mc.options.keyAttack.isDown();

        float forward = player.zza;
        float strafe = player.xxa;
        float yaw = player.getYRot();

        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut) {
            if (mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen) {
                mc.setScreen(null);
                sendInput(false, false, DragonRiderAction.OPEN_INVENTORY, null, forward, strafe, yaw);
                return;
            }
            if (mc.screen == null && mc.options.keyInventory.consumeClick()) {
                sendInput(false, false, DragonRiderAction.OPEN_INVENTORY, null, forward, strafe, yaw);
                return;
            }
        }

        boolean movementChanged = forward != lastForward
                || strafe != lastStrafe
                || Math.abs(yaw - lastYaw) > 0.1f
                || ascendDown != lastAscendDown
                || descendDown != lastDescendDown;

        if (movementChanged) {
            sendInput(ascendDown, descendDown, DragonRiderAction.NONE, null, forward, strafe, yaw);
            lastForward = forward;
            lastStrafe = strafe;
            lastYaw = yaw;
            lastAscendDown = ascendDown;
            lastDescendDown = descendDown;
        }

        if (accelerateDown != wasAccelerateDown) {
            DragonRiderAction action = accelerateDown
                    ? DragonRiderAction.ACCELERATE
                    : DragonRiderAction.STOP_ACCELERATE;
            sendInput(ascendDown, descendDown, action, null, forward, strafe, yaw);
        }

        // Only send takeoff request for dragons that can fly
        if (ascendDown && !wasAscendPressed) {
            boolean canTakeoffNow = dragon.canTakeoff();
            boolean alreadyFlying = dragon.isFlying();
            boolean breachWaterBypass =
                    (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx
                    || dragon instanceof com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane
                    || dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus)
                    && dragon.isInWaterOrBubble()
                    && !dragon.isUnderWater()
                    && !alreadyFlying;
            boolean raevyxFallRecoveryBypass =
                    dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx
                    && !alreadyFlying
                    && !dragon.onGround()
                    && !dragon.isInWaterOrBubble()
                    && !dragon.isInLava()
                    && (dragon.fallDistance >= 1.0F || dragon.getDeltaMovement().y <= -0.02D);
            if ((!alreadyFlying && canTakeoffNow) || breachWaterBypass || raevyxFallRecoveryBypass) {
                // Preserve the current ascend/descend state so the server keeps Space latched on takeoff
                sendInput(ascendDown, descendDown, DragonRiderAction.TAKEOFF_REQUEST, null, forward, strafe, yaw);
            }
        }

        if (toggleMeleeDown && !wasToggleMeleeDown) {
            // Volitans breath mode switch uses X while actively breathing; skip melee UI/messages.
            if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans && volitansBreathActive) {
                sendInput(false, false, DragonRiderAction.TOGGLE_MELEE, null, forward, strafe, yaw);
            } else
            if (dragon.hasSecondaryMelee()) {
                sendInput(false, false, DragonRiderAction.TOGGLE_MELEE, null, forward, strafe, yaw);

                int newMode = (dragon.getMeleeMode() + 1) % 2;
                DragonUIRegistry.getMeleeModeNotification()
                        .showNotification(newMode);
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("saintsdragons.message.no_secondary_melee"),
                        true
                );
            }
        }
        if (togglePitchModeDown && !wasTogglePitchModeDown
                && (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx
                || dragon instanceof com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane
                || dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus
                || dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans
                || dragon instanceof Varasuchus)) {
            sendInput(false, false, DragonRiderAction.TOGGLE_PITCH_MODE, null, forward, strafe, yaw);
        }
        if (tauntDown && !wasTauntDown) {
            sendInput(false, false, DragonRiderAction.TAUNT, null, forward, strafe, yaw);
        }

        // Double-tap dodge detection (Raevyx + Volitans)
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx
                || dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            boolean leftDown = mc.options.keyLeft.isDown();
            boolean rightDown = mc.options.keyRight.isDown();
            long currentTime = System.currentTimeMillis();

            // Detect left dodge (double-tap A)
            if (leftDown && !wasLeftKeyDown) {
                if (currentTime - lastLeftTapTime < DOUBLE_TAP_WINDOW_MS) {
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_A, null, forward, strafe, yaw);
                }
                lastLeftTapTime = currentTime;
            }

            // Detect right dodge (double-tap D)
            if (rightDown && !wasRightKeyDown) {
                if (currentTime - lastRightTapTime < DOUBLE_TAP_WINDOW_MS) {
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_D, null, forward, strafe, yaw);
                }
                lastRightTapTime = currentTime;
            }

            wasLeftKeyDown = leftDown;
            wasRightKeyDown = rightDown;
        }

        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus ||
            dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx ||
            dragon instanceof Varasuchus ||
            dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            boolean forwardDown = mc.options.keyUp.isDown();
            long currentTime = System.currentTimeMillis();

            // Detect double-tap W
            if (forwardDown && !wasForwardKeyDown) {
                if (currentTime - lastForwardTapTime < DOUBLE_TAP_WINDOW_MS) {
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_W, null, forward, strafe, yaw);
                }
                lastForwardTapTime = currentTime;
            }

            wasForwardKeyDown = forwardDown;
        }

        // Double-tap S detection (Ignivorus = Phase 2 toggle, Raevyx/Volitans = backward dash)
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus ||
            dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx ||
            dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            boolean backwardDown = mc.options.keyDown.isDown();
            long currentTime = System.currentTimeMillis();

            // Detect double-tap S
            if (backwardDown && !wasBackwardKeyDown) {
                if (currentTime - lastBackwardTapTime < DOUBLE_TAP_WINDOW_MS) {
                    sendInput(ascendDown, descendDown, DragonRiderAction.DOUBLE_TAP_S, null, forward, strafe, yaw);
                }
                lastBackwardTapTime = currentTime;
            }

            wasBackwardKeyDown = backwardDown;
        }

        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            handleVolitansDualTertiary(tertiaryDown, wasTertiaryAbilityDown, forward, strafe, yaw);
        } else {
            handleAbilityBinding(dragon.getTertiaryRiderAbility(), tertiaryDown, wasTertiaryAbilityDown, forward, strafe, yaw);
        }
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            handleVolitansDualPrimary(primaryDown, wasPrimaryAbilityDown, forward, strafe, yaw);
        } else {
            handleAbilityBinding(dragon.getPrimaryRiderAbility(), primaryDown, wasPrimaryAbilityDown, forward, strafe, yaw);
        }
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx) {
            handleRaevyxDualSecondary(secondaryDown, wasSecondaryAbilityDown, forward, strafe, yaw);
        } else {
            handleAbilityBinding(dragon.getSecondaryRiderAbility(), secondaryDown, wasSecondaryAbilityDown, forward, strafe, yaw);
        }
        handleAbilityBinding(dragon.getAttackRiderAbility(), attackDown, wasAttackDown, forward, strafe, yaw);

        wasAscendPressed = ascendDown;
        wasAccelerateDown = accelerateDown;
        wasTertiaryAbilityDown = tertiaryDown;
        wasPrimaryAbilityDown = primaryDown;
        wasSecondaryAbilityDown = secondaryDown;
        wasAttackDown = attackDown;
        wasToggleMeleeDown = toggleMeleeDown;
        wasTogglePitchModeDown = togglePitchModeDown;
        wasTauntDown = tauntDown;
    }

    private static void handleAbilityBinding(RiderAbilityBinding binding,
                                             boolean currentDown,
                                             boolean previousDown,
                                             float forward,
                                             float strafe,
                                             float yaw) {
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

    private static void handleVolitansDualTertiary(boolean currentDown,
                                                    boolean previousDown,
                                                    float forward,
                                                    float strafe,
                                                    float yaw) {
        if (currentDown) {
            if (!previousDown) {
                volitansTertiaryHoldTicks = 0;
                volitansBreathActive = false;
            }
            volitansTertiaryHoldTicks++;
            if (!volitansBreathActive && volitansTertiaryHoldTicks >= VOLITANS_TERTIARY_HOLD_TICKS) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE,
                        VolitansAbilities.VOLITANS_BREATH_ID, forward, strafe, yaw);
                volitansBreathActive = true;
            }
            return;
        }

        if (previousDown) {
            if (volitansBreathActive) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP,
                        VolitansAbilities.VOLITANS_BREATH_ID, forward, strafe, yaw);
            } else {
                sendInput(false, false, DragonRiderAction.ABILITY_USE,
                        VolitansAbilities.VOLITANS_CLAW_ID, forward, strafe, yaw);
            }
        }
        volitansTertiaryHoldTicks = 0;
        volitansBreathActive = false;
    }

    private static void handleVolitansDualPrimary(boolean currentDown,
                                                  boolean previousDown,
                                                  float forward,
                                                  float strafe,
                                                  float yaw) {
        long now = System.currentTimeMillis();
        if (currentDown) {
            if (!previousDown) {
                volitansPrimaryPressStartedAtMs = now;
                volitansPoisonBallActive = false;
            }
            if (!volitansPoisonBallActive
                    && volitansPrimaryPressStartedAtMs > 0L
                    && now - volitansPrimaryPressStartedAtMs >= VOLITANS_PRIMARY_HOLD_MS) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE,
                        VolitansAbilities.VOLITANS_POISON_BALL_ID, forward, strafe, yaw);
                volitansPoisonBallActive = true;
            }
            return;
        }

        if (previousDown) {
            if (volitansPoisonBallActive) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP,
                        VolitansAbilities.VOLITANS_POISON_BALL_ID, forward, strafe, yaw);
            } else {
                long heldMs = volitansPrimaryPressStartedAtMs > 0L ? now - volitansPrimaryPressStartedAtMs : 0L;
                if (heldMs >= VOLITANS_PRIMARY_HOLD_MS) {
                    // Fallback: if a late frame missed arming while held, still treat as hold-release shoot.
                    sendInput(false, false, DragonRiderAction.ABILITY_USE,
                            VolitansAbilities.VOLITANS_POISON_BALL_ID, forward, strafe, yaw);
                    sendInput(false, false, DragonRiderAction.ABILITY_STOP,
                            VolitansAbilities.VOLITANS_POISON_BALL_ID, forward, strafe, yaw);
                } else {
                    sendInput(false, false, DragonRiderAction.ABILITY_USE,
                            VolitansAbilities.VOLITANS_ROAR_ID, forward, strafe, yaw);
                }
            }
        }

        volitansPrimaryPressStartedAtMs = 0L;
        volitansPoisonBallActive = false;
    }

    private static void handleRaevyxDualSecondary(boolean currentDown,
                                                  boolean previousDown,
                                                  float forward,
                                                  float strafe,
                                                  float yaw) {
        if (currentDown) {
            if (!previousDown) {
                raevyxSecondaryHoldTicks = 0;
                raevyxGroundRendTriggered = false;
            }
            raevyxSecondaryHoldTicks++;
            if (!raevyxGroundRendTriggered && raevyxSecondaryHoldTicks >= RAEVYX_SECONDARY_HOLD_TICKS) {
                sendInput(false, false, DragonRiderAction.ABILITY_USE,
                        RaevyxAbilities.RAEVYX_GROUND_REND.getName(), forward, strafe, yaw);
                raevyxGroundRendTriggered = true;
            }
            return;
        }

        if (previousDown && !raevyxGroundRendTriggered) {
            sendInput(false, false, DragonRiderAction.ABILITY_USE,
                    RaevyxAbilities.RAEVYX_SUMMON_STORM.getName(), forward, strafe, yaw);
        }

        raevyxSecondaryHoldTicks = 0;
        raevyxGroundRendTriggered = false;
    }

    private static void handleLockedInputs(Minecraft mc, RideableDragonBase dragon) {
        boolean tertiaryDown = DRAGON_TERTIARY_ABILITY.isDown();
        boolean primaryDown = DRAGON_PRIMARY_ABILITY.isDown();
        boolean secondaryDown = DRAGON_SECONDARY_ABILITY.isDown();
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean toggleMeleeDown = DRAGON_TOGGLE_MELEE.isDown();
        boolean togglePitchModeDown = DRAGON_TOGGLE_PITCH_MODE.isDown();
        boolean tauntDown = DRAGON_TAUNT.isDown() && isCtrlDown(mc);

        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.volitans.Volitans) {
            if (volitansBreathActive) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP, VolitansAbilities.VOLITANS_BREATH_ID, 0f, 0f, 0f);
            }
            if (volitansPoisonBallActive) {
                sendInput(false, false, DragonRiderAction.ABILITY_STOP, VolitansAbilities.VOLITANS_POISON_BALL_ID, 0f, 0f, 0f);
            }
            volitansTertiaryHoldTicks = 0;
            volitansBreathActive = false;
            volitansPrimaryPressStartedAtMs = 0L;
            volitansPoisonBallActive = false;
        } else {
            handleLockedAbilityRelease(dragon.getTertiaryRiderAbility(), tertiaryDown, wasTertiaryAbilityDown);
        }
        if (dragon instanceof com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx) {
            raevyxSecondaryHoldTicks = 0;
            raevyxGroundRendTriggered = false;
        }
        handleLockedAbilityRelease(dragon.getPrimaryRiderAbility(), primaryDown, wasPrimaryAbilityDown);
        handleLockedAbilityRelease(dragon.getSecondaryRiderAbility(), secondaryDown, wasSecondaryAbilityDown);
        handleLockedAbilityRelease(dragon.getAttackRiderAbility(), attackDown, wasAttackDown);

        // Reset movement/double-tap tracking, but keep ability key states to avoid re-trigger on unlock.
        resetStateTracking();
        wasTertiaryAbilityDown = tertiaryDown;
        wasPrimaryAbilityDown = primaryDown;
        wasSecondaryAbilityDown = secondaryDown;
        wasAttackDown = attackDown;
        wasToggleMeleeDown = toggleMeleeDown;
        wasTogglePitchModeDown = togglePitchModeDown;
        wasTauntDown = tauntDown;
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
            sendInput(false, false, DragonRiderAction.ABILITY_STOP, abilityId, 0f, 0f, 0f);
        }
    }

    private static void sendInput(boolean goingUp,
                                  boolean goingDown,
                                  DragonRiderAction action,
                                  String abilityName,
                                  float forward,
                                  float strafe,
                                  float yaw) {
        NetworkHandler.sendToServer(new MessageDragonRideInput(
                goingUp,
                goingDown,
                action,
                abilityName,
                forward,
                strafe,
                yaw
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
        wasTogglePitchModeDown = false;
        wasTauntDown = false;
        volitansPrimaryPressStartedAtMs = 0L;
        volitansPoisonBallActive = false;
        lastForward = 0f;
        lastStrafe = 0f;
        lastYaw = 0f;
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
        volitansTertiaryHoldTicks = 0;
        volitansBreathActive = false;
        raevyxSecondaryHoldTicks = 0;
        raevyxGroundRendTriggered = false;
    }

    private static boolean isCtrlDown(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
    }
}
