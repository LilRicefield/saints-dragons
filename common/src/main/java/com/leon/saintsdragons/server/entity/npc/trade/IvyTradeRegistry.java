package com.leon.saintsdragons.server.entity.npc.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IvyTradeRegistry {
    private static final int HEARTY_MEAL_EGG_COUNT = 4;
    private static final int HEARTY_MEAL_SALMON_COUNT = 4;
    private static final int HEARTY_MEAL_OUTPUT_COUNT = 6;
    private static final int HEARTY_MEAL_MAX_USES = 9999;
    private static final List<VillagerTrades.ItemListing> DEFAULT_TRADES = List.of(
            (trader, random) -> createIgnivorusEggTrade(random),
            (trader, random) -> createRaevyxEggTrade(random),
            (trader, random) -> createVarasuchusEggTrade(random),
            (trader, random) -> createCindervaneEggTrade(random),
            (trader, random) -> createStegonautEggTrade(random)
    );

    private static volatile List<VillagerTrades.ItemListing> datapackTrades = List.of();
    private static volatile boolean replaceDefaults;

    private IvyTradeRegistry() {
    }

    public static void fillOffers(AbstractVillager trader, RandomSource random, MerchantOffers offers) {
        List<VillagerTrades.ItemListing> listings = new ArrayList<>();
        if (!replaceDefaults) {
            listings.addAll(DEFAULT_TRADES);
        }
        listings.addAll(datapackTrades);
        for (VillagerTrades.ItemListing listing : listings) {
            MerchantOffer offer = listing.getOffer(trader, random);
            if (offer != null) {
                offers.add(offer);
            }
        }
        addFixedOffers(offers);
    }

    static void replaceDatapackTrades(boolean replace, List<VillagerTrades.ItemListing> trades) {
        replaceDefaults = replace;
        datapackTrades = List.copyOf(trades);
    }

    static List<VillagerTrades.ItemListing> parseTrades(ResourceLocation fileId, JsonObject root) {
        List<VillagerTrades.ItemListing> result = new ArrayList<>();
        JsonArray trades = GsonHelper.getAsJsonArray(root, "trades");
        for (int i = 0; i < trades.size(); i++) {
            JsonObject trade = GsonHelper.convertToJsonObject(trades.get(i), fileId + " trade " + i);
            result.add(parseTrade(fileId, trade));
        }
        return result;
    }

    static boolean shouldReplace(JsonObject root) {
        return GsonHelper.getAsBoolean(root, "replace", false);
    }

    private static VillagerTrades.ItemListing parseTrade(ResourceLocation fileId, JsonObject trade) {
        StackFactory costA = parseStack(GsonHelper.getAsJsonObject(trade, "cost_a"), fileId);
        StackFactory costB = trade.has("cost_b") ? parseStack(GsonHelper.getAsJsonObject(trade, "cost_b"), fileId) : StackFactory.EMPTY;
        ResultFactory result = trade.has("results")
                ? parseResultPool(GsonHelper.getAsJsonArray(trade, "results"), fileId)
                : parseStack(GsonHelper.getAsJsonObject(trade, "result"), fileId);
        int maxUses = GsonHelper.getAsInt(trade, "max_uses", 5);
        int xp = GsonHelper.getAsInt(trade, "xp", 0);
        float priceMultiplier = GsonHelper.getAsFloat(trade, "price_multiplier", 0.05F);
        return (trader, random) -> {
            ItemStack secondCost = costB.create(random);
            if (secondCost.isEmpty()) {
                return new MerchantOffer(costA.create(random), result.create(random), maxUses, xp, priceMultiplier);
            }
            return new MerchantOffer(costA.create(random), secondCost, result.create(random), maxUses, xp, priceMultiplier);
        };
    }

    private static ResultPool parseResultPool(JsonArray array, ResourceLocation fileId) {
        List<WeightedResult> entries = new ArrayList<>();
        int totalWeight = 0;
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = GsonHelper.convertToJsonObject(array.get(i), fileId + " result " + i);
            int weight = Math.max(1, GsonHelper.getAsInt(entry, "weight", 1));
            totalWeight += weight;
            entries.add(new WeightedResult(parseStack(entry, fileId), weight));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException(fileId + " has an empty results pool");
        }
        return new ResultPool(entries, totalWeight);
    }

    private static StackFactory parseStack(JsonObject object, ResourceLocation fileId) {
        ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(object, "item"));
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Unknown item " + itemId + " in " + fileId);
        }
        CountRange count = parseCount(object);
        List<EnchantmentEntry> enchantments = parseEnchantments(object, fileId);
        return random -> {
            ItemStack stack = new ItemStack(item.get(), count.roll(random));
            for (EnchantmentEntry enchantment : enchantments) {
                if (random.nextFloat() <= enchantment.chance()) {
                    stack.enchant(enchantment.enchantment(), enchantment.level());
                }
            }
            return stack;
        };
    }

    private static CountRange parseCount(JsonObject object) {
        if (!object.has("count")) {
            return new CountRange(1, 1);
        }
        JsonElement element = object.get("count");
        if (element.isJsonObject()) {
            JsonObject count = element.getAsJsonObject();
            int min = GsonHelper.getAsInt(count, "min", 1);
            int max = GsonHelper.getAsInt(count, "max", min);
            return new CountRange(min, max);
        }
        int count = GsonHelper.convertToInt(element, "count");
        return new CountRange(count, count);
    }

    private static List<EnchantmentEntry> parseEnchantments(JsonObject object, ResourceLocation fileId) {
        if (!object.has("enchantments")) {
            return List.of();
        }
        List<EnchantmentEntry> result = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(object, "enchantments");
        for (JsonElement element : array) {
            JsonObject enchantmentJson = GsonHelper.convertToJsonObject(element, fileId + " enchantment");
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(enchantmentJson, "id"));
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.getOptional(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown enchantment " + id + " in " + fileId));
            int level = GsonHelper.getAsInt(enchantmentJson, "level", 1);
            float chance = GsonHelper.getAsFloat(enchantmentJson, "chance", 1.0F);
            result.add(new EnchantmentEntry(enchantment, level, chance));
        }
        return List.copyOf(result);
    }

    private static void addFixedOffers(MerchantOffers offers) {
        if (!hasHeartyMealOffer(offers)) {
            offers.add(createHeartyMealOffer());
        }
    }

    private static boolean hasHeartyMealOffer(MerchantOffers offers) {
        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA();
            ItemStack costB = offer.getCostB();
            ItemStack result = offer.getResult();
            if (costA.is(Items.EGG)
                    && costA.getCount() == HEARTY_MEAL_EGG_COUNT
                    && costB.is(Items.SALMON)
                    && costB.getCount() == HEARTY_MEAL_SALMON_COUNT
                    && result.is(ModItems.HEARTY_DRAGON_MEAL.get())
                    && result.getCount() == HEARTY_MEAL_OUTPUT_COUNT) {
                return true;
            }
        }
        return false;
    }

    private static MerchantOffer createHeartyMealOffer() {
        ItemStack eggs = new ItemStack(Items.EGG, HEARTY_MEAL_EGG_COUNT);
        ItemStack salmon = new ItemStack(Items.SALMON, HEARTY_MEAL_SALMON_COUNT);
        ItemStack result = new ItemStack(ModItems.HEARTY_DRAGON_MEAL.get(), HEARTY_MEAL_OUTPUT_COUNT);
        return new MerchantOffer(eggs, salmon, result, HEARTY_MEAL_MAX_USES, 0, 0.0F);
    }

    private static MerchantOffer createIgnivorusEggTrade(RandomSource random) {
        ItemStack result = createIgnivorusReward(random);
        return new MerchantOffer(new ItemStack(ModItems.IGNIVORUS_EGG.get()), result, 3, 5, 0.05F);
    }

    private static ItemStack createIgnivorusReward(RandomSource random) {
        int roll = random.nextInt(8);
        return switch (roll) {
            case 0 -> enchantedNetheriteSword(random);
            case 1 -> enchantedNetheriteAxe(random);
            case 2 -> enchantedNetheritePickaxe(random);
            case 3 -> enchantedNetheriteArmor(random, Items.NETHERITE_CHESTPLATE);
            case 4 -> enchantedNetheriteArmor(random, Items.NETHERITE_LEGGINGS);
            case 5 -> enchantedNetheriteArmor(random, Items.NETHERITE_HELMET);
            case 6 -> enchantedNetheriteArmor(random, Items.NETHERITE_BOOTS);
            default -> enchantedNetheriteHoe(random);
        };
    }

    private static ItemStack enchantedNetheriteSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35F) {
            stack.enchant(Enchantments.FIRE_ASPECT, 2);
        }
        if (random.nextFloat() < 0.2F) {
            stack.enchant(Enchantments.MOB_LOOTING, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_AXE);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.4F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        }
        return stack;
    }

    private static ItemStack enchantedNetheritePickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35F) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.25F) {
            stack.enchant(Enchantments.THORNS, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_HOE);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.45F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        }
        return stack;
    }

    private static MerchantOffer createRaevyxEggTrade(RandomSource random) {
        ItemStack result = createRaevyxReward(random);
        return new MerchantOffer(new ItemStack(ModItems.RAEVYX_EGG.get()), result, 5, 5, 0.05F);
    }

    private static ItemStack createRaevyxReward(RandomSource random) {
        int roll = random.nextInt(8);
        return switch (roll) {
            case 0 -> enchantedDiamondSword(random);
            case 1 -> enchantedDiamondAxe(random);
            case 2 -> enchantedDiamondPickaxe(random);
            case 3 -> enchantedDiamondArmor(random, Items.DIAMOND_CHESTPLATE);
            case 4 -> enchantedDiamondArmor(random, Items.DIAMOND_LEGGINGS);
            case 5 -> enchantedDiamondArmor(random, Items.DIAMOND_HELMET);
            case 6 -> enchantedDiamondArmor(random, Items.DIAMOND_BOOTS);
            default -> enchantedDiamondHoe(random);
        };
    }

    private static ItemStack enchantedDiamondSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.3F) {
            stack.enchant(Enchantments.FIRE_ASPECT, 2);
        }
        if (random.nextFloat() < 0.2F) {
            stack.enchant(Enchantments.MOB_LOOTING, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_AXE);
        stack.enchant(Enchantments.SHARPNESS, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondPickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.25F) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.2F) {
            stack.enchant(Enchantments.THORNS, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_HOE);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.45F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        }
        return stack;
    }

    private static MerchantOffer createCindervaneEggTrade(RandomSource random) {
        ItemStack result = createCindervaneReward(random);
        return new MerchantOffer(new ItemStack(ModItems.CINDERVANE_EGG.get()), result, 8, 5, 0.05F);
    }

    private static ItemStack createCindervaneReward(RandomSource random) {
        int roll = random.nextInt(12);
        return switch (roll) {
            case 0 -> enchantedIronSword(random);
            case 1 -> enchantedIronAxe(random);
            case 2 -> enchantedIronPickaxe(random);
            case 3 -> enchantedIronArmor(random, Items.IRON_CHESTPLATE);
            case 4 -> enchantedIronArmor(random, Items.IRON_LEGGINGS);
            case 5 -> enchantedIronArmor(random, Items.IRON_HELMET);
            case 6 -> enchantedIronArmor(random, Items.IRON_BOOTS);
            case 7 -> new ItemStack(Items.BLAZE_ROD, 4 + random.nextInt(5));
            case 8 -> new ItemStack(Items.MAGMA_CREAM, 8 + random.nextInt(9));
            case 9 -> new ItemStack(Items.FIRE_CHARGE, 12 + random.nextInt(13));
            case 10 -> new ItemStack(Items.ENDER_PEARL, 4 + random.nextInt(5));
            default -> enchantedIronHoe(random);
        };
    }

    private static ItemStack enchantedIronSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.3F) {
            stack.enchant(Enchantments.FIRE_ASPECT, 1);
        }
        if (random.nextFloat() < 0.15F) {
            stack.enchant(Enchantments.MOB_LOOTING, 2);
        }
        return stack;
    }

    private static ItemStack enchantedIronAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_AXE);
        stack.enchant(Enchantments.SHARPNESS, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.35F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        }
        return stack;
    }

    private static ItemStack enchantedIronPickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.3F) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else if (random.nextFloat() < 0.4F) {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 2);
        }
        return stack;
    }

    private static ItemStack enchantedIronArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.2F) {
            stack.enchant(Enchantments.THORNS, 1);
        }
        return stack;
    }

    private static ItemStack enchantedIronHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_HOE);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.4F) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        }
        return stack;
    }

    private static MerchantOffer createVarasuchusEggTrade(RandomSource random) {
        ItemStack result = createVarasuchusReward(random);
        return new MerchantOffer(new ItemStack(ModItems.VARASUCHUS_EGG.get()), result, 6, 5, 0.05F);
    }

    private static ItemStack createVarasuchusReward(RandomSource random) {
        int roll = random.nextInt(11);
        return switch (roll) {
            case 0 -> new ItemStack(Items.DIAMOND_SWORD);
            case 1 -> new ItemStack(Items.DIAMOND_AXE);
            case 2 -> new ItemStack(Items.DIAMOND_PICKAXE);
            case 3 -> new ItemStack(Items.DIAMOND_SHOVEL);
            case 4 -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case 5 -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case 6 -> new ItemStack(Items.DIAMOND_HELMET);
            case 7 -> new ItemStack(Items.DIAMOND_BOOTS);
            case 8 -> new ItemStack(Items.DIAMOND, 3 + random.nextInt(4));
            case 9 -> new ItemStack(Items.TRIDENT);
            default -> enchantedTrident(random);
        };
    }

    private static ItemStack enchantedTrident(RandomSource random) {
        ItemStack stack = new ItemStack(Items.TRIDENT);
        int enchantRoll = random.nextInt(4);
        switch (enchantRoll) {
            case 0 -> stack.enchant(Enchantments.IMPALING, 3);
            case 1 -> stack.enchant(Enchantments.LOYALTY, 2);
            case 2 -> stack.enchant(Enchantments.RIPTIDE, 2);
            default -> stack.enchant(Enchantments.UNBREAKING, 2);
        }
        return stack;
    }

    private static MerchantOffer createStegonautEggTrade(RandomSource random) {
        ItemStack result = createStegonautReward(random);
        return new MerchantOffer(new ItemStack(ModItems.STEGONAUT_EGG.get()), result, 12, 5, 0.05F);
    }

    private static ItemStack createStegonautReward(RandomSource random) {
        int roll = random.nextInt(12);
        return switch (roll) {
            case 0 -> new ItemStack(Items.IRON_INGOT, 16 + random.nextInt(17));
            case 1 -> new ItemStack(Items.COAL, 16 + random.nextInt(33));
            case 2 -> new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(9));
            case 3 -> new ItemStack(Items.REDSTONE, 16 + random.nextInt(9));
            case 4 -> new ItemStack(Items.LAPIS_LAZULI, 8 + random.nextInt(9));
            case 5 -> new ItemStack(Items.COOKED_BEEF, 16 + random.nextInt(17));
            case 6 -> new ItemStack(Items.GOLDEN_CARROT, 8 + random.nextInt(9));
            case 7 -> new ItemStack(Items.BREAD, 24 + random.nextInt(25));
            case 8 -> new ItemStack(Items.ARROW, 32 + random.nextInt(33));
            case 9 -> new ItemStack(Items.TORCH, 1 + random.nextInt(64));
            case 10 -> new ItemStack(Items.SCAFFOLDING, 32 + random.nextInt(33));
            default -> new ItemStack(Items.EMERALD, 4 + random.nextInt(5));
        };
    }

    private interface ResultFactory {
        ItemStack create(RandomSource random);
    }

    private interface StackFactory extends ResultFactory {
        StackFactory EMPTY = random -> ItemStack.EMPTY;
    }

    private record CountRange(int min, int max) {
        private CountRange {
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Invalid count range " + min + ".." + max);
            }
        }

        private int roll(RandomSource random) {
            if (min == max) {
                return min;
            }
            return min + random.nextInt(max - min + 1);
        }
    }

    private record EnchantmentEntry(Enchantment enchantment, int level, float chance) {
    }

    private record WeightedResult(StackFactory factory, int weight) {
    }

    private record ResultPool(List<WeightedResult> entries, int totalWeight) implements ResultFactory {
        @Override
        public ItemStack create(RandomSource random) {
            int roll = random.nextInt(totalWeight);
            for (WeightedResult entry : entries) {
                roll -= entry.weight();
                if (roll < 0) {
                    return entry.factory().create(random);
                }
            }
            return entries.get(entries.size() - 1).factory().create(random);
        }
    }
}
