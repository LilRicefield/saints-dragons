package com.leon.saintsdragons.common;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.*;
import com.leon.saintsdragons.server.entity.variant.SaintsDragonVariantRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

public final class SaintsDragonsCommon {
    public static final String MOD_ID = "saintsdragons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SaintsDragonsCommon() {
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static synchronized void init() {
        SaintsDragonsConfig.bootstrap();
        ToolsArmorConfig.bootstrap();
        DragonAttributeConfigLoader.bootstrap();
        SaintsDragonVariantRegistry.bootstrap();
        ModAbilities.register();
        ModMemoryTypes.register();
        ModSensorTypes.register();
        ModAttributes.register();
        ModEntities.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModItems.register();
        ModRecipes.register();
        ModPotions.register();
        ModSounds.register();
        ModParticles.register();
        SaintsCreativeTab.register();
        NetworkHandler.register();
    }
}
