package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "saintsdragonsspawning")
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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> raevyxAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> raevyxExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> stegonautAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> stegonautExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> cindervaneAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> cindervaneExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> varasuchusAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> varasuchusExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> ignivorusAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> ignivorusExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> volitansAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> volitansExcludedBiomes = new ArrayList<>();

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
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> nulljawAdditionalBiomes = new ArrayList<>();
    @ConfigEntry.Category("spawning")
    @ConfigEntry.Gui.Tooltip(count = 0)
    public List<String> nulljawExcludedBiomes = new ArrayList<>();

    // Others (NPCs, etc.)
    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean dragonGriefingEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean screenShakeEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean barrelRollEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonBankingCameraEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean stegonautBuffsEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean hungerDecayEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean happinessDecayEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    public boolean ivyHouseEnabled = true;

    @ConfigEntry.Category("others")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 20, max = 72000)
    public int ivyRestockInterval = 24000;
}
