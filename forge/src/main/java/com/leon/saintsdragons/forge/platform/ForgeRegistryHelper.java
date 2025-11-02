package com.leon.saintsdragons.forge.platform;

import com.leon.saintsdragons.platform.RegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ForgeRegistryHelper implements RegistryHelper {
    @Override
    public <T> RegistryWrapper<T> create(ResourceKey<? extends Registry<T>> registryKey,
                                         Supplier<Registry<T>> backingRegistry,
                                         String modId) {
        DeferredRegister<T> deferredRegister = DeferredRegister.create(registryKey, modId);
        return new Wrapper<>(deferredRegister);
    }

    private static final class Wrapper<T> implements RegistryWrapper<T> {
        private final DeferredRegister<T> deferredRegister;
        private final List<RegistryObject<? extends T>> entries = new ArrayList<>();

        private Wrapper(DeferredRegister<T> deferredRegister) {
            this.deferredRegister = deferredRegister;
        }

        @Override
        public <I extends T> Supplier<I> register(String name, Supplier<I> supplier) {
            RegistryObject<I> registryObject = deferredRegister.register(name, supplier);
            entries.add(registryObject);
            return registryObject::get;
        }

        @Override
        public void register() {
            deferredRegister.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
