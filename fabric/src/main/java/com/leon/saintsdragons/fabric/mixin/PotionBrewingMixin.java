package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public final class PotionBrewingMixin {
    private static boolean isCustomRecipeIngredient(ItemStack ingredient) {
        return ingredient.is(ModItems.VARASUCHUS_SCALE.get()) || ingredient.is(ModItems.IGNIVORUS_TOOTH.get());
    }

    private static boolean isSaintsDragonsPotion(ItemStack stack) {
        return stack.is(ModItems.POTION_OF_TIDEGUARD.get())
                || stack.is(ModItems.POTION_OF_SEARING.get())
                || PotionUtils.getPotion(stack) == ModPotions.VARASUCHUS_TIDEGUARD.get()
                || PotionUtils.getPotion(stack) == ModPotions.SEARING.get();
    }

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$allowCustomIngredients(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPotionIngredient", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$allowCustomPotionIngredients(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$blockAwkwardSplashAndLingering(
            ItemStack input,
            ItemStack ingredient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSaintsDragonsPotion(input)) {
            cir.setReturnValue(false);
            return;
        }

        if (!isCustomRecipeIngredient(ingredient)) {
            return;
        }

        if (PotionUtils.getPotion(input) != Potions.AWKWARD) {
            return;
        }

        if (!input.is(Items.POTION)) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "hasPotionMix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$blockVanillaPotionFamilyConversions(
            ItemStack input,
            ItemStack ingredient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(ingredient)
                && input.is(Items.POTION)
                && PotionUtils.getPotion(input) == Potions.AWKWARD) {
            cir.setReturnValue(true);
            return;
        }

        if (isSaintsDragonsPotion(input)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$customPotionItemOutput(
            ItemStack input,
            ItemStack ingredient,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (isSaintsDragonsPotion(input)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (PotionUtils.getPotion(input) != Potions.AWKWARD || !input.is(Items.POTION)) {
            return;
        }

        if (ingredient.is(ModItems.VARASUCHUS_SCALE.get())) {
            ItemStack output = new ItemStack(ModItems.POTION_OF_TIDEGUARD.get());
            PotionUtils.setPotion(output, ModPotions.VARASUCHUS_TIDEGUARD.get());
            cir.setReturnValue(output);
            return;
        }

        if (ingredient.is(ModItems.IGNIVORUS_TOOTH.get())) {
            ItemStack output = new ItemStack(ModItems.POTION_OF_SEARING.get());
            PotionUtils.setPotion(output, ModPotions.SEARING.get());
            cir.setReturnValue(output);
        }
    }
}
