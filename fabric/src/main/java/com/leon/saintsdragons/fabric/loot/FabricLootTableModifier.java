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

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            // Add Raevyx Egg to Pillager Outpost chests
            if (PILLAGER_OUTPOST_CHEST.equals(id)) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(1f)) // 20% chance
                        .add(LootItem.lootTableItem(ModItems.RAEVYX_EGG.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
