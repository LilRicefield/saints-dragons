package com.leon.saintsdragons.platform;

public interface ConfigHelper {
    ConfigBuilder commonBuilder(String fileName);

    interface ConfigBuilder {
        void push(String category);

        void pop();

        IntValue defineInt(String key, int defaultValue, int min, int max);

        void build();
    }

    interface IntValue {
        int get();
    }
}
