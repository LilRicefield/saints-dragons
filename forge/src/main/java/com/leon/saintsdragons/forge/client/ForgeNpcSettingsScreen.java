package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeNpcSettingsScreen extends ForgePagedConfigScreen {
    public ForgeNpcSettingsScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.npcs"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        SaintsDragonsConfig.bootstrap();
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.ivy")));
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval"),
                SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::get,
                SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::set,
                SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::save
        ));
    }

    @Override
    protected void onSave() {
        // Each common config value saves through its entry.
    }
}
