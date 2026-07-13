package com.leon.saintsdragons.fabric.platform;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.leon.saintsdragons.common.item.tools.DragonheartSwordItem;
import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Tier;
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
    public float getSwarmBattleMusicVolume() {
        return FabricClientConfigAccess.getSwarmBattleMusicVolume();
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                               int primaryColor,
                               int secondaryColor,
                               Item.Properties properties) {
        return new SpawnEggItem(entityType.get(), primaryColor, secondaryColor, properties);
    }

    @Override
    public Item createDraconianSwarmSpawnEgg(Supplier<? extends EntityType<? extends Mob>> displayEntityType,
                                             int primaryColor,
                                             int secondaryColor,
                                             Item.Properties properties) {
        return new SpawnEggItem(displayEntityType.get(), primaryColor, secondaryColor, properties) {
            @Override
            public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
                return com.leon.saintsdragons.common.item.DraconianSwarmSpawnEggSpawner.useOn(context);
            }
        };
    }

    @Override
    public Item createDragonheartSword(Tier tier,
                                       int attackDamageModifier,
                                       float attackSpeedModifier,
                                       double entityReach,
                                       float criticalDamageBonus,
                                       Item.Properties properties) {
        return new FabricDragonheartSwordItem(
                tier,
                attackDamageModifier,
                attackSpeedModifier,
                entityReach,
                criticalDamageBonus,
                properties
        );
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

    private static final class FabricDragonheartSwordItem extends DragonheartSwordItem {
        // Align Reach Entity Attributes' in-game attack distance with Forge's hitbox-based reach.
        private static final double REACH_ALIGNMENT = 2.0D;

        private FabricDragonheartSwordItem(Tier tier,
                                           int attackDamageModifier,
                                           float attackSpeedModifier,
                                           double entityReach,
                                           float criticalDamageBonus,
                                           Properties properties) {
            super(tier, attackDamageModifier, attackSpeedModifier, entityReach, criticalDamageBonus, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            Multimap<Attribute, AttributeModifier> modifiers = super.getDefaultAttributeModifiers(slot);
            if (slot != EquipmentSlot.MAINHAND) {
                return modifiers;
            }

            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(modifiers);
            builder.put(
                    ReachEntityAttributes.REACH,
                    new AttributeModifier(
                            TARGETING_REACH_MODIFIER_UUID,
                            "Dragonheart weapon targeting reach",
                            this.getTargetingReachBonus() + REACH_ALIGNMENT,
                            AttributeModifier.Operation.ADDITION
                    )
            );
            builder.put(
                    ReachEntityAttributes.ATTACK_RANGE,
                    new AttributeModifier(
                            ENTITY_REACH_MODIFIER_UUID,
                            "Dragonheart weapon reach",
                            this.getEntityReachBonus() + REACH_ALIGNMENT,
                            AttributeModifier.Operation.ADDITION
                    )
            );
            return builder.build();
        }
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
