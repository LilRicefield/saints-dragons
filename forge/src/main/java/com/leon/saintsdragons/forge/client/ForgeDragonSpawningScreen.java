package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
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
        IGNIVORUS,
        VARASUCHUS,
        NULLJAW,
        VOLITANS,
        ATROXIIA,
        OTHER
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
            case IGNIVORUS -> addIgnivorusEntries(entries);
            case VARASUCHUS -> addVarasuchusEntries(entries);
            case NULLJAW -> addNulljawEntries(entries);
            case VOLITANS -> addVolitansEntries(entries);
            case ATROXIIA -> addAtroxiiaEntries(entries);
            case OTHER -> addOtherEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(90, (width - 72) / 3);
        int spacing = 6;
        int rowWidth = buttonWidth * 3 + spacing * 2;
        int startX = (width - rowWidth) / 2;
        int yTop = 32;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.raevyx"), button -> {
            if (section != Section.RAEVYX) {
                section = Section.RAEVYX;
                rebuildWidgets();
            }
        }).bounds(startX, yTop, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.stegonaut"), button -> {
            if (section != Section.STEGONAUT) {
                section = Section.STEGONAUT;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing), yTop, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.cindervane"), button -> {
            if (section != Section.CINDERVANE) {
                section = Section.CINDERVANE;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 2, yTop, buttonWidth, 20).build());

        int yMiddle = yTop + 24;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.ignivorus"), button -> {
            if (section != Section.IGNIVORUS) {
                section = Section.IGNIVORUS;
                rebuildWidgets();
            }
        }).bounds(startX, yMiddle, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.varasuchus"), button -> {
            if (section != Section.VARASUCHUS) {
                section = Section.VARASUCHUS;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing), yMiddle, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.atroxiia"), button -> {
            if (section != Section.ATROXIIA) {
                section = Section.ATROXIIA;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 2, yMiddle, buttonWidth, 20).build());

        int yBottom = yMiddle + 24;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.volitans"), button -> {
            if (section != Section.VOLITANS) {
                section = Section.VOLITANS;
                rebuildWidgets();
            }
        }).bounds(startX, yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.nulljaw"), button -> {
            if (section != Section.NULLJAW) {
                section = Section.NULLJAW;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing), yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.spawn.other"), button -> {
            if (section != Section.OTHER) {
                section = Section.OTHER;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 2, yBottom, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("saintsdragons.config_screen.reset"), button -> {
            resetSection();
            rebuildWidgets();
        }).bounds(width / 2 - 150, height - 28, 60, 20).build());
    }

    @Override
    protected int getPanelTop() {
        return 108;
    }

    @Override
    protected void onSave() {
        // Spawn config values are read directly from config values; saving is handled per entry.
    }

    private void addRaevyxEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.raevyx")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.raevyx")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.RAEVYX_SPAWNING_ENABLED);
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
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.STEGONAUT_SPAWNING_ENABLED);
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
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.CINDERVANE_SPAWNING_ENABLED);
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
    }

    private void addIgnivorusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.ignivorus")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.structure")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.IGNIVORUS_SPAWNING_ENABLED);
    }

    private void addVarasuchusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.varasuchus")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.structure")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.VARASUCHUS_SPAWNING_ENABLED);
    }

    private void addNulljawEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.nulljaw")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.NULLJAW_SPAWNING_ENABLED);
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
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.VOLITANS_SPAWNING_ENABLED);
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

    private void addAtroxiiaEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.atroxiia")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.common")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.ATROXIIA_SPAWNING_ENABLED);
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE::save));
    }

    private void addOtherEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.other")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.moop")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.MOOP_SPAWNING_ENABLED);
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE::save));

        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.mossback")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.MOSSBACK_SPAWNING_ENABLED);
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.weight"),
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT::get,
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT::set,
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.min_group"),
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE::get,
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE::set,
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE::save));
        entries.add(new IntEntry(Component.translatable("config.saintsdragons.spawn.max_group"),
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE::get,
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE::set,
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE::save));

        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.ivy")));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.note.structure")));
        addSpawnEnabledEntry(entries,
                SaintsDragonsConfig.IVY_SPAWNING_ENABLED);
    }

    private void addSpawnEnabledEntry(List<ConfigEntry> entries,
                                      ConfigHelper.BooleanValue value) {
        entries.add(new BooleanEntry(
                Component.translatable("config.saintsdragons.spawn.enabled"),
                value::get,
                value::set,
                value::save));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.spawn.enabled.restart")));
    }

    private void resetSection() {
        switch (section) {
            case RAEVYX -> {
                SaintsDragonsConfig.RAEVYX_SPAWNING_ENABLED.set(SaintsDragonsConfig.RAEVYX_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.set(SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.set(SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.RAEVYX_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE.save();
            }
            case STEGONAUT -> {
                SaintsDragonsConfig.STEGONAUT_SPAWNING_ENABLED.set(SaintsDragonsConfig.STEGONAUT_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.set(SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.set(SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.STEGONAUT_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE.save();
            }
            case CINDERVANE -> {
                SaintsDragonsConfig.CINDERVANE_SPAWNING_ENABLED.set(SaintsDragonsConfig.CINDERVANE_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.set(SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.set(SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.CINDERVANE_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE.save();
            }
            case IGNIVORUS -> {
                SaintsDragonsConfig.IGNIVORUS_SPAWNING_ENABLED.set(SaintsDragonsConfig.IGNIVORUS_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.IGNIVORUS_SPAWNING_ENABLED.save();
            }
            case VARASUCHUS -> {
                SaintsDragonsConfig.VARASUCHUS_SPAWNING_ENABLED.set(SaintsDragonsConfig.VARASUCHUS_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.VARASUCHUS_SPAWNING_ENABLED.save();
            }
            case NULLJAW -> {
                SaintsDragonsConfig.NULLJAW_SPAWNING_ENABLED.set(SaintsDragonsConfig.NULLJAW_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.set(SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.set(SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.NULLJAW_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.NULLJAW_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.NULLJAW_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.NULLJAW_MAX_GROUP_SIZE.save();
            }

            case VOLITANS -> {
                SaintsDragonsConfig.VOLITANS_SPAWNING_ENABLED.set(SaintsDragonsConfig.VOLITANS_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED.set(SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.set(SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.set(SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.set(SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.VOLITANS_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE.save();
            }
            case ATROXIIA -> {
                SaintsDragonsConfig.ATROXIIA_SPAWNING_ENABLED.set(SaintsDragonsConfig.ATROXIIA_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT.set(SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE.set(SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE.set(SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.ATROXIIA_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE.save();
            }
            case OTHER -> {
                SaintsDragonsConfig.MOOP_SPAWNING_ENABLED.set(SaintsDragonsConfig.MOOP_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT.set(SaintsDragonsConfig.MOOP_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE.set(SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE.set(SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.MOSSBACK_SPAWNING_ENABLED.set(SaintsDragonsConfig.MOSSBACK_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT.set(SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT_DEFAULT);
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE.set(SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE.set(SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE_DEFAULT);
                SaintsDragonsConfig.IVY_SPAWNING_ENABLED.set(SaintsDragonsConfig.IVY_SPAWNING_ENABLED_DEFAULT);
                SaintsDragonsConfig.MOOP_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.MOOP_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.MOSSBACK_SPAWNING_ENABLED.save();
                SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT.save();
                SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE.save();
                SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE.save();
                SaintsDragonsConfig.IVY_SPAWNING_ENABLED.save();
            }
        }
    }
}
