package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.base.DragonEntity;

/**
 * Handles all keybind and control state logic for dragons
 */
public class DragonKeybindHandler {
    private final DragonEntity dragon;

    private byte controlState = 0;
    
    public DragonKeybindHandler(DragonEntity dragon) {
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
        boolean roar = (controlState & 8) != 0;
        boolean summon = (controlState & 16) != 0;

        dragon.setGoingUp(up);
        dragon.setGoingDown(down);

        // Rising-edge detect abilities while ridden
        boolean prevAttack = (previous & 4) != 0;
        boolean prevRoar = (previous & 8) != 0;
        boolean prevSummon = (previous & 16) != 0;

        if (dragon.getRidingPlayer() != null) {
            var rider = dragon.getRidingPlayer();
            
            // Handle attack keybind
            if (!prevAttack && attack) {
                var hitResult = rider.pick(6.0, 1.0f, false);
                if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.ENTITY) {
                    // Use the wyvern's primary attack ability
                    var chosen = dragon.getPrimaryAttackAbility();

                    if (chosen != null && dragon.combatManager.canStart(chosen)) {
                        // Align wyvern to rider's current view just before triggering the animation
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

            // Handle roar keybind (R)
            if (!prevRoar && roar) {
                var roarAbility = dragon.getRoaringAbility();
                if (roarAbility != null && dragon.combatManager.canStart(roarAbility)) {
                    dragon.combatManager.tryUseAbility(roarAbility);
                }
            }

            // Handle summon storm keybind (H)
            if (!prevSummon && summon) {
                var summonAbility = dragon.getChannelingAbility();
                if (summonAbility != null && dragon.combatManager.canStart(summonAbility)) {
                    dragon.combatManager.tryUseAbility(summonAbility);
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
