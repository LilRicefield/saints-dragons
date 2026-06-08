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
        VARASUCHUS,
        IGNIVORUS,
        VOLITANS
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
            case VARASUCHUS -> addVarasuchusEntries(entries);
            case IGNIVORUS -> addIgnivorusEntries(entries);
            case VOLITANS -> addVolitansEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(90, (width - 72) / 4);
        int spacing = 6;
        int totalTopWidth = buttonWidth * 4 + spacing * 3;
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

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.nulljaw"), button -> {
            if (section != Section.NULLJAW) {
                section = Section.NULLJAW;
                rebuildWidgets();
            }
        }).bounds(startTopX + (buttonWidth + spacing) * 3, yTop, buttonWidth, 20).build());

        int totalBottomWidth = buttonWidth * 3 + spacing * 2;
        int startBottomX = (width - totalBottomWidth) / 2;
        int yBottom = yTop + 24;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.varasuchus"), button -> {
            if (section != Section.VARASUCHUS) {
                section = Section.VARASUCHUS;
                rebuildWidgets();
            }
        }).bounds(startBottomX, yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.ignivorus"), button -> {
            if (section != Section.IGNIVORUS) {
                section = Section.IGNIVORUS;
                rebuildWidgets();
            }
        }).bounds(startBottomX + (buttonWidth + spacing), yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.volitans"), button -> {
            if (section != Section.VOLITANS) {
                section = Section.VOLITANS;
                rebuildWidgets();
            }
        }).bounds(startBottomX + (buttonWidth + spacing) * 2, yBottom, buttonWidth, 20).build());

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
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.raevyx")));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.spawn.custom_spawning"),
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED::get,
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED::set,
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED::save));
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
    }

    private void addStegonautEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.stegonaut")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.spawn.custom_spawning"),
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED::get,
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED::set,
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED::save));
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
    }

    private void addCindervaneEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.cindervane")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
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
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.spawn.egg_block_worldgen"),
                SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN::get,
                SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN::set,
                SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN::save));
    }

    private void addVarasuchusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.varasuchus")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE::save));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.spawn.egg_block_worldgen"),
                SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN::get,
                SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN::set,
                SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN::save));
    }

    private void addIgnivorusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.ignivorus")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
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
    }

    private void addNulljawEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.nulljaw")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
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
    }

    private void addVolitansEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.volitans")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.spawn.custom_spawning"),
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED::get,
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED::set,
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE::save));
    }

    private void resetSection() {
        switch (section) {
            case RAEVYX -> {
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.set(SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.save();
            }
            case STEGONAUT -> {
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.set(SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.save();
            }
            case CINDERVANE -> {
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.set(SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN.set(SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.CINDERVANE_EGG_BLOCK_WORLDGEN.save();
            }
            case NULLJAW -> {
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.set(SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.save();
            }
            case VARASUCHUS -> {
                SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT.set(SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE.set(SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE.set(SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN.set(SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN_DEFAULT);
                SaintsDragonsConfig.VARASUCHUS_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.VARASUCHUS_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.VARASUCHUS_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.VARASUCHUS_EGG_BLOCK_WORLDGEN.save();
            }
            case IGNIVORUS -> {
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.set(SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.set(SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.set(SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.IGNIVORUS_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.IGNIVORUS_MAX_GROUP_SIZE.save();
            }
            case VOLITANS -> {
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.set(SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.set(SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.set(SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.save();
            }
        }
    }
}
