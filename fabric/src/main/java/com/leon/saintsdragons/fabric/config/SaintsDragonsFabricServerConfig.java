package com.leon.saintsdragons.fabric.config;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/server/servercommon")
public final class SaintsDragonsFabricServerConfig implements ConfigData {
    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean dragonGriefingEnabled = SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean screenShakeEnabled = SaintsDragonsConfig.SCREEN_SHAKE_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean barrelRollEnabled = SaintsDragonsConfig.BARREL_ROLL_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean stegonautBuffsEnabled = SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean dragonBreedingEnabled = SaintsDragonsConfig.DRAGON_BREEDING_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean hungerDecayEnabled = SaintsDragonsConfig.HUNGER_DECAY_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean happinessDecayEnabled = SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED_DEFAULT;

    @ConfigEntry.Category("gameplay")
    @ConfigEntry.Gui.Tooltip
    public boolean wikiReminderEnabled = SaintsDragonsConfig.WIKI_REMINDER_ENABLED_DEFAULT;

    @ConfigEntry.Category("ivy")
    @ConfigEntry.Gui.Tooltip
    public boolean ivyHouseEnabled = SaintsDragonsConfig.IVY_HOUSE_ENABLED_DEFAULT;

    @ConfigEntry.Category("ivy")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 20, max = 72000)
    public int ivyRestockInterval = SaintsDragonsConfig.IVY_RESTOCK_INTERVAL_DEFAULT;
}
