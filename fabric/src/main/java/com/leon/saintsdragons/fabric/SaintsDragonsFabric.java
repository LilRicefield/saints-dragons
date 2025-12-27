package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.fabric.entity.part.FabricPartEntities;
import com.leon.saintsdragons.fabric.resource.FabricDragonAttributeReloadListener;
import com.leon.saintsdragons.fabric.world.FabricDragonSpawns;
import com.leon.saintsdragons.fabric.server.FabricServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SaintsDragonsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SaintsDragonsCommon.init();
        FabricPartEntities.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricDragonAttributeReloadListener());
        FabricServerEvents.init();

        CommonModEvents.registerEntityAttributes((type, builder) ->
                FabricDefaultAttributeRegistry.register(type, builder.build()));

        CommonModEvents.registerSpawnPlacements(SpawnPlacements::register);
        FabricDragonSpawns.register();

        CommonModEvents.registerCreativeTabEntries((tab, itemSupplier) ->
                ItemGroupEvents.modifyEntriesEvent(tab)
                        .register(entries -> entries.accept(itemSupplier.get())));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommonModEvents.registerCommands(dispatcher));
    }
}
