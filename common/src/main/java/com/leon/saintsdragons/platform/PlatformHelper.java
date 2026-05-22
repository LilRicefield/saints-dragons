package com.leon.saintsdragons.platform;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

import java.nio.file.Path;
import java.util.function.Supplier;

public interface PlatformHelper {
    RegistryHelper getRegistryHelper();
    NetworkHelper getNetworkHelper();
    ConfigHelper getConfigHelper();
    void runOnClient(Runnable runnable);
    <T> T callOnClient(Supplier<T> supplier);
    boolean isDevelopmentEnvironment();
    String getPlatformId();
    boolean isModLoaded(String modId);
    boolean isGenericDiveLoopEnabled();
    Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                        int primaryColor,
                        int secondaryColor,
                        Item.Properties properties);
   SimpleParticleType createSimpleParticle(boolean overrideLimiter);
    Path getConfigDirectory();
}
