package com.leon.saintsdragons.forge.loot;

import com.leon.saintsdragons.server.loot.DragonChestLootRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class DragonChestLootModifier extends LootModifier {
    public static final Codec<DragonChestLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst).apply(inst, DragonChestLootModifier::new));

    public DragonChestLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTable = context.getQueriedLootTableId();
        for (DragonChestLootRegistry.Entry entry : DragonChestLootRegistry.entriesFor(lootTable)) {
            double chance = entry.resolvedChance();
            if (chance > 0.0D && context.getRandom().nextDouble() < chance) {
                generatedLoot.add(entry.createStack());
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
