package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralized dragon griefing toggles (global + per-dragon).
 */
public final class DragonGriefingRules {
    private static final String GRIEFING_ENABLED_KEY = "griefing_enabled";

    private DragonGriefingRules() {
    }

    public static boolean isGlobalGriefingEnabled() {
        return SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED == null
                || SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.get();
    }

    public static boolean canCindervaneGriefing() {
        return canDragonGriefing(DragonAttributeConfigLoader.CINDERVANE_ID);
    }

    public static boolean canIgnivorusGriefing() {
        return canDragonGriefing(DragonAttributeConfigLoader.IGNIVORUS_ID);
    }

    public static boolean canNulljawGriefing() {
        return canDragonGriefing(DragonAttributeConfigLoader.NULLJAW_ID);
    }

    private static boolean canDragonGriefing(ResourceLocation dragonId) {
        if (!isGlobalGriefingEnabled()) {
            return false;
        }
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(dragonId)
                .extraBoolean(GRIEFING_ENABLED_KEY, true);
    }
}
