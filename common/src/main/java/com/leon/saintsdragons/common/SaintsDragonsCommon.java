package com.leon.saintsdragons.common;

import com.leon.saintsdragons.common.network.DragonAnimTickets;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.common.registry.stegonaut.StegonautAbilities;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaintsDragonsCommon {
    public static final String MOD_ID = "saintsdragons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SaintsDragonsCommon() {
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void init() {
        // GeckoLib data tickets MUST be registered first (before entities use them)
        DragonAnimTickets.bootstrap();

        ModEntities.register();
        ModItems.register();
        ModSounds.register();
        ModParticles.register();

        // Ensure ability registries are loaded on both logical sides.
        RaevyxAbilities.init();
        NulljawAbilities.init();
        CindervaneAbilities.init();
        StegonautAbilities.init();

        NetworkHandler.register();
    }
}
