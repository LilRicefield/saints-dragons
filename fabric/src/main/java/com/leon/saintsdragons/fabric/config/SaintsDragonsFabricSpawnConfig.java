package com.leon.saintsdragons.fabric.config;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/server/spawning")
public final class SaintsDragonsFabricSpawnConfig implements ConfigData {
    @ConfigEntry.Category("spawning")
    public boolean raevyxCustomSpawningEnabled = SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    public boolean stegonautCustomSpawningEnabled = SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    public boolean volitansCustomSpawningEnabled = SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT;

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
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int ignivorusSpawnWeight = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int ignivorusMinGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int ignivorusMaxGroupSize = 1;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int atroxiiaSpawnWeight = SaintsDragonsConfig.ATROXIIA_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int atroxiiaMinGroupSize = SaintsDragonsConfig.ATROXIIA_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int atroxiiaMaxGroupSize = SaintsDragonsConfig.ATROXIIA_MAX_GROUP_SIZE_DEFAULT;

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

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int moopSpawnWeight = SaintsDragonsConfig.MOOP_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int moopMinGroupSize = SaintsDragonsConfig.MOOP_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int moopMaxGroupSize = SaintsDragonsConfig.MOOP_MAX_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int mossbackSpawnWeight = SaintsDragonsConfig.MOSSBACK_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int mossbackMinGroupSize = SaintsDragonsConfig.MOSSBACK_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int mossbackMaxGroupSize = SaintsDragonsConfig.MOSSBACK_MAX_GROUP_SIZE_DEFAULT;

}
