package com.leon.saintsdragons.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.registry.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DragonlordArmorItem extends ArmorItem implements GeoItem {
    public static final String FLIGHT_CONTROLLER = "dragonlord_flight";
    public static final String FLAP_TRIGGER = "flap";
    private static final RawAnimation GLIDE =
            RawAnimation.begin().thenLoop("animation.dragonlord_armor.glide");
    private static final RawAnimation FLAP =
            RawAnimation.begin().thenPlay("animation.dragonlord_armor.flap");
    private static final UUID HELMET_MAX_HEALTH_UUID = UUID.fromString("205f4c06-8e5f-46b5-a0d1-bd0a4b9786d3");
    private static final UUID CHESTPLATE_MAX_HEALTH_UUID = UUID.fromString("4784a314-73a3-4215-96b2-739f5ed31922");
    private static final UUID LEGGINGS_MAX_HEALTH_UUID = UUID.fromString("3fda6176-d876-401c-a1bd-46f754cda64a");
    private static final UUID BOOTS_MAX_HEALTH_UUID = UUID.fromString("c998f3c4-f092-4116-8664-e6e5809db047");
    private static final UUID HELMET_FIRE_RESISTANCE_UUID = UUID.fromString("5e275157-050e-4217-bf91-59a107c3f0e5");
    private static final UUID CHESTPLATE_FIRE_RESISTANCE_UUID = UUID.fromString("9159eb75-1bed-4885-bbce-45d76a935f52");
    private static final UUID LEGGINGS_FIRE_RESISTANCE_UUID = UUID.fromString("49fd952f-d3cb-4aa8-aad4-8d4cbe75c529");
    private static final UUID BOOTS_FIRE_RESISTANCE_UUID = UUID.fromString("44774007-8d4d-4e20-86af-c0c23cbe5300");
    private static final UUID HELMET_BLAST_RESISTANCE_UUID = UUID.fromString("87429296-ff79-47e6-b547-c3b5ab20a6ee");
    private static final UUID CHESTPLATE_BLAST_RESISTANCE_UUID = UUID.fromString("83d9d091-c0bb-4474-8241-401382c9746b");
    private static final UUID LEGGINGS_BLAST_RESISTANCE_UUID = UUID.fromString("c697f967-163d-41dc-bd8a-dcb885cbf190");
    private static final UUID BOOTS_BLAST_RESISTANCE_UUID = UUID.fromString("931b023b-f0a1-4aaa-bc85-6338ec3d44d8");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = this::createFabricRenderProvider;

    public DragonlordArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getDefaultAttributeModifiers(slot);
        if (slot != this.getType().getSlot()) {
            return modifiers;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(ConfiguredItemAttributes.armor(
                modifiers,
                configuredDefense(),
                ToolsArmorConfig.DRAGONLORD_TOUGHNESS.get(),
                configuredKnockbackResistance()
        ));
        builder.put(
                Attributes.MAX_HEALTH,
                new AttributeModifier(
                        maxHealthUuid(),
                        "Dragonlord max health",
                        maxHealthMultiplier(),
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        builder.put(
                ModAttributes.FIRE_RESISTANCE.get(),
                new AttributeModifier(
                        fireResistanceUuid(),
                        "Dragonlord fire resistance",
                        ToolsArmorConfig.DRAGONLORD_FIRE_RESISTANCE.get(),
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                ModAttributes.BLAST_RESISTANCE.get(),
                new AttributeModifier(
                        blastResistanceUuid(),
                        "Dragonlord blast resistance",
                        ToolsArmorConfig.DRAGONLORD_BLAST_RESISTANCE.get(),
                        AttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
    }

    private double configuredKnockbackResistance() {
        return switch (this.getType()) {
            case HELMET -> ToolsArmorConfig.DRAGONLORD_HELMET_KNOCKBACK_RESISTANCE.get();
            case CHESTPLATE -> ToolsArmorConfig.DRAGONLORD_CHESTPLATE_KNOCKBACK_RESISTANCE.get();
            case LEGGINGS -> ToolsArmorConfig.DRAGONLORD_LEGGINGS_KNOCKBACK_RESISTANCE.get();
            case BOOTS -> ToolsArmorConfig.DRAGONLORD_BOOTS_KNOCKBACK_RESISTANCE.get();
        };
    }

    private UUID maxHealthUuid() {
        return switch (this.getType()) {
            case HELMET -> HELMET_MAX_HEALTH_UUID;
            case CHESTPLATE -> CHESTPLATE_MAX_HEALTH_UUID;
            case LEGGINGS -> LEGGINGS_MAX_HEALTH_UUID;
            case BOOTS -> BOOTS_MAX_HEALTH_UUID;
        };
    }

    private double maxHealthMultiplier() {
        return switch (this.getType()) {
            case HELMET -> ToolsArmorConfig.DRAGONLORD_HELMET_MAX_HEALTH_BONUS.get() / 100.0D;
            case CHESTPLATE -> ToolsArmorConfig.DRAGONLORD_CHESTPLATE_MAX_HEALTH_BONUS.get() / 100.0D;
            case LEGGINGS -> ToolsArmorConfig.DRAGONLORD_LEGGINGS_MAX_HEALTH_BONUS.get() / 100.0D;
            case BOOTS -> ToolsArmorConfig.DRAGONLORD_BOOTS_MAX_HEALTH_BONUS.get() / 100.0D;
        };
    }

    @Override
    public int getDefense() {
        return (int) Math.round(configuredDefense());
    }

    @Override
    public float getToughness() {
        return (float) ToolsArmorConfig.DRAGONLORD_TOUGHNESS.get();
    }

    private double configuredDefense() {
        return switch (this.getType()) {
            case HELMET -> ToolsArmorConfig.DRAGONLORD_HELMET_ARMOR.get();
            case CHESTPLATE -> ToolsArmorConfig.DRAGONLORD_CHESTPLATE_ARMOR.get();
            case LEGGINGS -> ToolsArmorConfig.DRAGONLORD_LEGGINGS_ARMOR.get();
            case BOOTS -> ToolsArmorConfig.DRAGONLORD_BOOTS_ARMOR.get();
        };
    }

    private UUID fireResistanceUuid() {
        return switch (this.getType()) {
            case HELMET -> HELMET_FIRE_RESISTANCE_UUID;
            case CHESTPLATE -> CHESTPLATE_FIRE_RESISTANCE_UUID;
            case LEGGINGS -> LEGGINGS_FIRE_RESISTANCE_UUID;
            case BOOTS -> BOOTS_FIRE_RESISTANCE_UUID;
        };
    }

    private UUID blastResistanceUuid() {
        return switch (this.getType()) {
            case HELMET -> HELMET_BLAST_RESISTANCE_UUID;
            case CHESTPLATE -> CHESTPLATE_BLAST_RESISTANCE_UUID;
            case LEGGINGS -> LEGGINGS_BLAST_RESISTANCE_UUID;
            case BOOTS -> BOOTS_BLAST_RESISTANCE_UUID;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<DragonlordArmorItem> flightController = new AnimationController<>(
                this,
                FLIGHT_CONTROLLER,
                3,
                state -> {
                    Entity wearer = state.getData(DataTickets.ENTITY);
                    if (!(wearer instanceof LivingEntity living)
                            || !living.isFallFlying()
                            || !DragonlordArmorSetBonus.isWearingFullSet(living)) {
                        return PlayState.STOP;
                    }

                    state.setAndContinue(GLIDE);
                    return PlayState.CONTINUE;
                }
        );
        flightController.triggerableAnim(FLAP_TRIGGER, FLAP);
        controllers.add(flightController);
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
            return Proxy.newProxyInstance(DragonlordArmorItem.class.getClassLoader(), new Class<?>[]{providerClass},
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
            Object proxy = Proxy.newProxyInstance(DragonlordArmorItem.class.getClassLoader(), new Class<?>[]{extensions},
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
            Class<?> provider = Class.forName("com.leon.saintsdragons.client.renderer.armor.DragonlordArmorRenderProvider");
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
