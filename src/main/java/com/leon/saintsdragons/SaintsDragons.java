package com.leon.saintsdragons;

import com.leon.saintsdragons.client.renderer.lightningdragon.LightningDragonRenderer;
import com.leon.saintsdragons.client.renderer.lightningdragon.LightningChainRenderer;
import com.leon.saintsdragons.client.renderer.primitivedrake.PrimitiveDrakeRenderer;
import com.leon.saintsdragons.client.ClientProxy;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.command.DragonAllyCommand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.GeckoLib;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;



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
        // Debug logging
        System.out.println("SaintsDragons: Mod constructor called");

        // Initialize GeckoLib to enable animation controllers and ticking
        GeckoLib.initialize();

        // Register deferred registers
        ModEntities.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModParticles.REGISTER.register(modBus);
        
        // Biome modifiers are handled via data generation in worldgen/biome_modifier/

        // Force-load ability registry so static registrations (BITE, HORN_GORE, LIGHTNING_BEAM) occur
        // This ensures AbilityRegistry knows about our abilities before any network-triggered use
        @SuppressWarnings("unused") var _forceInit = ModAbilities.BITE;

        // Register event handlers
        modBus.addListener(this::onEntityAttributes);
        modBus.addListener(this::onRegisterRenderers);
        modBus.addListener(this::onBuildCreativeTabContents);

        // Register spawn placements and commands on the Forge event bus
        MinecraftForge.EVENT_BUS.addListener(this::onSpawnPlacements);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Initialize client-side proxy
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientProxy clientProxy = new ClientProxy();
            clientProxy.clientInit();
        });

        NetworkHandler.register();
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        // Register entity attributes
        event.put(ModEntities.LIGHTNING_DRAGON.get(), LightningDragonEntity.createAttributes().build());
        event.put(ModEntities.PRIMITIVE_DRAKE.get(), PrimitiveDrakeEntity.createAttributes().build());
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register entity renderers
        event.registerEntityRenderer(ModEntities.LIGHTNING_DRAGON.get(), LightningDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_CHAIN.get(), LightningChainRenderer::new);
        event.registerEntityRenderer(ModEntities.PRIMITIVE_DRAKE.get(), PrimitiveDrakeRenderer::new);
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        // Add to spawn eggs tab
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.LIGHTNING_DRAGON_SPAWN_EGG);
            event.accept(ModItems.PRIMITIVE_DRAKE_SPAWN_EGG);
        }
        // Add to tools tab
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.DRAGON_ALLY_BOOK);
        }
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // Register spawn placement rules
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
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        // Register dragon ally commands
        DragonAllyCommand.register(event.getDispatcher());
    }
}
