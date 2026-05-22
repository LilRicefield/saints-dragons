package com.leon.saintsdragons.forge;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.variant.DragonVariantReloadListener;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.common.registry.ModPotions;
import com.leon.saintsdragons.forge.client.ForgeConfigRootScreen;
import com.leon.saintsdragons.forge.data.SaintsDragonBiomeTagsProvider;
import com.leon.saintsdragons.forge.data.SaintsDragonBlockTagsProvider;
import com.leon.saintsdragons.forge.data.SaintsDragonEntityTypeTagsProvider;
import com.leon.saintsdragons.forge.data.SaintsDragonItemTagsProvider;
import com.leon.saintsdragons.forge.data.SaintsDragonLootTableProvider;
import com.leon.saintsdragons.forge.init.ForgeBrewingRecipes;
import com.leon.saintsdragons.forge.loot.ModLootModifiers;
import com.leon.saintsdragons.forge.mixin.RangedAttributeAccessor;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig;
import com.leon.saintsdragons.forge.world.AddConditionalFeaturesBiomeModifier;
import com.leon.saintsdragons.forge.world.AddDragonsBiomeModifier;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Mod(SaintsDragonsCommon.MOD_ID)
public final class SaintsDragonsForge {
    private static final double ATTRIBUTE_CAP = 100000.0D;
    private static final String FORGE_ATTRIBUTES_CONFIG_FILE = SaintsDragonsConfig.SERVER_CONFIG_FOLDER + "/attributes.toml";
    private static final String FORGE_CLIENT_CONFIG_FILE = SaintsDragonsConfig.CLIENT_CONFIG_FOLDER + "/clientcommon.toml";
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, SaintsDragonsCommon.MOD_ID);

    public static final RegistryObject<Codec<AddConditionalFeaturesBiomeModifier>> ADD_CONDITIONAL_FEATURES =
            BIOME_MODIFIERS.register("add_conditional_features", () -> AddConditionalFeaturesBiomeModifier.CODEC);

    public static final RegistryObject<Codec<AddDragonsBiomeModifier>> ADD_DRAGONS =
            BIOME_MODIFIERS.register("add_dragons", () -> AddDragonsBiomeModifier.CODEC);

    public SaintsDragonsForge() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        raiseVanillaMaxHealthCap();
        BIOME_MODIFIERS.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC,
                FORGE_ATTRIBUTES_CONFIG_FILE);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientOnly::registerConfigScreen);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onBuildCreativeTabs);
        modEventBus.addListener(this::onRegisterSpawnPlacements);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onGatherData);
        modEventBus.addListener(this::onModConfigEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        SaintsDragonsCommon.init();
    }

    private static void raiseVanillaMaxHealthCap() {
        if (!(Attributes.MAX_HEALTH instanceof RangedAttribute ranged)) {
            raiseVanillaArmorCap();
            return;
        }

        RangedAttributeAccessor accessor = (RangedAttributeAccessor) ranged;
        if (accessor.saintsdragons$getMaxValue() < ATTRIBUTE_CAP) {
            accessor.saintsdragons$setMaxValue(ATTRIBUTE_CAP);
            SaintsDragonsCommon.LOGGER.info("Raised MAX_HEALTH attribute cap to {}", ATTRIBUTE_CAP);
        }
        raiseVanillaArmorCap();
    }

    private static void raiseVanillaArmorCap() {
        if (!(Attributes.ARMOR instanceof RangedAttribute ranged)) {
            return;
        }
        RangedAttributeAccessor accessor = (RangedAttributeAccessor) ranged;
        if (accessor.saintsdragons$getMaxValue() >= ATTRIBUTE_CAP) {
            return;
        }
        accessor.saintsdragons$setMaxValue(ATTRIBUTE_CAP);
        SaintsDragonsCommon.LOGGER.info("Raised ARMOR attribute cap to {}", ATTRIBUTE_CAP);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        CommonModEvents.registerEntityAttributes((type, builder) -> event.put(type, builder.build()));
    }
    private void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        CommonModEvents.registerCreativeTabEntries((tabKey, itemSupplier) -> {
            if (event.getTabKey().equals(tabKey)) {
                event.accept(itemSupplier::get);
            }
        });

        List<ItemStack> toRemove = new ArrayList<>();
        for (Map.Entry<ItemStack, ?> entry : event.getEntries()) {
            if (isHiddenVanillaPotionVariant(entry.getKey())) {
                toRemove.add(entry.getKey());
            }
        }
        for (ItemStack stack : toRemove) {
            event.getEntries().remove(stack);
        }
    }

    private static boolean isHiddenVanillaPotionVariant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }

        Potion potion = PotionUtils.getPotion(stack);
        return potion == ModPotions.VARASUCHUS_TIDEGUARD.get() || potion == ModPotions.SEARING.get();
    }

    private void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        CommonModEvents.registerSpawnPlacements(new CommonModEvents.SpawnPlacementRegistrar() {
            @Override
            public <T extends Mob> void register(
                    EntityType<T> type,
                    SpawnPlacements.Type placementType,
                    Heightmap.Types heightmap,
                    SpawnPlacements.SpawnPredicate<T> predicate
            ) {
                event.register(type, placementType, heightmap, predicate, SpawnPlacementRegisterEvent.Operation.AND);
            }
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommonModEvents.registerCommands(event.getDispatcher());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ForgeBrewingRecipes::register);
    }

    private void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existingFileHelper = event.getExistingFileHelper();

        var blockTags = new SaintsDragonBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(),
                new SaintsDragonItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(),
                new SaintsDragonEntityTypeTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new SaintsDragonBiomeTagsProvider(output, lookupProvider));
        generator.addProvider(event.includeServer(),
                SaintsDragonLootTableProvider.create(output));
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DragonAttributeConfigLoader.getInstance());
        event.addListener(DragonVariantReloadListener.getInstance());
    }

    private void onModConfigEvent(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (!SaintsDragonsCommon.MOD_ID.equals(config.getModId())) {
            return;
        }
        if (config.getType() != ModConfig.Type.COMMON) {
            return;
        }

        if (matchesConfigFile(config.getFileName(), FORGE_ATTRIBUTES_CONFIG_FILE)) {
            DragonAttributeConfigLoader.getInstance().refreshFromForgeConfig();
            applyAttributesToLoadedDragons();
        }
    }

    private static boolean matchesConfigFile(String actual, String expected) {
        return actual != null && actual.replace('\\', '/').equals(expected);
    }

    private void applyAttributesToLoadedDragons() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (var level : server.getAllLevels()) {
            AABB bounds = new AABB(
                    level.getWorldBorder().getMinX(),
                    level.getMinBuildHeight(),
                    level.getWorldBorder().getMinZ(),
                    level.getWorldBorder().getMaxX(),
                    level.getMaxBuildHeight(),
                    level.getWorldBorder().getMaxZ()
            );

            for (var dragon : level.getEntitiesOfClass(DragonEntity.class, bounds)) {
                if (dragon instanceof Cindervane cindervane) {
                    cindervane.applyConfiguredAttributes();
                } else if (dragon instanceof Raevyx raevyx) {
                    raevyx.applyConfiguredAttributes();
                } else if (dragon instanceof Varasuchus varasuchus) {
                    varasuchus.applyConfiguredAttributes();
                } else if (dragon instanceof Ignivorus ignivorus) {
                    ignivorus.applyConfiguredAttributes();
                } else if (dragon instanceof Volitans volitans) {
                    volitans.applyConfiguredAttributes();
                } else if (dragon instanceof Stegonaut stegonaut) {
                    stegonaut.applyConfiguredAttributes();
                } else if (dragon instanceof Nulljaw nulljaw) {
                    nulljaw.applyConfiguredAttributes();
                }
            }
        }
    }

    private static final class ClientOnly {
        private static void registerConfigScreen() {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,
                    ForgeClientConfig.CLIENT_SPEC,
                    FORGE_CLIENT_CONFIG_FILE);
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> new ForgeConfigRootScreen(parent)
                    )
            );
        }
    }
}
