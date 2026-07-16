package com.leon.saintsdragons.platform;

import java.util.List;

public interface ConfigHelper {
    ConfigBuilder commonBuilder(String fileName);

    interface ConfigBuilder {
        void push(String category);

        void pop();

        void comment(String comment);

        IntValue defineInt(String key, int defaultValue, int min, int max);

        DoubleValue defineDouble(String key, double defaultValue, double min, double max);

        BooleanValue defineBoolean(String key, boolean defaultValue);

        ListValue defineList(String key, List<String> defaultValue);

        void build();
    }

    interface IntValue {
        int get();

        default void set(int value) {
            // Optional for platforms that support writable configs.
        }

        default void save() {
            // Optional for platforms that support writable configs.
        }
    }

    interface DoubleValue {
        double get();

        default void set(double value) {
            // Optional for platforms that support writable configs.
        }

        default void save() {
            // Optional for platforms that support writable configs.
        }
    }

    interface BooleanValue {
        boolean get();

        default void set(boolean value) {
            // Optional for platforms that support writable configs.
        }

        default void save() {
            // Optional for platforms that support writable configs.
        }
    }

    interface ListValue {
        List<String> get();

        default void set(List<String> value) {
            // Optional for platforms that support writable configs.
        }

        default void save() {
            // Optional for platforms that support writable configs.
        }
    }
}
