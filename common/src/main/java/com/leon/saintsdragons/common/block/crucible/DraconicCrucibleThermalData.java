package com.leon.saintsdragons.common.block.crucible;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * Reloadable thermal values for the Crucible's intentionally fixed three heat levels.
 */
public record DraconicCrucibleThermalData(
        int level1ChargeCapacity,
        int level1ProcessingCost,
        int level2ChargeCapacity,
        int level2ProcessingCost,
        int level3ChargeCapacity,
        int level3ProcessingCost
) {
    public static final DraconicCrucibleThermalData DEFAULT =
            new DraconicCrucibleThermalData(5, 1, 11, 2, 17, 6);

    private static final int MAX_SYNCED_VALUE = Short.MAX_VALUE;

    public static DraconicCrucibleThermalData fromJson(JsonObject root) {
        JsonObject level1 = GsonHelper.getAsJsonObject(root, "level_1");
        JsonObject level2 = GsonHelper.getAsJsonObject(root, "level_2");
        JsonObject level3 = GsonHelper.getAsJsonObject(root, "level_3");
        DraconicCrucibleThermalData data = new DraconicCrucibleThermalData(
                readPositive(level1, "charge_capacity", "level_1"),
                readPositive(level1, "processing_cost", "level_1"),
                readPositive(level2, "charge_capacity", "level_2"),
                readPositive(level2, "processing_cost", "level_2"),
                readPositive(level3, "charge_capacity", "level_3"),
                readPositive(level3, "processing_cost", "level_3")
        );
        data.validate();
        return data;
    }

    public int chargeCapacity(int heatLevel) {
        return switch (heatLevel) {
            case 1 -> this.level1ChargeCapacity;
            case 2 -> this.level2ChargeCapacity;
            case 3 -> this.level3ChargeCapacity;
            default -> 0;
        };
    }

    public int processingCost(int requiredHeatLevel) {
        return switch (requiredHeatLevel) {
            case 1 -> this.level1ProcessingCost;
            case 2 -> this.level2ProcessingCost;
            case 3 -> this.level3ProcessingCost;
            default -> Integer.MAX_VALUE;
        };
    }

    public int minimumRemainingCharge(int requiredHeatLevel) {
        return switch (requiredHeatLevel) {
            case 1 -> 0;
            case 2 -> this.level1ChargeCapacity;
            case 3 -> this.level2ChargeCapacity;
            default -> Integer.MAX_VALUE;
        };
    }

    private void validate() {
        validateSyncedCapacity("level_1", this.level1ChargeCapacity);
        validateSyncedCapacity("level_2", this.level2ChargeCapacity);
        validateSyncedCapacity("level_3", this.level3ChargeCapacity);
        validateFundableCost("level_1", this.level1ProcessingCost);
        validateFundableCost("level_2", this.level2ProcessingCost);
        validateFundableCost("level_3", this.level3ProcessingCost);
        if (this.level1ChargeCapacity < this.level1ProcessingCost) {
            throw new IllegalArgumentException("level_1 charge_capacity must fund at least one level_1 operation");
        }
        if ((long) this.level2ChargeCapacity
                < (long) this.level1ChargeCapacity + this.level2ProcessingCost) {
            throw new IllegalArgumentException(
                    "level_2 charge_capacity must exceed level_1 capacity by at least its processing_cost");
        }
        if ((long) this.level3ChargeCapacity
                < (long) this.level2ChargeCapacity + this.level3ProcessingCost) {
            throw new IllegalArgumentException(
                    "level_3 charge_capacity must exceed level_2 capacity by at least its processing_cost");
        }
    }

    private static int readPositive(JsonObject level, String field, String levelName) {
        int value = GsonHelper.getAsInt(level, field);
        if (value <= 0) {
            throw new IllegalArgumentException(levelName + "." + field + " must be greater than zero");
        }
        return value;
    }

    private static void validateSyncedCapacity(String levelName, int capacity) {
        if (capacity > MAX_SYNCED_VALUE) {
            throw new IllegalArgumentException(
                    levelName + ".charge_capacity must not exceed " + MAX_SYNCED_VALUE);
        }
    }

    private static void validateFundableCost(String levelName, int cost) {
        if (cost > MAX_SYNCED_VALUE) {
            throw new IllegalArgumentException(
                    levelName + ".processing_cost must not exceed " + MAX_SYNCED_VALUE);
        }
    }
}
