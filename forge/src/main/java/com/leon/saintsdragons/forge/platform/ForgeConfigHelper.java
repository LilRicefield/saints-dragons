package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

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
        public void comment(String comment) {
            builder.comment(comment);
        }

        @Override
        public IntValue defineInt(String key, int defaultValue, int min, int max) {
            ForgeConfigSpec.IntValue value = builder.defineInRange(key, defaultValue, min, max);
            return new ForgeIntValue(value);
        }

        @Override
        public ListValue defineList(String key, List<String> defaultValue) {
            ForgeConfigSpec.ConfigValue<List<? extends String>> value =
                    builder.defineList(key, defaultValue, obj -> obj instanceof String);
            return new ForgeListValue(value);
        }

        @Override
        public void build() {
            ForgeConfigSpec spec = builder.build();
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec, fileName);
        }
    }

    private static final class ForgeIntValue implements IntValue {
        private final ForgeConfigSpec.IntValue value;

        private ForgeIntValue(ForgeConfigSpec.IntValue value) {
            this.value = value;
        }

        @Override
        public int get() {
            return value.get();
        }

        @Override
        public void set(int newValue) {
            value.set(newValue);
        }

        @Override
        public void save() {
            value.save();
        }
    }

    private static final class ForgeListValue implements ListValue {
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> value;

        private ForgeListValue(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
            this.value = value;
        }

        @Override
        public List<String> get() {
            return new ArrayList<>(value.get());
        }

        @Override
        public void set(List<String> newValue) {
            value.set(newValue);
        }

        @Override
        public void save() {
            value.save();
        }
    }
}
