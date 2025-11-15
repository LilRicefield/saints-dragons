package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Cloth Config-backed implementation. Only loaded if Cloth Config is present.
 */
final class FabricClothConfigHelper implements ConfigHelper {
    private static volatile ConfigHolder<SaintsDragonsFabricConfig> holder;

    static FabricClothConfigHelper create() {
        return new FabricClothConfigHelper();
    }

    private static ConfigHolder<SaintsDragonsFabricConfig> holder() {
        ConfigHolder<SaintsDragonsFabricConfig> current = holder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = holder;
                if (current == null) {
                    SaintsDragonsCommon.LOGGER.info("[Fabric] Detected Cloth Config; enabling editable spawn config");
                    AutoConfig.register(SaintsDragonsFabricConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricConfig.class);
                    holder = current;
                }
            }
        }
        return current;
    }

    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        holder(); // ensure config registration
        return new ClothBuilder();
    }

    private static final class ClothBuilder implements ConfigBuilder {
        @Override
        public void push(String category) {
            // Categories handled via annotations.
        }

        @Override
        public void pop() {
            // No-op (annotations only).
        }

        @Override
        public void comment(String comment) {
            // Comments handled via @Tooltip annotations in SaintsDragonsFabricConfig
        }

        @Override
        public IntValue defineInt(String key, int defaultValue, int min, int max) {
            IntSupplier supplier = supplierForKey(key, defaultValue);
            return new ClothIntValue(supplier, min, max);
        }

        @Override
        public ListValue defineList(String key, List<String> defaultValue) {
            Supplier<List<String>> supplier = listSupplierForKey(key, defaultValue);
            return supplier::get;
        }

        @Override
        public void build() {
            holder().save();
        }
    }

    private static IntSupplier supplierForKey(String key, int defaultValue) {
        return switch (key) {
            case "raevyxSpawnWeight" -> () -> holder().getConfig().raevyxSpawnWeight;
            case "raevyxMinGroupSize" -> () -> holder().getConfig().raevyxMinGroupSize;
            case "raevyxMaxGroupSize" -> () -> holder().getConfig().raevyxMaxGroupSize;
            case "stegonautSpawnWeight" -> () -> holder().getConfig().stegonautSpawnWeight;
            case "stegonautMinGroupSize" -> () -> holder().getConfig().stegonautMinGroupSize;
            case "stegonautMaxGroupSize" -> () -> holder().getConfig().stegonautMaxGroupSize;
            case "cindervaneSpawnWeight" -> () -> holder().getConfig().cindervaneSpawnWeight;
            case "cindervaneMinGroupSize" -> () -> holder().getConfig().cindervaneMinGroupSize;
            case "cindervaneMaxGroupSize" -> () -> holder().getConfig().cindervaneMaxGroupSize;
            case "nulljawSpawnWeight" -> () -> holder().getConfig().nulljawSpawnWeight;
            case "nulljawMinGroupSize" -> () -> holder().getConfig().nulljawMinGroupSize;
            case "nulljawMaxGroupSize" -> () -> holder().getConfig().nulljawMaxGroupSize;
            case "ignivorusSpawnWeight" -> () -> holder().getConfig().ignivorusSpawnWeight;
            case "ignivorusMinGroupSize" -> () -> holder().getConfig().ignivorusMinGroupSize;
            case "ignivorusMaxGroupSize" -> () -> holder().getConfig().ignivorusMaxGroupSize;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static Supplier<List<String>> listSupplierForKey(String key, List<String> defaultValue) {
        return switch (key) {
            case "raevyxAdditionalBiomes" -> () -> holder().getConfig().raevyxAdditionalBiomes;
            case "stegonautAdditionalBiomes" -> () -> holder().getConfig().stegonautAdditionalBiomes;
            case "cindervaneAdditionalBiomes" -> () -> holder().getConfig().cindervaneAdditionalBiomes;
            case "nulljawAdditionalBiomes" -> () -> holder().getConfig().nulljawAdditionalBiomes;
            case "ignivorusAdditionalBiomes" -> () -> holder().getConfig().ignivorusAdditionalBiomes;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config list key '{}'; using default", key);
                yield () -> defaultValue;
            }
        };
    }

    private static final class ClothIntValue implements IntValue {
        private final IntSupplier supplier;
        private final int min;
        private final int max;

        private ClothIntValue(IntSupplier supplier, int min, int max) {
            this.supplier = supplier;
            this.min = min;
            this.max = max;
        }

        @Override
        public int get() {
            int value = supplier.getAsInt();
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        }
    }
}
