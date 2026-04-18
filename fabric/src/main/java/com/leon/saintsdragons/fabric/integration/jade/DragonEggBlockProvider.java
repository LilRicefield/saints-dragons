package com.leon.saintsdragons.fabric.integration.jade;

import com.leon.saintsdragons.common.integration.JadeEggTooltipHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DragonEggBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        Component timerLine = JadeEggTooltipHelper.buildEggTimerLine(serverData);
        if (timerLine != null) {
            tooltip.add(timerLine);
        }

        Component pausedLine = JadeEggTooltipHelper.buildEggPausedLine(serverData);
        if (pausedLine != null) {
            tooltip.add(pausedLine);
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        JadeEggTooltipHelper.appendEggServerData(
                tag,
                accessor.getLevel(),
                accessor.getPosition(),
                accessor.getBlockState(),
                accessor.getBlockEntity()
        );
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation("saintsdragons", "egg_timer");
    }
}
