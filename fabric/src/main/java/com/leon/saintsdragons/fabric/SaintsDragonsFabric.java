package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.init.CommonBrewingRecipes;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.fabric.entity.part.FabricPartEntities;
import com.leon.saintsdragons.fabric.loot.FabricLootTableModifier;
import com.leon.saintsdragons.fabric.mixin.RangedAttributeAccessor;
import com.leon.saintsdragons.fabric.resource.FabricDragonAttributeReloadListener;
import com.leon.saintsdragons.fabric.server.FabricServerEvents;
import com.leon.saintsdragons.fabric.world.FabricDragonSpawns;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SaintsDragonsFabric implements ModInitializer {
    private static final double ATTRIBUTE_CAP = 10000.0D;

    @Override
    public void onInitialize() {
        SaintsDragonsCommon.init();
        CommonBrewingRecipes.register();
        raiseVanillaAttributeCaps();
        FabricPartEntities.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricDragonAttributeReloadListener());
        FabricServerEvents.init();
        FabricLootTableModifier.register();

        CommonModEvents.registerEntityAttributes(SaintsDragonsFabric::registerDefaultAttributes);

        CommonModEvents.registerSpawnPlacements(SpawnPlacements::register);
        FabricDragonSpawns.register();

        CommonModEvents.registerCreativeTabEntries((tab, itemSupplier) ->
                ItemGroupEvents.modifyEntriesEvent(tab)
                        .register(entries -> entries.accept(itemSupplier.get())));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommonModEvents.registerCommands(dispatcher));
    }

    private static void raiseVanillaAttributeCaps() {
        raiseAttributeCap(Attributes.MAX_HEALTH, "MAX_HEALTH");
        raiseAttributeCap(Attributes.ARMOR, "ARMOR");
    }

    private static void raiseAttributeCap(net.minecraft.world.entity.ai.attributes.Attribute attribute, String name) {
        if (!(attribute instanceof RangedAttribute ranged)) {
            return;
        }

        RangedAttributeAccessor accessor = (RangedAttributeAccessor) ranged;
        if (accessor.saintsdragons$getMaxValue() >= ATTRIBUTE_CAP) {
            return;
        }

        accessor.saintsdragons$setMaxValue(ATTRIBUTE_CAP);
        SaintsDragonsCommon.LOGGER.info("Raised {} attribute cap to {}", name, ATTRIBUTE_CAP);
    }

    private static <T extends LivingEntity> void registerDefaultAttributes(
            EntityType<? extends T> type,
            AttributeSupplier.Builder builder
    ) {
        // Avoid IDE contract false-positives on wildcard capture in inline lambdas.
        FabricDefaultAttributeRegistry.register(type, builder.build());
    }
}
