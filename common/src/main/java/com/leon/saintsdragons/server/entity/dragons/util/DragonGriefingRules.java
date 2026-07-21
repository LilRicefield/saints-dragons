package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

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

    public static boolean canSetBlocksOnFire(Level level) {
        return canDestroyBlocks(level) && SaintsDragonsConfig.isFireDragonBlockIgnitionEnabled();
    }

    public static boolean isProtectedFromPassiveTreeDestruction(ServerLevel level, BlockPos pos) {
        for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
            if (start != null && start.isValid()) {
                return true;
            }
        }
        return false;
    }
}
