package com.leon.saintsdragons.forge;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.forge.loot.ModLootModifiers;
import com.leon.saintsdragons.forge.world.AddDragonsBiomeModifier;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge entry point that bridges common setup into the mod lifecycle.
 */
@Mod(SaintsDragonsCommon.MOD_ID)
public final class SaintsDragonsForge {

    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, SaintsDragonsCommon.MOD_ID);

    private static final RegistryObject<Codec<AddDragonsBiomeModifier>> ADD_DRAGONS_CODEC =
            BIOME_MODIFIERS.register("add_dragons", () -> AddDragonsBiomeModifier.CODEC);

    public SaintsDragonsForge() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BIOME_MODIFIERS.register(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onBuildCreativeTabs);
        modEventBus.addListener(this::onRegisterSpawnPlacements);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        SaintsDragonsCommon.init();
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        CommonModEvents.registerEntityAttributes((type, builder) -> event.put(type, builder.build()));
    }

    private void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        CommonModEvents.registerCreativeTabEntries((tabKey, itemSupplier) -> {
            if (event.getTabKey().equals(tabKey)) {
                event.accept(itemSupplier::get);
            }
        });
    }

    private void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        CommonModEvents.registerSpawnPlacements(new CommonModEvents.SpawnPlacementRegistrar() {
            @Override
            public <T extends Mob> void register(
                    EntityType<T> type,
                    SpawnPlacements.Type placementType,
                    Heightmap.Types heightmap,
                    SpawnPlacements.SpawnPredicate<T> predicate
            ) {
                event.register(type, placementType, heightmap, predicate, SpawnPlacementRegisterEvent.Operation.AND);
            }
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommonModEvents.registerCommands(event.getDispatcher());
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DragonAttributeConfigLoader.getInstance());
    }
}
