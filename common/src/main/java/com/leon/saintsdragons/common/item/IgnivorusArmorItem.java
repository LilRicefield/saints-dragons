package com.leon.saintsdragons.common.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IgnivorusArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = this::createFabricRenderProvider;

    public IgnivorusArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        Object provider = createFabricRenderProvider();
        if (provider != null) {
            consumer.accept(provider);
        }
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    private Object createFabricRenderProvider() {
        try {
            Class<?> providerClass = Class.forName("software.bernie.geckolib.animatable.client.RenderProvider");
            return Proxy.newProxyInstance(IgnivorusArmorItem.class.getClassLoader(), new Class<?>[]{providerClass},
                    (proxy, method, args) -> {
                        if ("getHumanoidArmorModel".equals(method.getName()) || "getGenericArmorModel".equals(method.getName())) {
                            return getHumanoidArmorModel(args);
                        }
                        return null;
                    });
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public void initializeClient(Consumer<Object> consumer) {
        try {
            Class<?> extensions = Class.forName("net.minecraftforge.client.extensions.common.IClientItemExtensions");
            Object proxy = Proxy.newProxyInstance(IgnivorusArmorItem.class.getClassLoader(), new Class<?>[]{extensions},
                    (proxyInstance, method, args) -> {
                        if ("getHumanoidArmorModel".equals(method.getName()) || "getGenericArmorModel".equals(method.getName())) {
                            return getHumanoidArmorModel(args);
                        }
                        return defaultForgeExtensionValue(method.getReturnType());
                    });
            consumer.accept(proxy);
        } catch (ClassNotFoundException ignored) {
        }
    }

    private Object getHumanoidArmorModel(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("com.leon.saintsdragons.client.renderer.armor.IgnivorusArmorRenderProvider");
            return provider.getMethod("getHumanoidArmorModel", Object.class, Object.class, Object.class, Object.class)
                    .invoke(null, args[0], args[1], args[2], args[3]);
        } catch (ReflectiveOperationException ignored) {
            return args[3];
        }
    }

    private static Object defaultForgeExtensionValue(Class<?> type) {
        if (type == Boolean.TYPE) return false;
        if (type == Byte.TYPE) return (byte) 0;
        if (type == Short.TYPE) return (short) 0;
        if (type == Integer.TYPE) return 0;
        if (type == Long.TYPE) return 0L;
        if (type == Float.TYPE) return 0.0F;
        if (type == Double.TYPE) return 0.0D;
        if (type == Character.TYPE) return '\0';
        return null;
    }
}
