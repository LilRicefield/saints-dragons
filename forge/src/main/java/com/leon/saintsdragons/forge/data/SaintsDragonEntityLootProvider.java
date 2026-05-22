package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.stream.Stream;

public final class SaintsDragonEntityLootProvider extends EntityLootSubProvider {
    public SaintsDragonEntityLootProvider() {
        super(FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    public void generate() {
        add(ModEntities.CINDERVANE.get(), LootTable.lootTable());
        add(ModEntities.NULLJAW.get(), LootTable.lootTable());
        add(ModEntities.RAEVYX.get(), LootTable.lootTable());
        add(ModEntities.STEGONAUT.get(), LootTable.lootTable());
        add(ModEntities.VARASUCHUS.get(), LootTable.lootTable());
        add(ModEntities.IGNIVORUS.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(LootItem.lootTableItem(ModItems.IGNIVORUS_TOOTH.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .setBonusRolls(ConstantValue.exactly(0))
                        .when(LootItemRandomChanceCondition.randomChance(0.90F))
                        .add(LootItem.lootTableItem(ModItems.IGNIVORUS_HEART.get()))));

        add(ModEntities.VOLITANS.get(), LootTable.lootTable()
                .withPool(fishPool(Items.SALMON))
                .withPool(fishPool(Items.COD))
                .withPool(fishPool(Items.TROPICAL_FISH))
                .withPool(fishPool(Items.PUFFERFISH)));
    }

    private static LootPool.Builder fishPool(net.minecraft.world.level.ItemLike item) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .setBonusRolls(ConstantValue.exactly(0))
                .when(LootItemRandomChanceCondition.randomChance(0.40F))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(
                ModEntities.CINDERVANE.get(),
                ModEntities.IGNIVORUS.get(),
                ModEntities.NULLJAW.get(),
                ModEntities.RAEVYX.get(),
                ModEntities.STEGONAUT.get(),
                ModEntities.VARASUCHUS.get(),
                ModEntities.VOLITANS.get()
        );
    }
}
