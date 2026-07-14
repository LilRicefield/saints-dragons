package com.leon.saintsdragons.common.integration;

import com.leon.saintsdragons.common.block.DraconicCrucibleBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public final class JadeCrucibleTooltipHelper {
    public static final String TAG_REMAINING_TICKS = "CrucibleRemainingTicks";

    private JadeCrucibleTooltipHelper() {
    }

    public static void appendServerData(CompoundTag tag, @Nullable DraconicCrucibleBlockEntity crucible) {
        if (crucible != null && crucible.isProcessing()) {
            tag.putInt(TAG_REMAINING_TICKS, crucible.getRemainingProcessingTicks());
        }
    }

    @Nullable
    public static Component buildTimerLine(CompoundTag serverData) {
        if (!serverData.contains(TAG_REMAINING_TICKS)) {
            return null;
        }
        return Component.translatable(
                "jade.saintsdragons.draconic_crucible.processing",
                JadeEggTooltipHelper.formatTicks(serverData.getInt(TAG_REMAINING_TICKS)));
    }
}
