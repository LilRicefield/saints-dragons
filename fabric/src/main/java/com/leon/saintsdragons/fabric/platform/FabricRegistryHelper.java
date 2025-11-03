package com.leon.saintsdragons.fabric.platform;

import com.leon.saintsdragons.platform.RegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public final class FabricRegistryHelper implements RegistryHelper {
    @Override
    public <T> RegistryWrapper<T> create(net.minecraft.resources.ResourceKey<? extends Registry<T>> registryKey,
                                         Supplier<Registry<T>> backingRegistry,
                                         String modId) {
        Registry<T> registry = backingRegistry.get();
        return new Wrapper<>(registry, modId);
    }

    private static final class Wrapper<T> implements RegistryWrapper<T> {
        private final Registry<T> registry;
        private final String modId;

        private Wrapper(Registry<T> registry, String modId) {
            this.registry = registry;
            this.modId = modId;
        }

        @Override
        public <I extends T> Supplier<I> register(String name, Supplier<I> supplier) {
            ResourceLocation id = new ResourceLocation(modId, name);
            I value = supplier.get();
            Registry.register(registry, id, value);
            return () -> value;
        }

        @Override
        public void register() {
            // Fabric registers immediately, nothing to do.
        }
    }
}
