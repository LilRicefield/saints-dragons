package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public final class BrewingStandBlockEntityMixin {
    private static final int BOTTLE_SLOT_START = 0;
    private static final int BOTTLE_SLOT_END = 2;
    private static final int INGREDIENT_SLOT = 3;

    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void saintsdragons$brewCustomPotions(
            Level level,
            BlockPos pos,
            NonNullList<ItemStack> items,
            CallbackInfo ci
    ) {
        ItemStack ingredient = items.get(INGREDIENT_SLOT);
        if (ingredient.isEmpty()) {
            return;
        }

        boolean tideguard = ingredient.is(ModItems.VARASUCHUS_SCALE.get());
        boolean searing = ingredient.is(ModItems.IGNIVORUS_TOOTH.get());
        if (!tideguard && !searing) {
            return;
        }

        boolean brewedAny = false;
        for (int slot = BOTTLE_SLOT_START; slot <= BOTTLE_SLOT_END; slot++) {
            ItemStack input = items.get(slot);
            if (!input.is(Items.POTION) || PotionUtils.getPotion(input) != Potions.AWKWARD) {
                continue;
            }

            ItemStack output = new ItemStack(tideguard ? ModItems.POTION_OF_TIDEGUARD.get() : ModItems.POTION_OF_SEARING.get());
            PotionUtils.setPotion(output, tideguard ? ModPotions.VARASUCHUS_TIDEGUARD.get() : ModPotions.SEARING.get());
            items.set(slot, output);
            brewedAny = true;
        }

        if (!brewedAny) {
            return;
        }

        ingredient.shrink(1);
        level.levelEvent(1035, pos, 0);
        ci.cancel();
    }
}
