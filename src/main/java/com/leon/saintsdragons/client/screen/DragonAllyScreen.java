package com.leon.saintsdragons.client.screen;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.network.MessageDragonAllyManagement;
import com.leon.saintsdragons.common.network.MessageDragonAllyRequest;
import com.leon.saintsdragons.common.network.ModNetworkHandler;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * GUI screen for managing dragon allies.
 * Allows adding/removing allies by username with validation.
 */
public class DragonAllyScreen extends Screen {
    private final DragonEntity dragon;

    // GUI dimensions - adjust to match Minecraft's GUI scaling
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    
    // Calculate actual GUI size based on screen scaling
    private int getActualGuiWidth() {
        return Math.min(GUI_WIDTH, this.width - 40); // Leave 20px margin on each side
    }
    
    private int getActualGuiHeight() {
        return Math.min(GUI_HEIGHT, this.height - 40); // Leave 20px margin on top/bottom
    }
    private int leftPos;
    private int topPos;
    
    // Components
    private EditBox usernameInput;

    // Ally list display
    private List<String> allyList;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ALLIES = 8;
    
    public DragonAllyScreen(DragonEntity dragon) {
        super(Component.translatable("saintsdragons.gui.dragon_ally.title"));
        this.dragon = dragon;
        this.allyList = new java.util.ArrayList<>(); // Start empty, will be populated by server
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Center the GUI, but ensure it fits on screen
        int actualWidth = getActualGuiWidth();
        int actualHeight = getActualGuiHeight();
        
        this.leftPos = Math.max(0, (this.width - actualWidth) / 2);
        this.topPos = Math.max(0, (this.height - actualHeight) / 2);
        
        // Ensure GUI doesn't go off the bottom of the screen
        if (this.topPos + actualHeight > this.height - 20) { // Leave 20px margin from bottom
            this.topPos = this.height - actualHeight - 20; // Leave 20px margin from bottom
        }
        
        // Debug: Log GUI positioning (only once)
        SaintsDragons.LOGGER.info("GUI positioning - Screen: {}x{}, GUI: {}x{}, Position: ({}, {})", 
            this.width, this.height, GUI_WIDTH, GUI_HEIGHT, this.leftPos, this.topPos);
        
        // Request current ally list from server
        requestAllyListFromServer();
        
        // Username input field
        this.usernameInput = new EditBox(this.font, leftPos + 20, topPos + 30, 150, 20, 
            Component.translatable("saintsdragons.gui.dragon_ally.username_input"));
        this.usernameInput.setMaxLength(16); // Minecraft username limit
        this.addRenderableWidget(usernameInput);
        
        // Add ally button
        Button addButton = Button.builder(
                Component.translatable("saintsdragons.gui.dragon_ally.add"),
                button -> addAlly()
        ).bounds(leftPos + 180, topPos + 30, 60, 20).build();
        this.addRenderableWidget(addButton);
        
        // Remove ally button
        Button removeButton = Button.builder(
                Component.translatable("saintsdragons.gui.dragon_ally.remove"),
                button -> removeAlly()
        ).bounds(leftPos + 180, topPos + 55, 60, 20).build();
        this.addRenderableWidget(removeButton);
        
        // Close button - moved up to avoid covering inner border
        Button closeButton = Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(leftPos + GUI_WIDTH - 60, topPos + GUI_HEIGHT - 50, 50, 20).build();
        this.addRenderableWidget(closeButton);
        
        // Set initial focus
        this.setInitialFocus(usernameInput);
    }
    
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Use the reliable programmatic drawing system - it works perfectly!
        int actualWidth = getActualGuiWidth();
        int actualHeight = getActualGuiHeight();
        
