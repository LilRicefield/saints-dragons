package com.leon.saintsdragons;

import com.leon.saintsdragons.client.ClientProxy;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxLightningChainRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautRenderer;
import com.leon.saintsdragons.client.renderer.nulljaw.NulljawRenderer;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.command.DragonAllyCommand;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneMagmaBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

@Mod(SaintsDragons.MOD_ID)
public class SaintsDragons {
    public static final String MOD_ID = "saintsdragons";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public SaintsDragons() {
        this(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public SaintsDragons(IEventBus modBus) {

        GeckoLib.initialize();

        ModEntities.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModParticles.REGISTER.register(modBus);

        modBus.addListener(this::onEntityAttributes);
        modBus.addListener(this::onRegisterRenderers);
        modBus.addListener(this::onBuildCreativeTabContents);

        MinecraftForge.EVENT_BUS.addListener(this::onSpawnPlacements);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientProxy clientProxy = new ClientProxy();
            clientProxy.clientInit();
        });

        NetworkHandler.register();
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.RAEVYX.get(), Raevyx.createAttributes().build());
        event.put(ModEntities.STEGONAUT.get(), Stegonaut.createAttributes().build());
        event.put(ModEntities.CINDERVANE.get(), Cindervane.createAttributes().build());
        event.put(ModEntities.NULLJAW.get(), Nulljaw.createAttributes().build());
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RAEVYX.get(), RaevyxRenderer::new);
        event.registerEntityRenderer(ModEntities.RAEVYX_LIGHTNING_CHAIN.get(), RaevyxLightningChainRenderer::new);
        event.registerEntityRenderer(ModEntities.STEGONAUT.get(), StegonautRenderer::new);
        event.registerEntityRenderer(ModEntities.CINDERVANE.get(), CindervaneRenderer::new);
        event.registerEntityRenderer(ModEntities.NULLJAW.get(), NulljawRenderer::new);
        event.registerEntityRenderer(ModEntities.CINDERVANE_MAGMA_BLOCK.get(), CindervaneMagmaBlockRenderer::new);
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.RAEVYX_SPAWN_EGG);
            event.accept(ModItems.STEGONAUT_SPAWN_EGG);
            event.accept(ModItems.CINDERVANE_SPAWN_EGG);
            event.accept(ModItems.NULLJAW_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DRAGON_ALLY_BOOK);
            event.accept(ModItems.STEGONAUT_BINDER);
            event.accept(ModItems.RAEVYX_BINDER);
            event.accept(ModItems.CINDERVANE_BINDER);
            event.accept(ModItems.NULLJAW_BINDER);
        }
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.RAEVYX.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Raevyx::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.STEGONAUT.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Stegonaut::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.CINDERVANE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Cindervane::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.NULLJAW.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Nulljaw::canSpawn,
                SpawnPlacementRegisterEvent.Operation.AND
        );
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DragonAllyCommand.register(event.getDispatcher());
    }
}
