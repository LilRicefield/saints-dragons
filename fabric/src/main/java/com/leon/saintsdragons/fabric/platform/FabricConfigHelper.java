package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.platform.ConfigHelper;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FabricConfigHelper implements ConfigHelper {
    @Override
    public ConfigBuilder commonBuilder(String fileName) {
        return new FabricBuilder();
    }

    private static final class FabricBuilder implements ConfigBuilder {
        private final Deque<String> categories = new ArrayDeque<>();

        @Override
        public void push(String category) {
            categories.push(category);
        }

        @Override
        public void pop() {
            if (!categories.isEmpty()) {
                categories.pop();
            }
        }

        @Override
        public IntValue defineInt(String key, int defaultValue, int min, int max) {
            return new IntValueImpl(defaultValue);
        }

        @Override
        public void build() {
            // No persistent config on Fabric yet; defaults are used.
        }

        private static final class IntValueImpl implements IntValue {
            private final int value;

            private IntValueImpl(int value) {
                this.value = value;
            }

            @Override
            public int get() {
                return value;
            }
        }
    }
}
