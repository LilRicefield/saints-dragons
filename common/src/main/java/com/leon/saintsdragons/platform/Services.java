package com.leon.saintsdragons.platform;

import java.util.ServiceLoader;

/**
 * Central access point for platform specific services resolved via {@link ServiceLoader}.
 */
public final class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private Services() {
    }

    private static <T> T load(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing service implementation for " + serviceClass.getName()));
    }
}
