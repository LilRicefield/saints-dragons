package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "saintsdragons/client")
public final class SaintsDragonsFabricClientConfig implements ConfigData {
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonBankingCameraEnabled = true;
}
