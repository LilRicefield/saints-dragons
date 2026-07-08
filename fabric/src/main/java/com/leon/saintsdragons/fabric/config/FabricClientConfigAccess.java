package com.leon.saintsdragons.fabric.config;

import me.shedaniel.autoconfig.AutoConfig;

public final class FabricClientConfigAccess {
    private FabricClientConfigAccess() {
    }

    public static boolean isFirstPersonBankingCameraEnabled() {
        try {
            return AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class)
                    .getConfig()
                    .firstPersonBankingCameraEnabled;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static boolean isDiveCameraWobbleEnabled() {
        try {
            return AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class)
                    .getConfig()
                    .diveCameraWobbleEnabled;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static boolean isDiveSpeedLinesEnabled() {
        try {
            return AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class)
                    .getConfig()
                    .diveSpeedLinesEnabled;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static boolean isGenericDiveLoopEnabled() {
        try {
            return AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class)
                    .getConfig()
                    .genericDiveLoopEnabled;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static float getSwarmBattleMusicVolume() {
        try {
            int volume = AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class)
                    .getConfig()
                    .swarmBattleMusicVolume;
            return Math.max(0, Math.min(100, volume)) / 100.0F;
        } catch (RuntimeException ignored) {
            return 1.0F;
        }
    }
}
