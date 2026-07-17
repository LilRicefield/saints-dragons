package com.leon.saintsdragons.common.item;

import com.google.common.collect.Multimap;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

public class BloodTempestArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = this::createFabricRenderProvider;

    public BloodTempestArmorItem(ArmorMaterial armorMaterial, Type type, Properties properties) {
        super(armorMaterial, type, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != getType().getSlot()) {
            return base;
        }
        return ConfiguredItemAttributes.armor(base, configuredDefense(),
                ToolsArmorConfig.BLOOD_TEMPEST_TOUGHNESS.get(),
                ToolsArmorConfig.BLOOD_TEMPEST_KNOCKBACK_RESISTANCE.get());
    }

    @Override
    public int getDefense() {
        return (int) Math.round(configuredDefense());
    }

    @Override
    public float getToughness() {
        return (float) ToolsArmorConfig.BLOOD_TEMPEST_TOUGHNESS.get();
    }

    private double configuredDefense() {
        return switch (getType()) {
            case HELMET -> ToolsArmorConfig.BLOOD_TEMPEST_HELMET_ARMOR.get();
            case CHESTPLATE -> ToolsArmorConfig.BLOOD_TEMPEST_CHESTPLATE_ARMOR.get();
            case LEGGINGS -> ToolsArmorConfig.BLOOD_TEMPEST_LEGGINGS_ARMOR.get();
            case BOOTS -> ToolsArmorConfig.BLOOD_TEMPEST_BOOTS_ARMOR.get();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.saintsdragons.blood_tempest_armor.tooltip")
                .withStyle(ChatFormatting.GRAY));
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
            Class<?> renderProviderClass = Class.forName("software.bernie.geckolib.animatable.client.RenderProvider");
            return Proxy.newProxyInstance(
                    BloodTempestArmorItem.class.getClassLoader(),
                    new Class<?>[]{renderProviderClass},
                    (proxyInstance, method, args) -> {
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
            Object proxy = Proxy.newProxyInstance(
                    BloodTempestArmorItem.class.getClassLoader(),
                    new Class<?>[]{extensions},
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
            Class<?> provider = Class.forName("com.leon.saintsdragons.client.renderer.armor.BloodTempestRenderProvider");
            return provider.getMethod("getHumanoidArmorModel", Object.class, Object.class, Object.class, Object.class)
                    .invoke(null, args[0], args[1], args[2], args[3]);
        } catch (ReflectiveOperationException ignored) {
            return args[3];
        }
    }

    private static Object defaultForgeExtensionValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
