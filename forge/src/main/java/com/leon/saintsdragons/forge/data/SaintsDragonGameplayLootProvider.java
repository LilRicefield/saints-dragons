package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public final class SaintsDragonGameplayLootProvider implements LootTableSubProvider {
    private final boolean grooming;

    private SaintsDragonGameplayLootProvider(boolean grooming) {
        this.grooming = grooming;
    }

    public static SaintsDragonGameplayLootProvider entityTables() {
        return new SaintsDragonGameplayLootProvider(false);
    }

    public static SaintsDragonGameplayLootProvider groomingTables() {
        return new SaintsDragonGameplayLootProvider(true);
    }

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        if (grooming) {
            generateGrooming(output);
            return;
        }
        generateEntityContext(output);
    }

    private static void generateEntityContext(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        output.accept(DragonLootTables.CINDERVANE_FEMALE_DEATH, chanceTable(ModItems.CINDERVANE_EGG.get(), 0.12F));
        output.accept(DragonLootTables.IGNIVORUS_FEMALE_DEATH, chanceTable(ModItems.IGNIVORUS_EGG.get(), 0.12F));
        output.accept(DragonLootTables.RAEVYX_FEMALE_DEATH, chanceTable(ModItems.RAEVYX_EGG.get(), 0.12F));
        output.accept(DragonLootTables.STEGONAUT_FEMALE_DEATH, chanceTable(ModItems.STEGONAUT_EGG.get(), 0.12F));
        output.accept(DragonLootTables.VARASUCHUS_FEMALE_DEATH, chanceTable(ModItems.VARASUCHUS_EGG.get(), 0.12F));
        output.accept(DragonLootTables.VOLITANS_FEMALE_DEATH, chanceTable(ModItems.VOLITANS_EGG.get(), 0.12F));
        output.accept(DragonLootTables.IGNIVORUS_HIT, chanceTable(ModItems.IGNIVORUS_TOOTH.get(), 0.12F));
        output.accept(DragonLootTables.VOLITANS_HIT, chanceTable(ModItems.VOLITANS_SPINE.get(), 0.30F));
    }

    private static void generateGrooming(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        output.accept(DragonLootTables.CINDERVANE_GROOMING, chanceCountTable(ModItems.CINDERVANE_SCALE.get(), 0.30F, 1, 1));
        output.accept(DragonLootTables.IGNIVORUS_GROOMING, chanceCountTable(ModItems.IGNIVORUS_SCALE.get(), 0.35F, 1, 2));
        output.accept(DragonLootTables.RAEVYX_GROOMING, chanceCountTable(ModItems.RAEVYX_SCALE.get(), 0.35F, 1, 2));
        output.accept(DragonLootTables.STEGONAUT_GROOMING, chanceCountTable(ModItems.STEGONAUT_SCALE.get(), 0.30F, 1, 2));
        output.accept(DragonLootTables.VARASUCHUS_GROOMING, chanceCountTable(ModItems.VARASUCHUS_SCALE.get(), 0.30F, 1, 2));
        output.accept(DragonLootTables.VOLITANS_GROOMING, chanceCountTable(ModItems.VOLITANS_SCALE.get(), 0.30F, 1, 2));
    }

    private static LootTable.Builder chanceTable(ItemLike item, float chance) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .setBonusRolls(ConstantValue.exactly(0))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(item)));
    }

    private static LootTable.Builder chanceCountTable(ItemLike item, float chance, int min, int max) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .setBonusRolls(ConstantValue.exactly(0))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))));
    }
}
