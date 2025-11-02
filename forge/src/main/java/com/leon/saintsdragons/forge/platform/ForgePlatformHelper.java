package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.function.Supplier;

public final class ForgePlatformHelper implements PlatformHelper {
    private final ForgeRegistryHelper registryHelper = new ForgeRegistryHelper();
    private final ForgeNetworkHelper networkHelper = new ForgeNetworkHelper();
    private final ForgeConfigHelper configHelper = new ForgeConfigHelper();

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
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> runnable);
    }

    @Override
    public <T> T callOnClient(Supplier<T> supplier) {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> supplier);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
