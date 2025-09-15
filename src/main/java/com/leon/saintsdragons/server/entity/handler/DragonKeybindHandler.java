package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

/**
 * Handles all keybind and control state logic for the Lightning Dragon
 */
public class DragonKeybindHandler {
    private final LightningDragonEntity dragon;

    private byte controlState = 0;
    
    public DragonKeybindHandler(LightningDragonEntity dragon) {
        this.dragon = dragon;
    }
    
    // ===== CONTROL STATE SYSTEM =====
    /**
     * Main control state handler - processes keybind input and triggers abilities
     * This is the core method that handles Y key press/release for Enhanced Lightning Beam
     */
    public void setControlState(byte controlState) {
        byte previous = this.controlState;
        this.controlState = controlState;

        // Extract individual control states from bitfield
        boolean up = (controlState & 1) != 0;
        boolean down = (controlState & 2) != 0;
        boolean attack = (controlState & 4) != 0;

        dragon.setGoingUp(up);
        dragon.setGoingDown(down);
        // (Drop mirrored rider-attacking flag; not used elsewhere)

        // Rising-edge detect attack while ridden; entity-clicks are handled by DragonAttackEventHandler
        boolean prevAttack = (previous & 4) != 0;
        if (!prevAttack && attack && dragon.getRidingPlayer() != null) {
            var rider = dragon.getRidingPlayer();
            var hitResult = rider.pick(6.0, 1.0f, false);
            if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.ENTITY) {
                // Decide which melee to use: prefer bite while flying; randomize on ground
                boolean useBite = dragon.isFlying() || dragon.getRandom().nextBoolean();
                var chosen = useBite ? ModAbilities.BITE : ModAbilities.HORN_GORE;

                // Only align + trigger when the ability can actually start (cooldowns ready)
                if (dragon.combatManager.canStart(chosen)) {
                    // Align dragon to rider's current view just before triggering the animation
                    float yaw = rider.getYRot();
                    float pitch = rider.getXRot();
                    if (pitch > 35f) pitch = 35f;
                    if (pitch < -35f) pitch = -35f;
                    dragon.setYRot(yaw);
                    dragon.yBodyRot = yaw;
                    dragon.yHeadRot = yaw;
                    dragon.setXRot(pitch);

                    dragon.combatManager.tryUseAbility(chosen);
                }
            }
        }
    }
    
    private void setStateField(int bit, boolean value) {
        if (value) {
            controlState |= (byte) (1 << bit);
        } else {
            controlState &= (byte) ~(1 << bit);
        }
    }
    
    public void up(boolean up) {
        setStateField(0, up);
    }
    
    public void down(boolean down) {
        setStateField(1, down);
    }
}
