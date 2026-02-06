package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "saintsdragonsspawning")
public final class SaintsDragonsFabricConfig implements ConfigData {
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int raevyxSpawnWeight = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int raevyxMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int raevyxMaxGroupSize = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> raevyxAdditionalBiomes = new ArrayList<>();

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int stegonautSpawnWeight = 5;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int stegonautMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int stegonautMaxGroupSize = 4;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> stegonautAdditionalBiomes = new ArrayList<>();

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int cindervaneSpawnWeight = 3;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int cindervaneMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int cindervaneMaxGroupSize = 3;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> cindervaneAdditionalBiomes = new ArrayList<>();

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean cindervaneEggBlockWorldgen = true;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int nulljawSpawnWeight = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int nulljawMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int nulljawMaxGroupSize = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> nulljawAdditionalBiomes = new ArrayList<>();

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean nulljawEggBlockWorldgen = true;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int ignivorusSpawnWeight = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int ignivorusMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int ignivorusMaxGroupSize = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> ignivorusAdditionalBiomes = new ArrayList<>();

    // Others (NPCs, etc.)
    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 20, max = 72000)
    public int ivyRestockInterval = 24000;
}
