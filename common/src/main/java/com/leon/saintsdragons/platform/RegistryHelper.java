package com.leon.saintsdragons.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * Abstraction over registry operations that differ per platform.
 */
public interface RegistryHelper {
    <T> RegistryWrapper<T> create(ResourceKey<? extends Registry<T>> registryKey,
                                  Supplier<Registry<T>> backingRegistry,
                                  String modId);

    interface RegistryWrapper<T> {
        <I extends T> Supplier<I> register(String name, Supplier<I> supplier);

        void register();
    }
}
