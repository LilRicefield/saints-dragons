package com.leon.saintsdragons.fabric.loot;

import com.leon.saintsdragons.common.registry.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class FabricLootTableModifier {
    private static final ResourceLocation PILLAGER_OUTPOST_CHEST =
            new ResourceLocation("minecraft", "chests/pillager_outpost");
    private static final ResourceLocation SHIPWRECK_TREASURE_CHEST =
            new ResourceLocation("minecraft", "chests/shipwreck_treasure");
    private static final ResourceLocation ANCIENT_CITY_CHEST =
            new ResourceLocation("minecraft", "chests/ancient_city");
    private static final ResourceLocation BASTION_TREASURE_CHEST =
            new ResourceLocation("minecraft", "chests/bastion_treasure");
    private static final ResourceLocation NETHER_BRIDGE_CHEST =
            new ResourceLocation("minecraft", "chests/nether_bridge");

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            // Add Raevyx Egg to Pillager Outpost chests (20% chance)
            if (PILLAGER_OUTPOST_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.2f))
                        .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }

            // Add Raevyx Egg to Shipwreck Treasure chests (15% chance)
            if (SHIPWRECK_TREASURE_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }

            // Add Raevyx Egg to Ancient City chests (15% chance)
            if (ANCIENT_CITY_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }

            // Add Ignivorus Egg to Bastion Treasure chests (15% chance)
            if (BASTION_TREASURE_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }

            // Add Ignivorus Egg to Nether Fortress chests (15% chance)
            if (NETHER_BRIDGE_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }

            // Add Ignivorus Egg to Ancient City chests (10% chance)
            if (ANCIENT_CITY_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.10f))
                        .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
