package com.leon.saintsdragons.common;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.*;
import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.common.registry.varasuchus.VarasuchusAbilities;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.common.registry.stegonaut.StegonautAbilities;
import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
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
        return new ResourceLocation(MOD_ID, path);
    }

    public static void init() {
        GeckoLib.initialize();
        SaintsDragonsConfig.bootstrap();
        DragonAttributeConfigLoader.bootstrap();
        DragonAnimTickets.bootstrap();
        ModEntities.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModStructurePieces.register();
        ModStructures.register();
        ModMenus.register();
        ModItems.register();
        ModPotions.register();
        ModSounds.register();
        ModParticles.register();
        SaintsCreativeTab.register();
        RaevyxAbilities.init();
        VarasuchusAbilities.init();
        IgnivorusAbilities.init();
        CindervaneAbilities.init();
        StegonautAbilities.init();
        VolitansAbilities.init();
        NulljawAbilities.init();

        NetworkHandler.register();
    }
}