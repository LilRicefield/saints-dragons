package com.leon.saintsdragons.platform;

import java.util.List;

public interface ConfigHelper {
    ConfigBuilder commonBuilder(String fileName);

    interface ConfigBuilder {
        void push(String category);

        void pop();

        void comment(String comment);

        IntValue defineInt(String key, int defaultValue, int min, int max);

        ListValue defineList(String key, List<String> defaultValue);

        void build();
    }

    interface IntValue {
        int get();
    }

    interface ListValue {
        List<String> get();
    }
}
