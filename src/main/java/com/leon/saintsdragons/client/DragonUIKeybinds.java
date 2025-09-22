package com.leon.saintsdragons.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds for Dragon UI system
 */
@Mod.EventBusSubscriber(modid = "saintsdragons", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DragonUIKeybinds {
    public static final KeyMapping TOGGLE_DRAGON_UI = new KeyMapping(
        "key.saintsdragons.toggle_dragon_ui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F4, // F4 key
        "key.categories.saintsdragons"
    );
    
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DRAGON_UI);
    }
    
    /**
     * Handle keybind events
     */
    public static void handleKeybinds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        
        if (TOGGLE_DRAGON_UI.consumeClick()) {
            DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
            if (manager != null) {
                manager.getDragonStatusUI().toggleVisibility();
            }
        }
    }
}
