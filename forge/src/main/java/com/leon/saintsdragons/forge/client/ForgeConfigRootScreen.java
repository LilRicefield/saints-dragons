package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.client.ui.config.ConfigNavigationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeConfigRootScreen extends ConfigNavigationScreen {
    public ForgeConfigRootScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.title"), List.of(
                new Destination(
                        Component.translatable("saintsdragons.config_screen.client"),
                        ForgeConfigRootScreen::createClientMenu
                ),
                new Destination(
                        Component.translatable("saintsdragons.config_screen.server"),
                        ForgeConfigRootScreen::createServerMenu,
                        () -> !isRemoteServerSession(),
                        Component.translatable("saintsdragons.config_screen.server.remote_disabled")
                )
        ));
    }

    private static Screen createClientMenu(Screen parent) {
        return new ConfigNavigationScreen(parent,
                Component.translatable("saintsdragons.config_screen.client"),
                List.of(
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.client_common"),
                                ForgeClientCommonScreen::new
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.dragon_rider_camera"),
                                ForgeDragonRiderCameraScreen::new
                        )
                ));
    }

    private static Screen createServerMenu(Screen parent) {
        return new ConfigNavigationScreen(parent,
                Component.translatable("saintsdragons.config_screen.server"),
                List.of(
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.server_common"),
                                ForgeConfigRootScreen::createServerCommonMenu
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.attributes"),
                                ForgeDragonAttributesScreen::new
                        )
                ));
    }

    private static Screen createServerCommonMenu(Screen parent) {
        return new ConfigNavigationScreen(parent,
                Component.translatable("saintsdragons.config_screen.server_common"),
                List.of(
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.gameplay"),
                                ForgeServerGameplayScreen::new
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.dragon_needs"),
                                ForgeDragonNeedsScreen::new
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.spawning"),
                                ForgeDragonSpawningScreen::new
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.tools_armor"),
                                ForgeToolsArmorScreen::new
                        ),
                        new Destination(
                                Component.translatable("saintsdragons.config_screen.npcs"),
                                ForgeNpcSettingsScreen::new
                        )
                ));
    }

    private static boolean isRemoteServerSession() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.getSingleplayerServer() == null;
    }
}
