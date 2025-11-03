package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ForgeConfigHelper implements ConfigHelper {
    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        return new ForgeBuilder(fileName);
    }

    private static final class ForgeBuilder implements ConfigBuilder {
        private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        private final String fileName;

        private ForgeBuilder(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void push(String category) {
            builder.push(category);
        }

        @Override
        public void pop() {
            builder.pop();
        }

        @Override
        public IntValue defineInt(String key, int defaultValue, int min, int max) {
            ForgeConfigSpec.IntValue value = builder.defineInRange(key, defaultValue, min, max);
            return value::get;
        }

        @Override
        public void build() {
            ForgeConfigSpec spec = builder.build();
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec, fileName);
        }
    }
}
