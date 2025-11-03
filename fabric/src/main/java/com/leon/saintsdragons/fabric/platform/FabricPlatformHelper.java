package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Supplier;

public final class FabricPlatformHelper implements PlatformHelper {
    private final FabricRegistryHelper registryHelper = new FabricRegistryHelper();
    private final FabricNetworkHelper networkHelper = new FabricNetworkHelper();
    private final FabricConfigHelper configHelper = new FabricConfigHelper();

    @Override
    public RegistryHelper getRegistryHelper() {
        return registryHelper;
    }

    @Override
    public NetworkHelper getNetworkHelper() {
        return networkHelper;
    }

    @Override
    public ConfigHelper getConfigHelper() {
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
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                               int primaryColor,
                               int secondaryColor,
                               Item.Properties properties) {
        return new SpawnEggItem(entityType.get(), primaryColor, secondaryColor, properties);
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
}