        // Draw beautiful gold-bordered GUI
        guiGraphics.fill(leftPos, topPos, leftPos + actualWidth, topPos + actualHeight, 0x80000000); // Semi-transparent black background
        guiGraphics.fill(leftPos, topPos, leftPos + actualWidth, topPos + 1, 0xFFD4AF37); // Top border (gold)
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + actualHeight, 0xFFD4AF37); // Left border (gold)
        guiGraphics.fill(leftPos + actualWidth - 1, topPos, leftPos + actualWidth, topPos + actualHeight, 0xFFD4AF37); // Right border (gold)
        guiGraphics.fill(leftPos, topPos + actualHeight - 1, leftPos + actualWidth, topPos + actualHeight, 0xFFD4AF37); // Bottom border (gold)
        
        // Add some decorative elements to make it look more medieval/fantasy
        guiGraphics.fill(leftPos + 10, topPos + 10, leftPos + actualWidth - 10, topPos + 12, 0xFFB8860B); // Inner top border (darker gold)
        guiGraphics.fill(leftPos + 10, topPos + actualHeight - 12, leftPos + actualWidth - 10, topPos + actualHeight - 10, 0xFFB8860B); // Inner bottom border (darker gold)
        
        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, 
            leftPos + GUI_WIDTH / 2, topPos + 16, 0x404040);
        
        // Draw ally count
        String allyCountText = Component.translatable("saintsdragons.gui.dragon_ally.count", 
            allyList.size(), dragon.allyManager.getMaxAllies()).getString();
        guiGraphics.drawString(this.font, allyCountText, leftPos + 20, topPos + 80, 0x404040);
        
        // Draw ally list
        drawAllyList(guiGraphics);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void drawAllyList(GuiGraphics guiGraphics) {
        int startY = topPos + 100;
        int endY = topPos + GUI_HEIGHT - 40;
        int visibleCount = Math.min(MAX_VISIBLE_ALLIES, allyList.size() - scrollOffset);
        
        // Draw ally entries
        for (int i = 0; i < visibleCount; i++) {
            int index = i + scrollOffset;
            if (index >= allyList.size()) break;
            
            String allyName = allyList.get(index);
            int y = startY + (i * 12);
            
            // Highlight if mouse is over this entry (mouseX and mouseY are parameters from render method)
            // TODO: Implement mouse hover detection

            int color = 0x404040;
            guiGraphics.drawString(this.font, allyName, leftPos + 20, y, color);
        }
        
        // Draw scroll indicators if needed
        if (allyList.size() > MAX_VISIBLE_ALLIES) {
            if (scrollOffset > 0) {
                guiGraphics.drawString(this.font, "↑", leftPos + GUI_WIDTH - 20, startY, 0x404040);
            }
            if (scrollOffset + MAX_VISIBLE_ALLIES < allyList.size()) {
                guiGraphics.drawString(this.font, "↓", leftPos + GUI_WIDTH - 20, endY - 12, 0x404040);
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (allyList.size() > MAX_VISIBLE_ALLIES) {
            if (delta < 0 && scrollOffset < allyList.size() - MAX_VISIBLE_ALLIES) {
                scrollOffset++;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset--;
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle ally list clicking for removal
        if (button == 0) { // Left click
            int startY = topPos + 100;
            for (int i = 0; i < Math.min(MAX_VISIBLE_ALLIES, allyList.size() - scrollOffset); i++) {
                int index = i + scrollOffset;
                if (index >= allyList.size()) break;
                
                int y = startY + (i * 12);
                if (mouseX >= leftPos + 20 && mouseX <= leftPos + GUI_WIDTH - 40 &&
                    mouseY >= y && mouseY < y + 12) {
                    
                    String allyName = allyList.get(index);
                    usernameInput.setValue(allyName);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void addAlly() {
        String username = usernameInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        
        // Send add ally message to server
        ModNetworkHandler.sendToServer(new MessageDragonAllyManagement(
            dragon.getId(), MessageDragonAllyManagement.Action.ADD, username));
        
        usernameInput.setValue("");
    }
    
    private void removeAlly() {
        String username = usernameInput.getValue().trim();
        if (username.isEmpty()) {
            return;
        }
        
        // Send remove ally message to server
        ModNetworkHandler.sendToServer(new MessageDragonAllyManagement(
            dragon.getId(), MessageDragonAllyManagement.Action.REMOVE, username));
        
        usernameInput.setValue("");
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // Enter key to add ally
        if (keyCode == 257) { // Enter
            addAlly();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Request the current ally list from the server
     */
    private void requestAllyListFromServer() {
        // Send a request to get the current ally list
        ModNetworkHandler.sendToServer(new MessageDragonAllyRequest(dragon.getId()));
    }
    
    /**
     * Update the ally list (called from network handler)
     */
    public void updateAllyList(List<String> newAllyList) {
        this.allyList = newAllyList;
        this.scrollOffset = Math.min(scrollOffset, Math.max(0, allyList.size() - MAX_VISIBLE_ALLIES));
    }
}
