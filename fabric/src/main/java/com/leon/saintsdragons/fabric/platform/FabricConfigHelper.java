package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

import java.util.function.IntSupplier;

public final class FabricConfigHelper implements ConfigHelper {
    private static volatile ConfigHolder<SaintsDragonsFabricConfig> holder;

    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        return new FabricBuilder();
    }

    private static ConfigHolder<SaintsDragonsFabricConfig> holder() {
        ConfigHolder<SaintsDragonsFabricConfig> current = holder;
        if (current == null) {
            synchronized (FabricConfigHelper.class) {
                current = holder;
                if (current == null) {
                    AutoConfig.register(SaintsDragonsFabricConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricConfig.class);
                    holder = current;
                }
            }
        }
        return current;
    }

    private static final class FabricBuilder implements ConfigBuilder {
        private FabricBuilder() {
            holder(); // Ensure the config is registered before values are defined.
        }

        @Override
        public void push(String category) {
            // Categories are handled via annotations in the config data class.
        }

        @Override
        public void pop() {
            // No-op: see push.
        }

        @Override
        public IntValue defineInt(String key, int defaultValue, int min, int max) {
            IntSupplier supplier = supplierForKey(key, defaultValue);
            return new FabricIntValue(supplier, min, max);
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
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static final class FabricIntValue implements IntValue {
        private final IntSupplier supplier;
        private final int min;
        private final int max;

        private FabricIntValue(IntSupplier supplier, int min, int max) {
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
