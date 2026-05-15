package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/server/spawning")
public final class SaintsDragonsFabricSpawnConfig implements ConfigData {
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
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
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
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
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
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
    @ConfigEntry.Gui.Tooltip
    public boolean cindervaneEggBlockWorldgen = true;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int varasuchusSpawnWeight = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int varasuchusMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int varasuchusMaxGroupSize = 2;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean varasuchusEggBlockWorldgen = true;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
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
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int volitansSpawnWeight = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int volitansMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int volitansMaxGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int nulljawSpawnWeight = 4;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int nulljawMinGroupSize = 4;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int nulljawMaxGroupSize = 4;

}
