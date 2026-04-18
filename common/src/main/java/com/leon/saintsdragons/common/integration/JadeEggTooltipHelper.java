package com.leon.saintsdragons.common.integration;

import com.leon.saintsdragons.common.block.AbstractDragonEggBlockEntity;
import com.leon.saintsdragons.common.block.AbstractTimedDragonEggBlock;
import com.leon.saintsdragons.common.block.RaevyxEggBlock;
import com.leon.saintsdragons.common.block.RaevyxEggBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class JadeEggTooltipHelper {
    public static final String TAG_REMAINING_TICKS = "EggRemainingTicks";
    public static final String TAG_PAUSED = "EggPaused";

    private JadeEggTooltipHelper() {
    }

    public static void appendEggServerData(CompoundTag tag, Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (state.getBlock() instanceof RaevyxEggBlock eggBlock) {
            RaevyxEggBlockEntity eggEntity = blockEntity instanceof RaevyxEggBlockEntity raevyxEgg ? raevyxEgg : null;
            tag.putInt(TAG_REMAINING_TICKS, eggBlock.getRemainingHatchTicks(level, eggEntity));
            return;
        }

        if (state.getBlock() instanceof AbstractTimedDragonEggBlock<?> eggBlock
                && blockEntity instanceof AbstractDragonEggBlockEntity eggEntity) {
            tag.putInt(TAG_REMAINING_TICKS, eggBlock.getRemainingHatchTicks(level, pos, state, eggEntity));
            if (eggBlock.isHatchingPaused(level, pos, state, eggEntity)) {
                tag.putBoolean(TAG_PAUSED, true);
            }
        }
    }

    @Nullable
    public static Component buildEggTimerLine(CompoundTag serverData) {
        if (!serverData.contains(TAG_REMAINING_TICKS)) {
            return null;
        }
        return Component.translatable("jade.saintsdragons.egg_timer", formatTicks(serverData.getInt(TAG_REMAINING_TICKS)));
    }

    @Nullable
    public static Component buildEggPausedLine(CompoundTag serverData) {
        if (!serverData.getBoolean(TAG_PAUSED)) {
            return null;
        }
        return Component.translatable("jade.saintsdragons.egg_timer_paused");
    }

    public static Component formatTicks(int ticks) {
        int clampedTicks = Math.max(0, ticks);
        int totalSeconds = (int) Math.ceil(clampedTicks / 20.0D);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (minutes > 0 && seconds > 0) {
            return Component.translatable("jade.saintsdragons.time.minutes_seconds", minutes, seconds);
        }
        if (minutes > 0) {
            return Component.translatable("jade.saintsdragons.time.minutes", minutes);
        }
        return Component.translatable("jade.saintsdragons.time.seconds", seconds);
    }
}
