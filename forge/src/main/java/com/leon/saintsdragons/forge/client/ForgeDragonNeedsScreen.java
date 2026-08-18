package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeDragonNeedsScreen extends ForgePagedConfigScreen {
    public ForgeDragonNeedsScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.dragon_needs"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        SaintsDragonsConfig.bootstrap();
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.dragon_needs")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.hunger_decay"),
                SaintsDragonsConfig.HUNGER_DECAY_ENABLED::get,
                SaintsDragonsConfig.HUNGER_DECAY_ENABLED::set,
                SaintsDragonsConfig.HUNGER_DECAY_ENABLED::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.happiness_decay"),
                SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::get,
                SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::set,
                SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::save
        ));
    }

    @Override
    protected void onSave() {
        // Each common config value saves through its entry.
    }
}
