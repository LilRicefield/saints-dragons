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
        return ingredient.is(ModItems.NULLJAW_SCALE.get()) || ingredient.is(ModItems.IGNIVORUS_TOOTH.get());
    }

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$blockAwkwardSplashAndLingering(
            ItemStack input,
            ItemStack ingredient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!isCustomRecipeIngredient(ingredient)) {
            return;
        }

        if (PotionUtils.getPotion(input) != Potions.AWKWARD) {
            return;
        }

        if (!input.is(Items.POTION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$customPotionItemOutput(
            ItemStack ingredient,
            ItemStack input,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (PotionUtils.getPotion(input) != Potions.AWKWARD || !input.is(Items.POTION)) {
            return;
        }

        if (ingredient.is(ModItems.NULLJAW_SCALE.get())) {
            ItemStack output = new ItemStack(ModItems.POTION_OF_TIDEGUARD.get());
            PotionUtils.setPotion(output, ModPotions.NULLJAW_TIDEGUARD.get());
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
