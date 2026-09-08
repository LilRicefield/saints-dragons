package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/** Only exact terrain contacts are breakable after the ability's six-second gate. */
final class IgnivorusBreathTerrain {
    private static final int MAX_BREAKS_PER_TICK = 8;
    private static final int MAX_ATTEMPTS_PER_TICK = 16;

    void tick(ServerLevel level, Ignivorus dragon, Map<BlockPos, BlockState> impacts) {
        if (!DragonGriefingRules.canDestroyBlocks(level)) {
            return;
        }
        int broken = 0;
        int attempts = 0;
        for (var impact : impacts.entrySet()) {
            BlockPos pos = impact.getKey();
            BlockState state = impact.getValue();
            if (broken >= MAX_BREAKS_PER_TICK || attempts >= MAX_ATTEMPTS_PER_TICK) break;
            if (!level.hasChunkAt(pos) || level.getBlockState(pos) != state || !canBreak(level, pos, state)) continue;
            attempts++;
            if (!Services.PLATFORM.canDragonBreakBlock(level, dragon, pos, state)) {
                continue;
            }
            // Events may replace a block: only remove the exact state that received this tick's hit.
            if (level.getBlockState(pos) == state && level.destroyBlock(pos, true, dragon)) {
                broken++;
            }
        }
    }

    private boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && !state.is(BlockTags.DRAGON_IMMUNE)
                && state.getDestroySpeed(level, pos) >= 0;
    }

}
