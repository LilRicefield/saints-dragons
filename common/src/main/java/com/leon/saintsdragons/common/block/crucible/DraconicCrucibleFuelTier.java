package com.leon.saintsdragons.common.block.crucible;

import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum DraconicCrucibleFuelTier {
    NONE(0, null),
    LEVEL_1(1, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_1),
    LEVEL_2(2, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_2),
    LEVEL_3(3, ModTags.Items.DRACONIC_CRUCIBLE_FUEL_LEVEL_3);

    private final int heatLevel;
    private final TagKey<Item> tag;

    DraconicCrucibleFuelTier(int heatLevel, TagKey<Item> tag) {
        this.heatLevel = heatLevel;
        this.tag = tag;
    }

    public int heatLevel() {
        return this.heatLevel;
    }

    public int chargeCapacity() {
        return thermalData().chargeCapacity(this.heatLevel);
    }

    public boolean canProcess(int requiredHeatLevel) {
        return this.heatLevel >= requiredHeatLevel;
    }

    public int processingCost(int requiredHeatLevel) {
        if (!canProcess(requiredHeatLevel)) {
            return Integer.MAX_VALUE;
        }
        return thermalData().processingCost(requiredHeatLevel);
    }

    public boolean canFund(int charge, int requiredHeatLevel) {
        if (!canProcess(requiredHeatLevel)) {
            return false;
        }
        int cost = processingCost(requiredHeatLevel);
        return cost != Integer.MAX_VALUE
                && charge >= minimumRemainingCharge(requiredHeatLevel) + cost;
    }

    public static int minimumRemainingCharge(int requiredHeatLevel) {
        return thermalData().minimumRemainingCharge(requiredHeatLevel);
    }

    public static @NotNull DraconicCrucibleFuelTier fromHeatLevel(int heatLevel) {
        return switch (heatLevel) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            default -> NONE;
        };
    }

    public static @NotNull DraconicCrucibleFuelTier fromCharge(int charge) {
        if (charge <= 0) {
            return NONE;
        }
        if (charge <= LEVEL_1.chargeCapacity()) {
            return LEVEL_1;
        }
        if (charge <= LEVEL_2.chargeCapacity()) {
            return LEVEL_2;
        }
        return LEVEL_3;
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

    private static DraconicCrucibleThermalData thermalData() {
        return DraconicCrucibleThermalReloadListener.current();
    }
}
