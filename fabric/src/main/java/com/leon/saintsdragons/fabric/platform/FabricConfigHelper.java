package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.ConfigHelper;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric config helper that optionally hooks into Cloth Config.
 */
public final class FabricConfigHelper implements ConfigHelper {
    private final ConfigHelper delegate = createDelegate();

    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        return delegate.commonBuilder(fileName);
    }

    private static ConfigHelper createDelegate() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            try {
                return FabricClothConfigHelper.create();
            } catch (Throwable throwable) {
                SaintsDragonsCommon.LOGGER.error("[Fabric] Failed to initialise Cloth Config integration; falling back to defaults", throwable);
            }
        } else {
            SaintsDragonsCommon.LOGGER.info("[Fabric] Cloth Config not detected; using default spawn config values");
        }
        return new FabricDefaultConfigHelper();
    }

    /**
     * Default implementation that just returns the provided defaults without persistence.
     */
    private static final class FabricDefaultConfigHelper implements ConfigHelper {
        @Override
        public ConfigBuilder commonBuilder(String fileName) {
            return new DefaultBuilder();
        }

        private static final class DefaultBuilder implements ConfigBuilder {
            @Override
            public void push(String category) {
                // No-op
            }

            @Override
            public void pop() {
                // No-op
            }

            @Override
            public void comment(String comment) {
                // No-op
            }

            @Override
            public IntValue defineInt(String key, int defaultValue, int min, int max) {
                return () -> Math.max(min, Math.min(max, defaultValue));
            }

            @Override
            public ListValue defineList(String key, java.util.List<String> defaultValue) {
                return () -> new java.util.ArrayList<>(defaultValue);
            }

            @Override
            public void build() {
                // Nothing to persist
            }
        }
    }
}
