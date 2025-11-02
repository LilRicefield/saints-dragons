package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.fabricmc.api.ModInitializer;

public final class SaintsDragonsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SaintsDragonsCommon.init();
    }
}
