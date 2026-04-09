package com.leon.saintsdragons.fabric.loot;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
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
            DragonAttributeConfig raevyxConfig = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
            DragonAttributeConfig ignivorusConfig = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
            DragonAttributeConfig volitansConfig = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);

            double raevyxOutpostChance = clampChance(raevyxConfig.extraDouble("egg_loot_pillager_outpost", 0.2D));
            double raevyxAncientChance = clampChance(raevyxConfig.extraDouble("egg_loot_ancient_city", 0.15D));

            double ignivorusBastionChance = clampChance(ignivorusConfig.extraDouble("egg_loot_bastion_treasure", 0.15D));
            double ignivorusBridgeChance = clampChance(ignivorusConfig.extraDouble("egg_loot_nether_bridge", 0.15D));
            double ignivorusAncientChance = clampChance(ignivorusConfig.extraDouble("egg_loot_ancient_city", 0.10D));

            double volitansShipwreckChance = clampChance(volitansConfig.extraDouble("egg_loot_shipwreck_treasure", 0.12D));

            // Add Raevyx Egg to Pillager Outpost chests
            if (PILLAGER_OUTPOST_CHEST.equals(id)) {
                if (raevyxOutpostChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) raevyxOutpostChance))
                            .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }

            // Add Volitans Egg to Shipwreck Treasure chests
            if (SHIPWRECK_TREASURE_CHEST.equals(id)) {
                if (volitansShipwreckChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) volitansShipwreckChance))
                            .add(LootItem.lootTableItem(ModItems.VOLITANS_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }

            // Add Raevyx Egg to Ancient City chests
            if (ANCIENT_CITY_CHEST.equals(id)) {
                if (raevyxAncientChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) raevyxAncientChance))
                            .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }

            // Add Ignivorus Egg to Bastion Treasure chests
            if (BASTION_TREASURE_CHEST.equals(id)) {
                if (ignivorusBastionChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) ignivorusBastionChance))
                            .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }

            // Add Ignivorus Egg to Nether Fortress chests
            if (NETHER_BRIDGE_CHEST.equals(id)) {
                if (ignivorusBridgeChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) ignivorusBridgeChance))
                            .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }

            // Add Ignivorus Egg to Ancient City chests
            if (ANCIENT_CITY_CHEST.equals(id)) {
                if (ignivorusAncientChance > 0.0D) {
                    LootPool.Builder poolBuilder = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance((float) ignivorusAncientChance))
                            .add(LootItem.lootTableItem(ModItems.IGNIVORUS_EGG.get())
                                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                    tableBuilder.pool(poolBuilder.build());
                }
            }
        });
    }

    private static double clampChance(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }
}
