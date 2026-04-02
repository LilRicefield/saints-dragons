package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class DragonGriefingRules {
    private DragonGriefingRules() {
    }

    public static boolean canDestroyBlocks(Level level) {
        if (level == null) {
            return false;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        return SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED == null
                || SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.get();
    }
}
