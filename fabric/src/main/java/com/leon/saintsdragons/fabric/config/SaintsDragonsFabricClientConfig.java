package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/client/clientcommon")
public final class SaintsDragonsFabricClientConfig implements ConfigData {
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonBankingCameraEnabled = true;

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean thirdPersonBankingCameraEnabled = true;

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean diveCameraWobbleEnabled = true;

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean diveSpeedLinesEnabled = true;

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean genericDiveLoopEnabled = true;

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public int swarmBattleMusicVolume = 100;
}
