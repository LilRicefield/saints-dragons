package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeDragonSpawningScreen extends ForgePagedConfigScreen {
    private enum Section {
        RAEVYX,
        STEGONAUT,
        CINDERVANE,
        NULLJAW,
        IGNIVORUS
    }

    private Section section = Section.RAEVYX;

    public ForgeDragonSpawningScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.spawning"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        SaintsDragonsConfig.bootstrap();
        switch (section) {
            case RAEVYX -> addRaevyxEntries(entries);
            case STEGONAUT -> addStegonautEntries(entries);
            case CINDERVANE -> addCindervaneEntries(entries);
            case NULLJAW -> addNulljawEntries(entries);
            case IGNIVORUS -> addIgnivorusEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(90, (width - 50) / 3);
        int spacing = 6;
        int totalTopWidth = buttonWidth * 3 + spacing * 2;
        int startTopX = (width - totalTopWidth) / 2;
        int yTop = 32;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.raevyx"), button -> {
            if (section != Section.RAEVYX) {
                section = Section.RAEVYX;
                rebuildWidgets();
            }
        }).bounds(startTopX, yTop, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.stegonaut"), button -> {
            if (section != Section.STEGONAUT) {
                section = Section.STEGONAUT;
                rebuildWidgets();
            }
        }).bounds(startTopX + (buttonWidth + spacing), yTop, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.cindervane"), button -> {
            if (section != Section.CINDERVANE) {
                section = Section.CINDERVANE;
                rebuildWidgets();
            }
        }).bounds(startTopX + (buttonWidth + spacing) * 2, yTop, buttonWidth, 20).build());

        int totalBottomWidth = buttonWidth * 2 + spacing;
        int startBottomX = (width - totalBottomWidth) / 2;
        int yBottom = yTop + 24;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.nulljaw"), button -> {
            if (section != Section.NULLJAW) {
                section = Section.NULLJAW;
                rebuildWidgets();
            }
        }).bounds(startBottomX, yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.ignivorus"), button -> {
            if (section != Section.IGNIVORUS) {
                section = Section.IGNIVORUS;
                rebuildWidgets();
            }
        }).bounds(startBottomX + (buttonWidth + spacing), yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("saintsdragons.config_screen.reset"), button -> {
            resetSection();
            rebuildWidgets();
        }).bounds(width / 2 - 150, height - 28, 60, 20).build());
    }

    @Override
    protected int getPanelTop() {
        return 84;
    }

    @Override
    protected void onSave() {
        // Spawn config values are read directly from config values; saving is handled per entry.
    }

    private void addRaevyxEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.raevyx")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE::save));
        entries.add(new ListEntry(Component.translatable("config.saintsdragons.spawn.additional_biomes"),
                SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES::get,
                SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES::set,
                SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES::save));
    }

    private void addStegonautEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.stegonaut")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE::save));
        entries.add(new ListEntry(Component.translatable("config.saintsdragons.spawn.additional_biomes"),
                SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES::get,
                SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES::set,
                SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES::save));
    }

    private void addCindervaneEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.cindervane")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE::save));
        entries.add(new ListEntry(Component.translatable("config.saintsdragons.spawn.additional_biomes"),
                SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES::get,
                SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES::set,
                SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES::save));
    }

    private void addNulljawEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.nulljaw")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE::save));
        entries.add(new ListEntry(Component.translatable("config.saintsdragons.spawn.additional_biomes"),
                SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES::get,
                SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES::set,
                SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES::save));
    }

    private void addIgnivorusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.ignivorus")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE::save));
        entries.add(new ListEntry(Component.translatable("config.saintsdragons.spawn.additional_biomes"),
                SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES::get,
                SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES::set,
                SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES::save));
    }

    private void resetSection() {
        switch (section) {
            case RAEVYX -> {
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.set(SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES.set(java.util.Collections.emptyList());
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.RAEVYX_ADDITIONAL_BIOMES.save();
            }
            case STEGONAUT -> {
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.set(SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES.set(java.util.Collections.emptyList());
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.STEGONAUT_ADDITIONAL_BIOMES.save();
            }
            case CINDERVANE -> {
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.set(SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES.set(java.util.Collections.emptyList());
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.CINDERVANE_ADDITIONAL_BIOMES.save();
            }
            case NULLJAW -> {
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.set(SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES.set(java.util.Collections.emptyList());
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.NULLJAW_ADDITIONAL_BIOMES.save();
            }
            case IGNIVORUS -> {
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.set(SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.set(SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.set(SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES.set(java.util.Collections.emptyList());
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.IGNIVORUS_ADDITIONAL_BIOMES.save();
            }
        }
    }
}
