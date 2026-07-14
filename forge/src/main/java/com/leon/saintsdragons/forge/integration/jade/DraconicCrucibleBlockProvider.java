package com.leon.saintsdragons.forge.integration.jade;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.DraconicCrucibleBlockEntity;
import com.leon.saintsdragons.common.integration.JadeCrucibleTooltipHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DraconicCrucibleBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Component timerLine = JadeCrucibleTooltipHelper.buildTimerLine(accessor.getServerData());
        if (timerLine != null) {
            tooltip.add(timerLine);
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        JadeCrucibleTooltipHelper.appendServerData(tag,
                accessor.getBlockEntity() instanceof DraconicCrucibleBlockEntity crucible ? crucible : null);
    }

    @Override
    public ResourceLocation getUid() {
        return SaintsDragonsCommon.rl("draconic_crucible_timer");
    }
}
