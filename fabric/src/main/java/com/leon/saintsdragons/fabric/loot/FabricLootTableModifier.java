package com.leon.saintsdragons.fabric.loot;

import com.leon.saintsdragons.server.loot.DragonChestLootRegistry;
import com.leon.saintsdragons.server.loot.DragonChestLootReloadListener;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class FabricLootTableModifier {
    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            DragonChestLootReloadListener.ensureLoaded(resourceManager);
            for (DragonChestLootRegistry.Entry entry : DragonChestLootRegistry.entriesFor(id)) {
                double chance = entry.resolvedChance();
                if (chance <= 0.0D) {
                    continue;
                }
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance((float) chance))
                        .add(LootItem.lootTableItem(entry.item())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(entry.count()))));

                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
