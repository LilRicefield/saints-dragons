package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.fabric.world.FabricDragonSpawns;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SaintsDragonsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SaintsDragonsCommon.init();

        CommonModEvents.registerEntityAttributes((type, builder) ->
                FabricDefaultAttributeRegistry.register(type, builder.build()));

        CommonModEvents.registerSpawnPlacements(new CommonModEvents.SpawnPlacementRegistrar() {
            @Override
            public <T extends Mob> void register(
                    EntityType<T> type,
                    SpawnPlacements.Type placementType,
                    Heightmap.Types heightmap,
                    SpawnPlacements.SpawnPredicate<T> predicate
            ) {
                SpawnPlacements.register(type, placementType, heightmap, predicate);
            }
        });
        FabricDragonSpawns.register();

        CommonModEvents.registerCreativeTabEntries((tab, itemSupplier) ->
                ItemGroupEvents.modifyEntriesEvent(tab)
                        .register(entries -> entries.accept(itemSupplier.get())));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommonModEvents.registerCommands(dispatcher));
    }
}
