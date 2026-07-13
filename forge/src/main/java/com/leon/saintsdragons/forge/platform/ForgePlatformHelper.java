package com.leon.saintsdragons.forge.platform;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.leon.saintsdragons.common.item.tools.DragonheartSwordItem;
import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.NetworkHelper;
import com.leon.saintsdragons.platform.PlatformHelper;
import com.leon.saintsdragons.platform.RegistryHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.ForgeMod;

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
    public boolean isGenericDiveLoopEnabled() {
        return ForgeClientConfig.GENERIC_DIVE_LOOP_ENABLED == null
                || ForgeClientConfig.GENERIC_DIVE_LOOP_ENABLED.get();
    }

    @Override
    public float getSwarmBattleMusicVolume() {
        if (ForgeClientConfig.SWARM_BATTLE_MUSIC_VOLUME == null) {
            return 1.0F;
        }
        return Math.max(0, Math.min(100, ForgeClientConfig.SWARM_BATTLE_MUSIC_VOLUME.get())) / 100.0F;
    }

    @Override
    public Item createSpawnEgg(Supplier<? extends EntityType<? extends Mob>> entityType,
                               int primaryColor,
                               int secondaryColor,
                               Item.Properties properties) {
        return new ForgeSpawnEggItem(entityType, primaryColor, secondaryColor, properties);
    }

    @Override
    public Item createDraconianSwarmSpawnEgg(Supplier<? extends EntityType<? extends Mob>> displayEntityType,
                                             int primaryColor,
                                             int secondaryColor,
                                             Item.Properties properties) {
        return new ForgeSpawnEggItem(displayEntityType, primaryColor, secondaryColor, properties) {
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
        return new ForgeDragonheartSwordItem(
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
        return new MobBucketItem(entityType, () -> fluid, () -> emptySound, properties);
    }

    @Override
    public net.minecraft.core.particles.SimpleParticleType createSimpleParticle(boolean overrideLimiter) {
        return new net.minecraft.core.particles.SimpleParticleType(overrideLimiter);
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    private static final class ForgeDragonheartSwordItem extends DragonheartSwordItem {
        private ForgeDragonheartSwordItem(Tier tier,
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
                    ForgeMod.ENTITY_REACH.get(),
                    new AttributeModifier(
                            ENTITY_REACH_MODIFIER_UUID,
                            "Dragonheart weapon reach",
                            this.getEntityReachBonus(),
                            AttributeModifier.Operation.ADDITION
                    )
            );
            return builder.build();
        }
    }
}
