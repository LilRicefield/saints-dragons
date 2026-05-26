package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.sounds.SoundEvent;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class FabricPlatformHelper implements PlatformHelper {
    // Lazy initialization to avoid ServiceConfigurationError during early class loading
    private FabricRegistryHelper registryHelper;
    private FabricNetworkHelper networkHelper;
    private FabricConfigHelper configHelper;

    @Override
    public RegistryHelper getRegistryHelper() {
        if (registryHelper == null) {
            registryHelper = new FabricRegistryHelper();
        }
        return registryHelper;
    }

    @Override
    public NetworkHelper getNetworkHelper() {
        if (networkHelper == null) {
            networkHelper = new FabricNetworkHelper();
        }
        return networkHelper;
    }

    @Override
    public ConfigHelper getConfigHelper() {
        if (configHelper == null) {
            configHelper = new FabricConfigHelper();
        }
        return configHelper;
    }

    @Override
    public void runOnClient(Runnable runnable) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            runnable.run();
        }
    }

    @Override
    public <T> T callOnClient(Supplier<T> supplier) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return supplier.get();
        }
        return null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getPlatformId() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isGenericDiveLoopEnabled() {
        return FabricClientConfigAccess.isGenericDiveLoopEnabled();
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                               int primaryColor,
                               int secondaryColor,
                               Item.Properties properties) {
        return new SpawnEggItem(entityType.get(), primaryColor, secondaryColor, properties);
    }

    @Override
    public Item createMobBucket(Supplier<? extends EntityType<? extends Mob>> entityType,
                                Fluid fluid,
                                SoundEvent emptySound,
                                Item.Properties properties) {
        return new MobBucketItem(entityType.get(), fluid, emptySound, properties);
    }

    @Override
    public net.minecraft.core.particles.SimpleParticleType createSimpleParticle(boolean overrideLimiter) {
        return new SimpleParticleTypeImpl(overrideLimiter);
    }

    private static final class SimpleParticleTypeImpl extends net.minecraft.core.particles.SimpleParticleType {
        private SimpleParticleTypeImpl(boolean overrideLimiter) {
            super(overrideLimiter);
        }
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
