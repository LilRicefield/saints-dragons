package com.leon.saintsdragons.common.recipe;

import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DraconicCrucibleSmeltingRecipe(
        ResourceLocation id,
        Ingredient ingredient,
        ItemStack result,
        int requiredHeatLevel,
        int processingTime,
        int priority
) implements Recipe<Container> {
    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return container.getContainerSize() > 0 && this.ingredient.test(container.getItem(0));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return this.result;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DRACONIC_CRUCIBLE_SMELTING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.DRACONIC_CRUCIBLE_SMELTING_TYPE.get();
    }

    @Override
    public @NotNull ItemStack getToastSymbol() {
        return new ItemStack(ModItems.DRACONIC_CRUCIBLE.get());
    }

    public static final class Serializer implements RecipeSerializer<DraconicCrucibleSmeltingRecipe> {
        @Override
        public @NotNull DraconicCrucibleSmeltingRecipe fromJson(@NotNull ResourceLocation id,
                                                                 @NotNull JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int requiredHeatLevel = readHeatLevel(json);
            int processingTime = readProcessingTime(json);
            int priority = readPriority(json);
            return new DraconicCrucibleSmeltingRecipe(
                    id, ingredient, result, requiredHeatLevel, processingTime, priority);
        }

        @Override
        public @Nullable DraconicCrucibleSmeltingRecipe fromNetwork(@NotNull ResourceLocation id,
                                                                    @NotNull FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int requiredHeatLevel = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int priority = buffer.readInt();
            return new DraconicCrucibleSmeltingRecipe(
                    id, ingredient, result, requiredHeatLevel, processingTime, priority);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer,
                              @NotNull DraconicCrucibleSmeltingRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.requiredHeatLevel);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeInt(recipe.priority);
        }
    }

    static int readHeatLevel(JsonObject json) {
        int level = GsonHelper.getAsInt(json, "required_heat_level", 1);
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("required_heat_level must be between 1 and 3");
        }
        return level;
    }

    static int readProcessingTime(JsonObject json) {
        int processingTime = GsonHelper.getAsInt(json, "processing_time", 200);
        if (processingTime <= 0) {
            throw new IllegalArgumentException("processing_time must be greater than zero");
        }
        return processingTime;
    }

    static int readPriority(JsonObject json) {
        return GsonHelper.getAsInt(json, "priority", 0);
    }
}
