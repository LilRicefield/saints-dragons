package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.init.CommonModEvents;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModPotions;
import com.leon.saintsdragons.fabric.entity.part.FabricPartEntities;
import com.leon.saintsdragons.fabric.loot.FabricLootTableModifier;
import com.leon.saintsdragons.fabric.mixin.RangedAttributeAccessor;
import com.leon.saintsdragons.fabric.resource.FabricDragonAttributeReloadListener;
import com.leon.saintsdragons.fabric.resource.FabricDragonVariantReloadListener;
import com.leon.saintsdragons.fabric.server.FabricServerEvents;
import com.leon.saintsdragons.fabric.world.Spawns;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.List;

public final class SaintsDragonsFabric implements ModInitializer {
    private static final double ATTRIBUTE_CAP = 100000.0D;
    private static final List<ResourceKey<CreativeModeTab>> POTION_HIDE_TABS = List.of(
            CreativeModeTabs.FOOD_AND_DRINKS,
            CreativeModeTabs.INGREDIENTS,
            CreativeModeTabs.COMBAT,
            CreativeModeTabs.TOOLS_AND_UTILITIES,
            CreativeModeTabs.SEARCH
    );
    @Override
    public void onInitialize() {
        SaintsDragonsCommon.init();
        raiseVanillaAttributeCaps();
        FabricPartEntities.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricDragonAttributeReloadListener());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricDragonVariantReloadListener());
        FabricServerEvents.init();
        FabricLootTableModifier.register();
        CommonModEvents.registerEntityAttributes(SaintsDragonsFabric::registerDefaultAttributes);
        CommonModEvents.registerSpawnPlacements(SpawnPlacements::register);
        Spawns.register();
        CommonModEvents.registerCreativeTabEntries((tab, itemSupplier) -> ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(itemSupplier.get())));
        for (ResourceKey<CreativeModeTab> tab : POTION_HIDE_TABS) {
            ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> {
                entries.getDisplayStacks().removeIf(SaintsDragonsFabric::isHiddenVanillaPotionVariant);
                entries.getSearchTabStacks().removeIf(SaintsDragonsFabric::isHiddenVanillaPotionVariant);
            });
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CommonModEvents.registerCommands(dispatcher));
    }
    private static void raiseVanillaAttributeCaps() {
        raiseAttributeCap(Attributes.MAX_HEALTH, "MAX_HEALTH");
        raiseAttributeCap(Attributes.ARMOR, "ARMOR");
    }
    private static void raiseAttributeCap(Attribute attribute, String name) {
        if (!(attribute instanceof RangedAttribute ranged)) {
            return;
        }
        RangedAttributeAccessor accessor = (RangedAttributeAccessor) ranged;
        if (accessor.saintsdragons$getMaxValue() >= ATTRIBUTE_CAP) {
            return;
        }
        accessor.saintsdragons$setMaxValue(ATTRIBUTE_CAP);
        SaintsDragonsCommon.LOGGER.info("Raised {} attribute cap to {}", name, ATTRIBUTE_CAP);
    }

    private static <T extends LivingEntity> void registerDefaultAttributes(
            EntityType<? extends T> type,
            AttributeSupplier.Builder builder
    ) {
        FabricDefaultAttributeRegistry.register(type, builder.build());
    }
    private static boolean isHiddenVanillaPotionVariant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.is(ModItems.POTION_OF_TIDEGUARD.get()) || stack.is(ModItems.POTION_OF_SEARING.get())) {
            return true;
        }

        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }

        Potion potion = PotionUtils.getPotion(stack);
        return potion == ModPotions.VARASUCHUS_TIDEGUARD.get() || potion == ModPotions.SEARING.get();
    }
}
