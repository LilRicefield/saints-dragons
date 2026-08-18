package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeServerGameplayScreen extends ForgePagedConfigScreen {
    public ForgeServerGameplayScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.gameplay"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        SaintsDragonsConfig.bootstrap();

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.gameplay.world")));
        addBoolean(entries, "dragon_griefing", SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED);
        addBoolean(entries, "fire_dragon_block_ignition", SaintsDragonsConfig.FIRE_DRAGON_BLOCK_IGNITION_ENABLED);

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.gameplay.riding_effects")));
        addBoolean(entries, "screen_shake", SaintsDragonsConfig.SCREEN_SHAKE_ENABLED);
        addBoolean(entries, "barrel_roll", SaintsDragonsConfig.BARREL_ROLL_ENABLED);

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.gameplay.dragons")));
        addBoolean(entries, "stegonaut_buffs", SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED);
        addBoolean(entries, "dragon_breeding", SaintsDragonsConfig.DRAGON_BREEDING_ENABLED);

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.gameplay.notifications")));
        addBoolean(entries, "wiki_reminder", SaintsDragonsConfig.WIKI_REMINDER_ENABLED);
    }

    private static void addBoolean(List<ConfigEntry> entries, String key,
                                   com.leon.saintsdragons.platform.ConfigHelper.BooleanValue value) {
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others." + key),
                value::get,
                value::set,
                value::save
        ));
    }

    @Override
    protected void onSave() {
        // Each common config value saves through its entry.
    }
}
