package com.leon.saintsdragons.platform;

import java.util.function.Supplier;

/**
 * Entry point for platform specific functionality exposed to common code.
 */
public interface PlatformHelper {
    RegistryHelper getRegistryHelper();

    NetworkHelper getNetworkHelper();

    ConfigHelper getConfigHelper();

    /**
     * Execute the supplied runnable if the environment is a physical client.
     */
    void runOnClient(Runnable runnable);

    /**
     * Execute the supplied supplier lazily on the physical client and return the value or {@code null} otherwise.
     */
    <T> T callOnClient(Supplier<T> supplier);

    /**
     * @return {@code true} when running in a development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * @return {@code true} if the given mod id is loaded in the current environment.
     */
    boolean isModLoaded(String modId);

    /**
     * Create a platform-appropriate spawn egg item.
     */
    net.minecraft.world.item.Item createSpawnEgg(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> entityType,
                                                 int primaryColor,
                                                 int secondaryColor,
                                                 net.minecraft.world.item.Item.Properties properties);

    /**
     * Create a simple particle type (constructor is protected in vanilla).
     */
    net.minecraft.core.particles.SimpleParticleType createSimpleParticle(boolean overrideLimiter);
}
