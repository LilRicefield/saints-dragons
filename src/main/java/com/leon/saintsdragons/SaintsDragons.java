package com.leon.saintsdragons;

import com.leon.saintsdragons.client.ClientProxy;
import com.leon.saintsdragons.client.renderer.amphithere.AmphithereRenderer;
import com.leon.saintsdragons.client.renderer.lightningdragon.LightningChainRenderer;
import com.leon.saintsdragons.client.renderer.lightningdragon.LightningDragonRenderer;
import com.leon.saintsdragons.client.renderer.primitivedrake.PrimitiveDrakeRenderer;
import com.leon.saintsdragons.client.renderer.riftdrake.RiftDrakeRenderer;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.command.DragonAllyCommand;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import com.leon.saintsdragons.client.renderer.amphithere.AmphithereMagmaBlockRenderer;
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
        event.put(ModEntities.LIGHTNING_DRAGON.get(), LightningDragonEntity.createAttributes().build());
        event.put(ModEntities.PRIMITIVE_DRAKE.get(), PrimitiveDrakeEntity.createAttributes().build());
        event.put(ModEntities.AMPHITHERE.get(), AmphithereEntity.createAttributes().build());
        event.put(ModEntities.RIFT_DRAKE.get(), RiftDrakeEntity.createAttributes().build());
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LIGHTNING_DRAGON.get(), LightningDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_CHAIN.get(), LightningChainRenderer::new);
        event.registerEntityRenderer(ModEntities.PRIMITIVE_DRAKE.get(), PrimitiveDrakeRenderer::new);
        event.registerEntityRenderer(ModEntities.AMPHITHERE.get(), AmphithereRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT_DRAKE.get(), RiftDrakeRenderer::new);
        event.registerEntityRenderer(ModEntities.AMPHITHERE_MAGMA_BLOCK.get(), AmphithereMagmaBlockRenderer::new);
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.LIGHTNING_DRAGON_SPAWN_EGG);
            event.accept(ModItems.PRIMITIVE_DRAKE_SPAWN_EGG);
            event.accept(ModItems.AMPHITHERE_SPAWN_EGG);
            event.accept(ModItems.RIFT_DRAKE_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DRAGON_ALLY_BOOK);
            event.accept(ModItems.PRIMITIVE_DRAKE_BINDER);
            event.accept(ModItems.LIGHTNING_DRAGON_BINDER);
            event.accept(ModItems.AMPHITHERE_BINDER);
            event.accept(ModItems.RIFT_DRAKE_BINDER);
        }
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.LIGHTNING_DRAGON.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LightningDragonEntity::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.PRIMITIVE_DRAKE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                PrimitiveDrakeEntity::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.AMPHITHERE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AmphithereEntity::canSpawnHere,
                SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
                ModEntities.RIFT_DRAKE.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RiftDrakeEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.AND
        );
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DragonAllyCommand.register(event.getDispatcher());
    }
}
