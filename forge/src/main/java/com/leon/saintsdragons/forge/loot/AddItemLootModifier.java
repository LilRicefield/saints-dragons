package com.leon.saintsdragons.forge.loot;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AddItemLootModifier extends LootModifier {
    public static final Codec<AddItemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
            .and(Codec.INT.fieldOf("count").forGetter(m -> m.count))
            .and(Codec.DOUBLE.optionalFieldOf("chance", 1.0D).forGetter(m -> m.chance))
            .and(ResourceLocation.CODEC.optionalFieldOf("dragon_id").forGetter(m -> Optional.ofNullable(m.dragonId)))
            .and(Codec.STRING.optionalFieldOf("config_key").forGetter(m -> Optional.ofNullable(m.configKey)))
            .apply(inst, AddItemLootModifier::new));

    private final Item item;
    private final int count;
    private final double chance;
    private final ResourceLocation dragonId;
    private final String configKey;

    public AddItemLootModifier(LootItemCondition[] conditions,
                               Item item,
                               int count,
                               double chance,
                               Optional<ResourceLocation> dragonId,
                               Optional<String> configKey) {
        super(conditions);
        this.item = item;
        this.count = count;
        this.chance = chance;
        this.dragonId = dragonId.orElse(null);
        this.configKey = configKey.orElse(null);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        double resolvedChance = resolveChance();
        if (resolvedChance <= 0.0D || context.getRandom().nextDouble() >= resolvedChance) {
            return generatedLoot;
        }
        generatedLoot.add(new ItemStack(item, count));
        return generatedLoot;
    }

    private double resolveChance() {
        double resolved = chance;
        if (dragonId != null && configKey != null) {
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(dragonId);
            resolved = config.extraDouble(configKey, chance);
        }
        return Mth.clamp(resolved, 0.0D, 1.0D);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
