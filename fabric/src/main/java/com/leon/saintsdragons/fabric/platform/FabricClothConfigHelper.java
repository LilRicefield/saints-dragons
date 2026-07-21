package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricClientConfig;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricSpawnConfig;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricServerConfig;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricToolsArmorConfig;
import com.leon.saintsdragons.platform.ConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Cloth Config-backed implementation. Only loaded if Cloth Config is present.
 */
final class FabricClothConfigHelper implements ConfigHelper {
    private static volatile ConfigHolder<SaintsDragonsFabricSpawnConfig> spawnHolder;
    private static volatile ConfigHolder<SaintsDragonsFabricServerConfig> serverHolder;
    private static volatile ConfigHolder<SaintsDragonsFabricToolsArmorConfig> toolsArmorHolder;
    private static volatile ConfigHolder<SaintsDragonsFabricClientConfig> clientHolder;

    static FabricClothConfigHelper create() {
        return new FabricClothConfigHelper();
    }

    private static ConfigHolder<SaintsDragonsFabricSpawnConfig> spawnHolder() {
        ConfigHolder<SaintsDragonsFabricSpawnConfig> current = spawnHolder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = spawnHolder;
                if (current == null) {
                    SaintsDragonsCommon.LOGGER.info("[Fabric] Detected Cloth Config; enabling editable Saint's Dragons configs");
                    AutoConfig.register(SaintsDragonsFabricSpawnConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricSpawnConfig.class);
                    spawnHolder = current;
                }
            }
        }
        return current;
    }

    private static ConfigHolder<SaintsDragonsFabricServerConfig> serverHolder() {
        ConfigHolder<SaintsDragonsFabricServerConfig> current = serverHolder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = serverHolder;
                if (current == null) {
                    AutoConfig.register(SaintsDragonsFabricServerConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricServerConfig.class);
                    serverHolder = current;
                }
            }
        }
        return current;
    }

    private static ConfigHolder<SaintsDragonsFabricClientConfig> clientHolder() {
        ConfigHolder<SaintsDragonsFabricClientConfig> current = clientHolder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = clientHolder;
                if (current == null) {
                    AutoConfig.register(SaintsDragonsFabricClientConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricClientConfig.class);
                    clientHolder = current;
                }
            }
        }
        return current;
    }

    private static ConfigHolder<SaintsDragonsFabricToolsArmorConfig> toolsArmorHolder() {
        ConfigHolder<SaintsDragonsFabricToolsArmorConfig> current = toolsArmorHolder;
        if (current == null) {
            synchronized (FabricClothConfigHelper.class) {
                current = toolsArmorHolder;
                if (current == null) {
                    AutoConfig.register(SaintsDragonsFabricToolsArmorConfig.class, Toml4jConfigSerializer::new);
                    current = AutoConfig.getConfigHolder(SaintsDragonsFabricToolsArmorConfig.class);
                    toolsArmorHolder = current;
                }
            }
        }
        return current;
    }

    private static void ensureRegistered() {
        spawnHolder();
        serverHolder();
        toolsArmorHolder();
        clientHolder();
    }

    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        ensureRegistered();
        Runnable saver = ToolsArmorConfig.CONFIG_FILE.equals(fileName)
                ? toolsArmorHolder()::save
                : SaintsDragonsConfig.SERVER_CONFIG_FILE.equals(fileName)
                        ? serverHolder()::save
                        : spawnHolder()::save;
        return new ClothBuilder(saver);
    }

    private static final class ClothBuilder implements ConfigBuilder {
        private final Runnable saver;

        private ClothBuilder(Runnable saver) {
            this.saver = saver;
        }

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
            IntConsumer setter = intSetterForKey(key);
            return new ClothIntValue(supplier, setter, saver, min, max);
        }

        @Override
        public DoubleValue defineDouble(String key, double defaultValue, double min, double max) {
            return new ClothDoubleValue(
                    doubleSupplierForKey(key, defaultValue),
                    doubleSetterForKey(key),
                    saver,
                    min,
                    max
            );
        }

        @Override
        public BooleanValue defineBoolean(String key, boolean defaultValue) {
            BooleanSupplier supplier = booleanSupplierForKey(key, defaultValue);
            Consumer<Boolean> setter = booleanSetterForKey(key);
            return new ClothBooleanValue(supplier, setter, saver);
        }

        @Override
        public ListValue defineList(String key, List<String> defaultValue) {
            Supplier<List<String>> supplier = listSupplierForKey(key, defaultValue);
            return supplier::get;
        }

        @Override
        public void build() {
            saver.run();
        }
    }

    private static IntSupplier supplierForKey(String key, int defaultValue) {
        return switch (key) {
            case "raevyxSpawnWeight" -> () -> spawnHolder().getConfig().raevyxSpawnWeight;
            case "raevyxMinGroupSize" -> () -> spawnHolder().getConfig().raevyxMinGroupSize;
            case "raevyxMaxGroupSize" -> () -> spawnHolder().getConfig().raevyxMaxGroupSize;
            case "stegonautSpawnWeight" -> () -> spawnHolder().getConfig().stegonautSpawnWeight;
            case "stegonautMinGroupSize" -> () -> spawnHolder().getConfig().stegonautMinGroupSize;
            case "stegonautMaxGroupSize" -> () -> spawnHolder().getConfig().stegonautMaxGroupSize;
            case "cindervaneSpawnWeight" -> () -> spawnHolder().getConfig().cindervaneSpawnWeight;
            case "cindervaneMinGroupSize" -> () -> spawnHolder().getConfig().cindervaneMinGroupSize;
            case "cindervaneMaxGroupSize" -> () -> spawnHolder().getConfig().cindervaneMaxGroupSize;
            case "ignivorusSpawnWeight" -> () -> spawnHolder().getConfig().ignivorusSpawnWeight;
            case "ignivorusMinGroupSize" -> () -> spawnHolder().getConfig().ignivorusMinGroupSize;
            case "ignivorusMaxGroupSize" -> () -> spawnHolder().getConfig().ignivorusMaxGroupSize;
            case "volitansSpawnWeight" -> () -> spawnHolder().getConfig().volitansSpawnWeight;
            case "volitansMinGroupSize" -> () -> spawnHolder().getConfig().volitansMinGroupSize;
            case "volitansMaxGroupSize" -> () -> spawnHolder().getConfig().volitansMaxGroupSize;
            case "nulljawSpawnWeight" -> () -> spawnHolder().getConfig().nulljawSpawnWeight;
            case "nulljawMinGroupSize" -> () -> spawnHolder().getConfig().nulljawMinGroupSize;
            case "nulljawMaxGroupSize" -> () -> spawnHolder().getConfig().nulljawMaxGroupSize;
            case "ivyRestockInterval" -> () -> serverHolder().getConfig().ivyRestockInterval;
            case "bloodTempestDodgeCooldownTicks" -> () -> toolsArmorHolder().getConfig().bloodTempestDodgeCooldownTicks;
            case "bloodTempestKatanaAbilityCooldownTicks" -> () -> toolsArmorHolder().getConfig().bloodTempestKatanaAbilityCooldownTicks;
            case "dragonlordLavaFissureDurationTicks" -> () -> toolsArmorHolder().getConfig().dragonlordLavaFissureDurationTicks;
            case "dragonlordSwordAbilityCooldownTicks" -> () -> toolsArmorHolder().getConfig().dragonlordSwordAbilityCooldownTicks;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static IntConsumer intSetterForKey(String key) {
        return switch (key) {
            case "raevyxSpawnWeight" -> value -> spawnHolder().getConfig().raevyxSpawnWeight = value;
            case "raevyxMinGroupSize" -> value -> spawnHolder().getConfig().raevyxMinGroupSize = value;
            case "raevyxMaxGroupSize" -> value -> spawnHolder().getConfig().raevyxMaxGroupSize = value;
            case "stegonautSpawnWeight" -> value -> spawnHolder().getConfig().stegonautSpawnWeight = value;
            case "stegonautMinGroupSize" -> value -> spawnHolder().getConfig().stegonautMinGroupSize = value;
            case "stegonautMaxGroupSize" -> value -> spawnHolder().getConfig().stegonautMaxGroupSize = value;
            case "cindervaneSpawnWeight" -> value -> spawnHolder().getConfig().cindervaneSpawnWeight = value;
            case "cindervaneMinGroupSize" -> value -> spawnHolder().getConfig().cindervaneMinGroupSize = value;
            case "cindervaneMaxGroupSize" -> value -> spawnHolder().getConfig().cindervaneMaxGroupSize = value;
            case "ignivorusSpawnWeight" -> value -> spawnHolder().getConfig().ignivorusSpawnWeight = value;
            case "ignivorusMinGroupSize" -> value -> spawnHolder().getConfig().ignivorusMinGroupSize = value;
            case "ignivorusMaxGroupSize" -> value -> spawnHolder().getConfig().ignivorusMaxGroupSize = value;
            case "volitansSpawnWeight" -> value -> spawnHolder().getConfig().volitansSpawnWeight = value;
            case "volitansMinGroupSize" -> value -> spawnHolder().getConfig().volitansMinGroupSize = value;
            case "volitansMaxGroupSize" -> value -> spawnHolder().getConfig().volitansMaxGroupSize = value;
            case "nulljawSpawnWeight" -> value -> spawnHolder().getConfig().nulljawSpawnWeight = value;
            case "nulljawMinGroupSize" -> value -> spawnHolder().getConfig().nulljawMinGroupSize = value;
            case "nulljawMaxGroupSize" -> value -> spawnHolder().getConfig().nulljawMaxGroupSize = value;
            case "ivyRestockInterval" -> value -> serverHolder().getConfig().ivyRestockInterval = value;
            case "bloodTempestDodgeCooldownTicks" -> value -> toolsArmorHolder().getConfig().bloodTempestDodgeCooldownTicks = value;
            case "bloodTempestKatanaAbilityCooldownTicks" -> value -> toolsArmorHolder().getConfig().bloodTempestKatanaAbilityCooldownTicks = value;
            case "dragonlordLavaFissureDurationTicks" -> value -> toolsArmorHolder().getConfig().dragonlordLavaFissureDurationTicks = value;
            case "dragonlordSwordAbilityCooldownTicks" -> value -> toolsArmorHolder().getConfig().dragonlordSwordAbilityCooldownTicks = value;
            default -> value -> SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config int key '{}' for setter", key);
        };
    }

    private static BooleanSupplier booleanSupplierForKey(String key, boolean defaultValue) {
        return switch (key) {
            case "raevyxCustomSpawningEnabled" -> () -> spawnHolder().getConfig().raevyxCustomSpawningEnabled;
            case "stegonautCustomSpawningEnabled" -> () -> spawnHolder().getConfig().stegonautCustomSpawningEnabled;
            case "volitansCustomSpawningEnabled" -> () -> spawnHolder().getConfig().volitansCustomSpawningEnabled;
            case "dragonGriefingEnabled" -> () -> serverHolder().getConfig().dragonGriefingEnabled;
            case "fireDragonBlockIgnitionEnabled" -> () -> serverHolder().getConfig().fireDragonBlockIgnitionEnabled;
            case "screenShakeEnabled" -> () -> serverHolder().getConfig().screenShakeEnabled;
            case "barrelRollEnabled" -> () -> serverHolder().getConfig().barrelRollEnabled;
            case "stegonautBuffsEnabled" -> () -> serverHolder().getConfig().stegonautBuffsEnabled;
            case "dragonBreedingEnabled" -> () -> serverHolder().getConfig().dragonBreedingEnabled;
            case "hungerDecayEnabled" -> () -> serverHolder().getConfig().hungerDecayEnabled;
            case "happinessDecayEnabled" -> () -> serverHolder().getConfig().happinessDecayEnabled;
            case "wikiReminderEnabled" -> () -> serverHolder().getConfig().wikiReminderEnabled;
            case "bloodTempestDodgeEnabled" -> () -> toolsArmorHolder().getConfig().bloodTempestDodgeEnabled;
            case "bloodTempestKatanaAbilityEnabled" -> () -> toolsArmorHolder().getConfig().bloodTempestKatanaAbilityEnabled;
            case "dragonlordSwordAbilityEnabled" -> () -> toolsArmorHolder().getConfig().dragonlordSwordAbilityEnabled;
            case "dragonlordFlightEnabled" -> () -> toolsArmorHolder().getConfig().dragonlordFlightEnabled;
            case "dragonlordLavaFissureEnabled" -> () -> toolsArmorHolder().getConfig().dragonlordLavaFissureEnabled;
            default -> {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config boolean key '{}'; using default {}", key, defaultValue);
                yield () -> defaultValue;
            }
        };
    }

    private static Consumer<Boolean> booleanSetterForKey(String key) {
        return switch (key) {
            case "raevyxCustomSpawningEnabled" -> value -> spawnHolder().getConfig().raevyxCustomSpawningEnabled = value;
            case "stegonautCustomSpawningEnabled" -> value -> spawnHolder().getConfig().stegonautCustomSpawningEnabled = value;
            case "volitansCustomSpawningEnabled" -> value -> spawnHolder().getConfig().volitansCustomSpawningEnabled = value;
            case "dragonGriefingEnabled" -> value -> serverHolder().getConfig().dragonGriefingEnabled = value;
            case "fireDragonBlockIgnitionEnabled" -> value -> serverHolder().getConfig().fireDragonBlockIgnitionEnabled = value;
            case "screenShakeEnabled" -> value -> serverHolder().getConfig().screenShakeEnabled = value;
            case "barrelRollEnabled" -> value -> serverHolder().getConfig().barrelRollEnabled = value;
            case "stegonautBuffsEnabled" -> value -> serverHolder().getConfig().stegonautBuffsEnabled = value;
            case "dragonBreedingEnabled" -> value -> serverHolder().getConfig().dragonBreedingEnabled = value;
            case "hungerDecayEnabled" -> value -> serverHolder().getConfig().hungerDecayEnabled = value;
            case "happinessDecayEnabled" -> value -> serverHolder().getConfig().happinessDecayEnabled = value;
            case "wikiReminderEnabled" -> value -> serverHolder().getConfig().wikiReminderEnabled = value;
            case "bloodTempestDodgeEnabled" -> value -> toolsArmorHolder().getConfig().bloodTempestDodgeEnabled = value;
            case "bloodTempestKatanaAbilityEnabled" -> value -> toolsArmorHolder().getConfig().bloodTempestKatanaAbilityEnabled = value;
            case "dragonlordSwordAbilityEnabled" -> value -> toolsArmorHolder().getConfig().dragonlordSwordAbilityEnabled = value;
            case "dragonlordFlightEnabled" -> value -> toolsArmorHolder().getConfig().dragonlordFlightEnabled = value;
            case "dragonlordLavaFissureEnabled" -> value -> toolsArmorHolder().getConfig().dragonlordLavaFissureEnabled = value;
            default -> value -> SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config boolean key '{}' for setter", key);
        };
    }

    private static Supplier<List<String>> listSupplierForKey(String key, List<String> defaultValue) {
        SaintsDragonsCommon.LOGGER.warn("Unknown Fabric config list key '{}'; using default", key);
        return () -> defaultValue;
    }

    private static DoubleSupplier doubleSupplierForKey(String key, double defaultValue) {
        return () -> {
            try {
                return SaintsDragonsFabricToolsArmorConfig.class.getField(key)
                        .getDouble(toolsArmorHolder().getConfig());
            } catch (ReflectiveOperationException exception) {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric tools and armor config key '{}'; using default {}", key, defaultValue);
                return defaultValue;
            }
        };
    }

    private static DoubleConsumer doubleSetterForKey(String key) {
        return value -> {
            try {
                SaintsDragonsFabricToolsArmorConfig.class.getField(key)
                        .setDouble(toolsArmorHolder().getConfig(), value);
            } catch (ReflectiveOperationException exception) {
                SaintsDragonsCommon.LOGGER.warn("Unknown Fabric tools and armor config key '{}' for setter", key);
            }
        };
    }

    private static final class ClothBooleanValue implements BooleanValue {
        private final BooleanSupplier supplier;
        private final Consumer<Boolean> setter;
        private final Runnable saver;

        private ClothBooleanValue(BooleanSupplier supplier, Consumer<Boolean> setter, Runnable saver) {
            this.supplier = supplier;
            this.setter = setter;
            this.saver = saver;
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
            saver.run();
        }
    }

    private static final class ClothIntValue implements IntValue {
        private final IntSupplier supplier;
        private final IntConsumer setter;
        private final Runnable saver;
        private final int min;
        private final int max;

        private ClothIntValue(IntSupplier supplier, IntConsumer setter, Runnable saver, int min, int max) {
            this.supplier = supplier;
            this.setter = setter;
            this.saver = saver;
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

        @Override
        public void set(int value) {
            setter.accept(Math.max(min, Math.min(max, value)));
        }

        @Override
        public void save() {
            saver.run();
        }
    }

    private static final class ClothDoubleValue implements DoubleValue {
        private final DoubleSupplier supplier;
        private final DoubleConsumer setter;
        private final Runnable saver;
        private final double min;
        private final double max;

        private ClothDoubleValue(DoubleSupplier supplier, DoubleConsumer setter, Runnable saver, double min, double max) {
            this.supplier = supplier;
            this.setter = setter;
            this.saver = saver;
            this.min = min;
            this.max = max;
        }

        @Override
        public double get() {
            return Math.max(min, Math.min(max, supplier.getAsDouble()));
        }

        @Override
        public void set(double value) {
            setter.accept(Math.max(min, Math.min(max, value)));
        }

        @Override
        public void save() {
            saver.run();
        }
    }
}
