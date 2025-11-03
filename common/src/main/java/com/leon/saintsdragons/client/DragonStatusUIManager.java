package com.leon.saintsdragons.client;

import com.leon.saintsdragons.client.ui.DragonStatusUI;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Manager for the Dragon Status UI system.
 * Handles UI lifecycle and wyvern detection.
 */
public class DragonStatusUIManager {
    private static DragonStatusUIManager instance;
    private final DragonStatusUI dragonStatusUI;
    private DragonEntity lastRiddenDragon = null;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;
    
    private DragonStatusUIManager() {
        this.dragonStatusUI = new DragonStatusUI();
    }
    
    public static DragonStatusUIManager getInstance() {
        if (instance == null) {
            instance = new DragonStatusUIManager();
        }
        return instance;
    }
    
    /**
     * Update the UI system
     */
    public void update() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        
        // Check for screen size changes
        if (minecraft.getWindow() != null) {
            int currentWidth = minecraft.getWindow().getGuiScaledWidth();
            int currentHeight = minecraft.getWindow().getGuiScaledHeight();
            
            if (currentWidth != lastScreenWidth || currentHeight != lastScreenHeight) {
                lastScreenWidth = currentWidth;
                lastScreenHeight = currentHeight;
                dragonStatusUI.onWindowResize();
            }
        }
        
        Player player = minecraft.player;
        DragonEntity currentDragon = null;
        
        // Check if player is riding a wyvern
        if (player != null && player.getVehicle() instanceof DragonEntity) {
            currentDragon = (DragonEntity) player.getVehicle();
        }
        
        // Update UI if wyvern changed
        if (currentDragon != lastRiddenDragon) {
            dragonStatusUI.updateDragon(currentDragon);
            lastRiddenDragon = currentDragon;
            
            // Auto-hide UI when not riding a wyvern
            if (currentDragon == null && dragonStatusUI.isVisible()) {
                dragonStatusUI.setVisible(false);
            }
        }
    }
    
    /**
     * Get the Dragon Status UI instance
     */
    public DragonStatusUI getDragonStatusUI() {
        return dragonStatusUI;
    }
    
    /**
     * Handle mouse events
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return dragonStatusUI.mouseClicked(mouseX, mouseY, button);
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return dragonStatusUI.mouseReleased(mouseX, mouseY, button);
    }
    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return dragonStatusUI.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
