package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.common.registry.ModRecipes;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {
    @Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$categorizeCrucibleRecipes(
            Recipe<?> recipe,
            CallbackInfoReturnable<RecipeBookCategories> callback) {
        if (recipe.getType() == ModRecipes.DRACONIC_CRUCIBLE_SHAPED_TYPE.get()
                || recipe.getType() == ModRecipes.DRACONIC_CRUCIBLE_SMELTING_TYPE.get()) {
            callback.setReturnValue(RecipeBookCategories.UNKNOWN);
        }
    }
}
