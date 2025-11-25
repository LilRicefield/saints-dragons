package com.leon.saintsdragons.common;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModDataComponents;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.common.registry.stegonaut.StegonautAbilities;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.GeckoLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaintsDragonsCommon {
    public static final String MOD_ID = "saintsdragons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SaintsDragonsCommon() {
    }

    public static ResourceLocation rl(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        // Initialize GeckoLib runtime (required for animations)
        // GeckoLib bootstrap not required on 1.21.1; remove explicit initialize
        SaintsDragonsConfig.bootstrap();
        DragonAttributeConfigLoader.bootstrap();
        // GeckoLib data tickets MUST be registered first (before entities use them)
        DragonAnimTickets.bootstrap();

        ModDataComponents.register();
        ModEntities.register();
        ModItems.register();
        ModSounds.register();
        ModParticles.register();

        // Ensure ability registries are loaded on both logical sides.
        RaevyxAbilities.init();
        NulljawAbilities.init();
        IgnivorusAbilities.init();
        CindervaneAbilities.init();
        StegonautAbilities.init();

        NetworkHandler.register();
    }
}
