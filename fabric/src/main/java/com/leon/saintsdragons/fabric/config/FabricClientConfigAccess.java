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
}
