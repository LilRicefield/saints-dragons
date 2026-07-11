package com.leon.saintsdragons.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.registry.ModRecipes;
import net.minecraft.core.NonNullList;
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

import java.util.HashMap;
import java.util.Map;

public final class DraconicCrucibleShapedRecipe implements Recipe<Container> {
    public static final int GRID_SIZE = 9;

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int requiredHeatLevel;
    private final int processingTime;

    public DraconicCrucibleShapedRecipe(ResourceLocation id, NonNullList<Ingredient> ingredients,
                                        ItemStack result, int requiredHeatLevel, int processingTime) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.requiredHeatLevel = requiredHeatLevel;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        if (container.getContainerSize() < GRID_SIZE) {
            return false;
        }
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            Ingredient expected = this.ingredients.get(slot);
            ItemStack actual = container.getItem(slot);
            if (expected.isEmpty()) {
                if (!actual.isEmpty()) {
                    return false;
                }
            } else if (!expected.test(actual)) {
                return false;
            }
        }
        return true;
    }

    public void consumeInputs(Container container) {
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            if (!this.ingredients.get(slot).isEmpty()) {
                container.removeItem(slot, 1);
            }
        }
    }

    public int requiredHeatLevel() {
        return this.requiredHeatLevel;
    }

    public int processingTime() {
        return this.processingTime;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
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
        return ModRecipes.DRACONIC_CRUCIBLE_SHAPED_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.DRACONIC_CRUCIBLE_SHAPED_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<DraconicCrucibleShapedRecipe> {
        @Override
        public @NotNull DraconicCrucibleShapedRecipe fromJson(@NotNull ResourceLocation id,
                                                               @NotNull JsonObject json) {
            String[] pattern = readPattern(json);
            Map<Character, Ingredient> key = readKey(json);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(GRID_SIZE, Ingredient.EMPTY);
            int occupiedSlots = 0;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    char symbol = pattern[row].charAt(column);
                    if (symbol != ' ') {
                        Ingredient ingredient = key.get(symbol);
                        if (ingredient == null) {
                            throw new IllegalArgumentException("Pattern references undefined symbol '" + symbol + "'");
                        }
                        ingredients.set(column + row * 3, ingredient);
                        occupiedSlots++;
                    }
                }
            }
            if (occupiedSlots == 0) {
                throw new IllegalArgumentException("Draconic Crucible patterns must contain at least one ingredient");
            }

            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int requiredHeatLevel = DraconicCrucibleSmeltingRecipe.readHeatLevel(json);
            int processingTime = DraconicCrucibleSmeltingRecipe.readProcessingTime(json);
            return new DraconicCrucibleShapedRecipe(
                    id, ingredients, result, requiredHeatLevel, processingTime);
        }

        @Override
        public @Nullable DraconicCrucibleShapedRecipe fromNetwork(@NotNull ResourceLocation id,
                                                                  @NotNull FriendlyByteBuf buffer) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(GRID_SIZE, Ingredient.EMPTY);
            for (int slot = 0; slot < GRID_SIZE; slot++) {
                ingredients.set(slot, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            int requiredHeatLevel = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            return new DraconicCrucibleShapedRecipe(
                    id, ingredients, result, requiredHeatLevel, processingTime);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer,
                              @NotNull DraconicCrucibleShapedRecipe recipe) {
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.requiredHeatLevel);
            buffer.writeVarInt(recipe.processingTime);
        }

        private static String[] readPattern(JsonObject json) {
            var patternJson = GsonHelper.getAsJsonArray(json, "pattern");
            if (patternJson.size() != 3) {
                throw new IllegalArgumentException("Draconic Crucible patterns must contain exactly 3 rows");
            }
            String[] pattern = new String[3];
            for (int row = 0; row < 3; row++) {
                pattern[row] = GsonHelper.convertToString(patternJson.get(row), "pattern[" + row + "]");
                if (pattern[row].length() != 3) {
                    throw new IllegalArgumentException("Each Draconic Crucible pattern row must be exactly 3 characters");
                }
            }
            return pattern;
        }

        private static Map<Character, Ingredient> readKey(JsonObject json) {
            JsonObject keyJson = GsonHelper.getAsJsonObject(json, "key");
            Map<Character, Ingredient> key = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : keyJson.entrySet()) {
                if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
                    throw new IllegalArgumentException("Recipe key symbols must be one non-space character");
                }
                key.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
            }
            return key;
        }
    }
}
