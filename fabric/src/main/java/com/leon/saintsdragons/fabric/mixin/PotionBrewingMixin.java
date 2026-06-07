package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotionItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
public final class PotionBrewingMixin {
    @Unique
    private static boolean isCustomRecipeIngredient(ItemStack ingredient) {
        return ingredient.is(ModItems.VARASUCHUS_SCALE.get()) || ingredient.is(ModItems.IGNIVORUS_TOOTH.get());
    }

    @Unique
    private static boolean isSaintsDragonsPotion(ItemStack stack) {
        return stack.is(ModPotionItems.POTION_OF_TIDEGUARD.get())
                || stack.is(ModPotionItems.POTION_OF_SEARING.get())
                || PotionUtils.getPotion(stack) == ModPotions.TIDEGUARD.get()
                || PotionUtils.getPotion(stack) == ModPotions.SEARING.get();
    }

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$allowCustomIngredients(
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(itemStack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isPotionIngredient", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$allowCustomPotionIngredients(
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(itemStack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$blockAwkwardSplashAndLingering(
            ItemStack itemStack,
            ItemStack itemStack2,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSaintsDragonsPotion(itemStack)) {
            cir.setReturnValue(false);
            return;
        }

        if (!isCustomRecipeIngredient(itemStack2)) {
            return;
        }

        if (PotionUtils.getPotion(itemStack) != Potions.AWKWARD) {
            return;
        }

        if (!itemStack.is(Items.POTION)) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "hasPotionMix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$blockVanillaPotionFamilyConversions(
            ItemStack itemStack,
            ItemStack itemStack2,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isCustomRecipeIngredient(itemStack2)
                && itemStack.is(Items.POTION)
                && PotionUtils.getPotion(itemStack) == Potions.AWKWARD) {
            cir.setReturnValue(true);
            return;
        }

        if (isSaintsDragonsPotion(itemStack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$customPotionItemOutput(
            ItemStack itemStack,
            ItemStack itemStack2,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (isSaintsDragonsPotion(itemStack)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (PotionUtils.getPotion(itemStack) != Potions.AWKWARD || !itemStack.is(Items.POTION)) {
            return;
        }

        if (itemStack2.is(ModItems.VARASUCHUS_SCALE.get())) {
            ItemStack output = new ItemStack(ModPotionItems.POTION_OF_TIDEGUARD.get());
            PotionUtils.setPotion(output, ModPotions.TIDEGUARD.get());
            cir.setReturnValue(output);
            return;
        }

        if (itemStack2.is(ModItems.IGNIVORUS_TOOTH.get())) {
            ItemStack output = new ItemStack(ModPotionItems.POTION_OF_SEARING.get());
            PotionUtils.setPotion(output, ModPotions.SEARING.get());
            cir.setReturnValue(output);
        }
    }
}
