package com.leon.saintsdragons.client.screen;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.network.MessageDragonAllyManagement;
import com.leon.saintsdragons.common.network.MessageDragonAllyRequest;
import com.leon.saintsdragons.common.network.ModNetworkHandler;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * GUI screen for managing dragon allies.
 * Allows adding/removing allies by username with validation.
 */
public class DragonAllyScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(SaintsDragons.MOD_ID, "textures/gui/dragon_ally_screen.png");
    
    private final DragonEntity dragon;
    private final Player player;
    
    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    private int leftPos;
    private int topPos;
    
    // Components
    private EditBox usernameInput;
    private Button addButton;
    private Button removeButton;
    private Button closeButton;
    
    // Ally list display
    private List<String> allyList;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ALLIES = 8;
    
    public DragonAllyScreen(DragonEntity dragon, Player player) {
        super(Component.translatable("saintsdragons.gui.dragon_ally.title"));
        this.dragon = dragon;
        this.player = player;
        this.allyList = new java.util.ArrayList<>(); // Start empty, will be populated by server
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Center the GUI
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;
        
        // Request current ally list from server
        requestAllyListFromServer();
        
        // Username input field
        this.usernameInput = new EditBox(this.font, leftPos + 20, topPos + 30, 150, 20, 
            Component.translatable("saintsdragons.gui.dragon_ally.username_input"));
        this.usernameInput.setMaxLength(16); // Minecraft username limit
        this.addRenderableWidget(usernameInput);
        
        // Add ally button
        this.addButton = Button.builder(
            Component.translatable("saintsdragons.gui.dragon_ally.add"),
            button -> addAlly()
        ).bounds(leftPos + 180, topPos + 30, 60, 20).build();
        this.addRenderableWidget(addButton);
        
        // Remove ally button
        this.removeButton = Button.builder(
            Component.translatable("saintsdragons.gui.dragon_ally.remove"),
            button -> removeAlly()
        ).bounds(leftPos + 180, topPos + 55, 60, 20).build();
        this.addRenderableWidget(removeButton);
        
        // Close button
        this.closeButton = Button.builder(
            Component.translatable("gui.cancel"),
            button -> onClose()
        ).bounds(leftPos + GUI_WIDTH - 60, topPos + GUI_HEIGHT - 30, 50, 20).build();
        this.addRenderableWidget(closeButton);
        
        // Set initial focus
        this.setInitialFocus(usernameInput);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Draw GUI background
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, 
            leftPos + GUI_WIDTH / 2, topPos + 10, 0x404040);
        
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
            boolean hovered = false; // TODO: Implement mouse hover detection
            
            int color = hovered ? 0xFFFFFF : 0x404040;
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
