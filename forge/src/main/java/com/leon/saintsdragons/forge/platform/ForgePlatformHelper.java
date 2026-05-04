package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class ForgePlatformHelper implements PlatformHelper {
    // Lazy initialization to avoid ServiceConfigurationError during early class loading
    private ForgeRegistryHelper registryHelper;
    private ForgeNetworkHelper networkHelper;
    private ForgeConfigHelper configHelper;

    @Override
    public RegistryHelper getRegistryHelper() {
        if (registryHelper == null) {
            registryHelper = new ForgeRegistryHelper();
        }
        return registryHelper;
    }

    @Override
    public NetworkHelper getNetworkHelper() {
        if (networkHelper == null) {
            networkHelper = new ForgeNetworkHelper();
        }
        return networkHelper;
    }

    @Override
    public ConfigHelper getConfigHelper() {
        if (configHelper == null) {
            configHelper = new ForgeConfigHelper();
        }
        return configHelper;
    }

    @Override
    public void runOnClient(Runnable runnable) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> runnable.run());
    }

    @Override
    public <T> T callOnClient(Supplier<T> supplier) {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> supplier.get());
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public String getPlatformId() {
        return "forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                               int primaryColor,
                               int secondaryColor,
                               Item.Properties properties) {
        return new ForgeSpawnEggItem(entityType, primaryColor, secondaryColor, properties);
    }

    @Override
    public net.minecraft.core.particles.SimpleParticleType createSimpleParticle(boolean overrideLimiter) {
        return new net.minecraft.core.particles.SimpleParticleType(overrideLimiter);
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
