package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricSpawnConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Cloth Config-backed implementation. Only loaded if Cloth Config is present.
 */
final class FabricClothConfigHelper implements ConfigHelper {
    private static volatile ConfigHolder<SaintsDragonsFabricSpawnConfig> holder;

    static FabricClothConfigHelper create() {
        return new FabricClothConfigHelper();
    }

    private static ConfigHolder<SaintsDragonsFabricSpawnConfig> holder() {
        ConfigHolder<SaintsDragonsFabricSpawnConfig> current = holder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = holder;
                if (current == null) {
                    SaintsDragonsCommon.LOGGER.info("[Fabric] Detected Cloth Config; enabling editable spawn config");
                    AutoConfig.register(SaintsDragonsFabricSpawnConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricSpawnConfig.class);
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
        public BooleanValue defineBoolean(String key, boolean defaultValue) {
            BooleanSupplier supplier = booleanSupplierForKey(key, defaultValue);
            Consumer<Boolean> setter = booleanSetterForKey(key);
            return new ClothBooleanValue(supplier, setter);
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
            case "varasuchusSpawnWeight" -> () -> holder().getConfig().varasuchusSpawnWeight;
            case "varasuchusMinGroupSize" -> () -> holder().getConfig().varasuchusMinGroupSize;
            case "varasuchusMaxGroupSize" -> () -> holder().getConfig().varasuchusMaxGroupSize;
            case "ignivorusSpawnWeight" -> () -> holder().getConfig().ignivorusSpawnWeight;
            case "ignivorusMinGroupSize" -> () -> holder().getConfig().ignivorusMinGroupSize;
            case "ignivorusMaxGroupSize" -> () -> holder().getConfig().ignivorusMaxGroupSize;
            case "volitansSpawnWeight" -> () -> holder().getConfig().volitansSpawnWeight;
            case "volitansMinGroupSize" -> () -> holder().getConfig().volitansMinGroupSize;
            case "volitansMaxGroupSize" -> () -> holder().getConfig().volitansMaxGroupSize;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static BooleanSupplier booleanSupplierForKey(String key, boolean defaultValue) {
        return switch (key) {
            case "cindervaneEggBlockWorldgen" -> () -> holder().getConfig().cindervaneEggBlockWorldgen;
            case "varasuchusEggBlockWorldgen" -> () -> holder().getConfig().varasuchusEggBlockWorldgen;
            case "dragonGriefingEnabled" -> () -> holder().getConfig().dragonGriefingEnabled;
            case "screenShakeEnabled" -> () -> holder().getConfig().screenShakeEnabled;
            case "barrelRollEnabled" -> () -> holder().getConfig().barrelRollEnabled;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config boolean key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static Consumer<Boolean> booleanSetterForKey(String key) {
        return switch (key) {
            case "cindervaneEggBlockWorldgen" -> value -> holder().getConfig().cindervaneEggBlockWorldgen = value;
            case "varasuchusEggBlockWorldgen" -> value -> holder().getConfig().varasuchusEggBlockWorldgen = value;
            case "dragonGriefingEnabled" -> value -> holder().getConfig().dragonGriefingEnabled = value;
            case "screenShakeEnabled" -> value -> holder().getConfig().screenShakeEnabled = value;
            case "barrelRollEnabled" -> value -> holder().getConfig().barrelRollEnabled = value;
            default -> value -> SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config boolean key '{}' for setter", key);
        };
    }

    private static Supplier<List<String>> listSupplierForKey(String key, List<String> defaultValue) {
        return switch (key) {
            case "raevyxAdditionalBiomes" -> () -> holder().getConfig().raevyxAdditionalBiomes;
            case "raevyxExcludedBiomes" -> () -> holder().getConfig().raevyxExcludedBiomes;
            case "stegonautAdditionalBiomes" -> () -> holder().getConfig().stegonautAdditionalBiomes;
            case "stegonautExcludedBiomes" -> () -> holder().getConfig().stegonautExcludedBiomes;
            case "cindervaneAdditionalBiomes" -> () -> holder().getConfig().cindervaneAdditionalBiomes;
            case "cindervaneExcludedBiomes" -> () -> holder().getConfig().cindervaneExcludedBiomes;
            case "varasuchusAdditionalBiomes" -> () -> holder().getConfig().varasuchusAdditionalBiomes;
            case "varasuchusExcludedBiomes" -> () -> holder().getConfig().varasuchusExcludedBiomes;
            case "ignivorusAdditionalBiomes" -> () -> holder().getConfig().ignivorusAdditionalBiomes;
            case "ignivorusExcludedBiomes" -> () -> holder().getConfig().ignivorusExcludedBiomes;
            case "volitansAdditionalBiomes" -> () -> holder().getConfig().volitansAdditionalBiomes;
            case "volitansExcludedBiomes" -> () -> holder().getConfig().volitansExcludedBiomes;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config list key '{}'; using default", key);
                yield () -> defaultValue;
            }
        };
    }

    private static final class ClothBooleanValue implements BooleanValue {
        private final BooleanSupplier supplier;
        private final Consumer<Boolean> setter;

        private ClothBooleanValue(BooleanSupplier supplier, Consumer<Boolean> setter) {
            this.supplier = supplier;
            this.setter = setter;
        }

        @Override
        public boolean get() {
            return supplier.getAsBoolean();
        }

        @Override
        public void set(boolean value) {
            setter.accept(value);
        }

        @Override
        public void save() {
            holder().save();
        }
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
