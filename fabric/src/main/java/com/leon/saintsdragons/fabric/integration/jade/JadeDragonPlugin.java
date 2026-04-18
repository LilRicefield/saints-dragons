package com.leon.saintsdragons.fabric.integration.jade;

import com.leon.saintsdragons.common.block.AbstractDragonEggBlockEntity;
import com.leon.saintsdragons.common.block.AbstractTimedDragonEggBlock;
import com.leon.saintsdragons.common.block.RaevyxEggBlock;
import com.leon.saintsdragons.common.block.RaevyxEggBlockEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;


@WailaPlugin
public class JadeDragonPlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(DragonEntityProvider.INSTANCE, DragonEntity.class);
        registration.registerBlockDataProvider(DragonEggBlockProvider.INSTANCE, AbstractDragonEggBlockEntity.class);
        registration.registerBlockDataProvider(DragonEggBlockProvider.INSTANCE, RaevyxEggBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(DragonEntityProvider.INSTANCE, DragonEntity.class);
        registration.registerBlockComponent(DragonEggBlockProvider.INSTANCE, AbstractTimedDragonEggBlock.class);
        registration.registerBlockComponent(DragonEggBlockProvider.INSTANCE, RaevyxEggBlock.class);
    }
}
