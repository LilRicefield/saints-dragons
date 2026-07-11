package com.leon.saintsdragons.common.block.crucible;

import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum DraconicCrucibleFuelTier {
    NONE(0, 0, null),
    LEVEL_1(1, 5, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_1),
    LEVEL_2(2, 9, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_2),
    LEVEL_3(3, 18, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_3);

    private final int heatLevel;
    private final int chargeCapacity;
    private final TagKey<Item> tag;

    DraconicCrucibleFuelTier(int heatLevel, int chargeCapacity, TagKey<Item> tag) {
        this.heatLevel = heatLevel;
        this.chargeCapacity = chargeCapacity;
        this.tag = tag;
    }

    public int heatLevel() {
        return this.heatLevel;
    }

    public int chargeCapacity() {
        return this.chargeCapacity;
    }

    public boolean canProcess(int requiredHeatLevel) {
        return this.heatLevel >= requiredHeatLevel;
    }

    public int processingCost(int requiredHeatLevel) {
        if (!canProcess(requiredHeatLevel)) {
            return Integer.MAX_VALUE;
        }
        return switch (requiredHeatLevel) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 18;
            default -> Integer.MAX_VALUE;
        };
    }

    public static @NotNull DraconicCrucibleFuelTier fromHeatLevel(int heatLevel) {
        return switch (heatLevel) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            default -> NONE;
        };
    }

    public static @NotNull DraconicCrucibleFuelTier resolve(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }
        if (stack.is(LEVEL_3.tag)) {
            return LEVEL_3;
        }
        if (stack.is(LEVEL_2.tag)) {
            return LEVEL_2;
        }
        if (stack.is(LEVEL_1.tag)) {
            return LEVEL_1;
        }
        return NONE;
    }
}
