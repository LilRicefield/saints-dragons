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
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int requiredHeatLevel;
    private final int processingTime;
    private final int priority;

    public DraconicCrucibleShapedRecipe(ResourceLocation id, int width, int height,
                                        NonNullList<Ingredient> ingredients,
                                        ItemStack result, int requiredHeatLevel, int processingTime,
                                        int priority) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.requiredHeatLevel = requiredHeatLevel;
        this.processingTime = processingTime;
        this.priority = priority;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        if (container.getContainerSize() < GRID_SIZE) {
            return false;
        }
        return findMatch(container) != null;
    }

    public boolean consumeInputs(Container container) {
        Match match = findMatch(container);
        if (match == null) {
            return false;
        }
        for (int row = 0; row < this.height; row++) {
            for (int column = 0; column < this.width; column++) {
                int ingredientColumn = match.mirrored ? this.width - column - 1 : column;
                if (!this.ingredients.get(ingredientColumn + row * this.width).isEmpty()) {
                    int slot = match.offsetX + column + (match.offsetY + row) * 3;
                    container.removeItem(slot, 1);
                }
            }
        }
        return true;
    }

    @Nullable
    private Match findMatch(Container container) {
        for (int offsetY = 0; offsetY <= 3 - this.height; offsetY++) {
            for (int offsetX = 0; offsetX <= 3 - this.width; offsetX++) {
                if (matchesAt(container, offsetX, offsetY, false)) {
                    return new Match(offsetX, offsetY, false);
                }
                if (matchesAt(container, offsetX, offsetY, true)) {
                    return new Match(offsetX, offsetY, true);
                }
            }
        }
        return null;
    }

    private boolean matchesAt(Container container, int offsetX, int offsetY, boolean mirrored) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int recipeX = column - offsetX;
                int recipeY = row - offsetY;
                Ingredient expected = Ingredient.EMPTY;
                if (recipeX >= 0 && recipeX < this.width && recipeY >= 0 && recipeY < this.height) {
                    int ingredientX = mirrored ? this.width - recipeX - 1 : recipeX;
                    expected = this.ingredients.get(ingredientX + recipeY * this.width);
                }

                ItemStack actual = container.getItem(column + row * 3);
                if (expected.isEmpty() ? !actual.isEmpty() : !expected.test(actual)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int requiredHeatLevel() {
        return this.requiredHeatLevel;
    }

    public int processingTime() {
        return this.processingTime;
    }

    public int priority() {
        return this.priority;
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
        return width >= this.width && height >= this.height;
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
            int height = pattern.length;
            int width = pattern[0].length();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            int occupiedSlots = 0;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    char symbol = pattern[row].charAt(column);
                    if (symbol != ' ') {
                        Ingredient ingredient = key.get(symbol);
                        if (ingredient == null) {
                            throw new IllegalArgumentException("Pattern references undefined symbol '" + symbol + "'");
                        }
                        ingredients.set(column + row * width, ingredient);
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
            int priority = DraconicCrucibleSmeltingRecipe.readPriority(json);
            return new DraconicCrucibleShapedRecipe(
                    id, width, height, ingredients, result, requiredHeatLevel, processingTime, priority);
        }

        @Override
        public @Nullable DraconicCrucibleShapedRecipe fromNetwork(@NotNull ResourceLocation id,
                                                                  @NotNull FriendlyByteBuf buffer) {
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int slot = 0; slot < ingredients.size(); slot++) {
                ingredients.set(slot, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            int requiredHeatLevel = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int priority = buffer.readInt();
            return new DraconicCrucibleShapedRecipe(
                    id, width, height, ingredients, result, requiredHeatLevel, processingTime, priority);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer,
                              @NotNull DraconicCrucibleShapedRecipe recipe) {
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.requiredHeatLevel);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeInt(recipe.priority);
        }

        private static String[] readPattern(JsonObject json) {
            var patternJson = GsonHelper.getAsJsonArray(json, "pattern");
            if (patternJson.isEmpty() || patternJson.size() > 3) {
                throw new IllegalArgumentException("Draconic Crucible patterns must contain between 1 and 3 rows");
            }
            String[] pattern = new String[patternJson.size()];
            int width = -1;
            for (int row = 0; row < pattern.length; row++) {
                pattern[row] = GsonHelper.convertToString(patternJson.get(row), "pattern[" + row + "]");
                if (pattern[row].isEmpty() || pattern[row].length() > 3) {
                    throw new IllegalArgumentException("Each Draconic Crucible pattern row must contain between 1 and 3 characters");
                }
                if (width == -1) {
                    width = pattern[row].length();
                } else if (pattern[row].length() != width) {
                    throw new IllegalArgumentException("All Draconic Crucible pattern rows must have the same width");
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

    private record Match(int offsetX, int offsetY, boolean mirrored) {
    }
}
