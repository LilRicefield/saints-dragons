package com.leon.saintsdragons.fabric.config;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/server/spawning")
public final class SaintsDragonsFabricSpawnConfig implements ConfigData {
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean raevyxSpawningEnabled = SaintsDragonsConfig.RAEVYX_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean stegonautSpawningEnabled = SaintsDragonsConfig.STEGONAUT_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean cindervaneSpawningEnabled = SaintsDragonsConfig.CINDERVANE_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean ignivorusSpawningEnabled = SaintsDragonsConfig.IGNIVORUS_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean varasuchusSpawningEnabled = SaintsDragonsConfig.VARASUCHUS_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean atroxiiaSpawningEnabled = SaintsDragonsConfig.ATROXIIA_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean volitansSpawningEnabled = SaintsDragonsConfig.VOLITANS_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean nulljawSpawningEnabled = SaintsDragonsConfig.NULLJAW_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean moopSpawningEnabled = SaintsDragonsConfig.MOOP_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean mossbackSpawningEnabled = SaintsDragonsConfig.MOSSBACK_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    public boolean ivySpawningEnabled = SaintsDragonsConfig.IVY_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    public boolean raevyxCustomSpawningEnabled = SaintsDragonsConfig.RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    public boolean stegonautCustomSpawningEnabled = SaintsDragonsConfig.STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    public boolean volitansCustomSpawningEnabled = SaintsDragonsConfig.VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int raevyxSpawnWeight = SaintsDragonsConfig.RAEVYX_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int raevyxMinGroupSize = SaintsDragonsConfig.RAEVYX_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int raevyxMaxGroupSize = SaintsDragonsConfig.RAEVYX_MAX_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int stegonautSpawnWeight = SaintsDragonsConfig.STEGONAUT_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int stegonautMinGroupSize = SaintsDragonsConfig.STEGONAUT_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int stegonautMaxGroupSize = SaintsDragonsConfig.STEGONAUT_MAX_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int cindervaneSpawnWeight = SaintsDragonsConfig.CINDERVANE_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int cindervaneMinGroupSize = SaintsDragonsConfig.CINDERVANE_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int cindervaneMaxGroupSize = SaintsDragonsConfig.CINDERVANE_MAX_GROUP_SIZE_DEFAULT;

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
    public int volitansSpawnWeight = SaintsDragonsConfig.VOLITANS_SPAWN_WEIGHT_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int volitansMinGroupSize = SaintsDragonsConfig.VOLITANS_MIN_GROUP_SIZE_DEFAULT;

    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int volitansMaxGroupSize = SaintsDragonsConfig.VOLITANS_MAX_GROUP_SIZE_DEFAULT;

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
